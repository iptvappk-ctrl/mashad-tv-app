import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk') as z:
    for name in z.namelist():
        if name.endswith('.dex'):
            data = z.read(name)
            urls = re.findall(b'https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}[^\\s"\'<>]*', data)
            for u in set(urls):
                try:
                    u_str = u.decode('utf-8', errors='ignore')
                    if any(k in u_str.lower() for k in ['bein', 'stream', 'live', 'channel', 'api', 'm3u', 'ycn', 'yacine']):
                        print(f"[{name}] {u_str}")
                except:
                    pass
