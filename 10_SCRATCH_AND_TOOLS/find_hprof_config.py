import re

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    hprof = f.read()

# Let's search for "api" or "events" or "categories"
for target in [b'/events', b'/categories', b'categories/', b'yacinelive', b'ycnapi']:
    matches = [m.start() for m in re.finditer(re.escape(target), hprof)]
    print(f"Target {target}: {len(matches)}")
    for pos in matches[:3]:
        print(f"  [{pos}]", hprof[max(0, pos-60):pos+80])
