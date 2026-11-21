import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

idx = dex.find(b"tw_key?stream_name=")
if idx != -1:
    chunk = dex[max(0, idx - 500): min(len(dex), idx + 500)]
    strings = re.findall(rb"[\x20-\x7e]{4,}", chunk)
    print("Around tw_key:")
    for s in strings:
        print(" ", s.decode("latin1"))

idx2 = dex.find(b"deft.yacinelive.com/api/")
if idx2 != -1:
    chunk2 = dex[max(0, idx2 - 500): min(len(dex), idx2 + 500)]
    strings2 = re.findall(rb"[\x20-\x7e]{4,}", chunk2)
    print("Around deft.yacinelive.com/api/:")
    for s in strings2:
        print(" ", s.decode("latin1"))
