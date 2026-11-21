import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

# Let us search for occurrences of tw_key or deft across all classes
for m in re.finditer(rb"(L[a-zA-Z0-9_/$]+;)", dex):
    cls = m.group(1).decode("latin1")
    if "yacine" in cls.lower() or "madar" in cls.lower() or "stream" in cls.lower():
        print("Class:", cls)
