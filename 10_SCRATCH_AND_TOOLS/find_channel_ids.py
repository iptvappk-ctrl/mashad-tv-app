import re, json

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

# Search for JSON objects containing beIN SPORTS 1 or beIN SPORTS
pattern = rb'\{[^{}]*"name":"beIN SPORTS [1-9]"[^{}]*\}'
matches = re.findall(pattern, data)
print(f"Found {len(matches)} channel JSON candidates:")
for m in set(matches):
    print(m)

# Also let's search for "id":\d+ or "id":"\d+" near "beIN SPORTS"
for ch_name in [b"beIN SPORTS 1", b"beIN SPORTS 2", b"beIN SPORTS 3", b"beIN SPORTS 4"]:
    idx = data.find(ch_name)
    if idx != -1:
        start = max(0, idx - 150)
        end = min(len(data), idx + 150)
        print(f"Near {ch_name}:", data[start:end])
