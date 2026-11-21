import urllib.request, ssl, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://api.github.com/search/repositories?q=yacine+tv+api&sort=updated", headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
        data = json.loads(r.read().decode())
        for item in data.get("items", [])[:8]:
            print(item["full_name"], "Updated:", item["updated_at"], "Desc:", item["description"])
except Exception as e:
    print(e)
