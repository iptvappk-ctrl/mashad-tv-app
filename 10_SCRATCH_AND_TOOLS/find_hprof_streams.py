import re

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

m3u8s = set(re.findall(rb'https?://[^\s"\'<>\x00]+\.m3u8[^\s"\'<>\x00]*', data))
print('Found m3u8 count:', len(m3u8s))
for u in sorted(m3u8s):
    print(u.decode('utf-8', errors='ignore'))
