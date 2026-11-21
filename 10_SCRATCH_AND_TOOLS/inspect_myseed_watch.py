import urllib.request
from bs4 import BeautifulSoup
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36',
    'Referer': 'https://m.myseed.tv/%d9%81%d9%8a%d9%84%d9%85-we-die-tonight-2025-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/'
}
url = 'https://m.myseed.tv/%d9%81%d9%8a%d9%84%d9%85-we-die-tonight-2025-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/watch/'

req = urllib.request.Request(url, headers=headers)
html = urllib.request.urlopen(req, timeout=15).read().decode('utf-8', errors='ignore')
soup = BeautifulSoup(html, 'html.parser')

print('Title:', soup.title.string if soup.title else '')

print('\n--- IFRAMES ---')
for ifr in soup.find_all('iframe'):
    print('iframe attrs:', ifr.attrs)

print('\n--- SERVERS / PLAYERS LIST ---')
for el in soup.find_all(['li', 'div', 'button', 'a'], attrs={'data-url': True}) + \
          soup.find_all(['li', 'div', 'button', 'a'], attrs={'data-src': True}) + \
          soup.find_all(['li', 'div', 'button', 'a'], attrs={'data-server': True}) + \
          soup.find_all(['li', 'div', 'button', 'a'], attrs={'data-embed': True}) + \
          soup.find_all(['li', 'div', 'button', 'a'], attrs={'data-link': True}):
    print(el.name, 'attrs:', el.attrs, 'text:', el.get_text(strip=True))

print('\n--- ELEMENTS WITH CLASS SERVER / WATCH ---')
for el in soup.find_all(class_=re.compile(r'server|watch|player|embed', re.I)):
    if el.name in ['ul', 'div', 'section']:
        print(f'{el.name}.{el.get("class")}')
        for child in el.find_all(['li', 'button', 'a', 'span']):
            print('   child:', child.name, child.attrs, child.get_text(strip=True))

print('\n--- SCRIPTS ---')
for s in soup.find_all('script'):
    src = s.get('src', '')
    content = s.string or ''
    if 'server' in content.lower() or 'watch' in content.lower() or 'source' in content.lower():
        print('Script match snippet:')
        for line in content.split('\n'):
            if any(k in line.lower() for k in ['server', 'url', 'embed', 'player', 'ajax', 'post', 'get']):
                print('  ', line[:120])
