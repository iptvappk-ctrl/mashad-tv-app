# 📱 التوثيق الشامل لتطبيق الهاتف والشاشات الذكية (Zaqzouka TV - MadarFlutter)
**الإصدار:** 14 Moon (Release v1.0.67-72)  
**تاريخ التوثيق:** 16 نوفمبر 2026  
**المجلد الرئيسي للتطبيق:** `MadarFlutter/`  
**حزمة التطبيق (Package Name):** `com.zaqzouka.tv` / `com.madar.tv`  

---

## 📑 الفهرس
1. [نظرة عامة على التطبيق وفلسفة التصميم](#1-نظرة-عامة-على-التطبيق-وفلسفة-التصميم)
2. [الهيكل البنائي للمجلدات والملفات (Folder & Architecture Structure)](#2-الهيكل-البنائي-للمجلدات-والملفات)
3. [نظام إدارة الحالة والتشغيل (State Management & App Lifecycle)](#3-نظام-إدارة-الحالة-والتشغيل)
4. [محرك جلب وفهرسة المحتوى (Catalog Service & Scrapers Engine)](#4-محرك-جلب-وفهرسة-المحتوى)
5. [خوارزمية الفلترة الصارمة ومنع التداخل (Strict Bilingual Token Matching)](#5-خوارزمية-الفلترة-الصارمة-ومنع-التداخل)
6. [محرك التتبع اللحظي وتحديد الموقع الدقيق (Live Telemetry & Exact Geolocation)](#6-محرك-التتبع-اللحظي-وتحديد-الموقع-الدقيق)
7. [جسر الربط مع مشغل الفيديو الأصلي (NativeBridge & MethodChannel)](#7-جسر-الربط-مع-مشغل-الفيديو-الأصلي)
8. [دعم الشاشات وأجهزة التحكم عن بعد (Android TV & D-Pad Remote Navigation)](#8-دعم-الشاشات-وأجهزة-التحكم-عن-بعد)
9. [إعدادات البناء وملفات التكوين (Build Configurations & AndroidManifest)](#9-إعدادات-البناء-وملفات-التكوين)

---

## 1. نظرة عامة على التطبيق وفلسفة التصميم
تطبيق **Zaqzouka TV** هو تطبيق ترفيهي هجين مبني بإطار عمل **Flutter (Dart 3.x)** مخصص للعمل بكفاءة فائقة على كل من:
- **الهواتف الذكية والأجهزة اللوحية (Android Phones & Tablets):** واجهة لمس سلسة وسريعة متجاوبة مع كافة أبعاد الشاشات ومعدلات التحديث حتى 120Hz.
- **الشاشات الذكية وأجهزة التي في بوكس (Android TV, Google TV, FireTV Box):** متوافق كلياً مع معايير Android Leanback والتنقل الكامل باستخدام أزرار الريموت كنترول (D-Pad Navigation).

### الميزات الجوهرية للنسخة 14 Moon:
1. **الاعتماد 100% على البيانات الحقيقية:** لا توجد أي بيانات وهمية أو صور ثابتة Dummy Data. كافة الأفلام، المسلسلات، الأنمي، والمباريات يتم سحبها لحظياً وبشكل حي.
2. **منع اختلاط المحتوى:** تطبيق خوارزمية `Strict Bilingual Token Matching` لضمان عدم ظهور أي فيلم أجنبي داخل تفاصيل مسلسل عربي، أو عرض حلقات غير مطابقة نهائياً.
3. **التتبع الجغرافي فائق الدقة (Exact Geolocation):** يحدد التطبيق المدينة والمحافظة بدقة (مثل: `القاهرة، C، مصر (EG)`) ومزود خدمة الإنترنت (ISP) ونوع الجهاز.
4. **تكامل سوق البيانات والإعلانات (Monetization Ready):** تصنيف سلوك المشاهد إلى شرائح IAB تلقائياً لدعم إعلانات الفيديو VAST/VMAP وسماسرة البيانات DMP.
5. **تبديل السيرفرات التلقائي المقاوم للحظر:** الانتقال السلس بين سيرفرات المشاهدة عند تعطل أي سيرفر دون إشعار المستخدم بالانقطاع.

---

## 2. الهيكل البنائي للمجلدات والملفات
```
MadarFlutter/
├── android/                          # كود الأندرويد الأصلي وإعدادات Gradle
│   ├── app/
│   │   ├── build.gradle              # إعدادات البناء، الـ SDK (min 21, target 34)، وتوقيع التطبيق
│   │   └── src/main/
│   │       ├── AndroidManifest.xml   # أذونات الشبكة، وضع Leanback، والتوافق مع TV
│   │       └── kotlin/com/zaqzouka/tv/
│   │           └── MainActivity.kt   # نقطة استقبال MethodChannel للتحكم بالمشغل الأصلي
├── assets/                           # الخطوط، الأيقونات، والشعارات الرسمية
├── lib/                              # الكود المصدري بلغة Dart
│   ├── main.dart                     # نقطة الانطلاق الرئيسية، الثيم، وتهيئة الخدمات
│   ├── screens/                      # شاشات واجهة المستخدم
│   │   ├── home_screen.dart          # الشاشة الرئيسية والأقسام السريعة والبانر التفاعلي
│   │   ├── movies_screen.dart        # استعراض وتصنيف الأفلام (عربي، أجنبي، تركي، هندي)
│   │   ├── series_screen.dart        # استعراض المسلسلات والمواسم
│   │   ├── anime_screen.dart         # استعراض مسلسلات وأفلام الأنمي المترجمة والمدبلجة
│   │   ├── matches_screen.dart       # جدول المباريات الحية ونتائج اليوم وروابط البث
│   │   ├── details_screen.dart       # صفحة تفاصيل العمل، طاقم التمثيل، والسيرفرات
│   │   ├── episode_picker.dart       # منتقي الحلقات والمواسم الذكي
│   │   ├── search_screen.dart        # محرك البحث اللحظي مع ميزة Debounce ومطابقة الكلمات
│   │   ├── favorites_screen.dart     # المفضلة المحفوظة محلياً (SharedPreferences / Hive)
│   │   └── settings_screen.dart      # إعدادات التطبيق، الجودة الافتراضية، ومسح الكاش
│   └── src/                          # الخدمات الخلفية والمحركات المشتركة
│       ├── catalog_service.dart      # محرك جلب وفهرسة المحتوى والتواصل مع السيرفرات
│       ├── native_bridge.dart        # وسيط استدعاء مشغل ExoPlayer الأصلي عبر الأندرويد
│       ├── telemetry.dart            # محرك القياس عن بعد، تحديد الموقع، ونبضات الجلسات الحية
│       ├── title_matcher.dart        # خوارزمية الفلترة الصارمة ومنع تداخل العناوين
│       └── models/                   # نماذج البيانات (MediaItem, Server, Episode, Match)
├── pubspec.yaml                      # حزم ومكتبات Flutter وإعدادات النسخة
└── build/                            # نواتج التجميع وحزم APK
```

---

## 3. نظام إدارة الحالة والتشغيل (State Management & App Lifecycle)
يستخدم التطبيق مزيجاً هندسياً يجمع بين خفة الوزن والسرعة الفائقة دون استهلاك للذاكرة على الشاشات الاقتصادية ذات الـ 1GB RAM:
1. **ValueNotifier & ChangeNotifier:** لإدارة حالة البحث اللحظي، وتحديث قوائم السيرفرات دون إعادة بناء كامل الشجرة Widget Tree.
2. **WidgetsBindingObserver:** مراقبة حالة التطبيق (`AppLifecycleState`):
   - عند الانتقال إلى `paused` أو `detached`: يتم إيقاف نبضات التتبع مؤقتاً أو إنهاء الجلسة عبر `TelemetryService.endSession()`.
   - عند العودة إلى `resumed`: يتم استئناف فحص التحديثات ونبضات التتبع لضمان بقاء المستخدم كجلسة نشطة 🟢 في لوحة الأدمن.

```dart
// نموذج إدارة دورة حياة التطبيق في lib/main.dart
class _ZaqzoukaAppState extends State<ZaqzoukaApp> with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    TelemetryService.init(); // بدء جلسة التتبع اللحظي وتحديد الموقع
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused || state == AppLifecycleState.detached) {
      TelemetryService.pauseHeartbeat();
    } else if (state == AppLifecycleState.resumed) {
      TelemetryService.resumeHeartbeat();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    TelemetryService.endSession();
    super.dispose();
  }
}
```

---

## 4. محرك جلب وفهرسة المحتوى (Catalog Service & Scrapers Engine)
الملف: `MadarFlutter/lib/src/catalog_service.dart`

يعتمد التطبيق على جلب البيانات الحية مباشرة من واجهات السيرفر السحابي أو السحب المباشر من مزودات المحتوى الرئيسية (`Arabseed / m.myseed.tv`) مع دعم الكاش المحلي الذكي:

### آلية عمل جلب الكتالوج:
1. **طلب الكتالوج المجدول:** يرسل التطبيق طلب `HTTP GET` إلى نقطة الوصول السحابية أو المزود الأصلي حاملاً الـ User-Agent المخصص.
2. **استخراج البيانات وتنسيقها:**
   - العنوان الأصلي والعنوان المترجم.
   - رابط الملصق عالي الجودة (Poster URL) مع Fallback تلقائي للصور المكسورة.
   - تصنيف العمل: (أكشن، دراما، كوميديا، أنمي، رعب، رومانسي).
   - التقييم، سنة الإنتاج، والجودة (1080p Web-DL / 720p).
3. **الكاش المحلي المقاوم لضعف الشبكة (TTL Memory Cache):** يتم حفظ النتائج لمدة 15 دقيقة في الذاكرة لتوفير استهلاك الباقة وتأمين انتقال فوري وفائق السرعة بين الأقسام.

---

## 5. خوارزمية الفلترة الصارمة ومنع التداخل (Strict Bilingual Token Matching)
الملف: `MadarFlutter/lib/src/title_matcher.dart`

تم ابتكار هذه الخوارزمية خصيصاً لحل المشكلة الشائعة في تطبيقات البث (وهي ظهور أفلام أجنبية داخل صفحات مسلسلات عربية تحمل أسماء مشابهة أو ظهور محتوى غير ذي صلة):

### مبدأ عمل الخوارزمية:
1. **تطبيع النصوص ثنائي اللغة (Bilingual Normalization):**
   - إزالة علامات التشكيل، المد، وأشكال الألف المختلفة (`أ`, `إ`, `آ` -> `ا`).
   - إزالة الكلمات الزائدة مثل: (فيلم، مسلسل، كامل، مدبلج، مترجم، بجودة، HD، 1080p).
   - إزالة الحركات الإنجليزية والرموز الخاصة (`-`, `_`, `:`, `.`).
2. **تقسيم النص إلى رموز مميزة (Tokenization):**
   - استخراج الرموز الأساسية (Keywords) للعنوان الأصلي وللعنوان المقترح.
3. **المطابقة الصارمة للعنوان العربي والإنجليزي:**
   - إذا كان المحتوى يحتوي اسماً إنجليزياً وعربياً، يجب أن يحقق التطابق نسبة لا تقل عن 85% في الرموز الجوهرية.
4. **كشف ومنع الصفحات الوهمية ونتائج الـ Soft-404:**
   - فحص استجابة صفحات الحلقات للتأكد من احتوائها الفعلي على مشغلات فيديو وسيرفرات وليس صفحة فارغة أو إشعار خطأ في المزود.

---

## 6. محرك التتبع اللحظي وتحديد الموقع الدقيق (Live Telemetry & Exact Geolocation)
الملف: `MadarFlutter/lib/src/telemetry.dart`

يقوم هذا المحرك بنقل بيانات الاستخدام في الوقت الفعلي إلى السيرفر السحابي المركزي ولوحة تحكم الأدمن، دون جمع أي بيانات شخصية (PII) لتوافق تام مع سياسات الخصوصية:

### آلية تحديد الموقع الدقيق (Exact Geolocation):
يقوم التطبيق بالاستعلام من نقطة IP موثوقة ومطابقة الموقع مع مصفوفة المدن والمحافظات العربية:
- **تحويل الرموز الإنجليزية إلى أسماء عربية دقيقة:**
  - `Cairo` -> `القاهرة`
  - `Giza` -> `الجيزة`
  - `Alexandria` -> `الإسكندرية`
  - `Tanta` / `Gharbia` -> `طنطا، الغربية`
  - `Mansoura` / `Dakahlia` -> `المنصورة، الدقهلية`
  - `Riyadh` -> `الرياض`
  - `Dubai` -> `دبي`
- **حفظ وعرض الموقع في السجل:** يظهر في لوحة الأدمن بالصيغة: `القاهرة، C، مصر (EG)`.
- **تسجيل مزود خدمة الإنترنت (ISP):** مثل `Telecom Egypt (WE)`, `Vodafone Egypt`, `Orange`, `Etisalat`.

### نبضات القلب الدورية (Keep-Alive Heartbeat):
- يرسل التطبيق إشارة نبضية كل **25 ثانية** إلى `/api/telemetry/session/ping`.
- يتم إرسال اسم العمل المشاهد حالياً، الدقيقة الحالية، ومدة الجلسة الكلية.
- إذا لم يرسل التطبيق إشارة لمدة تتجاوز 45 ثانية، تقوم لوحة التحكم تلقائياً بتحديد الجلسة كـ `منتهية ⚪` لمنع تراكم أرقام وهمية.

### تصنيف الجمهور الذكي (Audience Segmentation):
يقوم التطبيق بحساب تصنيف IAB بناءً على سلوك المشاهدة الحالي:
- مشاهدة أفلام ومسلسلات -> `🍿 جمهور الترفيه والمنوعات`
- مشاهدة مباريات حية -> `⚽ جمهور الرياضة والمباريات`
- مشاهدة أنمي -> `🎌 جمهور الأنمي والشباب`
- مشاهدة أعمال أطفال -> `👨‍👩‍👧‍👦 جمهور العائلة والأطفال`

---

## 7. جسر الربط مع مشغل الفيديو الأصلي (NativeBridge & MethodChannel)
الملف: `MadarFlutter/lib/src/native_bridge.dart`  
الكود المقابل في الأندرويد: `MadarFlutter/android/app/src/main/kotlin/com/zaqzouka/tv/MainActivity.kt`

لضمان أعلى أداء ممكن أثناء فك تشفير الفيديو (Hardware Acceleration) وعدم حدوث أي تهنيج أو تسريب للذاكرة، يتم تشغيل البث عبر مشغل **ExoPlayer** الأصلي المكتوب بلغة Kotlin:

### بروتوكول الاتصال عبر MethodChannel:
- القناة: `com.zaqzouka.tv/player`
- الدوال المستدعاة:
  - `playVideo(url, title, headers, serverName, isLive)`
  - `pauseVideo()`
  - `seekTo(positionMs)`
  - `switchServer(newUrl, newHeaders)`
- التمرير التلقائي لترويسات الحماية (Headers): تمرير `Referer` و `User-Agent` المناسبين لكل سيرفر (مثل سيرفرات Voe أو Vidmoly) لضمان عدم رفض الرابط بكود 403 Forbidden.

---

## 8. دعم الشاشات وأجهزة التحكم عن بعد (Android TV & D-Pad Remote Navigation)
تم بناء الواجهة بنظام التركيز التلقائي (FocusNode Traversal) المعتمد في أنظمة التلفزيون:
1. **تظليل وتكبير العنصر النشط (Focus Elevation):**
   - عند الوقوف على بوستر الفيلم بالريموت، يتم تكبيره بنسبة 1.08x مع إضافة إطار مضيء ملون (Glow Border).
2. **الاستجابة الفورية لأزرار الريموت:**
   - أزرار الاتجاهات (Up, Down, Left, Right) للتنقل السلس في الشبكة.
   - زر التحديد (D-Pad Center / OK) لفتح التفاصيل أو بدء التشغيل.
   - زر الرجوع (Back Key) للعودة المنطقية خطوة واحدة للخلف دون الخروج المفاجئ.
   - أزرار التحكم بالوسائط (Play, Pause, Fast-Forward 10s, Rewind 10s).
3. **وضع ملء الشاشة التلقائي (Immersive Sticky Mode):**
   - إخفاء أشرطة النظام (System Bars) والتحكم الكامل بمساحة الشاشة.

---

## 9. إعدادات البناء وملفات التكوين (Build Configurations & AndroidManifest)
### الملف: `android/app/build.gradle`
```groovy
android {
    compileSdkVersion 34
    defaultConfig {
        applicationId "com.zaqzouka.tv"
        minSdkVersion 21
        targetSdkVersion 34
        versionCode 72
        versionName "1.0.67-72"
        multiDexEnabled true
    }
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### الملف: `android/app/src/main/AndroidManifest.xml`
- تمكين دعم الـ TV: `<uses-feature android:name="android.software.leanback" android:required="false" />`
- دعم شاشات التلفزيون التي لا تدعم اللمس: `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`
- إذن تشغيل روابط HTTP غير المشفرة عند الحاجة: `android:usesCleartextTraffic="true"`
- أذونات الوصول للإنترنت وحالة الشبكة:
  - `<uses-permission android:name="android.permission.INTERNET" />`
  - `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
  - `<uses-permission android:name="android.permission.WAKE_LOCK" />`
