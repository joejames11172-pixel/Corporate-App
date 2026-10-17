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
# Back must return to previous screen.
run('shell','input','keyevent','4');time.sleep(1)
returned=capture('returned-home')
assert any(match(x,'Audio · 20 sound themes') for x in returned),'Back navigation did not restore home'
tap(returned,'Audio · 20 sound themes','Audio')
audio=capture('audio')
assert any(match(x,'Audio themes') for x in audio),'Audio settings not opened'
assert any(match(x,'None') for x in audio),'Audio disable option is missing'
assert any(match(x,'Casino') for x in audio),'Casino audio option missing'
print('PASS Android emulator: launch, home, display, 40-theme header, Back, audio, None and Casino')
(folder/'results.txt').write_text('PASS launch/home/display/back/audio UI smoke test\n')
PY
adb logcat -d -t 3500 > "$QA_DIR/logcat.txt" || true