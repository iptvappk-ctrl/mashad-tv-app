import json

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\yacine_real_events.json', 'r', encoding='utf-8') as f:
    events = json.load(f)['data']

print(f"Total events: {len(events)}")
for i, e in enumerate(events):
    print(f"{i+1}. {e.get('champions')} | {e.get('team_1',{}).get('name')} vs {e.get('team_2',{}).get('name')} | {e.get('channel')} | معلق: {e.get('commentary')}")
