# توثيق وتحليل: موقع إيجي بست (EgyBest)
**الاسم الأقدم والأكثر شهرة في السينما العربية والعالمية**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://egybest.mx`
* **الميرورز الاحتياطية**: `https://egy.best`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://egybest.mx/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث الفعلي**:
  ```http
  GET https://egybest.mx/?s={query}
  ```
  *(تنبيه: الرابط القديم `/explore/?q=` تم استبداله لأنه يرجع خطأ 404)*
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://egybest.mx/movies/?page={page}`
  * المسلسلات: `https://egybest.mx/series/?page={page}`
* **محددات الـ DOM**:
  * **كرت العمل**: `a.movie, .movies-list-full a.movie, .content-box a, div.movie-card a`
  * **العنوان**: `h2, h3, .title, .movie-title, span`
  * **البوستر**: `img[data-src], img[src]`
  * **سنة العمل والتقييم**: `span.year, span.rating`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية صفحة الفيلم**:
  * صفحة إيجي بست تحتوي على جدول التحميل والمشاهدة أسفل تفاصيل القصة.
  * المشغل المدمج:
    `iframe.auto-iframe, iframe[src*='embed'], iframe[src]`
  * أزرار المشاهدة المباشرة:
    `a.btn[href*='/watch/'], a[href*='/view/']`
* **استخراج السيرفرات**:
  * الموقع يوفر مشغلات داخلية وخارجية (Vidmoly, Uqload, Doodstream).
  * إيجي بست يقوم أحياناً بوضع الروابط مشفرة في وسم مشغل جافا سكريبت خاص بالـ JWPlayer أو VideoJS.
  * نمط الاستخراج البرمجي:
    ```kotlin
    doc.select("iframe[src]").map { it.attr("abs:src") }
    ```

---

## 4. فك تشفير سيرفرات إيجي بست
* يحتوي الموقع على روابط HLS متكيفة (Adaptive Bitrate) تبدأ من جودة 360p حتى 1080p.
* تمرير ترويسة `Referer: https://egybest.mx/` إلزامي لكي لا يرفض خادم الـ CDN تسليم ملفات الفيديو.
