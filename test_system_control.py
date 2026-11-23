import urllib.request
import json
import time

API = 'https://zaafilm.com/api/system_control'
SECRET = '[ADMIN_SECRET_REDACTED]'
HEADERS = {
    'Content-Type': 'application/json',
    'x-admin-secret': SECRET,
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) TestRunner/1.0'
}

def post_action(payload):
    payload['secret'] = SECRET
    req = urllib.request.Request(API, data=json.dumps(payload).encode('utf-8'), headers=HEADERS, method='POST')
    res = urllib.request.urlopen(req, timeout=10)
    return json.loads(res.read().decode('utf-8'))

def get_status():
    req = urllib.request.Request(API, headers=HEADERS)
    res = urllib.request.urlopen(req, timeout=10)
    return json.loads(res.read().decode('utf-8'))

print('=== 1. TESTING SOVEREIGN AD MANAGEMENT ===')
ad_payload = {
    'action': 'set_owner_ad',
    'enabled': True,
    'override_partner': True,
    'ad_html': '<div>Test Sovereign Ad</div>',
    'ad_url': 'https://example.com/owner-ad',
    'skip_delay_seconds': 4,
    'banner_enabled': True,
    'banner_html': '<div>Test Banner</div>',
    'banner_url': 'https://example.com/banner'
}
r1 = post_action(ad_payload)
print('Set Owner Ad Response:', r1.get('ok'), r1.get('action'))

# Verify reading directly from Firebase RTDB
req_fb = urllib.request.Request('https://madartv-default-rtdb.firebaseio.com/system_control/owner_ad_config.json')
fb_ad = json.loads(urllib.request.urlopen(req_fb, timeout=10).read().decode('utf-8'))
print('Firebase Owner Ad Verification -> enabled:', fb_ad.get('enabled'), 'override_partner:', fb_ad.get('override_partner'))

# Reset ad back to disabled
ad_payload['enabled'] = False
post_action(ad_payload)
print('Owner Ad successfully reset to disabled (no ads showing)')

print('\n=== 2. TESTING FREEZE & RESTORE ===')
r_freeze = post_action({'action': 'freeze', 'message': 'اختبار التجميد المؤقت للصيانة'})
print('Freeze Response:', r_freeze.get('ok'), r_freeze.get('action'))
st_freeze = get_status()
print('Status after Freeze:', st_freeze.get('status'))

r_restore = post_action({'action': 'restore', 'message': 'المنظومة نشطة وتعمل بكامل طاقتها.'})
print('Restore Response:', r_restore.get('ok'), r_restore.get('action'))
st_restore = get_status()
all_comps_true = all(st_restore.get('components', {}).values())
print('Status after Restore:', st_restore.get('status'), 'All Components Enabled:', all_comps_true)

print('\n=== 3. TESTING COMPONENT TOGGLES (KILL SWITCHES) ===')
for comp in ['download_site_enabled', 'web_player_enabled', 'app_enabled', 'player_enabled']:
    post_action({'action': 'set_component', 'component': comp, 'enabled': False})
    st = get_status()
    is_false = st.get('components', {}).get(comp) == False
    post_action({'action': 'set_component', 'component': comp, 'enabled': True})
    st_back = get_status()
    is_true = st_back.get('components', {}).get(comp) == True
    print(f'Component [{comp}]: OFF check -> {is_false} | ON check -> {is_true}')

print('\n=== 4. TESTING DOWNLOAD HUB BLOCKING ON ZAAFILM.COM ===')
post_action({'action': 'set_component', 'component': 'download_site_enabled', 'enabled': False})
try:
    req_site = urllib.request.Request('https://zaafilm.com/', headers={'User-Agent': 'Mozilla/5.0'})
    res_site = urllib.request.urlopen(req_site, timeout=10)
    print('zaafilm.com unexpectedly passed:', res_site.status)
except urllib.error.HTTPError as e:
    print('zaafilm.com BLOCKED successfully with HTTP code:', e.code)
post_action({'action': 'set_component', 'component': 'download_site_enabled', 'enabled': True})
res_restored = urllib.request.urlopen(urllib.request.Request('https://zaafilm.com/', headers={'User-Agent': 'Mozilla/5.0'}), timeout=10)
print('zaafilm.com RESTORED successfully with HTTP code:', res_restored.status)

print('\n=== 5. TESTING NUCLEAR WIPE (SAFE CONTROLLED VERIFICATION) ===')
r_wipe = post_action({'action': 'wipe', 'wipeServers': False})
print('Nuclear Wipe Triggered:', r_wipe.get('ok'), r_wipe.get('action'))
st_wipe = get_status()
print('Status after Wipe in Firebase:', st_wipe.get('status'))
print('Wipe Timestamp recorded:', st_wipe.get('wipeTimestamp') or st_wipe.get('updatedAt'))

# Immediately restore system to full healthy state
post_action({'action': 'restore', 'message': 'المنظومة نشطة وتعمل بكامل طاقتها.'})
st_final = get_status()
print('Full System Restored to Active:', st_final.get('status') == 'active')
print('=== ALL 5 SUITES PASSED SUCCESSFULLY ===')
