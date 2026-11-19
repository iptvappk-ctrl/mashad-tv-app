# توثيق وتحليل منصة إيجي ديد (EgyDead) الشامل

---

## 1. بطاقة تعريف المنصة (Overview)

| البند | التفاصيل |
| :--- | :--- |
| **اسم المنصة** | إيجي ديد — EgyDead |
| **الدومين الشغال حالياً** | `https://egydead.surf` |
| **دومينات بديلة واحتياطية** | `https://egidead.cam` \| `https://egydead.casa` \| `https://ww38.egydead.co` |
| **نوع المحتوى** | أفلام أجنبية وعربية، مسلسلات تركية وأجنبية، أنمي، برامج |
| **محرك الموقع (CMS)** | نظام WordPress مخصص بالكامل مع قالب بث وفيديو متقدم |
| **طبيعة الحماية** | جدار حماية Cloudflare WAF (يتطلب رأس `Referer` و `User-Agent` قياسي) |

---

## 2. بنية الروابط ومحرك البحث (Search & URL Routing)

### أ. نقطة البحث المباشر (Search Endpoint)
* **الرابط:** `https://egydead.surf/?s={query}`
* **طريقة الإرسال:** `GET`
* **معامل البحث:** `s` (الكلمة المفتاحية)
* **تنبيه أمني هام لمنع 403:** يتطلب الموقع ترويسة `Referer: https://egydead.surf/` لعدم حظر الطلب من جدار الحماية.

### ب. هيكل روابط الأفلام والمسلسلات:
* **صفحة الفيلم الرئيسية:** `https://egydead.surf/فيلم-{slug}-مترجم/`
* **صفحة المشاهدة المباشرة:** إضافة المعامل `?watch=1` إلى رابط الفيلم:
  `https://egydead.surf/فيلم-{slug}-مترجم/?watch=1`
* **صفحة الحلقة:** `https://egydead.surf/الحلقة-{num}-{slug}/?watch=1`

---

## 3. محددات الاستخراج (DOM Selectors)

### أ. بطاقات المحتوى في نتائج البحث وقوائم الأفلام:
* **حاوية البطاقات:** `div.movies-list` أو `div.posts-list`
* **عنصر البطاقة:** `article.post-item` أو `div.movie-card`
* **رابط المادة:** `a.box` أو `a[href*="/فيلم-"]`
* **اسم الفيلم/المسلسل:** `h2.title` أو `span.movie-title`
* **صورة البوستر:** `img[src]` أو `img[data-src]`
* **سنة الإنتاج:** `span.year` أو `div.badge-year`
* **الجودة (1080p, 720p, WEB-DL):** `span.quality`

---

## 4. هندسة المشاهدة وسيرفرات البث (Player & Embed Architecture)

عند فتح رابط المشاهدة بزيادة `?watch=1`:
```html
<div class="watchAreaMaster">
  <!-- قائمة السيرفرات المتاحة -->
  <ul class="serversList" id="serversList">
    <li data-embed="https://govid.live/play/..." class="active">سيرفر إيجي ديد 1</li>
    <li data-embed="https://uqload.vc/embed-..." >Uqload</li>
    <li data-embed="https://vidmoly.me/w/..." >Vidmoly</li>
    <li data-embed="https://doodstream.com/e/..." >DoodStream</li>
  </ul>

  <!-- إطار المشغل النشط -->
  <div class="player-wrapper">
    <iframe id="video-player" src="https://govid.live/play/..." frameborder="0" allowfullscreen></iframe>
  </div>

  <!-- روابط التحميل المباشرة السريعة (سيرفرات احتياطية) -->
  <ul class="donwload-servers-list">
    <li><a href="https://uqload.vc/ql1kzsrb6m0t" target="_blank">uqload</a></li>
    <li><a href="https://vidmoly.me/..." target="_blank">vidmoly</a></li>
  </ul>
</div>
```

### كود الاستخراج المباشر (Extraction Algorithm):
1. إرسال طلب `GET` إلى صفحة المادة مع إضافة `?watch=1`.
2. فحص محتوى الصفحة وسحب الرابط الافتراضي من `iframe#video-player[src]`.
3. سحب بقية السيرفرات من عناصر `ul#serversList li[data-embed]` أو عناصر `a` بداخل `ul.donwload-servers-list`.
4. تحويل روابط التحميل (مثل Uqload) إلى صيغة التضمين `embed` لتشغيلها مباشرة في التطبيق.

---

## 5. فك تشفير روابط السيرفرات واستخراج جودات HLS

سيرفرات EgyDead تعتمد بالأساس على:
1. **Govid / EgyDead Server:**
   * رابط مضمن: `https://govid.live/play/{token}`
   * يقوم بتوليد ملف `master.m3u8` متعدد الجودات (1080p, 720p, 480p, 360p).
   * استخراج المسار المباشر عبر تتبع ترويسة المشغل أو فحص استجابة الـ Embed.
2. **Uqload (`uqload.vc` / `uqload.to`):**
   * رابط مباشر بصيغة `mp4` بدقة 720p و 1080p يمكن استخراجه من وسم `sources: [ { file: "..." } ]`.
3. **Vidmoly:**
   * استخراج رابط `master.m3u8` من كود `sources: [ { file: "..." } ]`.

---

## 6. متطلبات الشبكة والـ Headers البرمجية

```http
GET /?s=batman HTTP/1.1
Host: egydead.surf
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36
Referer: https://egydead.surf/
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
Accept-Language: ar,en-US;q=0.9,en;q=0.8
Connection: keep-alive
```
