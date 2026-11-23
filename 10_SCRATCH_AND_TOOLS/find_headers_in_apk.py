import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

# Let's search for header names in the dex
for word in [b'x-android', b'ver3.', b'ycntivi', b'addHeader', b'header(']:
    pos = 0
    while True:
        pos = dex.find(word, pos)
        if pos == -1: break
        start = max(0, pos - 80)
        end = min(len(dex), pos + 120)
        chunk = dex[start:end]
        cleaned = ''.join(chr(b) if 32 <= b < 127 else '.' for b in chunk)
        print(f"[{word.decode()} at {pos}]: {cleaned}")
        pos += len(word)
