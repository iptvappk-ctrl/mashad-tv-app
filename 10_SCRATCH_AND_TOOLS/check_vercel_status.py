import json, urllib.request, urllib.error

with open(r'<user_home>\AppData\Roaming\xdg.data\com.vercel.cli\auth.json', 'r') as f:
    token = json.load(f)['token']

team = 'team_0ub00L1M1LpPB0cXAubBayxK'
proj = 'prj_4ZdTdY4DCY0VajHhNk0h08pHc8jW'
headers = {'Authorization': f'Bearer {token}'}

print("=== CHECKING VERCEL PROJECT ===")
url = f'https://api.vercel.com/v9/projects/{proj}?teamId={team}'
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
        print("Project Name:", data.get('name'))
        print("Project Paused:", data.get('paused'))
        print("Disabled:", data.get('disabled'))
        print("Production Deployment:", data.get('targets', {}).get('production', {}).get('id'))
except urllib.error.HTTPError as e:
    print("Project HTTPError:", e.code, e.read().decode())

print("\n=== CHECKING DEPLOYMENTS ===")
url = f'https://api.vercel.com/v6/deployments?teamId={team}&projectId={proj}&limit=8'
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
        for d in data.get('deployments', []):
            print(f"{d.get('uid')} | {d.get('state')} | {d.get('url')} | {d.get('target')}")
except urllib.error.HTTPError as e:
    print("Deployments HTTPError:", e.code, e.read().decode())

print("\n=== CHECKING TEAM / BILLING / USAGE ===")
for ep in [f'https://api.vercel.com/v2/teams/{team}', f'https://api.vercel.com/v1/teams/{team}']:
    req = urllib.request.Request(ep, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            tdata = json.loads(resp.read().decode())
            print("Team name:", tdata.get('name'))
            print("Billing status:", tdata.get('billing', {}).get('status'))
            print("Plan:", tdata.get('billing', {}).get('plan'))
            print("Period:", tdata.get('billing', {}).get('period'))
            break
    except urllib.error.HTTPError as e:
        print(f"Team {ep} err:", e.code, e.read().decode()[:200])
