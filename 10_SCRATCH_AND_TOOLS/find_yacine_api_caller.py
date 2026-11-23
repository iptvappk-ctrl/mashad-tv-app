import zipfile

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')
    pos = dex.find(b'deft.yacinelive.com')
    while pos != -1:
        start = max(0, pos - 200)
        end = min(len(dex), pos + 300)
        snippet = dex[start:end]
        cleaned = ''.join(chr(b) if 32 <= b < 127 else '.' for b in snippet)
        print(f"[{pos}]: {cleaned}\n")
        pos = dex.find(b'deft.yacinelive.com', pos + 1)
