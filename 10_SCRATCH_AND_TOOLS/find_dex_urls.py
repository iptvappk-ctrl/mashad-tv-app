import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
dex = z.read("classes.dex")

# Let us find where retrofit or api requests are constructed in dev/google/yacinetv
# Search for url strings and api endpoints
for m in re.finditer(rb"https?://[^\x00\s\"\'<>]+", dex):
    url = m.group(0).decode("latin1")
    if "api" in url or "yacine" in url or "live" in url or "m3u8" in url:
        print("URL:", url)
