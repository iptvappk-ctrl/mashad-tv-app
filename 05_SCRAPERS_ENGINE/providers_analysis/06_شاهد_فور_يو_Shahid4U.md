# توثيق وتحليل: موقع شاهد فور يو (Shahid4U)
**عملاق المسلسلات العربية والدراما اليومية**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://shahid4u.cam`
* **الميرورز الاحتياطية**: `https://shahid4u.tv`, `https://shahid4u.to`
* **تنبيه تقني**: نطاق `.to` يفرض أحياناً حماية تحويل بـ JavaScript (`/lander`)، لذا نطاق `shahid4u.cam` هو الأكثر استقراراً للاتصال المباشر بدون حظر.
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://shahid4u.cam/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://shahid4u.cam/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://shahid4u.cam/category/movies/page/{page}/`
  * المسلسلات: `https://shahid4u.cam/category/series/page/{page}/`
* **محددات الـ DOM**:
  * **كرت العمل**: `.MovieItem a, article.MovieItem a, .GridItem a, article a`
  * **العنوان**: `h3, h2, .Title, strong, [title]`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية السيرفرات في شاهد فور يو**:
  * يحتوي الموقع على تبويبات متعددة للسيرفرات أسفل المشغل.
  * كود الأزرار: `.servers-tabs a, ul.servers-list li, a.server-btn`
  * كل زرار يحمل رابط السيرفر المباشر أو معرف `data-embed` أو `data-url`.
* **استخراج الروابط**:
  * السيرفرات المتاحة تشمل:
    * Vidmoly
    * Upstream
    * Streamwish
    * Doodstream
    * Filemoon
    * السيرفر السريع الخاص بشاهد فور يو

---

## 4. فك تشفير البث
* الموقع يستخدم مشغلات Embed شائعة يتم تحليل كود الجافا سكريبت الخاص بها بواسطة الـ `Extractor` التابع للمشغل لجلب ملف `m3u8` المباشر لتشغيله في ExoPlayer فوراً مع خيارات الجودات.
