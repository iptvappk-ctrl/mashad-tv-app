import urllib.request
from bs4 import BeautifulSoup
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36',
    'Referer': 'https://arabseed.here/'
}
url = 'https://arabseed.here/film/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86'

req = urllib.request.Request(url, headers=headers)
html = urllib.request.urlopen(req, timeout=15).read().decode('utf-8', errors='ignore')
soup = BeautifulSoup(html, 'html.parser')

print('Title:', soup.title.string if soup.title else '')

print('\n--- IFRAMES ---')
for ifr in soup.find_all('iframe'):
    print('iframe:', ifr.attrs)

print('\n--- WATCH / DOWNLOAD LINKS ---')
for a in soup.find_all('a'):
    href = a.get('href', '')
    text = a.get_text(strip=True)
    if any(k in href.lower() or k in text for k in ['watch', 'server', 'مشاهدة', 'تحميل', 'سيرفر', 'play', 'video']):
        print(f'{text[:30]} -> {href} (class: {a.get("class")})')

print('\n--- ELEMENTS WITH DATA ATTRIBUTES ---')
for el in soup.find_all(['li', 'div', 'button', 'a', 'span']):
    data = {k: v for k, v in el.attrs.items() if k.startswith('data-')}
    if data:
        print(el.name, data, el.get_text(strip=True)[:30])
