import zipfile, struct

with zipfile.ZipFile('c:/Users/hassa/Desktop/all project/iptv app/10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')

string_ids_size, string_ids_off = struct.unpack('<II', dex[56:64])
type_ids_size, type_ids_off = struct.unpack('<II', dex[64:72])
method_ids_size, method_ids_off = struct.unpack('<II', dex[88:96])

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

# Disassemble invoke opcodes in range 6637108..6637884
start = 6637108
end = 6637884
p = start
while p < end:
    op = dex[p]
    if op in (0x6e, 0x6f, 0x70, 0x71, 0x72): # invoke-virtual/super/direct/static/interface
        m_idx = dex[p+2] | (dex[p+3] << 8)
        print(f"[{p}] call {get_method_name(m_idx)}")
        p += 6
    elif op in (0x74, 0x75, 0x76, 0x77, 0x78): # invoke-.../range
        m_idx = dex[p+2] | (dex[p+3] << 8)
        print(f"[{p}] call/range {get_method_name(m_idx)}")
        p += 6
    else:
        p += 2
