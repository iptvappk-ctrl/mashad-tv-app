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

target = b'PlayerViewModel'
pos = 0
while True:
    pos = dex.find(target, pos)
    if pos == -1: break
    print(f"Target found at 0x{pos:x}")
    # print context
    print(dex[max(0, pos-100):pos+200])
    pos += len(target)
