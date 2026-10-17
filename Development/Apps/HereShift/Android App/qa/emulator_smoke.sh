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


# This separate test is isolated to a disposable, authorized Android emulator.
# HereShift's test-provider privileges are granted by the test harness only;
# no real device or third-party app is modified.
QA_DIR="${GITHUB_WORKSPACE:-$PWD}/qa-results"
APP='com.peostectica.hereshift'
adb shell pm grant "$APP" android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant "$APP" android.permission.ACCESS_FINE_LOCATION
adb shell pm grant "$APP" android.permission.POST_NOTIFICATIONS
adb shell appops set "$APP" android:mock_location allow
adb shell am start-foreground-service -n "$APP/.MockLocationService" \
   -a "$APP.START_TEST" --ed latitude 37.7749 --ed longitude -122.4194
sleep 5
adb exec-out run-as "$APP" cat shared_prefs/hereShift.xml > "$QA_DIR/foreground-active.xml"
if ! grep -q 'ACTIVE' "$QA_DIR/foreground-active.xml"; then
  echo 'FAILED: Foreground service never confirmed authorized active state' >&2
  cat "$QA_DIR/foreground-active.xml" >&2
  exit 1
fi
adb shell am startservice -n "$APP/.MockLocationService" -a "$APP.STOP_TEST"
sleep 2
adb exec-out run-as "$APP" cat shared_prefs/hereShift.xml > "$QA_DIR/foreground-stopped.xml"
if ! grep -q 'Stopped. Android test provider removed.' "$QA_DIR/foreground-stopped.xml"; then
  echo 'FAILED: Stop action did not confirm test provider cleanup' >&2
  cat "$QA_DIR/foreground-stopped.xml" >&2
  exit 1
fi
adb shell appops set "$APP" android:mock_location deny
printf 'PASS explicit emulator test-provider authorization, start, STOP, confirmed provider cleanup\n' >> "$QA_DIR/results.txt"
adb logcat -d -t 4500 > "$QA_DIR/logcat.txt" || true