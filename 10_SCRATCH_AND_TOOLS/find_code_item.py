import zipfile, struct

with zipfile.ZipFile('c:/Users/hassa/Desktop/all project/iptv app/10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')

string_ids_size, string_ids_off = struct.unpack('<II', dex[56:64])
type_ids_size, type_ids_off = struct.unpack('<II', dex[64:72])
proto_ids_size, proto_ids_off = struct.unpack('<II', dex[72:80])
field_ids_size, field_ids_off = struct.unpack('<II', dex[80:88])
method_ids_size, method_ids_off = struct.unpack('<II', dex[88:96])
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

def get_method_name(idx):
    class_idx, proto_idx, name_idx = struct.unpack('<HHI', dex[method_ids_off + idx*8 : method_ids_off + idx*8 + 8])
    return get_type(class_idx) + "->" + get_str(name_idx)

def read_uleb128(data, off):
    res = 0
    shift = 0
    while True:
        b = data[off]
        off += 1
        res |= (b & 0x7f) << shift
        if (b & 0x80) == 0:
            break
        shift += 7
    return res, off

pos = 6637412
print(f"Finding method covering {pos}...")

for i in range(class_defs_size):
    c_off = class_defs_off + i * 32
    class_idx, access_flags, superclass_idx, interfaces_off, source_file_idx, annotations_off, class_data_off, static_values_off = struct.unpack('<IIIIIIII', dex[c_off:c_off+32])
    if class_data_off == 0:
        continue
    p = class_data_off
    static_fields_size, p = read_uleb128(dex, p)
    instance_fields_size, p = read_uleb128(dex, p)
    direct_methods_size, p = read_uleb128(dex, p)
    virtual_methods_size, p = read_uleb128(dex, p)
    
    # skip static fields
    for _ in range(static_fields_size):
        _, p = read_uleb128(dex, p)
        _, p = read_uleb128(dex, p)
    # skip instance fields
    for _ in range(instance_fields_size):
        _, p = read_uleb128(dex, p)
        _, p = read_uleb128(dex, p)
        
    # read direct methods
    m_idx = 0
    for _ in range(direct_methods_size):
        m_diff, p = read_uleb128(dex, p)
        m_idx += m_diff
        access, p = read_uleb128(dex, p)
        code_off, p = read_uleb128(dex, p)
        if code_off > 0:
            insns_size = struct.unpack('<I', dex[code_off+12:code_off+16])[0]
            start = code_off + 16
            end = start + insns_size * 2
            if start <= pos < end:
                print(f"MATCH (direct): {get_method_name(m_idx)} [code: {start}..{end}]")
                print(f"Class: {get_type(class_idx)}")
                
    # read virtual methods
    m_idx = 0
    for _ in range(virtual_methods_size):
        m_diff, p = read_uleb128(dex, p)
        m_idx += m_diff
        access, p = read_uleb128(dex, p)
        code_off, p = read_uleb128(dex, p)
        if code_off > 0:
            insns_size = struct.unpack('<I', dex[code_off+12:code_off+16])[0]
            start = code_off + 16
            end = start + insns_size * 2
            if start <= pos < end:
                print(f"MATCH (virtual): {get_method_name(m_idx)} [code: {start}..{end}]")
                print(f"Class: {get_type(class_idx)}")
