import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

sites = [
    'https://yacineapp.tv',
    'https://yacine-tv.com',
    'https://yacinetv.be',
    'https://yacinetvapk.com',
    'https://yacine.app'
]

for s in sites:
    try:
        req = urllib.request.Request(s, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            html = r.read().decode('utf-8', errors='ignore')
            print(f"OK {s}: length {len(html)}")
            # look for apk links or api links
            apks = re.findall(r'https?://[^\s"\'<>]+\.apk', html)
            print("  APKs:", apks[:3])
    except Exception as e:
        print(f"FAIL {s}: {e}")
