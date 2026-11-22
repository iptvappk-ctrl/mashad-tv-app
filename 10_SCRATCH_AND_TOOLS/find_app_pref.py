import zipfile, re

apk_path = r'10_SCRATCH_AND_TOOLS\yacine.apk'
with zipfile.ZipFile(apk_path, 'r') as z:
    dex = z.read('classes.dex')
    # search for exact string "app" in dex string pool
    # in dex, strings are prefixed with uleb128 length: b'\x03app\x00'
    pos = 0
    while True:
        pos = dex.find(b'\x03app\x00', pos)
        if pos == -1: break
        print("Found \\x03app\\x00 at", pos)
        # print surrounding
        print(dex[max(0, pos-40):pos+60])
        pos += 5
