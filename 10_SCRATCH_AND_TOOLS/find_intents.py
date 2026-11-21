import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

pos = 0
while True:
    idx = dex.find(b"android.intent.action.VIEW", pos)
    if idx == -1:
        break
    chunk = dex[max(0, idx - 200): min(len(dex), idx + 200)]
    strings = re.findall(rb"[\x20-\x7e]{3,}", chunk)
    print("VIEW Intent:", [s.decode("latin1") for s in strings])
    pos = idx + 26
