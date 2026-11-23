import zipfile

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    for dex_name in z.namelist():
        if not dex_name.endswith('.dex'): continue
        dex = z.read(dex_name)
        pos = dex.find(b'deft.yacinelive.com')
        if pos != -1:
            print(f"Found in {dex_name} at {pos}")
            # Let's search backward for 'Lcom/' or 'L' class descriptor
            c_pos = dex.rfind(b'L', 0, pos)
            print("Surrounding 300 bytes before:")
            print(dex[max(0, pos-300):pos])
            print("Surrounding 300 bytes after:")
            print(dex[pos:pos+300])
