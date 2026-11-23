import socket

subdomains = [
    'api.ycnapi.com',
    'def.ycnapi.com',
    'def001.ycnapi.com',
    'def01.ycnapi.com',
    'app.ycnapi.com',
    'v3.ycnapi.com',
    'api.yacinelive.com',
    'live.yacinelive.com',
    'deft.yacinelive.com',
    'app.yacinelive.com',
    'v3.yacinelive.com',
    'ycnapi.com',
    'yacinelive.com',
    'yacineapp.tv',
    'api.yacineapp.tv'
]

for d in subdomains:
    try:
        ip = socket.gethostbyname(d)
        print(f"RESOLVED: {d} -> {ip}")
    except Exception as e:
        # print(f"FAILED: {d}")
        pass
