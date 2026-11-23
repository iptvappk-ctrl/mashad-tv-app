import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')
    pos = 0
    while True:
        pos = dex.find(b'tw_key', pos)
        if pos == -1: break
        print(f"tw_key at {pos}:")
        print(dex[max(0, pos-100):pos+150])
        pos += 6

    # also check 'twitch'
    for m in re.finditer(rb'twitch', dex):
        p = m.start()
        print(f"twitch at {p}: {dex[max(0, p-50):p+100]}")
