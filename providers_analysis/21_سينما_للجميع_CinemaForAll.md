# توثيق وتحليل شبكة سينما للجميع (CinemaForAll / Cima4U) الشامل

---

## 1. بطاقة تعريف المنصة (Overview)

| البند | التفاصيل |
| :--- | :--- |
| **اسم المنصة** | سينما للجميع — CinemaForAll (التابعة رسمياً لشبكة Cima4U) |
| **الدومين الشغال حالياً** | `https://c4u.cam` |
| **دومينات بديلة واحتياطية** | `https://cima4u4k.site` \| `https://cima4u.watch` \| `https://cima4film.cam` |
| **العلاقة بـ Cima4U** | "سينما للجميع" هو الاسم العربي الرسمي والمطبوع في شعار موقع `Cima4U` منذ تأسيسه، وتعمل جميع النطاقات تحت بنية ومكتبة سحابية واحدة موحدة |
| **نوع المحتوى** | أفلام أجنبية، عربية، تركية، هندية، آسيوية، وثائقية، ومسلسلات كاملة |
| **محرك الموقع (CMS)** | نظام WordPress مخصص مع واجهة إدارة ميديا متقدمة وقوالب سريعة |
| **طبيعة الحماية** | حماية Cloudflare WAF وتدوير تلقائي للدومينات لتفادي الحظر |

---

## 2. بنية الروابط ومحرك البحث (Search & URL Routing)

### أ. نقطة البحث المباشر (Search Endpoint)
* **الرابط:** `https://c4u.cam/?s={query}`
* **طريقة الإرسال:** `GET`
* **معامل البحث:** `s` (مثال: `https://c4u.cam/?s=avatar`)

### ب. هيكل روابط المواد:
* **صفحة الفيلم الرئيسية:** `https://c4u.cam/film-{slug}/` أو `https://c4u.cam/movie/{slug}`
* **صفحة الحلقة:** `https://c4u.cam/episode/{slug}/`
* **صفحة المشاهدة بالسيرفرات:** تضمين تبويبات المشغل داخل نفس صفحة المادة أو الرابط الفرعي `.../watch/`

---

## 3. محددات الاستخراج (DOM Selectors)

### أ. بطاقات النتائج في البحث والأقسام:
* **حاوية القائمة:** `div.posts-layout` أو `div.Block-Content`
* **عنصر البطاقة:** `div.movie-box` أو `div.Post-Item`
* **رابط المادة:** `a.box` أو `a[href*="/film-"]`
* **عنوان الفيلم/المسلسل:** `h2.title` أو `span.movie-title`
* **صورة البوستر:** `img[src]` أو `img[data-src]`
* **سنة العرض:** `span.year`
* **الجودة والترجمة:** `span.quality`

---

## 4. هندسة المشاهدة وسيرفرات البث (Player Tabs & Servers)

تعتمد شبكة سينما للجميع على تبويبات سيرفرات داخلية وخارجية:
```html
<div class="watch-servers">
  <ul class="servers-tabs">
    <li data-embed="https://uqload.to/embed-xxxx.html">سيرفر Uqload</li>
    <li data-embed="https://vidmoly.me/w/xxxx">سيرفر Vidmoly</li>
    <li data-embed="https://fembed.com/v/xxxx">سيرفر Fembed</li>
    <li data-embed="https://doodstream.com/e/xxxx">سيرفر DoodStream</li>
  </ul>
  <div class="embed-responsive">
    <iframe id="main-player" src="https://uqload.to/embed-xxxx.html" allowfullscreen></iframe>
  </div>
</div>
```

---

## 5. خوارزمية فك التشفير واستخراج تدفقات HLS و MP4

1. **سيرفر Uqload (الأفضل والأسرع لمشغل ExoPlayer المباشر):**
   * إرسال طلب لصفحة الـ embed.
   * استخراج رابط الفيديو المباشر من مصفوفة:
     `sources: [ { file: "https://...mp4" } ]`
   * تشغيل الفيديو مباشرة في المشغل مع دعم التقديم والتأخير والتحكم بالصوت.
2. **سيرفر Vidmoly:**
   * استخراج رابط `master.m3u8`.
   * تفعيل الجودات التلقائية (Adaptive Bitrate) أو تحديد جودة محددة (1080p / 720p).
3. **مشغل الـ WebView كدرع حماية نهائي:**
   * في حال تشفير روابط السيرفر بروابط وسيطة، يتم تشغيل السيرفر داخل الـ WebView المانع للإعلانات والنوافذ المنبثقة مباشرة دون أي توقف للمستخدم.
