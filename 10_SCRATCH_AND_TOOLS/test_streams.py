import requests

urls = [
    "https://bein-xtra-bein.amagi.tv/playlist.m3u8",
    "https://tvsen6.aynaott.com/beinsports/index.m3u8",
    "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8",
    "https://5c7b683162943.streamlock.net/live/ngrp:sportsone_all/playlist.m3u8"
]

for u in urls:
    try:
        r = requests.get(u, headers={"User-Agent": "Mozilla/5.0"}, timeout=5)
        print(u, "->", r.status_code, "len:", len(r.content))
    except Exception as e:
        print(u, "-> FAIL:", e)
