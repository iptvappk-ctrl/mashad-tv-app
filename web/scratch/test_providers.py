import urllib.request
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Accept-Language': 'ar,en;q=0.9',
}

def check(name, url):
    print(f"\n--- Checking {name}: {url} ---")
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=10) as r:
            html = r.read().decode('utf-8', errors='ignore')
            print(f"Status: {r.status}, HTML size: {len(html)}")

            iframes = re.findall(r'<iframe[^>]+src=["\']([^"\']+)["\']', html, re.I)
            print(f"iframes ({len(iframes)}):", iframes[:3])

            data_urls = re.findall(r'data-(?:url|embed|link|src)=["\']([^"\']+)["\']', html, re.I)
            print(f"data-url/embed/link/src ({len(data_urls)}):", data_urls[:6])

            links = re.findall(r'<a[^>]+data-url=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.I | re.DOTALL)
            print(f"a with data-url ({len(links)}):", [(l[0], re.sub('<[^<]+?>', '', l[1]).strip()[:30]) for l in links[:5]])
    except Exception as e:
        print(f"Error: {e}")

check("Akwam Servers Page", "https://akw.cam/watch/akfilm-the-dog-stars/servers")
check("WeCima Watch Page Player", "https://myweecima.beauty/play.php?vid=d5b3d7350")
