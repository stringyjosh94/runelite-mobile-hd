#!/usr/bin/env python3
"""Compile the isolated GLES fork against the APK's pinned RuneLite/LWJGL jars."""
from pathlib import Path
import os, subprocess, argparse, shutil
parser=argparse.ArgumentParser()
parser.add_argument("--native",action="store_true")
parser.add_argument("--native-build-dir")
args=parser.parse_args()
ROOT=Path(__file__).resolve().parents[1]
APP=ROOT.parent/'app'
BUILD=ROOT/'build/classes'; BUILD.mkdir(parents=True,exist_ok=True)
JDK=Path(os.environ.get('JAVA_HOME','/usr/lib/jvm/java-17-openjdk'))
LOMBOK=ROOT/'tools/lombok-1.18.30.jar'
classpath=str(APP/'assets/client/*')+os.pathsep+str(LOMBOK)
sources=sorted((ROOT/'src/main/java').rglob('*.java'))+sorted((ROOT/'compile-support').rglob('*.java'))
subprocess.run([str(JDK/'bin/javac'),'--release','17','-cp',classpath,'-processorpath',str(LOMBOK),'-d',str(BUILD),*map(str,sources)],check=True)
subprocess.run([str(JDK/'bin/jar'),'cf',str(APP/'assets/client/117hd-gles-port.jar'),'-C',str(BUILD),'dev/mobile/rlhd','-C',str(ROOT/'src/main/resources'),'.'],check=True)
print('117HD GLES fork compiled; rebuild the APK with the Android build script.')

if args.native:
    sdk=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'Android/Sdk')))
    ndk=Path(os.environ.get('ANDROID_NDK_HOME',str(sdk/'ndk/25.2.9519653')))
    native_build=Path(args.native_build_dir).resolve() if args.native_build_dir else ROOT/'build/native'
    env=dict(os.environ,JAVA_HOME=str(JDK))
    subprocess.run(['cmake','-S',str(ROOT/'native'),'-B',str(native_build),'-G','Ninja','-DCMAKE_BUILD_TYPE=Release',
        '-DCMAKE_TOOLCHAIN_FILE='+str(ndk/'build/cmake/android.toolchain.cmake'),'-DANDROID_ABI=arm64-v8a',
        '-DANDROID_PLATFORM=android-26','-DANDROID_STL=c++_static'],check=True,env=env)
    subprocess.run(['cmake','--build',str(native_build),'-j','2'],check=True,env=env)
    shutil.copy2(native_build/'librlhd_shader.so',APP/'lib/arm64-v8a/librlhd_shader.so')
    strip=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip'
    subprocess.run([str(strip),'--strip-unneeded',str(APP/'lib/arm64-v8a/librlhd_shader.so')],check=True)
