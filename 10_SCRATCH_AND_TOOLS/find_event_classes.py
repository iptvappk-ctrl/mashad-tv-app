import zipfile

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    for name in z.namelist():
        if name.endswith('.dex'):
            data = z.read(name)
            idx = 0
            while True:
                idx = data.find(b'Ldev/google/yacinetv/ui/event/', idx)
                if idx == -1:
                    break
                end = data.find(b';', idx)
                cls_name = data[idx:end+1].decode('latin1', errors='ignore')
                print(f'{name}: {cls_name}')
                idx = end + 1
