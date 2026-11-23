import json, urllib.request, urllib.error

with open(r'<user_home>\AppData\Roaming\xdg.data\com.vercel.cli\auth.json', 'r') as f:
    token = json.load(f)['token']

team = 'team_0ub00L1M1LpPB0cXAubBayxK'
headers = {'Authorization': f'Bearer {token}'}

endpoints = [
    f'https://api.vercel.com/v1/usage?teamId={team}',
    f'https://api.vercel.com/v2/usage?teamId={team}',
    f'https://api.vercel.com/v1/teams/{team}/billing/invoices',
    f'https://api.vercel.com/v2/teams/{team}/billing',
    f'https://api.vercel.com/v1/teams/{team}/limits',
    f'https://api.vercel.com/v9/projects/prj_4ZdTdY4DCY0VajHhNk0h08pHc8jW/analytics?teamId={team}'
]

for ep in endpoints:
    try:
        req = urllib.request.Request(ep, headers=headers)
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode()
            print(f"=== {ep} (status {resp.status}) ===")
            print(data[:400])
    except urllib.error.HTTPError as e:
        print(f"=== {ep} (code {e.code}) ===")
        print(e.read().decode()[:400])
    except Exception as ex:
        print(f"=== {ep} (error) ===", ex)
