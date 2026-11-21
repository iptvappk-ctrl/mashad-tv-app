import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

candidates = [
    "https://raw.githubusercontent.com/aimadnet/yacinetv-api/main/yacinetv.py",
    "https://raw.githubusercontent.com/aimadnet/yacinetv-api/main/yacinetv/__init__.py",
    "https://raw.githubusercontent.com/aimadnet/yacinetv-api/master/yacinetv.py",
    "https://raw.githubusercontent.com/aimadnet/yacinetv-api/master/yacinetv/__init__.py",
    "https://raw.githubusercontent.com/aimadnet/yacinetv-api/main/setup.py",
]

for url in candidates:
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            print("Found:", url)
            print(r.read().decode("utf-8", errors="replace")[:600])
            break
    except Exception as e:
        print("Failed:", url, e)
