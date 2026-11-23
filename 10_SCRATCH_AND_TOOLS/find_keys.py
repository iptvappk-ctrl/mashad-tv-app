import hashlib, subprocess

target_hashes = {
    'f4f4e2c5822ed8f852a5ad55c3e6c18b5597426523b2e0f0708f65d329d007ef': 'f4f4 (12 eps)',
    '5eae0c6f6b33ca266b2e5d10d61aad83dec2c7873628f813edbbfc297167ef5f': '5eae (2 eps)',
    '92f2699443aae0739af9a742304630393143dc673141fa244178da5cd56e3ddb': '92f2 (2 eps)',
    'df7c4f191564132e1c063896ed37ab2eb6a8b35af89f93fed8d776a9bf1a4871': 'df7c (2 eps)',
    'f47fa01cd854bcdc246ee1f86b7f0625ac1b7530ae3ee6a43fd7145adea112db': 'f47f (2 eps)',
}

# Collect all URLs mentioned in the logcat
out = subprocess.check_output(['adb', '-s', 'emulator-5554', 'logcat', '-d'], errors='ignore')
import re
urls = set(re.findall(r'https?://[^\s\"\'\)\]<>]+', out))

# Add variations
all_candidates = set()
for u in urls:
    all_candidates.add(u)
    all_candidates.add(u.rstrip('/'))
    all_candidates.add(u.lower())
    all_candidates.add(u.lower().rstrip('/'))
    import urllib.parse
    dec = urllib.parse.unquote(u)
    all_candidates.add(dec)
    all_candidates.add(dec.rstrip('/'))
    all_candidates.add(dec.lower())
    all_candidates.add(dec.lower().rstrip('/'))

print(f'Checking {len(all_candidates)} candidates...')
for c in all_candidates:
    h = hashlib.sha256(c.encode('utf-8')).hexdigest()
    if h in target_hashes:
        print(f'FOUND: {target_hashes[h]} -> {c}')
