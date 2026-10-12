"""Validate native dependencies before packaging; never relabel an ARM runtime."""
import json
import struct
from pathlib import Path

ABIS = {
    'arm64-v8a': ('aarch64', 2, 183, 'aarch64-linux-android'),
    'armeabi-v7a': ('arm', 1, 40, 'armv7a-linux-androideabi'),
    'x86_64': ('x86_64', 2, 62, 'x86_64-linux-android'),
    'x86': ('i386', 1, 3, 'i686-linux-android'),
}
GENERATED = {'libpojavexec_awt.so', 'liblwjgl_opengles.so', 'librlawt_android.so', 'libawt_xawt.so'}
EXCLUDED = {'libOSMesa_81.so', 'libvirgl_test_server.so'}

def elf_info(path):
    data = Path(path).read_bytes()
    if data[:4] != b'\x7fELF' or len(data) < 52 or data[5] != 1:
        raise ValueError('Not a supported little-endian ELF library: ' + str(path))
    bits, machine = data[4], struct.unpack_from('<H', data, 18)[0]
    if bits == 2:
        phoff = struct.unpack_from('<Q', data, 32)[0]
        size, count = struct.unpack_from('<HH', data, 54)
        fmt = '<IIQQQQQQ'
    elif bits == 1:
        phoff = struct.unpack_from('<I', data, 28)[0]
        size, count = struct.unpack_from('<HH', data, 42)
        fmt = '<IIIIIIII'
    else:
        raise ValueError('Unknown ELF class: ' + str(path))
    loads = []
    if size < struct.calcsize(fmt) or phoff + size * count > len(data):
        raise ValueError('Invalid ELF program headers: ' + str(path))
    for i in range(count):
        h = struct.unpack_from(fmt, data, phoff + i * size)
        if h[0] == 1:
            offset, address, alignment = (h[2], h[3], h[7]) if bits == 2 else (h[1], h[2], h[7])
            loads.append((offset, address, alignment))
    if not loads:
        raise ValueError('ELF has no load segments: ' + str(path))
    return bits, machine, all(a >= 16384 and (o-v) % 16384 == 0 for o,v,a in loads)

def validate(jre, native, abi, before_build=False, strict_pages=False):
    arch, bits, machine, _ = ABIS[abi]
    jre, native = Path(jre), Path(native)
    if not (jre/'lib/modules').is_file():
        raise ValueError('Missing Java modules for ' + abi + '; supply a complete matching runtime.')
    release = (jre/'release').read_text()
    expected = {'arm64-v8a': ('aarch64',), 'armeabi-v7a': ('arm', 'armv7l'),
                'x86_64': ('x86_64', 'amd64'), 'x86': ('i386', 'i486', 'i586', 'i686', 'x86')}[abi]
    if not any('OS_ARCH="'+a+'"' in release for a in expected):
        raise ValueError('Java runtime OS_ARCH does not match '+abi+'. ARM64 modules cannot be reused for this ABI.')
    if 'JAVA_VERSION="17.' not in release:
        raise ValueError('This client requires a matching Java 17 runtime.')
    layout = json.loads((jre/'native-layout.json').read_text())
    required = set(layout.values()) | {'libjvm.so','libjava.so','libjimage.so','libawt_headless.so',
        'liblwjgl.so','liblwjgl_opengl.so','libjnidispatch.so','librlhd_shader.so'} | GENERATED
    if before_build:
        required -= GENERATED
    missing = sorted(name for name in required if not (native/name).is_file())
    if missing:
        raise ValueError('Missing '+abi+' dependencies: '+', '.join(missing))
    unaligned = []
    for path in sorted(native.glob('*.so')):
        if path.name in EXCLUDED or (before_build and path.name in GENERATED):
            continue
        found_bits, found_machine, pages = elf_info(path)
        if (found_bits, found_machine) != (bits, machine):
            raise ValueError('Wrong CPU architecture: '+str(path))
        if not pages:
            unaligned.append(path.name)
    if unaligned and strict_pages:
        raise ValueError('Not 16 KB ELF aligned: '+', '.join(unaligned))
    return unaligned
