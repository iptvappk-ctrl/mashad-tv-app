import requests
import urllib3
from bs4 import BeautifulSoup
import sys

urllib3.disable_warnings()

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36'}
url = 'https://m.myseed.tv/%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d9%86%d8%b5%d9%81-%d8%a7%d9%84%d8%a3%d9%85-s1-eps6/'
r = requests.get(url, headers=headers, verify=False, timeout=15)
soup = BeautifulSoup(r.text, 'html.parser')

print("Checking paragraphs on episode page:")
for p in soup.find_all('p'):
    t = p.get_text(strip=True)
    if len(t) > 20:
        parent_cls = p.parent.get('class', [])
        p_cls = p.get('class', [])
        print(f"[{p_cls} in {parent_cls}]: {t}")

# Also check series parent page
series_url = 'https://m.myseed.tv/selary/%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d9%86%d8%b5%d9%81-%d8%a7%d9%84%d8%a3%d9%85-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/'
r_s = requests.get(series_url, headers=headers, verify=False, timeout=15)
soup_s = BeautifulSoup(r_s.text, 'html.parser')
print("\nChecking paragraphs on series page:")
for p in soup_s.find_all('p'):
    t = p.get_text(strip=True)
    if len(t) > 20:
        parent_cls = p.parent.get('class', [])
        p_cls = p.get('class', [])
        print(f"[{p_cls} in {parent_cls}]: {t}")
