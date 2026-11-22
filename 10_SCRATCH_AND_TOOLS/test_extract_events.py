with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

start = 9393015
chunk = data[start:start+200000]

import json
# Let's find every occurrence of ]}
for idx in range(1000, len(chunk)):
    if chunk[idx:idx+2] == b']}':
        candidate = chunk[:idx+2]
        try:
            parsed = json.loads(candidate.decode('utf-8'))
            print("Successfully parsed at length", len(candidate))
            with open('10_SCRATCH_AND_TOOLS/yacine_events_real.json', 'w', encoding='utf-8') as out:
                json.dump(parsed, out, ensure_ascii=False, indent=2)
            events = parsed.get('data', [])
            print(f"Total events: {len(events)}")
            for ev in events:
                t1 = ev.get('team_1', {}).get('name')
                t2 = ev.get('team_2', {}).get('name')
                ch = ev.get('channel')
                cm = ev.get('commentary')
                champ = ev.get('champions')
                id_ = ev.get('id')
                print(f"- [{ch}] ({champ}) {t1} vs {t2} | معلق: {cm}")
            break
        except Exception:
            continue
