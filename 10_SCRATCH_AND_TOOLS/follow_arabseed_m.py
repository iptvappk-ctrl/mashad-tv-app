import urllib.request
import http.cookiejar
import json
import re

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

headers = {'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36'}
watch_url = 'https://arabseed.here/watch/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86'

html = opener.open(urllib.request.Request(watch_url, headers=headers), timeout=15).read().decode('utf-8', errors='ignore')

m_key = re.search(r'const\s+watchResolveKey\s*=\s*["\']([^"\']+)["\']', html)
m_csrf = re.search(r'const\s+csrfToken\s*=\s*["\']([^"\']+)["\']', html)
m_servers = re.search(r'const\s+servers\s*=\s*(\[.*?\]);', html, re.DOTALL)

watch_key = m_key.group(1)
csrf_token = m_csrf.group(1)
servers = json.loads(m_servers.group(1))

post_headers = {
    'User-Agent': headers['User-Agent'],
    'Referer': watch_url,
    'Origin': 'https://arabseed.here',
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'X-CSRF-TOKEN': csrf_token,
    'X-Requested-With': 'XMLHttpRequest'
}

for s in servers[:5]:
    post_data = {'key': watch_key, 'token': s['resolve_token'], 'human_signal': None}
    req = urllib.request.Request('https://arabseed.here/watch/resolve', data=json.dumps(post_data).encode('utf-8'), headers=post_headers, method='POST')
    res = json.loads(opener.open(req, timeout=10).read().decode('utf-8'))
    m_url = res.get('url', '')
    print(s['name'], '->', m_url)
    m_full = ('https://arabseed.here' + m_url) if m_url.startswith('/') else m_url
    try:
        m_req = urllib.request.Request(m_full, headers={'User-Agent': headers['User-Agent'], 'Referer': watch_url})
        m_resp = opener.open(m_req, timeout=10)
        print('  FETCH STATUS:', m_resp.status, 'FINAL URL:', m_resp.geturl())
        txt = m_resp.read().decode('utf-8', errors='ignore')
        iframes = re.findall(r'<iframe\b[^>]*src=["\']([^"\']+)["\']', txt, re.I)
        for ifr in iframes:
            print('  -> IFRAME SRC:', ifr)
        scripts = re.findall(r'<script\b[^>]*>(.*?)</script>', txt, re.DOTALL)
        for sc in scripts:
            for l in sc.split('\n'):
                if any(k in l.lower() for k in ['location', 'eval', 'player', 'm3u8', 'mp4', 'src']):
                    print('  -> SCRIPT:', l.strip()[:100])
    except Exception as e:
        print('  M ERROR:', e)
