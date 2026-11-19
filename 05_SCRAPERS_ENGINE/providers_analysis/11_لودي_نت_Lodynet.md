# توثيق وتحليل: موقع لودي نت (Lodynet)
**المرجع الأول للدراما الهندية والآسيوية والتركية المدبلجة**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://lodynet.watch`
* **الميرورز الاحتياطية**: `https://lodynet.asia`, `https://lodynet.com`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://lodynet.watch/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://lodynet.watch/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://lodynet.watch/page/{page}/`
  * المسلسلات: `https://lodynet.watch/category/series/page/{page}/`
* **محددات الـ DOM**:
  * **كرت العمل**: `article.bt a, .content-box article a, h2.entry-title a, article a`
  * **العنوان**: `h2, h3, .entry-title, .title`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية السيرفرات في لودي نت**:
  * صفحة الحلقة تحتوي على أزرار متعددة لكل سيرفر بديل:
    `[data-embed], [data-src], [data-url], .ep-sv a, .server-btn`
  * السيرفرات المتاحة:
    * سيرفر لودي نت الخاص (Lodynet Direct)
    * Vidmoly
    * Voe
    * Doodstream
    * Ok.ru (شائع جداً في المسلسلات الهندية)

---

## 4. فك تشفير البث
* سيرفرات لودي نت الخاصة وسيرفر Ok.ru يتم استخراج مسار الـ HLS المباشر منها ليتمكن المشاهد من تغيير الجودة من 360p إلى 720p بسهولة.
