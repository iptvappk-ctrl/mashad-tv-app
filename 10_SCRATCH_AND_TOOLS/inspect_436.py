import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = "https://deft.yacinelive.com/api/categories"
req = urllib.request.Request(url, headers={
    'User-Agent': 'okhttp/4.11.0',
    'x-android-package': 'ver3.ycntivi.off',
    'Accept': 'application/json'
})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=6) as r:
        print("Success:", r.status, r.read())
except urllib.error.HTTPError as e:
    print("HTTPError:", e.code)
    print("Headers:", dict(e.headers))
    print("Body:", e.read()[:500])
except Exception as e:
    print("Other error:", e)
