# توثيق وتحليل: موقع فاصل إعلاني (FaselHD)
**المنصة الرائدة في هوليوود ومسلسلات الجودة الفائقة**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://faselhd.top`
* **الميرورز الاحتياطية**: `https://www.fasel-hd.co`, `https://faselhd.club`
* **ملاحظة أمنية**: تجنب الدومينات القديمة مثل `faselhd.cam` لأنها محجوبة أو محولة لصفحات رقابية.
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://faselhd.top/
  X-Requested-With: XMLHttpRequest
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://faselhd.top/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://faselhd.top/movies/?page={page}`
  * المسلسلات: `https://faselhd.top/series/?page={page}`
* **محددات الـ DOM**:
  * **كرت العمل**: `.postDiv a, div.list-movie a, a.list-title, a[href*='/movie/'], a[href*='/serie/']`
  * **العنوان**: `.list-title, .post__title, h2, h3`
  * **البوستر**: `img[data-src], img[src]`
  * **الجودة**: `.quality, .ribbon`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث (نظام حماية AJAX)
* **آلية الحماية المتبعة في فاصل HD**:
  الموقع لا يضع روابط السيرفرات في كود HTML الأساسي للصفحة لمنع برامج السحب السطحية!
* **خطوات فك السيرفرات**:
  1. قراءة صفحة الفيلم وجلب أزرار السيرفرات:
     `button.btn-service[data-embed], [data-embed]`
  2. كل زرار يحمل معرف سري `data-embed="xxxx"` واسم السيرفر (مثل سيرفر فاصل، سيرفر بديل، سيرفر جودة عالية).
  3. إرسال طلب `POST` فوري إلى نقطة النهاية:
     ```http
     POST https://faselhd.top/ajax/embed
     Content-Type: application/x-www-form-urlencoded
     X-Requested-With: XMLHttpRequest
     Referer: {pageUrl}

     id={data-embed}
     ```
  4. استجابة الخادم ترجع كود HTML صغير يحتوي على الـ `iframe` الحقيقي:
     ```html
     <iframe src="https://player.site/.../embed" frameborder="0"></iframe>
     ```
  5. التطبيق يقوم بتنفيذ هذه الطلبات بالتوازي (Parallel Threads) في أقل من ثانية واحدة لجميع الأزرار، ويجمع كافة السيرفرات.

---

## 4. فك تشفير سيرفرات المشاهدة
* السيرفر الداخلي لفاصل HD يقدم خيارات جودة HLS واضحة:
  `1080p, 720p, 480p, 360p`
* تدفق البث يتم تسليمه لـ ExoPlayer كملف `m3u8`، مما يسمح بالتبديل الفوري بين الجودات بدون انقطاع.
