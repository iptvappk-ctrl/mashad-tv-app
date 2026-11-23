import urllib.request, ssl, urllib.parse

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = "http://re300.new-redirect.online/live/918454578001/index.m3u8?t=viC6cnPU8YyGD4fZrhsq5Q&e=1795370386"
ref = "http://yacinneeee.live\u2060\u200d\u2060\u3164\u200c\u2060\u200d\u2060"
clean_ref = "".join(ch for ch in ref if 0x20 <= ord(ch) < 0x7F).strip()

candidates = [
    ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36", ref, False),
    ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36", clean_ref, False),
    ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36", clean_ref, True),
    ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36", "", False),
    ("Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36", clean_ref, False),
    ("Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36", "", False),
    ("YTV Player", "", False),
]

for i, (ua, r, orig) in enumerate(candidates):
    hdrs = {"User-Agent": ua}
    if r:
        hdrs["Referer"] = r.encode("utf-8").decode("latin-1")
    if orig and r:
        hdrs["Origin"] = "http://yacinneeee.live"
    try:
        req = urllib.request.Request(url, headers=hdrs)
        with urllib.request.urlopen(req, context=ctx, timeout=5) as resp:
            print(f"Cand {i} SUCCESS! Status {resp.status}")
            print(resp.read().decode("utf-8", errors="ignore")[:200])
            break
    except urllib.error.HTTPError as e:
        print(f"Cand {i} HTTP {e.code}")
    except Exception as e:
        print(f"Cand {i} ERR: {e}")
