import zipfile, re

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk') as z:
    for name in z.namelist():
        if name.endswith('.dex'):
            dex = z.read(name)
            # Find class descriptors like Lcom/...;
            classes = re.findall(b'L[a-zA-Z0-9_/$]+;', dex)
            app_classes = [c.decode('ascii', errors='ignore') for c in classes if not c.startswith((b'Landroid', b'Landroidx', b'Lcom/google', b'Lkotlin', b'Lio/reactivex', b'Lokhttp3', b'Lcom/squareup', b'Lcom/facebook', b'Lcom/applovin', b'Lcom/unity3d', b'Lcom/ironsource', b'Lcom/bytedance', b'Lcom/mbridge'))]
            print(f"[{name}] Non-standard classes: {len(app_classes)}")
            for c in sorted(set(app_classes))[:40]:
                print(c)
