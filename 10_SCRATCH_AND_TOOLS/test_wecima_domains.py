import urllib.request
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

urls = [
    'https://wecima.tips/',
    'https://mywecima.shop/',
    'https://mywecima1.best/',
    'https://wecima.cam/',
    'https://wecima.show/',
    'https://wecima.tube/'
]

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36'
}

for u in urls:
    try:
        req = urllib.request.Request(u, headers=headers)
        with urllib.request.urlopen(req, timeout=8, context=ctx) as resp:
            content = resp.read().decode('utf-8', errors='ignore')
            title = 'No Title'
            if '<title>' in content:
                title = content.split('<title>')[1].split('</title>')[0].strip()
            print(f'{u} -> {resp.status} (Length: {len(content)}) Title: {title[:40]}')
    except Exception as e:
        print(f'{u} -> ERROR: {e}')
