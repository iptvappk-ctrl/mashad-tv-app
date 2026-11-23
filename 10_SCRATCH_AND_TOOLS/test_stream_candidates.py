import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

candidates = [
    # beIN 1
    ("beIN 1 tvsen6", "https://tvsen6.aynaott.com/beinsports1/index.m3u8"),
    ("beIN 1 aynaott", "https://aynaott.com/beinsports1/index.m3u8"),
    ("beIN 1 shop", "https://vg162.adcdsd123321.shop/7.m3u8"),
    # beIN XTRA
    ("beIN XTRA cloudfront", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8"),
    ("beIN XTRA espanol", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8"),
    ("beIN XTRA amagi", "https://bein-xtra-bein.amagi.tv/playlist.m3u8"),
    ("beIN XTRA firetv", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8"),
    # beIN News / FTA
    ("beIN News FTA", "https://bein-fta-bein.amagi.tv/playlist.m3u8"),
    ("beIN News youtube", "https://www.youtube.com/watch?v=2e0o-Wq-k8M"),
]

for label, url in candidates:
    try:
        req = urllib.request.Request(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://yacinelive.com/'
        })
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            body = r.read(200)
            print(f"OK ({r.status}): {label} -> {len(body)} bytes: {body[:60]}")
    except Exception as e:
        print(f"FAIL: {label} -> {e}")
