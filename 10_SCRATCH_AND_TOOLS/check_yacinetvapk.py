import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://yacinetvapk.app", headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
        html = r.read().decode("utf-8", errors="ignore")
        apks = re.findall(r'href=[\'"]([^\'"]+\.apk[^\'"]*)[\'"]', html)
        print("APKs:", apks)
        urls = re.findall(r'https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}[^\s"\'<>]*', html)
        for u in set(urls):
            if any(k in u for k in ["download", "apk", "yacine"]):
                print("Link:", u)
except Exception as e:
    print(e)
