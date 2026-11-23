import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    for name in z.namelist():
        data = z.read(name)
        if b'firebase' in data.lower() or b'google_app_id' in data:
            print(f"File with firebase: {name}")
            matches = re.findall(rb'https?://[^\s"\'<>\x00]+', data)
            for m in matches:
                if b'firebase' in m:
                    print("  ", m.decode('utf-8', errors='ignore'))
