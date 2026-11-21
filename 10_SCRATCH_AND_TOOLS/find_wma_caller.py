import zipfile, struct

z = zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk')
dex = z.read('classes.dex')

string_ids_size = struct.unpack_from('<I', dex, 56)[0]
string_ids_off = struct.unpack_from('<I', dex, 60)[0]
string_offsets = [struct.unpack_from('<I', dex, string_ids_off + i * 4)[0] for i in range(string_ids_size)]

type_ids_size = struct.unpack_from('<I', dex, 64)[0]
type_ids_off = struct.unpack_from('<I', dex, 68)[0]
type_ids = [struct.unpack_from('<I', dex, type_ids_off + i * 4)[0] for i in range(type_ids_size)]

method_ids_size = struct.unpack_from('<I', dex, 88)[0]
method_ids_off = struct.unpack_from('<I', dex, 92)[0]

def get_string(idx):
    if idx >= string_ids_size: return ''
    off = string_offsets[idx]
    length = 0
    shift = 0
    p = off
    while True:
        b = dex[p]
        p += 1
        length |= (b & 0x7f) << shift
        if not (b & 0x80): break
        shift += 7
    end = dex.find(b'\x00', p)
    return dex[p:end].decode('utf-8', errors='replace')

def get_method(idx):
    if idx >= method_ids_size: return ''
    class_idx, proto_idx, name_idx = struct.unpack_from('<HHI', dex, method_ids_off + idx * 8)
    return f'{get_string(type_ids[class_idx])}->{get_string(name_idx)}'

class_defs_size = struct.unpack_from('<I', dex, 96)[0]
class_defs_off = struct.unpack_from('<I', dex, 100)[0]

def read_uleb128(p):
    val = 0
    shift = 0
    while True:
        b = dex[p]
        p += 1
        val |= (b & 0x7f) << shift
        if not (b & 0x80): break
        shift += 7
    return val, p

target_off = 6323742

for i in range(class_defs_size):
    class_idx, access_flags, superclass_idx, interfaces_off, source_file_idx, annotations_off, class_data_off, static_values_off = struct.unpack_from('<IIIIIIII', dex, class_defs_off + i * 32)
    if class_data_off == 0: continue
    p = class_data_off
    num_sf, p = read_uleb128(p)
    num_if, p = read_uleb128(p)
    num_dm, p = read_uleb128(p)
    num_vm, p = read_uleb128(p)
    for _ in range(num_sf):
        _, p = read_uleb128(p)
        _, p = read_uleb128(p)
    for _ in range(num_if):
        _, p = read_uleb128(p)
        _, p = read_uleb128(p)
    m_id = 0
    for _ in range(num_dm + num_vm):
        m_diff, p = read_uleb128(p)
        m_id += m_diff
        _, p = read_uleb128(p)
        code_off, p = read_uleb128(p)
        if code_off > 0:
            registers_size, ins_size, outs_size, tries_size, debug_info_off, insns_size = struct.unpack_from('<HHHHII', dex, code_off)
            start_off = code_off + 16
            end_off = start_off + insns_size * 2
            if start_off <= target_off < end_off:
                c_name = get_string(type_ids[class_idx])
                s_file = get_string(source_file_idx)
                print(f'Found! Class: {c_name}, Method: {get_method(m_id)}, Source: {s_file}')
                # print all strings in this method
                insns = dex[start_off:end_off]
                for j in range(0, len(insns) - 2, 2):
                    if insns[j] == 0x1a:
                        s_idx = struct.unpack('<H', insns[j+2:j+4])[0]
                        print('  const-string:', get_string(s_idx))
