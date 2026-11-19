# 🎬 توثيق مشغل ZAG Player وتطبيق زقزوقة (Player & Client Apps)

> هذا المستند يوثق بنية المشغل الأصلي المخصص `ZAG Player` وتطبيق العميل `MadarFlutter`، مع شرح هندسي مفصل للحلول الجذرية التي تم تطبيقها على مشاكل العرض الأربعة.

---

## 🏗️ البنية التقنية للمشغل (Player Architecture)

يجمع مشغل **ZAG Player** المطور بلغة Kotlin و Jetpack Compose بين تقنيتين لتوفير أقوى تجربة مشاهدة في الوطن العربي:

1. **محرك ExoPlayer / Google Media3:**
   - مخصص لتشغيل روابط البث المباشر للقنوات المشفرة بصيغة HLS (`.m3u8`) وروابط الأفلام المباشرة (`.mp4`).
   - يدعم التبديل اللحظي بين الجودات (1080p, 720p, 480p, 360p, 240p).
   - مزود بنظام مراقبة استهلاك الباقة بالجيجابايت في الساعة (Smart Bandwidth Sheet).
   - يدعم البث اللاسلكي لشاشات التلفزيون الذكية (Smart TV Casting عبر Google Cast و Miracast).

2. **عارض السينما الفائق (WebView Cinema Mode):**
   - مخصص لتشغيل السيرفرات المضمنة (Embed Servers) وصفحات اليوتيوب لملخصات المباريات والمباريات المعادة.
   - يعتمد على عارض مسرّع بالعتاد (Hardware-Accelerated Fullscreen WebView) مع حجب كامل للإعلانات والنوافذ المنبثقة.

---

## 🛠️ الحلول الهندسية للمشاكل الأربعة المبلغ عنها

### 1. حل مشكلة ميلان الشاشة إلى اليمين وظهور بار أسود على اليسار ("الشاشة مايلة يمين")

- **السبب الجذري:**
  1. عند تعيين لغة هاتف المستخدم على اللغة العربية، يقوم نظام الأندرويد و Compose بتعيين اتجاه التخطيط إلى `RTL` (من اليمين لليسار). عارض الويب ومشغل الفيديو عند تشغيل فيديو بنسبة أبعاد 16:9 في وضع RTL يقوم بمحاذاة الفيديو نحو الحافة اليمنى للشاشة، مما يترك فراغاً أسود بعرض 205 بكسل على الجانب الأيسر.
  2. افتراضياً، في وضع Landscape مع وجود نوتش أو ثقب الكاميرا، يطبق أندرويد حواف أمان (Letterboxing) لمنع التداخل مع الكاميرا.

- **الحل الجذري المطبق في كود `PlayerActivity.kt`:**
  1. تغليف شاشة المشغل بالكامل داخل `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`.
  2. ضبط اتجاه العرض برمجياً على `layoutDirection = View.LAYOUT_DIRECTION_LTR` لكل من `WebView` و `PlayerView`.
  3. تفعيل وضع `SHORT_EDGES` لملء الشاشة بالكامل خلف النوتش:
     ```kotlin
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
         window.attributes.layoutInDisplayCutoutMode =
             WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
     }
     ```
  4. حقن كود تنسيق CSS يجبر الفيديو على التمركز المطلق:
     ```css
     video, .html5-video-player, #player {
         position: fixed !important;
         top: 0 !important; left: 0 !important; right: 0 !important; bottom: 0 !important;
         margin: auto !important;
         width: 100vw !important; height: 100vh !important;
         object-fit: contain !important;
     }
     ```
  - **النتيجة:** الفيديو أصبح متمركزاً بالمليمتر في منتصف الشاشة بدون أي ميلان وبدون أي بار أسود جانبي!

---

### 2. حذف أيقونة كتم الصوت تماماً ومنع ظهورها ("مش عاوز علامة كتم الصوت تظهر")

- **السبب الجذري:**
  تقوم مشغلات الويب واليوتيوب للهواتف بإظهار زر عائم أعلى اليسار يحتوي على مكبر صوت ورمز `[ \x ]` لإلغاء كتم الصوت التلقائي (Unmute Button).

- **الحل الجذري المطبق:**
  1. في كود CSS المحقون: تم إخفاء جميع عناصر إلغاء الكتم نهائياً:
     ```css
     .ytp-unmute, .ytm-unmute, [class*="unmute" i], .ytp-volume-area, .ytp-mute-button {
         display: none !important;
         opacity: 0 !important;
         visibility: hidden !important;
         width: 0 !important; height: 0 !important;
         pointer-events: none !important;
     }
     ```
  2. في كود الجافاسكريبت المحقون: يتم تشغيل الصوت وإلغاء الكتم تلقائياً بمجرد بدء الفيديو:
     ```javascript
     var v = document.querySelector('video');
     if (v) {
         v.muted = false;
         v.volume = 1.0;
     }
     ```
  - **النتيجة:** اختفت أيقونة كتم الصوت تماماً ولم يعد لها أي وجود على الشاشة، ويعمل الصوت تلقائياً بدون أي تدخل من المستخدم!

---

### 3. حل مشكلة الخطأ 152 والنوافذ الخارجية ("فتح تطبيق يوتيوب الخارجي")

- **السبب الجذري:**
  1. روابط التضمين `embed/$id` ترفضها قنوات beIN SPORTS و UEFA بسبب حقوق البث وترجع Error 152.
  2. فتح صفحة الهاتف `m.youtube.com` يطلق أوامر Intent تدعو النظام لفتح تطبيق YouTube الرسمي أو خيار اختيار التطبيق (App Chooser).

- **الحل الجذري المطبق:**
  1. حظر جميع بروتوكولات الأندرويد الخارجية (`intent:`, `market:`, `vnd.youtube:`) في السطر الأول من دالة `shouldOverrideUrlLoading`.
  2. تحميل رابط المشاهدة المباشر `https://www.youtube.com/watch?v=$ytId` مع تعيين **Desktop Chrome User-Agent**:
     `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36`.
  - **النتيجة:** تعمل جميع الملخصات ومباريات دوري الأبطال واليورو مباشرة داخل التطبيق بملء الشاشة بدون أي نوافذ خارجية وبدون أي خطأ!

---

### 4. تفعيل الترجمة الاحترافية ("زر الترجمة منظر بس")

- **السبب الجذري:**
  1. في ExoPlayer: كان الكود يغير معايير `trackSelector` دون إبلاغ `player.trackSelectionParameters` اللحظي.
  2. في الفيديوهات التي لا تحتوي على مسار ترجمة أصلاً (مثل مباريات كرة القدم المباشرة والملخصات الصوتية)، كان الزر يتبدل دون أن يعلم المستخدم بعدم توفر ترجمة لذلك الفيديو.

- **الحل الجذري المطبق:**
  1. تحديث `player.trackSelectionParameters` مباشرة في ExoPlayer لتفعيل مسارات النص التلقائية فوراً:
     ```kotlin
     player?.let { p ->
         p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
             .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
             .build()
     }
     ```
  2. في اليوتيوب ومشغلات الويب: يتم حقن أوامر تشغيل ملفات الكابشن وتفعيل المسار العربي:
     ```javascript
     p.loadModule('captions');
     p.setOption('captions', 'track', {'languageCode': 'ar'});
     p.toggleSubtitlesOn();
     ```
  3. إضافة رسالة تأكيد تفاعلية (Toast Feedback) للمستخدم توضح حالة الترجمة:
     *"تم تفعيل الترجمة (تظهر إذا كان الفيديو يدعم الترجمة)"* أو *"تم إيقاف الترجمة"*.
