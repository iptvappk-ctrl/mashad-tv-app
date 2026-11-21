import urllib.request
import json

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36',
    'Referer': 'https://arabseed.here/watch/%D9%81%D9%8A%D9%84%D9%85-unbearable-christmas-2026-%D9%85%D8%AA%D8%B1%D8%AC%D9%85-%D8%A7%D9%88%D9%86-%D9%84%D8%A7%D9%8A%D9%86',
    'Origin': 'https://arabseed.here',
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'X-CSRF-TOKEN': 'IPvZZqxbLwV3j13oUMkil5mH0vTlUDDgb7dRYVug',
    'X-Requested-With': 'XMLHttpRequest'
}

data = {
    'key': '269e5b17fd15ed98743c07b3af7637cda51e4052a871025f',
    'token': 'd16dae5ce3fd1f97d6f709e789a9f76d4ddd823070d10f32',
    'human_signal': None
}

req = urllib.request.Request(
    'https://arabseed.here/watch/resolve',
    data=json.dumps(data).encode('utf-8'),
    headers=headers,
    method='POST'
)

try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        print('STATUS:', resp.status)
        print('RESPONSE:', resp.read().decode('utf-8'))
except Exception as e:
    print('ERROR:', e)
