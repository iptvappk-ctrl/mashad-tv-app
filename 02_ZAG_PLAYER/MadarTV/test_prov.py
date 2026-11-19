import requests
import urllib3
import re
from bs4 import BeautifulSoup

urllib3.disable_warnings()

# Let's inspect the catalog item from Arabseed & MultiProvider
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'ar,en-US;q=0.9,en;q=0.8'
}

print("Testing Arabseed direct fetch:")
url = 'https://m.myseed.tv/%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d9%86%d8%b5%d9%81-%d8%a7%d9%84%d8%a3%d9%85-s1-eps6/'
try:
    r = requests.get(url, headers=headers, timeout=10, verify=False)
    print("Arabseed status:", r.status_code)
except Exception as e:
    print("Arabseed error:", e)

print("\nTesting Esheeq fetch:")
esheeq_url = 'https://e.q-ask.video/video-anne-yarisi-e06/'
try:
    r = requests.get(esheeq_url, headers=headers, timeout=10, verify=False)
    print("Esheeq status:", r.status_code)
except Exception as e:
    print("Esheeq error:", e)
