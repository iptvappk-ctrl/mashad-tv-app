import requests
import urllib3
import re
from bs4 import BeautifulSoup

urllib3.disable_warnings()

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    'Referer': 'https://m.myseed.tv/'
}

url = 'https://m.myseed.tv/%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d9%86%d8%b5%d9%81-%d8%a7%d9%84%d8%a3%d9%85-s1-eps6/'
r = requests.get(url, headers=headers, verify=False, timeout=15)
soup = BeautifulSoup(r.text, 'html.parser')

print("Page status:", r.status_code)

# Collect episodes with episodeSelectors:
episodeSelectors = [
    "a.episode__item", ".episode__item a",
    "a[href*='%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9']",
    "a[href*='الحلقة']", "a[href*='-episode-']", "a[href*='/episode/']",
    ".Episodes--List a", ".EpisodeBlock a",
    ".episodes-list a", ".EpisodesList li a", ".eplister li a",
    ".eps-list li a", ".eps a", "a.ep-item", ".episodios li a", ".eplist a"
]

eps = []
for sel in episodeSelectors:
    found = soup.select(sel)
    if found:
        print(f"Selector [{sel}] matched {len(found)} elements")
        for el in found:
            href = el.get('href', '')
            txt = el.get_text(strip=True)
            eps.append((txt, href))

print(f"Total raw episodes found on episode page: {len(eps)}")

# Check parents/breadcrumbs
seriesLinkSelectors = [
    "a[href*='/series/']", "a[href*='/season/']", "a[href*='الموسم']",
    ".breadcrumb a", "ol.breadcrumb a", ".SeriesTitle a", ".series-title a",
    ".entry-breadcrumb a", "a.SeriesLink"
]
parents = []
for sel in seriesLinkSelectors:
    for el in soup.select(sel):
        h = el.get('href', '')
        t = el.get_text(strip=True)
        if h and h != url:
            parents.append((t, h))
print("Parents/Breadcrumbs found:", parents)
