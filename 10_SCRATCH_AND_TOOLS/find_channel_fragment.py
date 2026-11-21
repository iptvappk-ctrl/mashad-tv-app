import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

pos = 0
matches = []
while True:
    idx = dex.find(b"ChannelFragment", pos)
    if idx == -1:
        break
    chunk = dex[max(0, idx - 150): min(len(dex), idx + 150)]
    printable = re.findall(rb"[\x20-\x7e]{4,}", chunk)
    matches.append([s.decode("latin1") for s in printable])
    pos = idx + 15
for m in matches[:10]:
    print("MATCH:", m)
