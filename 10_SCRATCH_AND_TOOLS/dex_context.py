import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

idx = dex.find(b"deft.yacinelive.com/api/")
# In DEX format, string table has string_ids. Let us find method or field referencing it.
# Let us inspect 1500 bytes around idx
chunk = dex[max(0, idx - 1000): min(len(dex), idx + 1000)]
for line in re.findall(rb"[\x20-\x7e]{3,}", chunk):
    print(line.decode("latin1"))
