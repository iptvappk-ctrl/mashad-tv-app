package com.madar.tv.data

/**
 * StreamExtractorHelper.kt
 * =========================
 * استخراج روابط التشغيل المباشرة من embed servers.
 *
 * Supported extractors:
 *  - MySeed Gateway (ArabSeed CDN)
 *  - Vidmoly
 *  - Voe.sx
 *  - Uqload
 *  - DoodStream / Dood
 *  - Streamtape
 *  - Filemoon
 *  - Generic JS/regex fallback
 *
 * راجع: SCRAPERS_ENGINE/scrapers/extractors.py للمرجع الكامل
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class StreamResult(
    val directUrl: String,
    val type:      String,       // "hls" | "mp4" | "direct"
    val quality:   String = "HD",
    val headers:   Map<String, String> = emptyMap(),
    val extractor: String = "unknown",
)

object StreamExtractorHelper {

    private val DEFAULT_UA  = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
    private val MOBILE_UA   = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

    // =====================================================================
    // Auto-detect and route to correct extractor
    // =====================================================================

    /**
     * Auto-detect embed type from URL and extract direct stream.
     *
     * Usage:
     * ```kotlin
     * val stream = StreamExtractorHelper.autoExtract(embedUrl, httpClient)
     * // play stream.directUrl with stream.headers
     * ```
     */
    suspend fun autoExtract(embedUrl: String, client: OkHttpClient): StreamResult? {
        if (embedUrl.isBlank()) return null
        val url = embedUrl.lowercase()

        // Already a direct media link
        if (url.contains(".m3u8") || url.contains(".mp4")) {
            val type = if (url.contains(".m3u8")) "hls" else "mp4"
            return StreamResult(embedUrl, type, "Direct", extractor = "direct")
        }

        return when {
            "myseed"         in url || "player-gateway" in url -> extractMySeed(embedUrl, client)
            "vidmoly"        in url -> extractVidmoly(embedUrl, client)
            "voe.sx"         in url || "voe-unblock" in url   -> extractVoe(embedUrl, client)
            "uqload"         in url -> extractUqload(embedUrl, client)
            "dood"           in url || "ds2play"  in url || "doood" in url -> extractDood(embedUrl, client)
            "streamtape"     in url -> extractStreamtape(embedUrl, client)
            "filemoon"       in url || "moonplayer" in url    -> extractGeneric(embedUrl, client, "filemoon")
            "mixdrop"        in url -> extractGeneric(embedUrl, client, "mixdrop")
            "mp4upload"      in url -> extractGeneric(embedUrl, client, "mp4upload")
            "ok.ru"          in url || "okru" in url          -> extractOkRu(embedUrl, client)
            "dailymotion"    in url -> extractDailymotion(embedUrl, client)
            else                   -> extractGeneric(embedUrl, client, "generic")
        }
    }

    // =====================================================================
    // MySeed Gateway / ArabSeed CDN
    // =====================================================================

    /**
     * ArabSeed embeds via MySeed VIP Gateway CDN.
     * The player JS contains: file: "https://...cdn...mp4"
     */
    suspend fun extractMySeed(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchPage(embedUrl, client, referer = "https://m.myseed.tv/")
                val patterns = listOf(
                    Regex("""file\s*:\s*["']([^"']+\.mp4[^"']*)["']""",  RegexOption.IGNORE_CASE),
                    Regex("""file\s*:\s*["']([^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE),
                    Regex("""source\s+src=["']([^"']+\.mp4)["']""",       RegexOption.IGNORE_CASE),
                )
                for (p in patterns) {
                    val m = p.find(html) ?: continue
                    val url  = m.groupValues[1]
                    val type = if (url.contains(".m3u8")) "hls" else "mp4"
                    return@withContext StreamResult(
                        directUrl = url,
                        type      = type,
                        quality   = "HD",
                        headers   = mapOf("Referer" to "https://m.myseed.tv/"),
                        extractor = "myseed_gateway",
                    )
                }
                null
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // Vidmoly
    // =====================================================================

    suspend fun extractVidmoly(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchPage(embedUrl, client, referer = "https://vidmoly.to/")
                val patterns = listOf(
                    Regex("""sources\s*:\s*\[\s*\{\s*file\s*:\s*["']([^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE),
                    Regex("""file\s*:\s*["']([^"']+\.m3u8[^"']*)["']""",                           RegexOption.IGNORE_CASE),
                    Regex("""["']([^"']+\.m3u8[^"']*)["']""",                                       RegexOption.IGNORE_CASE),
                )
                for (p in patterns) {
                    val m   = p.find(html) ?: continue
                    return@withContext StreamResult(
                        directUrl = m.groupValues[1],
                        type      = "hls",
                        quality   = "HD",
                        headers   = mapOf("Referer" to "https://vidmoly.to/"),
                        extractor = "vidmoly",
                    )
                }
                null
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // Voe.sx
    // =====================================================================

    suspend fun extractVoe(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchPage(embedUrl, client, referer = "https://voe.sx/")
                // Pattern: 'hls': 'https://...'  or  sources: [{file:'...',type:'hls'}]
                val patterns = listOf(
                    Regex("""'hls'\s*:\s*'([^']+)'"""),
                    Regex(""""hls"\s*:\s*"([^"]+)""""),
                    Regex("""file\s*:\s*["']([^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE),
                )
                for (p in patterns) {
                    val m = p.find(html) ?: continue
                    return@withContext StreamResult(
                        directUrl = m.groupValues[1],
                        type      = "hls",
                        quality   = "HD",
                        headers   = mapOf("Referer" to "https://voe.sx/"),
                        extractor = "voe",
                    )
                }
                null
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // Uqload
    // =====================================================================

    suspend fun extractUqload(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchPage(embedUrl, client, referer = "https://uqload.co/")
                val m = Regex("""sources\s*:\s*\["([^"]+)"\]""").find(html)
                    ?: Regex("""file\s*:\s*["']([^"']+\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE).find(html)
                    ?: return@withContext null
                StreamResult(
                    directUrl = m.groupValues[1],
                    type      = "mp4",
                    quality   = "HD",
                    headers   = mapOf("Referer" to "https://uqload.co/"),
                    extractor = "uqload",
                )
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // DoodStream / Dood
    // =====================================================================

    suspend fun extractDood(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val doodUrl = embedUrl.replace("/e/", "/d/").replace("/f/", "/d/")
                val html    = fetchPage(doodUrl, client, referer = "https://dood.watch/")

                // Extract /pass_md5/... path
                val passMd5Match = Regex("""\$\.(?:get|post)\s*\(\s*'(/pass_md5[^']+)'""").find(html)
                    ?: return@withContext null
                val passMd5 = passMd5Match.groupValues[1]

                // Extract token
                val token = Regex("""token=([^&'"]+)""").find(html)?.groupValues?.get(1)
                    ?: return@withContext null

                val domain   = embedUrl.split("/").take(3).joinToString("/")
                val passResp = fetchPage("$domain$passMd5", client, referer = doodUrl)
                val baseLink = passResp.trim()
                val timestamp= System.currentTimeMillis()
                val directUrl= "${baseLink}${timestamp}?token=$token"

                StreamResult(
                    directUrl = directUrl,
                    type      = "mp4",
                    quality   = "HD",
                    headers   = mapOf(
                        "Referer"    to "https://dood.watch/",
                        "User-Agent" to MOBILE_UA,
                    ),
                    extractor = "doodstream",
                )
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // Streamtape
    // =====================================================================

    suspend fun extractStreamtape(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchPage(embedUrl, client, referer = "https://streamtape.com/")
                val m    = Regex("""'(//streamtape\.(?:com|to|net)/get_video\?[^']+)'""").find(html)
                    ?: Regex("""id="ideoolink"[^>]*>([^<]+)""").find(html)
                    ?: return@withContext null
                val partial = m.groupValues[1].trim()
                val url = if (partial.startsWith("//")) "https:$partial" else partial
                StreamResult(
                    directUrl = url,
                    type      = "mp4",
                    quality   = "HD",
                    headers   = mapOf("Referer" to "https://streamtape.com/"),
                    extractor = "streamtape",
                )
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // OK.ru
    // =====================================================================

    suspend fun extractOkRu(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchPage(embedUrl, client, referer = "https://ok.ru/")
                val m    = Regex(""""url"\s*:\s*"([^"]+\.m3u8[^"]*)"""", RegexOption.IGNORE_CASE).find(html)
                    ?: Regex(""""url"\s*:\s*"([^"]+\.mp4[^"]*)"""",  RegexOption.IGNORE_CASE).find(html)
                    ?: return@withContext null
                val url  = m.groupValues[1].replace("\\u0026", "&").replace("\\/", "/")
                StreamResult(
                    directUrl = url,
                    type      = if (url.contains(".m3u8")) "hls" else "mp4",
                    extractor = "okru",
                )
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // Dailymotion
    // =====================================================================

    suspend fun extractDailymotion(embedUrl: String, client: OkHttpClient): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val videoId = Regex("""/video/([A-Za-z0-9]+)""").find(embedUrl)?.groupValues?.get(1)
                    ?: return@withContext null
                val apiUrl  = "https://www.dailymotion.com/player/metadata/video/$videoId"
                val json    = fetchPage(apiUrl, client, referer = "https://www.dailymotion.com/")
                val m3u8    = Regex(""""hls_url"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
                    ?: return@withContext null
                StreamResult(
                    directUrl = m3u8.replace("\\", ""),
                    type      = "hls",
                    extractor = "dailymotion",
                )
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // Generic JS/regex fallback
    // =====================================================================

    suspend fun extractGeneric(embedUrl: String, client: OkHttpClient, name: String = "generic"): StreamResult? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchPage(embedUrl, client, referer = embedUrl)
                val patterns = listOf(
                    Regex("""file\s*:\s*["']([^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE),
                    Regex("""file\s*:\s*["']([^"']+\.mp4[^"']*)["']""",  RegexOption.IGNORE_CASE),
                    Regex("""source\s+src=["']([^"']+\.mp4[^"']*)["']""",RegexOption.IGNORE_CASE),
                    Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""",   RegexOption.IGNORE_CASE),
                    Regex("""["'](https?://[^"']+\.mp4[^"']*)["']""",    RegexOption.IGNORE_CASE),
                )
                for (p in patterns) {
                    val m = p.find(html) ?: continue
                    val url  = m.groupValues[1]
                    val type = if (url.contains(".m3u8")) "hls" else "mp4"
                    return@withContext StreamResult(
                        directUrl = url, type = type,
                        extractor = name,
                    )
                }
                null
            } catch (e: Exception) { null }
        }

    // =====================================================================
    // Internal helper
    // =====================================================================

    private fun fetchPage(url: String, client: OkHttpClient, referer: String = ""): String {
        val safeUrl = SeriesCatalogParser.asciiSafeUrl(url)
        val safeReferer = SeriesCatalogParser.asciiSafeHeaderValue(referer)
        val req = Request.Builder()
            .url(safeUrl)
            .header("User-Agent", DEFAULT_UA)
            .apply { if (safeReferer.isNotEmpty()) header("Referer", safeReferer) }
            .build()
        return client.newCall(req).execute().body?.string() ?: ""
    }
}
