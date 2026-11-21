import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

matches = re.findall(rb"(com\.[a-zA-Z0-9_.]*player[a-zA-Z0-9_.]*)", dex)
print("Player packages found:", set([m.decode("latin1") for m in matches]))
