# دليل المنظومة الشامل: المحركات، السكرابرات، المشغل، والدمج من الألف إلى الياء (V14)
# Mashad / Zakzouka IPTV & Cinema Ecosystem: Full Technical Architecture Guide

---

## 1. نظرة عامة على بنية النظام (System Architecture Overview)

تم تصميم وتطوير التطبيق ليكون منظومة سينمائية وتلفزيونية متكاملة فلكية الأداء تعتمد على بنية **هجينة متطورة (Hybrid Modern Architecture)** تجمع بين:
1. **واجهة المستخدم الذكية (Flutter Client UI):** لتوفير تجربة مستخدم سلسة، عصرية، وسريعة التجاوب تدعم اللغة العربية بالكامل (RTL)، وتعمل على الهواتف والأجهزة اللوحية وشاشات Android TV.
2. **محركات الاستخراج الأصلية (Native Kotlin High-Performance Engine):** تعمل في عمق نظام أندرويد لمعالجة وفك تشفير الروابط وتجاوز الإعلانات واستخراج السيرفرات بسرعة خيالية ومباشرة بدون استهلاك للذاكرة.
3. **مشغل الفيديو الموحد المتطور (`madar-player`):** مبني بأحدث تقنيات **Jetpack Compose + AndroidX Media3 (ExoPlayer)** مع طبقة هجينة ذكية تدعم تشغيل كافة مصادر الفيديو (HLS, DASH, MP4, Embed Pages, YouTube Cinema-Pure Mode).
4. **نظام التخزين المؤقت الذكي (Multi-Tier Disk & Memory Caching):** يضمن فتح الكتالوج وقوائم المسلسلات والقنوات فورياً حتى مع تقلبات الإنترنت.
5. **نظام التحديث التلقائي الفوري (Firebase Live App Update Engine):** فحص التحديثات وتنزيلها وتثبيتها من داخل التطبيق بضغطة زر واحدة.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Flutter Client Layer (UI)                       │
│  Main Page • Catalog Tab • Series & Movies • Live TV • Recaps (يوتيوب) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ MethodChannel
                                    │ (madar/native, madar/player, media.*)
┌───────────────────────────────────▼────────────────────────────────────┐
│                    Native Android Bridge & Scrapers                    │
│  FlutterMediaBridge • MultiProviderRepository • ArabseedRepository    │
│  AdBypassHelper • StreamExtractorHelper • CatalogDiskCache             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Intent Launch
                                    │ (Playback & Quality Parameters)
┌───────────────────────────────────▼────────────────────────────────────┐
│                   Unified Player Module (`madar-player`)               │
│  Jetpack Compose UI • ExoPlayer Media3 • Smart Bandwidth (HD موفّر)    │
│  Universal TV Cast • YouTube Cinema-Pure Clean Video Engine            │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. محركات الاستخراج ومزودات المحتوى (The 23+ Scrapers Suite)

تم توحيد وهندسة أكثر من 23 مزوداً للمحتوى العربي والأجنبي والآسيوي والرياضي لتعمل بدقة متناهية:

### أ. عرب سيد (Arabseed Core Engine)
- **الملف المصدري:** `MadarFlutter/android/app/src/main/kotlin/com/madar/tv/data/ArabseedRepository.kt`
- **آلية العمل:**
  1. استخراج الكتالوج عبر فئات الأفلام والمسلسلات الحديثة والقديمة مع ترقيم الصفحات (`/movies/page/X/`, `/series/page/X/`).
  2. التعامل مع بنية المسلسلات: فحص روابط المواسم واستخراج قائمة الحلقات بالترتيب الزمني الدقيق عبر `SeriesCatalogParser.kt`.
  3. استخراج خوادم المشاهدة: تجاوز طبقات التحويل وجلب خوادم المشاهدة مثل سيرفر عرب سيد المباشر (`MySeed / Uqload`) وسيرفرات البث الخارجية (VidBom, Dood, StreamWish).

### ب. الكتالوج الموحد ومزودات الأفلام والمسلسلات (Multi-Provider Aggregator)
- **الملف المصدري:** `MadarFlutter/android/app/src/main/kotlin/com/madar/tv/data/MultiProviderRepository.kt`
- **المزودات المدعومة:**
  - **Akwam (أكوام):** سحب الأقسام واستخراج روابط الفيديو المباشرة عبر معالجة معرفات المحتوى وروابط التحميل والمشاهدة.
  - **Cima4U (سيما فور يو) & Shahid4U (شاهد فور يو):** دعم السيرفرات المتعددة وفك الروابط المحمية وتجاوز صفحات الانتظار.
  - **Egydead & CimaClub & MyCima/Wecima:** البحث الشامل، جلب البوسترات عالية الدقة، القصة، التقييم، وتصنيف الجودات.
  - **HiTV & Asian Dramas (`HiTvRepository.kt`):** فك تشفير وتوفير المسلسلات الكورية، الصينية، والآسيوية بدقة عالية ومسارات ترجمة عربية.
  - **Anime (أنمي فور أب و أنمي ليك):** سحب الأنميات مع فصول الحلقات وتفاصيل المواسم.

### ج. محرك القنوات والبث المباشر (Live TV & Yacine TV Engine)
- **الملف المصدري:** `MadarFlutter/android/app/src/main/kotlin/com/madar/tv/data/ArabseedRepository.kt` & `update_yacine.py`
- **آلية العمل:**
  - فك تشفير خوارزمية **Yacine TV**: استخدام مفتاح AES-128 CBC لفك تشفير روابط البث الحي المشفرة الخاصة بالقنوات الرياضية المشفرة (beIN Sports, SSC) والقنوات العربية الترفيهية والإخبارية.
  - توفير مسارات تدفق HLS (`.m3u8`) مباشرة يتم تمريرها مع ترويسات مشفرة أصلية (`cleanUa` و `Referer` و `Origin`) لضمان عدم حظر البث أثناء المباريات.

### د. محرك ملخصات الأفلام (Movie Recaps Engine - YouTube Pure)
- **الملفات:** `MadarFlutter/lib/src/recaps.dart` و `PlayerActivity.kt`
- **آلية العمل:**
  - دعم **25 قناة ملخصات أفلام عربية رائدة ومخصصة** (مثل كوكلي، كوكلي 2، الخلاصة، فيلم في الخمسينة، إلخ).
  - قراءة الخلاصات الفورية عبر واجهة **Atom RSS** (`https://www.youtube.com/xml/feeds/videos.xml?channel_id=...`) مع fallback ذكي لصفحات الفيديو العامة عند الحاجة لتقليل استهلاك النت.
  - إمكانية التشغيل المباشر بزر واحد ("تشغيل") فوراً من الكارت دون الحاجة لدخول صفحة التفاصيل.

---

## 3. محرك تجاوز الإعلانات واستخراج التدفقات المباشرة (AdBypass & Direct Stream Extractor)

- **الملفات:**
  - `MadarFlutter/android/app/src/main/kotlin/com/madar/tv/data/AdBypassHelper.kt`
  - `MadarFlutter/android/app/src/main/kotlin/com/madar/tv/data/StreamExtractorHelper.kt`
- **القدرات التقنية:**
  1. **فك الجافاسكريبت المشفر (Packed JS Unpacker):** فك دوال `eval(function(p,a,c,k,e,d)...)` الشائعة في مستضيفات الفيديو لاستخراج روابط تدفق `file: "https://...master.m3u8"`.
  2. **دعم مستضيفات الفيديو العالمية:**
     - `DoodStream / Dood / D0000d`
     - `StreamWish / WishFast`
     - `VidBom / VidShare / VidFast`
     - `FileLions / Lion`
     - `UpStream`
     - `MixDrop`
     - `Mp4Upload`
     - `VidMoly`
  3. **استخراج الروابط المباشرة وتمريرها لمشغل ExoPlayer الأصلي** للاستفادة الكاملة من كوديك عتاد الجهاز (Hardware Decoders) والتحكم الكامل في الجودة بدون إعلانات منبثقة.

---

## 4. قنوات الربط والتكامل بين فلاتر وأندرويد (Flutter ↔ Native Bridge Architecture)

- **الملفات الأساسية:**
  - `MadarFlutter/android/app/src/main/kotlin/com/madar/madar_flutter/FlutterMediaBridge.kt`
  - `MadarFlutter/android/app/src/main/kotlin/com/madar/madar_flutter/MainActivity.kt`
  - `MadarFlutter/lib/services/native_scraper_service.dart`

### أ. قنوات الاتصال (MethodChannels):
1. **`madar/player`:** تشغيل الفيديوهات عبر إطلاق `PlayerActivity` بنية واضحة (`Intent`) وتمرير ترويسات HTTP كاملة وروابط الـ Backup.
2. **`catalog.*`:**
   - `catalog.search`: البحث في كافة المزودات موازياً واسترجاع النتائج مدمجة ومرتبة.
   - `catalog.details`: جلب القصة، النجوم، المخرج، التقييم، والخوادم.
   - `catalog.series_seasons`: جلب شجرة المواسم وحلقاتها بدقة.
   - `catalog.episodes`: استرجاع الحلقات مع روابط تشغيلها.
   - `catalog.servers`: فك الخوادم وجلب تدفقات الفيديو المباشرة.
   - `catalog.channels`: جلب قائمة القنوات الحية مع تصنيفاتها.
3. **`madar/native`:**
   - واجهة `NativeScraperService` التي تتيح لفلاتر استدعاء مستخرجات الـ Native مباشرة وتخزين الكاش محلياً لضمان سرعة الفتح.

---

## 5. مشغل الفيديو الموحد المتطور (`madar-player`)

- **المسار:** `MadarTV/madar-player/src/main/java/com/madar/player/PlayerActivity.kt`
- **التصميم:** واجهة سينمائية حديثة مبنية بنسبة 100% باستخدام **Jetpack Compose**.

### أ. البث الهجين (Hybrid Engine):
- **ExoPlayer Media3:** لتشغيل البث المباشر (HLS / m3u8) وملفات الفيديو (MP4 / MKV) بدقة ومرونة عالية.
- **Embedded Web Engine:** لصفحات السيرفرات التي تتطلب بيئة متصفح لتخطي الكابتشا والإعلانات مع حظر كل النوافذ المنبثقة وحظر إعادة التوجيه الإعلاني تلقائياً.

### ب. نظام التحكم في استهلاك الإنترنت وضبط الجودات (Smart Bandwidth Engine):
يدعم 5 أوضاع ذكية تناسب كافة سرعات النت:
1. **ضغط ذكي (HD موفّر):** يختار أعلى دقة HD موفّرة للبيانات بين 480p و 720p بدون تشويش.
2. **أقصى توفير للنت (Ultra Data Saver):** إجبار أقل معدل بت (Lowest Bitrate) للباقات الضعيفة والمحدودة.
3. **جودة متوازنة (Balanced):** توازن ذكي بين الوضوح وسرعة البث.
4. **أعلى جودة / جودة المصدر:** بث الفيديو بأقصى جودة وضوح دون أي قيود على سرعة النت.
5. **جودات يدوية (1080p, 720p, 480p, 360p):** يدوية بنقرة واحدة.
- **عداد حي للاستهلاك:** يحسب سرعة التدفق اللحظية (kbps / Mbps)، والاستهلاك التقديري بالجيجابايت في الساعة، والتكلفة التقديرية للباقة.

### ج. التوصيل بالشاشات والتلفزيون بضغطة زر واحدة (Universal Smart TV Connect):
- زر `📺 توصيل بالشاشة` يتيح 3 طرق ربط فورية:
  1. **العرض اللاسلكي / بث الشاشة (Wireless Display / Smart View / Miracast):** متوافق مع كافة شاشات سامسونج، إل جي، توشيبا، وشاشات الأندرويد.
  2. **بث Google Cast الرسمي:** للمستقبلات الداعمة لـ Chromecast.
  3. **تطبيقات البث الخارجية (مثل Web Video Caster):** تمرير رابط التدفق المباشر مع الترويسات لتشغيله على أي شاشة عبر الشبكة المنزلية.

---

## 6. تقنية تشغيل ملخصات يوتيوب النقية (YouTube Cinema-Pure Clean Video Engine)

تم حل كافة عيوب ومشاكل مشغلات الويب لليوتيوب جذرياً وتحويلها لتجربة ناتيف احترافية:

1. **إصلاح انهيار معالج الرسوميات (WebGPU / Renderer Crash - Code -1):**
   - إزالة حلقات `MutationObserver` المتداخلة التي كانت تفيض بافر السوكيت لنظام كروميوم.
   - الحفاظ على User-Agent القياسي لـ WebView لتفادي طلب واجهات WebGPU غير المتوافقة.
   - الامتثال الكامل لسياسة `TrustedHTML` و CSP الخاصة بيوتيوب.
2. **التدوير الطبيعي ودعم وضع البورتريه واللاندسكيب:**
   - استخدام `ActivityInfo.SCREEN_ORIENTATION_USER` لفتح المشغل بالوضع الرأسي الطبيعي عند إمساك الهاتف رأسياً، وبالوضع السينمائي عند قلبه أفقياً.
   - زر `⛶ تدوير` فوري في شريط التحكم للتبديل بلمسة واحدة بين الوضعين.
3. **التنظيف الجذري لكافة عناصر يوتيوب (Eradication of YouTube Web Chrome):**
   - تم اكتشاف وحذف حاويات التحكم الخاصة بيوتيوب موبايل:
     - `#player-controls` و `#player-control-overlay`
     - `.player-controls-content` و `.player-controls-background`
     - `creator-endscreen` و `expanding-endscreen-element`
     - `ytm-custom-control` و `ytm-watch-player-controls`
     - `ytm-endscreen-video-renderer` و `ytm-video-with-context-renderer`
     - `.ytFullscreenVideoRecommendationsRecommendation` و `#bottom-sheet-wrapper`
     - `.ytp-watermark` و `.ytp-chrome-top` و `.ytp-chrome-bottom` و `.ytp-pause-overlay`
   - **محرك التنظيف المزدوج (Dual Continuous Cleanup Loop):**
     - حقن CSS بقوة قصوى (`display: none !important; opacity: 0 !important; visibility: hidden !important; width: 0 !important; z-index: -9999 !important;`).
     - حلقة تنظيف ناتيف من كوتلن تعمل كل 250-400ms تحذف أي عنصر مقترح أو إعلان أو شاشة نهاية فور إنشائه من الـ DOM مباشرة (`el.parentNode.removeChild(el)`).
4. **عناصر تحكم ناتيف احترافية بالكامل (Golden Native Controls):**
   - شريط تقديم ذهبي ناعم مع عداد وقت دقيق (`00:00 / 00:00`).
   - أزرار تقديم وتأخير 10 ثوانٍ (`↺ 10s` و `↻ 10s`) مع دعم النقر المزدوج (Double-Tap Seek) على يمين ويسار الشاشة مع أنيميشن وتأثير بصري (`+10 ثواني` / `-10 ثواني`).
   - زر سرعات فوري بنقرة واحدة (1.0x, 1.25x, 1.5x, 2.0x).
   - زر تدوير شاشة مباشر (`⛶ تدوير`).
   - إخفاء تلقائي بعد 5 ثوانٍ لتتحول الشاشة إلى فيديو نقي 100% متمركز بنسبة 16:9 بدون أي شعار أو عناوين متداخلة.

---

## 7. آلية البناء والتحقق المباشر (Build & Verification Pipeline)

- تم بناء وتجميع تطبيق Flutter التجريبي بنجاح:
  - `flutter build apk --debug` -> `MadarFlutter/build/app/outputs/flutter-apk/app-debug.apk`
- تم بناء وتجميع حزمة المشغل الناتيف بنجاح:
  - `.\gradlew :madar-player:assembleDebug` -> `MadarTV/madar-player/build/outputs/apk/debug/madar-player-debug.apk`
- تم تثبيت الحزم واختبارها حياً على محاكي الأندرويد (`Artemis_Pixel_API_35` / `emulator-5554`):
  - تجربة تشغيل الأفلام والمسلسلات القديمة والحديثة.
  - تجربة القنوات الرياضية وقنوات البث الحي.
  - تجربة مشغل ملخصات الأفلام والتأكد من نقاء الشاشة التام وغياب أي شعارات أو اقتراحات دخيلة.

---
**تم إعداد هذا التوثيق ليكون مرجعاً تقنياً شاملاً لكافة تفاصيل التطبيق والمحركات.**
