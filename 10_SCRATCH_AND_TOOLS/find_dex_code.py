import zipfile, struct

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

# Parse DEX header
magic = dex[:8]
string_ids_size = struct.unpack_from("<I", dex, 56)[0]
string_ids_off = struct.unpack_from("<I", dex, 60)[0]

# Build string list
string_offsets = [struct.unpack_from("<I", dex, string_ids_off + i * 4)[0] for i in range(string_ids_size)]

def get_string(idx):
    off = string_offsets[idx]
    # read uleb128 length
    length = 0
    shift = 0
    p = off
    while True:
        b = dex[p]
        p += 1
        length |= (b & 0x7f) << shift
        if not (b & 0x80):
            break
        shift += 7
    # read string until 0
    end = dex.find(b"\x00", p)
    return dex[p:end].decode("utf-8", errors="replace")

# Find index of "https://deft.yacinelive.com/api/"
target_str = "https://deft.yacinelive.com/api/"
target_idx = None
for i in range(string_ids_size):
    if get_string(i) == target_str:
        target_idx = i
        print(f"Found target string at string_id {i}")
        break

if target_idx is not None:
    # search where target_idx (as 16-bit or 32-bit constant or opcode const-string) is used in code
    target_bytes_16 = struct.pack("<H", target_idx)
    pos = 0
    while True:
        idx = dex.find(b"\x1a" + target_bytes_16, pos) # 1a = const-string
        if idx == -1:
            break
        print(f"const-string found at byte offset {idx} (hex {hex(idx)})")
        pos = idx + 3
