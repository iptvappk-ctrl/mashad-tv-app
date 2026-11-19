# 🚀 زقزوقة TV - مركز التحديثات الفورية (Vercel OTA Hub)

هذا المشروع مخصص للرفع على منصة **Vercel** مجاناً في دقيقتين، ليكون بمثابة السيرفر الرسمي لتوزيع التحديثات الفورية لتطبيق زقزوقة TV ومشغل ZAG Player.

---

## 🌟 المميزات:
1. **واجهة تحميل احترافية وعصرية (`/`):** صفحة تحميل مباشرة باللغة العربية وسريعة جداً لتحميل الـ APK.
2. **رابط التحديثات الفوري (`/api/version` أو `/updates/android/latest.json`):** يقرأه التطبيق تلقائياً عند الفتح للتحقق من الإصدار وتنزيل التحديث فوراً.
3. **روابط تحميل مباشرة سريعة جداً (`/downloads/zaqzouka-app.apk` و `/downloads/zag-player.apk`).**

---

## 📦 طريقة الرفع على Vercel (بأسهل طريقة):

### الطريقة الأولى: عبر لوحة تحكم Vercel (الأسهل)
1. ادخل على حسابك في [Vercel](https://vercel.com).
2. اضغط **Add New...** ثم **Project**.
3. اسحب مجلد `zaqzouka-vercel-hub` أو ارفعه على GitHub واربطه بـ Vercel.
4. اضغط **Deploy**.
5. سيظهر لك رابط الموقع فوراً مثل: `https://zaqzouka-updates.vercel.app`.

### الطريقة الثانية: عبر Vercel CLI في سطر الأوامر
```bash
npm i -g vercel
cd zaqzouka-vercel-hub
vercel --prod
```

---

## 🔄 كيفية رفع تحديث جديد مستقبلاً:
1. استبدل ملف `zaqzouka-app.apk` أو `zag-player.apk` داخل مجلد `public/downloads/`.
2. عدل رقم الإصدار والـ SHA256 في ملفين:
   - `public/updates/android/latest.json`
   - `api/version.js`
3. أعد الرفع إلى Vercel (`vercel --prod`)، وفوراً ستصل التحديثات لجميع مستخدمي التطبيق في ثانية واحدة!
