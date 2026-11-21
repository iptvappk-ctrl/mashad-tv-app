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

code_off = 2033696
registers_size, ins_size, outs_size, tries_size, debug_info_off, insns_size = struct.unpack_from('<HHHHII', dex, code_off)
insns = dex[code_off + 16: code_off + 16 + insns_size * 2]

i = 0
while i < len(insns):
    op = insns[i]
    if op in [0x6e, 0x6f, 0x70, 0x71, 0x72]:
        m_idx = struct.unpack('<H', insns[i+2:i+4])[0]
        print(f'{i:04x}: invoke {get_method(m_idx)}')
        i += 6
    elif op == 0x1a:
        s_idx = struct.unpack('<H', insns[i+2:i+4])[0]
        print(f'{i:04x}: const-string "{get_string(s_idx)}"')
        i += 4
    elif op == 0x1b:
        s_idx = struct.unpack('<I', insns[i+2:i+6])[0]
        print(f'{i:04x}: const-string/jumbo "{get_string(s_idx)}"')
        i += 6
    else:
        i += 2
