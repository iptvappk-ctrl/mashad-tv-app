import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
}

# Search GitHub API for active m3u playlists with bein
search_urls = [
    'https://raw.githubusercontent.com/abusaid12/m3u/main/ar.m3u',
    'https://raw.githubusercontent.com/iptv-org/iptv/master/streams/ar.m3u',
    'https://raw.githubusercontent.com/yousif-salman/iptv/main/playlist.m3u',
    'https://raw.githubusercontent.com/realvjy/iptv/main/playlists/arabic.m3u',
    'https://raw.githubusercontent.com/tweakmy/iptv/master/playlist.m3u8',
    'https://raw.githubusercontent.com/mohamedali007/iptv/main/beIN.m3u',
    'https://raw.githubusercontent.com/abde-l/iptv/main/ar.m3u',
    'https://raw.githubusercontent.com/Yacine-TV/yacine-streams/main/streams.json',
]

for u in search_urls:
    try:
        req = urllib.request.Request(u, headers=headers)
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            data = r.read().decode('utf-8', errors='ignore')
            print(f"OK {u} -> {len(data)} chars")
            # find bein channels
            for line in data.splitlines():
                if 'bein' in line.lower() and ('http' in line.lower() or 'extinf' in line.lower()):
                    print("  ", line[:120])
    except Exception as e:
        # print(f"FAIL {u}: {e}")
        pass
