# مشهد TV — تطبيق Android وموقع ولوحة تحليلات

هذا المستودع يجمع تطبيق Android الأصلي وموقع الويب ولوحة إدارة محلية لمشروع مشهد TV. معلومات التحليلات التي تظهر في اللوحة مصدرها Google Analytics 4 بعد إعداد الربط؛ لا تُعرض أرقام تجريبية.

## مكونات المشروع

| المسار | المكوّن | التقنية |
| --- | --- | --- |
| `MadarTV/` | تطبيق Android | Kotlin، Jetpack Compose، Media3، Gradle |
| `web/` | موقع الويب وواجهاته ومسارات API | Next.js، React، TypeScript، Firebase Analytics اختياري |
| `dashboard/` | لوحة Analytics عربية محلية | Node.js، Google Analytics Data API |
| `DOCUMENTATION/` | التوثيق الفني والأمني | أدلة بناء وربط وتحليلات وحدود الحماية |

## تشغيل الموقع

```powershell
Set-Location "C:\Users\hassa\Desktop\all project\iptv app\web"
npm install
npm run dev
```

يفتح على `http://localhost:3000`. إعداد Firebase Web اختياري إلى أن تسجل Web app في مشروع Firebase وتملأ `web/.env.local` وفق `web/.env.example`.

## تشغيل لوحة التحليلات

```powershell
Set-Location "C:\Users\hassa\Desktop\all project\iptv app\dashboard"
Copy-Item .env.example .env
npm install
npm start
```

يفتح الخادم افتراضياً على `http://127.0.0.1:4040`. أضف رقم خاصية GA4 إلى `GA_PROPERTY_ID`، ووفّر اعتماداً للقراءة فقط عبر Google Application Default Credentials أو حساب خدمة مخزّن خارج المشروع. لا ترفع ملف اعتماد إلى GitHub. التفاصيل في [دليل Firebase Analytics ولوحة الإدارة](./DOCUMENTATION/08_FIREBASE_ANALYTICS_AND_DASHBOARD.md).

## بناء تطبيق Android

```powershell
Set-Location "C:\Users\hassa\Desktop\all project\iptv app\MadarTV"
.\gradlew.bat :app:assembleDebug
```

لإعداد Firebase Android، ضع `google-services.json` محلياً في `MadarTV/app/` ثم أعد البناء. الملف مستبعد من Git. من دون إعداد Firebase يظل البناء ممكناً، لكن التطبيق لن يسجل أحداث Analytics.

## البيانات المسجلة

تتضمن الأحداث `content_open`, `search`, `playback_start`, `playback_error`. لا يرسل التطبيق نص البحث أو رابط البث أو معرّف جهاز ثابت أو موديله. أحداث Firebase التلقائية مثل `first_open` لا تساوي عدد تنزيلات المتجر. لوحة GA4 تعرض تقارير مجمّعة ولا تعرض سجلاً بأسماء المستخدمين أو جلسات فردية.

## حدود حماية التطبيق

يستخدم بناء Release أداة R8 للتصغير والتشويش، لكن لا يوجد أسلوب يضمن استحالة تحليل APK عند امتلاك الملف. النص أو المفتاح الذي يحتاجه التطبيق أثناء التشغيل قابل للاستخراج. لا تحفظ أسرار خدمة أو مفاتيح حساسة داخل APK. راجع [توثيق حماية APK](./DOCUMENTATION/03_SECURITY_OBFUSCATION_V6.md).

## حالة الربط

مشروع Firebase `madartv` وتطبيق Android `com.madar.tv` مسجلان. ملف إعداد Android غير موجود في المستودع، وتسجيل تطبيق الويب وإعداد قراءة GA4 يحتاجان استكمالاً من حساب Firebase/Google. إلى أن تتم هذه الخطوات لا يمكن أن تعرض اللوحة بيانات استخدام حقيقية.
