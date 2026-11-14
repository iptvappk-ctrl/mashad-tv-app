import urllib.request, re

watch_url = 'https://m.myseed.tv/%d9%81%d9%8a%d9%84%d9%85-%d8%b9%d8%b3%d9%84-%d8%a3%d8%b3%d9%88%d8%af-2010-%d8%a7%d9%88%d9%86%d9%84%d8%a7%d9%8a%d9%86/watch/'
ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'

# 1. Fetch fresh watch page
req0 = urllib.request.Request(watch_url, headers={'User-Agent': ua})
with urllib.request.urlopen(req0, timeout=10) as resp0:
    html0 = resp0.read().decode('utf-8')
m0 = re.search(r'vids\.php\?t=([a-f0-9]+)', html0)
if not m0:
    print('No token in watch page!')
    exit(1)

token = m0.group(1)
print('Got token:', token)
vids_url = f'https://m.myseed.tv/vids.php?t={token}'

# 2. Fetch vids.php
req1 = urllib.request.Request(vids_url, headers={'User-Agent': ua, 'Referer': watch_url})
with urllib.request.urlopen(req1, timeout=10) as resp1:
    body1 = resp1.read().decode('utf-8')

m1 = re.search(r'src=["\']([^"\']+player-gateway[^"\']+)["\']', body1)
if not m1:
    print('No player-gateway in vids.php!')
    exit(1)

gw_url = m1.group(1).replace('&amp;', '&')
print('Got gateway url:', gw_url)

# 3. Fetch player-gateway.php
req2 = urllib.request.Request(gw_url, headers={'User-Agent': ua, 'Referer': vids_url})
with urllib.request.urlopen(req2, timeout=10) as resp2:
    body2 = resp2.read().decode('utf-8')

vm = re.search(r'<source\s+[^>]*src=["\']([^"\']+\.(?:mp4|m3u8)(?:\?[^"\']*)?)["\']', body2)
if vm:
    print('SUCCESS! Direct stream:', vm.group(1))
else:
    print('Body preview:', body2[:300])
