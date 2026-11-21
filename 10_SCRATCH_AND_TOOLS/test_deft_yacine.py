import urllib.request, ssl, base64, time, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

key = 'c!xZj+N9&G@Ev@vw'

def decrypt(enc, k):
    try:
        enc = base64.b64decode(enc.encode('ascii')).decode('ascii', errors='ignore')
        return ''.join(chr(ord(c) ^ ord(k[i % len(k)])) for i, c in enumerate(enc))
    except Exception as e:
        return f"ERR: {e}"

endpoints = ['categories', 'events', 'channel/1', 'channel/2', 'channel/3', 'channel/4', 'tw_key?stream_name=test']

for ep in endpoints:
    url = f"https://deft.yacinelive.com/api/{ep}"
    try:
        req = urllib.request.Request(url, headers={
            'User-Agent': 'okhttp/4.11.0',
            'x-android-package': 'ver3.ycntivi.off',
            'Accept': 'application/json'
        })
        with urllib.request.urlopen(req, context=ctx, timeout=6) as r:
            body = r.read().decode('utf-8', errors='ignore')
            t = r.headers.get('T', r.headers.get('t', ''))
            print(f"=== {ep} (status {r.status}) ===")
            print(f"t header: '{t}', body len: {len(body)}")
            dec = decrypt(body, key + t)
            print(f"Decrypted: {dec[:300]}")
    except Exception as e:
        print(f"FAIL {ep}: {e}")
