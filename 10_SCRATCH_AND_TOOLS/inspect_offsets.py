import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

for off in [6637414, 9484398, 9924899]:
    chunk = dex[max(0, off - 300): min(len(dex), off + 300)]
    strings = re.findall(rb"[\x20-\x7e]{4,}", chunk)
    print(f"Around {off}:")
    for s in strings:
        print(" ", s.decode("latin1"))
