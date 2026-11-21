import urllib.request, ssl, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://api.github.com/repos/aimadnet/yacinetv-api/contents", headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
        data = json.loads(r.read().decode())
        for f in data:
            print(f["name"], "->", f["download_url"])
except Exception as e:
    print(e)
