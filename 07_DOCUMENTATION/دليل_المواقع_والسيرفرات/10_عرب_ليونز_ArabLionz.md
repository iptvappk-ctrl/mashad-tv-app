# توثيق وتحليل: موقع عرب ليونز (ArabLionz)
**الاسم العريق في عالم التحميل والمشاهدة المباشرة**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://arablionz.cam`
* **الميرورز الاحتياطية**: `https://arablionz.tv`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://arablionz.cam/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://arablionz.cam/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://arablionz.cam/movies/page/{page}/`
  * المسلسلات: `https://arablionz.cam/series/page/{page}/`
* **محددات الـ DOM**:
  * **كرت العمل**: `article a, .MovieBlock a, h2.entry-title a, a[href*='/movie/']`
  * **العنوان**: `h2, h3, .Title, .entry-title, [title]`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية السيرفرات**:
  * عرب ليونز يوفر قائمة سيرفرات تحت عنوان "سيرفرات المشاهدة المباشرة":
    `ul.servers-list li a, .servers-box iframe[src]`
  * يحتوي على روابط مشغلات سريعة لـ Vidmoly, Uqload, Dood.

---

## 4. فك تشفير البث
* يدعم استخراج ملفات الفيديو بصيغ MP4 و HLS المباشرة بدون المرور على صفحات التحويل الإعلانية.
