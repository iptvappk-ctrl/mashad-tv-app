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

# Let's inspect class_data_off = 0x973246 for PlayerViewModel
# class_data format: uleb128 static_fields_size, instance_fields_size, direct_methods_size, virtual_methods_size
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

off = 0x973246
static_fields_size, off = read_uleb128(dex, off)
instance_fields_size, off = read_uleb128(dex, off)
direct_methods_size, off = read_uleb128(dex, off)
virtual_methods_size, off = read_uleb128(dex, off)

print(f"PlayerViewModel: static={static_fields_size}, inst={instance_fields_size}, direct={direct_methods_size}, virt={virtual_methods_size}")

# Skip fields
for _ in range(static_fields_size):
    _, off = read_uleb128(dex, off) # field_idx_diff
    _, off = read_uleb128(dex, off) # access_flags
for _ in range(instance_fields_size):
    _, off = read_uleb128(dex, off)
    _, off = read_uleb128(dex, off)

# Read direct methods
mid = 0
for _ in range(direct_methods_size):
    diff, off = read_uleb128(dex, off)
    mid += diff
    flags, off = read_uleb128(dex, off)
    code_off, off = read_uleb128(dex, off)
    print(f"Direct method: {get_method_name(mid)}, code_off=0x{code_off:x}")

# Read virtual methods
mid = 0
for _ in range(virtual_methods_size):
    diff, off = read_uleb128(dex, off)
    mid += diff
    flags, off = read_uleb128(dex, off)
    code_off, off = read_uleb128(dex, off)
    print(f"Virtual method: {get_method_name(mid)}, code_off=0x{code_off:x}")
