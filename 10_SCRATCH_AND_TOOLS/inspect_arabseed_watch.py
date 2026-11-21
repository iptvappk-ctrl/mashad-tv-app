import urllib.request
from bs4 import BeautifulSoup
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36',
    'Referer': 'https://arabseed.here/film/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86'
}
url = 'https://arabseed.here/watch/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86'

req = urllib.request.Request(url, headers=headers)
html = urllib.request.urlopen(req, timeout=15).read().decode('utf-8', errors='ignore')
soup = BeautifulSoup(html, 'html.parser')

print('Title:', soup.title.string if soup.title else '')

print('\n--- IFRAMES ---')
for ifr in soup.find_all('iframe'):
    print('iframe attrs:', ifr.attrs)

print('\n--- SERVERS / PLAYERS LIST ---')
for el in soup.find_all(['li', 'div', 'button', 'a']):
    data = {k: v for k, v in el.attrs.items() if k.startswith('data-')}
    if any(k in el.get('class', []) for k in ['server', 'active']) or data:
        print(el.name, el.get('class'), data, el.get_text(strip=True)[:40])

print('\n--- ALL A LINKS IN SERVERS SECTION ---')
for a in soup.select('.servers a, ul.servers li, .watch-servers a, .server a, a.server'):
    print(a.attrs, a.get_text(strip=True))

print('\n--- SCRIPTS ---')
for s in soup.find_all('script'):
    content = s.string or ''
    if any(k in content for k in ['iframe', 'server', 'player', 'ajax', 'm3u8', 'mp4']):
        print('Script match snippet:')
        for line in content.split('\n'):
            if any(k in line for k in ['src', 'url', 'server', 'http', 'fetch', 'post']):
                print('  ', line[:120])
