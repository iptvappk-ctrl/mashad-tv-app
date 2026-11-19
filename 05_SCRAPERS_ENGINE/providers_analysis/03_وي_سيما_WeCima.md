# توثيق وتحليل: موقع وي سيما / ماي سيما (WeCima / MyCima)
**البديل الأقوى لعرب سيد بعد دمج شبكة ماي سيما**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://wecima.cam`
* **الميرورز الحية وسيرفرات التحميل**:
  * `https://myweecima.shop` (سيرفرات التنزيل والبث البديلة)
  * `https://mycima.work`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://wecima.cam/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://wecima.cam/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://wecima.cam/category/افلام/?page={page}`
  * المسلسلات: `https://wecima.cam/category/مسلسلات/?page={page}`
* **محددات الـ DOM**:
  * **كرت العمل**: `.Thumb--GridItem a, .GridItem a, div.Block--Item a, article a`
  * **العنوان**: `strong.hasyear, h3, h2, .Title, [title]`
  * **البوستر**: `data-src, src, data-lazy-src`
  * **الجودة**: `.Quality, .Resolution`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **معرف الفيديو الذكي (`vid`)**:
  * وي سيما يعتمد على باراميتر فريد في كل رابط صفحة فيلم أو حلقة:
    `.../watch/?vid={ID}` أو `.../film-.../?vid={ID}`
  * يتم استخراج المعرف بواسطة Regex:
    ```kotlin
    val vid = Regex("vid=([a-zA-Z0-9]+)").find(pageUrl)?.groupValues?.get(1)
    ```
* **توليد روابط المشاهدة الفورية**:
  بمجرد استخراج الـ `vid`، لا يحتاج التطبيق إلى فحص صفحات وسيطة، بل يمكنه توليد السيرفرات مباشرة:
  1. **المشغل الرسمي لموقع وي سيما**:
     ```http
     https://wecima.cam/embed.php?vid={vid}
     ```
  2. **صفحة سيرفرات المشاهدة والتحميل البديلة (`downloads.php`)**:
     ```http
     https://myweecima.shop/downloads.php?vid={vid}
     ```
* **استخراج السيرفرات البديلة من `downloads.php`**:
  * عند قراءة HTML الصفحة، يتم سحب جميع روابط الـ `a[href]` التي تحتوي على دومينات البث الشائعة:
    * `Uqload`
    * `Vikingfile`
    * `Turboviplay`
    * `Usersdrive`
    * `Gofile`
    * `Vidspeed`
    * `Vidmoly`
    * `Forafile`
    * `Vidoba`

---

## 4. فك تشفير البث
* مشغل `embed.php` يحتوي على مشغل HTML5 قياسي ينادي على رابط HLS أو MP4.
* سيرفرات `Uqload` المستخرجة من صفحة التحميل توفر رابط MP4 مباشر فائق السرعة عبر نمط:
  `sources: [ "https://...uqload.com/..." ]`
* يدعم التبديل السلس بين الجودات بنقرة واحدة داخل التطبيق.
