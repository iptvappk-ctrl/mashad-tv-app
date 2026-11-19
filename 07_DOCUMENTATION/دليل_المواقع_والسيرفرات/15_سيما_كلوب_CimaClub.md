# توثيق وتحليل: موقع سيما كلوب (CimaClub)
**منصة الترفيه السينمائي المتكاملة**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://cimaclub.live`
* **الميرورز الاحتياطية**: `https://cimaclub.cam`, `https://cimacllub.lat`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://cimaclub.live/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://cimaclub.live/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://cimaclub.live/category/movies/page/{page}/`
  * المسلسلات: `https://cimaclub.live/category/series/page/{page}/`
* **محددات الـ DOM**:
  * **كرت العمل**: `article.MovieItem a, .MovieItem a, div.GridItem a, h3.Title a`
  * **العنوان**: `h3.Title, h3, h2, .Title, [title]`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية السيرفرات**:
  * صفحة المشاهدة تضم خيارات متعددة للسيرفرات:
    `a[href*='/watch/'], iframe[src], .servers-list a`
  * السيرفرات: Vidmoly, Uqload, Fast Server, Streamtape.

---

## 4. فك تشفير البث
* استخراج ملف المانيفست المباشر HLS/MP4 وتشغيله على المشغل لضمان أقصى سرعة واستقرار.
