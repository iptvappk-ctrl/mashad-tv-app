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

# Look for header names in string table
headers = []
for i in range(string_ids_size):
    s = get_str(i)
    if any(h in s.lower() for h in ['x-android', 'authorization', 'bearer', 'token', 'secret', 'signature', 'device-id', 'app-version']):
        headers.append((i, s))

for idx, h in headers:
    print(f"String {idx}: '{h}'")
