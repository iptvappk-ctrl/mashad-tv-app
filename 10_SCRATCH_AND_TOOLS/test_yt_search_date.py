import urllib.request, urllib.parse, re, json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    'Accept-Language': 'ar,en;q=0.9',
    'Cookie': 'SOCS=CAESEwgDEgk2OTU4Nzg5MjQaAmFyIAEaBgiA_K2uBg',
}
q = urllib.parse.quote('ملخص مباراة beIN SPORTS')
url = f'https://www.youtube.com/results?search_query={q}&sp=CAI%253D'
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=8) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
        m = re.search(r'var ytInitialData = ({.*?});</script>', html)
        if m:
            raw = m.group(1)
            vids = re.findall(r'"videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}\]', raw)
            print(f'Found {len(vids)} search results sorted by date:')
            for vid, title in vids[:10]:
                print(' ', vid, title)
except Exception as e:
    print('Error:', e)
