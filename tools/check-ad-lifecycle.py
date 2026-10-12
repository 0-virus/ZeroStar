"""Exercise test banners on the disposable AVD. Never taps advertising content."""
import json
from pathlib import Path
import re
import subprocess
import time
import sys
import xml.etree.ElementTree as ET

ADB=str(Path('.tools/android-sdk/platform-tools/adb.exe').resolve())
PKG='com.example.zerostar.debug'
OUT=Path('artifacts/qa-run/ad-lifecycle'); OUT.mkdir(parents=True,exist_ok=True)
def adb(*args, data=None):
    r=subprocess.run([ADB,'-s','emulator-5554',*args],input=data,capture_output=True,timeout=60)
    if r.returncode: raise RuntimeError(r.stderr.decode(errors='replace'))
    return r.stdout.decode('utf-8',errors='replace')
def sh(command): return adb('shell',command)
assert adb('emu','avd','name').splitlines()[0]=='zerostar-api26'
assert sh('getprop ro.kernel.qemu').strip()=='1'
def start(): sh(f'am start -W -n {PKG}/com.zerostar.game.MainActivity')
def stop(): sh(f'am force-stop {PKG}')
def tree():
    sh('uiautomator dump /sdcard/zerostar-ads.xml')
    return ET.fromstring(sh('cat /sdcard/zerostar-ads.xml'))
def bounds(node): return list(map(int,re.findall(r'\d+',node.get('bounds',''))))
def webs(root):
    # The SDK accessibility tree may expose nested WebViews for the same banner.
    # Count top-level visible WebView subtrees, not duplicate child wrappers.
    found=[]
    def visit(node, inside=False):
        web=node.get('class')=='android.webkit.WebView'
        if web and not inside and bounds(node)[3]>bounds(node)[1]: found.append(node)
        for child in node: visit(child,inside or web)
    visit(root)
    return found
def tap_game(label, tab=False):
    for _ in range(5):
        root=tree()
        # Only the two app tabs below the banner or explicit game dialogs/buttons.
        candidates=[n for n in root.iter('node') if n.get('text')==label and n.get('package')==PKG]
        if tab: candidates=[n for n in candidates if bounds(n)[1]>=550]
        if candidates:
            x1,y1,x2,y2=bounds(candidates[0]); sh(f'input tap {(x1+x2)//2} {(y1+y2)//2}'); return
        if any(n.get('text')=='다시 만나 반가워요' for n in root.iter('node')):
            for n in root.iter('node'):
                if n.get('text')=='확인':
                    x1,y1,x2,y2=bounds(n);sh(f'input tap {(x1+x2)//2} {(y1+y2)//2}')
        time.sleep(.2)
    raise AssertionError('Missing game control: '+label)
def capture(name, root=None):
    root=root if root is not None else tree()
    (OUT/(name+'.xml')).write_bytes(ET.tostring(root,encoding='utf-8'))
    sh('screencap -p /sdcard/zerostar-ads.png');adb('pull','/sdcard/zerostar-ads.png',str(OUT/(name+'.png')))
    return root
def saved(): return json.loads(sh(f'run-as {PKG} cat files/datastore/game.json'))
def network(up):
    # Android uses policy routing; the main routing table alone cannot prove
    # disconnection. Block both simulated radios and both IP families.
    for tool in ['iptables','ip6tables']:
        for interface in ['wlan0','radio0']:
            rule=f'-o {interface} -m comment --comment zerostar-test-offline -j REJECT'
            sh(f'if {tool} -C OUTPUT {rule} 2>/dev/null; then {tool} -D OUTPUT {rule}; fi')
            if not up: sh(f'{tool} -I OUTPUT 1 {rule}')
    sh(f'settings put global airplane_mode_on {0 if up else 1}')
    sh(f'am broadcast -a android.intent.action.AIRPLANE_MODE --ez state {"false" if up else "true"}')
    if up:
        sh('svc data enable');sh('svc wifi enable')
    routes=sh('ip route show table all')
    (OUT/('routes-online.txt' if up else 'routes-offline.txt')).write_text(routes,encoding='utf-8')
    firewall=sh('iptables -nvL OUTPUT')+'\n'+sh('ip6tables -nvL OUTPUT')
    assert ('zerostar-test-offline' in firewall) != up
    (OUT/('firewall-online.txt' if up else 'firewall-offline.txt')).write_text(firewall,encoding='utf-8')

if len(sys.argv)==3 and sys.argv[1]=='--network-only':
    assert sys.argv[2] in ['online','offline']
    network(sys.argv[2]=='online')
    print('Disposable AVD network '+sys.argv[2],flush=True)
    raise SystemExit(0)
def wait_banner(name):
    for _ in range(15):
        root=tree()
        if webs(root):
            assert len(webs(root))==1
            capture(name,root);return root
        # A return notice intentionally hides ads until acknowledged.
        if any(n.get('text')=='다시 만나 반가워요' for n in root.iter('node')): tap_game('확인')
        time.sleep(1)
    capture(name+'-unavailable',root)
    raise AssertionError('No visible test banner within bounded wait')

rows=[]
try:
    stop(); sh('wm size 360x640');sh('wm density 160')
    sh('settings put system accelerometer_rotation 0');sh('settings put system user_rotation 0')
    sh('settings put system font_scale 1.0')
    # Dedicated fixture, no real user progress. Keep UMP state to test update failure.
    fixture=dict(schemaVersion=1,balanceMilli=200000,earnedMilli=200000,upgradeLevel=0,collectedCount=0,
                 lastSettledEpochMs=int(sh('date +%s').strip())*1000,soundEnabled=False,reduceMotion=True,tutorialSeen=True)
    sh(f'run-as {PKG} mkdir -p files/datastore')
    adb('shell',f"run-as {PKG} sh -c 'cat > files/datastore/game.json'",data=json.dumps(fixture).encode())
    network(False); start(); tap_game('빛 모으기 · 20 별빛')
    for _ in range(20):
        if saved()['upgradeLevel']==1:break
        time.sleep(.1)
    assert saved()['upgradeLevel']==1
    root=capture('offline-purchase');assert len(webs(root))==0
    print('offline launch: game purchase succeeds, banner hidden PASS',flush=True)
    network(True);stop();start();wait_banner('network-restored')
    assert saved()['upgradeLevel']==1
    print('network restored + Activity restart: saved purchase and one test banner PASS',flush=True)
    (OUT/'memory-before.txt').write_text(sh(f'dumpsys meminfo {PKG}'),encoding='utf-8')
    for i in range(20):
        tap_game('설정',tab=True); root=tree(); assert len(webs(root))==0
        tap_game('홈',tab=True); root=tree(); assert len(webs(root))<=1
        rows.append(dict(cycle=i+1,visibleBannerViews=len(webs(root)),state=saved()))
        (OUT/'cycles.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
        print(f'tab disposal {i+1}/20: settings0 / home<=1 banner PASS',flush=True)
    (OUT/'memory-after.txt').write_text(sh(f'dumpsys meminfo {PKG}'),encoding='utf-8')
    for rotation in [1,0,1,0]:
        before=sh('dumpsys activity activities')
        sh(f'settings put system user_rotation {rotation}')
        time.sleep(2)
        root=tree();assert len(webs(root))<=1
        assert any(n.get('text')=='홈' for n in root.iter('node'))
        assert saved()['upgradeLevel']==1
        (OUT/f'rotation-{rotation}-activity.txt').write_text(sh('dumpsys activity activities'),encoding='utf-8')
        capture(f'rotation-{rotation}',root)
        print(f'rotation {rotation}: game and at most one banner PASS',flush=True)
finally:
    sh('settings put system user_rotation 0');sh('settings put system accelerometer_rotation 1')
    network(True);stop()
