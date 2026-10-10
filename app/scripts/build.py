#!/usr/bin/env python3
"""Build without Gradle; requires Android SDK 34, NDK 25+ and a host JDK 17."""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def run(*args, env=None):
    subprocess.run([str(a) for a in args], check=True, env=env)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--sdk', default=os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'Android/Sdk')))
    parser.add_argument('--jdk', default=os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-17-openjdk'))
    parser.add_argument('--build-dir')
    parser.add_argument('--ndk', default='25.2.9519653')
    parser.add_argument('--abi', choices=['arm64-v8a', 'x86_64'], default='arm64-v8a')
    parser.add_argument('--output', default=str(ROOT.parent / 'runelite-android-example.apk'))
    args = parser.parse_args()
    sdk, jdk = Path(args.sdk), Path(args.jdk)
    tools = sdk / 'build-tools/34.0.0'
    android = sdk / 'platforms/android-34/android.jar'
    ndk = sdk / 'ndk' / args.ndk
    arch = 'aarch64' if args.abi == 'arm64-v8a' else 'x86_64'
    clang = ndk / ('toolchains/llvm/prebuilt/linux-x86_64/bin/'+arch+'-linux-android26-clang')
    native = ROOT/'lib'/args.abi
    native.mkdir(parents=True, exist_ok=True)
    build = Path(args.build_dir) if args.build_dir else Path(tempfile.mkdtemp(prefix='runelite-android-build-'))
    build.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ, JAVA_HOME=str(jdk))
    env['PATH'] = str(jdk / 'bin') + os.pathsep + env['PATH']
    for name in ['android-classes', 'host-classes', 'dex']:
        shutil.rmtree(build / name, ignore_errors=True)
        (build / name).mkdir(exist_ok=True)
    run(jdk/'bin/javac', '--release', '17', '-cp', str(ROOT/'assets/client')+'/*', '-d', build/'host-classes',
        *sorted((ROOT/'src/host').rglob('*.java')))
    run(jdk/'bin/jar', 'cf', ROOT/'assets/client/android-host.jar', '-C', build/'host-classes', '.')
    run(clang, '-shared', '-fPIC', '-O2', '-Wall', '-Wextra', '-Wno-unused-parameter',
        '-Wl,-z,max-page-size=16384', '-I'+str(jdk/'include'), '-I'+str(jdk/'include/linux'),
        ROOT/'src/native/bridge.c', '-llog', '-ldl', '-landroid', '-o', native/'libpojavexec_awt.so')
    run(clang, '-shared', '-fPIC', '-O2', '-DLWJGL_LINUX', '-Wl,-z,max-page-size=16384',
        '-I'+str(jdk/'include'), '-I'+str(jdk/'include/linux'), '-I'+str(ROOT/'src/native/lwjgl-gles'),
        *sorted((ROOT/'src/native/lwjgl-gles').glob('*.c')), '-o', native/'liblwjgl_opengles.so')
    run(clang, '-shared', '-fPIC', '-O2', '-Wall', '-Wextra', '-Wno-unused-parameter',
        '-Wl,-z,max-page-size=16384', '-I'+str(jdk/'include'), '-I'+str(jdk/'include/linux'),
        ROOT/'src/native/gpu.c', '-ldl', '-llog', '-lEGL', '-landroid', '-pthread', '-o', native/'librlawt_android.so')
    # Build the no-X11 AWT JNI IDs with the same 16 KiB alignment.
    run(clang, '-shared', '-fPIC', '-O2', '-Wl,-z,max-page-size=16384',
        '-I'+str(jdk/'include'), '-I'+str(jdk/'include/linux'), ROOT/'src/native/xawt_fake.c',
        '-L'+str(native), '-Wl,--no-as-needed', '-lawt_headless', '-o', native/'libawt_xawt.so')
    run(jdk/'bin/javac', '-source', '8', '-target', '8', '-bootclasspath', str(android)+os.pathsep+str(tools/'core-lambda-stubs.jar'),
        '-d', build/'android-classes', ROOT/'src/host/dev/mobile/runelite/MobileHdProfiles.java', *sorted((ROOT/'src/android').rglob('*.java')))
    run(tools/'d8', '--min-api', '26', '--lib', android, '--output', build/'dex',
        *sorted((build/'android-classes').rglob('*.class')), env=env)
    payload = hashlib.sha256()
    for directory in [ROOT/'assets/client', ROOT/'assets/cacio', ROOT/'assets/jre', native]:
        for file in sorted(directory.rglob('*')):
            if file.is_file():
                payload.update(str(file.relative_to(ROOT)).encode())
                payload.update(file.read_bytes())
    payload.update((ROOT/'assets/jvm-options.json').read_bytes())
    (ROOT/'assets/payload-id.txt').write_text(payload.hexdigest())
    unsigned = build/'unsigned.apk'
    resources = build/'resources.zip'
    run(tools/'aapt2', 'compile', '--dir', ROOT/'res', '-o', resources)
    run(tools/'aapt2', 'link', '-I', android, '--manifest', ROOT/'AndroidManifest.xml', resources, '-o', unsigned)
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(build/'dex/classes.dex', 'classes.dex')
        for directory in ['assets', 'lib', 'licenses']:
            for file in sorted((ROOT/directory).rglob('*')):
                if file.is_file():
                    if directory == 'lib' and file.parent.name != args.abi: continue
                    if file.name in ('libOSMesa_81.so', 'libvirgl_test_server.so'): continue
                    name = str(file.relative_to(ROOT))
                    if directory == 'licenses': name = 'assets/licenses/' + str(file.relative_to(ROOT/'licenses'))
                    archive.write(file, name)
    aligned = build/'aligned.apk'
    run(tools/'zipalign', '-f', '-p', '4', unsigned, aligned)
    key = build/'debug.keystore'
    if not key.exists():
        run(jdk/'bin/keytool', '-genkeypair', '-keystore', key, '-storepass', 'android',
            '-keypass', 'android', '-alias', 'androiddebugkey', '-dname', 'CN=Android Example Debug',
            '-keyalg', 'RSA', '-keysize', '2048', '-validity', '3650')
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    run(tools/'apksigner', 'sign', '--ks', key, '--ks-pass', 'pass:android', '--key-pass',
        'pass:android', '--out', output, aligned, env=env)
    run(tools/'apksigner', 'verify', '--verbose', output, env=env)
    run(tools/'zipalign', '-c', '-v', '4', output)
    print('APK:', output.resolve())
    print('SHA256:', hashlib.sha256(output.read_bytes()).hexdigest())

if __name__ == '__main__':
    main()
