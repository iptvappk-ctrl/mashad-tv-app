import urllib.request, re

def resolve_myseed(movie_url):
    ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
    watch_url = movie_url.rstrip('/') + '/watch/'
    print('1. Fetching watch page:', watch_url)
    req0 = urllib.request.Request(watch_url, headers={'User-Agent': ua})
    with urllib.request.urlopen(req0, timeout=10) as resp0:
        html0 = resp0.read().decode('utf-8')
    m0 = re.search(r'vids\.php\?t=([a-f0-9]+)', html0)
    if not m0: return 'No token'
    token = m0.group(1)
    print('2. Fresh token:', token)
    vids_url = f'https://m.myseed.tv/vids.php?t={token}'
    req1 = urllib.request.Request(vids_url, headers={'User-Agent': ua, 'Referer': watch_url})
    with urllib.request.urlopen(req1, timeout=10) as resp1:
        body1 = resp1.read().decode('utf-8')
    m1 = re.search(r'src=["\']([^"\']+player-gateway[^"\']+)["\']', body1)
    if not m1: return 'No gateway'
    gw_url = m1.group(1).replace('&amp;', '&')
    print('3. Gateway URL:', gw_url)
    req2 = urllib.request.Request(gw_url, headers={'User-Agent': ua, 'Referer': vids_url})
    with urllib.request.urlopen(req2, timeout=10) as resp2:
        body2 = resp2.read().decode('utf-8')
    vm = re.search(r'<source\s+[^>]*src=["\']([^"\']+\.(?:mp4|m3u8)(?:\?[^"\']*)?)["\']', body2)
    if vm: return vm.group(1)
    return 'No video source'

movie = 'https://m.myseed.tv/%d9%81%d9%8a%d9%84%d9%85-%d8%b9%d8%b3%d9%84-%d8%a3%d8%b3%d9%88%d8%af-2010-%d8%a7%d9%88%d9%86%d9%84%d8%a7%d9%8a%d9%86/'
print('RESULT:', resolve_myseed(movie))
