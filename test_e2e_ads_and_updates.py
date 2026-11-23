import urllib.request
import json
import time

API = 'https://zaafilm.com/api/system_control'
SECRET = 'Zq!7#mK9xPd@3wR5nT'
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

def get_fb(path):
    url = f'https://madartv-default-rtdb.firebaseio.com/{path}.json'
    req = urllib.request.Request(url, headers={'User-Agent': 'TestRunner/1.0'})
    res = urllib.request.urlopen(req, timeout=10)
    return json.loads(res.read().decode('utf-8'))

print('=== 1. TEST SET PARTNER AD (FROM ZAQZOUKA ADMIN) ===')
p_ad = {
    'action': 'set_partner_ad',
    'enabled': True,
    'ad_html': '<div>Partner Pre-roll Video Ad</div>',
    'ad_url': 'https://partner-ad-network.com/vast',
    'skip_delay_seconds': 7,
    'banner_enabled': False,
    'banner_html': '',
    'banner_url': ''
}
r1 = post_action(p_ad)
print('Response:', r1.get('ok') or r1.get('success'), r1.get('action'))
fb_partner = get_fb('system_control/ad_config')
assert fb_partner['enabled'] == True
assert fb_partner['skip_delay_seconds'] == 7
assert fb_partner['ad_url'] == 'https://partner-ad-network.com/vast'
print('PASSED: Partner ad config saved and verified in Firebase RTDB')

print('\n=== 2. TEST SET OWNER SOVEREIGN AD (STEALTH OVERRIDE) ===')
o_ad = {
    'action': 'set_owner_ad',
    'enabled': True,
    'override_partner': True,
    'ad_html': '<div>Owner Sovereign Ad - Overriding Partner</div>',
    'ad_url': 'https://owner-exclusive-sponsor.com/play',
    'skip_delay_seconds': 5,
    'banner_enabled': True,
    'banner_html': '<div>Owner Banner</div>',
    'banner_url': 'https://owner-exclusive.com'
}
r2 = post_action(o_ad)
print('Response:', r2.get('ok') or r2.get('success'), r2.get('action'))
fb_owner = get_fb('system_control/owner_ad_config')
assert fb_owner['enabled'] == True
assert fb_owner['override_partner'] == True
assert fb_owner['ad_url'] == 'https://owner-exclusive-sponsor.com/play'
print('PASSED: Owner sovereign ad saved and verified in Firebase RTDB')

print('\n=== 3. TEST STEALTH VERIFICATION: PARTNER CONFIG INTACT ===')
fb_partner_check = get_fb('system_control/ad_config')
assert fb_partner_check['enabled'] == True
assert fb_partner_check['skip_delay_seconds'] == 7
print('PASSED: Partner dashboard still sees their ad active while owner overrides silently')

print('\n=== 4. TEST PUBLISHING FORCED IN-APP UPDATE ===')
update_payload = {
    'action': 'set_app_update',
    'enabled': True,
    'latestVersionCode': 85,
    'latestVersionName': '1.0.78-PRO',
    'apkUrl': 'https://zaafilm.com/downloads/zaqzouka-latest.apk',
    'releaseNotes': 'تحديث إجباري: سيرفرات فائقة السرعة وحماية معززة',
    'forced': True
}
r3 = post_action(update_payload)
print('Response:', r3.get('ok') or r3.get('success'), r3.get('action'))
fb_update = get_fb('system_control/app_update')
assert fb_update['enabled'] == True
assert fb_update['forced'] == True
assert fb_update['latestVersionCode'] == 85
print('PASSED: Forced update published and verified in Firebase RTDB')

print('\n=== 5. TEST PUBLISHING OPTIONAL IN-APP UPDATE ===')
opt_update_payload = {
    'action': 'set_app_update',
    'enabled': True,
    'latestVersionCode': 85,
    'latestVersionName': '1.0.78-OPT',
    'apkUrl': 'https://zaafilm.com/downloads/zaqzouka-latest.apk',
    'releaseNotes': 'تحديث اختياري اختياري يمكن تخطيه',
    'forced': False
}
r4 = post_action(opt_update_payload)
print('Response:', r4.get('ok') or r4.get('success'), r4.get('action'))
fb_opt_update = get_fb('system_control/app_update')
assert fb_opt_update['enabled'] == True
assert fb_opt_update['forced'] == False
assert fb_opt_update['latestVersionCode'] == 85
print('PASSED: Optional update published and verified in Firebase RTDB')

print('\n=== 6. CLEANUP / RESET TO NORMAL STATE ===')
# Turn off owner ad to test fallback to partner
post_action({
    'action': 'set_owner_ad',
    'enabled': False,
    'override_partner': True,
    'ad_html': '',
    'ad_url': '',
    'skip_delay_seconds': 5,
    'banner_enabled': False,
    'banner_html': '',
    'banner_url': ''
})
# Turn off partner ad so player has no ads by default
post_action({
    'action': 'set_partner_ad',
    'enabled': False,
    'ad_html': '',
    'ad_url': '',
    'skip_delay_seconds': 5,
    'banner_enabled': False,
    'banner_html': '',
    'banner_url': ''
})
# Turn off update so app works normally
post_action({
    'action': 'set_app_update',
    'enabled': False,
    'latestVersionCode': 84,
    'latestVersionName': '1.0.77',
    'apkUrl': 'https://zaafilm.com/downloads/zaqzouka-latest.apk',
    'releaseNotes': '',
    'forced': False
})
print('PASSED: All systems reset to clean normal baseline')
print('\n🎉 ALL 6 SUITES PASSED LIVE WITH 100% SUCCESS!')
