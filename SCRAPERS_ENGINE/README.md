# 🎬 SCRAPERS ENGINE — محرك السكرابر العربي
### MadarTV / مشهد — Flutter IPTV App
#### آخر تحديث: 2026-11-14

---

## 📦 محتويات الفولدر

```
SCRAPERS_ENGINE/
├── run_scraper.py                      ← Entry point (CLI + API server)
├── requirements.txt                    ← Python dependencies
├── README.md                           ← هذا الملف
├── CODEX_INTEGRATION_PROMPT.md         ← برومت الدمج الكامل مع Flutter/Android (37KB)
│
├── scrapers/                           ← Python package
│   ├── models.py                       ← MediaItem, Episode, StreamServer, Season
│   ├── base.py                         ← BaseScraper + extract_servers_smart()
│   ├── adbypass.py                     ← ✨ 10-Pattern Ad/Redirect Bypass Engine
│   ├── extractors.py                   ← StreamExtractor (Vidmoly, Voe, Dood, etc.)
│   ├── manager.py                      ← ScraperManager (parallel search & fetch)
│   ├── cli.py                          ← Click CLI
│   ├── api.py                          ← FastAPI REST server (8 endpoints)
│   └── providers/                      ← 24 مزود
│       ├── akwam.py          ✅
│       ├── arabseed.py       ✅
│       ├── wecima.py         ✅
│       ├── egybest.py        ✅
│       ├── egydead.py        ✅
│       ├── cimaclub.py       ✅
│       ├── cimanow.py        ✅
│       ├── cimawbas.py       ✅
│       ├── cima4u.py         ✅
│       ├── cima4up.py        ✅  (محدّث)
│       ├── cinemaforall.py   ✅
│       ├── topcinema.py      ✅
│       ├── fushaar.py        ✅
│       ├── lodynet.py        ✅
│       ├── esheeq.py         ✅  (محدّث — shot90 decoder)
│       ├── movies4u.py       ✅
│       ├── moviewatcher.py   ✅  (محدّث)
│       ├── movizland.py      ✅
│       ├── prestige.py       ✅
│       ├── spacemov.py       ✅
│       ├── arablionz.py      ❌  (SPA/JS-rendered)
│       ├── faselhd.py        ❌  (Cloudflare)
│       ├── moviflex.py       ❌  (domain dead)
│       └── shahid4u.py       ❌  (redirect loop)
│
└── docs/
    ├── AD_BYPASS_DOCUMENTATION.md      ← ✨ توثيق محرك تخطي الإعلانات (10 patterns)
    └── 13_UNIFIED_SCRAPERS_SUITE.md    ← التوثيق الهندسي الكامل
```

---

## 🚀 تشغيل سريع

```bash
# 1. تثبيت المكتبات
pip install -r requirements.txt

# 2. اختبار كل المزودات
python run_scraper.py test-all

# 3. بحث
python run_scraper.py search "batman" --provider akwam

# 4. تشغيل REST API server
python run_scraper.py serve --port 8000
# ثم افتح: http://localhost:8000/docs
```

---

## 📊 نتيجة الـ Test-All — **20/24** ✅

| المزود | الدومين | Items | الوضع |
|--------|---------|-------|-------|
| Akwam أكوام | akw.cam | 69 | ✅ |
| ArabSeed عرب سيد | m.myseed.tv | 33 | ✅ |
| WeCima وي سيما | mywecima.shop | 81 | ✅ |
| EgyBest إيجي بست | egybest.live | 20 | ✅ |
| EgyDead إيجي ديد | egidead.cam | 40 | ✅ |
| CimaClub سيما كلوب | cimaclub.live | 60 | ✅ |
| CimaNow سيما ناو | cimanow.cc | 27 | ✅ |
| CimaWbas سيما وبس | cimawbas.rest | 21 | ✅ |
| Cima4U سيما فور يو | c4u.cam | 48 | ✅ |
| **Cima4Up سيما فور أب** | cima4up-official.cyou | **211** | ✅ جديد |
| CinemaForAll سينما للجميع | c4u.cam | 48 | ✅ |
| TopCinema توب سينما | topcinemaa.one | 36 | ✅ |
| Fushaar فشار | fushaar.com | 56 | ✅ |
| **Esheeq قصة عشق** | qesset.net | 33 | ✅ جديد |
| Lodynet لودي نت | lodynet.watch | 12 | ✅ |
| Movies4U موفيز فور يو | movies4u.icu | 40 | ✅ |
| **MovieWatcher موفي واتشر** | moviewatcher-offcial.cyou | **89** | ✅ جديد |
| Movizland موفيز لاند | movizland.life | 10 | ✅ |
| Prestige بريستيج | prstej.asia | 30 | ✅ |
| SpaceMov سبيس موف | spacemov.xyz | 125 | ✅ |
| ArabLionz عرب ليونز | arablionz.cam | 0 | ❌ SPA |
| FaselHD فاصل إعلاني | faselhd.top | 0 | ❌ Cloudflare |
| Moviflex موفي فليكس | moviflex.net | 0 | ❌ Dead |
| Shahid4U شاهد فور يو | shahid4u.cam | 0 | ❌ Redirect |

---

## 🛡️ محرك تخطي الإعلانات — `scrapers/adbypass.py`

يعالج **10 أنماط** تلقائياً:

| Pattern | النمط | مثال |
|---------|-------|------|
| 1 | Ad Shortener (shot90, fc.lc) | قصة عشق → shot90 → base64 decode |
| 2 | Meta Refresh Redirect | صفحات "انتظر 5 ثواني" |
| 3 | JS window.location | صفحات redirect بـ JavaScript |
| **4** | **DooPlay AJAX** ⭐ | SpaceMov, MovieWatcher, Cima4Up |
| 5 | FaselHD data-embed AJAX | FaselHD (Cloudflare-protected) |
| 6 | WordPress get_server_link | WeCima, Akwam |
| **7** | **data-link HTML attrs** ⭐ | معظم المواقع العربية |
| 8 | Base64-encoded URLs | مواقع obfuscated |
| 9 | Cloudflare (❌ Browser req.) | faselhd, shahid4u |
| 10 | HTTP Redirect + Tracking strip | جميع المواقع |

**الاستخدام:**
```python
# Python
servers = scraper.extract_servers_smart(movie_url)

# Or directly
from scrapers.adbypass import resolve_page, make_bypass_session
session = make_bypass_session()
result = resolve_page(url, session, html, soup, base_url)
```

---

## 🔗 ملفات Kotlin الجديدة (في المشروع)

| الملف | المكان | الوظيفة |
|-------|--------|---------|
| `AdBypassHelper.kt` | `MadarFlutter/android/.../data/` | 10-pattern bypass engine |
| `StreamExtractorHelper.kt` | `MadarFlutter/android/.../data/` | 8 stream extractors |

---

## 📖 للدمج مع Flutter
اقرأ: **[CODEX_INTEGRATION_PROMPT.md](./CODEX_INTEGRATION_PROMPT.md)**

للتوثيق التقني للـ Ad Bypass:
اقرأ: **[docs/AD_BYPASS_DOCUMENTATION.md](./docs/AD_BYPASS_DOCUMENTATION.md)**
