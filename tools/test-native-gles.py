#!/usr/bin/env python3
"""Linux EGL test of Android presentation C source; requires desktop EGL headers and Linux LWJGL natives."""
from pathlib import Path
import argparse,os,subprocess,json
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--lwjgl-native-dir',type=Path,required=True);p.add_argument('--shader-native',type=Path,help='Optional matching Linux librlhd_shader.so for scene shader tests');a=p.parse_args()
native=a.lwjgl_native_dir.resolve();jdk=Path(os.environ.get('JAVA_HOME','/usr/lib/jvm/java-17-openjdk'))
build=ROOT/'build/tests-native';build.mkdir(parents=True,exist_ok=True)
lib=build/'librlawt_gles_test.so'
subprocess.run(['cc','-shared','-fPIC','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),str(ROOT/'app/src/native/gpu.c'),'-o',str(lib),'-lEGL','-ldl','-pthread'],check=True)
alias=build/'librlawt_android.so'
if alias.is_symlink() or alias.exists():alias.unlink()
alias.symlink_to(lib.name)
cp=str(ROOT/'app/assets/client/*')
subprocess.run([str(jdk/'bin/javac'),'-cp',cp,'-d',str(build),str(ROOT/'tests/java/dev/mobile/runelite/NativeGlesPresentationSmoke.java')],check=True)
options=json.loads((ROOT/'app/assets/jvm-options.json').read_text())
options += ['-Duser.home='+str(build/'home'),'-Drunelite.android.renderer=gles','-Drunelite.rlawtpath='+str(lib),'-Dorg.lwjgl.opengles.explicitInit=true','-Dorg.lwjgl.librarypath='+str(native),'-Djava.library.path='+str(build),
 '-Dswing.volatileImageBufferEnabled=false','-Dsun.java2d.opengl=false','-Dsun.java2d.xrender=false',
 '-javaagent:'+str(ROOT/'app/assets/cacio/cacio-agent.jar'),'-Xbootclasspath/a:'+os.pathsep.join(map(str,sorted((ROOT/'app/assets/cacio').glob('*.jar')))),
 '-cp',str(build)+os.pathsep+cp,'dev.mobile.runelite.NativeGlesPresentationSmoke']
for mode in ['async','sync']:
 print('Testing actual native GLES presentation:',mode,flush=True)
 env=dict(os.environ,EGL_PLATFORM='surfaceless',RUNELITE_RENDERER='gles',RUNELITE_GLES_READBACK=mode,LD_LIBRARY_PATH='/usr/lib'+os.pathsep+str(native))
 subprocess.run([str(jdk/'bin/java'),*options],env=env,check=True,timeout=60)

if a.shader_native:
 alias=build/'librlhd_shader.so'
 if alias.is_symlink() or alias.exists():alias.unlink()
 alias.symlink_to(a.shader_native.resolve())
 subprocess.run([str(jdk/'bin/javac'),'-cp',cp,'-d',str(build),str(ROOT/'tests/java/dev/mobile/runelite/EfficientShaderSmoke.java')],check=True)
 scene_options=options[:-1]+['-Dmobile.shader.resources='+str(ROOT/'hd-plugin/src/main/resources/dev/mobile/rlhd'),'dev.mobile.runelite.EfficientShaderSmoke']
 subprocess.run([str(jdk/'bin/java'),*scene_options],env=env,check=True,timeout=120)
