import struct

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    import zipfile
    z = zipfile.ZipFile(f)
    dex = z.read('classes.dex')

# DEX header
# string_ids_size at offset 56 (uint), string_ids_off at offset 60 (uint)
string_ids_size = struct.unpack_from('<I', dex, 56)[0]
string_ids_off = struct.unpack_from('<I', dex, 60)[0]

target_str = b'https://deft.yacinelive.com/api/'
target_str_id = None

for i in range(string_ids_size):
    str_data_off = struct.unpack_from('<I', dex, string_ids_off + i * 4)[0]
    # parse uleb128 length
    b = dex[str_data_off]
    off = 1
    if b & 0x80:
        b2 = dex[str_data_off + 1]
        off = 2
    s_bytes = dex[str_data_off + off : str_data_off + off + len(target_str)]
    if s_bytes == target_str:
        target_str_id = i
        print(f"Found target string id: {target_str_id} (0x{target_str_id:x})")
        break

if target_str_id is not None:
    # Now find const-string opcode: 0x1a (const-string reg, string_id) -> 1a reg [id: short]
    # or 0x1b (const-string/jumbo reg, string_id) -> 1b reg [id: int]
    # Search in dex bytecode
    pattern_1a = bytes([0x1a]) + b'.' + struct.pack('<H', target_str_id)
    import re
    for m in re.finditer(re.escape(bytes([0x1a])) + b'.' + re.escape(struct.pack('<H', target_str_id)), dex):
        print(f"Found const-string at 0x{m.start():x}")
        # print 50 bytes before and after
        snippet = dex[max(0, m.start()-60):m.start()+80]
        print(snippet.hex(' '))
