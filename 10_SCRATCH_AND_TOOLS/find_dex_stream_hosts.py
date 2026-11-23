import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')
    # Find all strings containing http or https
    matches = set(re.findall(rb'https?://[^\x00\s\"\'<>]+', dex))
    print(f"Total urls: {len(matches)}")
    for m in sorted(matches):
        s = m.decode('latin1', errors='ignore')
        if any(w in s for w in ['stream', 'm3u8', 'live', 'channel', 'tv', 'cdn', 'hls']):
            print("  ", s)
