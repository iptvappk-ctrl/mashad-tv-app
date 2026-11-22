with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    hprof = f.read()

import re
# Search for strings containing "com.ytv"
matches = [m.start() for m in re.finditer(rb'com\.ytv', hprof)]
print("Matches for com.ytv:", len(matches))
for pos in matches:
    print(hprof[max(0, pos-40):pos+100])
