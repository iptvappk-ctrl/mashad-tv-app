import zipfile, re

apk_path = r'10_SCRATCH_AND_TOOLS\yacine.apk'
with zipfile.ZipFile(apk_path, 'r') as z:
    dex = z.read('classes.dex')
    pos = dex.find(b'LC8/e;')
    print("Found LC8/e;: ", pos)
    if pos != -1:
        print(dex[pos-50:pos+150])
