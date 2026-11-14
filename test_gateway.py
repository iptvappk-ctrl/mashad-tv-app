import urllib.request
import re

vids_url = 'https://m.myseed.tv/vids.php?t=f6be9a58aad9844c1eb7485ac6909a3e7b9d4d5cb506517e621315769e293f42'
watch_url = 'https://m.myseed.tv/%d9%81%d9%8a%d9%84%d9%85-%d8%b9%d8%b3%d9%84-%d8%a3%d8%b3%d9%88%d8%af-2010-%d8%a7%d9%88%d9%86%d9%84%d8%a7%d9%8a%d9%86/watch/'

cookie_jar = urllib.request.HTTPCookieProcessor()
opener = urllib.request.build_opener(cookie_jar)

req = urllib.request.Request(vids_url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
    'Referer': watch_url
})
try:
    resp = opener.open(req)
    print('vids.php:', resp.status)
    content = resp.read().decode('utf-8')
    m = re.search(r'src=["\']([^"\']+player-gateway[^"\']+)["\']', content)
    if m:
        gw_url = m.group(1).replace('&amp;', '&')
        print('Gateway URL:', gw_url)
        gw_req = urllib.request.Request(gw_url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
            'Referer': vids_url
        })
        gw_resp = opener.open(gw_req)
        print('Gateway resp:', gw_resp.status)
        gw_content = gw_resp.read().decode('utf-8')
        vm = re.search(r'<source\s+src=["\']([^"\']+)["\']', gw_content)
        if vm:
            print('Direct video URL:', vm.group(1))
        else:
            print('No source tag! Preview:', gw_content[:500])
except Exception as e:
    print('Error:', e)
