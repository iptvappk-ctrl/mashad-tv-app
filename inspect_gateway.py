import urllib.request
import re
from bs4 import BeautifulSoup

url = 'https://d.myseed.tv/player-gateway.php?src=myseed&front=m.myseed.tv&t=f6be9a58aad9844c1eb7485ac6909a3e7b9d4d5cb506517e621315769e293f42'
ref = 'https://m.myseed.tv/'
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
    'Referer': ref
})
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        content = resp.read().decode('utf-8', errors='ignore')
    soup = BeautifulSoup(content, 'html.parser')
    print("Scripts count:", len(soup.find_all('script')))
    for s in soup.find_all('script'):
        src = s.get('src')
        if src:
            print("Script src:", src)
        else:
            text = s.string or ""
            if "eval" in text or "player" in text.lower() or "source" in text.lower() or "file" in text.lower():
                print("Inline script snippet:", text[:300], "\n---")
    for iframe in soup.find_all('iframe'):
        print("Iframe:", iframe)
    for video in soup.find_all('video'):
        print("Video:", video)
except Exception as e:
    print('Error:', e)
