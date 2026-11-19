# هندسة فك تشفير سيرفرات البث واستخراج الفيديو الخام (Stream Extractors)
**الدليل البرمجي لكيفية تحويل روابط الـ Embed إلى فيديوهات خام (.m3u8 / .mp4)**

---

## 💡 لماذا نحتاج الـ Stream Extractors؟
عندما يعطيك أي موقع رابط سيرفر خارجي مثل:
`https://vidmoly.to/embed/abc123xyz.html`
إذا فتحته مباشرة في متصفح أو WebView:
* ستظهر إعلانات ونوافذ منبثقة (Popups).
* قد يعلق المشغل في صفحة خطأ 404 إذا تم حذف الفيديو.
* لن تتمكن من التحكم في الجودة (1080p, 720p, 480p) من مشغل التطبيق.

**الحل الهندسي**: الدخول برمجياً لكود الـ JavaScript الخاص بالسيرفر، واستخراج رابط الفيديو المباشر (`.m3u8` أو `.mp4`)، وتسليمه لمشغل ExoPlayer مباشرة!

---

## 1. سيرفر Vidmoly (السيرفر الأكثر انتشاراً عربياً)
* **بنية الرابط**: `https://vidmoly.to/embed-{id}.html` أو `/embed/{id}.html`
* **آلية الحماية**: يضع رابط الـ HLS داخل كود JS في وسم `<script>`.
* **كود الاستخراج البرمجي (Kotlin / Java)**:
  ```kotlin
  // فحص استجابة الرابط
  val conn = URL(embedUrl).openConnection() as HttpURLConnection
  conn.setRequestProperty("User-Agent", UA)
  conn.setRequestProperty("Referer", referer)
  val html = conn.inputStream.bufferedReader().readText()

  // فحص 404 الفوري
  if (html.contains("404 Not Found") || html.contains("Video deleted")) {
      return null // السيرفر معطل -> الانتقال للبديل فوراً
  }

  // استخراج رابط الـ HLS المباشر
  val m3u8Regex = Regex("""(?:sources|file)\s*:\s*\[?\s*\{?\s*['"]?file['"]?\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE)
  val directM3u8 = m3u8Regex.find(html)?.groupValues?.get(1)
  ```
* **الجودات**: ملف `master.m3u8` المستخرج يحتوي تلقائياً على كافة الجودات:
  * `1080p`
  * `720p`
  * `480p`
  * `360p`

---

## 2. سيرفر Voe (Voe.sx)
* **بنية الرابط**: `https://voe.sx/e/{id}`
* **آلية الحماية**: يقوم أحياناً بتشفير كود السيرفر في متغير Base64 أو توجيه عبر جافا سكريبت.
* **كود الاستخراج البرمجي**:
  ```kotlin
  // 1. استخراج مباشر إذا كان غير مشفر
  val voeHls = Regex("""['"]hls['"]\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""").find(html)?.groupValues?.get(1)

  // 2. فك التشفير إذا كان في مصفوفة مصادر مشفرة Base64
  val base64Match = Regex("""atob\(['"]([A-Za-z0-9+/=]+)['"]\)""").find(html)?.groupValues?.get(1)
  if (base64Match != null) {
      val decoded = String(android.util.Base64.decode(base64Match, android.util.Base64.DEFAULT))
      // استخراج m3u8 من النص بعد فك Base64
  }
  ```

---

## 3. سيرفر Uqload
* **بنية الرابط**: `https://uqload.to/embed-{id}.html` أو `/embed/{id}.html`
* **آلية الحماية**: مشغل JWPlayer مخصص يحتوي على مصفوفة مصادر MP4 مباشرة.
* **كود الاستخراج البرمجي**:
  ```kotlin
  val uqRegex = Regex("""sources\s*:\s*\[\s*["']([^"']+\.mp4[^"']*)["']""")
  val directMp4 = uqRegex.find(html)?.groupValues?.get(1)
  ```
  * يرجع رابط MP4 فائق السرعة يدعم التقديم والترجيع الفوري (Seekable Stream).

---

## 4. سيرفر Streamtape
* **بنية الرابط**: `https://streamtape.com/e/{id}`
* **آلية الحماية**: يقوم بتركيب رابط الفيديو عبر دمج نصين في الجافا سكريبت لمنع الـ Regex البسيط:
  `document.getElementById('robotlink').innerHTML = ...`
* **كود الاستخراج البرمجي**:
  ```kotlin
  val idMatch = Regex("""document\.getElementById\('robotlink'\)\.innerHTML\s*=\s*'([^']+)'\s*\+\s*'([^']+)'""").find(html)
  val videoUrl = if (idMatch != null) {
      "https:" + idMatch.groupValues[1] + idMatch.groupValues[2]
  } else null
  ```

---

## 5. سيرفر Doodstream
* **بنية الرابط**: `https://dood.to/e/{id}`
* **آلية الحماية**: يعتمد على توكن Handshake زمني يسمى `pass_md5`.
* **خطوات فك التشفير**:
  1. البحث عن رابط التوكن في الصفحة:
     `val passUrl = Regex("""/pass_md5/[a-zA-Z0-9]+/[a-zA-Z0-9]+""").find(html)?.value`
  2. إرسال طلب `GET` إلى `https://dood.to$passUrl` مع `Referer: {doodEmbedUrl}`.
  3. الخادم يرجع مسار الفيديو المباشر.
  4. تركيب الرابط النهائي بإضافة أحرف عشوائية ووقت الصلاحية:
     `val finalVideo = "$directPrefix$randomStr?token=$token&expiry=$now"`

---

## 6. كيفية تفعيل الجودات المتعددة داخل مشغل ExoPlayer
عندما يستلم مشغل ExoPlayer رابط HLS المباشر (`.m3u8`):
1. **قراءة المسارات (Tracks)**: المشغل يكتشف تلقائياً دقة كل مسار وبايتات البث (`Height & Bitrate`).
2. **التحكم بالتردد (Quality Selection)**:
   * **1080p**: يتم إرسال `TrackSelectionOverride` لمجموعة الـ 1080p.
   * **720p**: يتم اختيار تدفق 720p المتوازن.
   * **480p / 360p**: يتم اختيار تدفق توفير البيانات للشبكات البطيئة.
3. كل هذا يتم محلياً في المشغل بسلاسة وبدون إعادة تحميل الصفحة أو انتظار إعلانات!
