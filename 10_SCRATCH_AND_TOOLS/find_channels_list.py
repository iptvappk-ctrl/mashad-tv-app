import re, json

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

# Search for "beIN SPORTS NEWS"
matches = [m.start() for m in re.finditer(rb'beIN SPORTS NEWS', data)]
print(f"Found 'beIN SPORTS NEWS': {len(matches)}")

for pos in matches:
    start = max(0, pos - 200)
    end = min(len(data), pos + 300)
    print("--- SNIPPET ---")
    print(data[start:end])
