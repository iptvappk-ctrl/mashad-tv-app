package com.madar.tv.social

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SuperProviderBridge
 * ───────────────────
 * Bridges the TypeScript API (social/src/api/) with Kotlin/Android code.
 * Calls the running Next.js dev server or a deployed endpoint to get:
 *   - Resolved stream URLs (no ads)
 *   - Quality tracks from M3U8
 *   - Ramadan series data
 *   - Franchise trees
 *
 * Usage (from FlutterMediaBridge or any Kotlin service):
 *   val bridge = SuperProviderBridge("http://localhost:3000")
 *   val result = bridge.superResolve(embedUrl = "https://dood.sh/e/abc123")
 *   val streams = result.optJSONArray("streams")
 *
 * When NOT using the Next.js server, all extraction happens natively via
 * the other classes in this package (QualityTrackExtractor, SmartMediaLinker…)
 */
class SuperProviderBridge(
    private val apiBaseUrl: String = "http://10.0.2.2:3000", // Android emulator → host
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val linker  = SmartMediaLinker()
    private val ramadan = RamadanEngine()

    // ── 1. Resolve embed URL → clean stream URLs ──────────────────────────────
    /**
     * Resolves one or more embed URLs to direct CDN stream URLs.
     * Tries the API server first; falls back to native extraction.
     *
     * Returns JSONObject:
     * {
     *   "streams":  [{ "url":"...", "quality":"1080p", "host":"doodstream" }],
     *   "best":     "https://...",
     *   "source":   "api" | "native"
     * }
     */
    suspend fun superResolve(
        embedUrl: String,
        referer: String = "",
        extraEmbedUrls: List<String> = emptyList(),
    ): JSONObject = withContext(Dispatchers.IO) {
        // Build query string
        val params = buildString {
            append("serverUrl=${encode(embedUrl)}")
            if (extraEmbedUrls.isNotEmpty()) {
                append("&embedUrls=${encode(extraEmbedUrls.joinToString(","))}")
            }
            if (referer.isNotEmpty()) append("&referer=${encode(referer)}")
        }

        try {
            val body = getJson("$apiBaseUrl/api/cinema/superresolve?$params")
            body.put("source", "api")
            body
        } catch (e: Exception) {
            // API unavailable — try native M3U8 quality extraction
            nativeFallbackResolve(embedUrl, referer)
        }
    }

    // ── 2. Quality tracks from M3U8 ───────────────────────────────────────────
    /**
     * Extracts quality tracks from an HLS master playlist.
     * Returns JSONObject from QualityTrackExtractor.buildQualityPackage
     */
    suspend fun getQualityTracks(streamUrl: String, referer: String = ""): JSONObject {
        return try {
            val params = "streamUrl=${encode(streamUrl)}&referer=${encode(referer)}"
            getJson("$apiBaseUrl/api/cinema/quality?$params")
        } catch (_: Exception) {
            QualityTrackExtractor.buildQualityPackage(streamUrl, referer)
        }
    }

    // ── 3. Ramadan series ─────────────────────────────────────────────────────
    /**
     * Get Ramadan series. category = "egyptian" | "syrian" | "gulf" | "comedy"
     * Returns JSONObject: { "series": [...], "category": "...", "count": N }
     */
    suspend fun getRamadanSeries(
        category: String = "all",
        live: Boolean = false,
        seriesId: String = "",
    ): JSONObject = withContext(Dispatchers.IO) {
        try {
            val params = "category=$category&live=$live&seriesId=${encode(seriesId)}"
            getJson("$apiBaseUrl/api/cinema/ramadan?$params")
        } catch (_: Exception) {
            // Native fallback
            val list = if (category == "all") ramadan.getAll()
            else ramadan.getByCategory(category)
            JSONObject()
                .put("series", list)
                .put("category", category)
                .put("count", list.length())
                .put("source", "native")
        }
    }

    // ── 4. Franchise / sequel linking ─────────────────────────────────────────
    /**
     * Returns franchise tree for a movie (all parts/sequels).
     */
    suspend fun getFranchise(title: String): JSONObject = withContext(Dispatchers.IO) {
        try {
            getJson("$apiBaseUrl/api/cinema/franchise?title=${encode(title)}")
        } catch (_: Exception) {
            linker.getFranchise(title)
        }
    }

    // ── 5. Series tree ────────────────────────────────────────────────────────
    /**
     * Returns season+episode tree for a series title.
     */
    fun getSeriesTree(title: String): JSONObject = linker.getSeriesTree(title)

    // ── 6. Provider list ──────────────────────────────────────────────────────
    /**
     * Returns all configured providers with their status.
     */
    suspend fun getProviders(): JSONArray = withContext(Dispatchers.IO) {
        try {
            getJson("$apiBaseUrl/api/cinema/providers").optJSONArray("providers") ?: nativeProviderList()
        } catch (_: Exception) {
            nativeProviderList()
        }
    }

    // ── 7. Proxy stream URL (ad-stripped) ────────────────────────────────────
    /**
     * Returns a proxied stream URL that strips ads from HLS playlists.
     * Format: /api/proxy/stream?url=...&ref=...
     */
    fun buildProxiedStreamUrl(streamUrl: String, referer: String = ""): String {
        return "$apiBaseUrl/api/proxy/stream?url=${encode(streamUrl)}&ref=${encode(referer)}"
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun getJson(url: String): JSONObject {
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "ZakzoukaApp/1.0 Android")
            .build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: "{}"
        return JSONObject(body)
    }

    private suspend fun nativeFallbackResolve(embedUrl: String, referer: String): JSONObject {
        // For M3U8 URLs: extract quality tracks directly
        return if (embedUrl.endsWith(".m3u8") || embedUrl.contains(".m3u8?")) {
            val pkg = QualityTrackExtractor.buildQualityPackage(embedUrl, referer)
            val streams = JSONArray()
            val tracks = pkg.optJSONArray("tracks") ?: JSONArray()
            for (i in 0 until tracks.length()) {
                val t = tracks.optJSONObject(i) ?: continue
                streams.put(
                    JSONObject()
                        .put("url", t.optString("streamUrl"))
                        .put("quality", t.optString("label"))
                        .put("host", "hls")
                )
            }
            JSONObject()
                .put("streams", streams)
                .put("best", pkg.optString("bestUrl", embedUrl))
                .put("source", "native-hls")
        } else {
            // Raw URL — pass as-is
            val streams = JSONArray()
            streams.put(
                JSONObject()
                    .put("url", embedUrl)
                    .put("quality", "Auto")
                    .put("host", "direct")
            )
            JSONObject()
                .put("streams", streams)
                .put("best", embedUrl)
                .put("source", "native-direct")
        }
    }

    private fun nativeProviderList(): JSONArray {
        val arr = JSONArray()
        listOf(
            "arabseed" to "عرب سيد",
            "wecima"   to "ويسيما",
            "faselhd"  to "فاصل HD",
            "egybest"  to "إيجي بست",
            "akwam"    to "أكوام",
            "cima4u"   to "سيما فور يو",
        ).forEach { (id, name) ->
            arr.put(JSONObject().put("id", id).put("name", name).put("status", "active"))
        }
        return arr
    }

    private fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
}
