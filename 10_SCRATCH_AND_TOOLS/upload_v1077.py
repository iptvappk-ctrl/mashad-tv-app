import urllib.request, json, os, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

token = 'REDACTED_TOKEN'
repo = 'iptvappk-ctrl/mashad-tv-app'
tag = 'v1.0.77'

headers = {
    'Authorization': f'token {token}',
    'User-Agent': 'Mozilla/5.0',
    'Accept': 'application/vnd.github.v3+json'
}

release_payload = {
    'tag_name': tag,
    'target_commitish': '2-upload',
    'name': f'Zaqzouka TV Release {tag} (Live Sports Fix)',
    'body': 'Updated release with live beIN SPORTS channel routing from Yacine TV API.',
    'draft': False,
    'prerelease': False
}

print(f'Creating release {tag} on {repo}...')
req = urllib.request.Request(
    f'https://api.github.com/repos/{repo}/releases',
    data=json.dumps(release_payload).encode('utf-8'),
    headers={**headers, 'Content-Type': 'application/json'},
    method='POST'
)

release_data = None
try:
    with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
        release_data = json.loads(resp.read().decode('utf-8'))
        print('Release created! ID:', release_data.get('id'))
except urllib.error.HTTPError as e:
    err_body = e.read().decode('utf-8')
    print(f'HTTP Error {e.code}: {err_body}')
    if e.code == 422:
        get_req = urllib.request.Request(f'https://api.github.com/repos/{repo}/releases/tags/{tag}', headers=headers)
        with urllib.request.urlopen(get_req, context=ctx, timeout=15) as r:
            release_data = json.loads(r.read().decode('utf-8'))
            print('Fetched existing release ID:', release_data.get('id'))

if not release_data:
    print('Failed to get release data!')
    exit(1)

upload_url_base = release_data['upload_url'].split('{')[0]

files_to_upload = [
    (r'c:\Users\hassa\Desktop\all project\iptv app\08_RELEASE_BINARIES\zaqzouka-app.apk', 'zaqzouka-app.apk'),
    (r'c:\Users\hassa\Desktop\all project\iptv app\08_RELEASE_BINARIES\zag-player.apk', 'zag-player.apk'),
]

for file_path, asset_name in files_to_upload:
    size = os.path.getsize(file_path)
    print(f'Uploading {asset_name} ({size / (1024*1024):.2f} MB)...')
    upload_url = f'{upload_url_base}?name={asset_name}'
    with open(file_path, 'rb') as f:
        file_bytes = f.read()
    up_req = urllib.request.Request(
        upload_url,
        data=file_bytes,
        headers={
            **headers,
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Length': str(size)
        },
        method='POST'
    )
    with urllib.request.urlopen(up_req, context=ctx, timeout=300) as up_resp:
        asset_info = json.loads(up_resp.read().decode('utf-8'))
        print(f"Uploaded {asset_name}! URL: {asset_info.get('browser_download_url')}")

print('ALL DONE!')
