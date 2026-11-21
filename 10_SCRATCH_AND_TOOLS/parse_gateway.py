import re

with open('gateway.html', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

print('Title:', re.findall(r'<title>(.*?)</title>', text, re.I))

for m in re.findall(r'https?://[^\s"\'<>]+\.(?:m3u8|mp4)[^\s"\'<>]*', text, re.I):
    print('Direct video stream URL:', m)

for m in re.findall(r'file\s*:\s*["\']([^"\']+)["\']', text):
    print('File URL:', m)

for m in re.findall(r'src\s*:\s*["\']([^"\']+)["\']', text):
    print('Src URL:', m)

# Look for unpacked or eval or script
for s in re.findall(r'<script\b[^>]*>(.*?)</script>', text, re.DOTALL):
    if len(s.strip()) > 50:
        print('--- SCRIPT CHUNK ---')
        lines = [line.strip() for line in s.split('\n') if line.strip()]
        for l in lines[:15]:
            print('  ', l[:120])
