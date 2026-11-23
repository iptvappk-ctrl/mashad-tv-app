import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    for name in z.namelist():
        if name.endswith('.dex'):
            dex = z.read(name)
            for m in re.finditer(rb'tw_key\?stream_name=', dex):
                start = max(0, m.start() - 200)
                end = min(len(dex), m.end() + 300)
                snippet = dex[start:end]
                cleaned = ''.join(chr(b) if 32 <= b < 127 else '.' for b in snippet)
                print(f"[{name} @ {m.start()}]:\n{cleaned}\n")
