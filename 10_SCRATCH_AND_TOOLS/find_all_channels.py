import re, json

with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    data = f.read()

pattern = rb'\{"id":"(\d+)","is_hide":(\d+),"logo":"([^"]*)","name":"([^"]+)","priority":(\d+)\}'
matches = re.findall(pattern, data)

channels = {}
for m in matches:
    cid, is_hide, logo, name, prio = m[0].decode(), int(m[1].decode()), m[2].decode(), m[3].decode('utf-8', 'ignore'), int(m[4].decode())
    channels[cid] = {
        "id": cid,
        "name": name,
        "logo": logo,
        "is_hide": is_hide,
        "priority": prio
    }

print(f"Total parsed channels: {len(channels)}")
for cid, ch in channels.items():
    print(f"- {ch['name']} (id: {cid}, logo: {ch['logo'][:40]}...)")

with open('10_SCRATCH_AND_TOOLS/yacine_real_channels.json', 'w', encoding='utf-8') as f:
    json.dump(list(channels.values()), f, ensure_ascii=False, indent=2)
