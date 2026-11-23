import zipfile

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    dex = z.read('classes.dex')
    pos = dex.find(b'https://deft.yacinelive.com/api/')
    print("Found at:", pos)
    # Search backwards for class name or method name (L...;)
    chunk = dex[max(0, pos-2000):pos]
    import re
    classes = re.findall(rb'L[a-zA-Z0-9_/]+;', chunk)
    print("Preceding classes:", classes[-10:])
    # Search forwards
    chunk_fwd = dex[pos:pos+2000]
    classes_fwd = re.findall(rb'L[a-zA-Z0-9_/]+;', chunk_fwd)
    print("Following classes:", classes_fwd[:10])
