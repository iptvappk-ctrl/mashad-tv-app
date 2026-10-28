import urllib.request
import urllib.parse
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Accept-Language': 'ar,en;q=0.9',
    'Referer': 'https://wb49584.fasel4k.site/movie/queen-at-sea-2026-2586',
    'X-Requested-With': 'XMLHttpRequest',
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
}

data = urllib.parse.urlencode({'id': '157850'}).encode('utf-8')
req = urllib.request.Request('https://wb49584.fasel4k.site/ajax/embed', data=data, headers=headers)
with urllib.request.urlopen(req, timeout=10) as r:
    res = r.read().decode('utf-8', errors='ignore')
    print("Response from FaselHD ajax embed:")
    print(res)
