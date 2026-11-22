with open('10_SCRATCH_AND_TOOLS/yacine_stream.hprof', 'rb') as f:
    hprof = f.read()

import re
# Search for channel id 1143098968567368033
cid_matches = [m.start() for m in re.finditer(rb'1143098968567368033', hprof)]
print(f"Matches for beIN 1 id: {len(cid_matches)}")
for pos in cid_matches:
    start = max(0, pos - 100)
    end = min(len(hprof), pos + 300)
    print("--- CID SNIPPET ---")
    print(hprof[start:end])
