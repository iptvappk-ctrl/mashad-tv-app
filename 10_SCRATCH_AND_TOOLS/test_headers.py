import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

tests = [
    {'User-Agent': 'okhttp/4.11.0', 'x-android-package': 'ver3.ycntivi.off'},
    {'User-Agent': 'okhttp/4.9.0', 'x-android-package': 'ver3.ycntivi.off'},
    {'User-Agent': 'okhttp/3.12.1', 'x-android-package': 'ver3.ycntivi.off'},
    {'User-Agent': 'okhttp/4.11.0', 'x-android-package': 'ver3.ycntivi.off', 'x-android-cert': '81e2354ee2ccf71ada90394f6c8bf74c66b01db6'},
    {'User-Agent': 'okhttp/4.11.0', 'x-android-package': 'ver3.ycntivi.off', 'x-android-cert': 'E6551F182360EC10732ABBE9E8F2905CBEF2C2155DB2F2726EC42A0044EC0C0A'},
    {'User-Agent': 'okhttp/4.11.0'},
    {'User-Agent': 'Mozilla/5.0 (Linux; Android 13)', 'x-android-package': 'ver3.ycntivi.off'},
]

for i, h in enumerate(tests):
    url = 'https://def777.ycnapi.com/api/categories'
    try:
        req = urllib.request.Request(url, headers=h)
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            body = r.read()
            print(f"Test {i} -> {r.status}, T={r.headers.get('T')}, len={len(body)}")
    except urllib.error.HTTPError as e:
        body = e.read()
        print(f"Test {i} -> HTTP {e.code}: {body[:100]}")
    except Exception as e:
        print(f"Test {i} -> {e}")
