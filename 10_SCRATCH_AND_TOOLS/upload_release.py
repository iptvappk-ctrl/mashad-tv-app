import urllib.request, json, os, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

token = 'REDACTED_TOKEN'
repo = 'iptvappk-ctrl/mashad-tv-app'
tag = 'v1.0.76'

headers = {
    'Authorization': f'token {token}',
    'User-Agent': 'Mozilla/5.0',
    'Accept': 'application/vnd.github.v3+json'
}

# 1. Create release
release_payload = {
    'tag_name': tag,
    'target_commitish': '2-upload',
    'name': f'Zaqzouka TV Release {tag}',
    'body': 'Official release of Zaqzouka TV Client and Zag Player engine.',
    'draft': False,
    'prerelease': False
}

print(f"Creating release {tag} on {repo}...")
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
        print("Release created successfully! ID:", release_data.get('id'))
except urllib.error.HTTPError as e:
    err_body = e.read().decode('utf-8')
    print(f"HTTP Error {e.code}: {err_body}")
    if e.code == 422: # Already exists?
        # Fetch existing release
        get_req = urllib.request.Request(f'https://api.github.com/repos/{repo}/releases/tags/{tag}', headers=headers)
        with urllib.request.urlopen(get_req, context=ctx, timeout=15) as r:
            release_data = json.loads(r.read().decode('utf-8'))
            print("Fetched existing release ID:", release_data.get('id'))

if not release_data:
    print("Could not get release data!")
    exit(1)

upload_url_template = release_data['upload_url'] # e.g. https://uploads.github.com/repos/.../releases/123/assets{?name,label}
upload_url_base = upload_url_template.split('{')[0]

files_to_upload = [
    (r'c:\Users\hassa\Desktop\all project\iptv app\realses\zaqzouka-app.apk', 'zaqzouka-app.apk', 'application/vnd.android.package-archive'),
    (r'c:\Users\hassa\Desktop\all project\iptv app\realses\zag-player.apk', 'zag-player.apk', 'application/vnd.android.package-archive'),
]

for file_path, asset_name, mime in files_to_upload:
    size = os.path.getsize(file_path)
    print(f"Uploading {asset_name} ({size / (1024*1024):.2f} MB)...")
    upload_url = f"{upload_url_base}?name={asset_name}"
    
    with open(file_path, 'rb') as f:
        file_bytes = f.read()
    
    up_req = urllib.request.Request(
        upload_url,
        data=file_bytes,
        headers={
            **headers,
            'Content-Type': mime,
            'Content-Length': str(size)
        },
        method='POST'
    )
    
    try:
        with urllib.request.urlopen(up_req, context=ctx, timeout=120) as up_resp:
            asset_info = json.loads(up_resp.read().decode('utf-8'))
            print(f"Uploaded {asset_name}! Direct download: {asset_info.get('browser_download_url')}")
    except Exception as e:
        print(f"Error uploading {asset_name}: {e}")

print("Done!")
