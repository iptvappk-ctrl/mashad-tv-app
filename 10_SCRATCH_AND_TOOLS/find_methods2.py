import struct

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    import zipfile
    z = zipfile.ZipFile(f)
    dex = z.read('classes.dex')

string_ids_off = struct.unpack_from('<I', dex, 60)[0]
field_ids_off = struct.unpack_from('<I', dex, 84)[0]
method_ids_off = struct.unpack_from('<I', dex, 92)[0]

def get_str(str_id):
    off = struct.unpack_from('<I', dex, string_ids_off + str_id * 4)[0]
    b = dex[off]
    o = 1 if (b & 0x80 == 0) else 2
    end = dex.find(b'\x00', off + o)
    return dex[off + o : end].decode('utf-8', errors='ignore')

def get_method_name(mid):
    class_idx, proto_idx, name_idx = struct.unpack_from('<HHI', dex, method_ids_off + mid * 8)
    return get_str(name_idx)

def read_uleb128(data, off):
    res = 0
    shift = 0
    while True:
        b = data[off]
        off += 1
        res |= (b & 0x7f) << shift
        if not (b & 0x80):
            break
        shift += 7
    return res, off

def print_class_methods(name, off):
    print(f"\n=== {name} (0x{off:x}) ===")
    s_f, off = read_uleb128(dex, off)
    i_f, off = read_uleb128(dex, off)
    d_m, off = read_uleb128(dex, off)
    v_m, off = read_uleb128(dex, off)
    for _ in range(s_f * 2 + i_f * 2):
        _, off = read_uleb128(dex, off)
    mid = 0
    for _ in range(d_m):
        diff, off = read_uleb128(dex, off)
        mid += diff
        _, off = read_uleb128(dex, off)
        code_off, off = read_uleb128(dex, off)
        print(f"Direct: {get_method_name(mid)}, code_off=0x{code_off:x}")
    mid = 0
    for _ in range(v_m):
        diff, off = read_uleb128(dex, off)
        mid += diff
        _, off = read_uleb128(dex, off)
        code_off, off = read_uleb128(dex, off)
        print(f"Virtual: {get_method_name(mid)}, code_off=0x{code_off:x}")

print_class_methods("PlayerViewModel$a", 0x991bc5)
print_class_methods("PlayerViewModel$b", 0x973218)
