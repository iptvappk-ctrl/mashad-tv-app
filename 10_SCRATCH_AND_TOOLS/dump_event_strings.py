import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    data = z.read('classes.dex')
    for m in re.finditer(b'dev/google/yacinetv/ui/event', data):
        start = max(0, m.start() - 200)
        end = min(len(data), m.end() + 400)
        s = data[start:end]
        printable = ''.join(chr(b) if 32 <= b < 127 else '.' for b in s)
        print(printable)
        print('-'*50)
