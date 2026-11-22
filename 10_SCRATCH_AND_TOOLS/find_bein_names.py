import re

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

pattern = rb'"name":"([^"]*bein[^"]*)"'
matches = re.findall(pattern, data, re.IGNORECASE)
print('Unique names matching bein:', set(matches))

pattern_all_names = rb'"name":"([^"]+)"'
all_names = set(re.findall(pattern_all_names, data))
print(f"Total names found: {len(all_names)}")
sports = [n for n in all_names if any(w in n.lower() for w in [b'sport', b'bein', b'ssc', b'alkass', b'ad '])]
print(f"Sports names: {sports}")
