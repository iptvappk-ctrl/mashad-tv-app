# 🏗️ 08 — الهيكل المنظم للمشروع (Clean Architecture Map)

> تاريخ إعادة التنظيم: نوفمبر 2026
> الحالة: ✅ منفّذ بالكامل

---

## 🗂️ الهيكل النهائي

```
c:\Users\hassa\Desktop\all project\iptv app\
│
├── README.md                        ← نقطة البداية - اقرأها أولاً
├── .git/                            ← git repository
├── .gitignore
│
├── 01_ZAQZOUKA_CLIENT/              ← 🟢 تطبيق زقزوقة Flutter الرئيسي
│   └── MadarFlutter/
│       ├── lib/
│       │   ├── main.dart            ← نقطة الدخول الرئيسية
│       │   ├── src/
│       │   │   ├── app_shared.dart  ← مشاركة البيانات والحالة
│       │   │   ├── catalog.dart     ← واجهة الكتالوج
│       │   │   ├── details.dart     ← تفاصيل الفيلم/المسلسل
│       │   │   ├── episodes.dart    ← الحلقات
│       │   │   ├── recaps.dart      ← الملخصات
│       │   │   ├── sports.dart      ← الرياضة
│       │   │   ├── telemetry.dart   ← إرسال الإحصائيات
│       │   │   └── firebase_quota_guard.dart ← 🛡️ حماية الكوتا (جديد)
│       │   └── services/
│       │       └── native_scraper_service.dart
│       ├── android/                 ← Native Android bridge
│       └── pubspec.yaml
│
├── 02_ZAG_PLAYER/                   ← 🟡 مشغل ZAG الأندرويد
│   └── MadarTV/
│       └── madar-player/
│           ├── src/main/
│           │   ├── kotlin/com/madar/player/
│           │   │   ├── PlayerActivity.kt    ← الشاشة الرئيسية للمشغل
│           │   │   ├── MainActivity.kt      ← نقطة الدخول
│           │   │   └── FlutterMediaBridge.kt← جسر Flutter↔Player
│           │   └── res/
│           └── build.gradle.kts
│
├── 03_MASTER_CONTROL/               ← 🔴 تطبيق التحكم الكامل (للمالك فقط)
│   └── ZaqzoukaMasterControl/
│       ← تطبيق Flutter
│       ← أزرار: تشغيل / تجميد / مسح كامل
│       ← يتصل بـ Firebase + Vercel API
│
├── 04_ADMIN_DASHBOARD/              ← 🟣 لوحة الإدارة
│   ├── ZaqzoukaAdmin/               ← واجهة Next.js
│   ├── dashboard/                   ← واجهة قديمة
│   └── dashboard-api/               ← Vercel Serverless API
│       ├── api/
│       │   ├── admin/               ← إجراءات الإدارة
│       │   └── ...
│       ├── database.rules.json      ← قواعد Firebase الأمنية
│       └── service-account.json     ← 🔑 مفتاح Firebase (محمي)
│
├── 05_SCRAPERS_ENGINE/              ← 🔵 محركات الجلب والسكرابينج
│   ├── SCRAPERS_ENGINE/             ← المحرك الرئيسي
│   ├── scrapers/                    ← سكرابرز متفرقة
│   ├── providers_analysis/          ← تحليل مزودي المحتوى
│   └── ReverseEngineering/          ← هندسة عكسية
│
├── 06_WEB_AND_DISTRIBUTION/         ← 🌐 الويب والتوزيع
│   ├── web/                         ← الموقع الرسمي
│   ├── social/                      ← أصول التواصل الاجتماعي
│   └── zaqzouka-vercel-hub/         ← مركز التوزيع (Vercel)
│       ├── api/
│       │   ├── status.js            ← 🛡️ Quota Guard API (جديد)
│       │   ├── system_control.js    ← تحكم النظام
│       │   └── version.js           ← الإصدارات
│       ├── public/
│       │   ├── status/
│       │   │   └── system.json      ← 🛡️ Static fallback (جديد)
│       │   ├── updates/android/
│       │   │   └── latest.json      ← معلومات آخر إصدار
│       │   └── downloads/           ← ملفات APK للتنزيل
│       └── vercel.json              ← إعدادات CDN (محدّث)
│
├── 07_DOCUMENTATION/                ← 📚 كل الوثائق
│   ├── ZAQZOUKA_MASTER_ARCHIVE/     ← أرشيف التوثيق الرئيسي
│   │   ├── 00_START_HERE_INDEX.md
│   │   ├── 01_ACCOUNTS_AND_CREDENTIALS.md
│   │   ├── 02_PROJECT_MAP_AND_ARCHITECTURE.md
│   │   ├── 03_SCRAPING_ENGINE_COMPLETE_GUIDE.md
│   │   ├── 04_MASTER_CONTROL_AND_SECURITY.md
│   │   ├── 05_PLAYER_AND_CLIENT_APPS.md
│   │   ├── 06_VERCEL_HUB_AND_OTA_UPDATES.md
│   │   ├── 07_FIREBASE_QUOTA_PROTECTION.md  ← (جديد)
│   │   └── 08_CLEAN_ARCHITECTURE.md         ← (هذا الملف)
│   ├── DOCUMENTATION/               ← وثائق متفرقة
│   ├── Zaqzouka_Security_Docs/      ← وثائق الأمان
│   ├── google_play_assets/          ← أصول Google Play
│   ├── دليل_المواقع_والسيرفرات/    ← 25 ملف تحليل مواقع
│   ├── documents for/               ← وثائق متنوعة
│   ├── README_AR.md                 ← README العربي القديم
│   └── مشروع_مشهد_التوثيق_الشامل.md
│
├── 08_RELEASE_BINARIES/             ← 📦 ملفات الإصدارات
│   ├── RELEASE_BINARIES/            ← APK + AAB الجاهزة
│   │   ├── zaqzouka-app.apk
│   │   ├── zag-player.apk
│   │   └── zaqzouka-master-control.apk
│   ├── zaqzouka-1.0.67-72.apk       ← إصدار قديم
│   ├── madar-player.apk
│   └── masshaad.apk
│
├── 09_LEGACY_ARCHIVES/              ← 🗄️ الأرشيف القديم
│   └── LEGACY_ARCHIVES/             ← 15+ APK/AAB قديمة
│
└── 10_SCRATCH_AND_TOOLS/            ← 🔧 أدوات ومؤقتات
    ├── screenshots/                 ← 150+ صورة اختبار
    ├── python_scripts/              ← سكربتات Python
    ├── java_files/                  ← ملفات Java/Class
    ├── html_css/                    ← ملفات HTML/CSS
    ├── logs_and_outputs/            ← سجلات وملفات نصية
    ├── misc/                        ← متفرقات
    ├── scratch/                     ← scratch الأصلي
    └── __pycache__/
```

---

## 🔄 ما تغيّر في إعادة التنظيم

| الوضع القديم | الوضع الجديد |
|---|---|
| `MadarFlutter/` في الـ Root | `01_ZAQZOUKA_CLIENT/MadarFlutter/` |
| `MadarTV/` في الـ Root | `02_ZAG_PLAYER/MadarTV/` |
| `ZaqzoukaMasterControl/` في الـ Root | `03_MASTER_CONTROL/ZaqzoukaMasterControl/` |
| `dashboard/`, `dashboard-api/` في الـ Root | `04_ADMIN_DASHBOARD/` |
| `SCRAPERS_ENGINE/`, `scrapers/` في الـ Root | `05_SCRAPERS_ENGINE/` |
| `web/`, `zaqzouka-vercel-hub/` في الـ Root | `06_WEB_AND_DISTRIBUTION/` |
| `ZAQZOUKA_MASTER_ARCHIVE/` في الـ Root | `07_DOCUMENTATION/` |
| `RELEASE_BINARIES/` في الـ Root | `08_RELEASE_BINARIES/` |
| `LEGACY_ARCHIVES/` في الـ Root | `09_LEGACY_ARCHIVES/` |
| ~200 ملف فضفاض في الـ Root | `10_SCRATCH_AND_TOOLS/` |

---

## ⚠️ تنبيهات مهمة للمطورين

> [!IMPORTANT]
> بعد التنظيم، مسارات البناء تغيّرت. استخدم دائماً المسار الجديد:
> ```bash
> # ❌ قديم
> cd "MadarFlutter"
> # ✅ جديد
> cd "01_ZAQZOUKA_CLIENT/MadarFlutter"
> ```

> [!WARNING]
> `service-account.json` في `04_ADMIN_DASHBOARD/dashboard-api/` محمي ولا يُرفع على GitHub أبداً.

> [!NOTE]
> git history محفوظ كاملاً - فقط الملفات انتقلت بالـ `shutil.move`.

---

*تاريخ الإنشاء: نوفمبر 2026*
