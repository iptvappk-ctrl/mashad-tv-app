import urllib.request, re

req = urllib.request.Request('https://www.youtube.com/@beINSPORTS/playlists', headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req, timeout=10) as r:
        html = r.read().decode('utf-8', errors='ignore')
        pls = re.findall(r'PL[a-zA-Z0-9_-]{16,34}', html)
        print('Playlists:', set(pls[:10]))
except Exception as e:
    print(e)
