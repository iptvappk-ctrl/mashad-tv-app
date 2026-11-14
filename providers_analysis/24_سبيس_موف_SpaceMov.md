# توثيق وتحليل منصة سبيس موف (SpaceMov) الشامل

---

## 1. بطاقة تعريف المنصة (Overview)

| البند | التفاصيل |
| :--- | :--- |
| **اسم المنصة** | سبيس موف — SpaceMov |
| **الدومين الشغال حالياً** | `https://spacemov-tv.com` |
| **دومينات بديلة واحتياطية** | `https://spacemov.net` \| `https://spacemov.top` \| `https://spacemov.xyz` |
| **نوع المحتوى** | أفلام سينمائية عالمية، مسلسلات تلفزيونية غربية، إنتاجات نيتفلكس وهولو |
| **محرك الموقع (CMS)** | نظام WordPress مع قالب البث العالمي DooPlay الشهير |
| **طبيعة الحماية** | جدار حماية Cloudflare مع تبديل دوري لعناوين النطاقات |

---

## 2. بنية الروابط ومحرك البحث (Search & URL Routing)

### أ. نقطة البحث المباشر (Search Endpoint)
* **الرابط:** `https://spacemov-tv.com/?s={query}`
* **طريقة الإرسال:** `GET`
* **معامل البحث:** `s` (مثال: `https://spacemov-tv.com/?s=avatar`)

### ب. هيكل روابط الأفلام والمسلسلات:
* **صفحة الفيلم:** `https://spacemov-tv.com/movies/{slug}/`
* **صفحة المسلسل:** `https://spacemov-tv.com/tv-series/{slug}/`
* **صفحة الحلقة:** `https://spacemov-tv.com/episodes/{slug}-season-{s}-episode-{e}/`

---

## 3. محددات الاستخراج (DOM Selectors - قالب DooPlay)

### أ. بطاقات المحتوى:
* **حاوية النتائج:** `div.items` أو `div.search-page`
* **عنصر البطاقة:** `article.item.movies` أو `article.item.tvshows`
* **رابط المادة:** `div.poster a`
* **عنوان العمل:** `div.data h3 a`
* **صورة البوستر:** `div.poster img[src]`
* **سنة الإنتاج:** `div.data span`
* **الجودة:** `span.quality`

---

## 4. هندسة المشاهدة وسيرفرات البث (DooPlay Player Architecture)

يستخدم نظام DooPlay تبويبات سيرفرات تعتمد على استدعاءات AJAX مخصصة:
```html
<ul id="playeroptionsul">
  <li id="player-option-1" class="dooplay_player_option" data-type="movie" data-post="12345" data-nume="1">
    <span class="title">Server 1 (VidCloud)</span>
  </li>
  <li id="player-option-2" class="dooplay_player_option" data-type="movie" data-post="12345" data-nume="2">
    <span class="title">Server 2 (Streamtape)</span>
  </li>
</ul>

<div id="source-player-1" class="source-box">
  <iframe class="metaframe rptss" src="https://vidcloud9.com/embed/..." frameborder="0" allowfullscreen></iframe>
</div>
```

---

## 5. خطة التكامل مع تطبيق زقزوقة IPTV

1. **إرسال استعلام البحث:** يقوم التطبيق بطلب `/?s={query}` مع ترويسة المتصفح المعتادة.
2. **استخراج السيرفر الافتراضي:** سحب رابط المشغل المضمن مباشرة من `iframe.metaframe[src]`.
3. **توليد السيرفرات البديلة:** قراءة تبويبات `#playeroptionsul li` لاستخراج باقي الخوادم الاحتياطية.
4. **التشغيل بدون إعلانات:**
   * استخراج ملف `master.m3u8` من سيرفر VidCloud لتشغيله في ExoPlayer بجودة 1080p.
   * في حال السيرفرات المحمية بإعلانات منبثقة، يقوم مشغل الـ WebView بفلترة النصوص الإعلانية وتشغيل البث في وضع ملء الشاشة الفوري.
