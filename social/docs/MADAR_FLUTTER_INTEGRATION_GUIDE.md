# 📱 دليل تكامل نظام زقزوقة الكامل
## MADAR_FLUTTER_INTEGRATION_GUIDE.md

> **قاعدة أساسية**: لا تُعدّل أي ملف موجود في `MadarFlutter/`. كل الملفات الجديدة جاهزة للـ **copy-paste** فقط.

---

## 🗺️ خريطة المشروع

```
social/
├── kotlin/                          ← 4 ملفات Kotlin جاهزة للدمج
│   ├── QualityTrackExtractor.kt    ← استخراج جودات M3U8
│   ├── SmartMediaLinker.kt         ← ربط الحلقات بالمسلسلات
│   ├── RamadanEngine.kt            ← قاعدة بيانات رمضان 2026
│   ├── AutoCrawlerWorker.kt        ← كراولر تلقائي كل 15 دقيقة
│   └── SuperProviderBridge.kt      ← جسر بين Kotlin والـ API
│
├── dart/                            ← 4 ملفات Dart جاهزة للدمج
│   ├── quality_selector.dart       ← Widget اختيار الجودة
│   ├── server_merger.dart          ← دمج وترتيب السيرفرات
│   ├── ramadan_section.dart        ← قسم مسلسلات رمضان
│   └── smart_linker_client.dart    ← Dart client للـ API
│
├── src/                             ← محركات TypeScript (Node.js)
│   ├── lib/extractors/             ← استخراج روابط 15+ سيرفر
│   ├── lib/quality/                ← معالجة M3U8
│   ├── lib/ramadan/                ← قاعدة بيانات رمضان
│   ├── lib/crawler/                ← كراولر 6 مواقع
│   ├── lib/linker/                 ← ربط المسلسلات والأفلام
│   └── api/cinema/                 ← API endpoints (Next.js)
│
└── docs/                            ← التوثيق الكامل
    ├── SITES_DATABASE_100.md       ← 110+ موقع
    ├── SYSTEM_ARCHITECTURE.md      ← المعمارية الكاملة
    └── QUALITY_AND_RAMADAN.md      ← أنظمة الجودة ورمضان
```

---

## ⚡ خطوات التكامل السريع (5 خطوات)

### الخطوة 1 — نسخ ملفات Kotlin

```bash
# المسار الهدف:
# MadarFlutter/android/app/src/main/kotlin/com/madar/tv/social/

mkdir -p "MadarFlutter/android/app/src/main/kotlin/com/madar/tv/social"

# انسخ الملفات:
copy social\kotlin\QualityTrackExtractor.kt  MadarFlutter\android\app\src\main\kotlin\com\madar\tv\social\
copy social\kotlin\SmartMediaLinker.kt       MadarFlutter\android\app\src\main\kotlin\com\madar\tv\social\
copy social\kotlin\RamadanEngine.kt          MadarFlutter\android\app\src\main\kotlin\com\madar\tv\social\
copy social\kotlin\AutoCrawlerWorker.kt      MadarFlutter\android\app\src\main\kotlin\com\madar\tv\social\
copy social\kotlin\SuperProviderBridge.kt    MadarFlutter\android\app\src\main\kotlin\com\madar\tv\social\
```

### الخطوة 2 — نسخ ملفات Dart

```bash
# انسخ الملفات:
copy social\dart\quality_selector.dart   MadarFlutter\lib\src\
copy social\dart\server_merger.dart      MadarFlutter\lib\src\
copy social\dart\ramadan_section.dart    MadarFlutter\lib\src\
copy social\dart\smart_linker_client.dart MadarFlutter\lib\src\
```

### الخطوة 3 — إضافة dependencies

في `MadarFlutter/android/app/build.gradle`:
```groovy
dependencies {
    // موجود بالفعل في التطبيق:
    // implementation 'com.squareup.okhttp3:okhttp:4.x.x'
    // implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.x.x'
    
    // أضف هذا إذا مش موجود:
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
}
```

في `MadarFlutter/pubspec.yaml`:
```yaml
dependencies:
  shared_preferences: ^2.2.0  # للـ quality_selector.dart
```

### الخطوة 4 — تشغيل AutoCrawlerWorker

في `MadarFlutter/android/app/src/main/kotlin/com/madar/madar_flutter/MainActivity.kt`:
```kotlin
// أضف هذا داخل onCreate():
import com.madar.tv.social.AutoCrawlerWorker

// في onCreate():
AutoCrawlerWorker.schedulePeriodicWork(applicationContext)

// تلقي الإشعارات بالحلقات الجديدة:
AutoCrawlerWorker.onNewRelease = { release ->
    val title = release.optString("title")
    val url = release.optString("pageUrl")
    // أضف للـ catalog أو أعمل notification
}
```

### الخطوة 5 — استخدام Quality Selector في episodes.dart

في `_openServer()` بعد ما تاخد الـ stream URL:
```dart
// بعد import في main.dart:
// part 'src/quality_selector.dart';
// part 'src/server_merger.dart';

// في episodes.dart _openServer():
Future<void> _openServer(Map<String, dynamic> server) async {
  // ... الكود الموجود ...
  
  // إضافة: اعرض quality selector لو في tracks
  if (url.endsWith('.m3u8')) {
    // اجلب tracks من Kotlin عبر NativeBridge
    final tracksJson = await _native.invokeMethod<String>('media.qualityTracks', {
      'url': url,
      'referer': referer,
    });
    if (tracksJson != null && mounted) {
      final json = jsonDecode(tracksJson) as Map<String, dynamic>;
      final tracks = parseQualityTracksFromJson(json);
      if (tracks.length > 1) {
        final chosen = await showQualitySelector(context, tracks: tracks, currentUrl: url);
        if (chosen != null) url = chosen.streamUrl;
      }
    }
  }
  
  // باقي الكود الموجود...
  await _launchPlayer(context, url, _fullTitle, ...);
}
```

---

## 🔧 ربط MethodChannel جديد في Kotlin

في `FlutterMediaBridge.kt`، أضف هذا الـ case **داخل** `onMethodCall`:

```kotlin
// في الـ when block الموجود:
"media.qualityTracks" -> {
    val url = call.argument<String>("url") ?: ""
    val referer = call.argument<String>("referer") ?: ""
    scope.launch {
        try {
            val pkg = QualityTrackExtractor.buildQualityPackage(url, referer)
            result.success(pkg.toString())
        } catch (e: Exception) {
            result.error("QUALITY_ERROR", e.message, null)
        }
    }
}

"ramadan.getSeries" -> {
    val category = call.argument<String>("category") ?: "all"
    val engine = RamadanEngine()
    val series = if (category == "all") engine.getAll() else engine.getByCategory(category)
    result.success(series.toString())
}

"linker.getSeriesTree" -> {
    val title = call.argument<String>("title") ?: ""
    val linker = SmartMediaLinker()
    result.success(linker.getSeriesTree(title).toString())
}
```

---

## 🌙 إضافة قسم رمضان في الشاشة الرئيسية

في `catalog.dart`، في أي مكان مناسب داخل الـ ListView:

```dart
// أضف في الـ imports:
// part 'src/ramadan_section.dart';

// في الـ body:
RamadanSectionWidget(
  onSeriesTap: (series) {
    // افتح صفحة تفاصيل المسلسل
    // أو ابحث عنه في الـ catalog الموجود
    final query = series.title;
    // _search(query); // أو أي منطق موجود
  },
),
```

---

## 🔄 دورة حياة النظام في التطبيق

```
المستخدم يفتح التطبيق
        │
        ▼
AutoCrawlerWorker يشتغل في الخلفية (كل 15 دقيقة)
        │
        ├── يفحص 6 مواقع: arabseed, wecima, faselhd, egybest, akwam, cima4u
        │
        ├── لقى حلقة جديدة؟ → SmartMediaLinker.registerItem()
        │                            │
        │                            ├── ربط بالمسلسل
        │                            ├── ربط بالموسم
        │                            └── ربط بالأجزاء
        │
        └── إشعار للمستخدم
        
المستخدم يختار حلقة
        │
        ▼
episodes.dart → _loadServers()
        │
        ├── NativeBridge.media.servers(pageUrl)
        │       │
        │       └── MultiProviderRepository (موجود أصلاً - 7 مصادر)
        │
        ├── ServerMerger.addAll(servers1 + servers2)
        │       └── ترتيب حسب السرعة + شيل الـ duplicates
        │
        └── عرض السيرفرات
        
المستخدم يضغط سيرفر
        │
        ▼
_openServer()
        │
        ├── هل هو M3U8؟ → QualityTrackExtractor.buildQualityPackage()
        │                       │
        │                       └── showQualitySelector() ← يختار الجودة
        │
        └── _launchPlayer(url, backupSources: merger.buildFallbackChain())
                │
                └── Player: لو الرابط فشل → ينتقل للتالي تلقائياً (< 2 ثانية)
```

---

## 🌐 API Endpoints المتاحة

| Endpoint | Method | الوظيفة |
|----------|--------|---------|
| `/api/cinema/superresolve?serverUrl=...` | GET | حل embed URL → CDN link |
| `/api/cinema/quality?streamUrl=...` | GET | استخراج جودات M3U8 |
| `/api/cinema/ramadan?category=egyptian` | GET | مسلسلات رمضان |
| `/api/cinema/franchise?title=...` | GET | شجرة أجزاء فيلم |
| `/api/cinema/providers` | GET | قائمة المزودين |
| `/api/proxy/stream?url=...&ref=...` | GET | stream بدون إعلانات |
| `/api/crawler` | GET | حالة الكراولر |
| `/api/crawler/trigger` | POST | تشغيل كراولر يدوي |

---

## 🏎️ نصائح الأداء

| الأولوية | الإجراء |
|----------|---------|
| ⚡ السرعة | `SuperProviderBridge` يعمل parallel لكل المصادر بـ coroutines |
| 🔄 Fallback | `backupSourcesJson` يحتوي 9+ سيرفرات — الـ player ينتقل تلقائياً |
| 💾 Cache | `CatalogDiskCache` موجود أصلاً في التطبيق — استخدمه |
| 🚫 بدون إعلانات | `/api/proxy/stream` يشيل الإعلانات من M3U8 |
| 📡 Ramadan | البيانات محلية (RamadanEngine.DATABASE) → لا يحتاج إنترنت |

---

## ✅ قائمة التحقق

- [ ] نسخ ملفات Kotlin إلى `com/madar/tv/social/`
- [ ] نسخ ملفات Dart إلى `lib/src/`
- [ ] إضافة `work-runtime-ktx` في build.gradle
- [ ] إضافة `shared_preferences` في pubspec.yaml
- [ ] تشغيل `AutoCrawlerWorker.schedulePeriodicWork()` في MainActivity
- [ ] إضافة `media.qualityTracks` case في FlutterMediaBridge
- [ ] إضافة `RamadanSectionWidget` في catalog.dart
- [ ] استدعاء `showQualitySelector()` في `_openServer()`
- [ ] تشغيل `flutter pub get`
- [ ] بناء APK: `flutter build apk --release`
