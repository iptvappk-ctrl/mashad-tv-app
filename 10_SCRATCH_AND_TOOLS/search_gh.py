import urllib.request, json, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

token = 'REDACTED_TOKEN'
query = 'bein+sports+m3u8'
url = f'https://api.github.com/search/repositories?q={query}&sort=updated&order=desc&per_page=10'

req = urllib.request.Request(url, headers={
    'Authorization': f'token {token}',
    'User-Agent': 'Mozilla/5.0',
    'Accept': 'application/vnd.github.v3+json'
})

try:
    with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
        data = json.loads(r.read().decode())
        print(f"Total count: {data.get('total_count')}")
        for item in data.get('items', [])[:8]:
            print(f"- {item['full_name']}: {item['description']} ({item['html_url']})")
except Exception as e:
    print("Error:", e)
