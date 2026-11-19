# 🎬 HiTV APK — توثيق شامل كامل
## نظام استخراج المحتوى وتكامله في تطبيق مشهد

---

## 📦 معلومات الـ APK

| البند | القيمة |
|-------|--------|
| اسم الملف | `hitv-4-6-6.apk` |
| الحجم | 82.6 MB |
| Package | `tech.hitv.movie` |
| الإصدار | 4.6.6 |
| Firebase Project | `hitv-351903` |
| Google Client ID | `670862826386-9otd9...` |
| تاريخ البناء | Nov 20, 2024 |

---

## 🏗️ هيكل الـ APK

```
hitv-4-6-6.apk
├── classes.dex          (55 KB - stub loader فقط)
├── assets/
│   ├── nedata.db        (22 MB - DEX مشفر - الكود الحقيقي)
│   ├── dic              (4 KB  - ELF مشفر بـ XOR 'dic')
│   ├── nedig.properties (335 B - إعدادات مشفرة)
│   ├── audience_network.dex (Facebook Ads)
│   └── ...
├── lib/
│   ├── arm64-v8a/
│   │   ├── libap9.so    (2.3 MB - الـ Packer/Loader الرئيسي)
│   │   ├── libcsydk.so  (954 KB - Encryption SDK مشفر)
│   │   ├── libdec.so    (174 KB - JNI Decryption)
│   │   ├── libijkffmpeg.so (12 MB - FFmpeg player)
│   │   ├── libijkplayer.so (IjkPlayer)
│   │   └── libijksdl.so
│   └── armeabi-v7a/
└── res/ + resources.arsc
```

---

## 🔐 نظام التشفير

### الـ Packer (libap9.so)
- يستخدم **DexProtector/VMP** (Virtual Machine Protection)
- يقرأ `nedata.db` المشفر من assets
- يفك تشفيره في الذاكرة ويحمله كـ DEX
- **لا يمكن فك تشفير nedata.db بدون تشغيل التطبيق**

### libdec.so - JNI Functions
```
Java_cibntest_baoyun_com_decjni_JniUtil_dec   - فك تشفير الاستجابات
Java_cibntest_baoyun_com_decjni_JniUtil_sign  - توقيع الطلبات
```
- يستخدم: AES, SHA256, SHA1, MD5, Base64, XOR, Blowfish

### assets/dic
- ملف ELF (32-bit x86) مشفر بـ XOR key = `'dic'`
- لفك تشفيره: `decoded[i] = raw[i] XOR 'dic'[i%3]`
- محتواه: stub library صغير (helper)

---

## 📱 الـ Activities الرئيسية

```
tech.hitv.movie.ui.activity.HimaHomeActivity       - الصفحة الرئيسية
tech.hitv.movie.ui.activity.HimaDramaActivity      - المسلسلات
tech.hitv.movie.ui.activity.HimaSeriesActivity     - السيريز
tech.hitv.movie.ui.activity.SeriesRankActivity     - الترتيب
tech.hitv.movie.ui.activity.SearchActivity         - البحث
tech.hitv.movie.ui.activity.StarDetailActivity     - النجوم
tech.hitv.movie.media.VerticalScreenVideoActivity  - مشغل الفيديو
tech.hitv.movie.login.activity.*                   - تسجيل الدخول
tech.hitv.movie.googlepay.*                        - المدفوعات/VIP
tv.xingxing.huasheng.BridgeBrowserActivity        - Live TV Bridge
```

---

## 🌐 الـ API (من التحليل والبحث)

### Base URL المحتملة
```
https://api.gohitv.com/s1/w/series/api/
https://api.baoyun.com/
https://cibntv.net/api/
```

### Headers المطلوبة
```
platform: android (أو pc/ios)
did: <device_id>  (16 char random string)
User-Agent: okhttp/4.11.0
Accept: application/json
Authorization: Bearer <token> (للمحتوى المدفوع)
```

### Endpoints المتوقعة
```
GET /home              - الصفحة الرئيسية
GET /category/list     - قائمة الفئات
GET /series/list       - قائمة المسلسلات
GET /series/{id}       - تفاصيل مسلسل
GET /series/{id}/episodes - حلقات المسلسل
GET /episode/{id}/stream  - رابط البث
GET /movie/list        - قائمة الأفلام
GET /search?q=...      - البحث
GET /live/list         - القنوات المباشرة (عبر xingxing)
```

---

## 🎬 المحتوى المتوقع

### أنواع المحتوى
- **Korean Dramas** - مسلسلات كورية
- **Chinese Dramas** - مسلسلات صينية
- **Thai Dramas** - مسلسلات تايلاندية
- **Japanese Anime** - أنيمي ياباني
- **Movies** - أفلام آسيوية
- **Live TV** (عبر tv.xingxing.huasheng)

### بنية البيانات المتوقعة
```json
{
  "id": "12345",
  "title": "My Lovely Liar",
  "title_cn": "可爱的谎言",
  "poster": "https://cdn.gohitv.com/poster/12345.jpg",
  "year": 2023,
  "country": "KR",
  "genre": ["Romance", "Fantasy"],
  "episodes": 16,
  "rating": 8.5,
  "description": "...",
  "language": "Korean",
  "subtitles": ["en", "ar", "zh"],
  "is_vip": false,
  "stream_url": "https://cdn.gohitv.com/stream/12345/ep1.m3u8"
}
```

---

## 🚫 إزالة الإعلانات

### الـ Ad SDKs الموجودة في الـ APK
1. **Facebook Audience Network** - `audience_network.dex`
2. **Mintegral** - `com.mbridge.msdk`
3. **Pangle** (ByteDance) - `com.bytedance.sdk`
4. **AppLovin** - `libapplovin-native-crash-reporter.so`
5. **Baidu Ads** - `com.baidu.mobads`
6. **IronSource** - `com.ironsource`
7. **Google Admob** - `com.google.android.gms.ads`
8. **Domob** - `com.domob`
9. **Bigo Ads** - `sg.bigo.ads`

### طريقة إزالة الإعلانات في التكامل
في `HiTvRepository.kt` - نطلب المحتوى مباشر من API بدون Ads SDK:
```kotlin
// نتجاهل أي response field متعلق بالإعلانات
// نفلتر نتائج type == "ad" أو "banner"
// نضبط ad_free = true في الـ request headers
```

---

## 🔧 خطة التكامل في مشهد

### الملفات المطلوب إنشاؤها
```
MadarTV/app/src/main/java/com/madar/tv/
├── data/
│   ├── HiTvModels.kt         - Data classes
│   └── HiTvRepository.kt     - API calls + parsing
└── ui/
    ├── HiTvScreen.kt         - Main UI (categories + content)
    ├── HiTvDetailScreen.kt   - تفاصيل مسلسل/فيلم
    └── HiTvPlayerScreen.kt   - مشغل الفيديو
```

### تعديل MadarApp.kt
إضافة Tab جديد: 🎬 كوري/آسيوي

---

## 📊 مقارنة YacineTV vs HiTV

| البند | YacineTV | HiTV |
|-------|----------|------|
| النوع | IPTV (بث مباشر) | VOD (أفلام/مسلسلات) |
| التشفير | XOR + Base64 | DexProtector + JNI |
| الـ API | مكشوفة بالكامل | محمية في nedata.db |
| المحتوى | قنوات رياضية + ماتشات | مسلسلات + أفلام آسيوية |
| الصعوبة | ⭐⭐ | ⭐⭐⭐⭐ |

---

## 🛠️ Scripts المتوفرة

| Script | الوظيفة |
|--------|---------|
| `hitv_analyze.py` | تحليل libap9.so و libdec.so |
| `hitv_deep_analyze.py` | تحليل عميق + XOR decode |
| `hitv_dic.py` | فك تشفير assets/dic |
| `hitv_api_test.py` | اختبار API endpoints |
| `hitv_baoyun_test.py` | اختبار Baoyun/CIBN APIs |

---

## ⚠️ ملاحظات مهمة

> [!IMPORTANT]
> الكود الحقيقي للتطبيق محمي في `nedata.db` بتشفير قوي (DexProtector).
> لاستخراجه يحتاج لـ dynamic analysis (تشغيل الـ APK + Frida hook).

> [!NOTE]
> طالما API endpoints مش متاحة بشكل واضح، التكامل هيعتمد على:
> 1. أي endpoints نلاقيها شغالة
> 2. أو استخدام WebView مع ad-blocking للعرض

> [!TIP]
> يمكن استخدام `mitmproxy` مع جهاز Android مـ rooted لالتقاط الـ traffic الفعلي

Last Updated: 2026-11-12
