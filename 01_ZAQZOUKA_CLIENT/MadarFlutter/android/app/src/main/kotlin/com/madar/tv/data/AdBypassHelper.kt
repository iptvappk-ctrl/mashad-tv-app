package com.madar.tv.data

/**
 * AdBypassHelper.kt
 * ==================
 * محرك تخطي الإعلانات والـ redirects لمواقع البث العربية.
 *
 * يعالج 10 أنماط:
 *  Pattern 1  — Ad Shortener Chains (shot90, fc.lc, ouo.io, …)
 *  Pattern 2  — Meta Refresh Redirect
 *  Pattern 3  — JavaScript window.location Redirect
 *  Pattern 4  — DooPlay WordPress AJAX Server Loading ⭐
 *  Pattern 5  — FaselHD data-embed AJAX
 *  Pattern 6  — WordPress get_server_link AJAX
 *  Pattern 7  — data-link / data-url / data-embed HTML Attributes ⭐
 *  Pattern 8  — Base64-Encoded Embed URLs in JS
 *  Pattern 9  — Cloudflare / Bot Protection (❌ Browser required — marked only)
 *  Pattern 10 — HTTP 301/302 Chain + Tracking Param Stripping
 *
 * راجع: SCRAPERS_ENGINE/docs/AD_BYPASS_DOCUMENTATION.md للتوثيق الكامل
 */

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder

object AdBypassHelper {

    // =====================================================================
    // Constants
    // =====================================================================

    private val AD_SHORTENER_HOSTS = setOf(
        "shot90.com", "shrinkme.io", "adfoc.us", "fc.lc", "bc.vc",
        "za.gl", "ur.ly", "oke.io", "ouo.io", "sh.st", "cpa.st",
        "go2links.net", "earnload.net", "short.pe", "link.tl", "exe.io",
        "up-load.io", "uploadrar.com"
    )

    private val JUNK_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
        "ref", "referrer", "fbclid", "gclid", "msclkid", "ttclid", "_ga"
    )

    private val SKIP_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp", ".css", ".js", ".ico")
    private val SKIP_DOMAINS    = setOf("google-analytics", "doubleclick", "facebook.com/plugins", "disqus")

    // =====================================================================
    // Pattern 1 — Ad Shortener Resolution
    // =====================================================================

    /** Returns true if [url] is served by a known ad shortener domain. */
    fun isAdShortener(url: String): Boolean {
        return try {
            val host = Uri.parse(url).host?.lowercase()?.removePrefix("www.") ?: return false
            host in AD_SHORTENER_HOSTS
        } catch (e: Exception) { false }
    }

    /**
     * Pattern 1a — shot90.com encodes the real URL as base64 in `?url=` param.
     * Example: https://shot90.com/article/xxx?url=aHR0cHM6Ly9xZXNzZXQubmV0Lw==
     * Returns the decoded real URL, or null if not applicable.
     */
    fun decodeShot90Url(shortUrl: String): String? {
        return try {
            val uri = Uri.parse(shortUrl)
            val encoded = uri.getQueryParameter("url") ?: return null
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            bytes.toString(Charsets.UTF_8).trim()
        } catch (e: Exception) { null }
    }

    /**
     * Pattern 1 (generic) + Pattern 10 — Follow redirect chain to real URL,
     * then strip tracking params from the final URL.
     */
    suspend fun resolveRedirectChain(url: String, client: OkHttpClient): String =
        withContext(Dispatchers.IO) {
            try {
                // shot90 fast path — no network needed
                if ("shot90.com" in url) {
                    decodeShot90Url(url)?.let { return@withContext it }
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .header("Referer",    "https://www.google.com/")
                    .build()

                val response = client.newCall(request).execute()
                val finalUrl = response.request.url.toString()
                stripTrackingParams(finalUrl)
            } catch (e: Exception) {
                url
            }
        }

    // =====================================================================
    // Pattern 2 — Meta Refresh Redirect
    // =====================================================================

    /**
     * Parses `<meta http-equiv="refresh" content="5; url=...">` from [html].
     * Returns the redirect URL or null.
     */
    fun extractMetaRefresh(html: String): String? {
        val pattern = Regex(
            """content=["'][0-9]+;\s*url=(https?://[^"'\s>]+)["']""",
            RegexOption.IGNORE_CASE
        )
        return pattern.find(html)?.groupValues?.get(1)
    }

    // =====================================================================
    // Pattern 3 — JS window.location Redirect
    // =====================================================================

    /**
     * Extracts URL from `window.location = "..."` or `location.replace("...")`.
     */
    fun extractJsRedirect(html: String): String? {
        val patterns = listOf(
            Regex("""window\.location(?:\.href)?\s*=\s*["']+(https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""location\.replace\s*\(\s*["']+(https?://[^"']+)["']""",           RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }
    }

    // =====================================================================
    // Pattern 4 — DooPlay WordPress AJAX
    // =====================================================================

    /**
     * Extracts (nonce, postId) from a DooPlay-themed detail page HTML.
     * DooPlay is a popular WordPress theme used by SpaceMov, MovieWatcher,
     * Cima4Up, and dozens of similar sites.
     */
    fun extractDooplayParams(html: String): Pair<String?, String?> {
        val nonce = Regex(""""nonce"\s*:\s*"([a-f0-9]+)"""").find(html)?.groupValues?.get(1)
        val postId = listOf(
            Regex(""""post_id"\s*:\s*"?(\d+)"?"""),
            Regex(""""postid"\s*:\s*"?(\d+)"?"""),
            Regex("""data-post=["'](\d+)["']"""),
        ).firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }
        return Pair(nonce, postId)
    }

    /**
     * POSTs to /wp-admin/admin-ajax.php with DooPlay action to get embed URLs.
     * Returns list of embed iframe src URLs.
     */
    suspend fun fetchDooplayServers(
        baseUrl: String,
        postId:  String,
        nonce:   String,
        client:  OkHttpClient,
    ): List<String> = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        try {
            val body = FormBody.Builder()
                .add("action", "doo_player_ajax")
                .add("post",   postId)
                .add("nonce",  nonce)
                .build()
            val safeBaseUrl = SeriesCatalogParser.asciiSafeHeaderValue(baseUrl)
            val req = Request.Builder()
                .url(SeriesCatalogParser.asciiSafeUrl("${baseUrl.trimEnd('/')}/wp-admin/admin-ajax.php"))
                .post(body)
                .header("X-Requested-With", "XMLHttpRequest")
                .apply { if (safeBaseUrl.isNotEmpty()) header("Referer", safeBaseUrl) }
                .header("User-Agent",        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .build()

            val resp    = client.newCall(req).execute()
            val jsonStr = resp.body?.string() ?: return@withContext results
            val json    = JSONObject(jsonStr)

            val embedHtml = json.optString("embed")
                .ifBlank  { json.optString("link") }
                .ifBlank  { json.optString("html") }

            if (embedHtml.isNotBlank()) {
                Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .findAll(embedHtml)
                    .forEach { results.add(it.groupValues[1]) }
            }

            // Some themes return a "servers" array
            json.optJSONArray("servers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val srv  = arr.optJSONObject(i) ?: continue
                    val link = srv.optString("embed").ifBlank { srv.optString("link") }
                    if (link.startsWith("http")) results.add(link)
                }
            }
        } catch (e: Exception) { /* silent — server may not be DooPlay */ }
        results
    }

    // =====================================================================
    // Pattern 5 — FaselHD-style data-embed AJAX
    // =====================================================================

    /**
     * Fetches embed URL via POST /ajax/embed with the data-embed ID.
     * NOTE: faselhd.top is currently Cloudflare-protected; this will return null.
     */
    suspend fun fetchFaselhdEmbed(
        baseUrl: String,
        embedId: String,
        client:  OkHttpClient,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder().add("id", embedId).build()
            val safeRef = SeriesCatalogParser.asciiSafeHeaderValue(baseUrl)
            val req  = Request.Builder()
                .url(SeriesCatalogParser.asciiSafeUrl("${baseUrl.trimEnd('/')}/ajax/embed"))
                .post(body)
                .header("X-Requested-With", "XMLHttpRequest")
                .apply { if (safeRef.isNotEmpty()) header("Referer", safeRef) }
                .build()
            val resp = client.newCall(req).execute()
            val json = JSONObject(resp.body?.string() ?: return@withContext null)
            json.optString("link").ifBlank { json.optString("url").ifBlank { null } }
        } catch (e: Exception) { null }
    }

    // =====================================================================
    // Pattern 6 — WordPress get_server_link AJAX
    // =====================================================================

    /**
     * Generic WordPress AJAX: action=get_server_link.
     * Used by WeCima, Akwam and similar sites.
     */
    suspend fun fetchWpServerLink(
        baseUrl:  String,
        serverId: String,
        nonce:    String,
        client:   OkHttpClient,
        action:   String = "get_server_link",
    ): String? = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("action", action)
                .add("id",     serverId)
                .add("nonce",  nonce)
                .build()
            val safeRef = SeriesCatalogParser.asciiSafeHeaderValue(baseUrl)
            val req  = Request.Builder()
                .url(SeriesCatalogParser.asciiSafeUrl("${baseUrl.trimEnd('/')}/wp-admin/admin-ajax.php"))
                .post(body)
                .header("X-Requested-With", "XMLHttpRequest")
                .apply { if (safeRef.isNotEmpty()) header("Referer", safeRef) }
                .build()
            val resp    = client.newCall(req).execute()
            val jsonStr = resp.body?.string() ?: return@withContext null
            val json    = JSONObject(jsonStr)
            var link    = json.optString("link").ifBlank { json.optString("url").ifBlank { null } }
            // Unwrap embed HTML if needed
            if (link != null && "<iframe" in link) {
                link = Regex("""src=["']([^"']+)["']""").find(link)?.groupValues?.get(1)
            }
            link
        } catch (e: Exception) { null }
    }

    // =====================================================================
    // Pattern 7 — data-* HTML Attributes
    // =====================================================================

    private val DATA_LINK_ATTRS = listOf(
        "data-link", "data-url", "data-embed", "data-src",
        "data-href", "data-redirect", "data-server",
    )

    /**
     * Scans [html] for all `data-link`, `data-url`, `data-embed`, etc. attributes
     * that contain HTTP URLs (skipping images, CSS, JS assets).
     * Returns list of (attributeName, url) pairs.
     */
    fun extractDataLinks(html: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        for (attr in DATA_LINK_ATTRS) {
            Regex("""$attr=["']([^"']+)["']""").findAll(html).forEach { match ->
                val url = match.groupValues[1].trim()
                if (url.startsWith("http") &&
                    SKIP_EXTENSIONS.none { url.endsWith(it) } &&
                    SKIP_DOMAINS.none    { url.contains(it) }
                ) {
                    results.add(Pair(attr, url))
                }
            }
        }
        return results
    }

    // =====================================================================
    // Pattern 8 — Base64-Encoded URLs in JS
    // =====================================================================

    /**
     * Finds base64 blobs in [html] that decode to HTTP URLs.
     * Common obfuscation technique on Arabic streaming sites.
     */
    fun extractBase64Urls(html: String): List<String> {
        val results = mutableListOf<String>()
        Regex("""["']([A-Za-z0-9+/]{30,}={0,2})["']""").findAll(html).forEach { match ->
            try {
                val blob    = match.groupValues[1]
                val decoded = Base64.decode(blob, Base64.DEFAULT).toString(Charsets.UTF_8).trim()
                if (decoded.startsWith("http") && "." in decoded &&
                    SKIP_EXTENSIONS.none { decoded.endsWith(it) }
                ) {
                    results.add(decoded)
                }
            } catch (e: Exception) { }
        }
        return results
    }

    // =====================================================================
    // Pattern 10 — Strip Tracking Params
    // =====================================================================

    /** Removes known analytics/tracking query parameters from [url]. */
    fun stripTrackingParams(url: String): String {
        return try {
            val uri     = Uri.parse(url)
            val builder = uri.buildUpon().clearQuery()
            uri.queryParameterNames
                .filter { it.lowercase() !in JUNK_PARAMS }
                .forEach { key -> builder.appendQueryParameter(key, uri.getQueryParameter(key)) }
            builder.build().toString()
        } catch (e: Exception) { url }
    }

    // =====================================================================
    // MASTER RESOLVER — combines all patterns
    // =====================================================================

    /**
     * Full ad/redirect bypass resolver for a watch/detail page.
     *
     * Given [pageUrl] and its [html] + [baseUrl], runs all 10 patterns and
     * returns a list of embed/stream URLs ready for [StreamExtractorHelper].
     *
     * Usage in MultiProviderRepository:
     * ```kotlin
     * val html = fetchPage(watchUrl)
     * val embedUrls = AdBypassHelper.resolveServers(watchUrl, html, provider.baseUrl, httpClient)
     * val streams = embedUrls.mapNotNull { embed -> StreamExtractorHelper.autoExtract(embed, httpClient) }
     * ```
     */
    suspend fun resolveServers(
        pageUrl: String,
        html:    String,
        baseUrl: String,
        client:  OkHttpClient,
    ): List<String> = withContext(Dispatchers.IO) {
        val servers = mutableListOf<String>()
        val seen    = mutableSetOf<String>()

        fun shouldSkip(url: String) =
            !url.startsWith("http") ||
            SKIP_EXTENSIONS.any { url.endsWith(it) } ||
            SKIP_DOMAINS.any    { url.contains(it) }

        suspend fun add(url: String) {
            if (url.isBlank() || seen.contains(url) || shouldSkip(url)) return
            val resolved = if (isAdShortener(url)) resolveRedirectChain(url, client) else url
            if (seen.add(resolved) && !shouldSkip(resolved)) {
                servers.add(resolved)
            }
        }

        // Pattern 2: meta refresh
        extractMetaRefresh(html)?.let { add(it) }

        // Pattern 3: JS location redirect
        extractJsRedirect(html)?.let { add(it) }

        // Pattern 7: data-* attributes (fastest — no network)
        extractDataLinks(html).forEach { (_, url) -> add(url) }

        // Pattern 4: DooPlay AJAX
        val (nonce, postId) = extractDooplayParams(html)
        if (nonce != null && postId != null) {
            fetchDooplayServers(baseUrl, postId, nonce, client).forEach { add(it) }
        }

        // Pattern 8: base64 blobs
        extractBase64Urls(html).forEach { add(it) }

        // Standard: iframe src / data-src
        Regex("""<iframe[^>]+(?:src|data-src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .forEach { add(it.groupValues[1]) }

        servers
    }

    // =====================================================================
    // Cloudflare detection (Pattern 9) — informational only
    // =====================================================================

    /**
     * Returns true if [html] looks like a Cloudflare bot challenge page.
     * When true, the page cannot be scraped with OkHttp — a real browser is needed.
     * Sites affected: faselhd.top, shahid4u.cam
     */
    fun isCloudflareChallenge(html: String): Boolean {
        return html.contains("cf-browser-verification", ignoreCase = true) ||
               html.contains("challenge-platform",      ignoreCase = true) ||
               html.contains("__cf_chl_",               ignoreCase = true) ||
               (html.length < 5000 && html.contains("cloudflare", ignoreCase = true))
    }
}
