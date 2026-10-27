# 📺 منظومة مَـشْـهَـدْ TV (Mashad IPTV Ecosystem)
## التوثيق الهندسي الشامل والكامل للمشروع (Full Engineering Documentation)

مرحباً بك في التوثيق الشامل لمنظومة **مَـشْـهَـدْ TV** (المعروفة سابقاً بـ MadarTV). هذا المجلد يحتوي على شرح معمّق وتفصيلي لكافة أركان المشروع، البرمجيات، خوارزميات فك التشفير، محركات الـ Scraping، نظام الحماية السداسي، وطرق التشغيل والبناء.

---

## 🗺️ خريطة وثائق المشروع (Table of Documents)

| الملف | المحتوى والعنوان | الوصف |
| :--- | :--- | :--- |
| [01_ARABSEED_SCRAPING_ENGINE.md](./01_ARABSEED_SCRAPING_ENGINE.md) | **محرك سحب عرب سيد وتوليد البث المباشر** | شرح تفصيلي لكيفية سحب التصنيفات، الأفلام، المسلسلات، الحلقات، الأجزاء، واستخراج روابط `m3u8` من سيرفرات Govid و Mohixstream. |
| [02_YACINE_TV_DECRYPTION_ENGINE.md](./02_YACINE_TV_DECRYPTION_ENGINE.md) | **محرك ياسين تيفي وفك تشفير الـ API** | الخوارزمية السرية لفك تشفير بيانات قنوات beIN Sports و SSC من سيرفرات ياسين، المفاتيح السرية، ونظام إخفاء اللوجو. |
| [03_SECURITY_OBFUSCATION_V6.md](./03_SECURITY_OBFUSCATION_V6.md) | **نظام الأمان والتشفير السداسي (v6 Engine)** | خط أنابيب التشفير الرياضي ذو الـ 6 طبقات، مكافحة الهندسة العكسية، واختبار فحص 21/21 DEX بنسبة 100%. |
| [04_FOOTBALL_AND_LIVESCORES.md](./04_FOOTBALL_AND_LIVESCORES.md) | **محرك المباريات والنتائج الحية (ESPN Integration)** | جلب مواعيد المباريات، النتائج الحية، الترتيب، وربطها التلقائي بالقنوات الناقلة. |
| [05_ANDROID_APP_AND_UI.md](./05_ANDROID_APP_AND_UI.md) | **معمارية تطبيق الأندرويد والواجهات** | Jetpack Compose، نظام Navigation Stack، ومشغل الفيديو الهجين (ExoPlayer + Web Sandbox). |
| [06_WEB_DASHBOARD_AND_DEPLOYMENT.md](./06_WEB_DASHBOARD_AND_DEPLOYMENT.md) | **منصة الويب، سيرفر التحليلات، وطريقة البناء** | تشغيل منصة الويب (Next.js 14)، سيرفر الرصد والتحليلات (Express/Node.js)، وطريقة بناء ملف الـ APK. |

---

## 🏛️ نظرة عامة على معمارية المنظومة (System Architecture)

المنظومة مصممة لتعمل كبيئة متكاملة تخدم المستخدمين على الأندرويد والويب مع مركز تحليلات فوري:

```
                                  ┌────────────────────────┐
                                  │   Upstream Sources     │
                                  │  - Arabseed (Cinema)   │
                                  │  - Yacine TV (Live)    │
                                  │  - ESPN API (Matches)  │
                                  └───────────┬────────────┘
                                              │
                     ┌────────────────────────┴────────────────────────┐
                     ▼                                                 ▼
        ┌─────────────────────────┐                       ┌─────────────────────────┐
        │   Android Mobile App    │                       │     Mashad Web App      │
        │       (MadarTV)         │                       │    (Next.js 14 / SSR)   │
        │ - Kotlin Jetpack Compose│                       │ - Cinema & Live TV      │
        │ - ExoPlayer (HLS Native)│                       │ - VideoJS Player        │
        │ - 6-Layer Obfuscation   │                       │ - Tailwind CSS Dark UI  │
        └────────────┬────────────┘                       └────────────┬────────────┘
                     │                                                 │
                     │          Telemetry & Heartbeats (Events)        │
                     └────────────────────────┬────────────────────────┘
                                              │
                                              ▼
                                 ┌─────────────────────────┐
                                 │ Mashad Analytics Server │
                                 │   (Node.js / Express)   │
                                 │ - Live Active Users     │
                                 │ - Realtime Streams Log  │
                                 │ - Content Popularity    │
                                 └─────────────────────────┘
```

---

## 🌟 المبادئ الهندسية الصارمة للمشروع
1. **Zero-Ad Experience:** يتم تطهير جميع الروابط من الإعلانات والنوافذ المنبثقة، وتوجيه الروابط المباشرة (`.m3u8` و `.mp4`) إلى المشغل الأصلي (Native ExoPlayer).
2. **Military-Grade DEX Obfuscation:** لا يتم تخزين أي رابط موقع أو سيرفر أو مفتاح API كنص صريح (Plain Text) في كود الأندرويد، بل يتم تشفيرها بخوارزمية خاصة مخصصة لمنع برامج الهندسة العكسية مثل Jadx و APK Easy Tool من استخراج الروابط.
3. **No External Paid Dependencies:** المنظومة تعمل بالكامل بدون الحاجة لاشتراكات مدفوعة أو Firebase، بل تعتمد على خوادم محلية وسحب ذكي ومباشر.
