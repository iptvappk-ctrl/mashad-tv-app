import re, json

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

# Let's inspect around the beIN SPORTS occurrences
for pos in [m.start() for m in re.finditer(rb'beIN SPORTS', data)][:15]:
    start = max(0, pos - 80)
    end = min(len(data), pos + 120)
    snippet = data[start:end]
    cleaned = ''.join(chr(b) if 32 <= b < 127 else ' ' for b in snippet)
    print(f"[{pos}] {cleaned}")
