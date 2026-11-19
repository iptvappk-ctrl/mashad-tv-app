# 🎬 ZAQZOUKA TV — مشروع زقزوقة TV

> المتحكم الوحيد والمالك: **المؤسس**  
> سر المشغّل: `zaqzouka_secret_2026` | رقم PIN: `999888`  
> مشروع Firebase: `madartv` | رابط RTDB: `https://madartv-default-rtdb.firebaseio.com`

---

## 📁 هيكل المشروع

```
iptv app/
├── 01_ZAQZOUKA_CLIENT/        ← تطبيق زقزوقة Flutter الرئيسي (MadarFlutter)
├── 02_ZAG_PLAYER/             ← مشغل ZAG الأندرويد (MadarTV/madar-player)
├── 03_MASTER_CONTROL/         ← تطبيق التحكم الكامل (ZaqzoukaMasterControl)
├── 04_ADMIN_DASHBOARD/        ← لوحة الإدارة + API (ZaqzoukaAdmin + dashboard-api)
├── 05_SCRAPERS_ENGINE/        ← محركات السكرابينج والتحليل
├── 06_WEB_AND_DISTRIBUTION/   ← الموقع + توزيع التحديثات (Vercel Hub)
├── 07_DOCUMENTATION/          ← كل الوثائق والأدلة
├── 08_RELEASE_BINARIES/       ← ملفات APK و AAB الجاهزة للنشر
├── 09_LEGACY_ARCHIVES/        ← أرشيف النسخ القديمة
└── 10_SCRATCH_AND_TOOLS/      ← أدوات مؤقتة وملفات العمل
```

---

## 🚀 البدء السريع

### بناء التطبيق الرئيسي
```bash
cd 01_ZAQZOUKA_CLIENT/MadarFlutter
flutter build apk --release
```

### بناء ZAG Player
```bash
cd 02_ZAG_PLAYER/MadarTV
./gradlew :madar-player:assembleRelease
```

### بناء تطبيق التحكم
```bash
cd 03_MASTER_CONTROL/ZaqzoukaMasterControl
flutter build apk --release
```

### نشر Vercel Hub
```bash
cd 06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub
vercel --prod
```

---

## 🛡️ أوامر التحكم الكاملة

| الأمر | الوصف |
|-------|-------|
| `freeze` | تجميد التطبيق على كل الأجهزة |
| `restore` | تشغيل التطبيق مرة أخرى |
| `wipe` | مسح كل شيء نهائياً - إغلاق كامل |

```bash
# تجميد
curl -X POST https://zaqzouka-hub.vercel.app/api/system_control \
  -H "x-admin-secret: zaqzouka_secret_2026" \
  -d '{"action":"freeze","message":"الصيانة جارية"}'

# تشغيل
curl -X POST https://zaqzouka-hub.vercel.app/api/system_control \
  -H "x-admin-secret: zaqzouka_secret_2026" \
  -d '{"action":"restore"}'

# مسح كامل (لا رجعة فيه!)
curl -X POST https://zaqzouka-hub.vercel.app/api/system_control \
  -H "x-admin-secret: zaqzouka_secret_2026" \
  -d '{"action":"wipe"}'
```

---

## 🏗️ المكونات

### 1. تطبيق زقزوقة (01_ZAQZOUKA_CLIENT)
- Flutter - Android
- Package: `com.madar.madar_flutter`
- يقرأ الكتالوج من سكرابر native
- يفتح الفيديو عبر ZAG Player

### 2. ZAG Player (02_ZAG_PLAYER)
- Kotlin + ExoPlayer + YouTube Custom Tab
- Package: `com.madar.player`
- يدعم HLS, MP4, YouTube embeds
- مع تبديل جودة + ترجمة + ملء الشاشة

### 3. تطبيق التحكم (03_MASTER_CONTROL)
- Flutter - يعمل على هاتف المالك فقط
- أزرار: **تشغيل / تجميد / مسح كامل**
- متصل بـ Firebase + Vercel API

### 4. لوحة الإدارة (04_ADMIN_DASHBOARD)
- Next.js + Vercel Serverless
- إحصائيات + جلسات + تحكم

### 5. محرك السكرابينج (05_SCRAPERS_ENGINE)
- Python scrapers لمواقع المحتوى
- تحليل روابط HLS/MP4

### 6. التوزيع (06_WEB_AND_DISTRIBUTION)
- Vercel Hub: توزيع APK + OTA updates
- Status API مع CDN cache (60 ثانية)

---

## 🔥 Firebase Quota Guard

نظام 5 طبقات لحماية الكوتا المجانية:

```
1M مستخدم
   ↓
Layer 1: Vercel CDN (s-maxage=60) ← 99.99% من الطلبات تُخدم هنا
   ↓ (cache miss)
Layer 2: In-memory cache (30 ثانية)
   ↓ (stale)
Layer 3: Local SharedPreferences (5 دقائق)
   ↓ (expired)
Layer 4: Firebase RTDB direct (مرة كل 5 دقائق فقط!)
   ↓ (error)
Layer 5: Hardcoded "active" fallback (لا يوقف التطبيق أبدًا)
```

**النتيجة:** Firebase لا يرى سوى طلب واحد كل 60 ثانية بدل مليون طلب!

---

## 📋 آخر إصدارات APK

| التطبيق | الإصدار | SHA256 |
|---------|---------|--------|
| زقزوقة App | 1.0.0 | `bad9e5b7...` |
| ZAG Player | 2.0.0 | `efbc2254...` |

الملفات في: `08_RELEASE_BINARIES/`

---

## 📚 التوثيق

كل الوثائق في: [`07_DOCUMENTATION/ZAQZOUKA_MASTER_ARCHIVE/`](07_DOCUMENTATION/ZAQZOUKA_MASTER_ARCHIVE/)

| الملف | المحتوى |
|-------|---------|
| `00_MASTER_INDEX.md` | الفهرس الرئيسي |
| `01_SYSTEM_OVERVIEW.md` | نظرة عامة |
| `02_FIREBASE_SCHEMA.md` | مخطط قاعدة البيانات |
| `03_SECURITY_AND_CONTROL.md` | الأمان والتحكم |
| `04_SCRAPERS_GUIDE.md` | دليل السكرابينج |
| `05_OTA_UPDATE_SYSTEM.md` | نظام التحديثات |
| `06_DEPLOYMENT_GUIDE.md` | دليل النشر |
| `07_FIREBASE_QUOTA_PROTECTION.md` | حماية الكوتا |
| `08_CLEAN_ARCHITECTURE.md` | الهيكل المنظم |

---

*آخر تحديث: نوفمبر 2026*
