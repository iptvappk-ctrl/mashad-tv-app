import subprocess, json

files = subprocess.check_output(['adb', '-s', 'emulator-5554', 'shell', 'grep -l "episodes" /data/data/com.madar.madar_flutter/no_backup/catalog-cache-v1/*.json']).decode('utf-8', errors='ignore').splitlines()
for f in files:
    f = f.strip()
    if not f: continue
    try:
        raw = subprocess.check_output(['adb', '-s', 'emulator-5554', 'shell', f'cat {f}'])
        data = json.loads(raw)
        seasons = data.get('payload', {}).get('seasons', [])
        if seasons:
            first_ep = seasons[0].get('episodes', [{}])[0].get('title', '')
            first_url = seasons[0].get('episodes', [{}])[0].get('pageUrl', '')
            total_eps = sum(len(s.get('episodes', [])) for s in seasons)
            print(f'{f}: {len(seasons)} seasons, {total_eps} eps | First: {first_ep} ({first_url})')
    except Exception as e:
        pass
