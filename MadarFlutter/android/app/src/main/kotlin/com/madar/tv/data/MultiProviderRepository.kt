package com.madar.tv.data



import okhttp3.FormBody

import okhttp3.OkHttpClient

import okhttp3.Request

import org.jsoup.Jsoup
import org.json.JSONObject

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService

import java.util.concurrent.Executors

import java.util.concurrent.Future

import java.util.concurrent.TimeUnit



/**

 * MultiProviderRepository — مجمّع المحتوى من 7 مواقع عربية شهيرة لتطبيق زقزوقة TV

 * ───────────────────────────────────────────────────────────────────────────────

 * يعمل بالتوازي (Thread Pool) مع cache ذكي لضمان سرعة فائقة.

 * المواقع المدعومة:

 * 1. أكوام (Akwam)

 * 2. وي سيما (WeCima)

 * 3. فاصل HD (FaselHD)

 * 4. إيجي بست (EgyBest)

 * 5. لودي نت (Lodynet)

 * 6. بريستيج (Brstej / Prestige)

 * 7. قصة عشق (Esheeq / 3sk)

 */

object MultiProviderRepository {

    private const val TAG = "MultiProviderRepository"



    // ── Constants ──────────────────────────────────────────────────────

    private const val UA =

        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"



    private const val LIST_TTL   = 10 * 60 * 1000L  // 10 دقائق

    private const val SEARCH_TTL = 8 * 60 * 1000L   // 8 دقائق

    private const val DETAIL_TTL = 30 * 60 * 1000L  // 30 دقيقة



    // ── Thread Pool ────────────────────────────────────────────────────

    // A cached pool can create one thread per source on every refresh/search.
    // Keep network fan-out parallel but bounded for low-memory phones.
    private val pool = Executors.newFixedThreadPool(6)
    // Keep detail/season lookups responsive even while catalog providers are busy.
    private val seasonPagePool = Executors.newFixedThreadPool(4)



    // ── HTTP Client ────────────────────────────────────────────────────

    private val http = OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .connectTimeout(6, TimeUnit.SECONDS)

        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)

        .followRedirects(true)

        .followSslRedirects(true)

        .build()



    // ── Caches ─────────────────────────────────────────────────────────

    private val listCache   = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()

    private val searchCache = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()

    private val detailCache = ConcurrentHashMap<String, ArabseedDetail>()
    private val detailCacheAt = ConcurrentHashMap<String, Long>()
    private val seasonCacheAt = ConcurrentHashMap<String, Long>()
    private const val SEASON_TTL = 30 * 60 * 1000L
    private const val SEASON_ENRICHMENT_RETRY_MS = 3 * 60 * 1000L
    private val seasonMirrorPool = Executors.newFixedThreadPool(6)
    private val seasonAggregationPool = Executors.newSingleThreadExecutor()
    private val seasonAggregationInFlight = ConcurrentHashMap.newKeySet<String>()


    private val seasonCache = ConcurrentHashMap<String, ArabseedSeasonBundle>()



    private fun catalogCacheKey(type: ArabseedType?, page: Int): String = when (type) {
        ArabseedType.MOVIE -> "movies:$page"
        ArabseedType.SERIES -> "series:$page"
        null -> "catalog:$page"
    }

    fun loadCachedCatalog(type: ArabseedType?, page: Int = 1): List<ArabseedItem>? {
        val key = catalogCacheKey(type, page)
        listCache[key]?.let { cached ->
            if (System.currentTimeMillis() - cached.first <= 14L * 24 * 60 * 60 * 1000) return cached.second
        }
        val snapshot = CatalogDiskCache.loadItems("providers:list:$key") ?: return null
        listCache[key] = snapshot.savedAt to snapshot.value
        return snapshot.value
    }

    fun loadCachedSearch(query: String): List<ArabseedItem>? {
        val key = "search:" + query.trim().lowercase()
        if (key == "search:") return null
        searchCache[key]?.let { cached ->
            if (System.currentTimeMillis() - cached.first <= 14L * 24 * 60 * 60 * 1000) return cached.second
        }
        val snapshot = CatalogDiskCache.loadItems("providers:" + key) ?: return null
        searchCache[key] = snapshot.savedAt to snapshot.value
        return snapshot.value
    }

    fun loadCachedDetail(pageUrl: String): ArabseedDetail? {
        detailCache[pageUrl]?.let { if (it.servers.isNotEmpty()) return it }
        val snapshot = CatalogDiskCache.loadDetail("providers:detail:" + pageUrl) ?: return null
        if (snapshot.value.servers.isEmpty()) return null
        detailCache[pageUrl] = snapshot.value
        detailCacheAt[pageUrl] = snapshot.savedAt
        return snapshot.value
    }

    fun getCachedDetail(pageUrl: String): ArabseedDetail? = detailCache[pageUrl]

    fun loadCachedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        seasonCache[pageUrl]?.let { return it }
        val snapshot = CatalogDiskCache.loadSeasonBundle("providers:seasons:v7:" + pageUrl) ?: return null
        val normalized = SeriesCatalogParser.normalizeSeasonBundle(snapshot.value)
        seasonCache[pageUrl] = normalized
        seasonCacheAt[pageUrl] = snapshot.savedAt
        return normalized
    }

    fun loadCachedMergedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        val snapshot = CatalogDiskCache.loadSeasonBundle("combined:seasons:v5:$pageUrl") ?: return null
        if (System.currentTimeMillis() - snapshot.savedAt > SEASON_TTL) return null
        return SeriesCatalogParser.normalizeSeasonBundle(snapshot.value)
    }

    /** Persist on-demand season-page results into the canonical series cache. */
    fun cacheSeasonPageEnrichment(
        seriesItem: ArabseedItem,
        seasonItem: ArabseedItem,
        seasonBundle: ArabseedSeasonBundle,
    ) {
        val existing = loadCachedMergedSeasonBundle(seriesItem.pageUrl) ?: ArabseedSeasonBundle(emptyList())
        val merged = mergeProviderSeasonBundles(
            listOf(seasonItem to seasonBundle, seriesItem to existing),
        )
        if (merged.seasons.isNotEmpty() || merged.relatedParts.isNotEmpty()) {
            CatalogDiskCache.saveSeasonBundle("combined:seasons:v5:${seriesItem.pageUrl}", merged)
        }
    }

    fun clearCache() {

        listCache.clear()

        searchCache.clear()

        detailCache.clear()

        seasonCache.clear()

    }



    // ══════════════════════════════════════════════════════════════════

    // Provider Definitions

    // ══════════════════════════════════════════════════════════════════



    data class ProviderConfig(

        val id: String,

        val name: String,

        val baseUrl: String,

        val moviesPath: String,

        val seriesPath: String,

        val searchPath: String,    // use {q} as placeholder

        val cardSelectors: List<String>,

        val titleSelectors: List<String>,

        val posterAttr: List<String> = listOf("data-src", "src", "data-lazy-src", "data-original", "data-lazy", "abs:src"),

        val qualitySelectors: List<String> = listOf(".quality", ".ribbon", ".__quality", ".qlty"),

        val yearSelectors: List<String> = listOf(".year", ".date"),

        val enabled: Boolean = true,

    )



    val PROVIDERS = listOf(

        // ── أكوام ──────────────────────────────────────────────────────

        ProviderConfig(

            id = "akwam",

            name = "أكوام",

            baseUrl = "https://akw.cam",

            moviesPath = "/movies?page={page}",

            seriesPath = "/series?page={page}",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "a.br-card-link[href*='/akfilm-']",

                "a.br-card-link[href*='/akseries-']",

                "a[href*='/akfilm-']",

                "a[href*='/akseries-']",

            ),

            titleSelectors = listOf("h3.br-card-title", "h3", "h2", ".title"),

        ),



        // ── وي سيما ────────────────────────────────────────────────────

        ProviderConfig(

            id = "wecima",

            name = "وي سيما",

            // .casa currently redirects to .shop; using the final host keeps relative URLs resolvable.
            baseUrl = "https://myweecima.shop",

            moviesPath = "/movies.php?page={page}",

            // The old series.php endpoint currently serves only its navigation shell;
            // the live catalog is published at all-series.php.
            seriesPath = "/all-series.php?page={page}",

            searchPath = "/search.php?keywords={q}",

            cardSelectors = listOf(

                "a[href*='watch.php?vid=']",

                ".block-post a",

                "li.block-post a",

            ),

            titleSelectors = listOf("[title]", "h2", "h3", ".title"),

        ),



        // ── فاصل HD ────────────────────────────────────────────────────

        ProviderConfig(

            id = "faselhd",

            name = "فاصل HD",

            baseUrl = "https://wb49584.fasel4k.site",

            moviesPath = "/movies/?page={page}",

            seriesPath = "/series/?page={page}",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "div.list-movie",

                ".list-movie",

                ".postDiv",

                "a.list-title",

                "a[href*='/movie/']",

                "a[href*='/serie/']",

            ),

            titleSelectors = listOf(".list-title", "h2", "h3", ".post__title"),

        ),



        // ── إيجي بست ───────────────────────────────────────────────────

        ProviderConfig(

            id = "egybest",

            name = "إيجي بست",

            baseUrl = "https://egybest.mx",

            moviesPath = "/movies/?page={page}",

            seriesPath = "/series/?page={page}",

            searchPath = "/explore/?q={q}",

            cardSelectors = listOf(

                "a.movie",

                ".movies-list-full a.movie",

                ".content-box a",

                "div.movie-card a",

            ),

            titleSelectors = listOf("h2", "h3", ".title", ".movie-title"),

        ),



        // ── لودي نت ────────────────────────────────────────────────────

        ProviderConfig(

            id = "lodynet",

            name = "لودي نت",

            baseUrl = "https://lodynet.watch",

            moviesPath = "/page/{page}/",

            seriesPath = "/category/series/page/{page}/",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "article.bt a",

                ".content-box article a",

                ".movies-list article a",

                "h2.entry-title a",

            ),

            titleSelectors = listOf("h2", "h3", ".entry-title", ".title"),
            enabled = false,
        ),



        // ── بريستيج ────────────────────────────────────────────────────

        ProviderConfig(

            id = "prestige",

            name = "بريستيج",

            baseUrl = "https://brstejj.cyou",

            moviesPath = "/cat03.php?cat=movies2-2224",

            seriesPath = "/moslslat.php",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "a[href*='video.php?vid=']",

                "a[href*='series1.php']",

                ".movie__block a",

                ".MovieBlock a",

                ".ptoday-card a",

                ".pgh-card a",

                ".pgh-series-card a",

                ".pgh-simple-card a",

            ),

            titleSelectors = listOf("h2", "h3", ".Title", "span.title", ".pgh-card-title", ".pgh-series-name"),

        ),



        // ── قصة عشق ────────────────────────────────────────────────────

        ProviderConfig(

            id = "esheeq",

            name = "قصة عشق",

            baseUrl = "https://e.q-ask.video",

            moviesPath = "/movies?page={page}",

            seriesPath = "/series?page={page}",

            searchPath = "/search?q={q}",

            cardSelectors = listOf(

                "div.thumb a",

                "article a",

                ".video-card a",

                "a[href*='/video-']",

            ),

            titleSelectors = listOf("h2", "h3", ".title"),

        ),

    )



    fun isMultiProvider(pageUrl: String): Boolean {

        return pageUrl.contains("akw.cam") || pageUrl.contains("akwam")

            || pageUrl.contains("wecima") || pageUrl.contains("myweecima")

            || pageUrl.contains("fasel") || pageUrl.contains("fasel4k")

            || pageUrl.contains("egybest") || pageUrl.contains("lodynet")

            || pageUrl.contains("brstej") || pageUrl.contains("brstejj.cyou") || pageUrl.contains("q-ask") || pageUrl.contains("3sk")

    }



    private fun getProviderForUrl(url: String): ProviderConfig? {

        return PROVIDERS.firstOrNull { p ->

            url.contains(p.baseUrl.replace("https://", "").replace("http://", ""))

        } ?: when {

            url.contains("akw") -> PROVIDERS.firstOrNull { it.id == "akwam" }

            url.contains("wecima") -> PROVIDERS.firstOrNull { it.id == "wecima" }

            url.contains("fasel") -> PROVIDERS.firstOrNull { it.id == "faselhd" }

            url.contains("egybest") -> PROVIDERS.firstOrNull { it.id == "egybest" }

            url.contains("lodynet") -> PROVIDERS.firstOrNull { it.id == "lodynet" }

            url.contains("brstej") -> PROVIDERS.firstOrNull { it.id == "prestige" }

            url.contains("q-ask") || url.contains("3sk") -> PROVIDERS.firstOrNull { it.id == "esheeq" }

            else -> null

        }

    }



    // ══════════════════════════════════════════════════════════════════

    // Fetch HTML helper

    // ══════════════════════════════════════════════════════════════════

    private fun fetchHtml(url: String, referer: String = ""): String {

        return try {

            val req = Request.Builder()

                .url(url)

                .header("User-Agent", UA)

                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

                .header("Accept-Language", "ar,en;q=0.9")

                .apply { if (referer.isNotBlank()) header("Referer", referer) }

                .build()

            val host = runCatching { java.net.URI.create(url).host.orEmpty() }.getOrDefault("invalid-url")
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) {
                    android.util.Log.w(TAG, "HTTP ${r.code} for host=$host")
                    ""
                } else {
                    val body = r.body?.string().orEmpty()
                    if (body.isBlank()) android.util.Log.w(TAG, "Empty HTML response for host=$host")
                    body
                }
            }

        } catch (error: Exception) {
            val host = runCatching { java.net.URI.create(url).host.orEmpty() }.getOrDefault("invalid-url")
            android.util.Log.w(TAG, "Fetch failed for host=$host (${error.javaClass.simpleName})")
            ""
        }

    }



    // ══════════════════════════════════════════════════════════════════

    // Parse items from a provider's HTML

    // ══════════════════════════════════════════════════════════════════

    private fun parseItems(

        html: String,

        provider: ProviderConfig,

        defaultType: ArabseedType,

    ): List<ArabseedItem> {

        if (html.isBlank()) return emptyList()

        val doc = try { Jsoup.parse(html, provider.baseUrl) } catch (_: Exception) { return emptyList() }

        val items = mutableListOf<ArabseedItem>()



        for (sel in provider.cardSelectors) {

            val elements = doc.select(sel)

            if (elements.isEmpty()) continue



            for (el in elements) {

                var rawHref = el.attr("abs:href").ifBlank { el.attr("href") }.trim()
                if (rawHref.isBlank() || rawHref.startsWith("#") || rawHref.startsWith("javascript:")) {
                    rawHref = el.selectFirst("a[href]")?.let { it.attr("abs:href").ifBlank { it.attr("href") } }?.trim() ?: ""
                }
                val href = when {
                    rawHref.startsWith("http") -> rawHref
                    rawHref.startsWith("/") -> "${provider.baseUrl.trimEnd('/')}$rawHref"
                    rawHref.isNotBlank() -> "${provider.baseUrl.trimEnd('/')}/$rawHref"
                    else -> ""
                }
                if (href.isBlank() || !href.startsWith("http")) continue

                if (href.contains("/category") || href.contains("/tag/") || href.contains("/page/") || href.contains("category.php") || href.contains("topvideos") || href.contains("favicons")) continue



                // Title

                var title = ""

                for (ts in provider.titleSelectors) {

                    title = el.selectFirst(ts)?.text()?.trim() ?: ""

                    if (title.isNotBlank()) break

                }

                if (title.isBlank()) title = el.attr("title").trim()

                if (title.isBlank()) title = el.selectFirst("img")?.attr("alt")?.trim() ?: ""

                if (title.isBlank()) {
                    val directTxt = el.ownText().trim().ifBlank { el.text().trim() }
                    if (directTxt.length in 2..100) title = directTxt
                }

                if (title.length < 2) continue



                // Poster
                val img = el.selectFirst("img") ?: el.selectFirst("[data-src]")
                var poster: String? = null

                // 1. Try img attributes (data-src, src, etc.)
                for (attr in provider.posterAttr) {
                    val raw = img?.attr(attr) ?: continue
                    poster = when {
                        raw.startsWith("http") -> raw
                        raw.startsWith("/") -> "${provider.baseUrl}$raw"
                        raw.isNotBlank() -> "${provider.baseUrl}/$raw"
                        else -> null
                    }
                    if (poster != null) break
                }

                // 2. Try abs:src fallback (Jsoup resolves relative URLs)
                if (poster == null) {
                    poster = img?.attr("abs:src")?.takeIf { it.startsWith("http") }
                }

                // 3. Try background-image in style attribute (WeCima, Akwam)
                if (poster == null) {
                    val styleEl = el.selectFirst("[style*='background-image']")
                        ?: el.selectFirst(".br-thumb, .ak-poster, .imgSer, .poster, .pm-video-thumb")
                    val style = styleEl?.attr("style") ?: ""
                    val bgMatch = Regex("""url\(['"]?(https?://[^'")\s]+)['"]?\)""").find(style)
                    if (bgMatch != null) poster = bgMatch.groupValues[1]
                }

                // 4. Try any child element's background-image style
                if (poster == null) {
                    for (child in el.allElements) {
                        val style = child.attr("style")
                        if (style.contains("background-image")) {
                            val bgMatch = Regex("""url\(['"]?(https?://[^'")\s]+)['"]?\)""").find(style)
                            if (bgMatch != null) { poster = bgMatch.groupValues[1]; break }
                        }
                    }
                }



                // Quality

                var quality = ""

                for (qs in provider.qualitySelectors) {

                    quality = el.selectFirst(qs)?.text()?.trim() ?: ""

                    if (quality.isNotBlank()) break

                }

                if (quality.isBlank()) quality = Regex("(?i)(4K|1080p|720p|480p|WEB-DL|BluRay|HD)")

                    .find("$href $title")?.value ?: "HD"



                // Year

                var year = ""

                for (ys in provider.yearSelectors) {

                    year = Regex("""\b(20\d\d|19\d\d)\b""")

                        .find(el.selectFirst(ys)?.text() ?: "")?.value ?: ""

                    if (year.isNotBlank()) break

                }

                if (year.isBlank()) year = Regex("""\b(20\d\d|19\d\d)\b""").find(href)?.value ?: ""



                val isSeries = defaultType == ArabseedType.SERIES

                    || href.contains("/series/") || href.contains("/serie/") || href.contains("/مسلسل")

                    || title.contains("مسلسل")
                    || SeriesCatalogParser.isEpisode(title, href)
                    || SeriesCatalogParser.seasonNumber(title, href) != null



                val cleanTitle = title

                    .replace(Regex("""(?i)(مترجم|مدبلج|مشاهدة|تحميل|اون لاين|فيلم|مسلسل|HD|4K|1080p|720p)"""), "")

                    .replace(Regex("""\s{2,}"""), " ").trim()



                items += ArabseedItem(

                    id = "${provider.id}_${href.trimEnd('/').substringAfterLast('/').take(60)}",

                    title = cleanTitle.ifBlank { title },

                    arabicTitle = title,

                    posterUrl = poster,

                    year = year,

                    quality = quality,

                    rating = "",

                    type = if (isSeries) ArabseedType.SERIES else ArabseedType.MOVIE,

                    pageUrl = href,

                    category = if (isSeries) "مسلسل" else "فيلم",

                    overview = provider.name,

                    providerName = provider.name,

                )

            }

            if (items.isNotEmpty()) break

        }

        return items.distinctBy { it.pageUrl }

    }



    // ══════════════════════════════════════════════════════════════════

    // PUBLIC LISTING & SEARCH

    // ══════════════════════════════════════════════════════════════════



    fun fetchMoviesAll(
        page: Int = 1,
        forceRefresh: Boolean = false,
        onPartial: ((List<ArabseedItem>) -> Unit)? = null,
    ): List<ArabseedItem> = fetchCatalogAll(page, ArabseedType.MOVIE, forceRefresh, onPartial)

    fun fetchSeriesAll(
        page: Int = 1,
        forceRefresh: Boolean = false,
        onPartial: ((List<ArabseedItem>) -> Unit)? = null,
    ): List<ArabseedItem> = fetchCatalogAll(page, ArabseedType.SERIES, forceRefresh, onPartial)

    /** Fetch both media types from every configured provider in parallel. */
    fun fetchCatalogAll(
        page: Int = 1,
        forceRefresh: Boolean = false,
        onPartial: ((List<ArabseedItem>) -> Unit)? = null,
    ): List<ArabseedItem> = fetchCatalogAll(page, type = null, forceRefresh = forceRefresh, onPartial = onPartial)

    private fun fetchCatalogAll(
        page: Int,
        type: ArabseedType?,
        forceRefresh: Boolean,
        onPartial: ((List<ArabseedItem>) -> Unit)? = null,
    ): List<ArabseedItem> {
        val cacheType = when (type) {
            ArabseedType.MOVIE -> "movies"
            ArabseedType.SERIES -> "series"
            null -> "catalog"
        }
        val key = "$cacheType:$page"
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            listCache[key]?.let { (savedAt, items) ->
                if (now - savedAt < LIST_TTL && items.isNotEmpty()) return items
            }
            val snapshot = CatalogDiskCache.loadItems("providers:list:$key")
            if (snapshot != null) {
                listCache[key] = snapshot.savedAt to snapshot.value
                if (now - snapshot.savedAt < LIST_TTL) return snapshot.value
            }
        }

        val types = if (type == null) {
            listOf(ArabseedType.MOVIE, ArabseedType.SERIES)
        } else {
            listOf(type)
        }
        val tasks = mutableListOf<() -> List<ArabseedItem>>()
        for (provider in PROVIDERS.filter { it.enabled }) {
            for (catalogType in types) {
                tasks += {
                    try {
                        val path = if (catalogType == ArabseedType.MOVIE) provider.moviesPath else provider.seriesPath
                        val url = "${provider.baseUrl}${path.replace("{page}", page.toString())}"
                        val html = fetchHtml(url, "${provider.baseUrl}/")
                        val parsed = parseItems(html, provider, catalogType)
                        if (html.isNotBlank() && parsed.isEmpty()) {
                            android.util.Log.w(TAG, "No catalog cards parsed for provider=${provider.name} type=$catalogType")
                        }
                        parsed
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
        }

        // The home catalog requests movies and series from each provider. With a six-thread pool
        // those twelve requests run in two waves, so allow both waves to finish while partial rows
        // continue reaching the UI immediately.
        val combined = collectParallel(
            tasks,
            timeoutSeconds = if (type == null) 16L else 8L,
        ) { batch ->
            if (batch.isNotEmpty()) onPartial?.invoke(deduplicateItems(batch, expectedType = type))
        }
        val deduped = deduplicateItems(combined, expectedType = type)
        // Keep catalog refresh bounded: AsyncImage loads poster images directly,
        // so opening one page here per missing poster only delays the first screen.
        val enriched = deduped
        if (enriched.isNotEmpty()) {
            listCache[key] = System.currentTimeMillis() to enriched
            CatalogDiskCache.saveItems("providers:list:$key", enriched)
        }
        return enriched
    }

    /** Clear catalog caches so a manual refresh reaches providers again. */
    fun clearCatalogCache() {
        listCache.clear()
        searchCache.clear()
    }

    private fun collectParallel(
        tasks: List<() -> List<ArabseedItem>>,
        timeoutSeconds: Long = 8L,
        onPartial: ((List<ArabseedItem>) -> Unit)? = null,
    ): List<ArabseedItem> {
        if (tasks.isEmpty()) return emptyList()
        val completion = ExecutorCompletionService<List<ArabseedItem>>(pool)
        val submitted = tasks.map { task -> completion.submit(Callable { task() }) }
        val results = mutableListOf<ArabseedItem>()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        for (index in tasks.indices) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) break
            val finished = try {
                completion.poll(remaining, TimeUnit.NANOSECONDS) ?: break
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            try {
                val batch = finished.get()
                results += batch
                if (batch.isNotEmpty()) onPartial?.invoke(batch)
            } catch (_: Exception) {
                // A failing source does not discard results from the others.
            }
        }
        submitted.filterNot { it.isDone }.forEach { it.cancel(true) }
        return results
    }
    fun searchAll(query: String, forceRefresh: Boolean = false): List<ArabseedItem> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val key = "search:${q.lowercase()}"
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            searchCache[key]?.let { (savedAt, items) ->
                if (now - savedAt < SEARCH_TTL && items.isNotEmpty()) return items
            }
            val snapshot = CatalogDiskCache.loadItems("providers:" + key)
            if (snapshot != null) {
                searchCache[key] = snapshot.savedAt to snapshot.value
                if (now - snapshot.savedAt < SEARCH_TTL) return snapshot.value
            }
        }

        val tasks = PROVIDERS.filter { it.enabled }.map { provider ->
            {
                try {
                    val encodedQuery = java.net.URLEncoder.encode(q, "UTF-8")
                    val url = "${provider.baseUrl}${provider.searchPath.replace("{q}", encodedQuery)}"
                    parseItems(fetchHtml(url, "${provider.baseUrl}/"), provider, ArabseedType.MOVIE)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
        val results = deduplicateItems(collectParallel(tasks))
        if (results.isNotEmpty()) {
            searchCache[key] = System.currentTimeMillis() to results
            CatalogDiskCache.saveItems("providers:" + key, results)
        }
        return results
    }

    /** Merge results from different catalog sources and return one playable row per work. */
    fun mergeCatalogItems(
        items: List<ArabseedItem>,
        expectedType: ArabseedType? = null,
    ): List<ArabseedItem> = deduplicateItems(items, expectedType)
    // ══════════════════════════════════════════════════════════════════

    // STORY, SERVERS, DETAIL & SEASONS FOR ALL PROVIDERS

    // ══════════════════════════════════════════════════════════════════



    fun fetchStoryAndCast(pageUrl: String): Pair<String, List<String>> {

        try {

            val html = fetchHtml(pageUrl)

            if (html.isBlank()) return "" to emptyList()

            val doc = Jsoup.parse(html)

            val selectors = listOf(

                ".post__story", ".story", ".Story", ".entry__desc", "div.desc",

                ".overview", ".description", ".entry-content p", ".post-story",

                ".story-content", "#story", "p.story", "div.story",

            )

            var overview = ""

            for (s in selectors) {

                val txt = doc.selectFirst(s)?.text()?.trim() ?: ""

                if (txt.length > 20 && !txt.contains("تحميل ومشاهدة")) {

                    overview = txt

                    break

                }

            }

            val cast = linkedSetOf<String>()

            doc.select("[itemprop=actor], [itemprop=actors], .actors a, .Actors a, .actor a, .cast a, .Cast a, .cast-list a, .post__actors a, .persons a")

                .forEach { node ->

                    val name = node.attr("content").ifBlank { node.selectFirst("[itemprop=name]")?.text()?.trim().orEmpty().ifBlank { node.text().trim() } }

                    if (name.isNotBlank() && name.length < 80) cast += name.replace(Regex("\\s+"), " ")

                }

            return overview to cast.toList()

        } catch (_: Exception) {}

        return "" to emptyList()

    }



    fun fetchStory(pageUrl: String): String = fetchStoryAndCast(pageUrl).first



    fun fetchServers(pageUrl: String): List<ArabseedServer> {

        val servers = mutableListOf<ArabseedServer>()

        val provider = getProviderForUrl(pageUrl)

        val baseUrl = provider?.baseUrl ?: ""



        try {

            // 1. Akwam Servers (watch/.../servers)

            if (pageUrl.contains("akw")) {

                val watchUrl = pageUrl.replace("/akfilm-", "/watch/akfilm-")

                    .replace("/akseries-", "/watch/akseries-")

                    .trimEnd('/') + "/servers"

                val watchHtml = fetchHtml(watchUrl, "https://akw.cam/")

                val wDoc = Jsoup.parse(watchHtml, "https://akw.cam/")

                val primaryIframe = wDoc.selectFirst("iframe")?.attr("src")?.trim() ?: ""

                if (primaryIframe.startsWith("http")) {

                    servers += ArabseedServer("سيرفر 1 (فائق السرعة)", primaryIframe)

                }

                for (el in wDoc.select("[data-url], [data-src], a.server, .watchServers a, iframe[src]")) {

                    val sUrl = el.attr("data-url").ifBlank { el.attr("data-src").ifBlank { el.attr("src").ifBlank { el.attr("href") } } }.trim()

                    val sName = el.text().trim().replace(Regex("(?i)(أكوام|akwam)"), "").trim()

                    if (sUrl.startsWith("http") && servers.none { it.embedUrl == sUrl }) {

                        val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""

                        servers += ArabseedServer("سيرفر ${servers.size + 1}$label", sUrl)

                    }

                }

            }



            // 2. WeCima Servers (embed.php?vid=... & downloads.php?vid=...)

            if (pageUrl.contains("wecima") || pageUrl.contains("myweecima")) {

                val vid = Regex("vid=([a-zA-Z0-9]+)").find(pageUrl)?.groupValues?.get(1) ?: ""

                if (vid.isNotBlank()) {

                    servers += ArabseedServer("سيرفر 1 (فائق السرعة)", "https://myweecima.shop/embed.php?vid=$vid")

                    try {

                        val dlHtml = fetchHtml("https://myweecima.shop/downloads.php?vid=$vid", pageUrl)

                        val dlDoc = Jsoup.parse(dlHtml, "https://myweecima.shop/")

                        for (a in dlDoc.select("a[href]")) {

                            val h = a.attr("href").trim()

                            val sName = a.text().trim().replace(Regex("(?i)(وي سيما|wecima)"), "").trim()

                            if (h.startsWith("http") && (

                                h.contains("uqload") || h.contains("vikingfile") || h.contains("turboviplay") ||

                                h.contains("usersdrive") || h.contains("gofile") || h.contains("vidspeed") ||

                                h.contains("forafile") || h.contains("down.") || h.contains("vidoba")

                            )) {

                                if (servers.none { it.embedUrl == h }) {

                                    val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""

                                    servers += ArabseedServer("سيرفر ${servers.size + 1}$label", h)

                                }

                            }

                        }

                    } catch (_: Exception) {}

                }

            }



            // 3. FaselHD special embed buttons -> AJAX call to /ajax/embed

            if (pageUrl.contains("fasel")) {

                val fHtml = fetchHtml(pageUrl, baseUrl)

                val fDoc = Jsoup.parse(fHtml, baseUrl)

                val embedButtons = fDoc.select("button.btn-service[data-embed], [data-embed]")

                val futures = mutableListOf<Future<Pair<String, String>?>>()

                for ((idx, btn) in embedButtons.take(6).withIndex()) {

                    val embedId = btn.attr("data-embed").trim()

                    val sName = btn.text().trim().replace(Regex("(?i)(فاصل|fasel)"), "").trim()

                    if (embedId.isNotBlank()) {

                        futures += pool.submit<Pair<String, String>?> {

                            try {

                                val formBody = FormBody.Builder().add("id", embedId).build()

                                val postReq = Request.Builder()

                                    .url("https://wb49584.fasel4k.site/ajax/embed")

                                    .post(formBody)

                                    .header("User-Agent", UA)

                                    .header("Referer", pageUrl)

                                    .header("X-Requested-With", "XMLHttpRequest")

                                    .build()

                                val respHtml = http.newCall(postReq).execute().use { it.body?.string() ?: "" }

                                val ifr = Jsoup.parse(respHtml).selectFirst("iframe")?.attr("src")?.trim() ?: ""

                                if (ifr.startsWith("http")) sName to ifr else null

                            } catch (_: Exception) { null }

                        }

                    }

                }

                for (f in futures) {

                    try {

                        val pair = f.get(3, TimeUnit.SECONDS)

                        if (pair != null && servers.none { it.embedUrl == pair.second }) {

                            val label = if (pair.first.isNotBlank() && pair.first.length < 25) " (${pair.first})" else ""

                            servers += ArabseedServer("سيرفر ${servers.size + 1}$label", pair.second)

                        }

                    } catch (_: Exception) {}

                }

            }



            // 4. Standard iframes and data-embed attributes (works across EgyBest, Lodynet, Prestige)

            val html = fetchHtml(pageUrl, baseUrl)

            if (html.isNotBlank()) {

                val doc = Jsoup.parse(html, baseUrl)

                for (ifr in doc.select("iframe[src]")) {

                    val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()

                    if (src.startsWith("http") && !src.contains("googletag") && !src.contains("facebook")) {

                        if (servers.none { it.embedUrl == src }) {

                            servers += ArabseedServer("سيرفر ${servers.size + 1}", src)

                        }

                    }

                }

                for (el in doc.select("[data-embed], [data-src], [data-link], .servers a, a.server-btn, .watch-buttons a")) {

                    val link = el.attr("data-embed").ifBlank {

                        el.attr("data-src").ifBlank { el.attr("data-link").ifBlank { el.attr("href") } }

                    }.trim()

                    val sName = el.text().trim().replace(Regex("(?i)(أكوام|فاصل|وي سيما|إيجي بست|لودي نت)"), "").trim()

                    if (link.startsWith("http") && !link.contains("google") && servers.none { it.embedUrl == link }) {

                        val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""

                        servers += ArabseedServer("سيرفر ${servers.size + 1}$label", link)

                    }

                }

            }

        } catch (_: Exception) {}



        return servers.take(10)

    }



    fun fetchDetail(item: ArabseedItem): ArabseedDetail {
        val now = System.currentTimeMillis()
        detailCache[item.pageUrl]?.let { cached ->
            if (cached.servers.isNotEmpty() && now - (detailCacheAt[item.pageUrl] ?: now) < DETAIL_TTL) return cached
        }

        var pageMetadata: Pair<String, List<String>> = "" to emptyList()
        var servers = emptyList<ArabseedServer>()

        try {
            pageMetadata = fetchStoryAndCast(item.pageUrl)
        } catch (_: Exception) {}

        try {
            servers = fetchServers(item.pageUrl)
        } catch (_: Exception) {}

        val detail = ArabseedDetail(
            item = item.copy(overview = pageMetadata.first.ifBlank { item.overview }),
            servers = servers,
            preResolvedStream = servers.firstOrNull()?.directStreamUrl,
            cast = pageMetadata.second,
        )

        if (servers.isNotEmpty()) {
            detailCache[item.pageUrl] = detail
            detailCacheAt[item.pageUrl] = System.currentTimeMillis()
            CatalogDiskCache.saveDetail("providers:detail:" + item.pageUrl, detail)
        }

        return detail
    }



    fun fetchSeasonsAndEpisodes(
        item: ArabseedItem,
        forceRefresh: Boolean = false,
        seasonNumbersOnly: Set<Int>? = null,
    ): ArabseedSeasonBundle {
        val now = System.currentTimeMillis()
        if (!forceRefresh) seasonCache[item.pageUrl]?.let { cached ->
            if (now - (seasonCacheAt[item.pageUrl] ?: now) < SEASON_TTL) return cached
        }
        val provider = getProviderForUrl(item.pageUrl)
        val baseUrl = provider?.baseUrl ?: ""

        return try {
            val html = fetchHtml(item.pageUrl, baseUrl)
            val doc = Jsoup.parse(html, baseUrl)
            doc.select("footer, script, style").remove()

            val rawEpisodes = mutableListOf<Triple<String, String, Int?>>()
            val selectors = listOf(
                ".episodes-list a", ".EpisodesList li a", ".eplister li a",
                ".eps-list li a", ".eps a", "a.episode__item", ".episode__item a", "a.ep-item",
                "a[href*='%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-']", "a[href*='الحلقة']",
                "a[href*='episode']", "a[href*='ep-']", ".postDiv a[href*='serie']",
                ".episodes-list [data-url], .episodes-list [data-href]",
                ".EpisodesList [data-url], .EpisodesList [data-href]",
                ".eplister [data-url], .eplister [data-href]",
                "[data-episode][data-url], [data-episode][data-href], [data-episode-link]"
            )

            fun collectEpisodes(d: org.jsoup.nodes.Document): List<Triple<String, String, Int?>> {
                val collected = linkedMapOf<String, Triple<String, String, Int?>>()
                for (sel in selectors) {
                    val els = d.select(sel)
                    if (els.isNotEmpty()) {
                        for (el in els) {
                            val href = listOf("href", "data-url", "data-href", "data-link", "data-episode-link")
                                .firstNotNullOfOrNull { attribute ->
                                    el.absUrl(attribute).ifBlank { el.attr(attribute).trim() }
                                        .takeIf { it.startsWith("http", ignoreCase = true) }
                                }.orEmpty()
                            val txt = el.text().trim().ifBlank { el.attr("title").trim() }
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
                            if (href.startsWith("http") && episodeNumber != null) {
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

            val currentPage = SeriesCatalogParser.normalizedUrl(item.pageUrl)
            val isEpisodePage = SeriesCatalogParser.isEpisode(item.title, item.pageUrl)
                || SeriesCatalogParser.isEpisode(item.latestEpisodeLabel, item.pageUrl)
            val seasonDiscoveryDocs = mutableListOf(doc)

            // Episode cards may not have a canonical series URL. Resolve their breadcrumb
            // parent even when the episode page exposes several links: those links can span
            // multiple seasons and must not be treated as one season's episode list.
            if ((rawEpisodes.size <= 4 || isEpisodePage) &&
                !(seasonNumbersOnly != null && SeriesCatalogParser.seasonNumber(item.title, item.pageUrl) != null)
            ) {
                val parentSelectors = listOf(
                    "a[href*='/series/']", "a[href*='/season/']", "a[href*='الموسم']",
                    ".breadcrumb a", "ol.breadcrumb a", ".entry-breadcrumb a", "nav a"
                )
                val parents = parentSelectors.flatMap { selector ->
                    doc.select(selector).mapNotNull { el ->
                        val href = el.attr("abs:href").ifBlank { el.attr("href") }
                        val txt = el.text().trim()
                        val valid = href.startsWith("http") && href != item.pageUrl
                            && !txt.contains("الرئيسية") && !txt.contains("home", ignoreCase = true)
                            && !SeriesCatalogParser.isEpisode(txt, href)
                        if (valid) href to txt else null
                    }
                }.distinctBy { SeriesCatalogParser.normalizedUrl(it.first) }
                    .sortedBy { (href, title) ->
                        when {
                            href.contains("/series/", ignoreCase = true) ||
                                href.contains("/selary/", ignoreCase = true) -> 0
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
                        val parentDoc = Jsoup.parse(fetchHtml(parentHref, baseUrl), baseUrl)
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

                // Evaluate the best canonical page first, regardless of network completion order.
                val orderedParents = parentResults.sortedWith(
                    compareByDescending<Triple<String, String, org.jsoup.nodes.Document>> {
                        SeriesCatalogParser.seasonNumber(it.second, it.first) == null &&
                            SeriesCatalogParser.partNumber(it.second, it.first) == null
                    }.thenByDescending { collectEpisodes(it.third).size },
                )
                for ((parentHref, parentTitle, parentDoc) in orderedParents) {
                    val parentEps = collectEpisodes(parentDoc)
                    val parentContainsCurrentEpisode = parentEps.any {
                        SeriesCatalogParser.normalizedUrl(it.second) == currentPage
                    }
                    val shouldUseParent = parentEps.isNotEmpty() && (
                        if (isEpisodePage) parentContainsCurrentEpisode
                        else parentEps.size > rawEpisodes.size
                    )
                    if (!shouldUseParent) continue
                    val isCanonical = SeriesCatalogParser.seasonNumber(parentTitle, parentHref) == null &&
                        SeriesCatalogParser.partNumber(parentTitle, parentHref) == null
                    val rank = if (isCanonical) 2 else 1
                    if (rank > selectedParentRank ||
                        (rank == selectedParentRank && parentEps.size > selectedParentEpisodeCount)
                    ) {
                        rawEpisodes.clear()
                        rawEpisodes.addAll(parentEps)
                        selectedParentRank = rank
                        selectedParentEpisodeCount = parentEps.size
                    }
                    seasonDiscoveryDocs += parentDoc
                    // Prefer the canonical series page. Its season tabs reveal the whole series.
                    if (isCanonical) break
                }
            }

            // A number of providers render only one selected season in the page body.
            // Follow explicit season tabs when their visible list is missing or incomplete.
            val seasonPages = seasonDiscoveryDocs.asReversed()
                .flatMap { SeriesCatalogParser.linkedSeasonPages(it, item.pageUrl) }
                .distinctBy { it.first }
            val seasonPageUrlByNumber = seasonPages.groupBy { it.first }
                .mapValues { (_, rows) -> rows.first().second }
            val visibleCounts = rawEpisodes.mapNotNull { (title, href, seasonNumber) ->
                val episodeNumber = SeriesCatalogParser.episodeNumber(title, href) ?: return@mapNotNull null
                val resolvedSeason = SeriesCatalogParser.seasonNumber(title, href)
                    ?: seasonNumber ?: return@mapNotNull null
                resolvedSeason to episodeNumber
            }.groupingBy { it.first }.eachCount()
            val seasonPagesToFetch = seasonPages
                .filter { (number, _) ->
                    (seasonNumbersOnly == null || number in seasonNumbersOnly) &&
                        (forceRefresh || (visibleCounts[number] ?: 0) < 2)
                }
                .take(30)
            val seasonPageCompletion = ExecutorCompletionService<Pair<Int, List<Triple<String, String, Int?>>>>(seasonPagePool)
            val seasonPageFutures = seasonPagesToFetch.map { (seasonNumber, seasonUrl) ->
                seasonPageCompletion.submit(Callable {
                    val seasonDoc = Jsoup.parse(fetchHtml(seasonUrl, baseUrl), baseUrl)
                    seasonDoc.select("footer, script, style").remove()
                    seasonNumber to collectEpisodes(seasonDoc)
                })
            }
            val seasonPageWaitSeconds = 7L
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
                for ((title, href, domSeason) in linkedEpisodes) {
                    val episodeNumber = SeriesCatalogParser.episodeNumber(title, href) ?: continue
                    val urlKey = SeriesCatalogParser.normalizedUrl(href)
                    val explicitSeason = SeriesCatalogParser.seasonNumber(title, href)
                    val resolvedSeason = explicitSeason ?: domSeason ?: linkedSeasonNumber
                    val existingIndex = rawEpisodes.indexOfFirst {
                        SeriesCatalogParser.normalizedUrl(it.second) == urlKey
                    }
                    if (existingIndex < 0) {
                        rawEpisodes += Triple(title, href, resolvedSeason)
                        seasonPageAssignments += urlKey
                    } else if (urlKey !in seasonPageAssignments && explicitSeason == null) {
                        rawEpisodes[existingIndex] = Triple(title, href, resolvedSeason)
                        seasonPageAssignments += urlKey
                    } else if (explicitSeason != null) {
                        rawEpisodes[existingIndex] = Triple(title, href, explicitSeason)
                        seasonPageAssignments += urlKey
                    }
                }
            }
            seasonPageFutures.forEach { if (!it.isDone) it.cancel(true) }

            val expectedSeasonNumber = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
                ?: SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")
            val parsedEpisodes = rawEpisodes.mapNotNull { (title, href, domSeasonNumber) ->
                val episodeNumber = SeriesCatalogParser.episodeNumber(title, href) ?: return@mapNotNull null
                val seasonNumber = domSeasonNumber
                    ?: SeriesCatalogParser.seasonNumber(title, href)
                Triple(seasonNumber, episodeNumber, href)
            }.distinctBy { (_, _, href) -> SeriesCatalogParser.normalizedUrl(href) }
            val labelledSeasons = parsedEpisodes.mapNotNull { it.first }.distinct()
            val inferredSeasonNumber = when {
                labelledSeasons.size > 1 -> null
                labelledSeasons.size == 1 -> labelledSeasons.single()
                expectedSeasonNumber != null -> expectedSeasonNumber
                !isEpisodePage -> 1
                else -> 1
            }
            val numberedEpisodes = parsedEpisodes.mapNotNull { (seasonNumber, episodeNumber, href) ->
                (seasonNumber ?: inferredSeasonNumber)?.let { Triple(it, episodeNumber, href) }
            }
            val seasons = numberedEpisodes.groupBy { it.first }.toSortedMap().map { (seasonNumber, rows) ->
                val episodes = rows
                    .distinctBy { it.second }
                    .sortedBy { it.second }
                    .map { (_, episodeNumber, href) ->
                        ArabseedEpisode(
                            number = episodeNumber,
                            title = "الحلقة $episodeNumber",
                            pageUrl = href,
                            seasonNumber = seasonNumber,
                            providerName = getProviderForUrl(item.pageUrl)?.name ?: item.providerName,
                        )
                    }
                ArabseedSeason(
                    seasonNumber,
                    SeriesCatalogParser.seasonTitle(seasonNumber),
                    episodes,
                    seasonPageUrlByNumber[seasonNumber].orEmpty(),
                )
            }
            val seasonsWithLinks = (seasons + seasonPages.mapNotNull { (number, url) ->
                if (seasons.any { it.number == number }) null
                else ArabseedSeason(number, SeriesCatalogParser.seasonTitle(number), emptyList(), url)
            }).map { season ->
                if (season.pageUrl.isNotBlank()) season
                else season.copy(pageUrl = seasonPageUrlByNumber[season.number].orEmpty())
            }

            val relatedParts = linkedMapOf<String, ArabseedItem>()
            val explicitPartSectionUrls = mutableSetOf<String>()
            val partSelectors = listOf(
                "a[href*='part']", "a[href*='الجزء']",
                ".parts a", ".parts-list a",
            )
            for (selector in partSelectors) {
                for (node in doc.select(selector)) {
                    val href = node.attr("abs:href").ifBlank { node.attr("href") }
                    val title = node.text().trim().replace(Regex("\\s+"), " ")
                    val isEp = SeriesCatalogParser.isEpisode(title, href)
                    val labelMatches = SeriesCatalogParser.hasPartMarker(title, href)
                    val isSeason = SeriesCatalogParser.seasonNumber(title, href) != null
                    val inExplicitPartSection = selector.startsWith(".")

                    if (!isEp && href.startsWith("http") && href != item.pageUrl && title.isNotBlank() &&
                        (labelMatches || isSeason || inExplicitPartSection)
                    ) {
                        val img = node.selectFirst("img")?.attr("abs:src")
                            ?.ifBlank { node.selectFirst("img")?.attr("src") }
                            ?: item.posterUrl
                        if (inExplicitPartSection) {
                            explicitPartSectionUrls += SeriesCatalogParser.normalizedUrl(href)
                        }
                        relatedParts.putIfAbsent(href, item.copy(
                            id = "${item.id}_${href.hashCode()}",
                            title = title,
                            arabicTitle = title,
                            pageUrl = href,
                            posterUrl = img,
                            overview = "",
                        ))
                    }
                }
                if (relatedParts.isNotEmpty()) break
            }

            val cleanParts = relatedParts.values
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
            val bundle = SeriesCatalogParser.normalizeSeasonBundle(
                ArabseedSeasonBundle(seasons = seasonsWithLinks, relatedParts = cleanParts),
            )
            if (bundle.seasons.isNotEmpty() || bundle.relatedParts.isNotEmpty()) {
                seasonCache[item.pageUrl] = bundle
                seasonCacheAt[item.pageUrl] = System.currentTimeMillis()
                CatalogDiskCache.saveSeasonBundle("providers:seasons:v7:" + item.pageUrl, bundle)
            }
            bundle
        } catch (_: Exception) {
            ArabseedSeasonBundle(emptyList())
        }
    }

    /** Merge the selected provider's season data with exact-title matches from the
     * other configured providers. Catalog cards are deduplicated for display, so this
     * lookup deliberately runs provider searches again to retain each source URL. */
    fun fetchSeasonBundleAcrossProviders(
        item: ArabseedItem,
        primaryBundle: ArabseedSeasonBundle,
        forceRefresh: Boolean = false,
    ): ArabseedSeasonBundle {
        val cacheKey = "combined:seasons:v5:${item.pageUrl}"
        if (!forceRefresh) loadCachedMergedSeasonBundle(item.pageUrl)?.let { cached ->
            if (!needsSeasonEnrichment(item, cached) &&
                (primaryBundle.seasons.isEmpty() ||
                    cached.seasons.sumOf { it.episodes.size } >= primaryBundle.seasons.sumOf { it.episodes.size })
            ) {
                return cached
            }
        }

        val title = SeriesCatalogParser.cleanSeriesTitle(
            item.parentSeriesTitle?.takeIf(String::isNotBlank) ?: item.title,
        )
        val titleKey = seriesIdentityKey(title)
        if (titleKey.length < 3 || !item.pageUrl.startsWith("http", ignoreCase = true)) return primaryBundle

        val year = releaseYear(item)
        val searchTasks = PROVIDERS.filter { it.enabled }.map { provider ->
            {
                try {
                    val encoded = java.net.URLEncoder.encode(title, "UTF-8")
                    val searchUrl = "${provider.baseUrl}${provider.searchPath.replace("{q}", encoded)}"
                    val html = fetchHtml(searchUrl, "${provider.baseUrl}/")
                    parseItems(html, provider, ArabseedType.SERIES)
                        .asSequence()
                        .filter { candidate ->
                            val exactTitleMatch = seriesIdentityKeys(candidate).any { it == titleKey }
                            val candidateYear = releaseYear(candidate)
                            val compatibleYear = year.isBlank() || candidateYear.isBlank() || year == candidateYear
                            candidate.type == ArabseedType.SERIES &&
                                !SeriesCatalogParser.isEpisode(candidate.title, candidate.pageUrl) &&
                                matchesSeriesTitle(titleKey, candidate) &&
                                (exactTitleMatch || compatibleYear)
                        }
                        .distinctBy { SeriesCatalogParser.normalizedUrl(it.pageUrl) }
                        .toList()
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        val discovered = collectParallel(searchTasks, timeoutSeconds = 5)
        val primaryUrl = SeriesCatalogParser.normalizedUrl(item.pageUrl)
        val candidates = selectSeriesPages(discovered)
            .filter { SeriesCatalogParser.normalizedUrl(it.pageUrl) != primaryUrl }
            .distinctBy { SeriesCatalogParser.normalizedUrl(it.pageUrl) }
        val primaryEpisodeCounts = primaryBundle.seasons.associate { season ->
            season.number to season.episodes.map { it.number }.distinct().size
        }
        val seasonPagesByNumber = candidates
            .mapNotNull { candidate ->
                SeriesCatalogParser.seasonNumber(candidate.title, candidate.pageUrl)?.let { it to candidate }
            }
            .groupBy({ it.first }, { it.second })
        val seasonsToRepair = (primaryEpisodeCounts.keys + seasonPagesByNumber.keys)
            .distinct()
            .filter { (primaryEpisodeCounts[it] ?: 0) < 2 }
            .sorted()
        val seasonPageMirrors = seasonsToRepair.flatMap { seasonNumber ->
            seasonPagesByNumber[seasonNumber].orEmpty()
                .groupBy { getProviderForUrl(it.pageUrl)?.id ?: it.providerName }
                .values
                .mapNotNull { rows -> rows.maxByOrNull(::catalogMetadataScore) }
                .take(2)
                .map { it to setOf(seasonNumber) }
        }
        val canonicalMirrors = candidates
            .filter { SeriesCatalogParser.seasonNumber(it.title, it.pageUrl) == null }
            .groupBy { getProviderForUrl(it.pageUrl)?.id ?: it.providerName }
            .values
            .mapNotNull { rows -> rows.maxByOrNull(::catalogMetadataScore) }
            .take(3)
            .map {
                val missingSeasons = seasonsToRepair.toSet()
                it to missingSeasons.takeIf { numbers -> numbers.isNotEmpty() }
            }
        val mirrors = (seasonPageMirrors + canonicalMirrors)
            .distinctBy { SeriesCatalogParser.normalizedUrl(it.first.pageUrl) }
            .take(8)

        val bundles = mutableListOf(item to primaryBundle)
        val completion = ExecutorCompletionService<Pair<ArabseedItem, ArabseedSeasonBundle>>(seasonMirrorPool)
        val futures = mirrors.map { (mirror, seasonNumbers) ->
            completion.submit(Callable {
                mirror to fetchSeasonsAndEpisodes(
                    mirror,
                    forceRefresh = true,
                    seasonNumbersOnly = seasonNumbers,
                )
            })
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        repeat(futures.size) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) return@repeat
            val finished = try {
                completion.poll(remaining, TimeUnit.NANOSECONDS) ?: return@repeat
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return@repeat
            }
            val completedBundle = runCatching { finished.get() }.getOrNull() ?: return@repeat
            bundles += completedBundle
            val partial = mergeProviderSeasonBundles(bundles)
            if (partial.seasons.isNotEmpty() || partial.relatedParts.isNotEmpty()) {
                CatalogDiskCache.saveSeasonBundle(cacheKey, partial)
            }
        }
        futures.filterNot { it.isDone }.forEach { it.cancel(true) }

        val merged = mergeProviderSeasonBundles(bundles)
        if (merged.seasons.isNotEmpty() || merged.relatedParts.isNotEmpty()) {
            CatalogDiskCache.saveSeasonBundle(cacheKey, merged)
        }
        return merged
    }

    /** Return cached/primary data immediately and continue cross-provider enrichment off the UI request. */
    fun fetchSeasonBundleWithBackgroundMerge(
        item: ArabseedItem,
        primaryBundle: ArabseedSeasonBundle,
        forceRefresh: Boolean = false,
    ): ArabseedSeasonBundle {
        val cached = loadCachedMergedSeasonBundle(item.pageUrl)
        if (!forceRefresh && !shouldRefreshSeasonBundle(item)) {
            return cached?.let { mergeProviderSeasonBundles(listOf(item to it, item to primaryBundle)) }
                ?: primaryBundle
        }

        val key = SeriesCatalogParser.normalizedUrl(item.pageUrl)
        if (key.isNotBlank() && seasonAggregationInFlight.add(key)) {
            CatalogDiskCache.saveJson(
                seasonEnrichmentAttemptKey(item.pageUrl),
                JSONObject().put("attemptedAt", System.currentTimeMillis()),
            )
            seasonAggregationPool.submit {
                try {
                    fetchSeasonBundleAcrossProviders(item, primaryBundle, forceRefresh)
                } catch (error: Exception) {
                    android.util.Log.w(TAG, "Season enrichment failed for provider=${item.providerName}", error)
                } finally {
                    seasonAggregationInFlight.remove(key)
                }
            }
        }

        val latestCached = loadCachedMergedSeasonBundle(item.pageUrl) ?: cached
        return latestCached?.let { mergeProviderSeasonBundles(listOf(item to it, item to primaryBundle)) }
            ?: primaryBundle
    }

    fun isSeasonAggregationRunning(pageUrl: String): Boolean =
        SeriesCatalogParser.normalizedUrl(pageUrl) in seasonAggregationInFlight

    fun shouldRefreshSeasonBundle(item: ArabseedItem): Boolean {
        if (item.type != ArabseedType.SERIES) return false
        if (CatalogDiskCache.loadJson(
                seasonEnrichmentAttemptKey(item.pageUrl),
                SEASON_ENRICHMENT_RETRY_MS,
            ) != null
        ) return false
        val cached = loadCachedMergedSeasonBundle(item.pageUrl) ?: return true
        return needsSeasonEnrichment(item, cached)
    }

    fun seasonNumbersNeedingRepair(bundle: ArabseedSeasonBundle?): Set<Int> {
        if (bundle == null) return emptySet()
        val seasonNumbers = bundle.seasons.map { it.number }.filter { it > 0 }.distinct().sorted()
        val incomplete = bundle.seasons
            .filter { it.number > 0 && it.episodes.size <= 1 }
            .mapTo(linkedSetOf()) { it.number }
        if (seasonNumbers.size > 1) {
            for (number in seasonNumbers.first()..seasonNumbers.last()) {
                if (number !in seasonNumbers) incomplete += number
            }
        }
        return incomplete
    }

    private fun seasonEnrichmentAttemptKey(pageUrl: String): String =
        "combined:season-attempt:v1:$pageUrl"

    private fun needsSeasonEnrichment(item: ArabseedItem, bundle: ArabseedSeasonBundle): Boolean {
        if (item.type != ArabseedType.SERIES) return false
        if (bundle.seasons.isEmpty()) return true
        if (bundle.seasons.any { it.episodes.size <= 1 }) return true
        val numbers = bundle.seasons.map { it.number }.filter { it > 0 }.distinct().sorted()
        return numbers.size > 1 && numbers.last() - numbers.first() + 1 > numbers.size
    }

    private fun matchesSeriesTitle(targetKey: String, candidate: ArabseedItem): Boolean {
        val keys = seriesIdentityKeys(candidate)
            .filter { it.length >= 3 }
            .distinct()
        if (targetKey in keys) return true
        if (keys.any { candidateKey ->
                val shorter = minOf(targetKey.length, candidateKey.length)
                shorter >= 7 && (targetKey.contains(candidateKey) || candidateKey.contains(targetKey))
            }) return true

        val targetTokens = targetKey.split(' ').filter { it.length > 1 }.toSet()
        return keys.any { key ->
            val candidateTokens = key.split(' ').filter { it.length > 1 }.toSet()
            val shared = targetTokens.intersect(candidateTokens).size
            shared >= 2 && shared.toDouble() / minOf(targetTokens.size, candidateTokens.size).coerceAtLeast(1) >= .8
        }
    }

    /**
     * Series seasons may be released in different years, and catalog cards often
     * append the season's release year to the title. Strip that year before matching
     * the work so an exact title match is not discarded as a different series.
     */
    private fun seriesIdentityKey(value: String): String =
        SeriesCatalogParser.normalizedSeriesTitle(
            value.replace(Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)"""), " "),
        )

    private fun seriesIdentityKeys(item: ArabseedItem): List<String> =
        listOfNotNull(item.parentSeriesTitle, item.title, item.arabicTitle)
            .map(::seriesIdentityKey)
            .filter { it.length >= 3 }
            .distinct()

    private fun selectSeriesPages(items: List<ArabseedItem>): List<ArabseedItem> = buildList {
        items.groupBy { getProviderForUrl(it.pageUrl)?.id ?: it.providerName }.forEach { (_, sourceItems) ->
            val canonical = sourceItems.filter {
                SeriesCatalogParser.seasonNumber(it.title, it.pageUrl) == null &&
                    SeriesCatalogParser.partNumber(it.title, it.pageUrl) == null
            }
            canonical.maxByOrNull(::catalogMetadataScore)?.let(::add)
            sourceItems
                .filter { SeriesCatalogParser.seasonNumber(it.title, it.pageUrl) != null }
                .groupBy { SeriesCatalogParser.seasonNumber(it.title, it.pageUrl) }
                .values
                .mapNotNull { rows -> rows.maxByOrNull(::catalogMetadataScore) }
                .forEach(::add)
        }
    }

    private fun mergeProviderSeasonBundles(
        bundles: List<Pair<ArabseedItem, ArabseedSeasonBundle>>,
    ): ArabseedSeasonBundle {
        val seasons = linkedMapOf<Int, MutableMap<String, Any?>>()
        for ((sourceItem, bundle) in bundles) {
            val sourceName = sourceItem.providerName.ifBlank {
                getProviderForUrl(sourceItem.pageUrl)?.name.orEmpty()
            }
            for (season in bundle.seasons) {
                if (season.number <= 0) continue
                val target = seasons.getOrPut(season.number) {
                    mutableMapOf(
                        "title" to SeriesCatalogParser.seasonTitle(season.number),
                        "pageUrl" to "",
                        "episodes" to linkedMapOf<Int, ArabseedEpisode>(),
                    )
                }
                if (season.pageUrl.isNotBlank() && (target["pageUrl"] as String).isBlank()) {
                    target["pageUrl"] = season.pageUrl
                }
                @Suppress("UNCHECKED_CAST")
                val episodeMap = target["episodes"] as MutableMap<Int, ArabseedEpisode>
                for (episode in season.episodes) {
                    if (episode.number <= 0 || episode.pageUrl.isBlank()) continue
                    episodeMap.putIfAbsent(
                        episode.number,
                        episode.copy(providerName = episode.providerName.ifBlank { sourceName }),
                    )
                }
            }
        }
        val mergedSeasons = seasons.map { (number, row) ->
            @Suppress("UNCHECKED_CAST")
            val episodes = (row["episodes"] as MutableMap<Int, ArabseedEpisode>)
                .values
                .sortedBy { it.number }
            ArabseedSeason(
                number = number,
                title = row["title"] as String,
                episodes = episodes,
                pageUrl = row["pageUrl"] as String,
            )
        }

        val relatedParts = linkedMapOf<String, ArabseedItem>()
        bundles.flatMap { it.second.relatedParts }.forEach { part ->
            val number = SeriesCatalogParser.partNumber(part.title, part.pageUrl)
            val key = number?.let { "part:$it" } ?: SeriesCatalogParser.normalizedSeriesTitle(part.title)
            if (key.isNotBlank()) relatedParts.putIfAbsent(key, part)
        }
        return SeriesCatalogParser.normalizeSeasonBundle(
            ArabseedSeasonBundle(mergedSeasons, relatedParts.values.toList()),
        )
    }



    // ══════════════════════════════════════════════════════════════════

    // Smart Deduplication

    // ══════════════════════════════════════════════════════════════════

    private fun normalizeTitle(title: String, type: ArabseedType): String {
        var normalized = SeriesCatalogParser.normalizeDigits(title)
            .lowercase()
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")

        if (type == ArabseedType.SERIES) {
            normalized = normalized.replace(
                Regex("""(?i)\s+(?:الموسم|موسم|season|الجزء|جزء|part|الحلقة|حلقة|episode|ep)\s*.*$"""),
                " ",
            )
        }

        return normalized
            .replace(Regex("""(?i)\b(فيلم|مسلسل|مترجم|مدبلج|كامل|مشاهدة|اون لاين|web[- ]?dl|bluray|hdtv|hdrip|hd|fhd|4k|2160p|1080p|720p|480p)\b"""), " ")
            .replace(Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)"""), " ")
            .replace(Regex("""[^\w\s\u0600-\u06FF]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun titleKeys(item: ArabseedItem): Set<String> =
        listOfNotNull(item.parentSeriesTitle, item.title, item.arabicTitle)
            .map { normalizeTitle(it, item.type) }
            .filter { it.length >= 2 }
            .toSet()

    private fun seriesEntryKeys(item: ArabseedItem): Set<String> {
        val titleKeys = titleKeys(item)
        val season = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
            ?: SeriesCatalogParser.seasonNumber(item.arabicTitle, item.pageUrl)
            ?: 1
        val part = SeriesCatalogParser.partNumber(item.title, item.pageUrl)
            ?: SeriesCatalogParser.partNumber(item.arabicTitle, item.pageUrl)
            ?: 1
        val episode = SeriesCatalogParser.episodeNumber(item.title, item.pageUrl)
            ?: SeriesCatalogParser.episodeNumber(item.arabicTitle, item.pageUrl)
        val identity = buildString {
            append("part=$part|season=$season")
            episode?.let { append("|episode=$it") }
        }
        return titleKeys.mapTo(linkedSetOf()) { "$it|$identity" }
    }

    private fun seriesBaseTitle(item: ArabseedItem): String {
        val raw = item.parentSeriesTitle?.takeIf(String::isNotBlank)
            ?: item.arabicTitle.takeIf(String::isNotBlank)
            ?: item.title
        return SeriesCatalogParser.cleanSeriesTitle(raw)
            .replace(Regex("""(?i)\b(?:مسلسل|series|مترجم|مدبلج|WEB[- ]?DL|1080p|720p|480p|HD)\b"""), " ")
            .replace(Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', '|', ':', '·')
            .ifBlank { item.title }
    }

    private fun collapseSeriesEntries(items: List<ArabseedItem>): List<ArabseedItem> {
        val parent = IntArray(items.size) { it }
        fun find(index: Int): Int {
            var root = index
            while (parent[root] != root) root = parent[root]
            var current = index
            while (parent[current] != current) {
                val next = parent[current]
                parent[current] = root
                current = next
            }
            return root
        }
        fun union(first: Int, second: Int) {
            val a = find(first)
            val b = find(second)
            if (a != b) parent[maxOf(a, b)] = minOf(a, b)
        }

        // Compare a series only with entries sharing a normalized title key.
        // This avoids scanning every previous item on each batch merge.
        val titleBuckets = mutableMapOf<String, MutableList<Int>>()
        items.forEachIndexed { index, item ->
            if (item.type != ArabseedType.SERIES) return@forEachIndexed
            val keys = titleKeys(item)
            val year = releaseYear(item)
            val candidates = keys.asSequence().flatMap { titleBuckets[it].orEmpty().asSequence() }.toSet()
            val knownYears = candidates.asSequence().map { releaseYear(items[it]) }
                .plus(year).filter { it.isNotBlank() }.toSet()
            candidates.forEach { candidateIndex ->
                val candidateYear = releaseYear(items[candidateIndex])
                val sameKnownYear = candidateYear.isNotBlank() && candidateYear == year
                val safelyUnknownYear = knownYears.size <= 1 && (candidateYear.isBlank() || year.isBlank())
                if (sameKnownYear || safelyUnknownYear) {
                    union(index, candidateIndex)
                }
            }
            keys.forEach { key -> titleBuckets.getOrPut(key) { mutableListOf() }.add(index) }
        }

        val groups = items.indices
            .filter { items[it].type == ArabseedType.SERIES }
            .groupBy(::find)
            .values
            .map { indices -> indices.map { it to items[it] } }

        val representatives = groups.associate { entries ->
            val latest = entries.maxWithOrNull(
                compareBy<Pair<Int, ArabseedItem>> {
                    SeriesCatalogParser.seasonNumber(it.second.title, it.second.pageUrl)
                        ?: SeriesCatalogParser.seasonNumber(it.second.arabicTitle, it.second.pageUrl)
                        ?: SeriesCatalogParser.partNumber(it.second.title, it.second.pageUrl)
                        ?: SeriesCatalogParser.partNumber(it.second.arabicTitle, it.second.pageUrl)
                        ?: 1
                }.thenBy {
                    SeriesCatalogParser.partNumber(it.second.title, it.second.pageUrl)
                        ?: SeriesCatalogParser.partNumber(it.second.arabicTitle, it.second.pageUrl)
                        ?: 1
                }.thenBy {
                    SeriesCatalogParser.episodeNumber(it.second.title, it.second.pageUrl)
                        ?: SeriesCatalogParser.episodeNumber(it.second.arabicTitle, it.second.pageUrl)
                        ?: 0
                }.thenBy { -it.first },
            ) ?: entries.first()
            val selected = latest.second
            val baseTitle = seriesBaseTitle(selected)
            val season = SeriesCatalogParser.seasonNumber(selected.title, selected.pageUrl)
                ?: SeriesCatalogParser.seasonNumber(selected.arabicTitle, selected.pageUrl)
            val part = SeriesCatalogParser.partNumber(selected.title, selected.pageUrl)
                ?: SeriesCatalogParser.partNumber(selected.arabicTitle, selected.pageUrl)
            val episode = SeriesCatalogParser.episodeNumber(selected.title, selected.pageUrl)
                ?: SeriesCatalogParser.episodeNumber(selected.arabicTitle, selected.pageUrl)
            val label = when {
                episode != null && season != null -> "الموسم $season · الحلقة $episode"
                episode != null -> "أحدث حلقة · $episode"
                season != null && season > 1 -> "أحدث موسم · $season"
                part != null && part > 1 -> "الجزء $part"
                else -> selected.latestEpisodeLabel
            }
            val sameProviderSeriesPage = entries.asSequence()
                .map { it.second }
                .firstOrNull {
                    it.providerName == selected.providerName
                        && !SeriesCatalogParser.isEpisode(it.title, it.pageUrl)
                        && it.pageUrl != selected.pageUrl
                }

            entries.first().first to selected.copy(
                title = baseTitle,
                arabicTitle = baseTitle,
                parentSeriesTitle = selected.parentSeriesTitle?.takeIf(String::isNotBlank) ?: baseTitle,
                parentSeriesPageUrl = selected.parentSeriesPageUrl
                    ?: sameProviderSeriesPage?.pageUrl,
                latestEpisodeLabel = label,
            )
        }.toMap()

        return buildList {
            items.forEachIndexed { index, item ->
                if (item.type != ArabseedType.SERIES) add(item)
                else representatives[index]?.let(::add)
            }
        }
    }

    private fun releaseYear(item: ArabseedItem): String =
        listOf(item.year, item.title, item.arabicTitle)
            .firstNotNullOfOrNull { Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)""").find(it)?.value }
            .orEmpty()

    private fun hasArabic(value: String): Boolean = value.any { it in '\u0600'..'\u06FF' }

    private fun hasUsablePoster(item: ArabseedItem): Boolean =
        item.posterUrl?.startsWith("http", ignoreCase = true) == true

    private fun catalogMetadataScore(item: ArabseedItem): Int {
        var score = 0
        if (hasUsablePoster(item)) score += 5
        if (releaseYear(item).isNotBlank()) score += 2
        if (item.quality.isNotBlank() && !item.quality.equals("HD", ignoreCase = true)) score += 1
        if (item.rating.isNotBlank()) score += 1
        if (item.overview.isNotBlank() && PROVIDERS.none { it.name == item.overview }) score += 1
        return score
    }

    private fun mergeDuplicate(existing: ArabseedItem, incoming: ArabseedItem): ArabseedItem {
        val preferred = if (catalogMetadataScore(incoming) > catalogMetadataScore(existing)) incoming else existing
        val other = if (preferred === existing) incoming else existing
        val arabicTitle = listOf(preferred.arabicTitle, other.arabicTitle, preferred.title, other.title)
            .firstOrNull { it.isNotBlank() && hasArabic(it) }
            ?: preferred.arabicTitle.ifBlank { other.arabicTitle }
        val overview = listOf(preferred.overview, other.overview)
            .firstOrNull { text -> text.isNotBlank() && PROVIDERS.none { it.name == text } }
            .orEmpty()

        return preferred.copy(
            title = listOf(preferred.title, other.title).firstOrNull { it.isNotBlank() && hasArabic(it) }
                ?: preferred.title.ifBlank { other.title },
            arabicTitle = arabicTitle,
            posterUrl = preferred.posterUrl?.takeIf { it.startsWith("http", ignoreCase = true) }
                ?: other.posterUrl?.takeIf { it.startsWith("http", ignoreCase = true) },
            year = preferred.year.ifBlank { other.year },
            quality = preferred.quality.takeIf { it.isNotBlank() && !it.equals("HD", ignoreCase = true) }
                ?: other.quality.ifBlank { preferred.quality },
            rating = preferred.rating.ifBlank { other.rating },
            overview = overview,
        )
    }

    private fun deduplicateItems(
        items: List<ArabseedItem>,
        expectedType: ArabseedType? = null,
    ): List<ArabseedItem> {
        val unique = mutableListOf<ArabseedItem>()
        val exactUrlIndex = mutableMapOf<Pair<ArabseedType, String>, Int>()
        val titleIndex = mutableMapOf<Pair<ArabseedType, String>, MutableSet<Int>>()

        fun indexItem(index: Int, item: ArabseedItem) {
            val normalizedUrl = SeriesCatalogParser.normalizedUrl(item.pageUrl)
            exactUrlIndex.putIfAbsent(item.type to normalizedUrl, index)
            titleKeys(item).forEach { key ->
                titleIndex.getOrPut(item.type to key) { linkedSetOf() }.add(index)
            }
        }

        for (item in items) {
            if (expectedType != null && item.type != expectedType) continue
            if (item.title.isBlank() || !item.pageUrl.startsWith("http", ignoreCase = true)) continue

            val keys = titleKeys(item)
            if (keys.isEmpty()) continue
            val year = releaseYear(item)
            val exactKey = item.type to SeriesCatalogParser.normalizedUrl(item.pageUrl)
            val candidates = exactUrlIndex[exactKey]?.let { setOf(it) }
                ?: keys.asSequence()
                    .flatMap { titleIndex[item.type to it].orEmpty().asSequence() }
                    .toSortedSet()
            val duplicateIndex = candidates.firstOrNull { index ->
                val current = unique[index]
                if (current.type != item.type) return@firstOrNull false
                if (SeriesCatalogParser.normalizedUrl(current.pageUrl) == exactKey.second) return@firstOrNull true
                val currentYear = releaseYear(current)
                val yearsCompatible = currentYear.isBlank() || year.isBlank() || currentYear == year
                val sameWorkTitle = titleKeys(current).any(keys::contains)
                if (!yearsCompatible || !sameWorkTitle) return@firstOrNull false
                item.type != ArabseedType.SERIES
                    || seriesEntryKeys(current).any(seriesEntryKeys(item)::contains)
            }
            if (duplicateIndex == null) {
                val index = unique.size
                unique += item
                indexItem(index, item)
            } else {
                val merged = mergeDuplicate(unique[duplicateIndex], item)
                unique[duplicateIndex] = merged
                indexItem(duplicateIndex, merged)
                exactUrlIndex.putIfAbsent(exactKey, duplicateIndex)
            }
        }
        return collapseSeriesEntries(unique)
    }
    // Fetch og:image from the source page
    private fun fetchOgImage(pageUrl: String): String? {
        return try {
            val html = fetchHtml(pageUrl)
            if (html.isBlank()) return null
            val doc = Jsoup.parse(html, pageUrl)
            doc.selectFirst("meta[property=og:image], meta[name=og:image]")
                ?.attr("content")
                ?.takeIf { it.startsWith("http") }
        } catch (_: Exception) { null }
    }

    // Enrich missing posters by fetching og:image from source site
    private fun enrichPostersFromSource(items: List<ArabseedItem>): List<ArabseedItem> {
        val needsPoster = items.filter { it.posterUrl.isNullOrBlank() }.take(12)
        if (needsPoster.isEmpty()) return items

        val batchPool = java.util.concurrent.Executors.newFixedThreadPool(6)
        val futureMap = needsPoster.associate { item ->
            item.pageUrl to batchPool.submit<String?> { fetchOgImage(item.pageUrl) }
        }
        batchPool.shutdown()

        val posterMap = mutableMapOf<String, String>()
        for ((url, future) in futureMap) {
            try {
                val p = future.get(1, TimeUnit.SECONDS)
                if (p != null) posterMap[url] = p
            } catch (_: Exception) {}
        }

        return items.map { item ->
            if (item.posterUrl.isNullOrBlank() && posterMap.containsKey(item.pageUrl))
                item.copy(posterUrl = posterMap[item.pageUrl])
            else item
        }
    }

}
