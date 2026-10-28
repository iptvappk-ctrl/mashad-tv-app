# 📺 منظومة مَـشْـهَـدْ TV (Mashad IPTV Ecosystem)
## التوثيق الهندسي الشامل والكامل للمشروع (Full Engineering Documentation)

مرحباً بك في توثيق منظومة **مَـشْـهَـدْ TV**. يشرح هذا المجلد مكونات المشروع، حدود تكاملات الطرف الثالث، الحماية الواقعية لملفات Android، وطريقة إعداد التحليلات والبناء.

---

## 🗺️ خريطة وثائق المشروع (Table of Documents)

| الملف | المحتوى والعنوان | الوصف |
| :--- | :--- | :--- |
| [01_ARABSEED_SCRAPING_ENGINE.md](./01_ARABSEED_SCRAPING_ENGINE.md) | **محرك سحب عرب سيد وتوليد البث المباشر** | شرح تفصيلي لكيفية سحب التصنيفات، الأفلام، المسلسلات، الحلقات، الأجزاء، واستخراج روابط `m3u8` من سيرفرات Govid و Mohixstream. |
| [02_YACINE_TV_DECRYPTION_ENGINE.md](./02_YACINE_TV_DECRYPTION_ENGINE.md) | **تكامل Yacine TV وفك استجابات الـ API** | تكامل القنوات والبيانات وطريقة قراءة الاستجابات؛ أي قيمة مضمنة في التطبيق تُعامل كبيانات مكشوفة للعميل وليست سراً. |
| [03_SECURITY_OBFUSCATION_V6.md](./03_SECURITY_OBFUSCATION_V6.md) | **حماية تطبيق Android وحدود حماية APK** | إعداد R8 وتوقيع Release، التمييز بين إخفاء النصوص والتشفير، والأسرار التي يجب نقلها إلى الخادم. |
| [04_FOOTBALL_AND_LIVESCORES.md](./04_FOOTBALL_AND_LIVESCORES.md) | **محرك المباريات والنتائج الحية (ESPN Integration)** | جلب مواعيد المباريات، النتائج الحية، الترتيب، وربطها التلقائي بالقنوات الناقلة. |
| [05_ANDROID_APP_AND_UI.md](./05_ANDROID_APP_AND_UI.md) | **معمارية تطبيق الأندرويد والواجهات** | Jetpack Compose، نظام Navigation Stack، ومشغل الفيديو الهجين (ExoPlayer + Web Sandbox). |
| [06_WEB_DASHBOARD_AND_DEPLOYMENT.md](./06_WEB_DASHBOARD_AND_DEPLOYMENT.md) | **منصة الويب ولوحة تحليلات GA4** | تشغيل الموقع ولوحة Node.js المتصلة بـ Google Analytics Data API وإعداد اعتماد القراءة والبناء. |
| [07_MULTI_PROVIDER_STREAMING_AGGREGATOR.md](./07_MULTI_PROVIDER_STREAMING_AGGREGATOR.md) | **معمارية مجمّع البث الموحد (16 موقعاً عربياً)** | توثيق هندسي شامل لـ 16 موقعاً سينمائياً (أكوام، فاصل، وي سيما، قصة عشق، لودي نت، برستيج...) مع خوارزميات البحث المتوازي وفك السيرفرات. |
| [08_FIREBASE_ANALYTICS_AND_DASHBOARD.md](./08_FIREBASE_ANALYTICS_AND_DASHBOARD.md) | **إعداد Firebase Analytics ولوحة الإدارة** | أحداث Android والويب، إعداد التطبيقات، ربط GA4 Data API، وقيود الإحصاءات الحقيقية. |

---

## 🏛️ نظرة عامة على معمارية المنظومة (System Architecture)

المنظومة تحتوي تطبيق Android وموقع ويب ولوحة محلية لقراءة تقارير GA4 المجمّعة بعد تهيئة الوصول:

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
                     │       Firebase Analytics (usage events)         │
                     └────────────────────────┬────────────────────────┘
                                              │
                                              ▼
                                 ┌─────────────────────────┐
                                 │ Google Analytics Data API│
                                 │  read-only local dashboard│
                                 │ - Aggregated reports      │
                                 │ - Realtime users (30 min) │
                                 │ - Event/content counts    │
                                 └─────────────────────────┘
```

---

## 🌟 المبادئ الهندسية الصارمة للمشروع
1. **بيانات صادقة:** لوحة التحليلات تعرض بيانات GA4 الحقيقية أو حالة إعداد واضحة، ولا تستخدم بذوراً أو أرقاماً شكلية.
2. **حماية Android الواقعية:** يستخدم بناء Release أداة R8 لتصغير الكود وإعادة تسمية الرموز. إخفاء النصوص الثابتة داخل APK قابل للعكس ولا يخفي الأسرار عن محلل يملك التطبيق؛ المفاتيح الحساسة يجب أن تبقى على الخادم. راجع [حدود حماية APK](./03_SECURITY_OBFUSCATION_V6.md).
3. **تحليلات أقل تدخلاً:** لا يُرسل التطبيق نص البحث أو رابط المشاهدة أو معرف جهاز مخصص. راجع [إعداد Firebase والتحليلات](./08_FIREBASE_ANALYTICS_AND_DASHBOARD.md).
