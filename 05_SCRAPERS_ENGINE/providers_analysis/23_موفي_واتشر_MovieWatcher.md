# توثيق وتحليل منصة موفي واتشر (MovieWatcher) الشامل

---

## 1. بطاقة تعريف المنصة (Overview)

| البند | التفاصيل |
| :--- | :--- |
| **اسم المنصة** | موفي واتشر — MovieWatcher |
| **الدومين الشغال حالياً** | `https://moviewatcher-offcial.cyou` |
| **دومينات بديلة واحتياطية** | `https://moviewatcher.is` \| `https://moviewatcher.site` \| `https://moviewatcher.io` |
| **نوع المحتوى** | أفلام ومسلسلات عالمية، هوليوود، سينما أجنبية، تصنيفات IMDb الأعلى تقييماً |
| **محرك الموقع (CMS)** | واجهة فهرسة سحابية متقدمة مع نظام توجيه للمشغلات السريعة |
| **طبيعة الحماية** | شهادة SSL مع نظام توجيه وسائط CDN عالمي |

---

## 2. بنية الروابط ومحرك البحث (Search & URL Routing)

### أ. نقطة البحث المباشر (Search Endpoint)
* **الرابط:** `https://moviewatcher-offcial.cyou/search/?q={query}`
* **طريقة الإرسال:** `GET`
* **معامل البحث:** `q` (مثال: `https://moviewatcher-offcial.cyou/search/?q=interstellar`)

### ب. هيكل روابط الأقسام:
* **الأفلام (Movies):** `https://moviewatcher-offcial.cyou/movies/`
* **المسلسلات (TV Shows):** `https://moviewatcher-offcial.cyou/tv-shows/`
* **الأعلى تقييماً (Top IMDb):** `https://moviewatcher-offcial.cyou/top-imdb/`
* **الرائج (Trending):** `https://moviewatcher-offcial.cyou/trending/`
* **صفحة الفيلم:** `https://moviewatcher-offcial.cyou/title/{slug}-{tmdb_id}/`

---

## 3. محددات الاستخراج (DOM Selectors)

### أ. بطاقات الأفلام والمسلسلات:
* **حاوية الشبكة:** `div.media-grid` أو `section.results`
* **عنصر البطاقة:** `div.media-card` أو `a[href*="/title/"]`
* **رابط العنوان:** `a[href*="/title/"]`
* **اسم العمل:** `h3.media-title` أو `span.title`
* **صورة البوستر:** `img[src*="/posters/"]` أو `img[src*="tmdb.org"]`
* **التقييم وسنة الإصدار:** `span.rating` و `span.year`

---

## 4. هندسة المشاهدة وسيرفرات البث (Embed & Multi-Source Resolvers)

تعتمد المنصة على شبكة سيرفرات سحابية متعددة المصادر (Cloud Streaming Aggregators):
1. **مزودات البث المدمجة:**
   * **SuperEmbed / 2Embed:** تدعم تشغيل تدفقات HLS بجميع الجودات (1080p, 720p, 480p, 360p).
   * **VidSrc API:** توفر تشغيل فوري بدون إعلانات وتدعم ترجمات Soft Subtitles بلغات متعددة ومنها العربية.
   * **VidCloud / UpCloud:** خوادم بث سريعة ذات معدل بت (Bitrate) عالي.

---

## 5. خطة التكامل مع تطبيق زقزوقة IPTV

1. **البحث المباشر:** يرسل التطبيق طلب استعلام لنقطة البحث وسحب المعرف الرقمي للفيلم (TMDB ID).
2. **استدعاء السيرفرات المتوافقة:**
   * يتم استدعاء مسار VidSrc المباشر وتمريره لـ ExoPlayer.
   * يتم استخراج جودة 1080p كجودة افتراضية، مع إتاحة قائمة اختيار الجودات (Track Selector) في المشغل.
   * في حال رغبة المستخدم في المشاهدة مع إعلانات محظورة، يضمن مشغل WebView تصفية جميع إعلانات البث الخارجية.
