import requests, base64

candidates = [
    "http://ver3.yacinelive.com/api",
    "https://ver3.yacinelive.com/api",
    "http://yacinelive.com/api",
    "https://yacinelive.com/api",
    "http://yacineapp.tv/api",
    "https://yacineapp.tv/api",
    "http://app.yacine-tv.net/api",
    "https://app.yacine-tv.net/api"
]

key_str = "c!xZj+N9&G@Ev@vw"
pkg = "ver3.ycntivi.off"
ua = "okhttp/4.11.0"

for base in candidates:
    url = f"{base}/categories"
    try:
        r = requests.get(url, headers={"User-Agent": ua, "Accept": "application/json", "x-android-package": pkg}, timeout=5)
        print(f"Testing {url} -> Status: {r.status_code}")
        if r.status_code == 200:
            t = r.headers.get("T") or r.headers.get("t") or ""
            full_key = (key_str + t).encode("utf-8")
            decoded_b64 = base64.b64decode(r.text.strip())
            decrypted = bytes([decoded_b64[i] ^ full_key[i % len(full_key)] for i in range(len(decoded_b64))])
            print(f"SUCCESS! Decrypted from {base}: {decrypted[:200].decode('utf-8', errors='replace')}")
            break
    except Exception as e:
        print(f"Fail {url}: {e}")
