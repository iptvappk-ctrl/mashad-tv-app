import urllib.request, ssl, zipfile, io, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://files.catbox.moe/8yj138.apk", headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=20) as resp:
        data = resp.read()
        print("Downloaded bytes from catbox:", len(data))
        z = zipfile.ZipFile(io.BytesIO(data))
        for name in z.namelist():
            if name.endswith(".dex"):
                dex = z.read(name)
                urls = set(re.findall(rb"https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}[^\x00\s\"\'<>]*", dex))
                for u in urls:
                    u_str = u.decode("utf-8", errors="ignore")
                    if any(k in u_str for k in ["api", "yacine", "ycn", "live", "stream", "channel"]):
                        if not any(x in u_str for x in ["google", "android", "schema", "w3.org", "facebook"]):
                            print(f"[{name}] {u_str}")
except Exception as e:
    print("Error:", e)
