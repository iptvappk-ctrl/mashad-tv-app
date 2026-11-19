# 🌐 دليل مركز التحديثات الفورية على Vercel (Vercel OTA Hub)

تم تجهيز مجلد مشروع كامل باسم `zaqzouka-vercel-hub` لرفعه على Vercel ليكون بمثابة السيرفر السحابي الموثوق لتوزيع التحديثات الفورية لجميع مستخدمي تطبيق زقزوقة TV ومشغل ZAG Player.

---

## 📁 محتويات مشروع Vercel:

```text
zaqzouka-vercel-hub/
├── public/
│   ├── index.html                  (صفحة التحميل العصرية باللغة العربية)
│   ├── downloads/
│   │   ├── zaqzouka-app.apk        (نسخة التطبيق الحالية 52 MB)
│   │   └── zag-player.apk          (نسخة المشغل الحالية 4.7 MB)
│   └── updates/
│       └── android/
│           └── latest.json         (ملف الإصدار الفحص التلقائي)
├── api/
│   ├── version.js                  (API فوري لمعرفة رقم الإصدار وتفاصيل الـ APK)
│   └── system_control.js           (API استقبال أوامر التجميد والمسح والاستعادة)
├── vercel.json                     (إعدادات التوجيه والـ CORS)
├── package.json
└── README.md
```

---

## 🚀 طريقة الرفع على Vercel في دقيقتين:

### الخيار 1: من موقع Vercel مباشرة (الأسهل)
1. افتح حسابك على [Vercel](https://vercel.com).
2. اضغط **Add New Project**.
3. قم بسحب وإسقاط مجلد `zaqzouka-vercel-hub` (أو ارفعه على حسابك في GitHub ثم اختر الـ Repository).
4. اضغط **Deploy**.
5. سيعطيك Vercel فوراً رابطاً مثل: `https://zaqzouka-updates.vercel.app`.

### الخيار 2: عبر سطر الأوامر (Vercel CLI)
```bash
npm i -g vercel
cd zaqzouka-vercel-hub
vercel --prod
```

---

## 🔄 كيف تنشر تحديثاً جديداً في المستقبل؟
عندما تريد إرسال تحديث جديد لكل الهواتف تلقائياً:
1. انسخ ملف الـ APK الجديد وضعه في مجلد `zaqzouka-vercel-hub/public/downloads/`.
2. حدّث رقم الإصدار الجديد ورمز الـ SHA256 في ملف `zaqzouka-vercel-hub/public/updates/android/latest.json`.
3. ارفع التحديث إلى Vercel (`vercel --prod`).
4. **النتيجة:** بمجرد فتح أي مستخدم لتطبيق زقزوقة، سيظهر له فوراً إشعار بوجود تحديث جديد ويبدأ التنزيل والتثبيت مباشرة في ثوانٍ!
