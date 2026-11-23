import urllib.request, ssl, base64, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

MASTER_KEY_PREFIX = "c!xZj+N9&G@Ev@vw"

def pk_dec(b64_str, key):
    raw = base64.b64decode(b64_str.strip())
    full_key = (MASTER_KEY_PREFIX + key).encode("utf-8")
    klen = len(full_key)
    res = bytes(b ^ full_key[i % klen] for i, b in enumerate(raw))
    return res.decode("utf-8", errors="replace")

def get(ep):
    url = f"https://def777.ycnapi.com/api/{ep}"
    req = urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": "okhttp/4.12.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
        t = r.headers.get("t") or r.headers.get("T") or ""
        return json.loads(pk_dec(r.read().decode("utf-8"), t))

cats = get("categories")
print("=== CATEGORIES ===")
for c in cats.get("data", []):
    print(c["id"], c["name"])

events = get("events")
print("\n=== REAL LIVE EVENTS TODAY ===")
for ev in events.get("data", [])[:10]:
    t1 = ev.get("team_1", {}).get("name")
    t2 = ev.get("team_2", {}).get("name")
    ch = ev.get("channel")
    champ = ev.get("champions")
    eid = ev.get("id")
    print(f"{eid} | {champ} | {ch} | {t1} vs {t2}")

# Now let's fetch channels for the first category
if cats.get("data"):
    cat0 = cats["data"][0]["id"]
    channels = get(f"categories/{cat0}/channels")
    print(f"\n=== CHANNELS for category {cat0} ({cats['data'][0]['name']}) ===")
    for ch in channels.get("data", [])[:15]:
        print(ch["id"], ch["name"])

    # And let's fetch streams for the first channel!
    if channels.get("data"):
        ch0_id = channels["data"][0]["id"]
        ch0_name = channels["data"][0]["name"]
        streams = get(f"channel/{ch0_id}")
        print(f"\n=== REAL STREAMS for {ch0_name} (id {ch0_id}) ===")
        print(json.dumps(streams, indent=2, ensure_ascii=False))

# And let's test config endpoint
cfg = get("config")
print("\n=== CONFIG ===")
print(json.dumps(cfg, indent=2, ensure_ascii=False))
