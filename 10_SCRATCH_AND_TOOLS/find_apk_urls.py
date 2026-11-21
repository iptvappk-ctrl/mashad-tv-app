import zipfile, re

z = zipfile.ZipFile("10_SCRATCH_AND_TOOLS/yacine.apk")
for name in z.namelist():
    if name.endswith(".dex"):
        data = z.read(name)
        urls = set(re.findall(rb"https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}[^\x00\s\"\'<>]*", data))
        for u in urls:
            u_str = u.decode("utf-8", errors="ignore")
            if any(k in u_str.lower() for k in ["api", "yacine", "ycn", "stream", "live", "channel"]):
                if not any(x in u_str for x in ["google", "android", "schema", "w3.org", "facebook", "onesignal", "startapp"]):
                    print(f"[{name}] {u_str}")
