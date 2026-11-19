# 🛡️ Ad Bypass & Redirect Handler — توثيق كامل
## مشروع MadarTV / مشهد — محرك السكرابر العربي

---

## 📋 ملخص

الملف: `scrapers/adbypass.py`

يعالج **10 أنماط** مختلفة من الإعلانات والـ redirects الموجودة في المواقع العربية للأفلام والمسلسلات، ويدمجها تلقائياً مع:
- `scrapers/base.py` — عبر `extract_servers_smart()`
- `scrapers/extractors.py` — عبر `_resolve_redirect()` في `StreamExtractor.resolve()`

---

## 🔟 الأنماط المعالجة

### Pattern 1 — Ad Shortener Redirect Chains

**ما هو:** مواقع تضغط/تشفر الرابط الحقيقي خلف نطاقات إعلانية مثل:
- `shot90.com` — يشفر الرابط بـ **base64** في parameter اسمه `url=`
- `fc.lc`, `bc.vc`, `za.gl`, `ur.ly`, `oke.io` — سلاسل redirect بـ 301/302
- `ouo.io`, `sh.st` — يتطلبان cookies/POST token
- `shrinkme.io`, `adfoc.us`, `exe.io` — redirect chains

**مثال حقيقي (من esheeq/قصة عشق):**
```
Input:  https://shot90.com/article/1789393512?url=aHR0cHM6Ly9xZXNzZXQubmV0L2NsYXJ1cy9oYWxlZi1lcGlzb2RlLTQyLw==
Output: https://qesset.net/clarus/halef-episode-42/
```

**الحل:**
```python
# shot90.com — base64 decode مباشر
def _decode_shot90(url):
    encoded = parse_qs(urlparse(url).query).get("url", [None])[0]
    return base64.b64decode(encoded + "=" * (-len(encoded) % 4)).decode()

# بقية الـ shorteners — follow redirects
response = session.get(url, allow_redirects=True)
real_url = response.url
```

---

### Pattern 2 — Meta Refresh Redirect

**ما هو:** بعض صفحات الـ "watch" تعمل redirect تلقائي بعد x ثانية:
```html
<meta http-equiv="refresh" content="5; url=https://embed.server/xxx">
```

**الحل:**
```python
META_REFRESH_RE = re.compile(r'content=["\'][0-9]+;\s*url=(https?://[^"\']+)["\']', re.I)
redirect_url = META_REFRESH_RE.search(html).group(1)
# ثم follow الـ URL الجديد
```

---

### Pattern 3 — JavaScript window.location Redirect

**ما هو:** بعض الصفحات تحوّل بـ JavaScript:
```javascript
window.location = "https://real.embed.site/player/xxx";
// أو
location.replace("https://real.embed.site/player/xxx");
```

**الحل:**
```python
JS_LOCATION_RE = re.compile(
    r'window\.location(?:\.href)?\s*=\s*["\']+(https?://[^"\']+)["\']', re.I
)
redirect_url = JS_LOCATION_RE.search(html).group(1)
```

---

### Pattern 4 — DooPlay WordPress AJAX Server Loading ⭐

**ما هو:** النمط الأكثر شيوعاً في المواقع العربية الحديثة. المواقع اللي بتستخدمه:
- **SpaceMov** (spacemov.xyz)
- **MovieWatcher** (moviewatcher-offcial.cyou)
- **Cima4Up** (cima4up-official.cyou)
- وعشرات المواقع الأخرى

**كيف يعمل:**
1. صفحة التفاصيل فيها `nonce` و `post_id` مخبيين في script tag
2. لما المستخدم يضغط "تشغيل" → JavaScript يبعت POST request
3. الـ server بيرجع JSON فيه embed URL

```python
# Step 1: Extract from detail page HTML
nonce   = re.search(r'"nonce"\s*:\s*"([a-f0-9]+)"', html).group(1)
post_id = re.search(r'"post_id"\s*:\s*"?(\d+)"?', html).group(1)

# Step 2: POST to WordPress AJAX
resp = session.post(
    base_url + "/wp-admin/admin-ajax.php",
    data={
        "action": "doo_player_ajax",
        "post":   post_id,
        "nonce":  nonce,
    },
    headers={"X-Requested-With": "XMLHttpRequest"},
)

# Step 3: Parse response
data = resp.json()
embed_html = data.get("embed") or data.get("link")
iframe_src = re.search(r'src=["\']([^"\']+)["\']', embed_html).group(1)
```

---

### Pattern 5 — FaselHD AJAX Embed (data-embed ID)

**ما هو:** موقع FaselHD (وأمثاله) يستخدم نظام embed مختلف:
```html
<div data-embed="12345" class="player-btn">تشغيل</div>
```
ثم JavaScript يبعت:
```
POST /ajax/embed
Body: id=12345
Response: {"link": "https://embed.server/xxx"}
```

**الحل:**
```python
embed_id = soup.select_one("[data-embed]").get("data-embed")
resp = session.post(base_url + "/ajax/embed", data={"id": embed_id})
embed_url = resp.json().get("link")
```

> ⚠️ **FaselHD محمي بـ Cloudflare** — الـ pattern ده موجود في الكود لكن الموقع لا يعمل بدون browser session حقيقي.

---

### Pattern 6 — WordPress get_server_link AJAX

**ما هو:** نسخة أبسط من Pattern 4، بتستخدمها مواقع مثل WeCima وAkwam:
```
POST /wp-admin/admin-ajax.php
Body: action=get_server_link&id=SERVER_ID&nonce=NONCE
```

**الحل:**
```python
server_id = soup.select_one("[data-id]").get("data-id")
resp = session.post(ajax_url, data={"action": "get_server_link", "id": server_id, "nonce": nonce})
embed_url = resp.json().get("link")
```

---

### Pattern 7 — data-* HTML Attributes (الأسرع والأسهل) ⭐

**ما هو:** بعض المواقع تضع الـ embed URLs مباشرة في HTML attributes:
```html
<li data-link="https://embed.server/xxx" data-quality="1080p">سيرفر 1</li>
<a data-embed="https://vidmoly.to/xxx" data-res="HD">تشغيل</a>
<div data-url="https://voe.sx/xxx">مشاهدة</div>
```

**الحل:**
```python
DATA_ATTRS = ["data-link", "data-url", "data-embed", "data-src", "data-href", "data-redirect"]
for attr in DATA_ATTRS:
    for el in soup.find_all(attrs={attr: True}):
        embed_url = el.get(attr)
        if embed_url and embed_url.startswith("http"):
            # process embed_url
```

---

### Pattern 8 — Base64-Encoded Embed URLs in Scripts

**ما هو:** بعض المواقع تحوّل (obfuscate) الـ embed URLs بـ base64 داخل الـ JavaScript:
```javascript
var player = atob("aHR0cHM6Ly92aWRtb2x5LnRvL2UveHh4eHh4");
// decodes to: https://vidmoly.to/e/xxxxxx
```

**الحل:**
```python
BASE64_BLOB_RE = re.compile(r'["\']([A-Za-z0-9+/]{30,}={0,2})["\']')
for match in BASE64_BLOB_RE.finditer(html):
    try:
        decoded = base64.b64decode(match.group(1) + "==").decode("utf-8")
        if decoded.startswith("http") and "." in decoded:
            # Found encoded URL!
    except Exception:
        pass
```

---

### Pattern 9 — Cloudflare / Bot Protection ❌

**ما هو:** مواقع مثل FaselHD وShahid4U محمية بـ Cloudflare Bot Management. الـ HTML اللي بيرجع هو صفحة JavaScript challenge مش المحتوى الحقيقي.

**لماذا لا يمكن تخطيه:**
- يحتاج browser حقيقي (Chrome/Firefox) لحل JS challenge
- يحتاج cookies صالحة من session حقيقي
- **لا يمكن تخطيه بـ requests فقط**

**الحل الممكن:**
```python
# استخدام Playwright (browser automation)
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto("https://faselhd.top/movie-xxx/")
    page.wait_for_load_state("networkidle")
    html = page.content()
    browser.close()
```

> المواقع المتأثرة: `faselhd.top`, `shahid4u.cam`, `arablionz.cam` (SPA/JS-rendered)

---

### Pattern 10 — HTTP 301/302 Chain with Tracking Params

**ما هو:** بعض الروابط تمر بسلسلة redirects وتضيف tracking parameters للـ URL النهائي:
```
https://site.com/out?ref=sidebar&utm_source=widget&url=https://real.site/
```

**الحل:**
```python
# requests يفعل هذا تلقائياً مع allow_redirects=True
response = session.get(url, allow_redirects=True)
final_url = response.url

# ثم نحذف الـ tracking params
JUNK_PARAMS = {"utm_source", "utm_medium", "utm_campaign", "ref", "referrer", "fbclid"}
clean_url = strip_tracking_params(final_url)
```

---

## 🔧 كيفية الاستخدام

### الاستخدام البسيط — `extract_servers_smart()`

في أي scraper يرث من `BaseScraper`:
```python
# بدل extract_servers() القديم:
servers = scraper.extract_servers_smart(movie_url)
# بيعمل كل الأنماط تلقائياً
```

### الاستخدام المباشر — `resolve_page()`

```python
from scrapers.adbypass import resolve_page, make_bypass_session

session = make_bypass_session()
html = session.get(url).text
from bs4 import BeautifulSoup
soup = BeautifulSoup(html, "lxml")

result = resolve_page(url=url, session=session, html=html, soup=soup, base_url="https://site.com")
# result = {
#   "servers": [{"name": "...", "embed_url": "...", "quality": "HD"}],
#   "redirected_to": None,
#   "dooplay": True/False,
# }
```

### حل ad shortener فقط:
```python
from scrapers.adbypass import bypass_ad_shortener, make_bypass_session

session = make_bypass_session()
real_url = bypass_ad_shortener("https://shot90.com/article/xxx?url=aHR0cHM6...", session)
# returns: "https://qesset.net/episode-42/"
```

### التحقق من meta refresh:
```python
from scrapers.adbypass import extract_meta_refresh, extract_js_redirect

redirect = extract_meta_refresh(html)
# or
redirect = extract_js_redirect(html, base_url="https://site.com")
```

---

## 📊 نتيجة الدمج على المزودات

| المزود | الـ Pattern المستخدم | نتيجة |
|--------|----------------------|--------|
| Esheeq (قصة عشق) | Pattern 1 (shot90 base64) | ✅ 33 مسلسل |
| SpaceMov | Pattern 4 (DooPlay AJAX) | ✅ 125 نتيجة |
| MovieWatcher | Pattern 4 (DooPlay AJAX) | ✅ 89 نتيجة |
| Cima4Up | Pattern 4 (DooPlay AJAX) | ✅ 211 نتيجة |
| ArabSeed | Pattern 7 (data-link) + MySeed extractor | ✅ 33 نتيجة |
| WeCima | Pattern 6 (WP AJAX) + Pattern 7 | ✅ 81 نتيجة |
| FaselHD | Pattern 9 (Cloudflare) | ❌ Browser required |
| ArabLionz | Pattern 9 (SPA/JS-rendered) | ❌ Browser required |

---

## 🔄 تدفق البيانات الكامل

```
User requests movie URL
        ↓
BaseScraper.extract_servers_smart(url)
        ↓
fetch_html(url)  ←── mirror failover
        ↓
resolve_page(url, session, html, soup, base_url)
        ├── Pattern 2: Meta refresh? → follow redirect → restart
        ├── Pattern 3: JS location? → follow redirect → restart
        ├── Pattern 7: data-* attrs → collect embed URLs
        │       └── Pattern 1: ad shortener? → decode/follow
        ├── Pattern 4: DooPlay nonce+postid? → AJAX POST → collect embed URLs
        └── Pattern 8: base64 blobs? → decode → collect embed URLs
        ↓
For each embed_url:
        └── StreamExtractor.resolve(embed_url)
                ├── Pattern 1: ad shortener → _resolve_redirect() → real embed URL
                ├── Direct .m3u8/.mp4 → return as-is
                ├── Vidmoly → extract_vidmoly()
                ├── Voe.sx → extract_voe()
                ├── Uqload → extract_uqload()
                ├── DoodStream → extract_dood()
                ├── Streamtape → extract_streamtape()
                ├── MySeed Gateway → extract_myseed_gateway()
                └── Generic → extract_generic()
        ↓
List[StreamServer] with direct_url, quality, headers
        ↓
Flutter/Kotlin app plays the stream ▶️
```

---

## ⚠️ مواقع تحتاج Playwright (Browser Mode)

الـ 4 مواقع دي **مش ممكن تشتغل بـ requests فقط**:

| الموقع | السبب |
|--------|-------|
| faselhd.top | Cloudflare Bot Management |
| shahid4u.cam | Cloudflare + JS Challenge |
| arablionz.cam | SPA — المحتوى يتحمل بـ JS (React/Vue) |
| moviflex.net | الدومين ميت تماماً |

**الحل للـ Playwright:**
```bash
pip install playwright
playwright install chromium
```
```python
from playwright.sync_api import sync_playwright

def fetch_with_browser(url: str) -> str:
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(user_agent="Mozilla/5.0...")
        page = ctx.new_page()
        page.goto(url, wait_until="networkidle", timeout=30000)
        content = page.content()
        browser.close()
        return content
```

---

## 📁 الملفات المرتبطة

| الملف | الوظيفة |
|-------|---------|
| [`scrapers/adbypass.py`](../scrapers/adbypass.py) | الموديول الرئيسي (10 patterns) |
| [`scrapers/base.py`](../scrapers/base.py) | `extract_servers_smart()` |
| [`scrapers/extractors.py`](../scrapers/extractors.py) | `StreamExtractor.resolve()` with `_resolve_redirect()` |
| [`scrapers/providers/esheeq.py`](../scrapers/providers/esheeq.py) | مثال تطبيقي (shot90 decode) |
