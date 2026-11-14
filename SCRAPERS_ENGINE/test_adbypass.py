"""Unit tests for adbypass module."""
import sys, base64
sys.path.insert(0, '.')

from scrapers.adbypass import (
    _decode_shot90, _is_ad_shortener,
    extract_meta_refresh, extract_js_redirect,
    extract_dooplay_params, extract_data_links,
    extract_base64_urls, strip_tracking_params,
)
from bs4 import BeautifulSoup

passed = 0
failed = 0

def test(name, expr, expected=True):
    global passed, failed
    if isinstance(expr, bool) and expected is True:
        result = expr
    else:
        result = (expr == expected)
    status = "PASS" if result else "FAIL"
    if not result:
        failed += 1
        print(f"  [{status}] {name}")
        print(f"         got: {expr!r}")
        print(f"         exp: {expected!r}")
    else:
        passed += 1
        print(f"  [{status}] {name}")

print("\n=== UNIT TESTS: adbypass.py ===\n")

# --- Test 1: shot90 decode ---
url_shot90 = 'https://shot90.com/article/789?url=aHR0cHM6Ly9xZXNzZXQubmV0L2NsYXJ1cy9oYWxlZi1lcGlzb2RlLTQyLw=='
decoded = _decode_shot90(url_shot90)
test("shot90 base64 decode", decoded, 'https://qesset.net/clarus/halef-episode-42/')

# --- Test 2: ad shortener detection ---
test("shot90.com is ad shortener",   _is_ad_shortener('https://shot90.com/article/123?url=abc'), True)
test("fc.lc is ad shortener",        _is_ad_shortener('https://fc.lc/abc'), True)
test("vidmoly.to is NOT shortener",  _is_ad_shortener('https://vidmoly.to/e/abc123') == False)
test("akw.cam is NOT shortener",     _is_ad_shortener('https://akw.cam/movie/test') == False)

# --- Test 3: meta refresh ---
html_meta = '<meta http-equiv="refresh" content="5; url=https://real.site/watch/">'
result_meta = extract_meta_refresh(html_meta)
test("meta refresh extract", result_meta, 'https://real.site/watch/')

html_meta2 = '<meta content="0;url=https://embed.site/player/" http-equiv="refresh">'
result_meta2 = extract_meta_refresh(html_meta2)
test("meta refresh extract (order variant)", result_meta2, 'https://embed.site/player/')

# --- Test 4: JS redirect ---
html_js1 = 'window.location.href = "https://embed.server/player/abc";'
test("JS window.location.href redirect", extract_js_redirect(html_js1), 'https://embed.server/player/abc')

html_js2 = "window.location = 'https://another.server/player/';"
test("JS window.location redirect",      extract_js_redirect(html_js2), 'https://another.server/player/')

html_js3 = "location.replace('https://replace.server/');"
result_js3 = extract_js_redirect(html_js3)
test("JS location.replace redirect",     result_js3 is not None)

# --- Test 5: DooPlay params ---
html_doo = 'var dooparams = {"nonce":"a1b2c3d4e5f6", "post_id":"98765"};'
nonce, post_id = extract_dooplay_params(html_doo)
test("DooPlay nonce extract",   nonce,   'a1b2c3d4e5f6')
test("DooPlay post_id extract", post_id, '98765')

html_doo2 = 'data-post="12345" data-nonce="xyz" class="doo-player"'
html_doo2b = '{"nonce":"xyz"}' + html_doo2  # nonce still from JSON
n2, pid2 = extract_dooplay_params(html_doo2b)
test("DooPlay post from data-post attr", pid2, '12345')

# --- Test 6: data-link extraction ---
html_data = '<li data-link="https://vidmoly.to/e/abc123" data-quality="1080p">Server 1</li>'
soup = BeautifulSoup(html_data, 'lxml')
links = extract_data_links(soup, 'https://site.com')
test("data-link extraction count",  len(links), 1)
test("data-link URL value",         links[0]['embed_url'], 'https://vidmoly.to/e/abc123')

html_data2 = '<div data-url="https://voe.sx/e/xyz789" data-embed="https://uqload.co/e/aaa"></div>'
soup2 = BeautifulSoup(html_data2, 'lxml')
links2 = extract_data_links(soup2)
test("data-url + data-embed extraction count", len(links2), 2)

# --- Test 7: base64 URLs ---
real_url = 'https://vidmoly.to/e/testxxx123456'
encoded  = base64.b64encode(real_url.encode()).decode()
html_b64 = f'var src = atob("{encoded}"); player.load(src);'
urls = extract_base64_urls(html_b64)
test("base64 URL detection",  real_url in urls)

# Short strings (< 30 chars) should NOT trigger
html_b64_short = 'var x = "aGVsbG8=";'  # "hello" — too short, not a URL
urls_short = extract_base64_urls(html_b64_short)
test("base64 short string skip", len(urls_short) == 0)

# --- Test 8: tracking param strip ---
dirty = 'https://site.com/movie/?utm_source=facebook&utm_medium=cpc&ref=sidebar&page=2'
clean = strip_tracking_params(dirty)
test("tracking params stripped",     'utm_source' not in clean)
test("tracking params stripped ref", 'ref' not in clean)
test("legit param preserved",        'page=2' in clean)

print(f"\n{'='*40}")
print(f"Results: {passed} PASSED  |  {failed} FAILED")
if failed == 0:
    print("ALL TESTS PASSED ✅")
else:
    print(f"⚠️  {failed} tests failed — see above")
    sys.exit(1)
