import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

idx = dex.find(b"categories/{id}/channels")
# Find class descriptor or annotations around it
start = max(0, idx - 500)
end = min(len(dex), idx + 500)
chunk = dex[start:end]
for s in re.findall(rb"[\x20-\x7e]{3,}", chunk):
    print(s.decode("latin1"))
