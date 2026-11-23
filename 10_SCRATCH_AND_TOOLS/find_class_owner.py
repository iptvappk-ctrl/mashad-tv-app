import zipfile, struct

with zipfile.ZipFile('c:/Users/hassa/Desktop/all project/iptv app/10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')

string_ids_size, string_ids_off = struct.unpack('<II', dex[56:64])
type_ids_size, type_ids_off = struct.unpack('<II', dex[64:72])
class_defs_size, class_defs_off = struct.unpack('<II', dex[96:104])

def get_str(idx):
    if idx >= string_ids_size: return ""
    off = struct.unpack('<I', dex[string_ids_off + idx*4 : string_ids_off + idx*4 + 4])[0]
    p = off
    while (dex[p] & 0x80) != 0: p += 1
    p += 1
    end = dex.find(b'\x00', p)
    return dex[p:end].decode('utf-8', errors='replace')

def get_type(idx):
    if idx >= type_ids_size: return ""
    descriptor_idx = struct.unpack('<I', dex[type_ids_off + idx*4 : type_ids_off + idx*4 + 4])[0]
    return get_str(descriptor_idx)

# Search all class definitions for one whose class_data_off or bytecode covers 6637412
pos = 6637412
# In DEX, method code_off is in class_data
for i in range(class_defs_size):
    c_off = class_defs_off + i * 32
    class_idx, access_flags, superclass_idx, interfaces_off, source_file_idx, annotations_off, class_data_off, static_values_off = struct.unpack('<IIIIIIII', dex[c_off:c_off+32])
    c_name = get_type(class_idx)
    if 'yacine' in c_name.lower() or 'ytv' in c_name.lower() or 'app' in c_name.lower():
        if class_data_off > 0:
            # Let's check if pos is within 50000 bytes of class_data_off
            if abs(class_data_off - pos) < 100000:
                print(f"Class: {c_name}, data_off: {class_data_off}")
