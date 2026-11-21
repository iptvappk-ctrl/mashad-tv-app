import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

keywords = [b"categories", b"channels", b"events", b"channel/", b"event/"]
for kw in keywords:
    pos = 0
    matches = []
    while len(matches) < 5:
        idx = dex.find(kw, pos)
        if idx == -1:
            break
        chunk = dex[max(0, idx - 100): min(len(dex), idx + 100)]
        printable = [s.decode("latin1") for s in re.findall(rb"[\x20-\x7e]{4,}", chunk)]
        matches.append(printable)
        pos = idx + len(kw) + 1
    print(f"=== Keyword {kw.decode()} ===")
    for m in matches:
        print(" ", m)
