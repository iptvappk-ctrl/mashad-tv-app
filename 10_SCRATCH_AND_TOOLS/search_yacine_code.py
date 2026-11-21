import urllib.request, ssl, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://api.github.com/search/code?q=c!xZj%2BN9%26G%40Ev%40vw", headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
        data = json.loads(r.read().decode())
        print("Total count:", data.get("total_count"))
        for item in data.get("items", [])[:10]:
            print(item["repository"]["full_name"], "->", item["path"])
except Exception as e:
    print(e)
