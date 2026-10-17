#!/usr/bin/env bash
# Peostectica / HereShift — private Android emulator smoke tests.
# No networked customer data, account registration, or real-world spoofing.
set -euo pipefail
APP='com.peostectica.hereshift'
QA_DIR="${GITHUB_WORKSPACE:-$PWD}/qa-results"
mkdir -p "$QA_DIR"
APK='Development/Apps/HereShift/Android App/app/build/outputs/apk/debug/app-debug.apk'
adb wait-for-device
adb install -r "$APK"
adb shell am start -W -n "$APP/.MainActivity"
sleep 4
adb shell pidof "$APP" >/dev/null
adb exec-out screencap -p > "$QA_DIR/home.png"

# Use the emulator's own UI tree, not pixel-position guesses, to operate the application.
python3 - "$QA_DIR" "$APP" <<'PY'
import sys,subprocess,xml.etree.ElementTree as ET,re,time
from pathlib import Path
folder=Path(sys.argv[1]); app=sys.argv[2]
def run(*args):
 return subprocess.run(['adb',*args],check=True,capture_output=True,text=True).stdout
def capture(name):
 try: run('shell','uiautomator','dump','/sdcard/hs-window.xml')
 except subprocess.CalledProcessError as exc: raise AssertionError(f'Unable to dump UI {name}: {exc.stderr}')
 raw=run('exec-out','cat','/sdcard/hs-window.xml')
 (folder/(name+'.xml')).write_text(raw)
 try: root=ET.fromstring(raw)
 except ET.ParseError as exc: raise AssertionError(f'UI not parseable {name}: {exc}')
 subprocess.run(['adb','exec-out','screencap','-p'],stdout=(folder/(name+'.png')).open('wb'),check=True)
 return list(root.iter())
def match(node,piece):
 return piece.casefold() in (node.get('text','')+' '+node.get('content-desc','')).casefold()
def tap(nodes,*phrases):
 for phrase in phrases:
  for node in nodes:
   if match(node,phrase):
    m=re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',node.get('bounds',''))
    if m:
     x1,y1,x2,y2=map(int,m.groups());run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
     time.sleep(1.2)
     return
 raise AssertionError('Cannot locate tappable target '+str(phrases))

home=capture('home')
assert any(match(x,'HereShift') for x in home),'App did not display HereShift landing page'
tap(home,'Display · 40 color themes','Display')
display=capture('display')
assert any(match(x,'40 approved paired themes') for x in display),'Display settings not opened'
assert any(match(x,'Monochrome') for x in display),'Theme list missing first option'
# Select theme 02, then confirm it persists through Back and reopening Settings.
tap(display,'02 · Slate Blue','Slate Blue')
selected=capture('display-selected')
buttons=[x for x in selected if x.get('class')=='android.widget.RadioButton']
assert len(buttons)>1 and buttons[1].get('checked')=='true','Theme 02 radio did not become selected'
# Back must return to previous screen.
run('shell','input','keyevent','4');time.sleep(1)
returned=capture('returned-home')
assert any(match(x,'Audio · 20 sound themes') for x in returned),'Back navigation did not restore home'
tap(returned,'Display · 40 color themes','Display')
persisted=capture('display-reopened')
radios=[x for x in persisted if x.get('class')=='android.widget.RadioButton']
assert len(radios)>1 and radios[1].get('checked')=='true','Theme 02 preference did not survive settings navigation'
run('shell','input','keyevent','4');time.sleep(1)
returned=capture('returned-home-again')
tap(returned,'Audio · 20 sound themes','Audio')
audio=capture('audio')
assert any(match(x,'Audio themes') for x in audio),'Audio settings not opened'
assert any(match(x,'None') for x in audio),'Audio disable option is missing'
assert any(match(x,'Casino') for x in audio),'Casino audio option missing'
tap(audio,'01 · Casino','Casino')
audio_selected=capture('audio-casino-selected')
radios=[x for x in audio_selected if x.get('class')=='android.widget.RadioButton']
assert len(radios)>1 and radios[1].get('checked')=='true','Casino selection was not saved'
print('PASS Android emulator: launch, Display selected theme persisted, Back navigation, Audio Casino selected, None option')
(folder/'results.txt').write_text('PASS launch/home/display/theme 02 persistence/back/audio Casino selection UI smoke test\n')
PY
adb logcat -d -t 3500 > "$QA_DIR/logcat.txt" || true



# Private service is android:exported=false. NEVER weaken its manifest merely so
# an adb shell caller can start it. Use the app's own user-facing UI to request
# and stop location injection inside an isolated authorized emulator.
python3 - "$QA_DIR" "$APP" <<'PY_SERVICE'
import sys,subprocess,xml.etree.ElementTree as ET,re,time
from pathlib import Path
folder=Path(sys.argv[1]);app=sys.argv[2]
def run(*args):
 return subprocess.run(['adb',*args],check=True,capture_output=True,text=True).stdout
def capture(name):
 run('shell','uiautomator','dump','/sdcard/hs-window.xml')
 raw=run('exec-out','cat','/sdcard/hs-window.xml')
 (folder/(name+'.xml')).write_text(raw)
 nodes=list(ET.fromstring(raw).iter())
 subprocess.run(['adb','exec-out','screencap','-p'],stdout=(folder/(name+'.png')).open('wb'),check=True)
 return nodes
def click(name,nodes):
 for node in nodes:
  if name.casefold() in (node.get('text','')+' '+node.get('content-desc','')).casefold():
   m=re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',node.get('bounds',''))
   if m:
    x1,y1,x2,y2=map(int,m.groups())
    run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    time.sleep(1.2)
    return True
 return False
def scroll_click(name):
 for i in range(7):
  nodes=capture('find-'+name.split()[0]+'-'+str(i))
  if click(name,nodes):return
  run('shell','input','swipe','160','530','160','240','380')
  time.sleep(.55)
 raise AssertionError('Could not scroll to '+name)
def get_status():return run('exec-out','run-as',app,'cat','shared_prefs/hereShift.xml')
run('shell','pm','grant',app,'android.permission.ACCESS_COARSE_LOCATION')
run('shell','pm','grant',app,'android.permission.ACCESS_FINE_LOCATION')
run('shell','pm','grant',app,'android.permission.POST_NOTIFICATIONS')
run('shell','appops','set',app,'android:mock_location','allow')
# Return from Audio to Home using normal Android Back.
run('shell','input','keyevent','4');time.sleep(1)
scroll_click('Start authorized mock-location session')
dialog=capture('mock-start-confirmation')
assert any('Confirm authorized test simulation' in x.get('text','') for x in dialog),'Missing informed mock-start confirmation'
assert click('CONTINUE',[node for node in dialog if node.get('class')=='android.widget.Button']),'Could not press the actual on-app confirmation button'
time.sleep(4)
# Save UI and logcat before assertions, including when the service cannot start.
capture('after-continue')
(folder/'logcat-service.txt').write_text(run('logcat','-d','-t','2500'))
active=get_status()
(folder/'foreground-active.xml').write_text(active)
assert 'ACTIVE' in active, 'Location provider did not report ACTIVE; '+active[-800:]
scroll_click('Stop simulation')
time.sleep(2)
status=get_status()
(folder/'foreground-stopped.xml').write_text(status)
assert 'Stopped. Android test provider removed.' in status,'Private Stop control failed to confirm provider cleanup: '+status[-800:]
run('shell','appops','set',app,'android:mock_location','deny')
(folder/'results.txt').open('a').write('PASS isolated emulator mock-provider start and Stop through non-exported app UI, cleanup confirmed\n')
print('PASS private test service authorized and stopped from app UI, provider removal confirmed')
PY_SERVICE
adb logcat -d -t 4500 > "$QA_DIR/logcat.txt" || true

# Verify the new real-user Development Mode is navigable and does not activate
# mock location merely by opening the test interface.
python3 - "$QA_DIR" "$APP" <<'PY_UI_TEST'
import subprocess,sys,xml.etree.ElementTree as ET,re,time
from pathlib import Path
folder=Path(sys.argv[1]);app=sys.argv[2]
def run(*args):
 return subprocess.run(['adb',*args],capture_output=True,text=True,check=True).stdout
run('shell','am','force-stop',app)
run('shell','am','start','-n',app+'/.MainActivity')
time.sleep(2)
def nodes(label):
 run('shell','uiautomator','dump','/sdcard/hs-window.xml')
 raw=run('exec-out','cat','/sdcard/hs-window.xml')
 (folder/(label+'.xml')).write_text(raw)
 return list(ET.fromstring(raw).iter())
def click(phrase):
 for attempt in range(9):
  for n in nodes('development-find-'+str(attempt)):
   if phrase.casefold() in (n.get('text','')+' '+n.get('content-desc','')).casefold():
    m=re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',n.get('bounds',''))
    if m:
     x1,y1,x2,y2=map(int,m.groups())
     run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
     time.sleep(1)
     return
  run('shell','input','swipe','170','570','170','230','400')
 raise AssertionError('Missing UI element '+phrase)
click('Development mode')
page=nodes('development-mode')
assert any('Real-user UI testing' in n.get('text','') for n in page),'Development Mode heading missing'
assert any('Guided interface checklist' in n.get('text','') for n in page),'UI checklist missing'
first_task_found=False
for attempt in range(8):
 page=nodes('development-checklist-'+str(attempt))
 if any('Home and navigation' in n.get('text','') for n in page):
  first_task_found=True
  break
 run('shell','input','swipe','170','600','170','230','380')
 time.sleep(.4)
assert first_task_found,'First test task missing after scrolling'
feedback_found=False
for attempt in range(10):
 page=nodes('development-feedback-'+str(attempt))
 if any('Tester feedback' in n.get('text','') for n in page):
  feedback_found=True
  break
 run('shell','input','swipe','170','600','170','210','380')
 time.sleep(.4)
assert feedback_found,'Feedback section missing after scrolling'
status=run('exec-out','run-as',app,'cat','shared_prefs/hereShift.xml')
assert 'ACTIVE' not in status,'Opening UI test mode unexpectedly activated location service'
print('PASS Development Mode opens with checklist and feedback without starting mock service')
PY_UI_TEST
