import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    for name in z.namelist():
        if name.endswith('.dex'):
            data = z.read(name)
            # Find all http/https domains
            urls = set(re.findall(rb'https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}[^\s"\'<>\x00]*', data))
            print(f"=== {name} ===")
            for u in sorted(urls):
                us = u.decode('utf-8', errors='ignore')
                if any(x in us for x in ['api', 'ycn', 'yacine', 'stream', 'live', 'key', 'channel', 'event', 'ott', 'm3u8']):
                    print("  ", us)
