import urllib.request
from bs4 import BeautifulSoup
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36'
}
url = 'https://m.myseed.tv/%d9%81%d9%8a%d9%84%d9%85-we-die-tonight-2025-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/'

req = urllib.request.Request(url, headers=headers)
html = urllib.request.urlopen(req, timeout=15).read().decode('utf-8', errors='ignore')
soup = BeautifulSoup(html, 'html.parser')

print('Title:', soup.title.string if soup.title else '')

print('\n--- IFRAMES ---')
for ifr in soup.find_all('iframe'):
    print('iframe:', ifr.attrs)

print('\n--- WATCH / DOWNLOAD / SERVER BUTTONS ---')
for a in soup.find_all(['a', 'button', 'li']):
    href = a.get('href', '')
    text = a.get_text(strip=True)
    classes = ' '.join(a.get('class', []))
    data_attrs = {k: v for k, v in a.attrs.items() if k.startswith('data-')}
    if any(k in href.lower() or k in text for k in ['watch', 'server', 'مشاهدة', 'تحميل', 'سيرفر', 'play', 'video']) or data_attrs:
        print(f'[{a.name}] text: "{text[:30]}" | href: {href} | class: {classes} | data: {data_attrs}')

print('\n--- SCRIPTS WITH EMBED / PLAYER ---')
for s in soup.find_all('script'):
    src = s.get('src', '')
    content = s.string or ''
    if src:
        if any(k in src.lower() for k in ['player', 'embed', 'video', 'watch', 'custom']):
            print('script src:', src)
    if content:
        for match in re.findall(r'(?:https?:)?//[^\s"\']+(?:m3u8|embed|player|watch|iframe)[^\s"\']*', content, re.I):
            print('match in script:', match)
