import struct

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    import zipfile
    z = zipfile.ZipFile(f)
    dex = z.read('classes.dex')

string_ids_off = struct.unpack_from('<I', dex, 60)[0]
def get_str(str_id):
    off = struct.unpack_from('<I', dex, string_ids_off + str_id * 4)[0]
    b = dex[off]
    o = 1 if (b & 0x80 == 0) else 2
    end = dex.find(b'\x00', off + o)
    return dex[off + o : end].decode('utf-8', errors='ignore')

# Scan around 0x654642 - 1000 bytes
for pos in range(0x654642 - 1500, 0x654642, 2):
    if dex[pos] == 0x1a:
        sid = struct.unpack_from('<H', dex, pos+2)[0]
        if sid < 60000:
            try:
                s = get_str(sid)
                if len(s) > 0 and len(s) < 80:
                    print(f"0x{pos:x}: sid={sid} -> '{s}'")
            except:
                pass
