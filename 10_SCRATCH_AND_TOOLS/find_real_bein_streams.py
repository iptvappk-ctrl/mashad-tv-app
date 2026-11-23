import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    'Referer': 'https://yacinelive.com/'
}

# Let's search various IPTV GitHub repos for beIN SPORTS 1-9
urls = [
    'https://raw.githubusercontent.com/iptv-org/iptv/master/streams/qa.m3u',
    'https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_ar.m3u',
    'https://raw.githubusercontent.com/moustafamagdi/MagdiIPTV/main/Bein.m3u',
    'https://raw.githubusercontent.com/ameeralmatar1000-cpu/arabic-iptv/main/arabic.m3u',
    'https://raw.githubusercontent.com/HeshamB7/Saudi-Arabic-Sports-IPTV/main/Sports.m3u',
    'https://raw.githubusercontent.com/iptv-Ram-Lil/arabic-iptv/main/iptv.m3u',
    'https://raw.githubusercontent.com/lyqaswqasw-cpu/bein_a.m3u8/main/bein_a.m3u8',
    'https://raw.githubusercontent.com/lyqaswqasw-cpu/oak_bein.m3u8/main/oak_bein.m3u8',
    'https://raw.githubusercontent.com/bermufine/dcmp/main/dcmp.m3u'
]

found_streams = {}

for u in urls:
    try:
        req = urllib.request.Request(u, headers=headers)
        with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
            lines = r.read().decode('utf-8', errors='ignore').splitlines()
            print(f"Fetched {u} ({len(lines)} lines)")
            current_name = None
            for line in lines:
                line = line.strip()
                if line.startswith('#EXTINF'):
                    current_name = line
                elif line.startswith('http') and current_name:
                    lower = current_name.lower()
                    if 'bein' in lower and 'sports' in lower:
                        found_streams[line] = current_name
                    current_name = None
    except Exception as e:
        print(f"FAIL {u}: {e}")

print(f"\nTotal candidate beIN streams found: {len(found_streams)}")
for stream, meta in list(found_streams.items())[:20]:
    print(f"{meta} -> {stream}")
