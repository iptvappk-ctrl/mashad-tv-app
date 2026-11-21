import urllib.request
import http.cookiejar
import json
import re

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
}

watch_url = 'https://arabseed.here/watch/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86'

html = opener.open(urllib.request.Request(watch_url, headers=headers), timeout=15).read().decode('utf-8', errors='ignore')

m_key = re.search(r'const\s+watchResolveKey\s*=\s*["\']([^"\']+)["\']', html)
m_csrf = re.search(r'const\s+csrfToken\s*=\s*["\']([^"\']+)["\']', html)
m_servers = re.search(r'const\s+servers\s*=\s*(\[.*?\]);', html, re.DOTALL)

watch_key = m_key.group(1)
csrf_token = m_csrf.group(1)
servers = json.loads(m_servers.group(1))

print('Using Page CSRF:', csrf_token)

for s in servers[:4]:
    post_headers = {
        'User-Agent': headers['User-Agent'],
        'Referer': watch_url,
        'Origin': 'https://arabseed.here',
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrf_token,
        'X-CSRF-TOKEN': csrf_token,
        'X-Requested-With': 'XMLHttpRequest'
    }
    post_data = {'key': watch_key, 'token': s['resolve_token'], 'human_signal': None}
    try:
        req = urllib.request.Request('https://arabseed.here/watch/resolve', data=json.dumps(post_data).encode('utf-8'), headers=post_headers, method='POST')
        res = json.loads(opener.open(req, timeout=10).read().decode('utf-8'))
        m_url = res.get('url', '')
        print(s['name'], '->', m_url)
        m_full = ('https://arabseed.here' + m_url) if m_url.startswith('/') else m_url
        txt = opener.open(urllib.request.Request(m_full, headers={'User-Agent': headers['User-Agent'], 'Referer': watch_url}), timeout=10).read().decode('utf-8', errors='ignore')
        iframes = re.findall(r'<iframe\b[^>]*src=["\']([^"\']+)["\']', txt, re.I)
        print('  IFRAME:', iframes)
    except Exception as e:
        print('  ERROR on', s['name'], ':', e)
