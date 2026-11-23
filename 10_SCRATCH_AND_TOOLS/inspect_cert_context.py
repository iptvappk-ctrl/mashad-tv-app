import zipfile, struct

with zipfile.ZipFile('c:/Users/hassa/Desktop/all project/iptv app/10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')

string_ids_size, string_ids_off = struct.unpack('<II', dex[56:64])
def get_str(idx):
    if idx >= string_ids_size: return ""
    off = struct.unpack('<I', dex[string_ids_off + idx*4 : string_ids_off + idx*4 + 4])[0]
    p = off
    while (dex[p] & 0x80) != 0: p += 1
    p += 1
    end = dex.find(b'\x00', p)
    return dex[p:end].decode('utf-8', errors='replace')

for pos in [2299552, 5484708, 5488844]:
    print(f"=== Offset {pos} ===")
    for i in range(pos-100, pos+150):
        if dex[i] == 0x1A:
            s_idx = dex[i+2] | (dex[i+3] << 8)
            s_val = get_str(s_idx)
            if s_val:
                print(f"  {i}: '{s_val}'")
