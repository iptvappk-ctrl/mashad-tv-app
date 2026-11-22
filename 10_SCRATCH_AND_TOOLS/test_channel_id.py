import urllib.request, re

req = urllib.request.Request('https://www.youtube.com/@beINSPORTS', headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=10) as r:
    html = r.read().decode('utf-8', errors='ignore')
    m = re.findall(r'UC[a-zA-Z0-9_-]{22}', html)
    print('Found UC IDs:', set(m))
