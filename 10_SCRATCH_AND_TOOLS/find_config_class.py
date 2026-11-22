import zipfile, re

apk_path = r'10_SCRATCH_AND_TOOLS\yacine.apk'
with zipfile.ZipFile(apk_path, 'r') as z:
    for dex_name in z.namelist():
        if not dex_name.endswith('.dex'): continue
        dex = z.read(dex_name)
        for term in [b'fetched_time', b'config']:
            matches = [m.start() for m in re.finditer(re.escape(term), dex)]
            for pos in matches[:5]:
                snippet = dex[max(0, pos-50):min(len(dex), pos+100)]
                if b'app' in snippet or b'pref' in snippet or b'get' in snippet or b'put' in snippet:
                    print(f"[{dex_name}] pos={pos}: {snippet}")
