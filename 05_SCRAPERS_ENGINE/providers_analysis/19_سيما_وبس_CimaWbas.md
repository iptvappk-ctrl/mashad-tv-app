# توثيق وتحليل منصة سيما وبس (CimaWbas) الشامل

---

## 1. بطاقة تعريف المنصة (Overview)

| البند | التفاصيل |
| :--- | :--- |
| **اسم المنصة** | سيما وبس — CimaWbas |
| **الدومين الشغال حالياً** | `https://cimawbas.rest` |
| **دومينات بديلة واحتياطية** | `https://cimawebas.com` \| `https://cima-wbas.net` |
| **نوع المحتوى** | أفلام ومسلسلات عربية وأجنبية، مسلسلات تركية ومدبلجة، أنمي |
| **محرك الموقع (CMS)** | سكربت phpMelody مخصص ومطور لإدارة الفيديو والمسلسلات |
| **طبيعة الحماية** | حماية قياسية بدون Cloudflare JS Challenge معقمة ضد البوتات العشوائية |

---

## 2. بنية الروابط ومحرك البحث (Search & URL Routing)

### أ. نقطة البحث المباشر (Search Endpoint)
* **الرابط:** `https://cimawbas.rest/search.php?keywords={query}`
* **طريقة الإرسال:** `GET`
* **معامل البحث:** `keywords` (مثال: `https://cimawbas.rest/search.php?keywords=batman`)

### ب. هيكل روابط الأقسام والمشاهدة:
* **قسم الأفلام الكامل:** `https://cimawbas.rest/movies.php`
* **قسم المسلسلات الكامل:** `https://cimawbas.rest/all-series.php`
* **أحدث الحلقات:** `https://cimawbas.rest/episodes.php`
* **صفحة تشغيل الفيديو (Watch Page):**
  `https://cimawbas.rest/watch.php?vid={video_id}`
  (مثال: `https://cimawbas.rest/watch.php?vid=e3cfd54a9`)

---

## 3. محددات الاستخراج (DOM Selectors)

### أ. بطاقات الفيديو في نتائج البحث وقوائم المحتوى:
* **حاوية القائمة:** `ul.pm-ul-browse-videos` أو `div.row-browse`
* **عنصر البطاقة:** `li.pm-li-video` أو `div.pm-li-category`
* **رابط الفيديو:** `a[href*="watch.php?vid="]`
* **عنوان الفيديو:** `h3 a` أو `span.pm-video-title`
* **صورة البوستر (Thumbnail):** `img.pm-thumb-video` أو `img[src*="/uploads/thumbs/"]`
* **مدة الفيديو والجودة:** `span.pm-label-duration`

---

## 4. هندسة المشاهدة وسيرفرات البث (Player & Embed Structure)

تعتمد المنصة على نمطين من المشغلات:
1. **مشغل السيرفرات المتعددة (Multi-Server Tabs):**
   يتم عرض قائمة بسيرفرات البث داخل عنصر `.list_servers`:
   ```html
   <ul class="list_servers">
     <li id="server_1vid" data-embed="&lt;iframe src='https://vidmoly.me/embed-xxx' ...&gt;">سيرفر 1 (Vidmoly)</li>
     <li id="server_2vid" data-embed="&lt;iframe src='https://uqload.to/embed-xxx' ...&gt;">سيرفر 2 (Uqload)</li>
     <li id="server_3vid" data-embed="&lt;iframe src='https://doodstream.com/e/xxx' ...&gt;">سيرفر 3 (DoodStream)</li>
   </ul>
   ```
   عند النقر يقوم كود الجافاسكربت بحقن كود الـ iframe داخل:
   `#Playerholder .embedded`

2. **مشغل الفيديو المباشر (BiBplayer / Native HTML5):**
   في المواد المستضافة داخلياً تظهر حاوية:
   `#video-wrapper #BiBplayer`
   وتحتوي على وسم `<video>` أو كود `jwplayer` مع رابط مصدر البث.

---

## 5. خوارزمية الاستخراج في التطبيق (Extractor Steps)

1. إرسال استعلام البحث إلى:
   `https://cimawbas.rest/search.php?keywords={query}`
2. استخراج قائمة النتائج مع رابط كل فيديو `watch.php?vid={vid}`.
3. فتح صفحة الفيديو وفحص الكود:
   * **الحالة الأولى:** استخراج روابط السيرفرات المضمنة من كود `data-embed` داخل عناصر `.list_servers li`.
   * **الحالة الثانية:** إذا كانت السيرفرات غير موجودة، يتم فحص عناصر `iframe` المباشرة أو كود مشغل الفيديو الداخلي.
4. إرسال رابط السيرفر (Uqload أو Vidmoly) إلى نظام فك التشفير لاستخراج تدفق HLS `.m3u8` أو `mp4` مباشرة.
