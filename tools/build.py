#!/usr/bin/env python3
"""Build the Android APK, optionally restoring the pinned offline dependencies."""
import argparse,hashlib,json,os,subprocess,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'app/scripts'))
from compatibility import ABIS, validate

def restore(pack):
    expected=(ROOT/'runtime-pack.sha256').read_text().split()[0]
    if hashlib.sha256(pack.read_bytes()).hexdigest()!=expected:
        raise ValueError('Wrong runtime pack or damaged download; use runtime-v35.zip from this project.')
    manifest=json.loads((ROOT/'runtime-manifest.json').read_text())['files']
    with zipfile.ZipFile(pack) as archive:
        if set(archive.namelist())!=set(manifest):raise ValueError('Runtime archive contents do not match the manifest')
        for name,entry in manifest.items():
            data=archive.read(name)
            if len(data)!=entry['size'] or hashlib.sha256(data).hexdigest()!=entry['sha256']:
                raise ValueError('Runtime integrity check failed: '+name)
            target=(ROOT/name).resolve()
            if not target.is_relative_to(ROOT):raise ValueError('Invalid archive path')
            target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
    print('Verified and restored pinned dependencies.',flush=True)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--dependencies',type=Path,help='Path to runelite-mobile-hd-runtime-v35.zip')
    p.add_argument('--sdk',type=Path,default=Path(os.environ.get('ANDROID_SDK_ROOT',str(Path.home()/'Android/Sdk'))))
    p.add_argument('--jdk',type=Path,default=Path(os.environ.get('JAVA_HOME','/usr/lib/jvm/java-17-openjdk')))
    p.add_argument('--ndk',default='25.2.9519653')
    p.add_argument('--abi',choices=list(ABIS),default='arm64-v8a')
    p.add_argument('--runtime-root',type=Path,help='Complete ABI runtime: jre/ and lib/<abi>/')
    p.add_argument('--require-16k',action='store_true')
    p.add_argument('--output',type=Path,help='APK output path')
    p.add_argument('--doctor',action='store_true',help='Only check required tools and runtime files')
    p.add_argument('--rebuild-native',action='store_true',help='Also rebuild glslang/SPIRV-Cross (requires CMake and Ninja)')
    args=p.parse_args()
    if args.dependencies:restore(args.dependencies.resolve())
    if args.runtime_root:args.runtime_root=args.runtime_root.resolve()
    jre=args.runtime_root/'jre' if args.runtime_root else ROOT/'app/assets/jre'
    native=args.runtime_root/'lib'/args.abi if args.runtime_root else ROOT/'app/lib'/args.abi
    unaligned=validate(jre,native,args.abi,before_build=True,strict_pages=args.require_16k)
    print('Runtime ABI verified:',args.abi,flush=True)
    if unaligned:print('16 KB support incomplete; incompatible libraries: '+', '.join(unaligned),flush=True)
    ndk=args.sdk/'ndk'/args.ndk
    needed=[args.jdk/'bin/javac',args.jdk/'bin/java',args.sdk/'build-tools/34.0.0/aapt2',args.sdk/'build-tools/34.0.0/d8',
        args.sdk/'platforms/android-34/android.jar',ndk/('toolchains/llvm/prebuilt/linux-x86_64/bin/'+ABIS[args.abi][3]+'26-clang'),
        jre/'lib/modules',ROOT/'app/assets/client/injected-client-1.13.1.jar',ROOT/'hd-plugin/tools/lombok-1.18.30.jar']
    missing=[str(path) for path in needed if not path.is_file()]
    if missing:
        print('Missing requirements:\n'+'\n'.join(missing)+'\nRead docs/BUILDING.md.',file=sys.stderr);return 1
    print('Tool and dependency checks passed.',flush=True)
    if args.doctor:return 0
    env=dict(os.environ,JAVA_HOME=str(args.jdk),ANDROID_HOME=str(args.sdk),ANDROID_SDK_ROOT=str(args.sdk),ANDROID_NDK_HOME=str(ndk))
    command=[sys.executable,str(ROOT/'hd-plugin/scripts/build.py'),'--abi',args.abi]
    if args.runtime_root:command+=['--runtime-root',str(args.runtime_root)]
    if args.rebuild_native:command+=['--native','--native-build-dir',str(ROOT/'build'/('shader-translator-'+args.abi))]
    subprocess.run(command,env=env,check=True)
    app_command=[sys.executable,str(ROOT/'app/scripts/build.py'),'--jdk',str(args.jdk),'--sdk',str(args.sdk),
        '--ndk',args.ndk,'--abi',args.abi,'--build-dir',str(ROOT/'build'/('android' if args.abi=='arm64-v8a' else 'android-'+args.abi)),
        '--output',str(args.output or ROOT/'dist'/('runelite-mobile-hd-v36.4-touch-'+args.abi+'.apk'))]
    if args.runtime_root:app_command+=['--runtime-root',str(args.runtime_root)]
    if args.require_16k:app_command+=['--require-16k']
    subprocess.run(app_command,env=env,check=True)
    return 0
if __name__=='__main__':
    try:sys.exit(main())
    except (ValueError,OSError,subprocess.CalledProcessError) as error:sys.exit(str(error))
