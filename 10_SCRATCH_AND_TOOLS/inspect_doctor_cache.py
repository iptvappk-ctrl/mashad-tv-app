import subprocess, json

files = [
    'f4f4e2c5822ed8f852a5ad55c3e6c18b5597426523b2e0f0708f65d329d007ef.json',
    '5eae0c6f6b33ca266b2e5d10d61aad83dec2c7873628f813edbbfc297167ef5f.json',
    '92f2699443aae0739af9a742304630393143dc673141fa244178da5cd56e3ddb.json',
    'df7c4f191564132e1c063896ed37ab2eb6a8b35af89f93fed8d776a9bf1a4871.json',
    'f47fa01cd854bcdc246ee1f86b7f0625ac1b7530ae3ee6a43fd7145adea112db.json',
    '1828563d075223c53270bd911f6d60ee5b416161e4bb71c0de442400c60b1f53.json',
    'd08c74465ff650d5e7776295b79baea1bdd539f345740b3ff9fd47e62e374127.json',
]

for name in files:
    path = f'/data/data/com.madar.madar_flutter/no_backup/catalog-cache-v1/{name}'
    try:
        raw = subprocess.check_output(['adb', '-s', 'emulator-5554', 'shell', f'cat {path}'])
        data = json.loads(raw)
        savedAt = data.get('savedAt', 0)
        seasons = data.get('payload', {}).get('seasons', [])
        total_eps = sum(len(s.get('episodes', [])) for s in seasons)
        print(f'{name} | savedAt={savedAt} | seasons={len(seasons)} | total_eps={total_eps}')
        for s in seasons:
            ep_nums = [ep.get('number') for ep in s.get('episodes', [])]
            providers = set(ep.get('providerName') for ep in s.get('episodes', []))
            print(f'   Season {s.get("number")} ({s.get("pageUrl")}): eps={ep_nums} | providers={providers}')
    except Exception as e:
        print(f'Error reading {name}: {e}')
