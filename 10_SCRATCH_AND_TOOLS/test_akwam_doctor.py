import urllib.request, urllib.parse, re

url = 'https://akwam.to/search?q=' + urllib.parse.quote('Doctor X')
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
try:
    with urllib.request.urlopen(req, timeout=10) as r:
        html = r.read().decode('utf-8', errors='ignore')
    pattern = re.compile(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', re.DOTALL)
    links = pattern.findall(html)
    print(f'Total links: {len(links)}')
    for href, txt in links:
        txt = re.sub(r'<[^>]+>', '', txt).strip()
        if 'doctor' in href.lower() or 'doctor' in txt.lower():
            print(f'HREF: {href} | TXT: {txt}')
except Exception as e:
    print('Error:', e)
