"""Destructive test data scenarios, restricted to this task's disposable debug AVD.

State seeding prepares completed stars/errors only. Registration/retry/reset are
performed through real UI and interruption uses real ActivityManager commands.
Never point this at a personal device or a release package.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

p = argparse.ArgumentParser()
p.add_argument('--mode', choices=['registration', 'storage', 'clock'], required=True)
p.add_argument('--count', type=int, default=20)
a = p.parse_args()
ADB = str(Path('.tools/android-sdk/platform-tools/adb.exe').resolve())
PKG = 'com.example.zerostar.debug'
OUT = Path('artifacts/qa-run/device-edges')
OUT.mkdir(parents=True, exist_ok=True)

def adb(*args, data=None, check=True):
    r = subprocess.run([ADB, '-s', 'emulator-5554', *args], input=data,
                       capture_output=True, timeout=60)
    if check and r.returncode:
        raise RuntimeError(r.stderr.decode('utf-8', errors='replace'))
    return r.stdout.decode('utf-8', errors='replace')

assert adb('emu', 'avd', 'name').splitlines()[0] in ('zerostar-16kb', 'zerostar-api26'), 'Wrong emulator'
assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1'
assert 'package:' in adb('shell', 'pm', 'path', PKG)

def shell(command): return adb('shell', command)
def stop(): shell(f'am force-stop {PKG}')
def start(): shell(f'am start -W -n {PKG}/com.zerostar.game.MainActivity')
def epoch(): return int(shell('date +%s').strip()) * 1000
def set_time(milliseconds):
    if int(shell('getprop ro.build.version.sdk').strip()) >= 28:
        shell(f'cmd alarm set-time {milliseconds}')
    else:
        stamp = time.strftime('%m%d%H%M%Y.%S', time.gmtime(milliseconds / 1000))
        shell(f'date -u {stamp}')
def raw(): return shell(f'run-as {PKG} cat files/datastore/game.json')
def saved(): return json.loads(raw())
def write_raw(value):
    shell(f'run-as {PKG} mkdir -p files/datastore')
    adb('shell', f"run-as {PKG} sh -c 'cat > files/datastore/game.json'", data=value.encode())

def state(**overrides):
    return dict(schemaVersion=1, balanceMilli=500000000, earnedMilli=500000000,
                upgradeLevel=3, collectedCount=0, lastSettledEpochMs=epoch(),
                soundEnabled=False, reduceMotion=True, tutorialSeen=True) | overrides

def seed(value):
    stop()
    write_raw(json.dumps(value))

def tree():
    shell('uiautomator dump /sdcard/zerostar-edges.xml')
    return ET.fromstring(shell('cat /sdcard/zerostar-edges.xml'))

def tap(label, required=True):
    for _ in range(4 if required else 1):
        root = tree()
        for node in root.iter('node'):
            if node.get('text') == label or node.get('content-desc') == label:
                x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
                shell(f'input tap {(x1+x2)//2} {(y1+y2)//2}')
                return True
        # A cold start can finish loading after the first UI dump. Dismiss only
        # the game's already-paid reward notice, never a registration/reset.
        if required and any(n.get('text') == '다시 만나 반가워요' for n in root.iter('node')):
            for node in root.iter('node'):
                if node.get('text') == '확인':
                    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
                    shell(f'input tap {(x1+x2)//2} {(y1+y2)//2}')
        time.sleep(.2)
    if required:
        raise AssertionError(f'Missing UI: {label}; ' + str([n.get('text') for n in root.iter('node')]))
    return False

def wait_state(predicate):
    until = time.monotonic() + 12
    while time.monotonic() < until:
        try:
            result = saved()
            if predicate(result): return result
        except (ValueError, RuntimeError): pass
        time.sleep(.15)
    raise AssertionError('Saved state did not reach expected value')

def capture(name):
    root = tree()
    (OUT / (name+'.xml')).write_bytes(ET.tostring(root, encoding='utf-8'))
    shell(f'screencap -p /sdcard/zerostar-edges.png')
    adb('pull', '/sdcard/zerostar-edges.png', str(OUT/(name+'.png')))
    return root

rows=[]
try:
    if a.mode == 'registration':
        for i in range(a.count):
            c = i % 6
            initial = state(collectedCount=c)
            seed(initial)
            start()
            tap('확인', required=False)
            tap('도감에 남기기')
            tap('도감 완성' if c == 5 else '다음 별 시작')
            committed = wait_state(lambda s: s['collectedCount'] == c+1)
            stop()  # immediately after observing the real persisted transaction
            start()
            restored = wait_state(lambda s: s['collectedCount'] == c+1)
            assert restored['soundEnabled'] is False and restored['reduceMotion'] is True
            assert restored['tutorialSeen'] is True
            if c < 5:
                assert restored['upgradeLevel'] == 0
                assert restored['balanceMilli'] == restored['earnedMilli'] < 100000
            else:
                assert restored['upgradeLevel'] == committed['upgradeLevel']
                assert restored['balanceMilli'] == committed['balanceMilli']
                assert restored['earnedMilli'] == committed['earnedMilli']
            rows.append(dict(cycle=i+1, before=initial, committed=committed, restored=restored))
            print(f'registration {i+1}/{a.count} PASS (C{c}->{c+1})', flush=True)
            (OUT/'registration.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
        capture('registered-restored')
    elif a.mode == 'storage':
        for kind, corrupt in [('truncated','{"schemaVersion":'),('future',json.dumps(state(schemaVersion=2)))]:
            stop(); write_raw(corrupt); start()
            root=capture(kind+'-error')
            assert any('기존 파일을 자동으로 지우지 않았습니다' in n.get('text','') for n in root.iter('node'))
            assert raw() == corrupt
            tap('다시 시도')
            assert raw() == corrupt
            # Repair only the deliberately seeded test fixture; retry is the actual UI.
            valid=state(balanceMilli=42000,earnedMilli=62000,upgradeLevel=1)
            write_raw(json.dumps(valid)); tap('다시 시도')
            restored=wait_state(lambda s:s['upgradeLevel']==1)
            root=capture(kind+'-retry-recovered')
            assert not any('기존 파일을 자동으로 지우지 않았습니다' in n.get('text','') for n in root.iter('node'))
            assert restored['earnedMilli']-restored['balanceMilli']==20000
            rows.append(dict(case=kind,restored=restored))
            print(kind+' preserve and UI retry PASS',flush=True)
        # A directory at DataStore's temporary-file path causes an actual failed write.
        seed(state(balanceMilli=200000,earnedMilli=220000,upgradeLevel=1))
        start(); tap('확인',required=False)
        before=saved()
        shell(f'run-as {PKG} mkdir files/datastore/game.json.tmp')
        shell(f"run-as {PKG} sh -c 'echo test > files/datastore/game.json.tmp/blocker'")
        tap('빛 모으기 · 25 별빛')
        root=capture('write-failure')
        assert any('기존 파일을 자동으로 지우지 않았습니다' in n.get('text','') for n in root.iter('node'))
        assert saved()==before
        shell(f'run-as {PKG} rm files/datastore/game.json.tmp/blocker')
        shell(f'run-as {PKG} rmdir files/datastore/game.json.tmp')
        tap('다시 시도'); tap('확인',required=False); tap('빛 모으기 · 25 별빛')
        after=wait_state(lambda s:s['upgradeLevel']==2)
        assert after['earnedMilli']-after['balanceMilli']==45000
        rows.append(dict(case='write-failure',before=before,after=after))
        print('write failure preserves file, retry and purchase PASS',flush=True)
        # Explicit reset is the only path that may replace a corrupt save.
        stop(); write_raw('{broken'); start(); tap('전체 초기화'); tap('취소')
        assert raw()=='{broken'
        tap('전체 초기화'); tap('모두 지우기')
        reset=wait_state(lambda s:s['collectedCount']==0 and s['upgradeLevel']==0)
        assert reset['balanceMilli'] < 10000 and reset['soundEnabled'] is True and reset['tutorialSeen'] is False
        capture('explicit-reset')
        rows.append(dict(case='explicit-reset',state=reset))
        print('corrupt save cancel preserves / explicit reset succeeds PASS',flush=True)
    else:
        auto=shell('settings get global auto_time').strip()
        try:
            shell('settings put global auto_time 0')
            for hours in [1,-2,24]:
                seed(state(balanceMilli=42000,earnedMilli=62000,upgradeLevel=1))
                start(); tap('확인',required=False); stop()
                before=saved()
                target=epoch()+hours*3600000
                set_time(target)
                actual=epoch()
                assert abs(actual-target)<3000, (target,actual)
                start()
                after=wait_state(lambda s:s['lastSettledEpochMs']>before['lastSettledEpochMs'] if hours>0 else s['lastSettledEpochMs']<before['lastSettledEpochMs'])
                seconds=max(0,min(28800000,after['lastSettledEpochMs']-before['lastSettledEpochMs']))//1000
                assert after['balanceMilli']-before['balanceMilli']==seconds*3000
                stop(); start(); again=saved()
                elapsed=max(0,again['lastSettledEpochMs']-after['lastSettledEpochMs'])//1000
                assert again['balanceMilli']-after['balanceMilli']==elapsed*3000
                rows.append(dict(hours=hours,before=before,after=after,reopened=again))
                print(f'OS clock {hours:+}h: {seconds}s reward, duplicate reopen safe PASS',flush=True)
        finally:
            stop(); set_time(int(time.time()*1000))
            shell(f'settings put global auto_time {auto}')
    (OUT/(a.mode+'.json')).write_text(json.dumps(rows,indent=2),encoding='utf-8')
finally:
    adb('shell', f'am force-stop {PKG}', check=False)
