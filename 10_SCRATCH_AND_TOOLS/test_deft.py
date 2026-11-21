import requests, base64

base_url = "https://deft.yacinelive.com/api"
key_str = "c!xZj+N9&G@Ev@vw"
pkg = "ver3.ycntivi.off"
ua = "okhttp/4.11.0"

for ep in ["categories", "events"]:
    url = f"{base_url}/{ep}"
    headers = {
        "User-Agent": ua,
        "Accept": "application/json",
        "x-android-package": pkg
    }
    try:
        r = requests.get(url, headers=headers, timeout=10)
        print(f"Status for {ep}: {r.status_code}")
        t = r.headers.get("T") or r.headers.get("t") or ""
        body = r.text
        print(f"Header T: {t}, Body length: {len(body)}")
        
        full_key = (key_str + t).encode("utf-8")
        decoded_b64 = base64.b64decode(body.strip())
        decrypted = bytes([decoded_b64[i] ^ full_key[i % len(full_key)] for i in range(len(decoded_b64))])
        print(f"Decrypted {ep} sample (first 400 chars):")
        print(decrypted[:400].decode("utf-8", errors="replace"))
    except Exception as e:
        print(f"Error on {ep}: {e}")
