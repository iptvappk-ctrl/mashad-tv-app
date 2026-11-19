package com.madar.tv.data



import com.madar.tv.core.X

import okhttp3.ConnectionPool

import okhttp3.Cookie

import okhttp3.CookieJar

import okhttp3.HttpUrl

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient

import okhttp3.Request

import org.jsoup.Jsoup

import java.io.IOException

import java.net.URLEncoder

import java.util.concurrent.ConcurrentHashMap

import java.util.concurrent.TimeUnit

import java.util.regex.Pattern



object ArabseedRepository {

    private const val TAG = "ArabseedRepository"



    // ═══════════════════════════════════════════════════════════════════════

    // ALL CONSTANTS — 6-layer encrypted via X.s/X.j (v6 engine)

    // Pipeline: XOR(K1)→ROT-L(3)→XOR(K2)→Caesar(+17)→Nibble-Swap→PosXOR(K3)

    // No plain-text string literals exist in this file.

    // ═══════════════════════════════════════════════════════════════════════



    // S1, S2, S3

    private val _s1 = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte())

    private val _s2 = byteArrayOf(135.toByte(), 21, 145.toByte(), 43, 168.toByte(), 184.toByte(), 191.toByte(), 54)

    private val _s3 = byteArrayOf(38, 26, 147.toByte(), 168.toByte(), 37, 48, 57)



    // S4

    private val _s4 = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 207.toByte(), 77, 217.toByte(), 83, 224.toByte(), 224.toByte(), 231.toByte(), 126, 86, 138.toByte(), 3, 24, 149.toByte(), 160.toByte(), 169.toByte(), 5)



    // G1, G2

    private val _g1 = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte())

    private val _g2 = byteArrayOf(138.toByte(), 159.toByte(), 20, 172.toByte(), 33, 27, 59, 176.toByte(), 74, 196.toByte())



    // GS

    private val _gs = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 194.toByte(), 199.toByte(), 92, 212.toByte(), 105, 67, 99, 248.toByte(), 114, 12, 32)



    // GP

    private val _gp = byteArrayOf(138.toByte(), 159.toByte(), 20, 172.toByte(), 33, 27, 59, 176.toByte(), 74, 196.toByte(), 248.toByte(), 88, 109, 226.toByte(), 233.toByte(), 221.toByte())



    // Pattern 1

    private val _mr = byteArrayOf(132.toByte(), 159.toByte(), 16, 163.toByte(), 41, 68, 182.toByte(), 147.toByte(), 185.toByte(), 199.toByte(), 93, 212.toByte(), 103, 28, 238.toByte(), 91, 217.toByte(), 240.toByte(), 10, 191.toByte(), 108, 7, 142.toByte(), 90, 155.toByte(), 73, 225.toByte(), 102, 119, 82, 115, 225.toByte(), 23, 88, 140.toByte(), 246.toByte(), 164.toByte(), 190.toByte(), 226.toByte(), 185.toByte(), 12, 216.toByte())



    // Pattern 2

    private val _ir = byteArrayOf(131.toByte(), 19, 26, 168.toByte(), 165.toByte(), 184.toByte(), 194.toByte(), 77, 102, 184.toByte(), 254.toByte(), 219.toByte(), 96, 225.toByte(), 203.toByte(), 131.toByte(), 218.toByte(), 173.toByte(), 127, 188.toByte(), 108, 211.toByte(), 0, 135.toByte(), 65, 17, 109, 63, 118, 241.toByte(), 43)



    // Pattern 3

    private val _ig = byteArrayOf(131.toByte(), 19, 26, 168.toByte(), 165.toByte(), 184.toByte(), 194.toByte(), 191.toByte(), 68, 197.toByte(), 126, 254.toByte(), 198.toByte(), 239.toByte(), 228.toByte(), 121, 243.toByte(), 140.toByte(), 160.toByte(), 158.toByte(), 23, 175.toByte(), 47, 5, 61, 23, 100, 62)



    // STORY_SEL = ".post__story"

    private val _ps = byteArrayOf(38, 22, 144.toByte(), 163.toByte(), 41, 195.toByte(), 196.toByte(), 191.toByte(), 69, 199.toByte(), 82, 220.toByte())

    private val _storyFallbacks = listOf(

        byteArrayOf(38, 22, 144.toByte(), 163.toByte(), 41, 195.toByte(), 196.toByte(), 191.toByte(), 69, 199.toByte(), 82, 220.toByte()), // .post__story

        byteArrayOf(38, 149.toByte(), 27, 169.toByte(), 40, 182.toByte()), // .story

        byteArrayOf(38, 229.toByte(), 27, 169.toByte(), 40, 182.toByte()), // .Story

        byteArrayOf(38, 156.toByte(), 16, 34, 40, 182.toByte(), 196.toByte(), 205.toByte(), 77, 196.toByte(), 210.toByte(), 211.toByte()), // .entry__desc

        byteArrayOf(5, 154.toByte(), 20, 9, 33, 184.toByte(), 182.toByte(), 183.toByte()), // div.desc

    )



    // DLINK_ATTR = "data-link"

    private val _da = byteArrayOf(5, 158.toByte(), 27, 168.toByte(), 133.toByte(), 60, 185.toByte(), 53, 200.toByte())



    // DLINK_SEL = "[data-link]"

    private val _dl = byteArrayOf(248.toByte(), 28, 145.toByte(), 34, 163.toByte(), 156.toByte(), 59, 176.toByte(), 78, 193.toByte(), 167.toByte())



    // DATA_URL_SEL = "[data-url]"

    private val _du = byteArrayOf(248.toByte(), 28, 145.toByte(), 34, 163.toByte(), 156.toByte(), 183.toByte(), 63, 73, 184.toByte())



    // DATA_EMBED_SEL = "[data-embed]"

    private val _de = byteArrayOf(248.toByte(), 28, 145.toByte(), 34, 163.toByte(), 156.toByte(), 191.toByte(), 178.toByte(), 76, 196.toByte(), 91, 174.toByte())



    // DATA_SRC_SEL = "[data-src]"

    private val _dss = byteArrayOf(248.toByte(), 28, 145.toByte(), 34, 163.toByte(), 156.toByte(), 182.toByte(), 63, 204.toByte(), 184.toByte())



    // IFRAME_SEL = "iframe"

    private val _ifs = byteArrayOf(131.toByte(), 19, 26, 168.toByte(), 165.toByte(), 184.toByte())



    // WATCH_SUFFIX = "/watch"

    private val _ws = byteArrayOf(166.toByte(), 155.toByte(), 145.toByte(), 34, 160.toByte(), 62)



    // ABS_HREF = "abs:href"

    private val _ah = byteArrayOf(135.toByte(), 29, 154.toByte(), 7, 39, 49, 191.toByte(), 57)



    // ABS_SRC = "abs:src"

    private val _as = byteArrayOf(135.toByte(), 29, 154.toByte(), 7, 168.toByte(), 49, 190.toByte())



    // DATA_SRC_RAW = "data-src"

    private val _dsr = byteArrayOf(5, 158.toByte(), 27, 168.toByte(), 133.toByte(), 177.toByte(), 54, 183.toByte())



    // DATA_URL_RAW = "data-url"

    private val _dur = byteArrayOf(5, 158.toByte(), 27, 168.toByte(), 133.toByte(), 176.toByte(), 54, 50)



    // DATA_EMBED_RAW = "data-embed"

    private val _der = byteArrayOf(5, 158.toByte(), 27, 168.toByte(), 133.toByte(), 184.toByte(), 187.toByte(), 55, 205.toByte(), 68)



    // DATA_LAZY_SRC = "data-lazy-src"

    private val _dls = byteArrayOf(5, 158.toByte(), 27, 168.toByte(), 133.toByte(), 60, 189.toByte(), 59, 195.toByte(), 224.toByte(), 210.toByte(), 91, 232.toByte())



    // IFRAME_SRC_RAW = "src"

    private val _isr = byteArrayOf(140.toByte(), 21, 146.toByte())



    // REFERER_HDR = "Referer"

    private val _rh = byteArrayOf(124, 156.toByte(), 28, 170.toByte(), 40, 184.toByte(), 54)



    // CONNECTION_HDR = "Connection"

    private val _ch = byteArrayOf(244.toByte(), 159.toByte(), 16, 41, 161.toByte(), 185.toByte(), 55, 176.toByte(), 206.toByte(), 71)



    // KEEP_ALIVE = "keep-alive"

    private val _ka = byteArrayOf(128.toByte(), 156.toByte(), 147.toByte(), 32, 133.toByte(), 186.toByte(), 59, 176.toByte(), 74, 196.toByte())



    // H4

    private val _alh = byteArrayOf(247.toByte(), 157.toByte(), 146.toByte(), 170.toByte(), 43, 48, 155.toByte(), 66, 207.toByte(), 71, 212.toByte(), 218.toByte(), 235.toByte(), 239.toByte(), 231.toByte())



    // V4

    private val _alv = byteArrayOf(135.toByte(), 21, 55, 170.toByte(), 34, 156.toByte(), 199.toByte(), 207.toByte(), 224.toByte(), 206.toByte(), 247.toByte(), 120, 74, 206.toByte(), 67, 254.toByte(), 118, 33, 9, 54, 187.toByte(), 139.toByte(), 129.toByte())



    // V5

    private val _ahv = byteArrayOf(13, 156.toByte(), 29, 34, 130.toByte(), 62, 55, 178.toByte(), 73, 96, 217.toByte(), 88, 99, 100, 225.toByte(), 255.toByte(), 247.toByte(), 132.toByte(), 5, 25, 146.toByte(), 11, 161.toByte(), 160.toByte(), 181.toByte(), 48, 207.toByte(), 103, 215.toByte(), 84, 211.toByte(), 202.toByte(), 103, 246.toByte(), 249.toByte(), 14, 135.toByte(), 153.toByte(), 157.toByte(), 30, 171.toByte(), 167.toByte(), 56, 145.toByte(), 71, 196.toByte(), 67, 243.toByte(), 223.toByte(), 192.toByte(), 73, 89, 223.toByte(), 172.toByte(), 9, 2, 31, 27, 43, 1, 59, 183.toByte(), 49, 193.toByte(), 225.toByte(), 90, 87, 104, 110, 120, 92, 113, 141.toByte(), 5, 17, 54, 12, 131.toByte(), 2, 147.toByte(), 191.toByte(), 224.toByte(), 105, 121, 127)



    // UA

    private val _ua = byteArrayOf(241.toByte(), 159.toByte(), 30, 172.toByte(), 37, 60, 189.toByte(), 149.toByte(), 229.toByte(), 103, 113, 112, 79, 151.toByte(), 225.toByte(), 125, 117, 15, 4, 19, 179.toByte(), 219.toByte(), 215.toByte(), 132.toByte(), 23, 158.toByte(), 232.toByte(), 232.toByte(), 116, 242.toByte(), 32, 104, 230.toByte(), 219.toByte(), 219.toByte(), 167.toByte(), 35, 22, 56, 62, 139.toByte(), 6, 201.toByte(), 56, 67, 68, 199.toByte(), 169.toByte(), 213.toByte(), 109, 150.toByte(), 252.toByte(), 121, 43, 39, 47, 58, 183.toByte(), 2, 133.toByte(), 155.toByte(), 150.toByte(), 66, 184.toByte(), 189.toByte(), 40, 167.toByte(), 206.toByte(), 195.toByte(), 252.toByte(), 121, 115, 141.toByte(), 38, 228.toByte(), 146.toByte(), 168.toByte(), 165.toByte(), 164.toByte(), 152.toByte(), 23, 189.toByte(), 69, 83, 210.toByte(), 236.toByte(), 239.toByte(), 197.toByte(), 215.toByte(), 221.toByte(), 33, 161.toByte(), 179.toByte(), 179.toByte(), 189.toByte(), 141.toByte(), 143.toByte(), 158.toByte(), 74, 72, 206.toByte(), 90, 214.toByte(), 80, 78, 76, 82, 85, 42, 169.toByte(), 32)



    private val _p_http = byteArrayOf(3, 20, 27, 32)



    // ── Decoys ────────────────────────────────────────────────────────────

    @Suppress("unused") private val _dc1 = byteArrayOf(42, 201.toByte(), 77, 13, 58, 190.toByte(), 211.toByte(), 7, 34, 121, 93)

    @Suppress("unused") private val _dc2 = byteArrayOf(19, 243.toByte(), 107, 91, 99, 235.toByte(), 161.toByte(), 73)

    @Suppress("unused") private val _dc3 = byteArrayOf(139.toByte(), 3, 27, 115, 65, 51, 127, 9, 73, 211.toByte())

    @Suppress("unused") private val _dc4 = byteArrayOf(91, 211.toByte(), 99, 123, 19, 65, 73, 59, 51, 115, 27)

    @Suppress("unused") private val _dc5 = byteArrayOf(172.toByte(), 91, 43, 123, 51, 27, 3, 73, 188.toByte())

    @Suppress("unused") private val _dc6 = byteArrayOf(235.toByte(), 123, 131.toByte(), 91, 59, 27, 115, 107)



    // ── Lazily resolved secrets ───────────────────────────────────────────

    private val BASE_URL:     String = "https://m.myseed.tv"

    private val BASE_SLASH:   String = "https://m.myseed.tv/"

    private val GOVID_URL:    String by lazy { X.j(_g1, _g2) }

    private val GOVID_SLASH:  String by lazy { X.s(_gs) }

    private val GOVID_PLAY:   String by lazy { X.s(_gp) }

    private val MOHIX_RE:     String by lazy { X.s(_mr) }

    private val IFRAME_RE:    String by lazy { X.s(_ir) }

    private val IFRAME_GOVID: String by lazy { X.s(_ig) }

    private val STORY_SEL:    String by lazy { X.s(_ps) }

    private val DLINK_ATTR:   String by lazy { X.s(_da) }

    private val SEL_DLINK:    String by lazy { X.s(_dl) }

    private val SEL_DURL:     String by lazy { X.s(_du) }

    private val SEL_DEMBED:   String by lazy { X.s(_de) }

    private val SEL_DSRC:     String by lazy { X.s(_dss) }

    private val SEL_IFRAME:   String by lazy { X.s(_ifs) }

    private val WATCH_SFX:    String by lazy { X.s(_ws) }

    private val A_HREF:       String by lazy { X.s(_ah) }

    private val A_SRC:        String by lazy { X.s(_as) }

    private val R_DSRC:       String by lazy { X.s(_dsr) }

    private val R_DURL:       String by lazy { X.s(_dur) }

    private val R_DEMBED:     String by lazy { X.s(_der) }

    private val R_DLAZY:      String by lazy { X.s(_dls) }

    private val R_ISRC:       String by lazy { X.s(_isr) }

    private val HDR_REF:      String by lazy { X.s(_rh) }

    private val HDR_CONN:     String by lazy { X.s(_ch) }

    private val VAL_KALIVE:   String by lazy { X.s(_ka) }

    private val HDR_ALANG:    String by lazy { X.s(_alh) }

    private val VAL_ALANG:    String by lazy { X.s(_alv) }

    private val VAL_AHTML:    String by lazy { X.s(_ahv) }

    private val UA:           String by lazy { X.s(_ua) }

    private val P_HTTP:       String by lazy { X.s(_p_http) }



    // ── Networking ────────────────────────────────────────────────────────



    private val cookieJar = object : CookieJar {

        private val store = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {

            store.getOrPut(url.host) { mutableListOf() }.apply {

                removeAll { existing -> cookies.any { it.name == existing.name } }

                addAll(cookies)

            }

        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =

            store[url.host] ?: emptyList()

    }



    private val connectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)



    private val client = OkHttpClient.Builder()

        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .connectionPool(connectionPool)
        .cookieJar(cookieJar)

        .connectTimeout(6, TimeUnit.SECONDS)

        .readTimeout(8, TimeUnit.SECONDS)
.callTimeout(12, TimeUnit.SECONDS)

        .followRedirects(true)

        .followSslRedirects(true)

        .retryOnConnectionFailure(true)

        .build()



    // ── Caches ────────────────────────────────────────────────────────────

    private val detailCache = ConcurrentHashMap<String, ArabseedDetail>()
    private val detailCacheAt = ConcurrentHashMap<String, Long>()
    private val prefetching = ConcurrentHashMap.newKeySet<String>()
    private val prefetchPool = java.util.concurrent.Executors.newFixedThreadPool(2)
    private const val DETAIL_CACHE_TTL = 30 * 60 * 1000L

    private val streamCache = ConcurrentHashMap<String, String>()

    private val listingCache = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()
    private val searchCache = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()
    private const val SEARCH_CACHE_TTL = 8 * 60 * 1000L

    private const val LISTING_CACHE_TTL = 3 * 60 * 1000L // 3 minutes



    fun loadCachedCategory(categoryPath: String, page: Int = 1): List<ArabseedItem>? {
        val rawClean = categoryPath.trim().trimStart('/')
        val cleanPath = when (rawClean) {
            "home/", "home", "" -> "main/"
            else -> rawClean
        }
        val key = "$cleanPath:$page"
        listingCache[key]?.let { cached ->
            if (System.currentTimeMillis() - cached.first <= 14L * 24 * 60 * 60 * 1000) return cached.second
        }
        val cached = CatalogDiskCache.loadItems("arabseed:list:$key") ?: return null
        listingCache[key] = cached.savedAt to cached.value
        return cached.value
    }

    fun loadCachedSearch(query: String): List<ArabseedItem>? {
        val key = query.trim().lowercase()
        if (key.isBlank()) return null
        searchCache[key]?.let { cached ->
            if (System.currentTimeMillis() - cached.first <= 14L * 24 * 60 * 60 * 1000) return cached.second
        }
        val cached = CatalogDiskCache.loadItems("arabseed:search:$key") ?: return null
        searchCache[key] = cached.savedAt to cached.value
        return cached.value
    }

    fun loadCachedDetail(pageUrl: String): ArabseedDetail? {
        detailCache[pageUrl]?.let { if (it.servers.isNotEmpty()) return it }
        val cached = CatalogDiskCache.loadDetail("arabseed:detail:" + pageUrl) ?: return null
        if (cached.value.servers.isEmpty()) return null
        detailCache[pageUrl] = cached.value
        detailCacheAt[pageUrl] = cached.savedAt
        return cached.value
    }

    fun loadCachedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        seasonBundleCache[pageUrl]?.let { return it }
        val cached = CatalogDiskCache.loadSeasonBundle("arabseed:seasons:v2:" + pageUrl) ?: return null
        val normalized = SeriesCatalogParser.normalizeSeasonBundle(cached.value)
        seasonBundleCache[pageUrl] = normalized
        seasonBundleCacheAt[pageUrl] = cached.savedAt
        return normalized
    }

    fun clearListingCache() {

        listingCache.clear()

    }



    fun getEmbedHeaders(): Map<String, String> = mapOf(HDR_REF to BASE_SLASH)

    fun getPlayerHeaders(): Map<String, String> = mapOf(HDR_REF to GOVID_SLASH)

    fun getPlayerUserAgent(): String = UA

    fun isAd(url: String): Boolean = isAdUrl(url)



    fun getCachedDetail(pageUrl: String): ArabseedDetail? = detailCache[pageUrl]



    fun prefetch(item: ArabseedItem) {
        if (detailCache.containsKey(item.pageUrl) || !prefetching.add(item.pageUrl)) return
        prefetchPool.execute {
            try { fetchDetail(item) } catch (_: Exception) {}
            finally { prefetching.remove(item.pageUrl) }
        }

    }



    // ── Public API ────────────────────────────────────────────────────────



    fun fetchCategory(categoryPath: String, page: Int = 1, forceRefresh: Boolean = false): List<ArabseedItem> {

        val rawClean = categoryPath.trim().trimStart('/')
        val cleanPath = when (rawClean) {
            "home/", "home", "" -> "main/"
            else -> rawClean
        }

        val cacheKey = "$cleanPath:$page"

        val now = System.currentTimeMillis()

        if (!forceRefresh) {

            listingCache[cacheKey]?.let { (timestamp, cachedList) ->

                if (now - timestamp < LISTING_CACHE_TTL && cachedList.isNotEmpty()) {

                    return cachedList

                }

            }

        }

        val url = if (page <= 1) {

            "$BASE_URL/$cleanPath"

        } else {

            val base = if (cleanPath.endsWith("/")) cleanPath else "$cleanPath/"

            "$BASE_URL/${base}page/$page/"

        }

        val html = fetchHtml(url)

        val defaultType = if (cleanPath.contains("مسلسل") || cleanPath.contains("series")) {

            ArabseedType.SERIES

        } else {

            ArabseedType.MOVIE

        }

        // Normalize the primary provider before exposing a category page. The
        // home screen can render this result before secondary providers finish.
        val parsedItems = parseListing(html, defaultType)
        if (html.isNotBlank() && parsedItems.isEmpty()) {
            android.util.Log.w(TAG, "No catalog cards parsed for category=$cleanPath page=$page")
        }
        var items = MultiProviderRepository.mergeCatalogItems(parsedItems)

        // Fallback: if home/main returned 0 items, retry with main/ directly
        if (items.isEmpty() && cleanPath != "main/") {
            try {
                val fallbackHtml = fetchHtml("$BASE_URL/main/")
                items = MultiProviderRepository.mergeCatalogItems(parseListing(fallbackHtml, defaultType))
            } catch (_: Exception) {}
        }

        if (items.isNotEmpty()) {

            listingCache[cacheKey] = Pair(now, items)

        }

            CatalogDiskCache.saveItems("arabseed:list:$cacheKey", items)
        return items

    }



    fun fetchMovies(page: Int = 1): List<ArabseedItem> =

        fetchCategory("movies/", page)



    fun fetchSeries(page: Int = 1): List<ArabseedItem> =

        fetchCategory("series/", page)



    fun search(query: String, forceRefresh: Boolean = false): List<ArabseedItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            searchCache[normalized]?.let { (savedAt, result) ->
                if (now - savedAt < SEARCH_CACHE_TTL && result.isNotEmpty()) return result
            }
            loadCachedSearch(normalized)?.let { cached ->
                val savedAt = searchCache[normalized]?.first ?: 0L
                if (now - savedAt < SEARCH_CACHE_TTL) return cached
            }
        }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$BASE_URL/?s=$encoded"
        val results = parseListing(fetchHtml(url), null)
        if (results.isNotEmpty()) {
            searchCache[normalized] = now to results
            CatalogDiskCache.saveItems("arabseed:search:$normalized", results)
        }
        return results
    }


    fun fetchStoryFast(pageUrl: String): String =

        try { parseOverview(fetchHtml(pageUrl)) } catch (_: Exception) { "" }



        fun fetchServersFast(pageUrl: String): List<ArabseedServer> {
        val cleanUrl = pageUrl.trimEnd('/')
        val watchUrl = if (cleanUrl.endsWith("/watch")) "$cleanUrl/" else "$cleanUrl/watch/"
        var servers = emptyList<ArabseedServer>()
        try {
            val watchHtml = fetchHtml(watchUrl)
            servers = parseServersFast(watchHtml)

            // Resolve MySeed direct MP4 via vids.php -> player-gateway.php
            try {
                val vidsServer = servers.firstOrNull { it.embedUrl.contains("vids.php") }
                val vidsSrc = vidsServer?.embedUrl ?: run {
                    val wDoc = Jsoup.parse(watchHtml, BASE_URL)
                    wDoc.selectFirst("iframe[src*='vids.php'], [data-link*='vids.php']")?.let {
                        val s = it.attr("abs:src").ifBlank { it.attr("src").ifBlank { it.attr("data-link") } }.trim()
                        if (s.startsWith("/")) "$BASE_URL$s" else s
                    }
                }
                if (!vidsSrc.isNullOrBlank()) {
                    val fullVids = if (vidsSrc.startsWith("/")) "$BASE_URL$vidsSrc" else vidsSrc
                    val vHtml = fetchHtmlWithReferer(fullVids, "$BASE_URL/")
                    val gwMatch = Regex("""src=["'](https?://[^"']*player-gateway\.php[^"']*)["']""").find(vHtml)
                    val gwUrl = gwMatch?.groupValues?.get(1)?.replace("&amp;", "&")
                    if (!gwUrl.isNullOrBlank()) {
                        val gwHtml = fetchHtmlWithReferer(gwUrl, "$BASE_URL/")
                        val directMatch = Regex("""<source\s+[^>]*src=["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE).find(gwHtml)
                        val directStream = directMatch?.groupValues?.get(1)
                        if (!directStream.isNullOrBlank()) {
                            val topServer = ArabseedServer("سيرفر ماي سيد (فائق السرعة ⚡)", fullVids, directStreamUrl = directStream)
                            servers = listOf(topServer) + servers.filterNot { it.embedUrl.contains("vids.php") }
                        }
                    }
                }
            } catch (_: Exception) {}

            if (servers.isEmpty()) servers = parseServersFast(fetchHtml(cleanUrl))
        } catch (_: Exception) {
            try { servers = parseServersFast(fetchHtml(cleanUrl)) } catch (_: Exception) {}
        }
        // Fallback: search if no servers found
        if (servers.isEmpty()) {
            try { servers = fetchServersViaSearch(pageUrl) } catch (_: Exception) {}
        }
        return servers
    }

    /**
     * لما الـ URL القديمة تعطي 404، يبحث تلقائياً بالعنوان المستخرج من الـ URL
     * ويجيب السيرفرات من أول نتيجة مطابقة.
     */
    private fun fetchServersViaSearch(originalUrl: String): List<ArabseedServer> {
        val slug = originalUrl.trimEnd('/').substringAfterLast('/')
        val titleFromSlug = slug
            .replace(Regex("""-s\d+-eps?\d+.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(مسلسل|فيلم|movie|series)-""", RegexOption.IGNORE_CASE), "")
            .replace("-", " ").trim()
        if (titleFromSlug.length < 2) return emptyList()
        val searchResults = try { search(titleFromSlug) } catch (_: Exception) { emptyList() }
        if (searchResults.isEmpty()) return emptyList()
        val epNum = Regex("""-eps?(\d+)""", RegexOption.IGNORE_CASE).find(originalUrl)?.groupValues?.get(1)?.toIntOrNull()
        val sNum  = Regex("""-s(\d+)-""",  RegexOption.IGNORE_CASE).find(originalUrl)?.groupValues?.get(1)?.toIntOrNull()
        if (epNum != null) {
            for (result in searchResults.take(3)) {
                try {
                    val bundle = fetchSeasonsAndEpisodes(result)
                    val targetSeason = if (sNum != null)
                        bundle.seasons.firstOrNull { it.number == sNum } ?: bundle.seasons.firstOrNull()
                    else bundle.seasons.firstOrNull()
                    val episode = targetSeason?.episodes?.firstOrNull { it.number == epNum }
                    if (episode != null) {
                        val epServers = try {
                            val w = episode.pageUrl.trimEnd('/') + "/watch/"
                            parseServersFast(fetchHtml(w)).ifEmpty { parseServersFast(fetchHtml(episode.pageUrl)) }
                        } catch (_: Exception) { emptyList() }
                        if (epServers.isNotEmpty()) return epServers
                    }
                } catch (_: Exception) {}
            }
        }
        for (result in searchResults.take(3)) {
            val s = try {
                val w = result.pageUrl.trimEnd('/') + "/watch/"
                parseServersFast(fetchHtml(w)).ifEmpty { parseServersFast(fetchHtml(result.pageUrl)) }
            } catch (_: Exception) { emptyList() }
            if (s.isNotEmpty()) return s
        }
        return emptyList()
    }





    fun fetchDetail(item: ArabseedItem): ArabseedDetail {
        val now = System.currentTimeMillis()
        detailCache[item.pageUrl]?.let { cached ->
            if (cached.servers.isNotEmpty() && now - (detailCacheAt[item.pageUrl] ?: now) < DETAIL_CACHE_TTL) {
                return cached
            }
        }

        var fullOverview = ""
        var servers = emptyList<ArabseedServer>()
        var preResolved: String? = null

        try {
            fullOverview = fetchStoryFast(item.pageUrl)
        } catch (_: Exception) {}

        try {
            servers = fetchServersFast(item.pageUrl)
            if (servers.isNotEmpty()) {
                try {
                    preResolved = resolveDirectStream(servers.first().embedUrl)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        val detail = ArabseedDetail(
            item = item.copy(overview = fullOverview.ifBlank { item.overview }),
            servers = servers,
            preResolvedStream = preResolved,
        )

        if (servers.isNotEmpty()) {
            detailCache[item.pageUrl] = detail
            detailCacheAt[item.pageUrl] = System.currentTimeMillis()
            CatalogDiskCache.saveDetail("arabseed:detail:" + item.pageUrl, detail)
        }

        return detail
    }



    // ── Seasons & Episodes ────────────────────────────────────────────────



    private val seasonBundleCache = ConcurrentHashMap<String, ArabseedSeasonBundle>()
    private val seasonBundleCacheAt = ConcurrentHashMap<String, Long>()
    private const val SEASON_CACHE_TTL = 6 * 60 * 60 * 1000L



        fun fetchSeasonsAndEpisodes(item: ArabseedItem): ArabseedSeasonBundle {

            val now = System.currentTimeMillis()
            seasonBundleCache[item.pageUrl]?.let { cached ->
                if (now - (seasonBundleCacheAt[item.pageUrl] ?: now) < SEASON_CACHE_TTL) return cached
            }

            return try {

                val html = fetchHtml(item.pageUrl)

                val doc = Jsoup.parse(html, BASE_URL)

                doc.select("footer, script, style").remove()

    

                val rawEpisodes = mutableListOf<Triple<String, String, Int?>>()
                val episodeSelectors = listOf(
                    "a.episode__item", ".episode__item a",
                    "a[href*='%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9']",
                    "a[href*='الحلقة']", "a[href*='-episode-']", "a[href*='/episode/']",
                    "a[href*='-eps']",  // MySeed: /مسلسل-حب-اسود-s1-eps20/
                    ".Episodes--List a", ".EpisodeBlock a",
                    ".episodes-list a", ".EpisodesList li a", ".eplister li a",
                    ".eps-list li a", ".eps a", "a.ep-item", ".episodios li a", ".eplist a"
                )

                fun collectEpisodes(d: org.jsoup.nodes.Document): List<Triple<String, String, Int?>> {
                    var best = mutableListOf<Triple<String, String, Int?>>()
                    for (sel in episodeSelectors) {
                        val els = d.select(sel)
                        if (els.isNotEmpty()) {
                            val current = mutableListOf<Triple<String, String, Int?>>()
                            for (el in els) {
                                val href = el.attr("abs:href").ifBlank { el.attr("href") }
                                val txt = el.text().trim().ifBlank { el.attr("title").trim() }
                                if (href.isBlank() || !href.startsWith(P_HTTP) || href.endsWith("/home/") || href == BASE_URL) continue
                                if (href.contains("/category/") || href.contains("/tag/") || href.contains("/genre/") || href.contains("/actor/")) continue

                                val hasEpisodeListParent = el.parents().any { ancestor ->
                                    val marker = "${ancestor.className()} ${ancestor.id()}".lowercase()
                                    listOf("episode", "episodes", "eplist", "eps-list", "episodeslist", "eps", "episodios")
                                        .any(marker::contains)
                                } || el.classNames().any { className ->
                                    className.contains("episode", ignoreCase = true)
                                        || className.contains("ep-item", ignoreCase = true)
                                }
                                val episodeNumber = SeriesCatalogParser.episodeNumber(
                                    title = txt,
                                    url = href,
                                    allowNumericTitle = hasEpisodeListParent,
                                )
                                val seasonNumber = SeriesCatalogParser.seasonNumberFromElement(el, txt, href)

                                if (episodeNumber != null) {
                                    val currentIndex = current.indexOfFirst {
                                        SeriesCatalogParser.normalizedUrl(it.second) == SeriesCatalogParser.normalizedUrl(href)
                                    }
                                    if (currentIndex < 0) {
                                        current += Triple(txt, href, seasonNumber)
                                    } else if (current[currentIndex].third == null && seasonNumber != null) {
                                        current[currentIndex] = Triple(txt, href, seasonNumber)
                                    }
                                }
                            }
                            if (current.size > best.size ||
                                (current.size == best.size && current.count { it.third != null } > best.count { it.third != null })
                            ) {
                                best = current
                            }
                        }
                    }
                    return best
                }

                val docEps = collectEpisodes(doc)
                for (ep in docEps) {
                    val existingIndex = rawEpisodes.indexOfFirst {
                        SeriesCatalogParser.normalizedUrl(it.second) == SeriesCatalogParser.normalizedUrl(ep.second)
                    }
                    if (existingIndex < 0) {
                        rawEpisodes += ep
                    } else if (rawEpisodes[existingIndex].third == null && ep.third != null) {
                        rawEpisodes[existingIndex] = ep
                    }
                }

                val currentPage = SeriesCatalogParser.normalizedUrl(item.pageUrl)
                val isEpisodePage = SeriesCatalogParser.isEpisode(item.title, item.pageUrl)
                    || SeriesCatalogParser.isEpisode(item.latestEpisodeLabel, item.pageUrl)

                // Episode cards may not have a canonical series URL. Resolve their breadcrumb
                // parent even when the episode page exposes several links: those links can span
                // multiple seasons and must not be treated as one season's episode list.
                if (rawEpisodes.size <= 4 || isEpisodePage) {
                    val seriesLinkSelectors = listOf(
                        "a[href*='/selary/']",  // MySeed parent-series path
                        "a[href*='/series/']", "a[href*='/season/']", "a[href*='الموسم']",
                        ".breadcrumb a", "ol.breadcrumb a", ".SeriesTitle a", ".series-title a",
                        ".entry-breadcrumb a", "a.SeriesLink"
                    )
                    val parents = seriesLinkSelectors.flatMap { selector ->
                        doc.select(selector).mapNotNull { el ->
                            val href = el.attr("abs:href").ifBlank { el.attr("href") }
                            val txt = el.text().trim()
                            val valid = href.isNotBlank() && href.startsWith(P_HTTP) && href != item.pageUrl
                                && !txt.contains("الرئيسية") && !txt.contains("home", ignoreCase = true)
                                && !SeriesCatalogParser.isEpisode(txt, href)
                            if (valid) href to txt else null
                        }
                    }.distinctBy { SeriesCatalogParser.normalizedUrl(it.first) }.take(8)

                    for ((parentHref, _) in parents) {
                        try {
                            val parentDoc = Jsoup.parse(fetchHtml(parentHref), BASE_URL)
                            parentDoc.select("footer, script, style").remove()
                            val parentEps = collectEpisodes(parentDoc)
                            val parentContainsCurrentEpisode = parentEps.any {
                                SeriesCatalogParser.normalizedUrl(it.second) == currentPage
                            }
                            val shouldUseParent = parentEps.isNotEmpty() && (
                                if (isEpisodePage) parentContainsCurrentEpisode
                                else parentEps.size > rawEpisodes.size
                            )
                            if (shouldUseParent) {
                                rawEpisodes.clear()
                                rawEpisodes.addAll(parentEps)
                                break
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                // Keep season identity attached to each link. Only infer one shared season when
                // the source gives no season labels at all; with multiple labelled seasons,
                // unlabeled sidebar/recommendation links are omitted instead of mixed in.
                val seasons = mutableListOf<ArabseedSeason>()
                if (rawEpisodes.isNotEmpty()) {
                    val expectedSeasonNumber = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
                        ?: SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")
                    val parsedEpisodes = rawEpisodes.mapNotNull { (title, url, domSeasonNumber) ->
                        val episodeNumber = SeriesCatalogParser.episodeNumber(title, url) ?: return@mapNotNull null
                        val seasonNumber = domSeasonNumber
                            ?: SeriesCatalogParser.seasonNumber(title, url)
                        Triple(seasonNumber, episodeNumber, url)
                    }.distinctBy { (_, _, url) -> SeriesCatalogParser.normalizedUrl(url) }
                    val labelledSeasons = parsedEpisodes.mapNotNull { it.first }.distinct()
                    val inferredSeasonNumber = when {
                        labelledSeasons.size > 1 -> null
                        labelledSeasons.size == 1 -> labelledSeasons.single()
                        expectedSeasonNumber != null -> expectedSeasonNumber
                        !isEpisodePage -> 1
                        else -> 1
                    }
                    val numberedEpisodes = parsedEpisodes.mapNotNull { (seasonNumber, episodeNumber, url) ->
                        (seasonNumber ?: inferredSeasonNumber)?.let { Triple(it, episodeNumber, url) }
                    }

                    numberedEpisodes.groupBy { it.first }.toSortedMap().forEach { (sNum, rows) ->
                        val sortedEps = rows
                            .distinctBy { it.second }
                            .sortedBy { it.second }
                            .map { (_, episodeNumber, url) ->
                                ArabseedEpisode(episodeNumber, "الحلقة $episodeNumber", url, sNum)
                            }
                        seasons += ArabseedSeason(
                            number = sNum,
                            title = SeriesCatalogParser.seasonTitle(sNum),
                            episodes = sortedEps,
                        )
                    }
                }

                // Find related parts/seasons linked from the page (EXCLUDING any episodes)
                val relatedParts = mutableListOf<ArabseedItem>()
                val partSelectors = listOf(
                    ".parts-list a", ".parts a",
                    "a[href*='%d8%a7%d9%84%d8%ac%d8%b2%d8%a1']", "a[href*='الجزء']",
                    "a[href*='part']"
                )
                for (sel in partSelectors) {
                    for (el in doc.select(sel)) {
                        val href = el.attr("abs:href").ifBlank { el.attr("href") }
                        val txt = el.text().trim().ifBlank { el.attr("title").trim() }
                        if (href.isNotBlank() && href.startsWith(P_HTTP)
                            && href != item.pageUrl && txt.isNotBlank()
                        ) {
                            val isEpisodeLike = SeriesCatalogParser.isEpisode(txt, href)
                            val isPart = SeriesCatalogParser.hasPartMarker(txt, href)
                            if (isPart && relatedParts.none { it.pageUrl == href }) {
                                val img = el.selectFirst("img")?.attr("abs:src")
                                    ?.ifBlank { el.selectFirst("img")?.attr("src") }
                                    ?: item.posterUrl
                                relatedParts += ArabseedItem(
                                    id = href.trimEnd('/').substringAfterLast('/').take(80),
                                    title = txt, arabicTitle = txt,
                                    posterUrl = img, year = item.year,
                                    quality = item.quality, rating = item.rating,
                                    type = item.type, pageUrl = href,
                                    category = item.category,
                                )
                            }
                        }
                    }
                    if (relatedParts.isNotEmpty()) break
                }

                // Additional smart discovery: search for other seasons/parts (EXCLUDING any episodes)
                val cleanBaseTitle = SeriesCatalogParser.cleanSeriesTitle(item.title)

                if (cleanBaseTitle.length >= 3) {
                    try {
                        val searchResults = search(cleanBaseTitle)
                        for (sr in searchResults) {
                            val srTitle = sr.title
                            val srUrl = sr.pageUrl
                            val isEpisode = SeriesCatalogParser.isEpisode(srTitle, srUrl)
                            val isPart = SeriesCatalogParser.hasPartMarker(srTitle, srUrl)

                            if (isPart && !isEpisode && srUrl != item.pageUrl && srTitle.contains(cleanBaseTitle, ignoreCase = true)) {
                                if (relatedParts.none { it.pageUrl == srUrl }) {
                                    relatedParts += sr
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Final safety filter: NO EPISODES in relatedParts
                val finalRelatedParts = relatedParts
                    .filter { part ->
                        SeriesCatalogParser.hasPartMarker(part.title, part.pageUrl)
                            && !SeriesCatalogParser.isEpisode(part.title, part.pageUrl)
                    }
                    .distinctBy { part ->
                        SeriesCatalogParser.partNumber(part.title, part.pageUrl)?.toString()
                            ?: SeriesCatalogParser.normalizedUrl(part.pageUrl)
                    }
                    .sortedWith(compareBy<ArabseedItem> {
                        SeriesCatalogParser.partNumber(it.title, it.pageUrl) ?: Int.MAX_VALUE
                    }.thenBy { it.title })
                    .take(12)

                val bundle = SeriesCatalogParser.normalizeSeasonBundle(ArabseedSeasonBundle(

                    seasons = seasons,

                    relatedParts = finalRelatedParts,

                ))

                seasonBundleCache[item.pageUrl] = bundle
                seasonBundleCacheAt[item.pageUrl] = System.currentTimeMillis()
                CatalogDiskCache.saveSeasonBundle("arabseed:seasons:v2:" + item.pageUrl, bundle)

                bundle

            } catch (_: Exception) {

                ArabseedSeasonBundle(emptyList())

            }

        }



    fun fetchEpisodeServers(episodeUrl: String): List<ArabseedServer> =

        fetchServersFast(episodeUrl)



    fun resolveDirectStream(serverUrl: String): String? = resolveDirectStreamInternal(serverUrl, 0)

    private fun resolveDirectStreamInternal(serverUrl: String, depth: Int): String? {
        if (depth > 2) return null
        try {
            streamCache[serverUrl]?.let { return it }

            if (serverUrl.contains(".m3u8") || serverUrl.contains(".mp4")) {
                streamCache[serverUrl] = serverUrl
                return serverUrl
            }

            // ── Fast direct provider resolvers ────────────────────────────
            // 1. Vidara / Sugbaioug API resolver
            if (serverUrl.contains("vidara.") || serverUrl.contains("sugbaioug.")) {
                val fileCode = serverUrl.trimEnd('/').substringAfterLast('/')
                if (fileCode.isNotBlank()) {
                    try {
                        val jsonBody = """{"filecode":"$fileCode","device":"android","codecs":["av01.0.09M.10"]}"""
                        val postReq = Request.Builder()
                            .url("https://vidara.to/api/stream")
                            .post(jsonBody.toRequestBody("application/json".toMediaType()))
                            .header("User-Agent", UA)
                            .header("Referer", serverUrl)
                            .header("Origin", "https://vidara.to")
                            .build()
                        client.newCall(postReq).execute().use { resp ->
                            val bodyStr = resp.body?.string().orEmpty()
                            val streamMatch = Regex(""""streaming_url"\s*:\s*"([^"]+)"""").find(bodyStr)
                            streamMatch?.groupValues?.get(1)?.let { stream ->
                                val cleanStream = stream.replace("\\/", "/")
                                if (cleanStream.startsWith("http")) {
                                    streamCache[serverUrl] = cleanStream
                                    return cleanStream
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            // 2. Vidmoly direct m3u8
            if (serverUrl.contains("vidmoly")) {
                try {
                    val vmHtml = fetchHtmlWithReferer(serverUrl, "https://m.myseed.tv/")
                    val vmMatch = Regex("""["'](https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)["']""").find(vmHtml)
                    vmMatch?.groupValues?.get(1)?.let { vmStream ->
                        streamCache[serverUrl] = vmStream
                        return vmStream
                    }
                } catch (_: Exception) {}
            }

            // 3. Uqload direct mp4
            if (serverUrl.contains("uqload")) {
                try {
                    val uqHtml = fetchHtmlWithReferer(serverUrl, "https://uqload.to/")
                    val uqMatch = Regex("""sources\s*:\s*\[\s*["']([^"']+\.mp4[^"']*)["']""").find(uqHtml)
                    uqMatch?.groupValues?.get(1)?.let { uqStream ->
                        if (uqStream.startsWith("http")) {
                            streamCache[serverUrl] = uqStream
                            return uqStream
                        }
                    }
                } catch (_: Exception) {}
            }

            val origin = try {
                val u = java.net.URI.create(serverUrl)
                "${u.scheme}://${u.host}/"
            } catch (_: Exception) { BASE_SLASH }

            val gHtml = fetchHtmlWithReferer(serverUrl, origin)
            if (gHtml.isBlank()) return null

            // 1. Direct <source src="..."> in HTML5 video tags (e.g. MySeed / player-gateway)
            val sourceMatch = Regex("""<source\s+[^>]*src=["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE).find(gHtml)
            sourceMatch?.groupValues?.getOrNull(1)?.let {
                streamCache[serverUrl] = it
                return it
            }

            // 2. Direct regex match for mp4 or m3u8 in scripts/page
            val dirRegex = Regex("""["'](https?://[^\s"'<>]+\.(?:m3u8|mp4)(?:\?[^\s"'<>]*)?)["']""", RegexOption.IGNORE_CASE)
            dirRegex.find(gHtml)?.groupValues?.getOrNull(1)?.let {
                streamCache[serverUrl] = it
                return it
            }

            // 3. Follow iframe hops (e.g. /vids.php -> player-gateway.php -> video.mp4)
            val doc = Jsoup.parse(gHtml, origin)
            for (ifr in doc.select("iframe")) {
                val src = ifr.attr("src").ifBlank { ifr.attr("data-src") }.trim()
                if (src.isNotBlank() && !isAdUrl(src)) {
                    val fullIfr = when {
                        src.startsWith("http://") || src.startsWith("https://") -> src
                        src.startsWith("//") -> "https:$src"
                        src.startsWith("/") -> origin.trimEnd('/') + src
                        else -> "$origin$src"
                    }
                    val resolved = resolveDirectStreamInternal(fullIfr, depth + 1)
                    if (resolved != null) {
                        streamCache[serverUrl] = resolved
                        return resolved
                    }
                }
            }

            // 4. Hex decoding check
            val hexRegex = Regex("""["'](68747470[0-9a-fA-F]{30,})["']""")
            hexRegex.findAll(gHtml).forEach { m ->
                val hex = m.groupValues[1]
                val dec = hexToString(hex)
                if (dec.startsWith("http") && (dec.contains(".m3u8") || dec.contains(".mp4"))) {
                    streamCache[serverUrl] = dec
                    return dec
                }
            }

            // 5. JWPlayer / videojs regex
            val jwRegex = Regex("""(?:file|sources|src)\s*:\s*(?:\[\s*\{[^}]*?file\s*:\s*["']([^"']+)["']|\[\s*["']([^"']+)["']|["']([^"']+\.(?:mp4|m3u8)[^"']*)["'])""", RegexOption.IGNORE_CASE)
            jwRegex.find(gHtml)?.let { jm ->
                val cand = jm.groupValues.drop(1).firstOrNull { it.isNotBlank() }
                if (cand != null && cand.startsWith("http")) {
                    streamCache[serverUrl] = cand
                    return cand
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun hexToString(hex: String): String {

        val sb = StringBuilder()

        var i = 0

        while (i < hex.length - 1) {

            sb.append(hex.substring(i, i + 2).toInt(16).toChar())

            i += 2

        }

        return sb.toString()

    }



    // ── HTML Fetching ─────────────────────────────────────────────────────



    private fun fetchHtml(urlStr: String): String =

        fetchHtmlWithReferer(urlStr, BASE_SLASH)



    private fun fetchHtmlWithReferer(urlStr: String, referer: String): String {

        val req = Request.Builder()

            .url(urlStr)

            .header("User-Agent", UA)

            .header("Accept", VAL_AHTML)

            .header(HDR_ALANG, VAL_ALANG)

            .header(HDR_REF, referer)

            .header(HDR_CONN, VAL_KALIVE)

            .build()



        val host = runCatching { java.net.URI.create(urlStr).host.orEmpty() }.getOrDefault("invalid-url")
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    android.util.Log.w(TAG, "HTTP ${resp.code} for host=$host")
                    throw IOException("HTTP ${resp.code}")
                }
                val body = resp.body?.string() ?: throw IOException("Empty response")
                if (body.isBlank()) android.util.Log.w(TAG, "Empty HTML response for host=$host")
                body
            }
        } catch (error: Exception) {
            if (error !is IOException || !error.message.orEmpty().startsWith("HTTP ")) {
                android.util.Log.w(TAG, "Fetch failed for host=$host (${error.javaClass.simpleName})")
            }
            throw error
        }

    }



    // ── Parsers ───────────────────────────────────────────────────────────



    private fun parseListing(html: String, defaultType: ArabseedType?): List<ArabseedItem> {

        val doc = Jsoup.parse(html, BASE_URL)

        // MySeed's current series catalog uses a new card template and /selary/ links;
        // without this selector the series page downloaded successfully but parsed no rows.
        val elements = doc.select("a.movie__block, .item__contents a, .movie-item a, article a, .postDiv a, .innov-series-card a")

        val items = mutableListOf<ArabseedItem>()



        for (el in elements) {

            val href = el.attr(A_HREF).ifBlank { el.attr("href") }

            if (href.isBlank() || !href.startsWith(P_HTTP)) continue

            if (href == BASE_SLASH || href.endsWith("/home/") || href.contains("/category/") || href.contains("/tag/")) continue



            val rawTitle = el.selectFirst("h3, h2, h4")?.text()?.ifBlank { null }

                ?: el.attr("title").ifBlank { null }

                ?: el.selectFirst("img")?.attr("alt")?.ifBlank { null }

                ?: continue



            if (rawTitle.length < 2) continue



            val title    = cleanTitle(rawTitle)

            val img      = el.selectFirst("img")

            val posterUrl = img?.attr(A_SRC)?.ifBlank { null }

                ?: img?.attr("src")?.ifBlank { null }

                ?: img?.attr(R_DSRC)?.ifBlank { null }

                ?: img?.attr(R_DLAZY)?.ifBlank { null }



            val quality  = el.selectFirst(".__quality, .quality")?.text()?.trim() ?: extractQuality(rawTitle)

            val category = el.selectFirst(".post__category, .category")?.text()?.trim() ?: "عام"

            val desc     = el.selectFirst(".post__info p, .desc, p")?.text()?.trim() ?: ""

            val year     = extractYear("$rawTitle $desc")



            val isSeriesLike = href.contains("مسلسل") || href.contains("series")

                || title.contains("مسلسل") || rawTitle.contains("مسلسل")

                || title.contains("الحلقة") || rawTitle.contains("الحلقة")

                || title.contains("الموسم") || rawTitle.contains("الموسم")

                || title.contains("حلقة") || rawTitle.contains("حلقة")

                || title.contains("موسم") || rawTitle.contains("موسم")

                || href.contains("episode") || href.contains("season")



            val type = when {

                isSeriesLike -> ArabseedType.SERIES

                defaultType != null -> defaultType

                else -> ArabseedType.MOVIE

            }



            items += ArabseedItem(

                id          = href.trimEnd('/').substringAfterLast('/').take(80).ifBlank { href.hashCode().toString() },

                title       = title,

                arabicTitle = rawTitle,

                posterUrl   = posterUrl,

                year        = year,

                quality     = quality,

                rating      = extractRating(desc),

                type        = type,

                pageUrl     = href,

                category    = category,

                overview    = desc,

            )

        }

        return items.distinctBy { it.pageUrl }

    }



    private fun parseOverview(html: String): String {
        if (html.isBlank()) return ""
        val doc = Jsoup.parse(html)
        val modernSelectors = listOf(
            ".post__story p",
            ".post__story",
            ".post__content",
            ".story",
            ".overview",
            ".description",
            "div.content p",
            "div.entry-content p"
        )
        for (sel in modernSelectors) {
            val el = doc.selectFirst(sel)
            if (el != null) {
                val t = cleanStoryText(el.text())
                if (t.length > 20 && !t.contains("حقوق الطبع") && !t.contains("dmca") && !t.contains("اسرع موقع")) {
                    return t
                }
            }
        }
        doc.selectFirst(STORY_SEL)?.text()?.let { cleanStoryText(it).takeIf { t -> t.length > 20 }?.let { return it } }
        for (b in _storyFallbacks) {
            val sel = X.s(b)
            val txt = doc.selectFirst(sel)?.text()?.let { cleanStoryText(it) } ?: ""
            if (txt.length > 25 && !txt.contains("اسرع موقع")) {
                return txt
            }
        }
        return ""
    }



    private fun cleanStoryText(raw: String): String =

        raw.replace("&quot;", "\"").replace("&amp;", "&").replace("&hellip;", "...")

            .replace(Regex("""\s{2,}"""), " ").trim()



        private fun parseServersFast(html: String): List<ArabseedServer> {
        val servers = mutableListOf<ArabseedServer>()
        val doc = Jsoup.parse(html)

        fun cleanServerName(rawName: String, url: String): String {
            val u = url.lowercase()
            return when {
                u.contains("vids.php") || u.contains("player-gateway") -> "سيرفر ماي سيد (فائق السرعة ⚡)"
                u.contains("bysezejataos") || u.contains("vidsharing") -> "Vidsharing (فائق السرعة ⚡)"
                u.contains("uqload") -> "uqload (سريع)"
                u.contains("voe.") -> "voe (سريع)"
                u.contains("vidara.") || u.contains("sugbaioug.") -> "vidara (سريع)"
                u.contains("vinvo.") -> "Vinvo (سريع)"
                u.contains("vidmoly.") -> "Vidmoly (سريع)"
                u.contains("playtube.") -> "Playtube"
                u.contains("dsvplay.") -> "DSV Play"
                rawName.isNotBlank() && !rawName.startsWith("سيرفر") -> rawName
                else -> "سيرفر ${servers.size + 1}"
            }
        }

        // 1. Elements with data-link
        for (el in doc.select("[data-link], $SEL_DLINK, ul.servers li, .watch-servers li, #servers li, .servers li")) {
            var link = el.attr("data-link").ifBlank { el.attr(DLINK_ATTR) }.trim()
            val rawName = el.attr("data-server-name").ifBlank { el.text() }.trim()
            if (link.isNotBlank() && !isAdUrl(link)) {
                if (link.contains("id=")) {
                    val b64 = link.substringAfter("id=").substringBefore("&")
                    try {
                        val decoded = String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT)).trim()
                        if (decoded.startsWith("http")) link = decoded
                    } catch (_: Exception) {}
                }
                val full = when {
                    link.startsWith("http://") || link.startsWith("https://") -> link
                    link.startsWith("//") -> "https:$link"
                    link.startsWith("/") -> "${BASE_URL.trimEnd('/')}$link"
                    else -> "${BASE_URL.trimEnd('/')}/$link"
                }
                val serverName = cleanServerName(rawName, full)
                if (servers.none { it.embedUrl == full }) servers += ArabseedServer(serverName, full)
            }
        }

        // 2. Elements with data-url, data-src, data-embed
        for (el in doc.select("$SEL_DURL, $SEL_DSRC, $SEL_DEMBED, [data-url], [data-src], [data-embed]")) {
            val link = el.attr(R_DURL).ifBlank { el.attr(R_DEMBED).ifBlank { el.attr(R_DSRC).ifBlank { el.attr("data-url").ifBlank { el.attr("data-embed") } } } }.trim()
            val rawName = el.attr("data-server-name").ifBlank { el.text() }.trim()
            if (link.isNotBlank() && (link.startsWith("http") || link.startsWith("/")) && !isAdUrl(link)) {
                val full = if (link.startsWith("http")) link else "${BASE_URL.trimEnd('/')}/" + link.trimStart('/')
                val serverName = cleanServerName(rawName, full)
                if (servers.none { it.embedUrl == full }) servers += ArabseedServer(serverName, full)
            }
        }

        // 3. Iframes
        for (ifr in doc.select("iframe, $SEL_IFRAME")) {
            val src = ifr.attr("src").ifBlank { ifr.attr(R_ISRC).ifBlank { ifr.attr(R_DSRC) } }.trim()
            if (src.isNotBlank() && !isAdUrl(src)) {
                val fullSrc = when {
                    src.startsWith("http://") || src.startsWith("https://") -> src
                    src.startsWith("//") -> "https:$src"
                    src.startsWith("/") -> "${BASE_URL.trimEnd('/')}$src"
                    else -> "${BASE_URL.trimEnd('/')}/$src"
                }
                val serverName = cleanServerName("سيرفر تشغيل", fullSrc)
                if (servers.none { it.embedUrl == fullSrc }) servers += ArabseedServer(serverName, fullSrc)
            }
        }

        return servers.take(10)
    }

    private fun b(vararg ints: Int): ByteArray = ByteArray(ints.size) { ints[it].toByte() }



    private val AD_PATTERNS = listOf(
        "doubleclick", "googlead", "googlesyndication", "popads", "adsterra",
        "propellerads", "monetag", "trafficjunky", "histats", "onclickmega",
        "ad-maven", "adcash", "exoclick", "juicyads", "bet365", "1xbet", "melbet",
        "facebook.com", "twitter.com", "instagram.com", "llvpn.com", "syndication",
        "vidoomy", "adservice", "adsystem", "onclick", "popunder", "banner",
        "track.", "ads.", "analytics", "coinhive", "adtrue", "revenuehits"
    )

    private fun isAdUrl(url: String): Boolean {
        val parsed = runCatching { java.net.URI.create(url) }.getOrNull() ?: return false
        val host = parsed.host.orEmpty().lowercase(java.util.Locale.ROOT)
        if (AD_PATTERNS.any { host.contains(it.lowercase(java.util.Locale.ROOT)) }) return true
        val path = parsed.path.orEmpty()
        return Regex("(?i)(^|/)(ads?|advert|banner|popunder|onclick|tracking|analytics)(/|$)")
            .containsMatchIn(path)
    }

    private fun cleanTitle(raw: String): String =

        raw.replace(Regex("""(?i)(مترجم|مدبلج|مشاهدة|تحميل|اون لاين|فيلم|مسلسل|HD|4K|1080p|720p|WEB-DL|BluRay)\s*"""), "")

            .replace(Regex("""\s{2,}"""), " ").trim().take(100)



    private fun extractQuality(text: String): String =

        Regex("""(?i)(4K|1080p|720p|480p|WEB-DL|BluRay|CAM|HD)""").find(text)?.groupValues?.get(1) ?: ""



    private fun extractYear(text: String): String =

        Regex("""\b(20\d\d|19\d\d)\b""").find(text)?.groupValues?.get(1) ?: ""



    private fun extractRating(text: String): String =

        Regex("""\b([1-9](\.\d)?)/10\b""").find(text)?.groupValues?.get(1) ?: ""

}

