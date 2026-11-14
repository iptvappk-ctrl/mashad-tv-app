# SCRAPERS ENGINE — Codex / Agent Integration Prompt
# مشروع: MadarTV / مشهد — Flutter IPTV App
# الهدف: دمج محرك السكرابر العربي مع تطبيق Flutter/Android

---

## 🧠 CONTEXT — اقرأ الأول

أنت Codex / AI coding agent. مهمتك دمج **محرك السكرابر العربي** (Python + Kotlin) مع تطبيق Flutter/Android موجود اسمه **MadarTV / مشهد**.

### المسار الكامل للمشروع
```
c:\Users\hassa\Desktop\all project\iptv app\
├── SCRAPERS_ENGINE\                    ← ✅ أنت هنا — المحرك الجديد
│   ├── run_scraper.py                  ← Entry point: CLI + FastAPI server
│   ├── requirements.txt
│   ├── scrapers\
│   │   ├── __init__.py
│   │   ├── models.py                   ← MediaItem, MediaDetail, StreamServer, Episode, Season
│   │   ├── base.py                     ← BaseScraper + extract_servers_smart() with ad bypass
│   │   ├── adbypass.py                 ← ✨ NEW: 10-pattern Ad/Redirect bypass engine
│   │   ├── extractors.py               ← StreamExtractor: Vidmoly, Voe, Uqload, Dood, Streamtape, MySeed
│   │   ├── manager.py                  ← ScraperManager: parallel search_all, movies, series, details
│   │   ├── cli.py                      ← Click CLI
│   │   ├── api.py                      ← FastAPI REST (8 endpoints)
│   │   └── providers\                  ← 24 مزود
│   │       ├── akwam.py
│   │       ├── arabseed.py
│   │       ├── wecima.py
│   │       ├── egybest.py
│   │       ├── cimaclub.py
│   │       ├── cimanow.py
│   │       ├── cimawbas.py
│   │       ├── cima4u.py
│   │       ├── cima4up.py
│   │       ├── cinemaforall.py
│   │       ├── topcinema.py
│   │       ├── fushaar.py
│   │       ├── lodynet.py
│   │       ├── movies4u.py
│   │       ├── movizland.py
│   │       ├── prestige.py
│   │       ├── spacemov.py
│   │       ├── egydead.py
│   │       ├── arablionz.py
│   │       ├── esheeq.py
│   │       ├── faselhd.py
│   │       ├── moviewatcher.py
│   │       ├── moviflex.py
│   │       └── shahid4u.py
│   └── docs\
│       └── 13_UNIFIED_SCRAPERS_SUITE.md
│
├── MadarFlutter\                       ← Flutter App
│   ├── lib\                            ← Dart/Flutter code
│   └── android\app\src\main\kotlin\com\madar\tv\
│       └── data\
│           ├── MultiProviderRepository.kt  ← 184KB ← ✅ الملف الرئيسي للدمج
│           ├── ArabseedRepository.kt       ← 94KB  ← مرجع
│           └── FlutterMediaBridge.kt       ← MethodChannel bridge
```

---

## 🎯 TASK — المهام المطلوبة

### TASK 1 — تحديث MultiProviderRepository.kt بالمعلومات الجديدة

افتح الملف:
```
MadarFlutter/android/app/src/main/kotlin/com/madar/tv/data/MultiProviderRepository.kt
```

ابحث عن `ProviderConfig` أو `companion object` أو `PROVIDERS` map وحدّث الـ domains والـ selectors بناءً على الجدول التالي:

#### ✅ مزودات مؤكدة تعمل (15/24)
| Provider | Base URL | Search Path | Movies Path | Card Selector | Notes |
|---|---|---|---|---|---|
| **akwam** | `https://akw.cam` | `/?s={q}` | `/movies/` | `a.GridItem` | — |
| **arabseed** | `https://m.myseed.tv` | `/find/?word={q}` | `/movies/` | `a[href*='film-']` | Note: param is `word` not `s` |
| **wecima** | `https://mywecima.shop` | `/?s={q}` | `/movies/` | `a[href*='watch-']` | Redirects to mywecima1.best |
| **egybest** | `https://egybest.live` | `/search/?q={q}` | `/browse.php?type=movie&page={p}&lang=ar` | `a.movie-card` | — |
| **cimaclub** | `https://cimaclub.live` | `/?s={q}` | `/movies/` | `a[href*='film']` | — |
| **cimanow** | `https://cimanow.cc` | `/?s={q}` | `/category/الافلام/page/{p}/` | `a[href*='film-']` | Arabic URL category |
| **cimawbas** | `https://cimawbas.rest` | `/?s={q}` | `/movies/` | `a[href*='film-']` | — |
| **cima4u** | `https://c4u.cam` | `/?s={q}` | `/movies/` | `a[href*='film-']` | — |
| **cinemaforall** | `https://c4u.cam` | `/?s={q}` | `/movies/` | `a[href*='film-']` | Same domain as cima4u |
| **topcinema** | `https://topcinemaa.one` | `/?s={q}` | `/movies/page/{p}/` | `a[href*='film-']` | — |
| **fushaar** | `https://fushaar.com` | `/?s={q}` | `/movies/` | `a.movie-block` | — |
| **lodynet** | `https://lodynet.watch` | `/?s={q}` | `/category/افلام-هندية/page/{p}/` | `a[href*='movie-']` | Indian/Asian only |
| **movies4u** | `https://movies4u.icu` | `/?s={q}` | `/movies/page/{p}/` | `a[href*='film-']` | — |
| **movizland** | `https://movizland.life` | `/?s={q}` | `/movies/page/{p}/` | `div.Block a` | Skip taxonomy URLs |
| **prestige** | `https://prstej.asia` | `/?s={q}` | `/movies/page/{p}/` | `a[href*='film-']` | — |
| **spacemov** | `https://spacemov.xyz` | `/?s={q}` | `/movies/page/{p}/` | `a[href*='film-']` | DooPlay AJAX |

#### ❌ مزودات فاشلة (تحتاج مراجعة لاحقة)
| Provider | Issue |
|---|---|
| **faselhd** | Cloudflare bot-protection — يحتاج Playwright/session |
| **shahid4u** | كل الدومينات تعمل redirect |
| **arablionz** | CSS selectors مش مطابقة |
| **cima4up** | Returns 0 items |
| **esheeq** | قسسة.net structure تغيرت |
| **moviewatcher** | Returns 0 items |
| **moviflex** | الدومين محتمل ميت |
| **egydead** | مؤكد على search، الـ movies page لم تُختبر |

---

### TASK 2 — إضافة HTTP Client للـ Python Scraper API في Flutter

#### Option A (مُفضل للتطوير والاختبار) — Flutter يتكلم مع Python FastAPI

**الخطوة 1:** شغّل السيرفر:
```bash
cd "SCRAPERS_ENGINE"
pip install -r requirements.txt
python run_scraper.py serve --port 8000
```

**الخطوة 2:** في Flutter، أنشئ `lib/services/scraper_api_service.dart`:

```dart
import 'package:http/http.dart' as http;
import 'dart:convert';

class ScraperApiService {
  static const String _baseUrl = 'http://localhost:8000';

  /// البحث في كل المزودات
  Future<List<Map<String, dynamic>>> searchAll(String query) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/search?q=${Uri.encodeComponent(query)}'),
    );
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return List<Map<String, dynamic>>.from(data['results'] ?? []);
    }
    throw Exception('Search failed: ${response.statusCode}');
  }

  /// الأفلام من مزود معين
  Future<List<Map<String, dynamic>>> getMovies(String provider, {int page = 1}) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/movies?provider=$provider&page=$page'),
    );
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return List<Map<String, dynamic>>.from(data['results'] ?? []);
    }
    throw Exception('Movies fetch failed');
  }

  /// المسلسلات
  Future<List<Map<String, dynamic>>> getSeries(String provider, {int page = 1}) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/series?provider=$provider&page=$page'),
    );
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return List<Map<String, dynamic>>.from(data['results'] ?? []);
    }
    throw Exception('Series fetch failed');
  }

  /// تفاصيل + سيرفرات + راوابط مباشرة
  Future<Map<String, dynamic>> getDetails(String url) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/details?url=${Uri.encodeComponent(url)}'),
    );
    if (response.statusCode == 200) {
      return json.decode(response.body);
    }
    throw Exception('Details fetch failed');
  }

  /// استخراج رابط المشاهدة المباشر
  Future<List<Map<String, dynamic>>> getServers(String url) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/servers?url=${Uri.encodeComponent(url)}'),
    );
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return List<Map<String, dynamic>>.from(data['servers'] ?? []);
    }
    throw Exception('Server extraction failed');
  }

  /// حلقات المسلسل
  Future<List<Map<String, dynamic>>> getEpisodes(String url) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/episodes?url=${Uri.encodeComponent(url)}'),
    );
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return List<Map<String, dynamic>>.from(data['episodes'] ?? []);
    }
    throw Exception('Episodes fetch failed');
  }

  /// المزودات المتاحة
  Future<List<String>> getProviders() async {
    final response = await http.get(Uri.parse('$_baseUrl/providers'));
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return List<String>.from(data['providers'] ?? []);
    }
    return [];
  }
}
```

**الخطوة 3:** في `pubspec.yaml` تأكد من وجود:
```yaml
dependencies:
  http: ^1.2.0
```

---

#### Option B (للـ production) — Kotlin native port في MultiProviderRepository.kt

ترجم منطق كل سكرابر لـ Kotlin بالنمط ده (نموذج ArabSeed):

```kotlin
// في MultiProviderRepository.kt — أضف كل provider جديد بالشكل ده:

data class ProviderConfig(
    val name: String,
    val baseUrl: String,
    val mirrors: List<String> = emptyList(),
    val searchPath: String = "/?s=",
    val moviesPath: String = "/movies/page/{page}/",
    val seriesPath: String = "/series/page/{page}/",
    val cardSelector: String = "a[href*='film-']",
    val titleSelector: String = "h3, .title, img[alt]",
    val imageSelector: String = "img",
    val paginationSelector: String = "a.next"
)

companion object {
    val PROVIDERS = mapOf(
        "akwam" to ProviderConfig(
            name = "أكوام",
            baseUrl = "https://akw.cam",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a.GridItem"
        ),
        "arabseed" to ProviderConfig(
            name = "عرب سيد",
            baseUrl = "https://m.myseed.tv",
            mirrors = listOf("https://arabseed.cam", "https://arabseed.ac"),
            searchPath = "/find/?word=",         // ← NOTE: param is 'word' not 's'
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "wecima" to ProviderConfig(
            name = "ويسيما",
            baseUrl = "https://mywecima.shop",
            mirrors = listOf("https://mywecima1.best"),
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='watch-']"
        ),
        "egybest" to ProviderConfig(
            name = "إيجي بيست",
            baseUrl = "https://egybest.live",
            mirrors = listOf("https://egybest.rest"),
            searchPath = "/search/?q=",
            moviesPath = "/browse.php?type=movie&page={page}&lang=ar",
            cardSelector = "a.movie-card"
        ),
        "cimaclub" to ProviderConfig(
            name = "سيما كلوب",
            baseUrl = "https://cimaclub.live",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "cimanow" to ProviderConfig(
            name = "سيما ناو",
            baseUrl = "https://cimanow.cc",
            searchPath = "/?s=",
            moviesPath = "/category/الافلام/page/{page}/",
            seriesPath = "/category/مسلسلات/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "cimawbas" to ProviderConfig(
            name = "سيما وبس",
            baseUrl = "https://cimawbas.rest",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "cima4u" to ProviderConfig(
            name = "سيما فور يو",
            baseUrl = "https://c4u.cam",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "topcinema" to ProviderConfig(
            name = "توب سينما",
            baseUrl = "https://topcinemaa.one",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "fushaar" to ProviderConfig(
            name = "فشار",
            baseUrl = "https://fushaar.com",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a.movie-block"
        ),
        "lodynet" to ProviderConfig(
            name = "لودي نت",
            baseUrl = "https://lodynet.watch",
            searchPath = "/?s=",
            moviesPath = "/category/افلام-هندية/page/{page}/",
            seriesPath = "/category/مسلسلات-هنديه/page/{page}/",
            cardSelector = "a[href*='movie-']"
        ),
        "movies4u" to ProviderConfig(
            name = "موفيز فور يو",
            baseUrl = "https://movies4u.icu",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "movizland" to ProviderConfig(
            name = "موفيز لاند",
            baseUrl = "https://movizland.life",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "div.Block a"
        ),
        "prestige" to ProviderConfig(
            name = "بريستيج",
            baseUrl = "https://prstej.asia",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        ),
        "spacemov" to ProviderConfig(
            name = "سبيس موف",
            baseUrl = "https://spacemov.xyz",
            searchPath = "/?s=",
            moviesPath = "/movies/page/{page}/",
            cardSelector = "a[href*='film-']"
        )
    )
}
```

---

### TASK 3 — Stream Extractor Patterns (استخراج روابط التشغيل)

هذه الأنماط مستخرجة من `SCRAPERS_ENGINE/scrapers/extractors.py`. أضفها في `MultiProviderRepository.kt` أو أنشئ ملف `StreamExtractorHelper.kt`:

```kotlin
object StreamExtractorHelper {

    // --- MySeed / ArabSeed VIP Gateway ---
    // الموقع يعمل redirect من embed link لـ CDN مباشر
    fun extractMySeed(embedUrl: String, client: OkHttpClient): String? {
        val resp = client.newCall(
            Request.Builder().url(embedUrl)
                .header("Referer", "https://m.myseed.tv/")
                .header("User-Agent", "Mozilla/5.0 (Android 14)")
                .build()
        ).execute()
        val body = resp.body?.string() ?: return null
        // Pattern: file: "https://...cdn...mp4"
        val regex = Regex("""file\s*:\s*["']([^"']+\.mp4[^"']*)["']""")
        return regex.find(body)?.groupValues?.get(1)
    }

    // --- Vidmoly ---
    fun extractVidmoly(embedUrl: String, client: OkHttpClient): String? {
        val resp = client.newCall(
            Request.Builder().url(embedUrl)
                .header("Referer", "https://vidmoly.to/")
                .build()
        ).execute()
        val body = resp.body?.string() ?: return null
        val regex = Regex("""sources\s*:\s*\[\{[^}]*file\s*:\s*["']([^"']+)["']""")
        return regex.find(body)?.groupValues?.get(1)
    }

    // --- Voe.sx ---
    fun extractVoe(embedUrl: String, client: OkHttpClient): String? {
        val resp = client.newCall(
            Request.Builder().url(embedUrl).build()
        ).execute()
        val body = resp.body?.string() ?: return null
        // Pattern: 'hls': 'https://...'
        val regex = Regex("""'hls'\s*:\s*'([^']+)'""")
        return regex.find(body)?.groupValues?.get(1)
    }

    // --- Uqload ---
    fun extractUqload(embedUrl: String, client: OkHttpClient): String? {
        val resp = client.newCall(
            Request.Builder().url(embedUrl).build()
        ).execute()
        val body = resp.body?.string() ?: return null
        val regex = Regex("""sources\s*:\s*\["([^"]+)"\]""")
        return regex.find(body)?.groupValues?.get(1)
    }

    // --- DoodStream ---
    fun extractDood(embedUrl: String, client: OkHttpClient): String? {
        val doodUrl = embedUrl.replace("/e/", "/d/")
        val resp = client.newCall(
            Request.Builder().url(doodUrl)
                .header("Referer", "https://dood.watch/")
                .build()
        ).execute()
        val body = resp.body?.string() ?: return null
        val passRegex = Regex("""\$\.(get|post)\('(/pass_md5[^']+)'""")
        val passMd5 = passRegex.find(body)?.groupValues?.get(2) ?: return null
        val tokenRegex = Regex("""token=([^&'"]+)""")
        val token = tokenRegex.find(body)?.groupValues?.get(1) ?: return null
        val domain = embedUrl.split("/")[2]
        val passResp = client.newCall(
            Request.Builder()
                .url("https://$domain$passMd5")
                .header("Referer", doodUrl)
                .build()
        ).execute()
        val base = passResp.body?.string() ?: return null
        return "${base}${System.currentTimeMillis()}?token=$token"
    }

    // --- Streamtape ---
    fun extractStreamtape(embedUrl: String, client: OkHttpClient): String? {
        val resp = client.newCall(
            Request.Builder().url(embedUrl).build()
        ).execute()
        val body = resp.body?.string() ?: return null
        val regex = Regex("""'(//streamtape\.com/get_video\?[^']+)'""")
        val partial = regex.find(body)?.groupValues?.get(1) ?: return null
        return "https:$partial"
    }

    // --- Generic JS file extractor (fallback) ---
    fun extractGeneric(pageBody: String): String? {
        val patterns = listOf(
            Regex("""file\s*:\s*["']([^"']+\.m3u8[^"']*)["']"""),
            Regex("""file\s*:\s*["']([^"']+\.mp4[^"']*)["']"""),
            Regex("""source\s+src=["']([^"']+\.mp4[^"']*)["']"""),
            Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""")
        )
        for (pattern in patterns) {
            val match = pattern.find(pageBody)
            if (match != null) return match.groupValues[1]
        }
        return null
    }
}
```

---

### TASK 4 — MethodChannel API بين Flutter (Dart) و Android (Kotlin)

#### في FlutterMediaBridge.kt — أضف الميثودز الجديدة:

```kotlin
// في setupMethodChannel() أو handleMethodCall()
"scraper_search" -> {
    val query = call.argument<String>("query") ?: ""
    val provider = call.argument<String>("provider") ?: "all"
    coroutineScope.launch {
        val results = multiProviderRepository.search(query, provider)
        result.success(results.map { it.toMap() })
    }
}
"scraper_movies" -> {
    val provider = call.argument<String>("provider") ?: "akwam"
    val page = call.argument<Int>("page") ?: 1
    coroutineScope.launch {
        val results = multiProviderRepository.getMovies(provider, page)
        result.success(results.map { it.toMap() })
    }
}
"scraper_series" -> {
    val provider = call.argument<String>("provider") ?: "akwam"
    val page = call.argument<Int>("page") ?: 1
    coroutineScope.launch {
        val results = multiProviderRepository.getSeries(provider, page)
        result.success(results.map { it.toMap() })
    }
}
"scraper_details" -> {
    val url = call.argument<String>("url") ?: ""
    coroutineScope.launch {
        val detail = multiProviderRepository.getDetails(url)
        result.success(detail?.toMap())
    }
}
"scraper_servers" -> {
    val url = call.argument<String>("url") ?: ""
    coroutineScope.launch {
        val servers = multiProviderRepository.extractServers(url)
        result.success(servers.map { it.toMap() })
    }
}
"scraper_episodes" -> {
    val url = call.argument<String>("url") ?: ""
    coroutineScope.launch {
        val episodes = multiProviderRepository.getEpisodes(url)
        result.success(episodes.map { it.toMap() })
    }
}
```

#### في Flutter (Dart) — lib/services/native_scraper_service.dart:

```dart
import 'package:flutter/services.dart';

class NativeScraperService {
  static const MethodChannel _channel = MethodChannel('com.madar.tv/media_bridge');

  Future<List<Map>> search(String query, {String provider = 'all'}) async {
    final result = await _channel.invokeMethod('scraper_search', {
      'query': query,
      'provider': provider,
    });
    return List<Map>.from(result ?? []);
  }

  Future<List<Map>> getMovies(String provider, {int page = 1}) async {
    final result = await _channel.invokeMethod('scraper_movies', {
      'provider': provider,
      'page': page,
    });
    return List<Map>.from(result ?? []);
  }

  Future<List<Map>> getSeries(String provider, {int page = 1}) async {
    final result = await _channel.invokeMethod('scraper_series', {
      'provider': provider,
      'page': page,
    });
    return List<Map>.from(result ?? []);
  }

  Future<Map?> getDetails(String url) async {
    final result = await _channel.invokeMethod('scraper_details', {'url': url});
    return result != null ? Map.from(result) : null;
  }

  Future<List<Map>> getServers(String url) async {
    final result = await _channel.invokeMethod('scraper_servers', {'url': url});
    return List<Map>.from(result ?? []);
  }

  Future<List<Map>> getEpisodes(String url) async {
    final result = await _channel.invokeMethod('scraper_episodes', {'url': url});
    return List<Map>.from(result ?? []);
  }
}
```

---

## 🔧 IMPORTANT NOTES — ملاحظات مهمة

### 1. Taxonomy URL Filter (مهم جداً)
في أي parser لروابط الكروت، تجاهل الروابط اللي فيها:
```
/category/, /genre/, /release-year/, /tag/, /actor/, /director/, /year/
```
لأنها تصفحية مش محتوى. مثال في Kotlin:
```kotlin
val TAXONOMY_KEYWORDS = listOf("category", "genre", "release-year", "tag", "actor", "director")
fun isTaxonomyUrl(url: String) = TAXONOMY_KEYWORDS.any { url.contains("/$it/") }
// استخدمه: if (!isTaxonomyUrl(href)) cards.add(href)
```

### 2. SSL / Certificate Bypass
بعض المواقع عندها سيرتفيكيت expired. في Python السكرابر بيعمل `verify=False`. في Kotlin لازم تعمل:
```kotlin
val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
})
val sslContext = SSLContext.getInstance("SSL")
sslContext.init(null, trustAllCerts, java.security.SecureRandom())
val sslSocketFactory = sslContext.socketFactory
val client = OkHttpClient.Builder()
    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
    .hostnameVerifier { _, _ -> true }
    .build()
```

### 3. Mirror Failover Pattern
```kotlin
suspend fun fetchWithMirrors(paths: List<String>, client: OkHttpClient): String? {
    for (baseUrl in paths) {
        try {
            val resp = client.newCall(Request.Builder().url(baseUrl).build()).execute()
            if (resp.isSuccessful) return resp.body?.string()
        } catch (e: Exception) {
            continue
        }
    }
    return null
}
```

### 4. ArabSeed Search — الفرق المهم
```
❌ WRONG: /find/?find=query   ← param name is NOT 'find'
✅ RIGHT:  /find/?word=query   ← param name is 'word'
```

### 5. EgyBest Browse Path
```
❌ WRONG: /movies/?page=1
✅ RIGHT:  /browse.php?type=movie&page=1&lang=ar
```

---

## 🚀 HOW TO START — الخطوات الفعلية

### للبدء السريع (Option A — Python API):
```bash
cd "c:\Users\hassa\Desktop\all project\iptv app\SCRAPERS_ENGINE"
pip install -r requirements.txt
python run_scraper.py serve --port 8000
# السيرفر شغال على http://localhost:8000
# افتح: http://localhost:8000/docs لـ Swagger UI
```

### اختبار سريع:
```bash
# بحث
python run_scraper.py search "inception" --provider akwam

# تست كل المزودات
python run_scraper.py test-all --query "2024"

# استخراج رابط مشاهدة
python run_scraper.py watch "<url_of_movie>"

# JSON output للـ Flutter
python run_scraper.py search "inception" --json
```

### للدمج الكامل في الـ production (Option B — Kotlin):
1. افتح `MultiProviderRepository.kt`
2. ابحث عن `companion object` أو `PROVIDERS` map
3. أضف / حدّث الـ provider configs من جدول TASK 1
4. أضف `StreamExtractorHelper.kt` من TASK 3
5. أضف الـ MethodChannel handlers من TASK 4
6. Build and test

---

## 📊 API ENDPOINTS REFERENCE

| Method | Endpoint | Description |
|---|---|---|
| GET | `/search?q={query}&provider={name}` | بحث (all للكل) |
| GET | `/movies?provider={name}&page={n}` | قائمة أفلام |
| GET | `/series?provider={name}&page={n}` | قائمة مسلسلات |
| GET | `/details?url={url}` | تفاصيل + سيرفرات |
| GET | `/servers?url={url}` | سيرفرات + روابط مباشرة |
| GET | `/episodes?url={url}` | حلقات المسلسل |
| GET | `/providers` | قائمة كل المزودات |
| GET | `/health` | صحة السيرفر |

---

## 📁 JSON Response Schemas

### MediaItem (search / movies / series result)
```json
{
  "id": "arabseed_film-inception-2010",
  "title": "Inception (ابتداء)",
  "url": "https://m.myseed.tv/film-inception-2010/",
  "thumbnail": "https://cdn.myseed.tv/covers/inception.jpg",
  "type": "movie",
  "year": "2010",
  "rating": "8.8",
  "provider": "arabseed",
  "quality": "1080p"
}
```

### StreamServer (details / servers result)
```json
{
  "name": "MySeed VIP",
  "url": "https://cdn.myseed.tv/files/inception.mp4",
  "quality": "1080p",
  "type": "direct",
  "extractor": "myseed_gateway"
}
```

### Episode
```json
{
  "title": "الحلقة 1",
  "url": "https://cimaclub.live/series-name/season-1/episode-1/",
  "season": 1,
  "episode": 1,
  "thumbnail": "..."
}
```




---

## 🛡️ TASK 5 — Ad Bypass & Redirect Handler (محرك تخطي الإعلانات)

### ما يفعله `scrapers/adbypass.py`

الموديول ده بيعالج **10 أنماط** من الإعلانات والـ redirects الموجودة في المواقع العربية:

| Pattern | النمط | الموقع النموذجي |
|---------|-------|----------------|
| 1 | Ad Shortener Chains (shot90, fc.lc, ouo.io) | قصة عشق / أي موقع يستخدم link shorteners |
| 2 | Meta Refresh Redirect | صفحات "انتظر 5 ثواني" |
| 3 | JS window.location Redirect | صفحات بدون محتوى HTML مباشر |
| **4** | **DooPlay WordPress AJAX** ⭐ | SpaceMov, MovieWatcher, Cima4Up وأكثر |
| 5 | FaselHD data-embed AJAX | FaselHD (Cloudflare-protected) |
| 6 | WordPress get_server_link AJAX | WeCima, Akwam |
| **7** | **data-link / data-url Attributes** ⭐ | معظم المواقع العربية |
| 8 | Base64-Encoded URLs in scripts | مواقع obfuscated |
| 9 | Cloudflare Bot Protection ❌ | faselhd, shahid4u — Browser required |
| 10 | HTTP Redirect Chain + Tracking Params | جميع المواقع |

### كيف تستخدمه في Kotlin (MultiProviderRepository.kt)

#### Ad Shortener Decoder — Kotlin Implementation

```kotlin
import android.util.Base64
import java.net.URL

object AdBypassHelper {

    // === Pattern 1: shot90.com base64 decode ===
    fun decodeSshot90Url(shortUrl: String): String {
        return try {
            val uri = android.net.Uri.parse(shortUrl)
            val encoded = uri.getQueryParameter("url") ?: return shortUrl
            val decoded = Base64.decode(encoded, Base64.DEFAULT).toString(Charsets.UTF_8)
            decoded
        } catch (e: Exception) {
            shortUrl
        }
    }

    // === Pattern 1 (generic): Follow redirect chain ===
    fun resolveRedirectChain(url: String, client: OkHttpClient): String {
        return try {
            if ("shot90.com" in url) return decodeSshot90Url(url)
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile)")
                .header("Referer", "https://www.google.com/")
                .build()
            val response = client.newCall(request).execute()
            stripTrackingParams(response.request.url.toString())
        } catch (e: Exception) {
            url
        }
    }

    // === Pattern 10: Strip tracking params ===
    fun stripTrackingParams(url: String): String {
        val junk = setOf("utm_source","utm_medium","utm_campaign","ref","referrer","fbclid","gclid")
        return try {
            val uri = android.net.Uri.parse(url)
            val builder = uri.buildUpon()
            // Remove junk params
            val params = uri.queryParameterNames.filter { it !in junk }
            builder.clearQuery()
            params.forEach { key -> builder.appendQueryParameter(key, uri.getQueryParameter(key)) }
            builder.build().toString()
        } catch (e: Exception) {
            url
        }
    }

    // === Pattern 7: Extract data-link / data-url from HTML ===
    fun extractDataLinks(html: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val attrs = listOf("data-link", "data-url", "data-embed", "data-href", "data-redirect")
        for (attr in attrs) {
            val pattern = Regex("""$attr=["']([^"']+)["']""")
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.startsWith("http") && !url.endsWith(".jpg") && !url.endsWith(".png")) {
                    results.add(Pair(attr, url))
                }
            }
        }
        return results
    }

    // === Pattern 4: DooPlay AJAX ===
    fun extractDooplayParams(html: String): Pair<String?, String?> {
        val nonce = Regex(""""nonce"\s*:\s*"([a-f0-9]+)"""").find(html)?.groupValues?.get(1)
        val postId = Regex(""""post_id"\s*:\s*"?(\d+)"?""").find(html)?.groupValues?.get(1)
            ?: Regex("""data-post=["'](\d+)["']""").find(html)?.groupValues?.get(1)
        return Pair(nonce, postId)
    }

    suspend fun fetchDooplayServers(
        baseUrl: String,
        postId: String,
        nonce: String,
        client: OkHttpClient
    ): List<String> {
        val results = mutableListOf<String>()
        try {
            val body = FormBody.Builder()
                .add("action", "doo_player_ajax")
                .add("post", postId)
                .add("nonce", nonce)
                .build()
            val request = Request.Builder()
                .url("$baseUrl/wp-admin/admin-ajax.php")
                .post(body)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", baseUrl)
                .build()
            val response = client.newCall(request).execute()
            val json = org.json.JSONObject(response.body?.string() ?: return results)
            val embedHtml = json.optString("embed") ?: json.optString("link") ?: return results
            Regex("""src=["']([^"']+)["']""").find(embedHtml)?.groupValues?.get(1)?.let {
                results.add(it)
            }
        } catch (e: Exception) { /* ignore */ }
        return results
    }

    // === Pattern 2: Meta Refresh ===
    fun extractMetaRefresh(html: String): String? {
        return Regex("""content=["'][0-9]+;\s*url=(https?://[^"'\s>]+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
    }

    // === Pattern 3: JS window.location ===
    fun extractJsRedirect(html: String): String? {
        return Regex("""window\.location(?:\.href)?\s*=\s*["']+(https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
    }

    // === Pattern 8: Base64 URLs in scripts ===
    fun extractBase64Urls(html: String): List<String> {
        val results = mutableListOf<String>()
        val pattern = Regex("""["']([A-Za-z0-9+/]{30,}={0,2})["']""")
        pattern.findAll(html).forEach { match ->
            try {
                val decoded = Base64.decode(match.groupValues[1], Base64.DEFAULT).toString(Charsets.UTF_8)
                if (decoded.startsWith("http") && "." in decoded) {
                    results.add(decoded)
                }
            } catch (e: Exception) { }
        }
        return results
    }

    // === MASTER RESOLVER: combines all patterns ===
    suspend fun resolveServers(
        pageUrl: String,
        html: String,
        baseUrl: String,
        client: OkHttpClient
    ): List<String> {
        val servers = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun add(url: String) {
            val resolved = if (isAdShortener(url)) resolveRedirectChain(url, client) else url
            if (resolved.startsWith("http") && seen.add(resolved)) {
                if (!resolved.endsWith(".jpg") && !resolved.endsWith(".png")) {
                    servers.add(resolved)
                }
            }
        }

        // Pattern 2 & 3: Page-level redirect
        extractMetaRefresh(html)?.let { add(it) }
        extractJsRedirect(html)?.let { add(it) }

        // Pattern 7: data-* attributes
        extractDataLinks(html).forEach { (_, url) -> add(url) }

        // Pattern 4: DooPlay AJAX
        val (nonce, postId) = extractDooplayParams(html)
        if (nonce != null && postId != null) {
            fetchDooplayServers(baseUrl, postId, nonce, client).forEach { add(it) }
        }

        // Pattern 8: Base64 URLs
        extractBase64Urls(html).forEach { add(it) }

        // Standard: iframe src
        Regex("""<iframe[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { add(it.groupValues[1]) }

        return servers
    }

    fun isAdShortener(url: String): Boolean {
        val adHosts = setOf(
            "shot90.com","shrinkme.io","adfoc.us","fc.lc","bc.vc",
            "za.gl","ur.ly","oke.io","ouo.io","sh.st","cpa.st","exe.io"
        )
        return try {
            val host = android.net.Uri.parse(url).host?.lowercase()?.removePrefix("www.") ?: ""
            host in adHosts
        } catch (e: Exception) { false }
    }
}
```

### استدعاء في MultiProviderRepository.kt

```kotlin
// في دالة getStreamServers() أو getWatchLinks():
val html = fetchPage(watchUrl)

// الطريقة الجديدة بدل البحث اليدوي عن iframes فقط:
val embedUrls = AdBypassHelper.resolveServers(
    pageUrl  = watchUrl,
    html     = html,
    baseUrl  = provider.baseUrl,
    client   = httpClient
)

// ثم كل embed URL نمرره للـ StreamExtractorHelper
val streams = embedUrls.mapNotNull { embed ->
    when {
        "vidmoly" in embed -> StreamExtractorHelper.extractVidmoly(embed, httpClient)
        "voe.sx"  in embed -> StreamExtractorHelper.extractVoe(embed, httpClient)
        "dood"    in embed -> StreamExtractorHelper.extractDood(embed, httpClient)
        "uqload"  in embed -> StreamExtractorHelper.extractUqload(embed, httpClient)
        "streamtape" in embed -> StreamExtractorHelper.extractStreamtape(embed, httpClient)
        "myseed"  in embed -> StreamExtractorHelper.extractMySeed(embed, httpClient)
        embed.contains(".m3u8") || embed.contains(".mp4") -> embed  // Direct link
        else -> null
    }
}
```

---

## 📖 التوثيق الكامل للـ Ad Bypass

اقرأ: **[docs/AD_BYPASS_DOCUMENTATION.md](./docs/AD_BYPASS_DOCUMENTATION.md)**

يحتوي على:
- شرح تفصيلي لكل من الـ 10 أنماط
- أمثلة حقيقية من المواقع العربية
- Flow diagram كامل للـ pipeline
- متى تستخدم Playwright (browser mode)

---

## 🚦 ملخص ما يجب فعله Codex

### أولاً — Python API (جاهز، شغّله فقط):
```bash
cd SCRAPERS_ENGINE
pip install -r requirements.txt
python run_scraper.py serve --port 8000
```

### ثانياً — Kotlin Integration (ما يجب إضافته):
1. **أضف `AdBypassHelper.kt`** من كود TASK 5 أعلاه في:
   `MadarFlutter/android/app/src/main/kotlin/com/madar/tv/data/`

2. **أضف `StreamExtractorHelper.kt`** من كود TASK 3 في نفس المكان

3. **حدّث `MultiProviderRepository.kt`**:
   - أضف الـ provider configs من جدول TASK 1
   - استبدل دوال `getStreamServers()` بـ `AdBypassHelper.resolveServers()`

4. **حدّث `FlutterMediaBridge.kt`** بالـ MethodChannel handlers من TASK 4

### ثالثاً — Flutter (Dart):
1. أضف `lib/services/scraper_api_service.dart` من TASK 2 (Option A)
2. أو `lib/services/native_scraper_service.dart` من TASK 4

### رابعاً — Test:
```bash
# Python side
python run_scraper.py search "batman" --provider akwam --json
python run_scraper.py watch "https://akw.cam/movie/batman-2022/"

# Kotlin side: run unit tests
./gradlew test
./gradlew assembleDebug
```

