import urllib.request, xml.etree.ElementTree as ET
from datetime import datetime, timezone

feed = 'https://www.youtube.com/feeds/videos.xml?channel_id=UCJUCcJUeh0Cz2xyKwkw5Q1w'
req = urllib.request.Request(feed, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=5) as resp:
    root = ET.fromstring(resp.read())
    entries = root.findall('{http://www.w3.org/2005/Atom}entry')
    now = datetime.now(timezone.utc)
    for e in entries:
        vid = e.find('{http://www.youtube.com/xml/schemas/2015}videoId').text
        title = e.find('{http://www.w3.org/2005/Atom}title').text
        pub_str = e.find('{http://www.w3.org/2005/Atom}published').text
        pub_dt = datetime.fromisoformat(pub_str)
        diff_hours = (now - pub_dt).total_seconds() / 3600
        if diff_hours < 1:
            time_ago = 'منذ لحظات'
        elif diff_hours < 2:
            time_ago = 'منذ ساعة'
        elif diff_hours < 24:
            time_ago = f'منذ {int(diff_hours)} ساعات'
        elif diff_hours < 48:
            time_ago = 'أمس'
        else:
            time_ago = f'قبل {int(diff_hours/24)} أيام'
        print(f'{vid} | {time_ago} | {title}')
