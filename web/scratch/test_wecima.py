import urllib.request
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Accept-Language': 'ar,en;q=0.9',
    'Referer': 'https://myweecima.beauty/watch.php?vid=25189b1bc'
}

url = "https://myweecima.beauty/downloads.php?vid=25189b1bc"
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=10) as r:
    html = r.read().decode('utf-8', errors='ignore')
    links = re.findall(r'<a[^>]+href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.I | re.DOTALL)
    print("Download links in WeCima:")
    for l in links:
        if 'http' in l[0] and not 'myweecima' in l[0]:
            print(" ->", l[0], re.sub('<[^<]+?>', '', l[1]).strip())
