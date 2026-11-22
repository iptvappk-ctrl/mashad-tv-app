import json

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\yacine_real_channels.json', 'r', encoding='utf-8') as f:
    channels = json.load(f)

print(f"Total channels: {len(channels)}")
for ch in channels:
    print(f"id='{ch['id']}', name='{ch['name']}', logo='{ch['logo']}'")
