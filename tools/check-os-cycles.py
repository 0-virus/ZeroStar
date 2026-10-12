"""Exercise OS interruption on a disposable emulator debug installation only.
Run after a real UI purchase/registration. Does not inject game state.
"""
import argparse, json, subprocess, time
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--serial', required=True)
p.add_argument('--package', default='com.example.zerostar.debug')
p.add_argument('--mode', choices=['force-stop','background'], required=True)
p.add_argument('--count', type=int, default=20)
p.add_argument('--output', required=True)
a=p.parse_args()
assert a.serial.startswith('emulator-') and a.package.endswith('.debug'), 'Use a disposable debug emulator'
adb=Path('.tools/android-sdk/platform-tools/adb.exe')
def run(*args):
    return subprocess.run([str(adb),'-s',a.serial,*args],check=True,capture_output=True,text=True,encoding='utf-8',timeout=40).stdout

def saved():
    return json.loads(run('shell','run-as',a.package,'cat','files/datastore/game.json'))

def start():
    run('shell','am','start','-W','-n',a.package+'/com.zerostar.game.MainActivity')
    time.sleep(0.5)

start()
baseline=saved()
previous=baseline
rows=[]
for index in range(a.count):
    if a.mode=='force-stop': run('shell','am','force-stop',a.package)
    else: run('shell','input','keyevent','KEYCODE_HOME')
    time.sleep(0.3)
    start()
    current=saved()
    for key in ['upgradeLevel','collectedCount','soundEnabled','reduceMotion','tutorialSeen']:
        assert current[key]==baseline[key], (index,key,current,baseline)
    assert current['balanceMilli']>=previous['balanceMilli']
    assert current['earnedMilli']>=previous['earnedMilli']
    assert current['earnedMilli']-current['balanceMilli']==baseline['earnedMilli']-baseline['balanceMilli']
    rate=(1+2*previous['upgradeLevel'])*(1000+250*previous['collectedCount'])
    elapsed=max(0,current['lastSettledEpochMs']-previous['lastSettledEpochMs'])
    gain=current['earnedMilli']-previous['earnedMilli']
    assert gain>=rate*max(0,elapsed//1000-1), (index,"lost production",gain,elapsed)
    assert gain<=rate*((elapsed+999)//1000+1), (index,'duplicate production',gain,elapsed)
    rows.append({'cycle':index+1,'mode':a.mode,'state':current})
    previous=current
    print(f'{a.mode}: {index+1}/{a.count} passed',flush=True)
    Path(a.output).write_text(json.dumps({'baseline':baseline,'cycles':rows},ensure_ascii=False,indent=2),encoding='utf-8')

