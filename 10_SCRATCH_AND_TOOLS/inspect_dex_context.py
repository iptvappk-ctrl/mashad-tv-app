import zipfile

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk') as z:
    for name in z.namelist():
        if name.endswith('.dex'):
            dex = z.read(name)
            idx = 0
            while True:
                idx = dex.find(b'deft.yacinelive.com', idx)
                if idx == -1:
                    break
                start = max(0, idx - 400)
                end = min(len(dex), idx + 600)
                snippet = dex[start:end]
                # print readable strings in snippet
                print(f"=== MATCH in {name} at offset {idx} ===")
                chars = []
                for b in snippet:
                    if 32 <= b <= 126:
                        chars.append(chr(b))
                    else:
                        chars.append(' ')
                print(''.join(chars))
                idx += 20
