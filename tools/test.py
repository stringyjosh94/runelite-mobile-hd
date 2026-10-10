#!/usr/bin/env python3
"""Offline desktop regression tests. Does not establish Android compatibility or phone FPS."""
from pathlib import Path
import os,subprocess,json,re
ROOT=Path(__file__).resolve().parents[1];BUILD=ROOT/'build/tests';BUILD.mkdir(parents=True,exist_ok=True)
JDK=Path(os.environ.get('JAVA_HOME','/usr/lib/jvm/java-17-openjdk'))
# Cross-language contract: Android gestures must reach our host handler through JNI.
emitted=set(re.findall(r'NativeBridge.input\((\d+)',(ROOT/'app/src/android/dev/mobile/runelite/MainActivity.java').read_text()))
bridge=(ROOT/'app/src/native/bridge.c').read_text()
route=next(line for line in bridge.splitlines() if 'jclass input =' in line)
routed=set(re.findall(r'type==(\d+)',route))
assert emitted <= routed, 'Android gestures missing JNI routing: '+str(emitted-routed)
print('ANDROID_JNI_GESTURE_CONTRACT_OK',flush=True)
# Android EGL lifecycle with deterministic window/EGL doubles (no driver claim).
lifecycle=BUILD/'surface-lifecycle'
subprocess.run(['cc','-D__ANDROID__','-I'+str(ROOT/'tests/native/include'),'-I'+str(JDK/'include'),'-I'+str(JDK/'include/linux'),str(ROOT/'tests/native/surface_lifecycle.c'),'-ldl','-o',str(lifecycle)],check=True)
subprocess.run([str(lifecycle)],check=True,timeout=10)

cp=str(ROOT/'app/assets/client/*')
sources=list((ROOT/'tests/java').rglob('*.java'))+[ROOT/'app/src/android/dev/mobile/runelite/StoredLogin.java',ROOT/'app/src/android/dev/mobile/runelite/ViewportSizing.java']
subprocess.run([str(JDK/'bin/javac'),'-cp',cp,'-d',str(BUILD),*map(str,sources)],check=True)
options=json.loads((ROOT/'app/assets/jvm-options.json').read_text())
options=[o for o in options if not o.startswith('-Dcacio.managed.screensize=')]
options+=['-Duser.home='+str(BUILD/'home'),'-Dcacio.managed.screensize=1400x540','-Dswing.volatileImageBufferEnabled=false','-Dsun.java2d.opengl=false','-Dsun.java2d.xrender=false',
    '-javaagent:'+str(ROOT/'app/assets/cacio/cacio-agent.jar'),'-Xbootclasspath/a:'+os.pathsep.join(map(str,sorted((ROOT/'app/assets/cacio').glob('*.jar')))),
    '-cp',str(BUILD)+os.pathsep+cp]
env=dict(os.environ,LD_LIBRARY_PATH='/usr/lib'+os.pathsep+os.environ.get('LD_LIBRARY_PATH',''))
for test in ['dev.mobile.runelite.DirectSurfaceCompositionSmoke','dev.mobile.runelite.GpuFramePoolSmoke','dev.mobile.runelite.SidebarTargetSmoke','dev.mobile.runelite.StretchSizingSmoke','ViewportSmoke','dev.mobile.runelite.SidebarScrollSmoke','HdCompatibilitySmoke','HdProfilesSmoke','StoredLoginSmoke','dev.mobile.runelite.CompositionSmoke','dev.mobile.runelite.RealCameraWheelSmoke','MenuPointerSmoke','KeyboardFocusSmoke','PointerInputSmoke','dev.mobile.runelite.WidgetHitSmoke']:
    print('Checking',test,flush=True)
    subprocess.run([str(JDK/'bin/java'),*options,test],env=env,check=True,timeout=30)
print('All desktop regression tests passed. Phone testing remains necessary.')
