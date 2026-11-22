import urllib.request, socket

for host in ["tv.variety-buy.store", "def777.ycnapi.com"]:
    try:
        ip = socket.gethostbyname(host)
        print(f"Resolved {host} to: {ip}")
    except Exception as e:
        print(f"DNS failed for {host}: {e}")

# Test tv.variety-buy.store
for endpoint in ["events", "categories", "api/events", "api/categories"]:
    url = f"https://tv.variety-buy.store/{endpoint}"
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "okhttp/4.11.0",
            "x-android-package": "ver3.ycntivi.off",
            "Accept": "application/json"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            print(f"Success for {url}: status {resp.status}")
            t = resp.headers.get("T") or resp.headers.get("t")
            print("  T header:", t)
            body = resp.read()
            print("  Body len:", len(body))
    except Exception as e:
        print(f"Failed {url}: {e}")
