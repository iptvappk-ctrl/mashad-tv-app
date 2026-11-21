# 📱 تطبيق مَشْهَدْ TV — نسخة الأندرويد الأصلية (Android Native)

> **اسم التطبيق الرسمي:** مشهد (Mashad)  
> **Package Name:** `com.madar.tv`  
> **الإصدار:** 0.2.0 — تطبيق Android أصلي، مع مشغّل منفصل داخل المشروع.

---

## 🌟 نظرة عامة

تطبيق Android مبني بـ Kotlin وJetpack Compose، ويضم واجهات للأفلام والمسلسلات والقنوات والمباريات. حماية R8 تقلل حجم Release وتعيد تسمية الرموز؛ لا يوجد تشفير يجعل APK مستحيلاً على التحليل، وأي سر يحتاجه العميل يمكن استخراجه منه.

---

## 🧩 التقنيات والمكتبات المستخدمة

- **لغة البرمجة:** Kotlin 2.2.20 (مع دعم Coroutines و Flow للتعامل مع العمليات غير المتزامنة).
- **واجهة المستخدم:** Jetpack Compose وMaterial Design 3 بواجهة فاتحة واتجاه RTL.
- **مشغل الفيديو:** AndroidX Media3 (ExoPlayer) المتخصص في بث تدفقات HLS (`.m3u8`) و DASH (`.mpd`) بكفاءة عالية وبدون تقطيع.
- **مشغل مشهد المصاحب:** وحدة `:madar-player` مستقلة، مع اختيار مسار الجودة وتوفير البيانات وGoogle Cast؛ يفتحها مشهد من رابط HTTPS.
- **الشبكات والاتصال:** OkHttp 4 مع إعداد Interceptors مخصصة لتزويد الترويسات المطلوبة وتخطي الحظر.
- **تحميل الصور والملصقات:** Coil 3 مدمج مع ذاكرة تخزين مؤقت (Caching) فائقة السرعة.
- **تقليص Release:** R8 وresource shrinking؛ راجع [حدود حماية APK](../DOCUMENTATION/03_SECURITY_OBFUSCATION_V6.md) ولا تضع أسراراً في التطبيق.

---

## 🏛️ البنية البرمجية والمستودعات (Repositories)

```text
MadarTV/
├── app/                           # تطبيق مشهد: com.madar.tv
│   └── src/main/java/com/madar/tv/
│       ├── core/                  # التحليلات والإشعارات
│       ├── data/                  # مصادر الأفلام والمسلسلات والقنوات
│       └── ui/                    # Compose، التنقل، وصفحات العرض
└── madar-player/                  # المشغل المصاحب: com.mader.player
    └── src/main/java/com/madar/player/
        ├── PlayerActivity.kt      # Media3، اختيار الجودة، وزر Google Cast
        └── CastOptionsProvider.kt # إعداد Google Cast
```

---

## 🛡️ حماية APK بواقعية

بناء Release يستخدم R8 لتقليل الكود وإعادة تسمية بعض الرموز. هذا يرفع تكلفة القراءة السطحية، لكنه لا يجعل APK غير قابل للتحليل ولا يخفي مفتاحاً يحتاجه التطبيق أثناء التشغيل. `X.kt` يفك قيماً قديمة مموهة؛ التحويلات والمفاتيح موجودة في العميل ويمكن عكسها. لا يوجد ضمان أن مهندساً أو أداة تحليل لن يستخرجا البيانات من APK.

لا تضع كلمات مرور أو مفاتيح خدمة أو أسرار مزود بيانات في التطبيق. إعداد Firebase Android، عند إضافته، يحتوي على معرّفات مشروع عامة وليس مفتاح خدمة. راجع [توثيق حدود حماية APK](../DOCUMENTATION/03_SECURITY_OBFUSCATION_V6.md) قبل إصدار التطبيق.

---

## 📊 Firebase Analytics

`MashadTelemetry.kt` يسجل أحداث فتح العمل والبحث وبدء المشاهدة وأخطاء التشغيل في Firebase Analytics إذا كانت إعدادات Firebase موجودة. Firebase يسجل تلقائياً `first_open` و`session_start` بعد إعداد SDK. لا يُرسل التطبيق نص البحث أو معرّف جهاز دائم أو موديل الجهاز أو رابط البث. تبدأ الأحداث بالظهور بعد تسجيل تطبيق Firebase وإعادة بناء التطبيق؛ لا تُعرض بيانات افتراضية. اتبع [دليل إعداد Analytics](../DOCUMENTATION/08_FIREBASE_ANALYTICS_AND_DASHBOARD.md).

---

## 🛠️ دليل البناء

لبناء حزمة Google Play للبلاير من مجلد `02_ZAG_PLAYER/MadarTV`:

```powershell
.\gradlew.bat :madar-player:clean :madar-player:bundleRelease
```

- حزمة Play Console: `madar-player/build/outputs/bundle/release/madar-player-release.aab`.
- نسخة الرفع الحالية: `08_RELEASE_BINARIES/RELEASE_BINARIES/zag-player-v1.1.29-code29.aab`.
- معرّف البلاير: `com.mader.player`، الإصدار `1.1.29` (رقم البناء 29)، ويدعم Android 7.0 فما بعد (`minSdk 24`).
- ملف APK المحلي: `madar-player/build/outputs/apk/release/madar-player-release.apk`.
- توقيع Release يحتاج `madar-player/release-key.jks` و`madar-player/keystore.properties` محليًا. الملفان مستثنيان من Git، والبناء يتوقف إذا لم يتوفر إعداد التوقيع بدل استخدام مفتاح Debug.
- تطبيق `:app` منفصل عن البلاير، وله إعدادات وحزمة إصدار خاصة به.

## الاختبار المغلق في Google Play

لحسابات المطور الشخصية الجديدة، يتطلب طلب الوصول إلى الإنتاج اختبارًا مغلقًا يضم 12 مختبرًا على الأقل اشتركوا في الاختبار باستمرار مدة 14 يومًا. إضافة بريد المختبر للقائمة وحدها لا تحتسب اشتراكه. راجع [متطلبات الاختبار الرسمية](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en).

المشغل جاهز كحزمة رفع للاختبار المغلق؛ يتبقى استكمال إعداد المسار في Play Console، اختيار المختبرين ونشر رابط الانضمام. لا يعني نجاح البناء أو تجهيز الحزمة أن التطبيق رُفع أو نُشر.

التحديث الداخلي لتطبيق `:app` منفصل عن إصدار البلاير؛ يحتاج قناة تنزيل عامة ومفتاح توقيع ثابت. Play In-App Updates لا ينطبق على تطبيق التثبيت الجانبي.