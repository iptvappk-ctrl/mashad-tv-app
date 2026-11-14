package com.madar.tv.data

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

// ============================================================
// HiTvRepository — استخراج المحتوى من HiTV
// ============================================================

class HiTvRepository {

    companion object {
        private const val TAG = "HiTvRepository"

        // --- API Endpoints (من التحليل) ---
        private val BASE_URLS = listOf(
            "https://api.gohitv.com",
            "https://baoyun.com",
        )

        // Device ID ثابت (يمكن تغييره)
        private val DEVICE_ID = "madar_${System.currentTimeMillis() % 1000000}"

        // Headers المطلوبة
        private fun buildHeaders(extra: Map<String, String> = emptyMap()): Map<String, String> =
            mapOf(
                "User-Agent"   to "okhttp/4.11.0",
                "platform"     to "android",
                "did"          to DEVICE_ID,
                "Accept"       to "application/json",
                "Content-Type" to "application/json"
            ) + extra
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // ============================================================
    // الطريقة الأساسية للطلب
    // ============================================================
    private fun get(path: String, base: String = BASE_URLS[0]): JSONObject? {
        return try {
            val url = "$base$path"
            val reqBuilder = Request.Builder().url(url)
            buildHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: return null
            if (resp.isSuccessful) {
                JSONObject(body)
            } else {
                Log.w(TAG, "HTTP ${resp.code} for $url")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $path failed: ${e.message}")
            null
        }
    }

    private fun getArray(path: String, base: String = BASE_URLS[0]): JSONArray? {
        return try {
            val url = "$base$path"
            val reqBuilder = Request.Builder().url(url)
            buildHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: return null
            if (resp.isSuccessful) JSONArray(body) else null
        } catch (e: Exception) {
            Log.e(TAG, "GET array $path failed: ${e.message}")
            null
        }
    }

    // ============================================================
    // 1. الصفحة الرئيسية
    // ============================================================
    fun getHome(): HiTvHome {
        // محاولة API
        val json = tryEndpoints(listOf(
            "/s1/w/series/api/home",
            "/api/home",
            "/v1/home"
        ))

        if (json != null) {
            return parseHome(json)
        }

        // Fallback: إرجاع هيكل فارغ (سيُملأ من WebView)
        return HiTvHome()
    }

    private fun tryEndpoints(endpoints: List<String>): JSONObject? {
        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val result = get(ep, base)
                if (result != null) return result
            }
        }
        return null
    }

    private fun parseHome(json: JSONObject): HiTvHome {
        return HiTvHome(
            banners      = parseContentList(json.optJSONArray("banners") ?: json.optJSONArray("banner")),
            trending     = parseContentList(json.optJSONArray("trending") ?: json.optJSONArray("hot")),
            newEpisodes  = parseContentList(json.optJSONArray("new_episodes") ?: json.optJSONArray("latest")),
            koreanDramas = parseContentList(json.optJSONArray("korean") ?: json.optJSONArray("kr")),
            chineseDramas= parseContentList(json.optJSONArray("chinese") ?: json.optJSONArray("cn")),
            movies       = parseContentList(json.optJSONArray("movies") ?: json.optJSONArray("movie"))
        )
    }

    // ============================================================
    // 2. قائمة المحتوى حسب الفئة
    // ============================================================
    fun getByCategory(categoryId: String, page: Int = 1): List<HiTvContent> {
        val endpoints = listOf(
            "/s1/w/series/api/series/list?category=$categoryId&page=$page",
            "/api/series?cat=$categoryId&page=$page",
            "/api/category/$categoryId?page=$page",
            "/v1/list?type=$categoryId&page=$page"
        )

        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val json = get(ep, base) ?: continue
                val arr = json.optJSONArray("list")
                    ?: json.optJSONArray("data")
                    ?: json.optJSONArray("items")
                    ?: continue
                return parseContentList(arr)
            }
        }
        return emptyList()
    }

    // ============================================================
    // 3. تفاصيل المحتوى
    // ============================================================
    fun getDetail(contentId: String): HiTvContent? {
        val endpoints = listOf(
            "/s1/w/series/api/series/$contentId",
            "/api/series/$contentId",
            "/api/detail?id=$contentId",
            "/v1/series/$contentId"
        )

        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val json = get(ep, base)?.optJSONObject("data") ?: continue
                return parseContent(json)
            }
        }
        return null
    }

    // ============================================================
    // 4. الحلقات
    // ============================================================
    fun getEpisodes(contentId: String): List<HiTvEpisode> {
        val endpoints = listOf(
            "/s1/w/series/api/series/$contentId/episodes",
            "/api/series/$contentId/episodes",
            "/api/episodes?series_id=$contentId",
            "/v1/episodes?content_id=$contentId"
        )

        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val json = get(ep, base) ?: continue
                val arr = json.optJSONArray("episodes")
                    ?: json.optJSONArray("data")
                    ?: json.optJSONArray("list")
                    ?: continue
                return parseEpisodes(arr, contentId)
            }
        }
        return emptyList()
    }

    // ============================================================
    // 5. رابط البث (الأهم!)
    // ============================================================
    fun getStream(episodeId: String, quality: String = "auto"): HiTvStream? {
        val endpoints = listOf(
            "/s1/w/series/api/episode/$episodeId/stream",
            "/api/episode/$episodeId/stream",
            "/api/stream?episode_id=$episodeId&quality=$quality",
            "/v1/play?id=$episodeId",
            "/play/$episodeId"
        )

        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val json = get(ep, base) ?: continue
                val streamUrl = extractStreamUrl(json) ?: continue
                return HiTvStream(
                    url = streamUrl,
                    quality = json.optString("quality", quality),
                    type = detectStreamType(streamUrl),
                    headers = mapOf(
                        "User-Agent" to "okhttp/4.11.0",
                        "Referer" to "https://www.gohitv.com/"
                    ),
                    subtitleUrl = json.optString("subtitle_url", "")
                )
            }
        }
        return null
    }

    private fun extractStreamUrl(json: JSONObject): String? {
        // محاولة مختلف مفاتيح الـ URL
        val keys = listOf("url", "stream_url", "play_url", "m3u8", "hls_url", "video_url", "src")
        for (key in keys) {
            val url = json.optString(key, "")
            if (url.isNotEmpty() && (url.startsWith("http") || url.startsWith("rtmp"))) {
                return url
            }
        }
        // محاولة nested data
        val data = json.optJSONObject("data") ?: return null
        for (key in keys) {
            val url = data.optString(key, "")
            if (url.isNotEmpty() && url.startsWith("http")) return url
        }
        return null
    }

    private fun detectStreamType(url: String): String = when {
        url.contains(".m3u8") -> "hls"
        url.contains(".mpd") -> "dash"
        url.contains(".mp4") -> "mp4"
        url.contains("rtmp://") -> "rtmp"
        else -> "hls"
    }

    // ============================================================
    // 6. البحث
    // ============================================================
    fun search(query: String, page: Int = 1): List<HiTvContent> {
        val encodedQ = java.net.URLEncoder.encode(query, "UTF-8")
        val endpoints = listOf(
            "/s1/w/series/api/search?q=$encodedQ&page=$page",
            "/api/search?keyword=$encodedQ&page=$page",
            "/search?q=$encodedQ"
        )

        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val json = get(ep, base) ?: continue
                val arr = json.optJSONArray("results")
                    ?: json.optJSONArray("data")
                    ?: json.optJSONArray("list")
                    ?: continue
                return parseContentList(arr)
            }
        }
        return emptyList()
    }

    // ============================================================
    // 7. المحتوى الجديد والشائع
    // ============================================================
    fun getTrending(country: String = "KR"): List<HiTvContent> {
        val endpoints = listOf(
            "/s1/w/series/api/trending?country=$country",
            "/api/hot?country=$country",
            "/api/trending"
        )
        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val json = get(ep, base) ?: continue
                val arr = json.optJSONArray("list") ?: json.optJSONArray("data") ?: continue
                return parseContentList(arr)
            }
        }
        return emptyList()
    }

    fun getNewUpdates(): List<HiTvContent> {
        val endpoints = listOf(
            "/s1/w/series/api/new",
            "/api/latest",
            "/api/new_episodes"
        )
        for (base in BASE_URLS) {
            for (ep in endpoints) {
                val json = get(ep, base) ?: continue
                val arr = json.optJSONArray("list") ?: json.optJSONArray("data") ?: continue
                return parseContentList(arr)
            }
        }
        return emptyList()
    }

    // ============================================================
    // Parsers
    // ============================================================
    private fun parseContentList(arr: JSONArray?): List<HiTvContent> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { parseContent(arr.getJSONObject(i)) }.getOrNull()
        }
    }

    private fun parseContent(j: JSONObject): HiTvContent {
        val country = j.optString("country", j.optString("area", ""))
        return HiTvContent(
            id             = j.optString("id", j.optString("series_id", "")),
            title          = j.optString("title", j.optString("name", j.optString("series_name", ""))),
            titleAr        = j.optString("title_ar", ""),
            poster         = j.optString("poster", j.optString("cover", j.optString("thumbnail", ""))),
            backdrop       = j.optString("backdrop", j.optString("banner", "")),
            year           = j.optInt("year", j.optInt("release_year", 0)),
            country        = country,
            genres         = parseStringList(j.optJSONArray("genres") ?: j.optJSONArray("tags")),
            totalEpisodes  = j.optInt("total_episodes", j.optInt("episode_count", 0)),
            latestEpisode  = j.optInt("latest_episode", j.optInt("current_episode", 0)),
            rating         = j.optDouble("rating", j.optDouble("score", 0.0)).toFloat(),
            description    = j.optString("description", j.optString("intro", j.optString("summary", ""))),
            language       = j.optString("language", ""),
            subtitles      = parseStringList(j.optJSONArray("subtitles")),
            isVip          = j.optBoolean("is_vip", j.optInt("vip", 0) == 1),
            isCompleted    = j.optBoolean("is_completed", j.optString("status", "") == "completed"),
            type           = detectType(j.optString("type", "drama"))
        )
    }

    private fun parseEpisodes(arr: JSONArray, contentId: String): List<HiTvEpisode> {
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val j = arr.getJSONObject(i)
                HiTvEpisode(
                    id        = j.optString("id", j.optString("episode_id", "$contentId-${i+1}")),
                    contentId = contentId,
                    number    = j.optInt("number", j.optInt("episode_number", i + 1)),
                    title     = j.optString("title", "الحلقة ${i + 1}"),
                    thumbnail = j.optString("thumbnail", j.optString("cover", "")),
                    duration  = j.optInt("duration", 0),
                    streamUrl = j.optString("stream_url", j.optString("url", "")),
                    isVip     = j.optBoolean("is_vip", false)
                )
            }.getOrNull()
        }
    }

    private fun parseStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { runCatching { arr.getString(it) }.getOrNull() }
    }

    private fun detectType(typeStr: String): HiTvContentType = when {
        typeStr.contains("movie", true) -> HiTvContentType.MOVIE
        typeStr.contains("anime", true) -> HiTvContentType.ANIME
        typeStr.contains("variety", true) || typeStr.contains("show", true) -> HiTvContentType.VARIETY
        typeStr.contains("live", true) -> HiTvContentType.LIVE
        else -> HiTvContentType.DRAMA
    }

    // ============================================================
    // WebView URL للعرض المباشر (Fallback)
    // ============================================================
    fun getWebUrl(contentId: String): String =
        "https://www.gohitv.com/series/$contentId"

    fun getSearchWebUrl(query: String): String =
        "https://www.gohitv.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"

    // URL الرئيسية
    fun getHomeWebUrl(): String = "https://www.gohitv.com"

    // ============================================================
    // AdBlock Script للـ WebView
    // ============================================================
    val AD_BLOCK_JS = """
(function() {
    // إخفاء الإعلانات
    var adSelectors = [
        '.ad', '.ads', '.advertisement', '.ad-container', '.ad-wrapper',
        '.banner-ad', '.popup-ad', '[class*="ad-"]', '[id*="ad-"]',
        '[class*="banner"]', '.interstitial', '.overlay-ad',
        'iframe[src*="ads"]', 'iframe[src*="doubleclick"]',
        'iframe[src*="facebook.com/plugins/ad"]',
        '[class*="applovin"]', '[class*="admob"]', '[class*="mintegral"]',
        '.dm_ad', '[class*="pangle"]', '[class*="ironSource"]'
    ];
    
    function removeAds() {
        adSelectors.forEach(function(sel) {
            document.querySelectorAll(sel).forEach(function(el) {
                el.style.display = 'none';
                el.remove();
            });
        });
    }
    
    // تشغيل فوراً
    removeAds();
    
    // مراقبة التغييرات
    var observer = new MutationObserver(removeAds);
    observer.observe(document.body, { childList: true, subtree: true });
    
    // Block ad requests
    var originalFetch = window.fetch;
    window.fetch = function(url, opts) {
        if (typeof url === 'string' && (
            url.includes('doubleclick') || url.includes('googlesyndication') ||
            url.includes('facebook.com/tr') || url.includes('applovin') ||
            url.includes('mintegral') || url.includes('pangle') ||
            url.includes('ironsource') || url.includes('baidu/mobads') ||
            url.includes('/ads/') || url.includes('/ad/')
        )) {
            return Promise.reject(new Error('Ad blocked'));
        }
        return originalFetch.apply(this, arguments);
    };
    
    console.log('مشهد TV: تم تفعيل حظر الإعلانات ✓');
})();
""".trimIndent()
}
