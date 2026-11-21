import struct

target_idx = 45501
with open("10_SCRATCH_AND_TOOLS/find_dex_code.py") as f:
    pass

import zipfile
z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

target_bytes_32 = struct.pack("<I", target_idx)
target_bytes_16 = struct.pack("<H", target_idx)

# Search for opcode 0x1b (const-string/jumbo AA BBBB BBBB) -> 1b AA <4 bytes>
pos = 0
found = []
while True:
    idx = dex.find(target_bytes_16, pos)
    if idx == -1:
        break
    found.append(idx)
    pos = idx + 2
print("Found 16-bit occurrences:", len(found), found[:10])

pos = 0
found32 = []
while True:
    idx = dex.find(target_bytes_32, pos)
    if idx == -1:
        break
    found32.append(idx)
    pos = idx + 4
print("Found 32-bit occurrences:", len(found32), found32[:10])
