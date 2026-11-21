import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

idx = 0
found = []
while True:
    idx = dex.find(b"ChannelFragment.kt", idx)
    if idx == -1:
        break
    chunk = dex[max(0, idx - 400): min(len(dex), idx + 800)]
    strings = re.findall(rb"[\x20-\x7e]{3,}", chunk)
    found.append([s.decode("latin1") for s in strings])
    idx += 18

for f in found:
    print("CHUNK:", f)
