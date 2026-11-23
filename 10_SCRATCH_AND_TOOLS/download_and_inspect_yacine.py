import urllib.request, ssl, zipfile, io, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = 'https://raw.githubusercontent.com/yacinetv2/yacinetv2.3/refs/heads/main/YacineTV_____V3.2.apk'
print("Downloading Yacine V3.2 APK...")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, context=ctx, timeout=30) as r:
    data = r.read()
    print("Downloaded size:", len(data))

z = zipfile.ZipFile(io.BytesIO(data))
print("Files in zip:", len(z.namelist()))
for name in z.namelist():
    if name.endswith('.dex'):
        dex = z.read(name)
        # Find all api / stream URLs
        urls = set(re.findall(rb'https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}[^\s"\'<>\x00]*', dex))
        for u in sorted(urls):
            us = u.decode('utf-8', errors='ignore')
            if any(k in us for k in ['api', 'live', 'ycn', 'yacine', 'stream']):
                if not any(x in us for x in ['google', 'android', 'apple', 'schema']):
                    print(f"  [{name}] {us}")
