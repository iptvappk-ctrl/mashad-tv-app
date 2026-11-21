import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://raw.githubusercontent.com/aimadnet/yacinetv-api/main/api.py", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
    print(r.read().decode("utf-8", errors="replace"))
