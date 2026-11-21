import zipfile, struct

z = zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk')
arsc = z.read('resources.arsc')

# Let's search for YT_BASE64_KEY or other strings in arsc
pos = 0
found = []
while True:
    idx = arsc.find(b'\x00\x00', pos)
    if idx == -1: break
    # check if string follows
    pos = idx + 2

# Search all UTF-8 / UTF-16 strings in arsc
import re
strings = re.findall(rb'([a-zA-Z0-9_\-\.\:\/]{4,})', arsc)
candidates = set()
for s in strings:
    s_dec = s.decode('latin1')
    if any(k in s_dec.lower() for k in ['http', 'api', 'host', 'key', 'yacine', 'ycn', 'domain', 'deft']):
        candidates.add(s_dec)

for c in sorted(candidates):
    print(c)
