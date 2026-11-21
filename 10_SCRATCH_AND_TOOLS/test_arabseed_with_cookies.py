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

req1 = urllib.request.Request(watch_url, headers=headers)
resp1 = opener.open(req1, timeout=15)
html = resp1.read().decode('utf-8', errors='ignore')

print('Step 1 cookies:')
for c in cj:
    print(' ', c.name, '=', c.value[:20] + '...')

# Extract fresh tokens
m_key = re.search(r'const\s+watchResolveKey\s*=\s*["\']([^"\']+)["\']', html)
m_csrf = re.search(r'const\s+csrfToken\s*=\s*["\']([^"\']+)["\']', html)
m_servers = re.search(r'const\s+servers\s*=\s*(\[.*?\]);', html, re.DOTALL)

if m_key and m_csrf and m_servers:
    watch_key = m_key.group(1)
    csrf_token = m_csrf.group(1)
    servers = json.loads(m_servers.group(1))
    print(f'Found {len(servers)} servers. Resolving server 0: {servers[0]["name"]}')
    
    post_headers = {
        'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36',
        'Referer': watch_url,
        'Origin': 'https://arabseed.here',
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrf_token,
        'X-Requested-With': 'XMLHttpRequest'
    }
    
    post_data = {
        'key': watch_key,
        'token': servers[0]['resolve_token'],
        'human_signal': None
    }
    
    req2 = urllib.request.Request(
        'https://arabseed.here/watch/resolve',
        data=json.dumps(post_data).encode('utf-8'),
        headers=post_headers,
        method='POST'
    )
    
    try:
        resp2 = opener.open(req2, timeout=10)
        print('RESOLVE STATUS:', resp2.status)
        res_json = json.loads(resp2.read().decode('utf-8'))
        print('RESOLVE RESULT:', res_json)
    except Exception as e:
        print('RESOLVE ERROR:', e)
else:
    print('Failed to extract tokens from HTML')
