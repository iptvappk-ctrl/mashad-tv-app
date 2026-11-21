import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

idx = dex.find(b"tw_key?stream_name=")
chunk = dex[max(0, idx - 800): min(len(dex), idx + 800)]
for s in re.findall(rb"[\x20-\x7e]{3,}", chunk):
    print(s.decode("latin1"))
