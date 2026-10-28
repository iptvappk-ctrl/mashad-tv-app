# 📺 منظومة مَـشْـهَـدْ TV (Mashad IPTV Ecosystem)
## التوثيق الهندسي الشامل والكامل للمشروع (Full Engineering Documentation)

مرحباً بك في التوثيق الشامل لمنظومة **مَـشْـهَـدْ TV** (المعروفة سابقاً بـ MadarTV). هذا المجلد يحتوي على شرح معمّق وتفصيلي لكافة أركان المشروع، البرمجيات، خوارزميات فك التشفير، محركات الـ Scraping، نظام الحماية السداسي، وطرق التشغيل والبناء.

---

## 🗺️ خريطة وثائق المشروع (Table of Documents)

| الملف | المحتوى والعنوان | الوصف |
| :--- | :--- | :--- |
| [01_ARABSEED_SCRAPING_ENGINE.md](./01_ARABSEED_SCRAPING_ENGINE.md) | **محرك سحب عرب سيد وتوليد البث المباشر** | شرح تفصيلي لكيفية سحب التصنيفات، الأفلام، المسلسلات، الحلقات، الأجزاء، واستخراج روابط `m3u8` من سيرفرات Govid و Mohixstream. |
| [02_YACINE_TV_DECRYPTION_ENGINE.md](./02_YACINE_TV_DECRYPTION_ENGINE.md) | **تكامل Yacine TV وفك استجابات الـ API** | تكامل القنوات والبيانات وطريقة قراءة الاستجابات؛ أي قيمة مضمنة في التطبيق تُعامل كبيانات مكشوفة للعميل وليست سراً. |
| [03_SECURITY_OBFUSCATION_V6.md](./03_SECURITY_OBFUSCATION_V6.md) | **حماية تطبيق Android وحدود حماية APK** | إعداد R8 وتوقيع Release، التمييز بين إخفاء النصوص والتشفير، والأسرار التي يجب نقلها إلى الخادم. |
| [04_FOOTBALL_AND_LIVESCORES.md](./04_FOOTBALL_AND_LIVESCORES.md) | **محرك المباريات والنتائج الحية (ESPN Integration)** | جلب مواعيد المباريات، النتائج الحية، الترتيب، وربطها التلقائي بالقنوات الناقلة. |
| [05_ANDROID_APP_AND_UI.md](./05_ANDROID_APP_AND_UI.md) | **معمارية تطبيق الأندرويد والواجهات** | Jetpack Compose، نظام Navigation Stack، ومشغل الفيديو الهجين (ExoPlayer + Web Sandbox). |
| [06_WEB_DASHBOARD_AND_DEPLOYMENT.md](./06_WEB_DASHBOARD_AND_DEPLOYMENT.md) | **منصة الويب، سيرفر التحليلات، وطريقة البناء** | تشغيل منصة الويب (Next.js 14)، سيرفر الرصد والتحليلات (Express/Node.js)، وطريقة بناء ملف الـ APK. |
| [07_MULTI_PROVIDER_STREAMING_AGGREGATOR.md](./07_MULTI_PROVIDER_STREAMING_AGGREGATOR.md) | **معمارية مجمّع البث الموحد (16 موقعاً عربياً)** | توثيق هندسي شامل لـ 16 موقعاً سينمائياً (أكوام، فاصل، وي سيما، قصة عشق، لودي نت، برستيج...) مع خوارزميات البحث المتوازي وفك السيرفرات. |

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
        │ - R8 Release Shrinking  │                       │ - Tailwind CSS UI       │
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
2. **حماية Android الواقعية:** يستخدم بناء Release أداة R8 لتصغير الكود وإعادة تسمية الرموز. إخفاء النصوص الثابتة داخل APK قابل للعكس ولا يخفي الأسرار عن محلل يملك التطبيق؛ المفاتيح الحساسة يجب أن تبقى على الخادم. راجع [حدود حماية APK](./03_SECURITY_OBFUSCATION_V6.md).
3. **No External Paid Dependencies:** المنظومة تعمل بالكامل بدون الحاجة لاشتراكات مدفوعة أو Firebase، بل تعتمد على خوادم محلية وسحب ذكي ومباشر.
