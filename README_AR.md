# مشهد TV — تطبيق Android وموقع ولوحة تحليلات

هذا المستودع يجمع تطبيق Android الأصلي وموقع الويب ولوحة إدارة محلية لمشروع مشهد TV. معلومات التحليلات التي تظهر في اللوحة مصدرها Google Analytics 4 بعد إعداد الربط؛ لا تُعرض أرقام تجريبية.

## مكونات المشروع

| المسار | المكوّن | التقنية |
| --- | --- | --- |
| `MadarTV/` | تطبيق مشهد ومشغل Android مستقل | Kotlin، Jetpack Compose، Media3، Google Cast، Gradle |
| `web/` | موقع الويب وواجهاته ومسارات API | Next.js، React، TypeScript، Firebase Analytics اختياري |
| `dashboard/` | لوحة Analytics عربية محلية | Node.js، Google Analytics Data API |
| `DOCUMENTATION/` | التوثيق الفني والأمني | أدلة بناء وربط وتحليلات وحدود الحماية |

## تشغيل الموقع

```powershell
Set-Location "C:\Users\hassa\Desktop\all project\iptv app\web"
npm install
npm run dev
```

يفتح على `http://localhost:3000`. إعداد Firebase Web موجود محلياً في `web/.env.local`، وهو مستبعد من Git.

## تشغيل لوحة التحليلات

```powershell
Set-Location "C:\Users\hassa\Desktop\all project\iptv app\dashboard"
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
npm install
npm start
```

يفتح الخادم افتراضياً على `http://127.0.0.1:4040`. رقم خاصية GA4 مضبوط محلياً. يلزم ملف حساب الخدمة للقراءة فقط في `C:\Users\hassa\.config\madartv\analytics-reader.json` حتى تتصل اللوحة بالتقارير. لا ترفع ملف الاعتماد إلى GitHub. التفاصيل في [دليل Firebase Analytics ولوحة الإدارة](./DOCUMENTATION/08_FIREBASE_ANALYTICS_AND_DASHBOARD.md).

## بناء تطبيق Android

```powershell
Set-Location "C:\Users\hassa\Desktop\all project\iptv app\MadarTV"
.\gradlew.bat :app:assembleDebug :madar-player:assembleDebug
```

للتجربة الجانبية ثبّت APK المشغل `com.madar.player` ثم APK مشهد `com.madar.tv`. التحديث الداخلي لمشهد مؤجل حاليًا.

إعداد Firebase Android موجود محلياً في `MadarTV/app/google-services.json`، والملف مستبعد من Git. البناء الحالي يمر عبر Google Services plugin.

## البيانات المسجلة

تتضمن الأحداث `content_open`, `search`, `playback_start`, `playback_error`. لا يرسل التطبيق نص البحث أو رابط البث أو معرّف جهاز ثابت أو موديله. أحداث Firebase التلقائية مثل `first_open` لا تساوي عدد تنزيلات المتجر. لوحة GA4 تعرض تقارير مجمّعة ولا تعرض سجلاً بأسماء المستخدمين أو جلسات فردية.

## حدود حماية التطبيق

يستخدم بناء Release أداة R8 للتصغير والتشويش، لكن لا يوجد أسلوب يضمن استحالة تحليل APK عند امتلاك الملف. النص أو المفتاح الذي يحتاجه التطبيق أثناء التشغيل قابل للاستخراج. لا تحفظ أسرار خدمة أو مفاتيح حساسة داخل APK. راجع [توثيق حماية APK](./DOCUMENTATION/03_SECURITY_OBFUSCATION_V6.md).

## حالة الربط

مشروع Firebase `madartv` وتطبيقا Android والويب مسجلان. خاصية GA4 رقمها `561118297`، وData API مفعّلة، وبُعد `content_title` مسجل. حساب `madartv-analytics-reader` لديه Viewer فقط مع حجب مقاييس التكلفة والإيراد. لا تعرض اللوحة الأرقام الحقيقية حتى يتوفر ملف الخدمة في المسار المحلي أعلاه وتصل أحداث من بناء Android أو الويب المهيأ.
