import re

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

urls = set(re.findall(rb'https?://[^\s"\'<>\x00-\x1f]+\.m3u8[^\s"\'<>\x00-\x1f]*', data))
print(f'Found {len(urls)} m3u8 URLs:')
for u in sorted(urls):
    print(u.decode('latin1', errors='ignore'))
