# 🔐 دليل الحسابات والمفاتيح السرية (Accounts & Credentials)

> ⚠️ **سري للغاية - للمالك والمطور فقط:** يحتوي هذا الملف على جميع الحسابات، المفاتيح، والكلمات السرية المستخدمة في إدارة وتشغيل ومراقبة منصة "زقزوقة تيفي".

---

## 1. حساب ومنصة فايربيس (Firebase Console)

تعتمد المنظومة على منصة **Google Firebase** لإدارة التحديثات اللحظية، ترخيص الهواتف، والتحكم عن بُعد في توقف وتشغيل التطبيقات ومسح الكاش.

| البيان | القيمة / الإعداد |
| :--- | :--- |
| **اسم المشروع (Project ID)** | `madartv` |
| **رقم المشروع (Project Number)** | `409052366723` |
| **مفتاح واجهة برمجة التطبيقات (Web API Key)** | `AIzaSyCrda4aOYIPnD_uBJ3Rpk60nZzBSnrL9fc` |
| **رابط قاعدة البيانات اللحظية (RTDB URL)** | `https://madartv-default-rtdb.firebaseio.com` |
| **مستودع التخزين السحابي (Storage Bucket)** | `madartv.firebasestorage.app` |
| **معرف تطبيق الأندرويد الأساسي (Flutter App ID)** | `1:409052366723:android:bc452737c581a8c63bd8b6` |
| **معرف تطبيق المشغل (Player App ID)** | `1:409052366723:android:b3f343cd7702d71c3bd8b6` |
| **اسم حزمة تطبيق العميل (Client Package Name)** | `com.madar.madar_flutter` |
| **اسم حزمة المشغل (Player Package Name)** | `com.mader.player` (أو `com.madar.tv`) |
| **ملف الضبط الأساسي** | `MadarFlutter/android/app/google-services.json` |

### المتغيرات البيئية الخاصة بـ Firebase Admin SDK (في السيرفرات و Vercel):
- `FIREBASE_PROJECT_ID`: `madartv`
- `FIREBASE_DATABASE_URL`: `https://madartv-default-rtdb.firebaseio.com`
- `FIREBASE_CLIENT_EMAIL`: البريد الخدمي لحساب الإدارة (Service Account Email) الموجود في إعدادات Project Settings -> Service accounts داخل منصة Firebase.
- `FIREBASE_PRIVATE_KEY`: المفتاح الخاص للخدمة بصيغة PEM المشفرة.

---

## 2. حساب ومنصة فيرسل (Vercel Cloud Platform)

تُستخدم منصة **Vercel** كموزع سحابي عالي السرعة (Serverless Global CDN) لاستضافة مركز التحديثات الفورية OTA ونقاط النهاية البرمجية API للتحكم في المنظومة.

| البيان | القيمة / الإعداد |
| :--- | :--- |
| **اسم المشروع الأول (OTA Distribution Hub)** | `zaqzouka-vercel-hub` |
| **اسم المشروع الثاني (Admin & Control API)** | `dashboard-api` |
| **رابط النشر الافتراضي (Production URL)** | `https://zaqzouka-updates.vercel.app` |
| **مسار التحديثات الرسمي** | `https://zaqzouka-updates.vercel.app/updates/android/latest.json` |
| **مسار تحميل تطبيق العميل** | `https://zaqzouka-updates.vercel.app/downloads/zaqzouka-app.apk` |
| **مسار تحميل المشغل** | `https://zaqzouka-updates.vercel.app/downloads/zag-player.apk` |
| **مسار تحميل تطبيق التحكم** | `https://zaqzouka-updates.vercel.app/downloads/zaqzouka-master-control.apk` |
| **التحكم بالسيرفرات عن بعد (API Endpoint)** | `/api/system_control` |

### المتغيرات البيئية المسجلة على Vercel (Project Settings -> Environment Variables):
```bash
ADMIN_SECRET=zaqzouka_secret_2026
FIREBASE_PROJECT_ID=madartv
FIREBASE_DATABASE_URL=https://madartv-default-rtdb.firebaseio.com
NODE_ENV=production
```

---

## 3. كلمات السر ومفاتيح الأمان العليا (Master Security Credentials)

### أ. الرمز السري لتطبيق التحكم (Master Control PIN):
- **الرمز الافتراضي:** `999888`
- **وظيفته:** فتح شاشة لوحة المفاتيح الأمنية داخل تطبيق `ZaqzoukaMasterControl` للدخول إلى لوحة إدارة الطوارئ.
- **تغيير الرمز:** يمكن تغييره من داخل التطبيق نفسه ويتم حفظه مشفراً محلياً في الذاكرة المحمية للهاتف عبر `flutter_secure_storage`.

### ب. الكود السري للطلبات البرمجية (Admin Secret Key):
- **القيمة:** `zaqzouka_secret_2026`
- **وظيفته:** مفتاح المصادقة الإلزامي في ترويسة الطلب (`x-admin-secret`) لكل طلبات التجميد والمسح وإعادة التهيئة الموجهة إلى السيرفر و Firebase لمنع أي محاولة خارجية أو تخمين عشوائي.

### ج. مفتاح الخزنة المشفرة (Encrypted Offline Vault Key):
- **خوارزمية التشفير:** `AES-256-GCM` مع اشتقاق المفاتيح بواسطة `PBKDF2` (10,000 تكرار مع Salt عشوائي).
- **وظيفتها:** يقوم تطبيق التحكم بتخزين نسخة احتياطية كاملة من روابط السيرفرات والقنوات ومفاتيح البث داخل خزنة مشفرة محلياً على هاتف المالك فقط.
- **الفائدة الأمنية القصوى:** لو قام أي شخص باختراق فايربيس أو حذف قاعدة البيانات أو غدر بك، لا يمكنه الوصول إلى هذه الخزنة، وتستطيع بضغطة زر استعادة المنظومة كاملة من هاتفك في 3 ثوانٍ.

---

## 4. مسارات تخزين الاعتمادات في الكود المصدري

> ⚠️ **تحديث:** بعد إعادة التنظيم (نوفمبر 2026)، المسارات الجديدة هي:

1. `01_ZAQZOUKA_CLIENT/MadarFlutter/android/app/google-services.json` — اعتمادات Firebase للتطبيق
2. `04_ADMIN_DASHBOARD/dashboard-api/api/system_control.js` — أوامر التجميد والمسح والاستعادة
3. `04_ADMIN_DASHBOARD/dashboard-api/service-account.json` — مفتاح Firebase Admin SDK 🔑
4. `03_MASTER_CONTROL/ZaqzoukaMasterControl/lib/main.dart` — تطبيق التحكم الكامل
5. `06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/api/status.js` — Quota Guard API (جديد)
6. `06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/api/system_control.js` — System Control API
7. `06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/public/updates/android/latest.json` — OTA manifest
8. `06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/public/status/system.json` — Static fallback (جديد)

## 5. روابط Vercel Endpoints (محدّثة)

| الـ Endpoint | الوصف | Cache |
|---|---|---|
| `GET /api/status` | حالة النظام (Quota Guard) | 60 ثانية CDN |
| `POST /api/system_control` | تحكم كامل (freeze/restore/wipe) | لا cache |
| `GET /status/system.json` | Static fallback | 120 ثانية CDN |
| `GET /updates/android/latest.json` | OTA manifest | No cache |
| `GET /downloads/[app].apk` | تنزيل APK | - |
