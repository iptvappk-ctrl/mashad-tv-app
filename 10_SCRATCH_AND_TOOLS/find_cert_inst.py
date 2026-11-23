import zipfile, struct, re

with zipfile.ZipFile('c:/Users/hassa/Desktop/all project/iptv app/10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')

target_idx = 35031
b_lo = target_idx & 0xFF
b_hi = (target_idx >> 8) & 0xFF

matches = list(re.finditer(re.escape(bytes([0x1a])) + rb'.' + re.escape(bytes([b_lo, b_hi])), dex))
print('const-string matches for X-Android-Cert:', len(matches))
for m in matches:
    print('Match at:', m.start())
