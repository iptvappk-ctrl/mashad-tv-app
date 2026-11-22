import urllib.request, re, json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    'Accept-Language': 'ar,en;q=0.9',
}
url = 'https://www.youtube.com/@beINSPORTS/videos'
try:
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
        print('Final URL:', resp.geturl())
        match = re.search(r'var ytInitialData = ({.*?});</script>', html)
        if match:
            raw_json = match.group(1)
            print('Found ytInitialData len:', len(raw_json))
            # find videoId and title
            videos = re.findall(r'"videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}\]', raw_json)
            print(f'Found {len(videos)} videos!')
            for vid, title in videos[:15]:
                print(f'{vid} -> {title}')
        else:
            print('No ytInitialData. Maybe consent page?')
            if 'consent.youtube.com' in resp.geturl() or 'consent' in html:
                print('Hit consent page!')
except Exception as e:
    print('Exception:', e)
