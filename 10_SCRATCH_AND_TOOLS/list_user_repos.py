import urllib.request, json, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request('https://api.github.com/users/iptvappk-ctrl/repos', headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=6) as resp:
        repos = json.loads(resp.read().decode())
        print(f'Total repos for iptvappk-ctrl: {len(repos)}')
        for r in repos:
            print(f"- {r['name']}: {r['html_url']}")
except Exception as e:
    print('Error:', e)
