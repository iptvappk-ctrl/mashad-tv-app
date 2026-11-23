import subprocess, re

out = subprocess.check_output(['git', 'log', '-p', '-G', 'm3u8'], errors='ignore')
matches = set(re.findall(r'https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*', out))
print(f"Total unique m3u8 URLs found in git history: {len(matches)}")
for m in sorted(matches):
    print(" ", m)
