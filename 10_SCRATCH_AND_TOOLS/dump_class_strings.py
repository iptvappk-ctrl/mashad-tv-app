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

def dump_class_strings(c_data_off, name):
    print(f"=== {name} ===")
    p = c_data_off
    static_fields_size, p = read_uleb128(dex, p)
    instance_fields_size, p = read_uleb128(dex, p)
    direct_methods_size, p = read_uleb128(dex, p)
    virtual_methods_size, p = read_uleb128(dex, p)
    for _ in range(static_fields_size):
        _, p = read_uleb128(dex, p)
        _, p = read_uleb128(dex, p)
    for _ in range(instance_fields_size):
        _, p = read_uleb128(dex, p)
        _, p = read_uleb128(dex, p)
    methods = []
    m_idx = 0
    for _ in range(direct_methods_size):
        m_diff, p = read_uleb128(dex, p)
        m_idx += m_diff
        _, p = read_uleb128(dex, p)
        code_off, p = read_uleb128(dex, p)
        methods.append((m_idx, code_off))
    m_idx = 0
    for _ in range(virtual_methods_size):
        m_diff, p = read_uleb128(dex, p)
        m_idx += m_diff
        _, p = read_uleb128(dex, p)
        code_off, p = read_uleb128(dex, p)
        methods.append((m_idx, code_off))
    for m_id, code_off in methods:
        if code_off > 0:
            insns_size = struct.unpack('<I', dex[code_off+12:code_off+16])[0]
            start = code_off + 16
            end = start + insns_size * 2
            for i in range(start, end, 2):
                if dex[i] == 0x1A:
                    s_idx = dex[i+2] | (dex[i+3] << 8)
                    s_val = get_str(s_idx)
                    if s_val:
                        print(f"  [{m_id}] '{s_val}'")

dump_class_strings(9510797, 'LD8/a;')
dump_class_strings(9514330, 'LE8/a;')
dump_class_strings(9767407, 'LE8/h;')
