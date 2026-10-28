# 📱 تطبيق مَشْهَدْ TV — نسخة الأندرويد الأصلية (Android Native)

> **اسم التطبيق الرسمي:** مشهد (Mashad)  
> **Package Name:** `com.madar.tv`  
> **الإصدار:** 0.1.0 — تطبيق Android أصلي، مع تجهيز اختياري لـ Firebase Analytics.

---

## 🌟 نظرة عامة

تطبيق Android مبني بـ Kotlin وJetpack Compose، ويضم واجهات للأفلام والمسلسلات والقنوات والمباريات. حماية R8 تقلل حجم Release وتعيد تسمية الرموز؛ لا يوجد تشفير يجعل APK مستحيلاً على التحليل، وأي سر يحتاجه العميل يمكن استخراجه منه.

---

## 🧩 التقنيات والمكتبات المستخدمة

- **لغة البرمجة:** Kotlin 2.2.20 (مع دعم Coroutines و Flow للتعامل مع العمليات غير المتزامنة).
- **واجهة المستخدم:** Jetpack Compose وMaterial Design 3 بواجهة فاتحة واتجاه RTL.
- **مشغل الفيديو:** AndroidX Media3 (ExoPlayer) المتخصص في بث تدفقات HLS (`.m3u8`) و DASH (`.mpd`) بكفاءة عالية وبدون تقطيع.
- **الشبكات والاتصال:** OkHttp 4 مع إعداد Interceptors مخصصة لتزويد الترويسات المطلوبة وتخطي الحظر.
- **تحميل الصور والملصقات:** Coil 3 مدمج مع ذاكرة تخزين مؤقت (Caching) فائقة السرعة.
- **تقليص Release:** R8 وresource shrinking؛ راجع [حدود حماية APK](../DOCUMENTATION/03_SECURITY_OBFUSCATION_V6.md) ولا تضع أسراراً في التطبيق.

---

## 🏛️ البنية البرمجية والمستودعات (Repositories)

```text
com.madar.tv/
├── core/
│   ├── X.kt                      # فك ترميز نصوص قديمة مموهة؛ ليست حماية تشفيرية
│   └── PlayerActivity.kt         # مشغل الفيديو المتكامل عبر AndroidX Media3
├── data/
│   ├── ArabseedRepository.kt     # مستودع جلب وفك تشفير سينما عرب سيد وسيرفرات المشاهدة
│   ├── YacineRepository.kt       # مستودع قنوات وباقات وسيرفرات شبكة ياسين تيفي
│   ├── FootballRepository.kt     # مستودع جدول ومواعيد ونتائج المباريات الحية
│   ├── LiveScoreRepository.kt    # مستودع اللحظات المباشرة للدوريات الأوروبية
│   ├── PublicChannelRepository.kt# مستودع القنوات العامة التلفزيونية المجانية
│   └── MashadTelemetry.kt        # أحداث Analytics اختيارية ومجمّعة
└── ui/
    ├── MainActivity.kt           # النشاط الرئيسي وإدارة التنقل وثيم التطبيق
    ├── components/               # مكونات واجهة المستخدم (بطاقات، مشغلات، قوائم)
    └── theme/                    # الثيم والألوان والخطوط الرسمية
```

---

## 🛡️ حماية APK بواقعية

بناء Release يستخدم R8 لتقليل الكود وإعادة تسمية بعض الرموز. هذا يرفع تكلفة القراءة السطحية، لكنه لا يجعل APK غير قابل للتحليل ولا يخفي مفتاحاً يحتاجه التطبيق أثناء التشغيل. `X.kt` يفك قيماً قديمة مموهة؛ التحويلات والمفاتيح موجودة في العميل ويمكن عكسها. لا يوجد ضمان أن مهندساً أو أداة تحليل لن يستخرجا البيانات من APK.

لا تضع كلمات مرور أو مفاتيح خدمة أو أسرار مزود بيانات في التطبيق. إعداد Firebase Android، عند إضافته، يحتوي على معرّفات مشروع عامة وليس مفتاح خدمة. راجع [توثيق حدود حماية APK](../DOCUMENTATION/03_SECURITY_OBFUSCATION_V6.md) قبل إصدار التطبيق.

---

## 📊 Firebase Analytics

`MashadTelemetry.kt` يسجل أحداث فتح العمل والبحث وبدء المشاهدة وأخطاء التشغيل في Firebase Analytics إذا كانت إعدادات Firebase موجودة. Firebase يسجل تلقائياً `first_open` و`session_start` بعد إعداد SDK. لا يُرسل التطبيق نص البحث أو معرّف جهاز دائم أو موديل الجهاز أو رابط البث. تبدأ الأحداث بالظهور بعد تسجيل تطبيق Firebase وإعادة بناء التطبيق؛ لا تُعرض بيانات افتراضية. اتبع [دليل إعداد Analytics](../DOCUMENTATION/08_FIREBASE_ANALYTICS_AND_DASHBOARD.md).

---

## 🛠️ دليل البناء (Build Instructions)

لإنتاج حزمة Release:
```powershell
Set-Location "c:\Users\hassa\Desktop\all project\iptv app\MadarTV"
./gradlew.bat assembleRelease
```

- ملف الـ APK النهائي يتولد في:
  `app/build/outputs/apk/release/app-release-unsigned.apk`
- لا يوجد نسخ تلقائي إلى سطح المكتب. يتطلب توزيع Release إعداد مفاتيح توقيع مالك التطبيق بشكل مستقل وآمن.
