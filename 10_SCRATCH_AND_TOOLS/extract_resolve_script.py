import urllib.request
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36',
    'Referer': 'https://arabseed.here/film/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86'
}
url = 'https://arabseed.here/watch/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86'
req = urllib.request.Request(url, headers=headers)
html = urllib.request.urlopen(req, timeout=15).read().decode('utf-8', errors='ignore')

for idx, s in enumerate(re.finditer(r'<script\b[^>]*>(.*?)</script>', html, re.DOTALL)):
    content = s.group(1)
    if 'resolveUrl' in content:
        print(f'=== SCRIPT {idx} ===')
        print(content)
