# توثيق وتحليل: موقع عرب سيد (ArabSeed)
**الموقع الأساسي لخدمة الترفيه المباشر**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://m.myseed.tv`
* **الميرورز الاحتياطية**: `https://m.arabseed.site`, `https://arabseed.show`
* **البروتوكول المطلوب**: HTTPS إلزامي.
* **الترويسات (Headers) المطلوبة للاتصال بدون حظر**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://m.myseed.tv/
  Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث الفوري**:
  ```http
  GET https://m.myseed.tv/find/?find={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://m.myseed.tv/movies/page/{page}/`
  * المسلسلات: `https://m.myseed.tv/series/page/{page}/`
* **محددات الـ DOM لاستخراج العناصر**:
  * **كرت العمل (Movie/Series Card)**:
    `ul.Blocks-Ul li.BlockItem, div.MovieBlock, a.MovieItem`
  * **الرابط المباشر للمقال**:
    `a[href*='/post/'], a[href*='/watch/'], a.BlockItem-Link`
  * **العنوان**:
    `h4.Title, .BlockTitle, strong, h3`
  * **البوستر (Poster Image)**:
    `img[data-src]`, `img[src]`
  * **سنة الإنتاج والجودة**:
    `span.Year, span.Quality, span.badge`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية صفحة الفيلم/الحلقة**:
  * صفحة المقال تحتوي على تفاصيل العمل (القصة، الممثلين، زر المشاهدة).
  * زر الانتقال لبوابة المشاهدة يحمل رابطاً على النمط:
    `https://m.myseed.tv/watch/{slug}/`
* **استخراج السيرفرات المتاحة**:
  الموقع يوفر عادة 4 إلى 8 خيارات سيرفرات متباينة:
  1. **سيرفر عرب سيد الداخلي (MySeed Fast Server)**:
     * الرابط يكون بصيغة: `https://m.myseed.tv/vids.php?id={vid}&token={token}`
     * أو بوابة: `https://m.myseed.tv/player-gateway.php?id={id}`
  2. **السيرفرات الخارجية (Embeds)**:
     * Vidmoly: `https://vidmoly.to/embed/{id}.html`
     * Voe: `https://voe.sx/e/{id}`
     * Vidsharing: `https://vidsharing.io/embed/{id}`
     * Doodstream: `https://dood.to/e/{id}`
* **المحددات البرمجية داخل HTML صفحة المشاهدة**:
  * زرار السيرفرات: `.servers-list li, .watch-servers a, .server-select option`
  * إطار المشغل: `iframe[src*='vids.php'], iframe[src*='vidmoly'], iframe[src*='voe'], iframe[src]`

---

## 4. فك تشفير رابط البث المباشر (HLS / m3u8)
* **سيرفر عرب سيد الداخلي (`vids.php`)**:
  1. يتم إرسال طلب `GET` إلى رابط `vids.php` مع `Referer: https://m.myseed.tv/`.
  2. داخل رد الـ HTML، يوجد كود مشغل فيديو يحتوي على وسم:
     ```html
     <source src="https://cdn.myseed.tv/hls/{hash}/master.m3u8" type="application/x-mpegURL">
     ```
  3. استخراج الرابط بواسطة Regex:
     ```kotlin
     val m3u8Regex = Regex("""["'](https?://[^"'\s<>]+\.m3u8(?:\?[^"'\s<>]*)?)["']""")
     ```
  4. هذا الرابط يدعم تعدد الجودات (1080p, 720p, 480p, 360p) مباشرة داخل مشغل التطبيق بدون إعلانات وبدون صفحات وسيطة.
