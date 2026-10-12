#!/usr/bin/env python3
"""Export Git-visible source, excluding ignored local dependencies and outputs."""
import argparse,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def selected_files():
    probe=subprocess.run(['git','rev-parse','--show-toplevel'],cwd=ROOT,text=True,capture_output=True)
    if probe.returncode or Path(probe.stdout.strip()).resolve()!=ROOT:
        raise ValueError('Initialise Git in this source folder first: git init -b main')
    names=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode().split('\0')
    result=[]
    for name in sorted(set(filter(None,names))):
        p=ROOT/name
        if not p.is_file():continue
        leaf=p.name.lower()
        if leaf.startswith('credentials.properties') or leaf.endswith(('.keystore','.jks','.apk','.idsig','.log')) or leaf=='.env' or '.runelite' in p.parts:
            raise ValueError('Private/generated file visible to Git: '+name)
        if name.startswith(('app/assets/','app/lib/','hd-plugin/tools/','build/','dist/')):
            raise ValueError('Build dependency/output visible to Git: '+name)
        if p.stat().st_size>=95*1024*1024:raise ValueError('Oversized source file: '+name)
        result.append((name,p))
    return result
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,default=ROOT/'dist/runelite-mobile-hd-source-v36.4.zip')
    parser.add_argument('--check',action='store_true',help='Check the export list without writing an archive')
    a=parser.parse_args();files=selected_files()
    print(f'Checked {len(files)} source files; {sum(p.stat().st_size for _,p in files)/1024/1024:.1f} MiB uncompressed.')
    print('This checks filenames/sizes, not every possible secret in file contents. Review git diff before pushing.')
    if a.check:return
    a.output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(a.output,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for name,p in files:z.write(p,'runelite-mobile-hd/'+name)
    print('Source archive:',a.output.resolve())
if __name__=='__main__':main()
