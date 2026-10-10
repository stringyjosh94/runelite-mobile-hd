#!/usr/bin/env python3
"""Maintainer tool: snapshot existing pinned dependencies, not private app data."""
import argparse,hashlib,json,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,default=ROOT/'dist/runelite-mobile-hd-runtime-v35.zip');a=p.parse_args()
    files={};generated={'payload-id.txt','android-host.jar','117hd-gles-port.jar','libpojavexec_awt.so','liblwjgl_opengles.so','librlawt_android.so','libawt_xawt.so','libOSMesa_81.so','libvirgl_test_server.so'}
    for directory in ('app/assets','app/lib','app/licenses','hd-plugin/tools'):
        for f in sorted((ROOT/directory).rglob('*')):
            if not f.is_file() or f.name in generated:continue
            name=f.relative_to(ROOT).as_posix()
            if directory=='app/lib' and f.parent.name!='arm64-v8a':continue
            if directory=='hd-plugin/tools' and f.name!='lombok-1.18.30.jar':continue
            if f.name.startswith('credentials.properties') or f.suffix in ('.keystore','.jks','.log'):raise ValueError('Private file: '+name)
            files[name]=f
    for required in ('app/assets/jre/lib/modules','app/assets/client/injected-client-1.13.1.jar','app/lib/arm64-v8a/librlhd_shader.so','hd-plugin/tools/lombok-1.18.30.jar'):
        if required not in files:raise ValueError('Restore/build dependencies before packaging: '+required)
    manifest={'version':35,'description':'Pinned build dependencies for v35; generated bridge/HD outputs are rebuilt from source. No credentials or signing keys.','files':{n:{'size':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for n,f in files.items()}}
    a.output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(a.output,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for n,f in files.items():z.write(f,n)
    (ROOT/'runtime-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    digest=hashlib.sha256(a.output.read_bytes()).hexdigest()
    (ROOT/'runtime-pack.sha256').write_text(digest+'  '+a.output.name+'\n')
    print('Pack:',a.output.resolve());print('SHA256:',digest)
if __name__=='__main__':main()
