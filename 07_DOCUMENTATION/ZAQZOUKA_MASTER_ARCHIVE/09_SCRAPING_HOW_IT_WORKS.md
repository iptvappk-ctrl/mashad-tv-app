# 🕷️ 09 — دليل سحب الأفلام والمسلسلات (Scraping Engine Guide)

> كيف يجلب النظام الأفلام والمسلسلات من المواقع؟ كل شيء مشروح هنا.

---

## 🏗️ هيكل محرك السكرابينج

```
05_SCRAPERS_ENGINE/
├── SCRAPERS_ENGINE/              ← المحرك الجديد الكامل
│   ├── run_scraper.py            ← نقطة التشغيل الرئيسية
│   ├── requirements.txt          ← المتطلبات
│   └── scrapers/
│       ├── base.py               ← الكلاس الأساسي (BaseScraper)
│       ├── models.py             ← نماذج البيانات
│       ├── extractors.py         ← استخراج روابط الفيديو
│       ├── adbypass.py           ← تجاوز الإعلانات وإعادة التوجيه
│       ├── manager.py            ← مدير المزودين
│       ├── api.py                ← Flask API
│       └── providers/            ← مزودو المحتوى (23 موقع)
│           ├── arabseed.py
│           ├── egybest.py
│           ├── wecima.py
│           ├── cima4u.py
│           └── ... (23 مزود)
└── scrapers/                     ← نسخة أقدم (نفس الكود)
```

---

## 🔄 كيف يعمل النظام؟ (خطوة بخطوة)

### المرحلة 1: طلب المستخدم
```
المستخدم يفتح التطبيق
       ↓
Flutter App تطلب قائمة أفلام/مسلسلات
       ↓
Native Kotlin Bridge (NativeScraperService)
       ↓
يستدعي Python scraper engine عبر subprocess
```

### المرحلة 2: اختيار المزود
```python
# manager.py
class ScraperManager:
    def get_scraper(self, provider_id: str) -> BaseScraper:
        # يختار المزود المناسب
        # لو المزود فشل → يجرب المرايا (mirror_urls)
```

### المرحلة 3: جلب الكتالوج
```python
# base.py
class BaseScraper(ABC):
    def get_movies(self, page=1) -> List[MediaItem]:
        # 1. يرسل HTTP request بـ Chrome User-Agent
        # 2. يحلل HTML بـ BeautifulSoup
        # 3. يستخرج: عنوان، صورة، رابط، السنة، التقييم
        # 4. يعيد قائمة MediaItem

    def get_series(self, page=1) -> List[MediaItem]:
        # نفس العملية للمسلسلات

    def search(self, query: str) -> List[MediaItem]:
        # البحث بالاسم
```

### المرحلة 4: جلب تفاصيل + روابط
```python
def get_detail(self, url: str) -> MediaDetail:
    # 1. يفتح صفحة الفيلم/المسلسل
    # 2. يستخرج: وصف، ممثلين، مدة، تقييم
    # 3. يستخرج روابط السيرفرات (ودود، Govid, Doodstream, إلخ)
    # 4. يعيد MediaDetail مع قائمة StreamServer
```

### المرحلة 5: استخراج رابط الفيديو المباشر
```python
# extractors.py
class StreamExtractor:
    def extract(self, server_url: str) -> Optional[str]:
        # لكل نوع سيرفر:
        # - Doodstream → يستخرج /d/ token
        # - Govid → يفك تشفير JS
        # - Streamtape → يفك obfuscated URL
        # - HLS مباشر → يرجعه كما هو
        # يرجع رابط HLS أو MP4 مباشر
```

---

## 🌐 المواقع المدعومة (23 موقع)

| المزود | نوع المحتوى | الموثوقية |
|--------|------------|-----------|
| `arabseed` | أفلام + مسلسلات عربية | ⭐⭐⭐⭐⭐ |
| `egybest` | أفلام + مسلسلات | ⭐⭐⭐⭐⭐ |
| `wecima` | أفلام + مسلسلات + أنمي | ⭐⭐⭐⭐ |
| `cima4u` | أفلام + مسلسلات | ⭐⭐⭐⭐ |
| `cima4up` | أفلام | ⭐⭐⭐ |
| `cimaclub` | أفلام + مسلسلات | ⭐⭐⭐⭐ |
| `cimanow` | أفلام جديدة | ⭐⭐⭐ |
| `cimawbas` | محتوى عربي | ⭐⭐⭐ |
| `cinemaforall` | أفلام | ⭐⭐⭐ |
| `egydead` | أفلام | ⭐⭐⭐ |
| `egybest` | الأفضل في مصر | ⭐⭐⭐⭐⭐ |
| `faselhd` | HD فقط | ⭐⭐⭐⭐ |
| `fushaar` | مسلسلات خليجية | ⭐⭐⭐ |
| `lodynet` | أفلام + مسلسلات | ⭐⭐⭐ |
| `movies4u` | أفلام أجنبية | ⭐⭐⭐ |
| `moviewatcher` | أفلام | ⭐⭐ |
| `moviflex` | أفلام | ⭐⭐⭐ |
| `movizland` | أفلام | ⭐⭐⭐⭐ |
| `prestige` | محتوى مميز | ⭐⭐⭐ |
| `shahid4u` | شبيه شاهد | ⭐⭐⭐ |
| `spacemov` | أفلام | ⭐⭐ |
| `topcinema` | سينما | ⭐⭐⭐ |
| `akwam` | شامل | ⭐⭐⭐⭐ |
| `arablionz` | مسلسلات | ⭐⭐⭐ |
| `esheeq` | رومانسي | ⭐⭐ |

---

## 🔧 نظام تجاوز الإعلانات (AdBypass)

```python
# adbypass.py - يتعامل مع:
resolve_page()          # يتبع كل redirects تلقائياً
bypass_ad_shortener()   # يتجاوز: shorte.st, ouo.io, linkvertise
extract_meta_refresh()  # يستخرج من <meta http-equiv="refresh">
extract_js_redirect()   # يفك JS obfuscation
extract_dooplay_params()# يستخرج parameters من DooPlay theme
fetch_dooplay_servers() # يجلب قائمة سيرفرات DooPlay بالـ AJAX
fetch_wp_server_link()  # يجلب روابط WordPress servers
make_bypass_session()   # يصنع session بـ headers تشبه Chrome
```

---

## 📡 أنواع سيرفرات الفيديو

| السيرفر | النوع | طريقة الاستخراج |
|---------|-------|----------------|
| Doodstream | embed | `/d/` token + cookie |
| Govid | embed | JS decode |
| Streamtape | embed | obfuscated URL |
| Voe.sx | embed | JS variable |
| Uptostream | embed | API call |
| Vidhide | embed | JS decode |
| HLS مباشر | stream | يُستخدم مباشرة |
| MP4 مباشر | file | يُستخدم مباشرة |

---

## 🚀 تشغيل السكرابر

```bash
# من مجلد SCRAPERS_ENGINE
pip install -r requirements.txt

# جلب أفلام arabseed
python run_scraper.py --provider arabseed --type movies

# بحث
python run_scraper.py --provider egybest --search "الفيل الأزرق"

# تفاصيل فيلم
python run_scraper.py --provider wecima --detail "https://wecima.show/watch/..."

# تشغيل API
python -m scrapers.api  # يبدأ على http://localhost:5000
```

---

## 🔗 تكامل Flutter ↔ Python

```kotlin
// MadarFlutter/services/NativeScraperService.kt
// يستدعي Python عبر MethodChannel
_native.invokeMethod('scrape', {
  'provider': 'arabseed',
  'action': 'movies',
  'page': 1
})
```

```python
# النتيجة: JSON قائمة أفلام
[{
  "id": "movie_123",
  "title": "الفيل الأزرق 2",
  "year": 2019,
  "rating": 8.2,
  "poster": "https://...",
  "url": "https://arabseed.com/watch/...",
  "type": "movie"
}]
```

---

## ⚠️ ملاحظات مهمة

> [!WARNING]
> المواقع قد تغير domain بشكل متكرر. في هذه الحالة:
> 1. افتح `providers/[اسم_الموقع].py`
> 2. غيّر `base_url` و `mirror_urls`

> [!TIP]
> لإضافة موقع جديد:
> 1. انسخ `providers/arabseed.py`
> 2. غيّر `provider_id`, `base_url`, والـ selectors
> 3. أضفه في `manager.py`

> [!NOTE]
> `adbypass.py` يتعامل مع معظم أنواع الحماية تلقائياً.
> لو موقع جديد عنده حماية مختلفة، أضف handler في `adbypass.py`

---

*تاريخ الإنشاء: نوفمبر 2026*
