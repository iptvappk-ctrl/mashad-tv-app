import re, json

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

start = 9393015
chunk = data[start:start+50000]

# Find all JSON event objects by regex
# Pattern: {"champions":...}
pattern = rb'\{"champions":.*?"team_2":\{"id":\d+,"logo":".*?","name":".*?"\}\}'
matches = re.findall(pattern, chunk)

print(f"Regex found {len(matches)} event matches!")

parsed_events = []
for m in matches:
    try:
        s = m.decode('utf-8')
        ev = json.loads(s)
        parsed_events.append(ev)
    except Exception as e:
        # Try cleaning
        try:
            s = m.decode('utf-8', errors='ignore')
            ev = json.loads(s)
            parsed_events.append(ev)
        except:
            print("Failed match:", m[:100])

print(f"Successfully decoded {len(parsed_events)} events:")
for ev in parsed_events:
    t1 = ev.get('team_1', {}).get('name')
    t2 = ev.get('team_2', {}).get('name')
    ch = ev.get('channel')
    cm = ev.get('commentary')
    champ = ev.get('champions')
    st = ev.get('start_time')
    et = ev.get('end_time')
    print(f"🏆 {champ} | ⚽ {t1} 🆚 {t2} | 📺 {ch} | 🎙️ {cm}")

with open('10_SCRATCH_AND_TOOLS/yacine_real_events.json', 'w', encoding='utf-8') as f:
    json.dump({"data": parsed_events}, f, ensure_ascii=False, indent=2)
