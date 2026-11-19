# توثيق وتحليل منصة سيما ناو (CimaNow) الشامل

---

## 1. بطاقة تعريف المنصة (Overview)

| البند | التفاصيل |
| :--- | :--- |
| **اسم المنصة** | سيما ناو — CimaNow |
| **الدومين الشغال حالياً** | `https://cimanow.info` |
| **دومينات بديلة واحتياطية** | `https://cimanow.me` \| `https://cimanow.cc` \| `https://cimanow.net` |
| **نوع المحتوى** | أفلام ومسلسلات عربية، أجنبية، تركية، هندية، آسيوية، أنمي |
| **محرك الموقع (CMS)** | نظام مبني بلغة PHP (Laravel) مخصص بالكامل مع واجهة REST API داخلية |
| **طبيعة الحماية** | نظام حماية ضد البوتات يعتمد على CSRF Token و Tokens مشفرة للسيرفرات |

---

## 2. بنية الروابط ومحرك البحث (Search & URL Routing)

### أ. نقطة البحث المباشر (Search Endpoint)
* **الرابط:** `https://cimanow.info/search?q={query}`
* **طريقة الإرسال:** `GET`
* **معامل البحث:** `q` (مثال: `https://cimanow.info/search?q=avatar`)

### ب. هيكل روابط المواد والمشاهدة:
* **صفحة الفيلم:** `https://cimanow.info/film/{slug}`
* **صفحة الحلقة:** `https://cimanow.info/episode/{slug}`
* **صفحة المشاهدة الرسمية:** استبدال مسار `/film/` أو `/episode/` بمسار `/watch/`:
  `https://cimanow.info/watch/{slug}`
  (مثال: `https://cimanow.info/watch/فيلم-model-citizen-2026-مترجم`)

---

## 3. محددات الاستخراج (DOM Selectors)

### أ. بطاقات نتائج البحث وقوائم المحتوى:
* **حاوية البطاقات:** `div.row` أو `div.movies-grid`
* **عنصر البطاقة:** `div.card` أو `div.movie-box`
* **رابط المادة:** `a.stretched-link` أو `a[href*="/film/"]` أو `a[href*="/episode/"]`
* **اسم المادة:** `h3.card-title` أو `div.title`
* **البوستر:** `img.card-img-top` أو `img[data-src]`
* **شارة الجودة:** `span.badge-quality`

---

## 4. هندسة المشاهدة ونظام فك التشفير بالـ AJAX (Watch & Server Resolver)

منصة سيما ناو تستخدم أسلوباً برمجياً حديثاً لا يضع روابط السيرفرات في الـ HTML مباشرة، بل تعتمد على مصفوفة JSON وتشفير التوكن مع طلب AJAX:

### أ. البيانات المحقونة داخل كود الصفحة:
```javascript
const servers = [
  {"id": 345, "name": "earnvids", "rank": 1, "resolve_token": "f343881ff18f..."},
  {"id": 293, "name": "vinovo",   "rank": 2, "resolve_token": "8f8484e26a30..."},
  {"id": 371, "name": "uqload",   "rank": 4, "resolve_token": "8300d5024ff2..."},
  {"id": 376, "name": "byse",     "rank": 5, "resolve_token": "8fffe48f6ea4..."},
  {"id": 19,  "name": "voe",      "rank": 7, "resolve_token": "36734680a050..."}
];

const watchResolveKey = "b6259fae5a0a366384d5976d03ba68254f35ea9b7efd7a44";
const resolveUrl = "https://cimanow.info/watch/resolve";
const csrfToken = "igZo552qRsRUBvUw4qivoC3ug6rJJAQ7uSctzeEK";
```

### ب. طلب استخراج الرابط الحقيقي (Resolve API Call):
* **الرابط:** `POST https://cimanow.info/watch/resolve`
* **الترويسات الإلزامية:**
  ```http
  Accept: application/json
  Content-Type: application/json
  X-CSRF-TOKEN: {csrfToken}
  X-Requested-With: XMLHttpRequest
  Referer: https://cimanow.info/watch/{slug}
  ```
* **جسم الطلب (JSON Payload):**
  ```json
  {
    "key": "b6259fae5a0a366384d5976d03ba68254f35ea9b7efd7a44",
    "token": "8300d5024ff2ac3e347994cc8a7f31e5cf35281799fd8cd0",
    "human_signal": null
  }
  ```
* **الاستجابة الناتجة (JSON Response):**
  ```json
  {
    "ok": true,
    "url": "https://uqload.to/embed-xxxxxx.html"
  }
  ```

---

## 5. خوارزمية التشغيل وفك التشفير المباشر (Direct Stream Pipeline)

1. يقوم التطبيق بطلب صفحة `/watch/{slug}`.
2. استخراج قيمة `csrfToken` و `watchResolveKey` ومصفوفة `servers` عبر تعبير منتظم (Regex).
3. إرسال طلب POST فوري لنقطة `/watch/resolve` لأول سيرفر مدعوم وسريع (مثل `uqload` أو `voe` أو `byse`).
4. بعد استلام رابط الـ Embed النهائي:
   * **إذا كان Uqload:** يتم سحب رابط الـ mp4 المباشر وتشغيله في ExoPlayer.
   * **إذا كان Voe:** يتم فك تشفير سكريبت HLS واستخراج `master.m3u8` متعدد الجودات (1080p, 720p, 480p).
   * **احتياطياً:** إذا فشل فك التشفير يتم تمرير الرابط لمشغل WebView المدمج.

---

## 6. نموذج كود Kotlin للتطبيق

```kotlin
fun resolveCimaNowServer(watchHtml: String, serverToken: String, resolveKey: String, csrfToken: String): String? {
    val client = OkHttpClient()
    val json = JSONObject().apply {
        put("key", resolveKey)
        put("token", serverToken)
        put("human_signal", JSONObject.NULL)
    }
    val request = Request.Builder()
        .url("https://cimanow.info/watch/resolve")
        .addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/json")
        .addHeader("X-CSRF-TOKEN", csrfToken)
        .addHeader("X-Requested-With", "XMLHttpRequest")
        .addHeader("Referer", "https://cimanow.info/")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return null
            val resObj = JSONObject(body)
            if (resObj.optBoolean("ok")) {
                return resObj.optString("url")
            }
        }
    }
    return null
}
```
