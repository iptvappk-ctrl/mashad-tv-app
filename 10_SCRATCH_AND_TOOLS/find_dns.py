import socket, requests

subdomains = [
    "def001.ycnapi.com", "api.ycnapi.com", "v3.ycnapi.com",
    "deft.yacinelive.com", "api.yacinelive.com", "ver3.yacinelive.com", "v3.yacinelive.com", "app.yacinelive.com", "live.yacinelive.com",
    "api.yacineapp.tv", "app.yacineapp.tv",
    "api.yacine-tv.net", "app.yacine-tv.net",
    "api.yacinetv.stream", "live.yacinetv.stream",
    "ycn.yacinelive.com", "cdn.yacinelive.com", "s1.yacinelive.com", "s2.yacinelive.com",
    "ytv.yacinelive.com", "player.yacinelive.com",
    "stream.yacinelive.com", "stream.yacineapp.tv",
    "yacine.today", "api.yacine.today",
    "yacinetv.link", "api.yacinetv.link"
]

for d in subdomains:
    try:
        ip = socket.gethostbyname(d)
        print(f"RESOLVED: {d} -> {ip}")
    except Exception:
        pass
