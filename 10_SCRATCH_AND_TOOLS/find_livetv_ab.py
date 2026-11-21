import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

for target in [b"Ldev/google/yacinetv/ui/livetv/a;", b"Ldev/google/yacinetv/ui/livetv/b;"]:
    idx = dex.find(target)
    print("Found target:", target, "at", idx)
    chunk = dex[max(0, idx - 100): min(len(dex), idx + 400)]
    strings = re.findall(rb"[\x20-\x7e]{3,}", chunk)
    print("Strings:", [s.decode("latin1") for s in strings])
