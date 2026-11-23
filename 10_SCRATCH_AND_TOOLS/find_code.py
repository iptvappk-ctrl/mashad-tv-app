with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    import zipfile
    z = zipfile.ZipFile(f)
    dex = z.read('classes.dex')

code_off = 0x6092f0
print(dex[code_off:code_off+100].hex(' '))
