import zipfile, re

apk_path = r'10_SCRATCH_AND_TOOLS\yacine.apk'
with zipfile.ZipFile(apk_path, 'r') as z:
    dex = z.read('classes.dex')
    matches = [m.start() for m in re.finditer(rb'Utils\.kt', dex)]
    print(f"Found Utils.kt: {len(matches)}")
    for pos in matches:
        start = max(0, pos - 200)
        end = min(len(dex), pos + 300)
        print("--- SNIPPET ---")
        print(dex[start:end])
