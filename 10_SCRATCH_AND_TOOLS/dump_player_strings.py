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

for i in range(class_defs_size):
    c_off = class_defs_off + i * 32
    class_idx = struct.unpack('<I', dex[c_off:c_off+4])[0]
    c_name = get_type(class_idx)
    if 'PlayerViewModel' in c_name or 'PlayerActivity' in c_name or 'ytvclib/utils/C' in c_name:
        class_data_off = struct.unpack('<I', dex[c_off+24:c_off+28])[0]
        if class_data_off > 0:
            p = class_data_off
            sf, p = read_uleb128(dex, p)
            inf, p = read_uleb128(dex, p)
            dm, p = read_uleb128(dex, p)
            vm, p = read_uleb128(dex, p)
            for _ in range(sf + inf):
                _, p = read_uleb128(dex, p); _, p = read_uleb128(dex, p)
            methods = []
            m_idx = 0
            for _ in range(dm):
                m_diff, p = read_uleb128(dex, p); m_idx += m_diff; _, p = read_uleb128(dex, p)
                c_off, p = read_uleb128(dex, p); methods.append(c_off)
            m_idx = 0
            for _ in range(vm):
                m_diff, p = read_uleb128(dex, p); m_idx += m_diff; _, p = read_uleb128(dex, p)
                c_off, p = read_uleb128(dex, p); methods.append(c_off)
            print(f"=== {c_name} ===")
            for code_off in methods:
                if code_off > 0:
                    insns_size = struct.unpack('<I', dex[code_off+12:code_off+16])[0]
                    start = code_off + 16
                    end = start + insns_size * 2
                    for k in range(start, end, 2):
                        if dex[k] == 0x1A:
                            s_idx = dex[k+2] | (dex[k+3] << 8)
                            s_val = get_str(s_idx)
                            if len(s_val) > 2:
                                print(f"  '{s_val}'")
