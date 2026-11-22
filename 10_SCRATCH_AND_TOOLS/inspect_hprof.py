with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

print("File size:", len(data))

# Search for "فولهام" (Fulham)
target = "فولهام".encode('utf-8')
idx = 0
found = []
while True:
    idx = data.find(target, idx)
    if idx == -1: break
    found.append(idx)
    idx += len(target)

print("Found occurrences of فولهام:", len(found))
for pos in found[:10]:
    start = max(0, pos - 200)
    end = min(len(data), pos + 400)
    snippet = data[start:end]
    # Clean non-printable
    cleaned = ''.join(chr(b) if 32 <= b < 127 or b > 127 else ' ' for b in snippet)
    print("--- AT", pos, "---")
    print(cleaned)
