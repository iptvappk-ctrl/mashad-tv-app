# توثيق وتحليل: موقع قصة عشق (Esheeq / 3sk)
**المنصة الحصرية الأولى للمسلسلات والدراما التركية**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://e.q-ask.video`
* **الميرورز الاحتياطية**: `https://3sk.top`, `https://esheeq.net`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://e.q-ask.video/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://e.q-ask.video/search?q={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://e.q-ask.video/movies?page={page}`
  * المسلسلات: `https://e.q-ask.video/`
* **محددات الـ DOM**:
  * **كرت العمل**: `div.thumb a, article a, .video-card a, a[href*='/video-']`
  * **العنوان**: `h2, h3, .title`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية المشغل في قصة عشق**:
  * قصة عشق تستخدم مشغلات خاصة بها مبنية على منصات بث تركية سريعة.
  * محددات الإطارات:
    `iframe[src*='/video-'], iframe[src*='embed'], .video-player iframe`
  * السيرفرات المتوفرة:
    * سيرفر قصة عشق الأصلي (3sk Server)
    * Vidmoly
    * Doodstream
    * Uqload

---

## 4. فك تشفير البث والجودات
* مشغل قصة عشق يعتمد تدفق HLS مباشر يحتوي على مسارات صوتية ومترجمة مع جودات:
  `1080p Full HD, 720p HD, 480p SD`
* تسليم الـ HLS لمشغل ExoPlayer يتيح تفعيل الترجمة واختيار الجودة بدقة.
