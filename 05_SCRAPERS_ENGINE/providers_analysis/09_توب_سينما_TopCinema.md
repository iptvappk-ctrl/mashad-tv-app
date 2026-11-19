# توثيق وتحليل: موقع توب سينما (TopCinema)
**المنصة الصاعدة لعروض السينما الحصرية**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://topcinemaa.vip`
* **الميرورز الاحتياطية**: `https://topcinema.cam`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://topcinemaa.vip/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://topcinemaa.vip/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://topcinemaa.vip/category/movies/page/{page}/`
  * المسلسلات: `https://topcinemaa.vip/category/series/page/{page}/`
* **محددات الـ DOM**:
  * **كرت العمل**: `.Thumb--GridItem a, .GridItem a, article a, div.Block--Item a`
  * **العنوان**: `strong.hasyear, h3, h2, .Title, [title]`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية السيرفرات**:
  * يعتمد قالباً مشابهاً لماي سيما مع أزرار سيرفرات مدمجة:
    `.server-item, .servers-list a, iframe[src]`
  * السيرفرات المتوفرة: Vidmoly, Uqload, Doodstream, Fast Top Server.

---

## 4. فك تشفير البث والجودات
* استخراج روابط m3u8 التلقائية وتشغيلها فوراً عبر ExoPlayer مع دعم الجودات المختلفة.
