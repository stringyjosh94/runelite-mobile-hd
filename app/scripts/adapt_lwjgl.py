#!/usr/bin/env python3
"""Adapt the FCL core Library class to load our platform bridge instead of Pojav.
Constant-pool replacement preserves bytecode and frame offsets. Input is the
unmodified FCL lwjgl.jar; output keeps all other entries intact.
"""
import struct
import sys
import zipfile

def adapt(data):
    assert data[:4] == b'\xca\xfe\xba\xbe'
    count = struct.unpack_from('>H', data, 8)[0]
    out = bytearray(data[:10]); pos = 10; index = 1; replaced = 0
    sizes = {3:4, 4:4, 5:8, 6:8, 7:2, 8:2, 9:4, 10:4, 11:4, 12:4, 15:3, 16:2, 17:4, 18:4, 19:2, 20:2}
    while index < count:
        tag = data[pos]; pos += 1; out.append(tag)
        if tag == 1:
            length = struct.unpack_from('>H', data, pos)[0]; pos += 2
            value = data[pos:pos+length]; pos += length
            if value == b'pojavexec': value = b'rlawt_android'; replaced += 1
            out += struct.pack('>H', len(value)) + value
        else:
            size = sizes[tag]; out += data[pos:pos+size]; pos += size
            if tag in (5,6): index += 1
        index += 1
    assert replaced == 1, replaced
    return bytes(out) + data[pos:]

def main():
    with zipfile.ZipFile(sys.argv[1]) as source, zipfile.ZipFile(sys.argv[2], 'w', zipfile.ZIP_DEFLATED) as target:
        for info in source.infolist():
            data = source.read(info)
            if info.filename == 'org/lwjgl/system/Library.class': data = adapt(data)
            target.writestr(info, data)
if __name__ == '__main__': main()
