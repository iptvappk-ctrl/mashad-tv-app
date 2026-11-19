# 🎯 10 — دليل البداية السريعة (Quick Start Guide)

> **للمالك:** كل حاجة محتاج تعرفها عشان تشغّل وتتحكم في المنظومة

---

## 🏠 الوضع الحالي للمشروع

```
📁 c:\Users\hassa\Desktop\all project\iptv app\
├── 01_ZAQZOUKA_CLIENT\      ← تطبيق زقزوقة Flutter
├── 02_ZAG_PLAYER\           ← مشغل ZAG Android
├── 03_MASTER_CONTROL\       ← تطبيق التحكم (للمالك فقط)
├── 04_ADMIN_DASHBOARD\      ← لوحة الإدارة + API
├── 05_SCRAPERS_ENGINE\      ← محرك السكرابينج
├── 06_WEB_AND_DISTRIBUTION\ ← الويب + Vercel Hub
├── 07_DOCUMENTATION\        ← كل الوثائق
├── 08_RELEASE_BINARIES\     ← APK جاهزة للتوزيع
├── 09_LEGACY_ARCHIVES\      ← أرشيف قديم
└── 10_SCRATCH_AND_TOOLS\    ← أدوات مؤقتة
```

---

## ⚡ أسرع طريقة للتحكم في النظام

### 🔴 إيقاف كل الأجهزة فوراً
```bash
curl -X POST https://zaqzouka-hub.vercel.app/api/system_control \
  -H "Content-Type: application/json" \
  -H "x-admin-secret: zaqzouka_secret_2026" \
  -d "{\"action\":\"freeze\",\"message\":\"الخدمة متوقفة مؤقتاً\"}"
```

### 🟢 تشغيل كل الأجهزة مرة أخرى
```bash
curl -X POST https://zaqzouka-hub.vercel.app/api/system_control \
  -H "Content-Type: application/json" \
  -H "x-admin-secret: zaqzouka_secret_2026" \
  -d "{\"action\":\"restore\"}"
```

### 💥 مسح كل شيء نهائياً (لا رجعة فيه!)
```bash
curl -X POST https://zaqzouka-hub.vercel.app/api/system_control \
  -H "Content-Type: application/json" \
  -H "x-admin-secret: zaqzouka_secret_2026" \
  -d "{\"action\":\"wipe\"}"
```

> **أو:** استخدم تطبيق **ZaqzoukaMasterControl** من هاتفك بضغطة زر!

---

## 📱 بناء وتوزيع التحديثات

### 1. بناء تطبيق زقزوقة
```bash
cd "c:\Users\hassa\Desktop\all project\iptv app\01_ZAQZOUKA_CLIENT\MadarFlutter"
flutter build apk --release
# الناتج: build/app/outputs/flutter-apk/app-release.apk
```

### 2. بناء ZAG Player
```bash
cd "c:\Users\hassa\Desktop\all project\iptv app\02_ZAG_PLAYER\MadarTV"
.\gradlew :madar-player:assembleRelease
# الناتج: madar-player/build/outputs/apk/release/madar-player-release.apk
```

### 3. رفع التحديث على Vercel

```bash
# 1. انسخ APK الجديدة
copy app-release.apk "..\..\08_RELEASE_BINARIES\RELEASE_BINARIES\zaqzouka-app.apk"

# 2. حدّث latest.json
# افتح: 06_WEB_AND_DISTRIBUTION\zaqzouka-vercel-hub\public\updates\android\latest.json
# غيّر: versionCode, versionName, sha256

# 3. انشر على Vercel
cd "06_WEB_AND_DISTRIBUTION\zaqzouka-vercel-hub"
vercel --prod
```

---

## 🔥 Firebase - معلومات الاتصال

| المعلومة | القيمة |
|---------|--------|
| Project ID | `madartv` |
| RTDB URL | `https://madartv-default-rtdb.firebaseio.com` |
| API Key | `AIzaSyCrda4aOYIPnD_uBJ3Rpk60nZzBSnrL9fc` |
| Project Number | `409052366723` |

### قراءة حالة النظام الآن:
```bash
curl https://zaqzouka-hub.vercel.app/api/status
# أو مباشرة من Firebase:
curl https://madartv-default-rtdb.firebaseio.com/system_control.json
```

---

## 🕷️ تشغيل السكرابر

```bash
cd "c:\Users\hassa\Desktop\all project\iptv app\05_SCRAPERS_ENGINE\SCRAPERS_ENGINE"

# تثبيت المتطلبات (مرة واحدة)
pip install -r requirements.txt

# جلب أفلام arabseed
python run_scraper.py --provider arabseed --type movies

# بحث
python run_scraper.py --provider egybest --search "الفيل الأزرق"
```

---

## 🔑 الأسرار والمفاتيح

| المفتاح | القيمة |
|---------|--------|
| Admin Secret | `zaqzouka_secret_2026` |
| Master PIN | `999888` |
| API Key (Vercel) | `ZQ-OWNER-2026-SECRET-14MOON` |

---

## 📊 مراقبة الكوتا

**للتأكد أن Quota Guard شغّال:**
```bash
# يجب أن يرجع Cache-Control: s-maxage=60
curl -I https://zaqzouka-hub.vercel.app/api/status
```

**للتأكد من حالة Firebase:**
```bash
# Firebase Console → Usage tab
# يجب أن يكون simultaneous connections < 10 حتى مع ملايين المستخدمين
```

---

## 🆘 حل المشكلات الشائعة

| المشكلة | الحل |
|---------|------|
| التطبيق لا يستجيب لـ freeze | تأكد أن Vercel deployed وأن `x-admin-secret` صح |
| Firebase quota نفدت | شغّل Quota Guard في Flutter (firebase_quota_guard.dart) |
| الموقع غيّر domain | حدّث `base_url` في `providers/[site].py` |
| APK لا يتحدث | حدّث `latest.json` في Vercel Hub |
| لوحة الإدارة لا تعمل | تأكد من `service-account.json` في `04_ADMIN_DASHBOARD/dashboard-api/` |

---

*آخر تحديث: نوفمبر 2026*
