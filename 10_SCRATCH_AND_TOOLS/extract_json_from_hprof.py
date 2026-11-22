with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

pos = 9393477
# Look backward to find the start of JSON (e.g. `[{"` or `{"success"` or `{"data"`)
start = pos
while start > 0 and data[start:start+1] not in (b'[', b'{'):
    start -= 1

# Try to parse or find matching bracket
print("Found start candidate at", start, data[start:start+50])

# Let's search backward for 'data' or similar
for search_start in range(pos, max(0, pos - 10000), -1):
    if data[search_start:search_start+8] in (b'{"data":', b'[{"id":', b'[{"cham'):
        print("Better start at", search_start, data[search_start:search_start+40])
        start = search_start
        break

# Now find where the JSON ends
# Count braces or brackets
depth = 0
end = start
in_quote = False
escape = False
starter = data[start:start+1]
closer = b']' if starter == b'[' else b'}'

for i in range(start, min(len(data), start + 500000)):
    b = data[i:i+1]
    if escape:
        escape = False
        continue
    if b == b'\\':
        escape = True
        continue
    if b == b'"':
        in_quote = not in_quote
        continue
    if not in_quote:
        if b == starter:
            depth += 1
        elif b == closer:
            depth -= 1
            if depth == 0:
                end = i + 1
                break

json_bytes = data[start:end]
print("Extracted JSON length:", len(json_bytes))
with open('10_SCRATCH_AND_TOOLS/extracted_events.json', 'wb') as out:
    out.write(json_bytes)

import json
try:
    parsed = json.loads(json_bytes.decode('utf-8'))
    print("SUCCESSFULLY PARSED JSON!")
    if isinstance(parsed, list):
        print("Item count:", len(parsed))
        for item in parsed:
            print("Event:", item.get("id"), item.get("channel"), item.get("commentary"), item.get("team_1", {}).get("name"), "vs", item.get("team_2", {}).get("name"))
    elif isinstance(parsed, dict):
        print("Keys:", parsed.keys())
        data_arr = parsed.get("data", [])
        print("Data count:", len(data_arr))
        for item in data_arr:
            print("Event:", item.get("id"), item.get("channel"), item.get("commentary"), item.get("team_1", {}).get("name"), "vs", item.get("team_2", {}).get("name"))
except Exception as e:
    print("Parse error:", e)
    # Print start and end
    print("Raw start:", json_bytes[:200])
    print("Raw end:", json_bytes[-200:])
