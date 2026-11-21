import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

idx = dex.find(b"Ldev/google/yacinetv/ui/livetv/ChannelFragment;")
print("Class descriptor found at:", idx)
chunk = dex[max(0, idx - 400): min(len(dex), idx + 800)]
strings = re.findall(rb"[\x20-\x7e]{3,}", chunk)
for s in strings:
    print(s.decode("latin1"))
