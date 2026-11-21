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

for i in range(method_ids_size):
    class_idx, proto_idx, name_idx = struct.unpack_from('<HHI', dex, method_ids_off + i * 8)
    c_name = get_string(type_ids[class_idx])
    m_name = get_string(name_idx)
    if 'PlayerViewModel' in c_name:
        print(f'{c_name}->{m_name}')
