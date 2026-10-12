#!/usr/bin/env python3
"""Offline ABI/page-size checks with deliberately mismatched dependency fixtures."""
import json, struct, sys, tempfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'app/scripts'))
from compatibility import ABIS, GENERATED, elf_info, validate

def elf(bits,machine,alignment=16384):
    d=bytearray(256);d[:6]=b'\x7fELF'+bytes([bits,1]);struct.pack_into('<H',d,18,machine)
    if bits==2:
        struct.pack_into('<Q',d,32,64);struct.pack_into('<HH',d,54,56,1)
        struct.pack_into('<IIQQQQQQ',d,64,1,5,0,0,0,256,256,alignment)
    else:
        struct.pack_into('<I',d,28,52);struct.pack_into('<HH',d,42,32,1)
        struct.pack_into('<IIIIIIII',d,52,1,0,0,0,256,256,5,alignment)
    return d

def rejects(action,substring):
    try: action()
    except ValueError as e: assert substring in str(e),str(e)
    else: raise AssertionError('Invalid runtime accepted: '+substring)

with tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp);jre=root/'jre';native=root/'native';(jre/'lib').mkdir(parents=True);native.mkdir()
    (jre/'lib/modules').write_bytes(b'fixture');(jre/'native-layout.json').write_text(json.dumps({'lib/libjava.so':'libjava.so'}))
    required=GENERATED|{'libjvm.so','libjava.so','libjimage.so','libawt_headless.so','liblwjgl.so','liblwjgl_opengl.so','libjnidispatch.so','librlhd_shader.so'}
    for abi,(arch,bits,machine,_) in ABIS.items():
        (jre/'release').write_text('OS_ARCH="'+arch+'"\nJAVA_VERSION="17.0.10"\n')
        for name in required:(native/name).write_bytes(elf(bits,machine))
        assert validate(jre,native,abi,strict_pages=True)==[]
        (native/'libjvm.so').write_bytes(elf(bits,machine,4096))
        assert validate(jre,native,abi)==['libjvm.so']
        rejects(lambda:validate(jre,native,abi,strict_pages=True),'Not 16 KB')
        (native/'libjvm.so').write_bytes(elf(bits,0))
        rejects(lambda:validate(jre,native,abi),'Wrong CPU')
        (native/'libjvm.so').unlink()
        rejects(lambda:validate(jre,native,abi),'Missing')
    (jre/'release').write_text('OS_ARCH="aarch64"\nJAVA_VERSION="17.0.10"')
    rejects(lambda:validate(jre,native,'x86_64'),'OS_ARCH')
    (native/'bad.so').write_bytes(b'invalid')
    rejects(lambda:elf_info(native/'bad.so'),'ELF')
print('ABI_AND_PAGE_SIZE_VALIDATION_OK')
