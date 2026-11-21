import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = "https://raw.githubusercontent.com/yacinetv2/yacinetv2.3/refs/heads/main/YacineTV_____V3.2.apk"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
    data = resp.read()

with open("10_SCRATCH_AND_TOOLS/yacine_sample.apk", "wb") as f:
    f.write(data)
print("Saved APK bytes:", len(data))
