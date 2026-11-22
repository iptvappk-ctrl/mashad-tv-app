import json

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\yacine_real_events.json', 'r', encoding='utf-8') as f:
    events = json.load(f)['data']

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\yacine_real_channels.json', 'r', encoding='utf-8') as f:
    channels = json.load(f)

# Format channels for Kotlin
kt_channels = []
for i, ch in enumerate(channels):
    prio = 25 - i
    kt_channels.append(f'        YacineChannel(id = "{ch["id"]}", name = "{ch["name"]}", logo = "{ch["logo"]}", isHide = 0, priority = {prio}),')

# Format events for Kotlin
kt_events = []
for ev in events:
    t1 = ev['team_1']
    t2 = ev['team_2']
    cid = ev['id']
    champ = ev['champions']
    chan = ev['channel']
    st = ev['start_time']
    et = ev['end_time']
    comm = ev['commentary']
    t1_name = t1['name']
    t1_logo = t1.get('logo', '')
    t1_id = str(t1.get('id', ''))
    t2_name = t2['name']
    t2_logo = t2.get('logo', '')
    t2_id = str(t2.get('id', ''))
    kt_events.append(f'''        YacineEvent(
            id = "{cid}",
            champions = "{champ}",
            channel = "{chan}",
            startTime = {st}L,
            endTime = {et}L,
            commentary = "{comm}",
            team1 = YacineTeam(id = "{t1_id}", name = "{t1_name}", logo = "{t1_logo}"),
            team2 = YacineTeam(id = "{t2_id}", name = "{t2_name}", logo = "{t2_logo}")
        ),''')

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\kt_channels.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(kt_channels))

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\kt_events.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(kt_events))

print(f"Generated {len(kt_channels)} channels and {len(kt_events)} events.")
