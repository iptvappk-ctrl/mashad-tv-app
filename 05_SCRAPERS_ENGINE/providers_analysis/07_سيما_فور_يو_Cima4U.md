# توثيق وتحليل: موقع سيما فور يو (Cima4U)
**مكتبة السينما المتجددة على مدار الساعة**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://c4u.cam`
* **الميرورز الاحتياطية**: `https://cima4u.tv`, `https://cima-4u.cam`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://c4u.cam/
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://c4u.cam/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://c4u.cam/movies/?page={page}`
  * المسلسلات: `https://c4u.cam/series/?page={page}`
* **محددات الـ DOM**:
  * **كرت العمل**: `.MovieBlock a, .Block--Movie a, article.MovieBlock a, a[href*='/watch/'], .movies-list a`
  * **العنوان**: `h3, h2, .Title, strong, [title]`
  * **البوستر**: `img[data-src], img[src]`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية التبويبات (Tab-Pane)**:
  * يعتمد سيما فور يو على تبويبات Bootstrap (`.nav-tabs` و `.tab-content`).
  * كل تبويب يحمل سيرفر مشاهدة داخلي أو خارجي.
  * محددات استخراج الإطارات:
    `.tab-pane iframe[src], .watch-box iframe, a.server-btn[data-url]`
  * السيرفرات المتوفرة:
    * سيرفر سيما فور يو الداخلي (C4U Server)
    * Vidmoly
    * Voe
    * Uqload
    * Streamtape

---

## 4. فك تشفير البث والجودات
* مشغل سيما فور يو الداخلي يعتمد خوادم بث HLS بمفتاح توكن زمني.
* التطبيق يلتقط تدفق البث ويسلمه لـ ExoPlayer ليتمكن المستخدم من اختيار الجودة (1080p, 720p, 480p) من شريط التحكم.
