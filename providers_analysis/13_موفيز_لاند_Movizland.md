# توثيق وتحليل: موقع موفيز لاند (Movizland)
**أحد أقدم مواقع السينما والأفلام المترجمة**

---

## 1. بيانات النطاق والاتصال
* **الدومين الذي اجتاز فحص البحث الحي في 2026-11-13**: `https://movizland.life`
* **الملاحظة**: `movizland.online` أعاد صفحة تحويل، و`.com` لم يعرض عنوان صفحة صالحاً في الفحص؛ لم تتم إضافتهما كمرآتين.
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://movizland.online/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://movizland.life/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * تحقق من أقسام الأفلام والمسلسلات المطلوبة مباشرة قبل استخدامها؛ فحص هذا اليوم غطّى البحث فقط.
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
