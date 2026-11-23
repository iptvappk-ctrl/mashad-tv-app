import struct

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    import zipfile
    z = zipfile.ZipFile(f)
    dex = z.read('classes.dex')

target_offset = 0x654764

# Let's find which class_def has code covering target_offset
class_defs_size = struct.unpack_from('<I', dex, 96)[0]
class_defs_off = struct.unpack_from('<I', dex, 100)[0]
string_ids_off = struct.unpack_from('<I', dex, 60)[0]
type_ids_off = struct.unpack_from('<I', dex, 68)[0]

def get_str(str_id):
    off = struct.unpack_from('<I', dex, string_ids_off + str_id * 4)[0]
    b = dex[off]
    o = 1 if (b & 0x80 == 0) else 2
    end = dex.find(b'\x00', off + o)
    return dex[off + o : end].decode('utf-8', errors='ignore')

def get_type_name(type_id):
    descriptor_idx = struct.unpack_from('<I', dex, type_ids_off + type_id * 4)[0]
    return get_str(descriptor_idx)

# Search class_defs
found_class = None
for i in range(class_defs_size):
    c_off = class_defs_off + i * 32
    class_idx, access_flags, superclass_idx, interfaces_off, source_file_idx, annotations_off, class_data_off, static_values_off = struct.unpack_from('<IIIIIIII', dex, c_off)
    if class_data_off == 0: continue
    
    # Check if target_offset is roughly in this class's methods
    # We can inspect class_data_off
    # But simpler: scan class_data_off
    pass

# Alternative: search string constants right before target_offset
for sid in [45501]: # our target
    print(f"Target string: {get_str(sid)}")

# Let's inspect string references around 0x654764:
# 1a 02 98 b0 -> string 0xb098 = 45208
# 1a 05 bd b1 -> string 0xb1bd = 45501
s1 = struct.unpack_from('<H', dex, 0x654764 - 24)[0]
print(f"String before: {get_str(0xb098)}")

# Let's find any type descriptors near 0x654764
# In bytecode: check-cast, new-instance (22), etc.
# 22 02 0b 2d -> type 0x2d0b = 11531
print("Type 1:", get_type_name(0x2d0b))
# 22 03 c2 01 -> type 0x01c2 = 450
print("Type 2:", get_type_name(0x01c2))
# 22 03 fa 2d -> type 0x2dfa
print("Type 3:", get_type_name(0x2dfa))
