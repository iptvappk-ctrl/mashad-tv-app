import zipfile, io, re

with open("10_SCRATCH_AND_TOOLS/scratch/inspect_yacine_apk.py") as f:
    pass

import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://raw.githubusercontent.com/yacinetv2/yacinetv2.3/refs/heads/main/YacineTV_____V3.2.apk", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
    data = resp.read()

z = zipfile.ZipFile(io.BytesIO(data))
for name in z.namelist():
    if name.endswith(".dex"):
        dex = z.read(name)
        # Find occurrences of strings around deft.yacinelive.com
        idx = 0
        while True:
            idx = dex.find(b"yacinelive.com", idx)
            if idx == -1:
                break
            start = max(0, idx - 100)
            end = min(len(dex), idx + 100)
            print(f"[{name}] Context: {dex[start:end]}")
            idx += 14
