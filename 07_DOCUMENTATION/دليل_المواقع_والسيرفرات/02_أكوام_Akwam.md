# توثيق وتحليل: موقع أكوام (Akwam)
**أشهر وأضخم مكتبة سينمائية عربية**

---

## 1. بيانات النطاق والاتصال
* **الدومين الرئيسي الشغال حالياً**: `https://akw.cam`
* **الميرورز الاحتياطية**: `https://akwam.to`, `https://akwam.cx`, `https://akwam.in`
* **الترويسات المطلوبة (Headers)**:
  ```http
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
  Referer: https://akw.cam/
  Accept-Language: ar,en-US;q=0.9,en;q=0.8
  ```

---

## 2. هندسة البحث وجلب القوائم
* **رابط البحث**:
  ```http
  GET https://akw.cam/?s={query}
  ```
* **تصفح الأقسام والصفحات**:
  * الأفلام: `https://akw.cam/movies?page={page}`
  * المسلسلات: `https://akw.cam/series?page={page}`
* **محددات الـ DOM**:
  * **كرت العمل**: `a.br-card-link[href*='/akfilm-'], a.br-card-link[href*='/akseries-'], .br-card a`
  * **العنوان**: `h3.br-card-title, h3, h2, .title`
  * **الصورة (البوستر)**: `img[data-src], img[src]`
  * **الجودة وسنة الإصدار**: `.badge, .quality, .year`

---

## 3. هندسة صفحات المشاهدة وسيرفرات البث
* **بنية التحويل البرمجي لصفحة المشاهدة**:
  * رابط صفحة الفيلم يكون على نمط:
    `https://akw.cam/akfilm-1234/اسم-الفيلم`
  * صفحة السيرفرات الرسمية في أكوام تُشتق مباشرة بتغيير الرابط إلى:
    `https://akw.cam/watch/akfilm-1234/servers`
  * هذه الميزة في أكوام تتيح تخطي صفحة القصة الطويلة والذهاب مباشرة إلى صلب السيرفرات!
* **استخراج السيرفرات المتاحة**:
  الموقع يعرض نوعين من السيرفرات في صفحة `/servers`:
  1. **المشغل الأساسي الداخلي**:
     * `iframe[src]` الرئيسي داخل عنصر المشغل.
  2. **قائمة السيرفرات الخارجية والبديلة**:
     * كود العناصر: `div.watchServers a, a.server, [data-url], [data-src]`
     * استخراج الرابط المباشر من خصائص:
       `el.attr("data-url")` أو `el.attr("data-src")` أو `el.attr("href")`
     * السيرفرات الشائعة: Akwam Fast Server, Vidmoly, Dood, Streamtape, Uqload.

---

## 4. فك تشفير سيرفرات أكوام
* سيرفرات أكوام الداخلية تعتمد مشغل JWPlayer مخصص.
* الـ Manifest المباشر يظهر في الـ HTML كـ:
  ```javascript
  file: "https://storage.akwam.to/video/master.m3u8?token=..."
  ```
* المشغل يحتاج تمرير `Referer: https://akw.cam/` حتى لا يعطي خطأ HTTP 403 Forbidden.
* عند التقاط هذا الرابط، يعطي جودات: **1080p, 720p, 480p, 360p** جاهزة في ExoPlayer.
