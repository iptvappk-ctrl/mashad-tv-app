import zipfile, re

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    apk = zipfile.ZipFile(f)
    dex = apk.read('classes.dex')

for target in [b'channel/{id}', b'channel/', b'api/channel']:
    matches = [m.start() for m in re.finditer(re.escape(target), dex)]
    print(f"Target {target}: {len(matches)}")
    for pos in matches:
        print("  ", dex[max(0, pos-40):pos+80])
