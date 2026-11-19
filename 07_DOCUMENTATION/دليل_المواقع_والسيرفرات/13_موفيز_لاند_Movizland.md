# توثيق وتحليل: موقع موفيز لاند (Movizland)
**أحد أقدم مواقع السينما والأفلام المترجمة**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://movizland.online`
* **الميرورز الاحتياطية**: `https://movizland.com`, `https://movizland.net`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://movizland.online/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://movizland.online/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://movizland.online/category/movies/page/{page}/`
  * المسلسلات: `https://movizland.online/category/series/page/{page}/`
* **محددات الـ DOM**:
  * **كرت العمل**: `article.MovieItem a, .MovieItem a, .Block--Item a, article a`
  * **العنوان**: `h3, h2, .Title, strong.hasyear, [title]`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية السيرفرات**:
  * تحتوي الصفحة على جدول سيرفرات متكامل مع خيارات الجودة:
    `.tab-content iframe, iframe[src], a.server-btn`
  * السيرفرات: Vidmoly, Voe, Dood, Streamtape, Uqload, Movizland Fast.

---

## 4. فك تشفير البث
* يتم استخراج رابط الفيديو المباشر (`.m3u8` أو `.mp4`) بدون المرور بصفحات الإعلانات المنبثقة.
