import struct

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    import zipfile
    z = zipfile.ZipFile(f)
    dex = z.read('classes.dex')

string_ids_off = struct.unpack_from('<I', dex, 60)[0]
type_ids_off = struct.unpack_from('<I', dex, 68)[0]
class_defs_size = struct.unpack_from('<I', dex, 96)[0]
class_defs_off = struct.unpack_from('<I', dex, 100)[0]

def get_str(str_id):
    off = struct.unpack_from('<I', dex, string_ids_off + str_id * 4)[0]
    b = dex[off]
    o = 1 if (b & 0x80 == 0) else 2
    end = dex.find(b'\x00', off + o)
    return dex[off + o : end].decode('utf-8', errors='ignore')

def get_type_name(type_id):
    descriptor_idx = struct.unpack_from('<I', dex, type_ids_off + type_id * 4)[0]
    return get_str(descriptor_idx)

for i in range(class_defs_size):
    c_off = class_defs_off + i * 32
    class_idx, access_flags, superclass_idx, interfaces_off, source_file_idx, annotations_off, class_data_off, static_values_off = struct.unpack_from('<IIIIIIII', dex, c_off)
    cname = get_type_name(class_idx)
    if 'PlayerViewModel' in cname or 'Stream' in cname and 'ytv' in cname:
        print(f"Class: {cname}, class_data_off=0x{class_data_off:x}")
