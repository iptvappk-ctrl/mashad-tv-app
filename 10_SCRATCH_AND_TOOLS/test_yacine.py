import requests, base64

base_url = "https://def001.ycnapi.com/api"
key_str = "c!xZj+N9&G@Ev@vw"
pkg = "ver3.ycntivi.off"
ua = "okhttp/4.11.0"

def test_endpoint(ep):
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
        print(f"Decrypted {ep} sample (first 300 chars):")
        print(decrypted[:300].decode("utf-8", errors="replace"))
    except Exception as e:
        print(f"Error on {ep}: {e}")

test_endpoint("categories")
test_endpoint("events")
