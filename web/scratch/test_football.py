import urllib.request
import json
from datetime import datetime

LEAGUES = [
    {"code": "eng.1", "name": "الدوري الإنجليزي الممتاز"},
    {"code": "esp.1", "name": "الدوري الإسباني"},
    {"code": "ita.1", "name": "الدوري الإيطالي"},
    {"code": "ger.1", "name": "الدوري الألماني"},
    {"code": "fra.1", "name": "الدوري الفرنسي"},
    {"code": "uefa.champions", "name": "دوري أبطال أوروبا"},
]

headers = {'User-Agent': 'Mozilla/5.0'}

upcoming_all = []
finished_all = []

for league in LEAGUES:
    url = f"https://site.api.espn.com/apis/site/v2/sports/soccer/{league['code']}/scoreboard"
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=6) as r:
            data = json.loads(r.read().decode('utf-8'))
            
            # 1. Existing events in scoreboard (mostly post or in)
            for ev in data.get('events', []):
                state = ev.get('status', {}).get('type', {}).get('state', 'pre')
                if state == 'post':
                    finished_all.append(f"[{league['name']}] {ev['name']} (انتهت)")
                elif state == 'in':
                    upcoming_all.append(f"[{league['name']}] {ev['name']} (مباشر الآن 🔴)")

            # 2. Check future calendar date
            cal = data.get('leagues', [{}])[0].get('calendar', [])
            future_dates = [d for d in cal if d[:10] >= '2026-10-27']
            if future_dates:
                next_date_str = future_dates[0][:10].replace('-', '')
                future_url = f"https://site.api.espn.com/apis/site/v2/sports/soccer/{league['code']}/scoreboard?dates={next_date_str}"
                freq = urllib.request.Request(future_url, headers=headers)
                with urllib.request.urlopen(freq, timeout=6) as fr:
                    fdata = json.loads(fr.read().decode('utf-8'))
                    for fev in fdata.get('events', []):
                        fstate = fev.get('status', {}).get('type', {}).get('state', 'pre')
                        if fstate in ('pre', 'in'):
                            upcoming_all.append(f"[{league['name']}] {fev['name']} ({fev['date'][:10]})")
    except Exception as e:
        print(f"Error {league['name']}: {e}")

print(f"Total Upcoming: {len(upcoming_all)}")
for u in upcoming_all[:10]:
    print(" ->", u)

print(f"\nTotal Finished: {len(finished_all)}")
for f in finished_all[:10]:
    print(" ->", f)
