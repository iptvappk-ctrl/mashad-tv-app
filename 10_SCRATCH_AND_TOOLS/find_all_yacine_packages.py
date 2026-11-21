import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

classes = set(re.findall(rb"Ldev/google/yacinetv/[a-zA-Z0-9_/]+", dex))
for c in sorted(classes):
    print(c.decode("latin1"))
