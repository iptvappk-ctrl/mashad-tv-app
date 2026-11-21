import zipfile, io, re

with open("10_SCRATCH_AND_TOOLS/inspect_catbox.py") as f:
    pass

import urllib.request, ssl

req = urllib.request.Request("https://files.catbox.moe/8yj138.apk", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req) as resp:
    data = resp.read()

z = zipfile.ZipFile(io.BytesIO(data))
dex_data = z.read("classes.dex")

# Search for deft.yacinelive.com and examine surrounding bytes
pos = 0
while True:
    idx = dex_data.find(b"deft.yacinelive.com", pos)
    if idx == -1:
        break
    start = max(0, idx - 200)
    end = min(len(dex_data), idx + 200)
    chunk = dex_data[start:end]
    # Extract readable strings
    strings = re.findall(rb"[\x20-\x7e]{4,}", chunk)
    print(f"Match at {idx}:", [s.decode("latin1") for s in strings])
    pos = idx + 19
