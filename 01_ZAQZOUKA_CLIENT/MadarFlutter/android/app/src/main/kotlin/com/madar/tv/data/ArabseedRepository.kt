package com.madar.tv.data



import com.madar.tv.core.X

import okhttp3.ConnectionPool

import okhttp3.Cookie

import okhttp3.CookieJar

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup

import java.io.IOException

import java.net.URLEncoder

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

import java.util.concurrent.TimeUnit

import java.util.regex.Pattern



object ArabseedRepository {

    private const val TAG = "ArabseedRepository"



    // ═══════════════════════════════════════════════════════════════════════

    // ALL CONSTANTS â€” 6-layer encrypted via X.s/X.j (v6 engine)

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
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = store.getOrPut(url.host) { mutableListOf() }
            synchronized(list) {
                list.removeAll { existing -> cookies.any { it.name == existing.name } }
                list.addAll(cookies)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = store[url.host] ?: return emptyList()
            return synchronized(list) { list.toList() }
        }
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
    private val serverLookupPool = Executors.newFixedThreadPool(4)
    private val serverFallbackPool = Executors.newFixedThreadPool(2)
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
        val key = "v2:$cleanPath:$page"
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

    private fun sanitizeDetailForCacheAge(detail: ArabseedDetail, savedAt: Long): ArabseedDetail? {
        val freshTokenWindow = (System.currentTimeMillis() - savedAt) < 45_000L
        if (freshTokenWindow) return detail.takeIf { it.servers.isNotEmpty() }
        val stableServers = detail.servers
            .filterNot { it.embedUrl.contains("vids.php", true) || it.embedUrl.contains("player-gateway", true) }
            .map { it.copy(directStreamUrl = "") }
        if (stableServers.isEmpty()) return null
        return detail.copy(servers = stableServers, preResolvedStream = null)
    }

    fun loadCachedDetail(pageUrl: String): ArabseedDetail? {
        detailCache[pageUrl]?.let { cached ->
            sanitizeDetailForCacheAge(cached, detailCacheAt[pageUrl] ?: 0L)?.let { return it }
        }
        val cached = CatalogDiskCache.loadDetail("arabseed:detail:v3:" + pageUrl) ?: return null
        val sanitized = sanitizeDetailForCacheAge(cached.value, cached.savedAt) ?: return null
        detailCache[pageUrl] = cached.value
        detailCacheAt[pageUrl] = cached.savedAt
        return sanitized
    }

    fun loadCachedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        seasonBundleCache[pageUrl]?.let { return it }
        CatalogDiskCache.loadSeasonBundle("arabseed:seasons:v29:" + pageUrl)?.let { cached ->
            val normalized = SeriesCatalogParser.normalizeSeasonBundle(cached.value)
            seasonBundleCache[pageUrl] = normalized
            seasonBundleCacheAt[pageUrl] = cached.savedAt
            return normalized
        }
        return null
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

        val cacheKey = "v2:$cleanPath:$page"

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

        // Only retry the home route itself. Falling back from a failed genre page
        // to the home catalog mislabeled unrelated Arabic titles as foreign results.
        if (items.isEmpty() && cleanPath.trimEnd('/') == "main") {
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



    fun search(query: String, forceRefresh: Boolean = false, maxPages: Int = 1): List<ArabseedItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        val now = System.currentTimeMillis()
        if (!forceRefresh && maxPages <= 1) {
            searchCache[normalized]?.let { (savedAt, result) ->
                if (now - savedAt < SEARCH_CACHE_TTL && result.isNotEmpty()) return result
            }
            loadCachedSearch(normalized)?.let { cached ->
                val savedAt = searchCache[normalized]?.first ?: 0L
                if (now - savedAt < SEARCH_CACHE_TTL) return cached
            }
        }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        // MySeed uses /find/?word= (the old /?s= returns an error page)
        val url = "$BASE_URL/find/?word=$encoded"
        var html = fetchHtml(url)
        val allResults = parseListing(html, null).toMutableList()
        if (allResults.isEmpty()) {
            val plainQuery = SeriesCatalogParser.normalizeArabicSearchQuery(query)
            if (plainQuery.isNotBlank() && plainQuery != query.trim()) {
                val plainUrl = "$BASE_URL/find/?word=${URLEncoder.encode(plainQuery, "UTF-8")}"
                val plainHtml = fetchHtml(plainUrl)
                if (plainHtml.isNotBlank()) {
                    html = plainHtml
                    allResults += parseListing(plainHtml, null)
                }
            }
        }
        if (maxPages > 1 && html.isNotBlank()) {
            try {
                val doc = Jsoup.parse(html, BASE_URL)
                // Try standard WordPress pagination selectors first
                var paginationLinks = doc.select(".pagination a[href], .page-numbers[href], .page-numbers a[href], .wp-pagenavi a[href], a[rel='next'], .nav-next a[href]")
                    .mapNotNull { it.absUrl("href").trim().takeIf(String::isNotBlank) }
                    .filter { it.startsWith("http") && it != url }
                    .distinct()
                if (paginationLinks.isEmpty()) {
                    // MySeed uses /find/page/N/?word=... for search pagination
                    val encodedWord = url.substringAfter("word=").substringBefore('&')
                    paginationLinks = (2..maxPages).map { p -> "$BASE_URL/find/page/$p/?word=$encodedWord" }
                }
                val targets = paginationLinks.take(maxPages - 1)
                val completion = ExecutorCompletionService<List<ArabseedItem>>(seasonPagePool)
                val futures = targets.map { pageLink ->
                    completion.submit(Callable {
                        runCatching {
                            val pageHtml = fetchHtmlWithReferer(pageLink, url, callTimeoutSeconds = 5)
                            if (pageHtml.isBlank()) emptyList() else parseListing(pageHtml, null)
                        }.getOrDefault(emptyList())
                    })
                }
                val pageDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
                var remainingPages = futures.size
                while (remainingPages > 0) {
                    val left = pageDeadline - System.nanoTime()
                    if (left <= 0L) break
                    val finished = try {
                        completion.poll(left, TimeUnit.NANOSECONDS) ?: break
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    remainingPages--
                    runCatching { finished.get() }.getOrNull()?.let { pageItems ->
                        allResults.addAll(pageItems)
                    }
                }
                futures.filterNot { it.isDone }.forEach { it.cancel(true) }
            } catch (_: Exception) {}
        }
        val distinctResults = allResults.distinctBy { it.pageUrl }
        if (distinctResults.isNotEmpty() && maxPages <= 1) {
            searchCache[normalized] = now to distinctResults
            CatalogDiskCache.saveItems("arabseed:search:$normalized", distinctResults)
        }
        return distinctResults
    }


    fun fetchStoryFast(pageUrl: String): String =

        try { parseOverview(fetchHtml(pageUrl)) } catch (_: Exception) { "" }



        fun fetchServersFast(pageUrl: String): List<ArabseedServer> {
        val cleanUrl = pageUrl.trimEnd('/')
        val watchUrl = if (cleanUrl.endsWith("/watch")) "$cleanUrl/" else "$cleanUrl/watch/"
        val urls = listOf(watchUrl, cleanUrl).distinct()
        val completion = ExecutorCompletionService<Pair<String, List<ArabseedServer>>>(serverLookupPool)
        val pageFutures = urls.map { url ->
            completion.submit(Callable {
                val isCanonicalWatchPage = url == watchUrl
                url to parseServersFast(fetchHtml(url), pageUrl = url, allowGenericIframes = isCanonicalWatchPage)
            })
        }
        val pageResults = mutableMapOf<String, List<ArabseedServer>>()
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(8000)
        repeat(pageFutures.size) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) return@repeat
            val finished = try {
                completion.poll(remaining, TimeUnit.NANOSECONDS) ?: return@repeat
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return@repeat
            }
            runCatching { finished.get() }.getOrNull()?.let { (url, rows) ->
                pageResults[url] = rows
            }
        }
        pageFutures.filterNot { it.isDone }.forEach { it.cancel(true) }

        // The /watch page owns the playback server list. A detail page may
        // contain poster, trailer, or recommendation frames, so it is only a
        // fallback when the canonical page has no server entries.
        val watchServers = pageResults[watchUrl].orEmpty()
        val detailServers = if (watchServers.isEmpty()) pageResults[cleanUrl].orEmpty() else emptyList()
        val servers = (watchServers + detailServers)
            .filter { isServerCandidateUrl(it.embedUrl) }
            .distinctBy { it.embedUrl.trimEnd('/') }
            .take(10)

        // Old links occasionally need a title lookup. Keep that repair path,
        // but cap its wait so a slow search cannot hold the watch screen open.
        if (servers.isEmpty()) {
            val fallback = serverFallbackPool.submit(Callable { fetchServersViaSearch(pageUrl) })
            try {
                return fallback.get(5, TimeUnit.SECONDS).distinctBy { it.embedUrl }.take(10)
            } catch (_: Exception) {
                fallback.cancel(true)
            }
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
        val matchingResults = searchResults.filter { result ->
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf(titleFromSlug),
                targetPageUrls = listOf(originalUrl),
                candidateTitles = listOfNotNull(result.parentSeriesTitle, result.title, result.arabicTitle),
                candidatePageUrls = listOf(result.pageUrl),
            )
        }
        if (matchingResults.isEmpty()) return emptyList()
        val epNum = Regex("""-eps?(\d+)""", RegexOption.IGNORE_CASE).find(originalUrl)?.groupValues?.get(1)?.toIntOrNull()
        val sNum  = Regex("""-s(\d+)-""",  RegexOption.IGNORE_CASE).find(originalUrl)?.groupValues?.get(1)?.toIntOrNull()
        if (epNum != null && sNum != null) {
            for (result in matchingResults.take(3)) {
                try {
                    if (SeriesCatalogParser.isEpisode(result.title, result.pageUrl) &&
                        SeriesCatalogParser.episodeNumber(result.title, result.pageUrl) == epNum &&
                        SeriesCatalogParser.seasonNumber(result.title, result.pageUrl) == sNum
                    ) {
                        val directServers = fetchEpisodeServers(result.pageUrl)
                        if (directServers.isNotEmpty()) return directServers
                    }
                    val bundle = fetchSeasonsAndEpisodes(result)
                    val targetSeason = bundle.seasons.firstOrNull { it.number == sNum }
                    val episode = targetSeason?.episodes?.firstOrNull { it.number == epNum }
                    if (episode != null) {
                        val epServers = try {
                            val w = episode.pageUrl.trimEnd('/') + "/watch/"
                            parseServersFast(fetchHtml(w), pageUrl = w).ifEmpty {
                                parseServersFast(fetchHtml(episode.pageUrl), pageUrl = episode.pageUrl, allowGenericIframes = false)
                            }
                        } catch (_: Exception) { emptyList() }
                        if (epServers.isNotEmpty()) return epServers
                    }
                } catch (_: Exception) {}
            }
        }
        for (result in matchingResults.take(3)) {
            val s = try {
                val w = result.pageUrl.trimEnd('/') + "/watch/"
                parseServersFast(fetchHtml(w), pageUrl = w).ifEmpty {
                    parseServersFast(fetchHtml(result.pageUrl), pageUrl = result.pageUrl, allowGenericIframes = false)
                }
            } catch (_: Exception) { emptyList() }
            if (s.isNotEmpty()) return s
        }
        return emptyList()
    }





    fun fetchDetail(item: ArabseedItem): ArabseedDetail {
        val now = System.currentTimeMillis()
        detailCache[item.pageUrl]?.let { cached ->
            val age = now - (detailCacheAt[item.pageUrl] ?: now)
            val hasEphemeral = cached.servers.any {
                it.embedUrl.contains("vids.php", true) || it.embedUrl.contains("player-gateway", true)
            }
            if (cached.servers.isNotEmpty() && age < (if (hasEphemeral) 45_000L else DETAIL_CACHE_TTL)) {
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
            // ── جرّب كل السيرفرات بالترتيب حتى يشتغل واحد ─────────────────
            if (servers.isNotEmpty()) {
                val resolvedServers = mutableListOf<ArabseedServer>()
                var foundWorking = false
                for (server in servers) {
                    if (!foundWorking && server.embedUrl.isNotBlank()) {
                        try {
                            val resolved = resolveDirectStream(server.embedUrl, item.pageUrl)
                            if (!resolved.isNullOrBlank()) {
                                // ضيف السيرفر مع directStreamUrl
                                resolvedServers += server.copy(directStreamUrl = resolved)
                                preResolved = resolved
                                foundWorking = true
                                continue
                            }
                        } catch (_: Exception) {}
                    }
                    resolvedServers += server
                }
                servers = resolvedServers
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
            CatalogDiskCache.saveDetail("arabseed:detail:v3:" + item.pageUrl, detail)
        }

        return detail
    }



    // ── Seasons & Episodes ────────────────────────────────────────────────



    private val seasonBundleCache = ConcurrentHashMap<String, ArabseedSeasonBundle>()
    private val seasonBundleCacheAt = ConcurrentHashMap<String, Long>()
    private val synthesizedBundleCache = ConcurrentHashMap<String, ArabseedSeasonBundle>()
    private val seasonPagePool = Executors.newFixedThreadPool(10)
    private const val SEASON_CACHE_TTL = 30 * 60 * 1000L



    fun saveSynthesizedBundle(pageUrl: String, bundle: ArabseedSeasonBundle) {
        // Keep in-memory only; never poison persistent season cache with partial 1-2 ep catalog stubs.
        synthesizedBundleCache[pageUrl] = bundle
    }

    fun fetchSeasonsAndEpisodes(
            item: ArabseedItem,
            forceRefresh: Boolean = false,
            seasonNumbersOnly: Set<Int>? = null,
        ): ArabseedSeasonBundle {

            val now = System.currentTimeMillis()
            if (!forceRefresh) seasonBundleCache[item.pageUrl]?.let { cached ->
                if (now - (seasonBundleCacheAt[item.pageUrl] ?: now) < SEASON_CACHE_TTL) return cached
            }

            return try {

                val html = fetchSeasonHtml(item.pageUrl)
                val csrfTokenRef = java.util.concurrent.atomic.AtomicReference(
                    Regex("""csrf[_]{1,2}token['"]?\s*:\s*["']([A-Za-z0-9_-]+)["']""")
                        .find(html)?.groupValues?.getOrNull(1).orEmpty(),
                )
                android.util.Log.i(TAG, "fetchSeasonsAndEpisodes url=${item.pageUrl} title=${item.title} htmlLen=${html.length}")
                val doc = Jsoup.parse(html, BASE_URL)
                doc.select("footer, script, style").remove()
                val allLinks = doc.select("a[href]").map { it.attr("href") to it.text().trim() }
                val epLinks = allLinks.filter {
                    val h = it.first.lowercase()
                    val t = it.second.lowercase()
                    h.contains("eps") || h.contains("episode") || t.contains("الحلقة") || h.contains("selary") || h.contains("series")
                }
                android.util.Log.i(TAG, "fetchSeasonsAndEpisodes relevant links found (${epLinks.size}): ${epLinks.take(20)}")



                val rawEpisodes = mutableListOf<Triple<String, String, Int?>>()
                val pageSeasonNumber = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
                val requestedSeasonPageNumber = seasonNumbersOnly?.singleOrNull()
                    ?.takeIf { pageSeasonNumber == it }
                val targetIdentityTitles = listOfNotNull(
                    item.parentSeriesTitle,
                    item.title,
                    item.arabicTitle,
                )
                val targetIdentityUrls = listOfNotNull(
                    item.parentSeriesPageUrl,
                    item.pageUrl,
                )
                fun collectEpisodes(d: org.jsoup.nodes.Document): List<Triple<String, String, Int?>> {
                    val collected = linkedMapOf<String, Triple<String, String, Int?>>()
                    for (sel in SeriesCatalogParser.episodeLinkSelectors) {
                        val els = d.select(sel)
                        if (els.isNotEmpty()) {
                            for (el in els) {
                                if (SeriesCatalogParser.isInsideUnrelatedContentContainer(el)) continue
                                val href = listOf("href", "data-url", "data-href", "data-link", "data-episode-link")
                                    .firstNotNullOfOrNull { attribute ->
                                        val candidate = el.absUrl(attribute).ifBlank { el.attr(attribute).trim() }
                                        MultiProviderRepository.resolveProviderContentPageUrl(candidate, item.pageUrl)
                                    }.orEmpty()
                                val txt = SeriesCatalogParser.episodeLinkText(el)
                                if (href.isBlank() || !href.startsWith(P_HTTP) || href.endsWith("/home/") || href == BASE_URL) continue
                                if (SeriesCatalogParser.hasConflictingSeasonNumbers(txt, href)) continue
                                if (href.contains("/category/") || href.contains("/tag/") || href.contains("/genre/") || href.contains("/actor/")) continue

                                val episodeNumber = SeriesCatalogParser.episodeNumber(
                                    title = txt,
                                    url = href,
                                    allowNumericTitle = SeriesCatalogParser.isEpisodeListElement(el),
                                )
                                if (episodeNumber != null && !SeriesCatalogParser.episodeBelongsToSeries(
                                        targetTitles = targetIdentityTitles,
                                        targetPageUrls = targetIdentityUrls,
                                        episodeTitle = txt,
                                        episodeUrl = href,
                                        allowEpisodeListContext = SeriesCatalogParser.isEpisodeListElement(el),
                                    )
                                ) continue
                                val linkSeason = SeriesCatalogParser.seasonNumber(txt, href)
                                    ?: SeriesCatalogParser.seasonNumberFromElement(el, txt, href)
                                if (seasonNumbersOnly != null && linkSeason != null &&
                                    linkSeason !in seasonNumbersOnly
                                ) continue
                                // A nearby season container beats the page's season hint.
                                // Provider pages can include links from another season in a sidebar.
                                val seasonNumber = linkSeason ?: requestedSeasonPageNumber

                                if (episodeNumber != null) {
                                    val key = SeriesCatalogParser.normalizedUrl(href)
                                    val previous = collected[key]
                                    if (previous == null || (previous.third == null && seasonNumber != null)) {
                                        collected[key] = Triple(txt, href, seasonNumber)
                                    }
                                }
                            }
                        }
                    }
                    return collected.values.toList()
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

                // A clean sequence on page 1 does not mean a long-running series
                // has no later pages. Follow the provider's explicit pager.
                val seasonHasMorePages = linkedMapOf<Int, Boolean>()
                val pageHasPager = SeriesCatalogParser.linkedEpisodePages(doc, item.pageUrl).isNotEmpty()
                if (pageHasPager) {
                    val paginatedResult = SeriesCatalogParser.collectPaginatedEpisodesWithMore(
                        firstDocument = doc,
                        firstPageUrl = item.pageUrl,
                        fetchDocument = { pageUrl ->
                            fetchSeasonHtml(pageUrl).takeIf(String::isNotBlank)?.let { Jsoup.parse(it, pageUrl) }
                        },
                        parseEpisodes = ::collectEpisodes,
                        maxPages = when {
                            seasonNumbersOnly != null -> 1
                            forceRefresh -> 8
                            else -> 4
                        },
                        timeoutSeconds = when {
                            seasonNumbersOnly != null -> 4
                            forceRefresh -> 8
                            else -> 5
                        },
                    )
                    (requestedSeasonPageNumber ?: pageSeasonNumber)?.let {
                        seasonHasMorePages[it] = paginatedResult.hasMorePages
                    }
                    for (ep in paginatedResult.episodes) {
                        val existingIndex = rawEpisodes.indexOfFirst {
                            SeriesCatalogParser.normalizedUrl(it.second) == SeriesCatalogParser.normalizedUrl(ep.second)
                        }
                        if (existingIndex < 0) rawEpisodes += ep
                        else {
                            val prev = rawEpisodes[existingIndex]
                            rawEpisodes[existingIndex] = Triple(
                                if (prev.first.isBlank()) ep.first else prev.first,
                                prev.second,
                                prev.third ?: ep.third,
                            )
                        }
                    }
                } else if (pageSeasonNumber != null) {
                    seasonHasMorePages[pageSeasonNumber] = false
                }

                val currentPage = SeriesCatalogParser.normalizedUrl(item.pageUrl)
                val isEpisodePage = SeriesCatalogParser.isEpisode(item.title, item.pageUrl)
                    || SeriesCatalogParser.isEpisode(item.latestEpisodeLabel, item.pageUrl)
                val seasonDiscoveryDocs = mutableListOf(doc)

                // Episode cards may not have a canonical series URL. Resolve their breadcrumb
                // parent even when the episode page exposes several links: those links can span
                // multiple seasons and must not be treated as one season's episode list.
                // Also probe parent when episodes are few (< 8) since MySeed often shows only
                // the latest 1-2 episodes on a series page â€” the parent /selary/ page has more.
                if ((rawEpisodes.size < 8 || isEpisodePage) &&
                    !(seasonNumbersOnly != null && SeriesCatalogParser.seasonNumber(item.title, item.pageUrl) != null)
                ) {
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
                            val valid = href.isNotBlank() && href.startsWith(P_HTTP) &&
                                MultiProviderRepository.isProviderContentUrl(href, item.pageUrl) &&
                                href != item.pageUrl
                                && !txt.contains("الرئيسية") && !txt.contains("home", ignoreCase = true)
                                && !SeriesCatalogParser.isEpisode(txt, href)
                            if (valid) href to txt else null
                        }
                    }.distinctBy { SeriesCatalogParser.normalizedUrl(it.first) }
                        .sortedBy { (href, title) ->
                            when {
                                href.contains("/selary/", ignoreCase = true) ||
                                    href.contains("/series/", ignoreCase = true) -> 0
                                SeriesCatalogParser.seasonNumber(title, href) == null &&
                                    SeriesCatalogParser.partNumber(title, href) == null -> 1
                                else -> 2
                            }
                        }
                        .take(4)

                    var selectedParentRank = -1
                    var selectedParentEpisodeCount = rawEpisodes.size
                    val parentCompletion = ExecutorCompletionService<Triple<String, String, org.jsoup.nodes.Document>>(seasonPagePool)
                    val parentFutures = parents.map { (parentHref, parentTitle) ->
                        parentCompletion.submit(Callable {
                            val parentHtml = fetchSeasonHtml(parentHref)
                            if (csrfTokenRef.get().isBlank()) {
                                Regex("""csrf[_]{1,2}token['"]?\s*:\s*["']([A-Za-z0-9_-]+)["']""")
                                    .find(parentHtml)?.groupValues?.getOrNull(1)?.let { found ->
                                        csrfTokenRef.compareAndSet("", found)
                                    }
                            }
                            val parentDoc = Jsoup.parse(parentHtml, BASE_URL)
                            parentDoc.select("footer, script, style").remove()
                            Triple(parentHref, parentTitle, parentDoc)
                        })
                    }
                    val parentResults = mutableListOf<Triple<String, String, org.jsoup.nodes.Document>>()
                    val parentDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                    repeat(parentFutures.size) {
                        val remaining = parentDeadline - System.nanoTime()
                        if (remaining <= 0L) return@repeat
                        val finished = try {
                            parentCompletion.poll(remaining, TimeUnit.NANOSECONDS) ?: return@repeat
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return@repeat
                        }
                        runCatching { finished.get() }.getOrNull()?.let(parentResults::add)
                    }
                    parentFutures.filterNot { it.isDone }.forEach { it.cancel(true) }

                    val orderedParents = parentResults.sortedWith(
                        compareByDescending<Triple<String, String, org.jsoup.nodes.Document>> {
                            SeriesCatalogParser.seasonNumber(it.second, it.first) == null &&
                                SeriesCatalogParser.partNumber(it.second, it.first) == null
                        }.thenByDescending { collectEpisodes(it.third).size },
                    )
                    for ((parentHref, parentTitle, parentDoc) in orderedParents) {
                        if (!SeriesCatalogParser.pageBelongsToSeries(
                                targetTitles = targetIdentityTitles,
                                targetPageUrls = targetIdentityUrls,
                                candidateTitles = listOf(parentTitle),
                                candidatePageUrls = listOf(parentHref),
                            )
                        ) continue
                        val parentEps = collectEpisodes(parentDoc)
                        val parentContainsCurrentEpisode = parentEps.any {
                            SeriesCatalogParser.normalizedUrl(it.second) == currentPage
                        }
                        // Use parent if it has more episodes OR has higher episode numbers
                        // (MySeed /selary/ may show only the latest episode, e.g. ep12,
                        // while the series page shows ep1-ep2 â€” the parent indicates more episodes exist)
                        val parentMaxEpNum = parentEps.mapNotNull { (t, u, _) ->
                            SeriesCatalogParser.episodeNumber(t, u)
                        }.maxOrNull() ?: 0
                        val currentMaxEpNum = rawEpisodes.mapNotNull { (t, u, _) ->
                            SeriesCatalogParser.episodeNumber(t, u)
                        }.maxOrNull() ?: 0
                        val shouldUseParent = parentEps.isNotEmpty() && (
                            if (isEpisodePage) parentContainsCurrentEpisode
                            else parentEps.size > rawEpisodes.size || parentMaxEpNum > currentMaxEpNum
                        )
                        if (!shouldUseParent) continue
                        val isCanonical = SeriesCatalogParser.seasonNumber(parentTitle, parentHref) == null &&
                            SeriesCatalogParser.partNumber(parentTitle, parentHref) == null
                        val rank = if (isCanonical) 2 else 1
                        if (rank > selectedParentRank ||
                            (rank == selectedParentRank && parentEps.size > selectedParentEpisodeCount)
                        ) {
                            // Merge parent episodes with existing ones to avoid losing data
                            // when parent only shows the latest episode (e.g. MySeed /selary/ page)
                            val mergedEps = if (parentEps.size >= rawEpisodes.size) {
                                parentEps
                            } else {
                                val byKey = linkedMapOf<String, Triple<String, String, Int?>>()
                                rawEpisodes.forEach { byKey[SeriesCatalogParser.normalizedUrl(it.second)] = it }
                                parentEps.forEach { byKey[SeriesCatalogParser.normalizedUrl(it.second)] = it }
                                byKey.values.toMutableList()
                            }
                            rawEpisodes.clear()
                            rawEpisodes.addAll(mergedEps)
                            selectedParentRank = rank
                            selectedParentEpisodeCount = mergedEps.size
                        }
                        seasonDiscoveryDocs += parentDoc
                        if (isCanonical) break
                    }
                }

                // ── MySeed Season AJAX (/season__episodes/) FIRST ───────────────────────
                // MySeed series & episode pages only pre-render the active season HTML and
                // load every other `#seasons__list li[data-term]` dynamically via AJAX POST.
                // Run AJAX before HTML season-tab GETs so complete seasons need 0 extra page loads.
                val ajaxLoadedSeasons = mutableSetOf<Int>()
                val csrfToken = csrfTokenRef.get()
                if (csrfToken.isNotBlank()) {
                    val existingNumbersBySeasonAjax = rawEpisodes.mapNotNull { (title, url, seasonNum) ->
                        val resolvedS = SeriesCatalogParser.seasonNumber(title, url) ?: seasonNum ?: return@mapNotNull null
                        val epNum = SeriesCatalogParser.episodeNumber(title, url, allowNumericTitle = true) ?: return@mapNotNull null
                        resolvedS to epNum
                    }.groupBy({ it.first }, { it.second })

                    val termTargets = linkedMapOf<Int, String>()
                    seasonDiscoveryDocs.asReversed().forEach { discoveryDoc ->
                        discoveryDoc.select("#seasons__list li[data-term], li[data-term][data-url]").forEach { li ->
                            val termId = li.attr("data-term").trim()
                            val label = li.text().trim()
                            val tabUrl = li.attr("abs:data-url").ifBlank { li.attr("data-url") }.trim()
                            val sNum = SeriesCatalogParser.seasonNumber(label, tabUrl)
                                ?: SeriesCatalogParser.seasonNumber(label, "")
                            if (termId.isNotBlank() && sNum != null && sNum in 1..60 &&
                                (seasonNumbersOnly == null || sNum in seasonNumbersOnly)
                            ) {
                                val currentNums = existingNumbersBySeasonAjax[sNum].orEmpty()
                                val needsSeasonFetch = forceRefresh || currentNums.size < 2 ||
                                    SeriesCatalogParser.needsEpisodeSequenceRepair(currentNums)
                                if (needsSeasonFetch) termTargets.putIfAbsent(sNum, termId)
                            }
                        }
                    }

                    if (termTargets.isNotEmpty()) {
                        val ajaxOrigin = runCatching {
                            val u = java.net.URI(item.pageUrl)
                            if (u.scheme in setOf("http", "https") && !u.host.isNullOrBlank()) "${u.scheme}://${u.host}" else BASE_URL
                        }.getOrDefault(BASE_URL).trimEnd('/')
                        val ajaxCompletion = ExecutorCompletionService<Pair<Int, List<Triple<String, String, Int?>>>>(seasonPagePool)
                        val ajaxFutures = termTargets.entries.map { (sNum, termId) ->
                            ajaxCompletion.submit(Callable {
                                val form = FormBody.Builder()
                                    .add("season_id", termId)
                                    .add("csrf_token", csrfToken)
                                    .build()
                                val safePageRef = SeriesCatalogParser.asciiSafeHeaderValue(item.pageUrl)
                                val req = Request.Builder()
                                    .url(SeriesCatalogParser.asciiSafeUrl("$ajaxOrigin/season__episodes/"))
                                    .post(form)
                                    .header("User-Agent", UA)
                                    .apply { if (safePageRef.isNotBlank()) header("Referer", safePageRef) }
                                    .header("Origin", ajaxOrigin)
                                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .build()
                                val bodyStr = runCatching {
                                    val call = client.newCall(req)
                                    call.timeout().timeout(5, TimeUnit.SECONDS)
                                    call.execute().use { r -> if (r.isSuccessful) r.body?.string().orEmpty() else "" }
                                }.getOrDefault("")
                                val innerHtml = runCatching { JSONObject(bodyStr).optString("html") }
                                    .getOrDefault("")
                                    .ifBlank { if (bodyStr.trimStart().startsWith("<")) bodyStr else "" }
                                if (innerHtml.isBlank()) return@Callable sNum to emptyList()
                                val epDoc = Jsoup.parse(innerHtml, ajaxOrigin)
                                val parsed = epDoc.select("a[href]").mapNotNull { a ->
                                    val href = a.absUrl("href").ifBlank { a.attr("href") }.trim()
                                    if (!href.startsWith("http") || href.contains("/category/") || href.contains("/tag/")) return@mapNotNull null
                                    val txt = a.text().replace(Regex("""\s+"""), " ").trim().ifBlank { a.attr("title").trim() }
                                    if (SeriesCatalogParser.hasConflictingSeasonNumbers(txt, href)) return@mapNotNull null
                                    val epNum = SeriesCatalogParser.episodeNumber(txt, href, allowNumericTitle = true)
                                        ?: return@mapNotNull null
                                    val linkSeason = SeriesCatalogParser.seasonNumber(txt, href)
                                    val resolvedSeason = linkSeason ?: sNum
                                    if (resolvedSeason != sNum) return@mapNotNull null
                                    Triple(txt.ifBlank { "الحلقة $epNum" }, href, resolvedSeason)
                                }
                                sNum to parsed
                            })
                        }
                        val ajaxDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7)
                        var remainingAjax = ajaxFutures.size
                        while (remainingAjax > 0) {
                            val left = ajaxDeadline - System.nanoTime()
                            if (left <= 0L) break
                            val done = try { ajaxCompletion.poll(left, TimeUnit.NANOSECONDS) ?: break }
                            catch (_: InterruptedException) { Thread.currentThread().interrupt(); break }
                            remainingAjax--
                            val (sNum, parsedList) = runCatching { done.get() }.getOrNull() ?: continue
                            if (parsedList.isNotEmpty()) ajaxLoadedSeasons += sNum
                            for (ep in parsedList) {
                                val urlKey = SeriesCatalogParser.normalizedUrl(ep.second)
                                val existingIdx = rawEpisodes.indexOfFirst {
                                    SeriesCatalogParser.normalizedUrl(it.second) == urlKey
                                }
                                if (existingIdx < 0) rawEpisodes += ep
                                else rawEpisodes[existingIdx] = ep.copy(third = sNum)
                            }
                            android.util.Log.i(TAG, "MySeed AJAX season=$sNum loaded=${parsedList.size} episodes")
                        }
                        ajaxFutures.filterNot { it.isDone }.forEach { it.cancel(true) }
                    }
                }

                // Some series pages only render the active season's episode list while
                // the remaining seasons are linked in tabs. Fetch any remaining incomplete season pages
                // concurrently and let the season page provide the missing group context.
                val seasonPages = seasonDiscoveryDocs.asReversed()
                    .flatMap { document ->
                        SeriesCatalogParser.linkedSeasonPages(document, item.pageUrl) { pageUrl ->
                            MultiProviderRepository.isProviderContentUrl(pageUrl, item.pageUrl)
                        }
                    }
                    .distinctBy { it.first }
                val seasonPageUrlByNumber = seasonPages.groupBy { it.first }
                    .mapValues { (_, rows) -> rows.first().second }
                val visibleEpisodeNumbers = rawEpisodes.mapNotNull { (title, url, seasonNumber) ->
                    val episodeNumber = SeriesCatalogParser.episodeNumber(
                        title,
                        url,
                        allowNumericTitle = seasonNumber != null,
                    ) ?: return@mapNotNull null
                    val resolvedSeason = SeriesCatalogParser.seasonNumber(title, url)
                        ?: seasonNumber ?: return@mapNotNull null
                    resolvedSeason to episodeNumber
                }.groupBy({ it.first }, { it.second })
                val seasonPagesToFetch = seasonPages
                    .filter { (number, _) ->
                        val knownNums = visibleEpisodeNumbers[number].orEmpty()
                        number != requestedSeasonPageNumber &&
                            (seasonNumbersOnly == null || number in seasonNumbersOnly) &&
                            (SeriesCatalogParser.needsEpisodeSequenceRepair(knownNums) ||
                                (forceRefresh && number !in ajaxLoadedSeasons))
                    }
                    .take(
                        when {
                            seasonNumbersOnly != null -> 1
                            forceRefresh -> 20
                            else -> 12
                        },
                    )
                val seasonPageCompletion = ExecutorCompletionService<Pair<Int, List<Triple<String, String, Int?>>>>(seasonPagePool)
                val seasonPageFutures = seasonPagesToFetch.map { (seasonNumber, seasonUrl) ->
                    seasonPageCompletion.submit(Callable {
                        val seasonDoc = Jsoup.parse(fetchSeasonHtml(seasonUrl), BASE_URL)
                        seasonDoc.select("footer, script, style").remove()
                        val paginated = SeriesCatalogParser.collectPaginatedEpisodesWithMore(
                            firstDocument = seasonDoc,
                            firstPageUrl = seasonUrl,
                            fetchDocument = { pageUrl ->
                                fetchSeasonHtml(pageUrl).takeIf(String::isNotBlank)?.let { Jsoup.parse(it, pageUrl) }
                            },
                            parseEpisodes = { page ->
                                collectEpisodes(page).map { (title, url, number) ->
                                    Triple(title, url, number ?: seasonNumber)
                                }
                            },
                            maxPages = when {
                                seasonNumbersOnly != null -> 1
                                forceRefresh -> 10
                                else -> 4
                            },
                            timeoutSeconds = when {
                                seasonNumbersOnly != null -> 4
                                forceRefresh -> 8
                                else -> 5
                            },
                        )
                        seasonHasMorePages[seasonNumber] = paginated.hasMorePages
                        seasonNumber to paginated.episodes
                    })
                }
                // Fetch linked season tabs concurrently with enough headroom for long series.
                val seasonPageWaitSeconds = when {
                    seasonNumbersOnly != null -> 4L
                    forceRefresh && seasonPagesToFetch.size >= 12 -> 10L
                    seasonPagesToFetch.size >= 12 -> 7L
                    seasonPagesToFetch.size >= 4 -> 6L
                    else -> 5L
                }
                val seasonPageDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seasonPageWaitSeconds)
                val seasonPageAssignments = mutableSetOf<String>()
                var unfinishedSeasonPages = seasonPageFutures.size
                while (unfinishedSeasonPages > 0) {
                    val remaining = seasonPageDeadline - System.nanoTime()
                    if (remaining <= 0L) break
                    val finished = try {
                        seasonPageCompletion.poll(remaining, TimeUnit.NANOSECONDS) ?: break
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    unfinishedSeasonPages--
                    val result = runCatching { finished.get() }.getOrNull() ?: continue
                    val (linkedSeasonNumber, linkedEpisodes) = result
                    for ((title, url, parsedSeason) in linkedEpisodes) {
                        val episodeNumber = SeriesCatalogParser.episodeNumber(
                            title,
                            url,
                            allowNumericTitle = parsedSeason != null,
                        ) ?: continue
                        val urlKey = SeriesCatalogParser.normalizedUrl(url)
                        val explicitSeason = SeriesCatalogParser.seasonNumber(title, url) ?: parsedSeason
                        if (explicitSeason != null && explicitSeason != linkedSeasonNumber) continue
                        val resolvedSeason = explicitSeason ?: linkedSeasonNumber
                        val existingIndex = rawEpisodes.indexOfFirst {
                            SeriesCatalogParser.normalizedUrl(it.second) == urlKey
                        }
                        if (existingIndex < 0) {
                            rawEpisodes += Triple(title, url, resolvedSeason)
                            seasonPageAssignments += urlKey
                        } else if (urlKey !in seasonPageAssignments) {
                            rawEpisodes[existingIndex] = Triple(title, url, resolvedSeason)
                            seasonPageAssignments += urlKey
                        }
                    }
                }
                seasonPageFutures.forEach { if (!it.isDone) it.cancel(true) }

                // ── MySeed direct URL probing ──────────────────────────────────────────────
                // MySeed uses predictable episode URL patterns like:
                //   /مسلسل-doctor-x-s1-eps3/  or  /مسلسل-حب-اسود-s1-eps3/
                // The search endpoint only surfaces the latest 1-2 episodes, so we build
                // episode URLs directly by probing ep1..ep40 with HEAD requests.
                run {
                    val rawUrl = item.pageUrl.trimEnd('/')
                    val slugWithEps = rawUrl.substringAfterLast('/')
                    val epsSuffix = Regex("""-s(\d+)-eps(\d+)$""", RegexOption.IGNORE_CASE).find(slugWithEps)
                    val epsOnlySuffix = Regex("""-eps(\d+)$""", RegexOption.IGNORE_CASE).find(slugWithEps)
                    val (slugBase, detectedSeason) = when {
                        epsSuffix != null -> {
                            val sn = epsSuffix.groupValues[1].toIntOrNull()
                            if (sn == null) null to null else {
                                val base = slugWithEps.substring(0, epsSuffix.range.first)
                                base to sn
                            }
                        }
                        // `-epsN` alone does not identify a season; never guess season 1.
                        epsOnlySuffix != null -> null to null
                        else -> null to null
                    }
                    val existingSeasonNums = if (detectedSeason != null) {
                        rawEpisodes.filter { it.third == null || it.third == detectedSeason }
                            .mapNotNull { SeriesCatalogParser.episodeNumber(it.first, it.second) }
                            .distinct()
                            .sorted()
                    } else emptyList()
                    val needsGapRepair = detectedSeason != null &&
                        SeriesCatalogParser.needsEpisodeSequenceRepair(existingSeasonNums)
                    if (slugBase != null && detectedSeason != null && slugBase.length > 3 &&
                        (rawEpisodes.size < 5 || needsGapRepair)
                    ) {
                        android.util.Log.i(TAG, "MySeed direct probe: slugBase=$slugBase season=$detectedSeason currentRaw=${rawEpisodes.size}")
                        val baseDir = rawUrl.substringBeforeLast('/').trimEnd('/')
                        val baseEpUrl = "$baseDir/${slugBase}-s${detectedSeason}-eps"
                        fun probeWave(targets: List<Int>, waitSec: Long): List<Pair<Int, String>> {
                            if (targets.isEmpty()) return emptyList()
                            val ecs = ExecutorCompletionService<Pair<Int, String>?>(seasonPagePool)
                            val futures = targets.map { epNum ->
                                ecs.submit(Callable<Pair<Int, String>?> {
                                    val epUrl = "$baseEpUrl$epNum/"
                                    try {
                                        val req = Request.Builder().url(SeriesCatalogParser.asciiSafeUrl(epUrl)).head()
                                            .header("User-Agent", UA).header("Referer", BASE_SLASH).build()
                                        val call = client.newCall(req)
                                        call.timeout().timeout(3, TimeUnit.SECONDS)
                                        call.execute().use { resp ->
                                            if (resp.isSuccessful) epNum to epUrl else null
                                        }
                                    } catch (_: Exception) { null }
                                })
                            }
                            val found = mutableListOf<Pair<Int, String>>()
                            val dl = System.nanoTime() + TimeUnit.SECONDS.toNanos(waitSec)
                            var rem = futures.size
                            while (rem > 0) {
                                val left = dl - System.nanoTime()
                                if (left <= 0L) break
                                val done = runCatching { ecs.poll(left, TimeUnit.NANOSECONDS) }.getOrNull() ?: break
                                rem--
                                runCatching { done.get() }.getOrNull()?.let(found::add)
                            }
                            futures.filterNot { it.isDone }.forEach { it.cancel(true) }
                            return found
                        }
                        val wave1Targets = if (rawEpisodes.size < 5) {
                            val hi = maxOf(16, (existingSeasonNums.maxOrNull() ?: 16).coerceAtMost(24))
                            (1..hi).filter { it !in existingSeasonNums }
                        } else {
                            val hi = (existingSeasonNums.maxOrNull() ?: 0).coerceAtMost(40)
                            (1..hi).filter { it !in existingSeasonNums }.take(24)
                        }
                        val wave1 = probeWave(wave1Targets, 3L)
                        val wave2 = if (rawEpisodes.size < 5 && wave1.any { it.first >= 14 }) {
                            probeWave((17..40).filter { it !in existingSeasonNums && wave1.none { w -> w.first == it } }, 3L)
                        } else emptyList()
                        for ((epNum, epUrl) in (wave1 + wave2).sortedBy { it.first }) {
                            val urlKey = SeriesCatalogParser.normalizedUrl(epUrl)
                            if (rawEpisodes.none { SeriesCatalogParser.normalizedUrl(it.second) == urlKey }) {
                                rawEpisodes += Triple("الحلقة $epNum", epUrl, detectedSeason)
                            }
                        }
                        android.util.Log.i(TAG, "MySeed direct probe done: rawEpisodes=${rawEpisodes.size}")
                    }
                }

                // Keep season identity attached to each link. Only infer one shared season when
                // the source gives no season labels at all; with multiple labelled seasons,
                // unlabeled sidebar/recommendation links are omitted instead of mixed in.
                val seasons = mutableListOf<ArabseedSeason>()
                if (rawEpisodes.isNotEmpty()) {
                    val expectedSeasonNumber = pageSeasonNumber
                        ?: if (seasonNumbersOnly == null && (isEpisodePage || seasonPages.isEmpty())) {
                            SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")
                        } else null
                    val parsedEpisodes = rawEpisodes.mapNotNull { (title, url, domSeasonNumber) ->
                        val episodeNumber = SeriesCatalogParser.episodeNumber(
                            title,
                            url,
                            allowNumericTitle = domSeasonNumber != null,
                        ) ?: return@mapNotNull null
                        val seasonNumber = domSeasonNumber
                            ?: SeriesCatalogParser.seasonNumber(title, url)
                        Triple(seasonNumber, episodeNumber, url)
                    }.distinctBy { (_, _, url) -> SeriesCatalogParser.normalizedUrl(url) }
                    val labelledSeasons = parsedEpisodes.mapNotNull { it.first }.distinct()
                    val inferredSeasonNumber = when {
                        labelledSeasons.size > 1 -> null
                        labelledSeasons.size == 1 -> labelledSeasons.single()
                        requestedSeasonPageNumber != null -> requestedSeasonPageNumber
                        seasonNumbersOnly != null -> null
                        expectedSeasonNumber != null -> expectedSeasonNumber
                        else -> null
                    }
                    val numberedEpisodes = parsedEpisodes.mapNotNull { (seasonNumber, episodeNumber, url) ->
                        (seasonNumber ?: inferredSeasonNumber)?.takeIf { resolvedSeason ->
                            (seasonNumbersOnly == null || resolvedSeason in seasonNumbersOnly) &&
                                (seasonPages.isEmpty() || resolvedSeason in seasonPageUrlByNumber ||
                                    pageSeasonNumber == resolvedSeason || resolvedSeason in ajaxLoadedSeasons ||
                                    seasonNumber != null)
                        }?.let { Triple(it, episodeNumber, url) }
                    }
                    val finalEpisodeLimits = rawEpisodes.mapNotNull { (title, url, domSeason) ->
                        val finalNumber = SeriesCatalogParser.finalEpisodeNumber(title, url)
                            ?: return@mapNotNull null
                        val seasonNumber = SeriesCatalogParser.seasonNumber(title, url)
                            ?: domSeason ?: requestedSeasonPageNumber ?: inferredSeasonNumber
                            ?: return@mapNotNull null
                        seasonNumber to finalNumber
                    }.groupBy({ it.first }, { it.second }).mapValues { (_, numbers) -> numbers.minOrNull()!! }
                    val boundedEpisodes = numberedEpisodes.filter { (seasonNumber, episodeNumber) ->
                        episodeNumber <= (finalEpisodeLimits[seasonNumber] ?: Int.MAX_VALUE)
                    }

                    boundedEpisodes.groupBy { it.first }.toSortedMap().forEach { (sNum, rows) ->
                        val sortedEps = rows
                            .groupBy { it.second }
                            .toSortedMap()
                            .map { (episodeNumber, variants) ->
                                val url = variants.first().third
                                val alternateUrls = variants.map { it.third }
                                    .distinctBy(SeriesCatalogParser::normalizedUrl)
                                    .filter { SeriesCatalogParser.normalizedUrl(it) != SeriesCatalogParser.normalizedUrl(url) }
                                ArabseedEpisode(
                                    episodeNumber,
                                    "الحلقة $episodeNumber",
                                    url,
                                    sNum,
                                    item.providerName,
                                    alternateUrls,
                                )
                            }
                        seasons += ArabseedSeason(
                            number = sNum,
                            title = SeriesCatalogParser.seasonTitle(sNum),
                            episodes = sortedEps,
                            pageUrl = seasonPageUrlByNumber[sNum].orEmpty().ifBlank {
                                item.pageUrl.takeIf { pageSeasonNumber == sNum }.orEmpty()
                            },
                            hasMorePages = seasonHasMorePages[sNum] ?: false,
                        )
                    }
                }

                for ((seasonNumber, seasonUrl) in seasonPages) {
                    val existingIndex = seasons.indexOfFirst { it.number == seasonNumber }
                    if (existingIndex >= 0 && seasons[existingIndex].pageUrl.isBlank()) {
                        seasons[existingIndex] = seasons[existingIndex].copy(pageUrl = seasonUrl)
                    }
                }


                // ── Fallback discovery for series where each episode is a separate post ──
                // Also trigger when episodes are few or have gaps, or when seasons are missing (e.g. only season 3 & 4 found, or missing season 1).
                val detectedSeason = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
                    ?: SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")
                val maxSeasonFound = seasons.maxOfOrNull { it.number } ?: 0
                val minSeasonFound = seasons.minOfOrNull { it.number } ?: 0
                val hasSeasonGaps = seasons.map { it.number }.sorted().zipWithNext().any { (a, b) -> b - a > 1 }
                val hasEpisodeGaps = seasons.any { season ->
                    SeriesCatalogParser.needsEpisodeSequenceRepair(season.episodes.map { it.number })
                }
                val isMissingLowerSeasons = minSeasonFound > 1
                val isMissingDetectedSeason = detectedSeason != null && seasons.none { it.number == detectedSeason }
                val isMissingInterveningSeasons = maxSeasonFound > seasons.size

                val seasonsNeedSearch = forceRefresh ||
                    seasons.isEmpty() ||
                    seasons.all { it.episodes.isEmpty() } ||
                    isMissingLowerSeasons ||
                    isMissingDetectedSeason ||
                    isMissingInterveningSeasons ||
                    hasSeasonGaps ||
                    hasEpisodeGaps
                val rawTitleCandidate = item.parentSeriesTitle?.takeIf(String::isNotBlank)
                    ?: item.title.takeIf { !it.contains("الموسم") || it.length > 15 }
                    ?: run {
                        val slug = item.pageUrl.trimEnd('/').substringAfterLast('/')
                        val unslug = runCatching { java.net.URLDecoder.decode(slug, "UTF-8") }.getOrDefault(slug)
                        unslug.replace(Regex("""-s\d+.*"""), "").replace('-', ' ').trim()
                    }
                android.util.Log.i(TAG, "seasonsNeedSearch=$seasonsNeedSearch episodeCount=${seasons.sumOf { it.episodes.size }} rawTitleCandidate=$rawTitleCandidate")
                if (seasonsNeedSearch) {
                    val cleanBaseTitle = SeriesCatalogParser.cleanSeriesTitle(rawTitleCandidate)
                    // Also try the title without common episode-number noise so search hits more
                    val strippedTitle = cleanBaseTitle
                        .replace(Regex("""(?i)^(مسلسل|سلسلة)\s+"""), "")
                        .replace(Regex("""(?i)\s+(الحلقة|حلقة|ep\.?|episode)\s*\d+.*"""), "")
                        .trim()
                    val colonPrefix = cleanBaseTitle.substringBefore(':').trim()
                    val dashPrefix = cleanBaseTitle.substringBefore('-').trim()
                    val searchCandidates = buildList {
                        if (colonPrefix.length >= 2 && colonPrefix != cleanBaseTitle) add(colonPrefix)
                        if (dashPrefix.length >= 2 && dashPrefix != cleanBaseTitle && dashPrefix != colonPrefix) add(dashPrefix)
                        if (strippedTitle.length >= 2 && strippedTitle != cleanBaseTitle) add(strippedTitle)
                        if (cleanBaseTitle.length >= 2) add(cleanBaseTitle)
                    }.distinct().take(2)
                    val searchSeasonContext = if (isEpisodePage) null else {
                        pageSeasonNumber ?: SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")
                    }
                    android.util.Log.i(TAG, "searchCandidates for '$cleanBaseTitle': $searchCandidates")
                    val discoveredEpisodes = mutableListOf<Triple<String, String, Int?>>()
                    val fetchedSearchItems = mutableListOf<ArabseedItem>()
                    for (query in searchCandidates) {
                        try {
                            val results = search(query, forceRefresh = true, maxPages = 4)
                            fetchedSearchItems.addAll(results)
                            android.util.Log.i(TAG, "search query='$query' returned ${results.size} items: ${results.take(4).map { it.title }}")
                            for (sr in results) {
                                val srEpNum = SeriesCatalogParser.episodeNumber(sr.title, sr.pageUrl)
                                if (srEpNum != null && SeriesCatalogParser.isEpisode(sr.title, sr.pageUrl)) {
                                    val titlesMatch = SeriesCatalogParser.pageBelongsToSeries(
                                        targetTitles = listOfNotNull(
                                            item.parentSeriesTitle,
                                            item.title,
                                            item.arabicTitle,
                                            cleanBaseTitle,
                                            strippedTitle,
                                        ),
                                        targetPageUrls = listOfNotNull(item.parentSeriesPageUrl, item.pageUrl),
                                        candidateTitles = listOfNotNull(sr.parentSeriesTitle, sr.title, sr.arabicTitle),
                                        candidatePageUrls = listOf(sr.pageUrl),
                                    )
                                    if (titlesMatch) {
                                        val sNum = SeriesCatalogParser.seasonNumber(sr.title, sr.pageUrl)
                                            ?: searchSeasonContext
                                            ?: continue
                                        if (seasonNumbersOnly == null || sNum in seasonNumbersOnly) {
                                            discoveredEpisodes += Triple(sr.title, sr.pageUrl, sNum)
                                        }
                                    }
                                }
                            }
                            if (discoveredEpisodes.isNotEmpty()) break
                        } catch (_: Exception) {}
                    }
                    // Save fetched results in searchCache so relatedParts reuses them with 0 extra HTTP requests
                    if (fetchedSearchItems.isNotEmpty() && cleanBaseTitle.isNotBlank()) {
                        searchCache[cleanBaseTitle.lowercase()] = System.currentTimeMillis() to fetchedSearchItems.distinctBy { it.pageUrl }
                    }
                    if (discoveredEpisodes.isNotEmpty()) {
                        val newSeasons = mutableListOf<ArabseedSeason>()
                        discoveredEpisodes.distinctBy { SeriesCatalogParser.normalizedUrl(it.second) }
                            .groupBy { it.third ?: 1 }
                            .toSortedMap()
                            .forEach { (sNum, eps) ->
                                val sorted = eps.mapNotNull { (title, url, _) ->
                                    val epNum = SeriesCatalogParser.episodeNumber(title, url) ?: return@mapNotNull null
                                    ArabseedEpisode(
                                        number = epNum,
                                        title = "الحلقة $epNum",
                                        pageUrl = url,
                                        seasonNumber = sNum,
                                        providerName = item.providerName
                                    )
                                }.distinctBy { it.number }.sortedBy { it.number }
                                if (sorted.isNotEmpty()) {
                                    // Merge with existing season episodes to avoid losing already-fetched data
                                    val existingSeason = seasons.firstOrNull { it.number == sNum }
                                    val mergedEpisodes = if (existingSeason != null) {
                                        val byNumber = linkedMapOf<Int, ArabseedEpisode>()
                                        existingSeason.episodes.forEach { byNumber[it.number] = it }
                                        sorted.forEach { byNumber[it.number] = it }
                                        byNumber.values.sortedBy { it.number }
                                    } else sorted
                                    val seasonPage = existingSeason?.pageUrl?.takeIf { it.isNotBlank() }
                                        ?: item.pageUrl.takeIf { pageSeasonNumber == sNum }.orEmpty()
                                    newSeasons += ArabseedSeason(
                                        number = sNum,
                                        title = SeriesCatalogParser.seasonTitle(sNum),
                                        episodes = mergedEpisodes,
                                        pageUrl = seasonPage
                                    )
                                }
                            }
                        if (newSeasons.isNotEmpty()) {
                            val allSeasonNumbers = (seasons.map { it.number } + newSeasons.map { it.number }).distinct().sorted()
                            val mergedAllSeasons = allSeasonNumbers.mapNotNull { num ->
                                val fromNew = newSeasons.firstOrNull { it.number == num }
                                val fromExisting = seasons.firstOrNull { it.number == num }
                                when {
                                    fromNew != null && fromExisting != null -> {
                                        val byNumber = linkedMapOf<Int, ArabseedEpisode>()
                                        fromExisting.episodes.forEach { byNumber[it.number] = it }
                                        fromNew.episodes.forEach { byNumber[it.number] = it }
                                        ArabseedSeason(
                                            number = num,
                                            title = fromNew.title.ifBlank { fromExisting.title },
                                            episodes = byNumber.values.sortedBy { it.number },
                                            pageUrl = fromExisting.pageUrl.ifBlank { fromNew.pageUrl }
                                        )
                                    }
                                    fromNew != null -> fromNew
                                    fromExisting != null -> fromExisting
                                    else -> null
                                }
                            }
                            seasons.clear()
                            seasons.addAll(mergedAllSeasons)
                        }
                    } else if (isEpisodePage && seasons.sumOf { it.episodes.size } < 3) {
                        // Current page itself is an episode but no search results found.
                        // Only overwrite if we have almost nothing â€” keep any existing seasons.
                        val epNum = SeriesCatalogParser.episodeNumber(item.title, item.pageUrl)
                        val sNum = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
                        val existingSeason = sNum?.let { number -> seasons.firstOrNull { it.number == number } }
                        if (epNum != null && sNum != null && existingSeason == null) {
                            seasons += ArabseedSeason(
                                number = sNum,
                                title = SeriesCatalogParser.seasonTitle(sNum),
                                episodes = listOf(
                                    ArabseedEpisode(
                                        number = epNum,
                                        title = "الحلقة $epNum",
                                        pageUrl = item.pageUrl,
                                        seasonNumber = sNum,
                                        providerName = item.providerName
                                    )
                                ),
                                pageUrl = item.pageUrl,
                                hasMorePages = false,
                            )
                        }
                    }
                }

                // Find related parts/seasons linked from the page (EXCLUDING any episodes)
                val relatedParts = mutableListOf<ArabseedItem>()
                val explicitPartSectionUrls = mutableSetOf<String>()
                val partSelectors = listOf(
                    ".parts-list a", ".parts a",
                    "a[href*='%d8%a7%d9%84%d8%ac%d8%b2%d8%a1']", "a[href*='الجزء']",
                    "a[href*='part']"
                )
                for (sel in partSelectors) {
                    for (el in doc.select(sel)) {
                        if (SeriesCatalogParser.isInsideUnrelatedContentContainer(el)) continue
                        val href = el.attr("abs:href").ifBlank { el.attr("href") }
                        val txt = el.text().trim().ifBlank { el.attr("title").trim() }
                        if (href.isNotBlank() && href.startsWith(P_HTTP)
                            && href != item.pageUrl && txt.isNotBlank()
                        ) {
                            val isEpisodeLike = SeriesCatalogParser.isEpisode(txt, href)
                            val isPart = SeriesCatalogParser.hasPartMarker(txt, href)
                            val isSeason = SeriesCatalogParser.seasonNumber(txt, href) != null
                            val inExplicitPartSection = sel.startsWith(".")
                            if (!isEpisodeLike && (isPart || isSeason || inExplicitPartSection) &&
                                relatedParts.none { it.pageUrl == href }
                            ) {
                                val img = el.selectFirst("img")?.attr("abs:src")
                                    ?.ifBlank { el.selectFirst("img")?.attr("src") }
                                    ?: item.posterUrl
                                if (inExplicitPartSection) {
                                    explicitPartSectionUrls += SeriesCatalogParser.normalizedUrl(href)
                                }
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

                // Additional smart discovery: only run search when page exposed no parts/seasons yet.
                val cleanBaseTitle = SeriesCatalogParser.cleanSeriesTitle(item.title)

                if (cleanBaseTitle.length >= 3 && (forceRefresh || (relatedParts.isEmpty() && seasons.size <= 1 && seasons.sumOf { it.episodes.size } < 3))) {
                    try {
                        val searchResults = search(cleanBaseTitle, forceRefresh = false, maxPages = 2)
                        for (sr in searchResults) {
                            val srTitle = sr.title
                            val srUrl = sr.pageUrl
                            val isEpisode = SeriesCatalogParser.isEpisode(srTitle, srUrl)
                            val isPart = SeriesCatalogParser.hasPartMarker(srTitle, srUrl)
                            val isSeason = SeriesCatalogParser.seasonNumber(srTitle, srUrl) != null

                            if ((isPart || isSeason) && !isEpisode && srUrl != item.pageUrl &&
                                srTitle.contains(cleanBaseTitle, ignoreCase = true)
                            ) {
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
                        !SeriesCatalogParser.isEpisode(part.title, part.pageUrl) &&
                            (SeriesCatalogParser.hasPartMarker(part.title, part.pageUrl) ||
                                SeriesCatalogParser.seasonNumber(part.title, part.pageUrl) != null ||
                                SeriesCatalogParser.normalizedUrl(part.pageUrl) in explicitPartSectionUrls)
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

                if (bundle.seasons.isNotEmpty() || bundle.relatedParts.isNotEmpty()) {
                    seasonBundleCache[item.pageUrl] = bundle
                    seasonBundleCacheAt[item.pageUrl] = System.currentTimeMillis()
                    CatalogDiskCache.saveSeasonBundle("arabseed:seasons:v29:" + item.pageUrl, bundle)
                }

                bundle

            } catch (_: Exception) {

                ArabseedSeasonBundle(emptyList())

            }

        }



    /** Fetch a small page batch for the selected season and return its next pager URL. */
    fun fetchMoreSeasonEpisodes(
        seasonUrl: String,
        cursorUrl: String,
        seasonNumber: Int,
        knownEpisodeUrls: Set<String>,
        pageLimit: Int = 4,
        episodeLimit: Int = 60,
    ): Pair<ArabseedSeasonBundle, String?> {
        val seasonUri = runCatching { java.net.URI(seasonUrl.trim()) }.getOrNull()
            ?: return ArabseedSeasonBundle(emptyList()) to null
        if (seasonNumber !in 1..60 || seasonUri.scheme !in setOf("http", "https") || seasonUri.host.isNullOrBlank() ||
            !MultiProviderRepository.isProviderContentUrl(seasonUrl, seasonUrl)
        ) return ArabseedSeasonBundle(emptyList()) to null

        val known = knownEpisodeUrls.map(SeriesCatalogParser::normalizedUrl)
            .filter(String::isNotBlank).toSet()
        val seasonSeriesIdentity = SeriesCatalogParser.seriesIdentityFromUrl(seasonUrl)
        val added = linkedMapOf<String, ArabseedEpisode>()
        val visited = linkedSetOf<String>()
        var pageUrl = cursorUrl.trim().ifBlank { seasonUrl.trim() }
        var nextPageUrl: String? = null
        var pagesFetched = 0
        val limit = episodeLimit.coerceIn(1, 120)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(18)

        while (pagesFetched < pageLimit.coerceIn(1, 8) && added.size < limit &&
            System.nanoTime() < deadline
        ) {
            val normalizedPage = SeriesCatalogParser.normalizedUrl(pageUrl)
            if (!visited.add(normalizedPage)) break
            if (!MultiProviderRepository.isProviderContentUrl(pageUrl, seasonUrl)) break

            val html = runCatching { fetchSeasonHtml(pageUrl) }.getOrDefault("")
            if (html.isBlank()) {
                // Preserve the cursor so the user can retry after a transient failure.
                nextPageUrl = pageUrl
                break
            }
            val document = Jsoup.parse(html, pageUrl).apply { select("footer, script, style").remove() }
            pagesFetched++
            SeriesCatalogParser.parseEpisodeEntries(
                document = document,
                currentPageUrl = pageUrl,
                defaultSeasonNumber = seasonNumber,
                isAllowedContentUrl = { candidate ->
                    MultiProviderRepository.isProviderContentUrl(candidate, seasonUrl)
                },
            ).forEach { (title, episodeUrl, parsedSeason) ->
                if (added.size >= limit || (parsedSeason != null && parsedSeason != seasonNumber)) return@forEach
                if (seasonSeriesIdentity != null && !SeriesCatalogParser.episodeBelongsToSeries(
                        targetTitles = listOf(seasonSeriesIdentity),
                        targetPageUrls = listOf(seasonUrl),
                        episodeTitle = title,
                        episodeUrl = episodeUrl,
                        allowEpisodeListContext = true,
                    )
                ) return@forEach
                val key = SeriesCatalogParser.normalizedUrl(episodeUrl)
                if (key.isBlank() || key in known || key in added) return@forEach
                val number = SeriesCatalogParser.episodeNumber(title, episodeUrl) ?: return@forEach
                added[key] = ArabseedEpisode(
                    number = number,
                    title = "الحلقة $number",
                    pageUrl = episodeUrl,
                    seasonNumber = seasonNumber,
                    providerName = "عرب سيد",
                )
            }

            val next = SeriesCatalogParser.nextEpisodePage(document, pageUrl)
            if (next.isNullOrBlank() || SeriesCatalogParser.normalizedUrl(next) in visited) {
                nextPageUrl = null
                break
            }
            nextPageUrl = next
            pageUrl = next
        }

        val season = ArabseedSeason(
            number = seasonNumber,
            title = SeriesCatalogParser.seasonTitle(seasonNumber),
            episodes = added.values.sortedBy { it.number },
            pageUrl = seasonUrl,
        )
        android.util.Log.i(
            TAG,
            "season.more provider=arabseed season=$seasonNumber pages=$pagesFetched added=${season.episodes.size} hasMore=${nextPageUrl != null}",
        )
        return ArabseedSeasonBundle(listOf(season)) to nextPageUrl
    }

    fun fetchEpisodeServers(episodeUrl: String): List<ArabseedServer> =

        fetchServersFast(episodeUrl)



    fun resolveDirectStreamWithReferer(serverUrl: String, referer: String = ""): MultiProviderRepository.ResolvedStreamResult? {
        val unwrapped = MultiProviderRepository.decodeIdWrapperUrl(serverUrl)
        if (!isSafeResolverUrl(unwrapped)) return null
        val safeUnwrapped = SeriesCatalogParser.asciiSafeUrl(unwrapped)
        val sourceReferer = referer.trim().ifBlank {
            runCatching {
                val uri = java.net.URI(safeUnwrapped)
                "${uri.scheme}://${uri.host}/"
            }.getOrDefault(BASE_SLASH)
        }.let(SeriesCatalogParser::asciiSafeHeaderValue).ifBlank { BASE_SLASH }
        val direct = resolveDirectStreamInternal(safeUnwrapped, sourceReferer, 0)?.takeIf(::isSafeResolverUrl)
        if (!direct.isNullOrBlank()) {
            val unwrappedOrigin = runCatching {
                val u = java.net.URI(safeUnwrapped)
                "${u.scheme}://${u.host}/"
            }.getOrDefault(BASE_SLASH)
            val gwOrigin = if (unwrapped.contains("vids.php")) "https://d.myseed.tv/" else ""
            val refererCandidates = listOfNotNull(
                gwOrigin.takeIf { it.isNotBlank() },
                unwrappedOrigin,
                safeUnwrapped,
                sourceReferer,
                BASE_SLASH,
                "",
            ).distinct()
            val workingReferer = MultiProviderRepository.verifyPlayableStreamWithReferer(direct, refererCandidates)
            if (workingReferer != null) {
                return MultiProviderRepository.ResolvedStreamResult(direct, workingReferer)
            }
        }
        return MultiProviderRepository.resolveStreamWithReferer(safeUnwrapped, sourceReferer)
    }

    fun resolveDirectStream(serverUrl: String, referer: String = ""): String? {
        val unwrapped = MultiProviderRepository.decodeIdWrapperUrl(serverUrl)
        if (!isSafeResolverUrl(unwrapped)) return null
        val safeUnwrapped = SeriesCatalogParser.asciiSafeUrl(unwrapped)
        val sourceReferer = referer.trim().ifBlank {
            runCatching {
                val uri = java.net.URI(safeUnwrapped)
                "${uri.scheme}://${uri.host}/"
            }.getOrDefault(BASE_SLASH)
        }.let(SeriesCatalogParser::asciiSafeHeaderValue).ifBlank { BASE_SLASH }
        return resolveDirectStreamInternal(safeUnwrapped, sourceReferer, 0)
            ?.takeIf(::isSafeResolverUrl)
    }

    private fun isSafeResolverUrl(value: String): Boolean {
        val uri = runCatching { java.net.URI(SeriesCatalogParser.asciiSafeUrl(value)) }.getOrNull() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        if (uri.scheme !in setOf("http", "https") || host.isBlank()) return false
        if (Regex("(?i)\\.(apng|avif|bmp|gif|jpe?g|png|svg|webp)$").containsMatchIn(uri.path.orEmpty())) return false
        val blocked = listOf(
            "google.com",
            "googleadservices.com",
            "doubleclick.net",
            "googlesyndication.com",
        )
        return blocked.none { host == it || host.endsWith(".$it") }
    }

    private fun resolveDirectStreamInternal(serverUrl: String, referer: String, depth: Int): String? {
        if (depth > 2) return null
        try {
            val cacheKey = "$serverUrl|$referer"
            // vids.php tokens expire every ~60 seconds â€” skip cache for those URLs
            val isVidsPhpUrl = serverUrl.contains("vids.php") || serverUrl.contains("player-gateway")
            if (!isVidsPhpUrl) streamCache[cacheKey]?.let { return it }

            val sourceUri = runCatching { java.net.URI(serverUrl) }.getOrNull() ?: return null
            val sourcePath = sourceUri.path.orEmpty().lowercase()
            if (sourcePath.endsWith(".m3u8") || sourcePath.endsWith(".mp4")) {
                streamCache[cacheKey] = serverUrl
                return serverUrl
            }

            // ── MySeed vids.php dedicated resolver ───────────────────────────────
            // The watch page embeds /vids.php?t=TOKEN which wraps player-gateway.php
            // which in turn has <source src="https://cdn.../video.mp4">
            // Tokens expire in ~60s and player-gateway requires iframe Sec-Fetch headers + vids.php referer.
            if (serverUrl.contains("vids.php")) {
                try {
                    val vidsHtml = fetchHtmlWithReferer(serverUrl, referer.ifBlank { BASE_SLASH })
                    val gwMatch = Regex("""src=["']([^"']+player-gateway[^"']+)["']""").find(vidsHtml)
                    gwMatch?.groupValues?.get(1)?.let { rawGw ->
                        val gwUrl = rawGw.replace("&amp;", "&")
                        val gwHtml = fetchGatewayHtml(gwUrl, serverUrl)
                        val srcPattern = Regex(
                            """<source\s+[^>]*src=["']([^"']+\.(?:mp4|m3u8)(?:\?[^"']*)?)["']""",
                            RegexOption.IGNORE_CASE,
                        )
                        val srcCandidates = srcPattern.findAll(gwHtml)
                            .mapNotNull { it.groupValues.getOrNull(1)?.replace("&amp;", "&")?.trim() }
                            .toList()
                        val stream = srcCandidates.firstOrNull { it.substringBefore('?').endsWith(".m3u8", true) }
                            ?: srcCandidates.firstOrNull()
                        if (!stream.isNullOrBlank() && stream.startsWith("http")) {
                            android.util.Log.i("ArabseedRepo", "vids.php resolved to ${stream.take(60)}")
                            return stream
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ArabseedRepo", "vids.php resolver failed: ${e.message}")
                }
            }

            // ── Fast direct provider resolvers ────────────────────────────
            // 1. Vidara / Sugbaioug API resolver
            if (serverUrl.contains("vidara.") || serverUrl.contains("sugbaioug.")) {
                val fileCode = serverUrl.trimEnd('/').substringAfterLast('/')
                if (fileCode.isNotBlank()) {
                    val safeServerRef = SeriesCatalogParser.asciiSafeHeaderValue(serverUrl)
                    val vidaraOrigin = runCatching {
                        val u = java.net.URI(SeriesCatalogParser.asciiSafeUrl(serverUrl))
                        "${u.scheme}://${u.host}"
                    }.getOrDefault("https://vidara.to")
                    for (apiOrigin in listOf(vidaraOrigin, "https://vidara.to").distinct()) {
                        try {
                            val jsonBody = """{"filecode":"$fileCode","device":"android","codecs":["av01.0.09M.10"]}"""
                            val postReq = Request.Builder()
                                .url("$apiOrigin/api/stream")
                                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                                .header("User-Agent", UA)
                                .apply { if (safeServerRef.isNotBlank()) header("Referer", safeServerRef) }
                                .header("Origin", apiOrigin)
                                .build()
                            client.newCall(postReq).execute().use { resp ->
                                val bodyStr = resp.body?.string().orEmpty()
                                val streamMatch = Regex(""""streaming_url"\s*:\s*"([^"]+)"""").find(bodyStr)
                                streamMatch?.groupValues?.get(1)?.let { stream ->
                                    val cleanStream = stream.replace("\\/", "/")
                                    if (cleanStream.startsWith("http")) {
                                        streamCache["$serverUrl|$referer"] = cleanStream
                                        return cleanStream
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // 2. Vidmoly direct m3u8
            if (serverUrl.contains("vidmoly")) {
                try {
                    val vmHtml = fetchHtmlWithReferer(serverUrl, referer)
                    val vmMatch = Regex("""["'](https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)["']""").find(vmHtml)
                    vmMatch?.groupValues?.get(1)?.let { vmStream ->
                        streamCache[cacheKey] = vmStream
                        return vmStream
                    }
                } catch (_: Exception) {}
            }

            // 3. Uqload direct mp4
            if (serverUrl.contains("uqload")) {
                try {
                    val uqHtml = fetchHtmlWithReferer(serverUrl, referer)
                    val uqMatch = Regex("""sources\s*:\s*\[\s*["']([^"']+\.mp4[^"']*)["']""").find(uqHtml)
                    uqMatch?.groupValues?.get(1)?.let { uqStream ->
                        if (uqStream.startsWith("http")) {
                            streamCache[cacheKey] = uqStream
                            return uqStream
                        }
                    }
                } catch (_: Exception) {}
            }

            // 4. Voe.sx resolver (MKGMa + JS redirect + legacy patterns)
            if (serverUrl.contains("voe.sx") || serverUrl.contains("voe.")) {
                try {
                    MultiProviderRepository.resolveEmbedToStreamWithMeta(serverUrl, referer.ifBlank { BASE_SLASH })?.first?.let { voeStream ->
                        if (voeStream.startsWith("http")) {
                            streamCache[cacheKey] = voeStream
                            return voeStream
                        }
                    }
                } catch (_: Exception) {}
            }

            // 5. Vidsharing / Byse / Govid resolver (AES-256-GCM + XFS + govid iframe hops)
            if (serverUrl.contains("vidsharing") || serverUrl.contains("byse") || serverUrl.contains("govid")) {
                try {
                    MultiProviderRepository.resolveEmbedToStreamWithMeta(serverUrl, referer.ifBlank { BASE_SLASH })?.first?.let { stream ->
                        if (stream.startsWith("http")) {
                            streamCache[cacheKey] = stream
                            return stream
                        }
                    }
                } catch (_: Exception) {}
            }

            // 6. Govid resolver — يفتح صفحة govid ويستخرج m3u8 من player config
            if (serverUrl.contains("govid") || serverUrl.contains("govid.me")) {
                try {
                    val origin = try {
                        val u = java.net.URI.create(serverUrl)
                        "${u.scheme}://${u.host}/"
                    } catch (_: Exception) { serverUrl }
                    val goHtml = fetchHtmlWithReferer(serverUrl, referer.ifBlank { origin })
                    // Govid: jwplayer("player").setup({sources:[{file:"...m3u8"}]})
                    val goMatch = Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE).find(goHtml)
                    goMatch?.groupValues?.get(1)?.let { goStream ->
                        if (goStream.startsWith("http")) {
                            streamCache[cacheKey] = goStream
                            return goStream
                        }
                    }
                    // Govid sometimes uses a JSON API call: /api/source/FILEID
                    val fileIdMatch = Regex("""/(?:api/source|embed)/([A-Za-z0-9]+)""").find(serverUrl)
                    fileIdMatch?.groupValues?.get(1)?.let { fileId ->
                        val apiUrl = "${origin}api/source/$fileId"
                        val apiResp = fetchHtmlWithReferer(apiUrl, serverUrl)
                        val apiMatch = Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").find(apiResp)
                        apiMatch?.groupValues?.get(1)?.let { goStream ->
                            if (goStream.startsWith("http")) {
                                streamCache[cacheKey] = goStream
                                return goStream
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 7. Doodstream resolver
            if (serverUrl.contains("dood") || serverUrl.contains("ds2play") || serverUrl.contains("playmogo") || serverUrl.contains("doodstream")) {
                try {
                    val origin = try {
                        val u = java.net.URI.create(serverUrl)
                        "${u.scheme}://${u.host}/"
                    } catch (_: Exception) { serverUrl }
                    val doodHtml = fetchHtmlWithReferer(serverUrl, referer.ifBlank { origin })
                    // Doodstream: window.location = '/pass_md5/HASH/TOKEN?...' → then URL + random + timestamp
                    val passMd5Match = Regex("""\$\.get\(['"]([^'"]+/pass_md5/[^'"]+)['"]""").find(doodHtml)
                        ?: Regex("""['"]([^'"]+/pass_md5/[^'"]+)['"]""").find(doodHtml)
                    passMd5Match?.groupValues?.get(1)?.let { md5Path ->
                        val md5Url = when {
                            md5Path.startsWith("http") -> md5Path
                            md5Path.startsWith("/") -> origin.trimEnd('/') + md5Path
                            else -> "$origin$md5Path"
                        }
                        val md5Resp = fetchHtmlWithReferer(md5Url, serverUrl).trim()
                        if (md5Resp.startsWith("http")) {
                            val token = Regex("""token=([^&]+)""").find(doodHtml)?.groupValues?.get(1) ?: ""
                            val timestamp = System.currentTimeMillis() / 1000
                            val doodStream = "${md5Resp}${System.currentTimeMillis()}?token=$token&expiry=$timestamp"
                            streamCache[cacheKey] = doodStream
                            return doodStream
                        }
                    }
                } catch (_: Exception) {}
            }

            // 8. Streamtape resolver
            if (serverUrl.contains("streamtape")) {
                try {
                    val stHtml = fetchHtmlWithReferer(serverUrl, referer.ifBlank { "https://streamtape.com/" })
                    // Streamtape: id="ideoooolink" or document.getElementById('ideoooolink').innerHTML
                    val stMatch = Regex("""id="ideoooolink">([^<]+)""").find(stHtml)
                        ?: Regex("""innerHTML\s*=\s*["']([^"']+streamtape[^"']+)["']""").find(stHtml)
                    stMatch?.groupValues?.get(1)?.let { raw ->
                        val stStream = when {
                            raw.startsWith("//") -> "https:$raw"
                            raw.startsWith("http") -> raw
                            else -> return@let
                        }
                        // Streamtape sometimes obfuscates the URL by splitting it into two strings
                        val cleaned = stStream.replace("\"+'\"+'\"", "").replace("//", "/").let {
                            if (it.startsWith("https:/")) "https://${it.substringAfter("https:/").trimStart('/')}" else it
                        }
                        if (cleaned.startsWith("http") && cleaned.contains("streamtape")) {
                            streamCache[cacheKey] = cleaned
                            return cleaned
                        }
                    }
                } catch (_: Exception) {}
            }

            val origin = try {
                val u = java.net.URI.create(serverUrl)
                "${u.scheme}://${u.host}/"
            } catch (_: Exception) { BASE_SLASH }

            val gHtml = fetchHtmlWithReferer(serverUrl, referer.ifBlank { origin })
            if (gHtml.isBlank()) return null

            // 1. Direct <source src="..."> in HTML5 video tags (e.g. MySeed / player-gateway)
            val sourcePattern = Regex(
                """<source\s+[^>]*src=["']([^"']+\.(?:mp4|m3u8)(?:\?[^"']*)?)["']""",
                RegexOption.IGNORE_CASE,
            )
            val sourceCandidates = sourcePattern.findAll(gHtml).mapNotNull {
                it.groupValues.getOrNull(1)?.replace("&amp;", "&")?.trim()
            }.toList()
            (sourceCandidates.firstOrNull { it.substringBefore('?').endsWith(".m3u8", true) }
                ?: sourceCandidates.firstOrNull())?.let {
                streamCache[cacheKey] = it
                return it
            }

            // 2. Direct regex match for mp4 or m3u8 in scripts/page
            val dirRegex = Regex("""["'](https?://[^\s"'<>]+\.(?:m3u8|mp4)(?:\?[^\s"'<>]*)?)["']""", RegexOption.IGNORE_CASE)
            dirRegex.findAll(gHtml).mapNotNull { it.groupValues.getOrNull(1) }.toList()
                .let { candidates -> candidates.firstOrNull { it.substringBefore('?').endsWith(".m3u8", true) } ?: candidates.firstOrNull() }
                ?.let {
                streamCache[cacheKey] = it
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
                    val resolved = resolveDirectStreamInternal(fullIfr, serverUrl, depth + 1)
                    if (resolved != null) {
                        streamCache[cacheKey] = resolved
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
                    streamCache[cacheKey] = dec
                    return dec
                }
            }

            // 5. JWPlayer / videojs regex â€” extended patterns
            val jwRegex = Regex("""(?:file|sources|src)\s*:\s*(?:\[\s*\{[^}]*?file\s*:\s*["']([^"']+)["']|\[\s*["']([^"']+)["']|["']([^"']+\.(?:mp4|m3u8)[^"']*)["'])""", RegexOption.IGNORE_CASE)
            jwRegex.find(gHtml)?.let { jm ->
                val cand = jm.groupValues.drop(1).firstOrNull { it.isNotBlank() }
                if (cand != null && cand.startsWith("http")) {
                    streamCache[cacheKey] = cand
                    return cand
                }
            }

            // 6. Broader stream URL search â€” any URL with /hls/ /video/ /stream/ path
            val broadStreamRegex = Regex(
                """["'](https?://[^"'\s<>]{10,}(?:/hls/|/stream/|/video/|/media/|/cdn/)[^"'\s<>]+)["']""",
                RegexOption.IGNORE_CASE
            )
            broadStreamRegex.findAll(gHtml).mapNotNull { it.groupValues.getOrNull(1) }
                .firstOrNull { it.startsWith("http") }
                ?.let {
                    streamCache[cacheKey] = it
                    return it
                }

            // 7. eval/atob decoded content search
            val atobRegex = Regex("""atob\(['"]([A-Za-z0-9+/=]{20,})['"]""")
            atobRegex.findAll(gHtml).forEach { m ->
                val decoded = runCatching {
                    String(android.util.Base64.decode(m.groupValues[1], android.util.Base64.DEFAULT))
                }.getOrNull() ?: return@forEach
                val innerM3u8 = Regex("""["'](https?://[^"'\s<>]+\.m3u8[^"'\s<>]*)["']""").find(decoded)
                innerM3u8?.groupValues?.get(1)?.let {
                    if (it.startsWith("http")) {
                        streamCache[cacheKey] = it
                        return it
                    }
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



    private fun fetchSeasonHtml(urlStr: String): String =

        fetchHtmlWithReferer(urlStr, BASE_SLASH, callTimeoutSeconds = 12)



    private fun fetchGatewayHtml(urlStr: String, referer: String): String {
        val safeUrl = SeriesCatalogParser.asciiSafeUrl(urlStr).ifBlank { return "" }
        val safeReferer = SeriesCatalogParser.asciiSafeHeaderValue(referer).ifBlank { BASE_SLASH }
        val req = runCatching {
            Request.Builder()
                .url(safeUrl)
                .header("User-Agent", UA)
                .header("Accept", VAL_AHTML)
                .header(HDR_ALANG, VAL_ALANG)
                .header(HDR_REF, safeReferer)
                .header("Sec-Fetch-Dest", "iframe")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "same-site")
                .header(HDR_CONN, VAL_KALIVE)
                .build()
        }.getOrNull() ?: return ""
        val host = runCatching { java.net.URI.create(urlStr).host.orEmpty() }.getOrDefault("invalid-url")
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                android.util.Log.w(TAG, "Gateway HTTP ${resp.code} for host=$host")
                throw IOException("HTTP ${resp.code}")
            }
            resp.body?.string() ?: throw IOException("Empty gateway response")
        }
    }

    private fun fetchHtmlWithReferer(
        urlStr: String,
        referer: String,
        callTimeoutSeconds: Long? = null,
    ): String {
        val safeUrl = SeriesCatalogParser.asciiSafeUrl(urlStr).ifBlank { return "" }
        val safeReferer = SeriesCatalogParser.asciiSafeHeaderValue(referer).ifBlank { BASE_SLASH }
        val host = runCatching { java.net.URI.create(safeUrl).host.orEmpty() }.getOrDefault("invalid-url")
        return try {
            val req = Request.Builder()
                .url(safeUrl)
                .header("User-Agent", UA)
                .header("Accept", VAL_AHTML)
                .header(HDR_ALANG, VAL_ALANG)
                .header(HDR_REF, safeReferer)
                .header(HDR_CONN, VAL_KALIVE)
                .build()
            val call = client.newCall(req)
            callTimeoutSeconds?.let { call.timeout().timeout(it, TimeUnit.SECONDS) }
            call.execute().use { resp ->
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



    private fun parseServersFast(
        html: String,
        pageUrl: String = "",
        allowGenericIframes: Boolean = true,
    ): List<ArabseedServer> {
        val servers = mutableListOf<ArabseedServer>()
        val doc = Jsoup.parse(html)
        if (MultiProviderRepository.isHomePageOrListingPage(doc, pageUrl)) {
            return emptyList()
        }

        fun isSeriesNavigationElement(element: org.jsoup.nodes.Element, url: String): Boolean {
            val label = listOf(
                element.text(),
                element.attr("title"),
                element.attr("aria-label"),
                element.attr("data-name"),
            ).filter(String::isNotBlank).joinToString(" ")
            if (SeriesCatalogParser.isEpisode(label, "") ||
                SeriesCatalogParser.seasonNumber(label, "") != null ||
                SeriesCatalogParser.partNumber(label, "") != null
            ) return true
            val markers = (listOf(element) + element.parents()).take(6)
                .flatMap { listOf(it.id(), it.className()) }
                .joinToString(" ")
            return Regex(
                "(?i)(^|[\\s_-])(" +
                    "season[-_ ]?(tabs|list|nav|selector)|episode[-_ ]?(list|grid|tabs|nav|pagination)|" +
                    "episodes[-_ ]?(list|grid|nav)|eplist|eps-list|series[-_ ]?(list|grid|navigation)|" +
                    "part[-_ ]?(list|tabs)|related|recommend|suggest|sidebar|widget)([\\s_-]|$)",
            ).containsMatchIn(markers)
        }

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
                if (isServerCandidateUrl(full) && !isSeriesNavigationElement(el, full) && servers.none { it.embedUrl == full }) {
                    servers += ArabseedServer(serverName, full)
                }
            }
        }

        // 2. Elements with data-url, data-src, data-embed
        for (el in doc.select("$SEL_DURL, $SEL_DSRC, $SEL_DEMBED, [data-url], [data-src], [data-embed]")) {
            var link = el.attr(R_DURL).ifBlank { el.attr(R_DEMBED).ifBlank { el.attr(R_DSRC).ifBlank { el.attr("data-url").ifBlank { el.attr("data-embed") } } } }.trim()
            val rawName = el.attr("data-server-name").ifBlank { el.text() }.trim()
            if (link.isNotBlank() && !isAdUrl(link)) {
                link = MultiProviderRepository.decodeIdWrapperUrl(link)
                if (link.startsWith("http") || link.startsWith("/")) {
                    val full = if (link.startsWith("http")) link else "${BASE_URL.trimEnd('/')}/" + link.trimStart('/')
                    val serverName = cleanServerName(rawName, full)
                    if (isServerCandidateUrl(full) && !isSeriesNavigationElement(el, full) && servers.none { it.embedUrl == full }) {
                        servers += ArabseedServer(serverName, full)
                    }
                }
            }
        }

        // The detail page can have artwork/trailer iframes. Only parse generic
        // iframe server tabs from the canonical watch page.
        if (allowGenericIframes) for (ifr in doc.select("iframe, $SEL_IFRAME")) {
            val src = ifr.attr("src").ifBlank { ifr.attr(R_ISRC).ifBlank { ifr.attr(R_DSRC) } }.trim()
            if (src.isNotBlank() && !isAdUrl(src)) {
                val fullSrc = when {
                    src.startsWith("http://") || src.startsWith("https://") -> src
                    src.startsWith("//") -> "https:$src"
                    src.startsWith("/") -> "${BASE_URL.trimEnd('/')}$src"
                    else -> "${BASE_URL.trimEnd('/')}/$src"
                }
                val serverName = cleanServerName("سيرفر تشغيل", fullSrc)
                if (isServerCandidateUrl(fullSrc) && !isSeriesNavigationElement(ifr, fullSrc) && servers.none { it.embedUrl == fullSrc }) {
                    servers += ArabseedServer(serverName, fullSrc)
                }
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
        val safeUrl = SeriesCatalogParser.asciiSafeUrl(url).ifBlank { return false }
        val parsed = runCatching { java.net.URI.create(safeUrl) }.getOrNull() ?: return false
        val host = parsed.host.orEmpty().lowercase(java.util.Locale.ROOT)
        if (AD_PATTERNS.any { host.contains(it.lowercase(java.util.Locale.ROOT)) }) return true
        val path = parsed.path.orEmpty()
        return Regex("(?i)(^|/)(ads?|advert|banner|popunder|onclick|tracking|analytics)(/|$)")
            .containsMatchIn(path)
    }

    private fun isServerCandidateUrl(url: String, pageUrl: String? = null): Boolean {
        val clean = SeriesCatalogParser.asciiSafeUrl(url)
        val uri = runCatching { java.net.URI(clean) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(java.util.Locale.ROOT).orEmpty()
        val path = uri.path.orEmpty().lowercase(java.util.Locale.ROOT)
        if (uri.scheme !in setOf("http", "https") || host.isBlank() || isAdUrl(clean)) return false
        val deadEmbedHosts = listOf("govid.live", "luluvdo.com", "vidplayer.live", "shahied4u.vidplayer.live")
        if (deadEmbedHosts.any { host == it || host.endsWith(".$it") }) return false
        if (Regex("(?i)\\.(?:apng|avif|bmp|gif|jpe?g|png|svg|webp|css|js|ico|woff2?|ttf|eot|otf|xml|json|txt|pdf|zip|rar)$").containsMatchIn(path)) return false
        if (host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be") return false

        // Never accept the item/source page itself as a server
        if (pageUrl != null) {
            val cleanPage = pageUrl.trim().trimEnd('/')
            if (clean.trimEnd('/') == cleanPage || clean.substringBefore('?') == cleanPage.substringBefore('?')) {
                return false
            }
        }

        // Never accept site navigation, login, plans, catalog, etc.
        val blockedRoutes = listOf(
            "/login", "/logout", "/register", "/signup", "/signin", "/auth",
            "/plans", "/pricing", "/privacy", "/terms", "/contact", "/about",
            "/catalog", "/search", "/category", "/genre", "/tag", "/tags",
            "/actor", "/director", "/filter", "/years", "/schedule", "/episode-schedule",
            "/dmca", "/policy", "/faq", "/rss", "/feed", "/sitemap"
        )
        if (path.isEmpty() || path == "/" || blockedRoutes.any { path.contains(it) }) return false

        val isContentDetailRoute = path.startsWith("/movie/") || path.startsWith("/drama/") ||
            path.startsWith("/series/") || path.startsWith("/selary/") || path.startsWith("/post/") ||
            path.startsWith("/show/") || path.startsWith("/title/") || path.startsWith("/film/") ||
            path.startsWith("/episode/")
        if (isContentDetailRoute) {
            val hasPlaybackMarker = path.contains("/watch") || path.contains("/servers") ||
                path.contains("/embed") || path.contains("/player") || path.contains("/play") ||
                path.contains("/video") || path.contains("/v/") || path.contains("/e/") ||
                clean.contains("?watch=") || clean.contains("?embed=") || clean.contains("?play=") ||
                clean.contains("&watch=") || clean.contains("&embed=") || clean.contains("&play=") ||
                clean.contains("vids.php")
            if (!hasPlaybackMarker) return false
        }

        return true
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
