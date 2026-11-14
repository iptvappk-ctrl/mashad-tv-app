# توثيق وتحليل: موقع بريستيج (Prestige)
**المصدر الأول للدراما العربية والمسلسلات الحصرية**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://prstej.asia`
* **الميرورز الاحتياطية**: `https://brstej.com`, `https://brstejj.cyou`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://prstej.asia/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://prstej.asia/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://prstej.asia/movies`
  * المسلسلات: `https://prstej.asia/series`
* **محددات الـ DOM**:
  * **كرت العمل**: `.pgh-card a, .pgh-series-card a, .ptoday-card a, .movie__block a`
  * **العنوان**: `.pgh-card-title, .pgh-series-name, h2, h3, .Title`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية السيرفرات في بريستيج**:
  * بريستيج يعتمد مشغل فيديو متطور في صفحة الحلقة:
    `iframe[src*='player'], iframe[src*='embed'], iframe[src]`
  * يحتوي على سيرفرات بديلة منوعة لضمان عمل الحلقة عند الضغط العالي أثناء موسم رمضان.

---

## 4. فك تشفير البث
* استخراج ملف الـ HLS المباشر وتشغيله على المشغل الداخلي مع التحكم الكامل في الجودة.
