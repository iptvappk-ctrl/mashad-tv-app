import zipfile, re

z = zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk')
arsc = z.read('resources.arsc')

for target in [b'google_api_key', b'ycntv-7a08e', b'yt_base64_key']:
    pos = 0
    while True:
        idx = arsc.find(target, pos)
        if idx == -1: break
        print(f'=== Found {target} at {idx} ===')
        chunk = arsc[max(0, idx - 100): min(len(arsc), idx + 200)]
        strings = re.findall(rb'[\x20-\x7e]{3,}', chunk)
        print([s.decode('latin1') for s in strings])
        pos = idx + len(target)
