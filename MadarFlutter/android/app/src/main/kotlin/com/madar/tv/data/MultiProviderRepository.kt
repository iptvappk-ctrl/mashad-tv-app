package com.madar.tv.data

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit



/**

 * MultiProviderRepository — provider-backed catalog, season, and playback metadata aggregation.

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
    private val pool = Executors.newFixedThreadPool(8)
    private val providerPool = Executors.newFixedThreadPool(24)
    private val backupServerPool = Executors.newFixedThreadPool(12)
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

    private val providerPageHttp = http.newBuilder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .build()

    // Season pagination runs after the detail screen is already visible. Give these
    // enrichment requests a realistic window without slowing catalog/detail first paint.
    private val seasonHttp = http.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()



    // ── Caches ─────────────────────────────────────────────────────────

    private val listCache   = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()

    private val searchCache = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()

    private val detailCache = ConcurrentHashMap<String, ArabseedDetail>()
    private val detailCacheAt = ConcurrentHashMap<String, Long>()
    // A dead/parked mirror should not be retried for every category, title, and
    // season request. Transient network failures cool down briefly; redirects
    // outside the configured provider and interstitial pages cool down longer.
    private val providerRouteCooldownUntil = ConcurrentHashMap<String, Long>()
    private const val PROVIDER_TRANSIENT_COOLDOWN_MS = 30 * 1000L
    private const val PROVIDER_BAD_ROUTE_COOLDOWN_MS = 10 * 60 * 1000L
    private data class ServerFallbackSnapshot(
        val savedAt: Long,
        val servers: List<ArabseedServer>,
        val complete: Boolean,
    )
    private val serverFallbackCache = ConcurrentHashMap<String, ServerFallbackSnapshot>()
    private data class StreamProbe(val checkedAt: Long, val playable: Boolean)
    private val streamProbeCache = ConcurrentHashMap<String, StreamProbe>()
    private const val STREAM_PROBE_TTL_MS = 20_000L
    private const val SERVER_FALLBACK_TTL = 10 * 60 * 1000L
    private const val SERVER_FALLBACK_PARTIAL_TTL = 45 * 1000L
    private const val SERVER_FALLBACK_EMPTY_TTL = 90 * 1000L
    private val seasonCacheAt = ConcurrentHashMap<String, Long>()
    private const val SEASON_TTL = 30 * 60 * 1000L
    private const val SEASON_ENRICHMENT_RETRY_MS = 5 * 60 * 1000L
    private const val SEASON_ENRICHMENT_PARTIAL_RETRY_MS = 30 * 1000L
    private const val SEASON_ENRICHMENT_ATTEMPT_TTL_MS = 24 * 60 * 60 * 1000L
    private val seasonMirrorPool = Executors.newFixedThreadPool(6)
    private val seasonAggregationPool = Executors.newSingleThreadExecutor()
    private val seasonAggregationInFlight = ConcurrentHashMap.newKeySet<String>()


    private val seasonCache = ConcurrentHashMap<String, ArabseedSeasonBundle>()



    private fun catalogCacheKey(type: ArabseedType?, page: Int): String = when (type) {
        // The v2 namespace deliberately invalidates snapshots created while
        // one configured provider was disabled or category pages were primary-only.
        ArabseedType.MOVIE -> "movies:v5:$page"
        ArabseedType.SERIES -> "series:v5:$page"
        null -> "catalog:v5:$page"
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
        val key = "search:v7:" + query.trim().lowercase()
        if (query.isBlank()) return null
        searchCache[key]?.let { cached ->
            if (System.currentTimeMillis() - cached.first <= 14L * 24 * 60 * 60 * 1000) return cached.second
        }
        val snapshot = CatalogDiskCache.loadItems("providers:" + key) ?: return null
        searchCache[key] = snapshot.savedAt to snapshot.value
        return snapshot.value
    }

    fun loadCachedDetail(pageUrl: String): ArabseedDetail? {
        detailCache[pageUrl]?.let { if (it.servers.isNotEmpty()) return it }
        val snapshot = CatalogDiskCache.loadDetail("providers:detail:v3:" + pageUrl) ?: return null
        if (snapshot.value.servers.isEmpty()) return null
        detailCache[pageUrl] = snapshot.value
        detailCacheAt[pageUrl] = snapshot.savedAt
        return snapshot.value
    }

    fun getCachedDetail(pageUrl: String): ArabseedDetail? = detailCache[pageUrl]

    fun loadCachedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        seasonCache[pageUrl]?.let { return it }
        val snapshot = CatalogDiskCache.loadSeasonBundle("providers:seasons:v24:" + pageUrl) ?: return null
        val normalized = SeriesCatalogParser.normalizeSeasonBundle(snapshot.value)
        seasonCache[pageUrl] = normalized
        seasonCacheAt[pageUrl] = snapshot.savedAt
        return normalized
    }

    fun loadCachedMergedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        val snapshot = CatalogDiskCache.loadSeasonBundle("combined:seasons:v23:$pageUrl") ?: return null
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
        val targetSeasonNumber = SeriesCatalogParser.seasonNumber(seasonItem.title, seasonItem.pageUrl)
            ?: seasonBundle.seasons.singleOrNull()?.number
        val correctedSeason = seasonBundle.seasons
            .firstOrNull { it.number == targetSeasonNumber && it.episodes.isNotEmpty() }
            ?.let { season ->
                val previousEpisodes = existing.seasons
                    .firstOrNull { it.number == season.number }
                    ?.episodes
                    .orEmpty()
                val episodesByNumber = linkedMapOf<Int, ArabseedEpisode>()
                previousEpisodes
                    .filter { it.number > 0 && it.pageUrl.isNotBlank() }
                    .forEach { episodesByNumber[it.number] = it.copy(seasonNumber = season.number) }
                // Fresh page data wins duplicate numbers, while the union keeps
                // episodes that a partial provider response omitted this time.
                season.episodes
                    .filter { it.number > 0 && it.pageUrl.isNotBlank() }
                    .forEach { episodesByNumber[it.number] = it.copy(seasonNumber = season.number) }
                season.copy(
                    episodes = episodesByNumber.values
                        .sortedBy { it.number },
                )
            }
        val merged = if (targetSeasonNumber != null && correctedSeason != null) {
            SeriesCatalogParser.normalizeSeasonBundle(
                existing.copy(
                    seasons = existing.seasons.filterNot { it.number == targetSeasonNumber } + correctedSeason,
                ),
            )
        } else {
            mergeProviderSeasonBundles(listOf(seasonItem to seasonBundle, seriesItem to existing))
        }
        if (merged.seasons.isNotEmpty() || merged.relatedParts.isNotEmpty()) {
            CatalogDiskCache.saveSeasonBundle("combined:seasons:v23:${seriesItem.pageUrl}", merged)
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
        /** Mirror domains tried in order if baseUrl is unreachable */
        val mirrorDomains: List<String> = emptyList(),
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

    // Remember a domain only after its actual listing route returned parseable data.
    private data class DomainProbe(val baseUrl: String, val checkedAt: Long, val responseCode: Int)
    private val resolvedBaseUrlCache = ConcurrentHashMap<String, DomainProbe>()

    private fun pageOrigin(url: String, fallback: String = ""): String = runCatching {
        val uri = java.net.URI(url.trim())
        if (uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "${uri.scheme}://${uri.host}"
        } else fallback
    }.getOrDefault(fallback)

    private fun hostMatches(url: String, configuredDomain: String): Boolean {
        val actual = runCatching { java.net.URI(url.trim()).host.orEmpty() }
            .getOrDefault("").lowercase().removePrefix("www.")
        val expected = runCatching { java.net.URI(configuredDomain.trim()).host.orEmpty() }
            .getOrDefault("").lowercase().removePrefix("www.")
        return actual.isNotBlank() && expected.isNotBlank() &&
            (actual == expected || actual.endsWith(".$expected"))
    }

    private fun isConfiguredProviderHost(provider: ProviderConfig, url: String): Boolean =
        (listOf(provider.baseUrl) + provider.mirrorDomains).any { hostMatches(url, it) }

    private fun providerRouteKey(provider: ProviderConfig, baseUrl: String): String {
        val host = runCatching { java.net.URI.create(baseUrl).host.orEmpty() }
            .getOrDefault(baseUrl)
            .lowercase(java.util.Locale.ROOT)
        return "${provider.id}|$host"
    }

    private fun isProviderRouteCoolingDown(provider: ProviderConfig, baseUrl: String, now: Long): Boolean {
        val key = providerRouteKey(provider, baseUrl)
        val until = providerRouteCooldownUntil[key] ?: return false
        if (until > now) return true
        providerRouteCooldownUntil.remove(key, until)
        return false
    }

    private fun coolDownProviderRoute(provider: ProviderConfig, baseUrl: String, durationMs: Long) {
        providerRouteCooldownUntil[providerRouteKey(provider, baseUrl)] =
            System.currentTimeMillis() + durationMs
    }

    /** Reject interstitials and unrelated parked/compromised pages before DOM parsing. */
    private fun isProviderInterstitialOrParkedPage(html: String): Boolean {
        val doc = runCatching { Jsoup.parse(html) }.getOrNull() ?: return true
        val title = doc.title().trim().lowercase(java.util.Locale.ROOT)
        val heading = doc.select("h1,h2").take(3).joinToString(" ") { it.text() }
            .lowercase(java.util.Locale.ROOT)
        val visible = "$title $heading"
        if (title.contains("redirecting") || title == "loading..." ||
            title.contains("just a moment") || title.contains("access denied") ||
            title.contains("site not found") || title.contains("domain for sale") ||
            title.contains("this domain is for sale")
        ) return true
        return Regex(
            "(?i)\\b(casino|gambling|jackpot|kaskustoto|slot online|sports betting|1xbet)\\b",
        ).containsMatchIn(visible)
    }

    val PROVIDERS = listOf(

        // ── 1. أكوام (Akwam) ───────────────────────────────────────────
        ProviderConfig(
            id = "akwam",
            name = "أكوام",
            baseUrl = "https://akw.cam",
            mirrorDomains = listOf("https://akwam.cx"),
            moviesPath = "/movies?page={page}",
            seriesPath = "/series?page={page}",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a.br-card-link[href*='/akfilm-']",
                "a.br-card-link[href*='/akseries-']",
                "a[href*='/akfilm-']",
                "a[href*='/akseries-']",
                ".br-card a",
            ),
            titleSelectors = listOf("h3.br-card-title", "h3", "h2", ".title"),
        ),

        // ── 2. وي سيما (WeCima) ────────────────────────────────────────
        // Live-tested 2026-11-14: mywecima.shop → 81 results ✅
        ProviderConfig(
            id = "wecima",
            name = "وي سيما",
            baseUrl = "https://mywecima.shop",
            mirrorDomains = listOf("https://mywecima1.best", "https://wecima.cam"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='watch-']", ".Thumb--GridItem a", ".GridItem a",
                "div.Block--Item a", "article a",
            ),
            titleSelectors = listOf("strong.hasyear", "h3", "h2", ".Title", "[title]"),
        ),

        // ── 3. فاصل إعلاني (FaselHD) ──────────────────────────────────
        ProviderConfig(
            id = "faselhd",
            name = "فاصل إعلاني",
            baseUrl = "https://faselhd.top",
            mirrorDomains = listOf("https://www.fasel-hd.co", "https://faselhd.club"),
            moviesPath = "/movies/?page={page}",
            seriesPath = "/series/?page={page}",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                ".postDiv a", "div.list-movie a", "a.list-title",
                "a[href*='/movie/']", "a[href*='/serie/']", "article a",
            ),
            titleSelectors = listOf(".list-title", ".post__title", "h2", "h3"),
        ),

        // ── 4. إيجي بست (EgyBest) ──────────────────────────────────────
        // Live-tested 2026-11-14: egybest.live → 20 results ✅
        // IMPORTANT: moviesPath uses browse.php NOT /movies/
        ProviderConfig(
            id = "egybest",
            name = "إيجي بست",
            baseUrl = "https://egybest.live",
            mirrorDomains = listOf("https://egybest.rest"),
            moviesPath = "/browse.php?type=movie&page={page}&lang=ar",
            seriesPath = "/browse.php?type=series&page={page}&lang=ar",
            searchPath = "/search/?q={q}",
            cardSelectors = listOf(
                "a.movie-card", ".movies-list-full a.movie",
                ".content-box a", "a.movie",
            ),
            titleSelectors = listOf("h2", "h3", ".title", ".movie-title", "span"),
        ),

        // ── 5. شاهد فور يو (Shahid4U) ──────────────────────────────────
        ProviderConfig(
            id = "shahid4u",
            name = "شاهد فور يو",
            baseUrl = "https://shahid4u.cam",
            mirrorDomains = listOf("https://shahid4u.tv", "https://shahid4u.to"),
            moviesPath = "/category/movies/page/{page}/",
            seriesPath = "/category/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                ".MovieItem a", "article.MovieItem a",
                ".GridItem a", "article a",
                "a[href*='/movie/']", "a[href*='/series/']",
            ),
            titleSelectors = listOf("h3", "h2", ".Title", "strong", "[title]"),
        ),

        // ── 6. سيما فور يو (Cima4U) ────────────────────────────────────
        ProviderConfig(
            id = "cima4u",
            name = "سيما فور يو",
            baseUrl = "https://c4u.cam",
            mirrorDomains = listOf("https://cima4u.watch", "https://cima4u4k.site"),
            moviesPath = "/movies/?page={page}",
            seriesPath = "/series/?page={page}",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                ".MovieBlock a", ".Block--Movie a",
                "article.MovieBlock a", "a[href*='/watch/']",
                ".movies-list a", "article a",
            ),
            titleSelectors = listOf("h3", "h2", ".Title", "strong", "[title]"),
        ),

        // ── 7. فشار (Fushaar) ──────────────────────────────────────────
        ProviderConfig(
            id = "fushaar",
            name = "فشار",
            baseUrl = "https://fushaar.com",
            mirrorDomains = emptyList(),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article.poster a", "article.movie a", "article a", ".poster a",
            ),
            titleSelectors = listOf(".title", "h3", "h2", "[title]"),
        ),

        // ── 8. توب سينما (TopCinema) ───────────────────────────────────
        // Live-tested 2026-11-14: topcinemaa.one → 36 results ✅
        ProviderConfig(
            id = "topcinema",
            name = "توب سينما",
            baseUrl = "https://topcinemaa.one",
            mirrorDomains = listOf("https://topcinemaa.vip", "https://topcinema.cam"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='film-']", ".Thumb--GridItem a", ".GridItem a",
                "article a", "div.Block--Item a",
            ),
            titleSelectors = listOf("strong.hasyear", "h3", "h2", ".Title", "[title]"),
        ),

        // ── 9. عرب ليونز (ArabLionz) ───────────────────────────────────
        ProviderConfig(
            id = "arablionz",
            name = "عرب ليونز",
            baseUrl = "https://arablionz.cam",
            mirrorDomains = emptyList(),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article a", ".MovieBlock a", "h2.entry-title a", "a[href*='/movie/']",
            ),
            titleSelectors = listOf("h2", "h3", ".Title", ".entry-title", "[title]"),
        ),

        // ── 10. لودي نت (Lodynet) ──────────────────────────────────────
        // Live-tested 2026-11-14: 12-44 results ✅ (Indian/Asian content)
        ProviderConfig(
            id = "lodynet",
            name = "لودي نت",
            baseUrl = "https://lodynet.watch",
            mirrorDomains = listOf("https://lodynet.com"),
            moviesPath = "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d9%87%d9%86%d8%af%d9%8a%d8%a9/page/{page}/",
            seriesPath = "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d9%87%d9%86%d8%af%d9%8a%d9%87/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='movie-']", "article.bt a", ".content-box article a",
                "h2.entry-title a", "article a",
            ),
            titleSelectors = listOf("h2", "h3", ".entry-title", ".title"),
        ),

        // ── 11. قصة عشق (Esheeq) ───────────────────────────────────────
        // Live-tested 2026-11-14: 33 Turkish series ✅
        // Uses shot90.com base64 redirect — AdBypassHelper.decodeShot90Url() handles this
        ProviderConfig(
            id = "esheeq",
            name = "قصة عشق",
            baseUrl = "https://qesset.net",
            mirrorDomains = listOf("https://qeseh.net"),
            moviesPath = "/category/yeni-filmler/page/{page}/",
            seriesPath = "/son-bolumler/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article a[href*='shot90']", "article a",
                "div.thumb a", ".video-card a",
            ),
            titleSelectors = listOf("h2", "h3", ".title", ".entry-title"),
        ),

        // ── 12. موفيز لاند (Movizland) ─────────────────────────────────
        ProviderConfig(
            id = "movizland",
            name = "موفيز لاند",
            baseUrl = "https://movizland.life",
            mirrorDomains = emptyList(),
            moviesPath = "/category/movies/page/{page}/",
            seriesPath = "/category/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article.MovieItem a", ".MovieItem a",
                ".Block--Item a", "article a",
                "a[href*='/movie/']", "a[href*='/series/']",
            ),
            titleSelectors = listOf("h3", "h2", ".Title", "strong.hasyear", "[title]"),
        ),

        // ── 13. بريستيج (Prestige) ─────────────────────────────────────
        ProviderConfig(
            id = "prestige",
            name = "بريستيج",
            baseUrl = "https://prstej.asia",
            mirrorDomains = listOf("https://prstej.lat", "https://brstej.com"),
            moviesPath = "/movies",
            seriesPath = "/series",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                ".pgh-card a", ".pgh-series-card a", ".ptoday-card a",
                ".movie__block a", "article a",
                "a[href*='/movie/']", "a[href*='/series/']",
            ),
            titleSelectors = listOf(".pgh-card-title", ".pgh-series-name", "h2", "h3", ".Title"),
        ),

        // ── 14. سيما كلوب (CimaClub) ───────────────────────────────────
        ProviderConfig(
            id = "cimaclub",
            name = "سيما كلوب",
            baseUrl = "https://cimaclub.live",
            mirrorDomains = listOf("https://cimaclub.living"),
            moviesPath = "/category/movies/page/{page}/",
            seriesPath = "/category/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article.MovieItem a", ".MovieItem a",
                "div.GridItem a", "article a",
                "h3.Title a", "a[href*='/watch/']",
            ),
            titleSelectors = listOf("h3.Title", "h3", "h2", ".Title", "[title]"),
        ),

        // The remaining documented sources are first-class providers too.
        // These selectors stay intentionally broad and only read public catalog
        // links; provider-specific playback still uses the page's normal embeds.
        // Live-tested 2026-11-14: egydead.surf → 403; egidead.cam is the working mirror ✅
        ProviderConfig(
            id = "egydead",
            name = "إيجي ديد",
            baseUrl = "https://egidead.cam",
            mirrorDomains = listOf("https://egydead.surf"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "li.movieItem a", "article.post-item a",
                "div.movie-card a", "a.box", "article a",
            ),
            titleSelectors = listOf("h2.title", "span.movie-title", "h2", "h3", "[title]"),
        ),
        // Live-tested 2026-11-14: cimanow.cc is the working domain ✅
        ProviderConfig(
            id = "cimanow",
            name = "سيما ناو",
            baseUrl = "https://cimanow.cc",
            mirrorDomains = listOf("https://cimanow.info", "https://cimanow.net"),
            moviesPath = "/category/%d8%a7%d9%84%d8%a7%d9%81%d9%84%d8%a7%d9%85/page/{page}/",
            seriesPath = "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='film-']", "div.card a", "article a",
                "div.movie-box a", "a[href*='/episode/']",
            ),
            titleSelectors = listOf("h3.card-title", "div.title", "h3", "h2", "[title]"),
        ),
        // Live-tested 2026-11-14: WordPress theme — standard /?s= search ✅
        ProviderConfig(
            id = "cimawbas",
            name = "سيما وبس",
            baseUrl = "https://cimawbas.rest",
            mirrorDomains = listOf("https://cimawebas.com", "https://cima-wbas.net"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='film-']", "article a",
                "li.pm-li-video a", "div.pm-li-category a",
                "a[href*='watch.php?vid=']",
            ),
            titleSelectors = listOf("h3 a", "span.pm-video-title", "h3", "h2", "[title]"),
        ),
        // Live-tested 2026-11-14: DooPlay theme — 211 items ✅
        // moviesPath = /movies/page/{page}/ ; searchPath = /?s={q}
        ProviderConfig(
            id = "cima4up",
            name = "سيما فور أب",
            baseUrl = "https://cima4up-official.cyou",
            mirrorDomains = emptyList(),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article a[href*='/title/']", "article a",
                "a[href*='/title/']", "div.c4u-card a",
            ),
            titleSelectors = listOf("h2.c4u-card-title", "span.title", "h2", "h3", "[title]"),
        ),
        // Live-tested 2026-11-14: WordPress theme ✅
        ProviderConfig(
            id = "movies4u",
            name = "موفيز فور يو",
            baseUrl = "https://movies4u.icu",
            mirrorDomains = emptyList(),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='film-']", "article a",
                "div.movie-card a", "a[href*='/movie/']",
            ),
            titleSelectors = listOf("h2", "span.movie-title", "h3", "[title]"),
        ),
        // Live-tested 2026-11-14: DooPlay theme — 89 items ✅
        // seriesPath = /series/page/{page}/ ; searchPath = /?s={q}
        ProviderConfig(
            id = "moviewatcher",
            name = "موفي واتشر",
            baseUrl = "https://moviewatcher-offcial.cyou",
            mirrorDomains = listOf("https://moviewatcher.is"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article a[href*='/title/']", "article a",
                "div.media-card a", "a[href*='/title/']",
            ),
            titleSelectors = listOf("h3.media-title", "span.title", "h3", "h2", "[title]"),
        ),
        // Live-tested 2026-11-14: spacemov.xyz is the working domain ✅
        ProviderConfig(
            id = "spacemov",
            name = "سبيس موف",
            baseUrl = "https://spacemov.xyz",
            mirrorDomains = listOf("https://spacemov-tv.com"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/tv-series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='film-']",
                "article.item.movies div.poster a",
                "article.item.tvshows div.poster a",
                "article.item a",
            ),
            titleSelectors = listOf("div.data h3 a", "h3", "h2", "[title]"),
        ),
        ProviderConfig(
            id = "moviflex",
            name = "موفي فليكس",
            baseUrl = "https://moviflex.net",
            // The configured mirrors failed DNS resolution in the live route check.
            mirrorDomains = emptyList(),
            moviesPath = "/movie/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf("div.movie-item a.film-poster", "div.flw-item a.film-poster", "a.film-poster"),
            titleSelectors = listOf("h3.film-name a", "h3", "h2", "[title]"),
        ),

        // ── 25. عرب سيد (ArabSeed) ─────────────────────────────────────
        // Live-tested 2026-11-14: myseed.tv — search param is 'word' not 's' ✅
        ProviderConfig(
            id = "arabseed",
            name = "عرب سيد",
            baseUrl = "https://m.myseed.tv",
            mirrorDomains = listOf("https://arabseed.cam", "https://arabseed.ac"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/find/?word={q}",
            cardSelectors = listOf(
                "a[href*='film-']", "a[href*='%d9%81%d9%8a%d9%84%d9%85-']",
                "article a", ".GridItem a",
            ),
            titleSelectors = listOf("h3", "h2", ".title", "img[alt]", "[title]"),
        ),

    )



    fun isMultiProvider(pageUrl: String): Boolean {
        // Check against all known provider base URLs and mirror domains dynamically
        if (PROVIDERS.any { p ->
            (listOf(p.baseUrl) + p.mirrorDomains).any { hostMatches(pageUrl, it) }
        }) return true
        // Legacy keyword fallbacks
        return pageUrl.contains("akw.cam") || pageUrl.contains("akwam")
            || pageUrl.contains("wecima") || pageUrl.contains("myweecima")
            || pageUrl.contains("fasel") || pageUrl.contains("fasel4k") || pageUrl.contains("faselhd")
            || pageUrl.contains("egybest") || pageUrl.contains("lodynet")
            || pageUrl.contains("brstej") || pageUrl.contains("prstej")
            || pageUrl.contains("q-ask") || pageUrl.contains("3sk")
            || pageUrl.contains("c4u.cam") || pageUrl.contains("cima4u")
            || pageUrl.contains("cimaclub")
            || pageUrl.contains("shahid4u")
            || pageUrl.contains("movizland")
            || pageUrl.contains("ivbestt") || pageUrl.contains("ivybestt")
            || pageUrl.contains("mycima") || pageUrl.contains("my-cima")
            || pageUrl.contains("aflam") || pageUrl.contains("cima")
    }

    private val providerContentHostRoots: List<String> by lazy {
        PROVIDERS.flatMap { provider -> listOf(provider.baseUrl) + provider.mirrorDomains }
            .mapNotNull { domain -> runCatching { java.net.URI(domain).host }.getOrNull() } + listOf(
            "myseed.tv", "wecima.casa", "fasel4k.site",
            "wecima.cam", "c4u.cam", "cimaclub.live",
            "shahid4u.to", "shahid4u.cam", "movizland.com",
            "prstej.asia", "aflam.to", "mycima.work",
        )
    }

    fun isProviderContentUrl(candidateUrl: String, referencePageUrl: String): Boolean =
        SeriesCatalogParser.isProviderContentPageUrl(
            candidateUrl = candidateUrl,
            referencePageUrl = referencePageUrl,
            knownHostRoots = providerContentHostRoots,
        )

    /** Unwrap only common share/redirect parameters whose target is a known content host. */
    fun resolveProviderContentPageUrl(candidateUrl: String, referencePageUrl: String): String? {
        val candidate = candidateUrl.trim()
        if (isProviderContentUrl(candidate, referencePageUrl)) return candidate
        val uri = runCatching { java.net.URI(candidate) }.getOrNull() ?: return null
        val redirectKeys = setOf("u", "url", "href", "link", "redirect", "redirect_url", "continue")
        val values = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
            val rawKey = pair.substringBefore('=')
            val key = runCatching {
                java.net.URLDecoder.decode(rawKey, "UTF-8")
            }.getOrDefault(rawKey).lowercase()
            if (key !in redirectKeys) return@mapNotNull null
            val rawValue = pair.substringAfter('=', "")
            var value = rawValue
            repeat(2) {
                value = runCatching { java.net.URLDecoder.decode(value, "UTF-8") }
                    .getOrDefault(value)
            }
            value
        }
        return values.firstOrNull { isProviderContentUrl(it, referencePageUrl) }
    }



    private fun getProviderForUrl(url: String): ProviderConfig? {
        return PROVIDERS.firstOrNull { p ->
            (listOf(p.baseUrl) + p.mirrorDomains).any { hostMatches(url, it) }
        } ?: when {

            url.contains("akw") -> PROVIDERS.firstOrNull { it.id == "akwam" }

            url.contains("wecima") || url.contains("myweecima") || url.contains("mycima") -> PROVIDERS.firstOrNull { it.id == "wecima" }

            url.contains("fasel") -> PROVIDERS.firstOrNull { it.id == "faselhd" }

            url.contains("egybest") || url.contains("ivbestt") -> PROVIDERS.firstOrNull { it.id == "egybest" }

            url.contains("lodynet") -> PROVIDERS.firstOrNull { it.id == "lodynet" }

            url.contains("brstej") || url.contains("prstej") -> PROVIDERS.firstOrNull { it.id == "prestige" }

            url.contains("q-ask") || url.contains("3sk") -> PROVIDERS.firstOrNull { it.id == "esheeq" }

            url.contains("c4u") -> PROVIDERS.firstOrNull { it.id == "cima4u" }

            url.contains("cimaclub") -> PROVIDERS.firstOrNull { it.id == "cimaclub" }

            url.contains("shahid4u") -> PROVIDERS.firstOrNull { it.id == "shahid4u" }

            url.contains("movizland") -> PROVIDERS.firstOrNull { it.id == "movizland" }
            url.contains("fushaar") -> PROVIDERS.firstOrNull { it.id == "fushaar" }
            url.contains("topcinema") -> PROVIDERS.firstOrNull { it.id == "topcinema" }
            url.contains("arablionz") -> PROVIDERS.firstOrNull { it.id == "arablionz" }
            url.contains("egydead") -> PROVIDERS.firstOrNull { it.id == "egydead" }

            url.contains("aflam.to") -> PROVIDERS.firstOrNull { it.id == "filmbalara" }

            url.contains("az-movies") -> PROVIDERS.firstOrNull { it.id == "actioncima" }

            else -> null

        }

    }



    // ══════════════════════════════════════════════════════════════════

    // Fetch HTML helper

    // ══════════════════════════════════════════════════════════════════

    private fun fetchHtml(url: String, referer: String = ""): String =
        fetchHtmlWithClient(url, referer, http)

    private fun fetchSeasonHtml(url: String, referer: String = ""): String =
        fetchHtmlWithClient(url, referer, seasonHttp)

    /** Fetch a provider route and fail over by the route's actual response, not the homepage. */
    private fun fetchProviderListing(
        provider: ProviderConfig,
        path: String,
        type: ArabseedType,
    ): List<ArabseedItem> {
        val now = System.currentTimeMillis()
        val cachedBase = resolvedBaseUrlCache[provider.id]
            ?.takeIf { it.responseCode in 200..399 && now - it.checkedAt < 10 * 60 * 1000L }
            ?.baseUrl
        val bases = (listOfNotNull(cachedBase, provider.baseUrl) + provider.mirrorDomains)
            .distinct()
            .filterNot { isProviderRouteCoolingDown(provider, it, now) }
        for (base in bases) {
            val root = base.trimEnd('/')
            var effectiveBase = root
            var finalUrl = "$root$path"
            val html = fetchHtmlWithClient("$root$path", "$root/", providerPageHttp) { redirectedUrl ->
                finalUrl = redirectedUrl
                effectiveBase = pageOrigin(redirectedUrl, root)
            }
            if (html.isBlank()) {
                coolDownProviderRoute(provider, root, PROVIDER_TRANSIENT_COOLDOWN_MS)
                continue
            }
            if (!isConfiguredProviderHost(provider, finalUrl)) {
                android.util.Log.w(TAG, "Rejected redirected provider page id=${provider.id} host=${pageOrigin(finalUrl)}")
                coolDownProviderRoute(provider, root, PROVIDER_BAD_ROUTE_COOLDOWN_MS)
                continue
            }
            if (isProviderInterstitialOrParkedPage(html)) {
                android.util.Log.w(TAG, "Rejected interstitial provider response id=${provider.id} host=${pageOrigin(finalUrl)}")
                coolDownProviderRoute(provider, root, PROVIDER_BAD_ROUTE_COOLDOWN_MS)
                continue
            }
            val rows = parseItems(html, provider, type, effectiveBase)
            if (rows.isNotEmpty()) {
                providerRouteCooldownUntil.remove(providerRouteKey(provider, root))
                resolvedBaseUrlCache[provider.id] = DomainProbe(effectiveBase, now, 200)
                return rows
            }
        }
        return emptyList()
    }

    private fun fetchHtmlWithClient(
        url: String,
        referer: String,
        client: OkHttpClient,
        onFinalUrl: ((String) -> Unit)? = null,
    ): String {

        return try {

            val req = Request.Builder()

                .url(url)

                .header("User-Agent", UA)

                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

                .header("Accept-Language", "ar,en;q=0.9")

                .apply { if (referer.isNotBlank()) header("Referer", referer) }

                .build()

            val host = runCatching { java.net.URI.create(url).host.orEmpty() }.getOrDefault("invalid-url")
            client.newCall(req).execute().use { r ->
                onFinalUrl?.invoke(r.request.url.toString())
                if (!r.isSuccessful) {
                    android.util.Log.w(TAG, "HTTP ${r.code} for host=$host")
                    ""
                } else {
                    val contentType = r.header("Content-Type").orEmpty().lowercase(java.util.Locale.ROOT)
                    if (contentType.isNotBlank() &&
                        !contentType.contains("text/html") &&
                        !contentType.contains("application/xhtml+xml")
                    ) {
                        android.util.Log.w(TAG, "Unexpected provider content type for host=$host type=$contentType")
                        return@use ""
                    }
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

        pageBaseUrl: String = provider.baseUrl,

    ): List<ArabseedItem> {

        if (html.isBlank()) return emptyList()

        val doc = try { Jsoup.parse(html, pageBaseUrl) } catch (_: Exception) { return emptyList() }

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
                    rawHref.startsWith("/") -> "${pageBaseUrl.trimEnd('/')}$rawHref"
                    rawHref.isNotBlank() -> "${pageBaseUrl.trimEnd('/')}/$rawHref"
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
                        raw.startsWith("/") -> "${pageBaseUrl.trimEnd('/')}$raw"
                        raw.isNotBlank() -> "${pageBaseUrl.trimEnd('/')}/$raw"
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
        val key = catalogCacheKey(type, page)
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
                        val path = catalogPagePath(provider, catalogType, page)
                        // Some legacy source pages expose only a single listing page.
                        // Re-fetching them for every page made "load more" repeat forever.
                        if (path == null) {
                            emptyList()
                        } else {
                            val parsed = fetchProviderListing(provider, path, catalogType)
                            if (parsed.isEmpty()) {
                                android.util.Log.w(TAG, "No catalog cards parsed for provider=${provider.name} type=$catalogType")
                            }
                            parsed
                        }
                    } catch (error: Exception) {
                        android.util.Log.w(
                            TAG,
                            "Catalog fetch failed: provider=${provider.id} type=$catalogType page=$page (${error.javaClass.simpleName})",
                        )
                        emptyList()
                    }
                }
            }
        }

        // The home catalog requests movies and series from each provider. With a bounded pool
        // those twelve requests run in two waves, so allow both waves to finish while partial rows
        // continue reaching the UI immediately.
        val report = collectParallelReport(
            tasks,
            timeoutSeconds = if (type == null) 14L else 9L,
            executor = providerPool,
        ) { batch ->
            if (batch.isNotEmpty()) onPartial?.invoke(deduplicateItems(batch, expectedType = type))
        }
        val deduped = deduplicateItems(report.items, expectedType = type)
        // Keep catalog refresh bounded: AsyncImage loads poster images directly,
        // so opening one page here per missing poster only delays the first screen.
        val enriched = deduped
        if (enriched.isNotEmpty() && report.completedTasks == report.totalTasks) {
            listCache[key] = System.currentTimeMillis() to enriched
            CatalogDiskCache.saveItems("providers:list:$key", enriched)
        } else if (report.completedTasks < report.totalTasks) {
            android.util.Log.w(
                TAG,
                "Catalog incomplete type=${type ?: "all"} page=$page " +
                    "completed=${report.completedTasks}/${report.totalTasks}; partial result not cached",
            )
        }
        return enriched
    }

    private fun catalogPagePath(provider: ProviderConfig, type: ArabseedType, page: Int): String? {
        val template = if (type == ArabseedType.MOVIE) provider.moviesPath else provider.seriesPath
        if (template.isBlank()) return null
        if (provider.id == "esheeq" && type == ArabseedType.SERIES) {
            return if (page <= 1) "/" else "/page/$page/"
        }
        // A single-page listing contributes on page one, then drops out cleanly.
        if (page > 1 && !template.contains("{page}")) return null
        return template.replace("{page}", page.toString())
    }

    /** Clear catalog caches so a manual refresh reaches providers again. */
    fun clearCatalogCache() {
        listCache.clear()
        searchCache.clear()
    }

    private data class CategoryFeed(
        val providerId: String,
        val firstPagePath: String,
        val nextPagePath: String? = null,
    )

    private fun expectedCategoryType(categoryId: String): ArabseedType? = when (categoryId) {
        "all_series", "foreign_series", "turkish_series", "arabic_series",
        "asian_series", "indian_series", "dubbed_series", "anime_series",
        "ramadan", "ramadan_2026", "ramadan_2025", "ramadan_2024", "ramadan_2023" -> ArabseedType.SERIES
        "all_movies", "foreign_movies", "arabic_movies", "turkish_movies",
        "asian_movies", "indian_movies", "animation_movies", "dubbed_movies", "anime_movies" -> ArabseedType.MOVIE
        else -> null
    }

    /**
     * Only fan out to a source when it has a category-specific route. Using a
     * provider's generic series/movie page for a language category was the cause
     * of Arabic titles leaking into foreign-only filters.
     */
    private fun categoryFeeds(categoryId: String): List<CategoryFeed> = when (categoryId) {
        "turkish_series" -> listOf(
            CategoryFeed("wecima", "/category.php?cat=turkish-series", "/category.php?cat=turkish-series&page={page}"),
            CategoryFeed("lodynet", "/category/مشاهدة-مسلسلات-تركية/"),
            CategoryFeed("esheeq", "/", "/page/{page}/"),
            CategoryFeed("egydead", "/category/مسلسلات-تركية/", "/category/مسلسلات-تركية/page/{page}/"),
        )
        "turkish_movies" -> listOf(
            CategoryFeed("wecima", "/category.php?cat=turkish-films", "/category.php?cat=turkish-films&page={page}"),
            CategoryFeed("egybest", "/movies/turkish/?page=1", "/movies/turkish/?page={page}"),
            CategoryFeed("egydead", "/category/افلام-تركية/", "/category/افلام-تركية/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-turki/"),
        )
        "foreign_movies" -> listOf(
            CategoryFeed("akwam", "/category/افلام-اجنبي", "/category/افلام-اجنبي/page/{page}"),
            CategoryFeed("wecima", "/category.php?cat=foreign-films", "/category.php?cat=foreign-films&page={page}"),
            CategoryFeed("egydead", "/category/افلام-اجنبي/", "/category/افلام-اجنبي/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-ajnabi/"),
        )
        "arabic_movies" -> listOf(
            CategoryFeed("wecima", "/category.php?cat=arabic-movies", "/category.php?cat=arabic-movies&page={page}"),
            CategoryFeed("egybest", "/movies/arabic/?page=1", "/movies/arabic/?page={page}"),
            CategoryFeed("egydead", "/category/افلام-عربي/", "/category/افلام-عربي/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-arabi/"),
        )
        "arabic_series" -> listOf(
            CategoryFeed("wecima", "/category.php?cat=arabic-tv-series", "/category.php?cat=arabic-tv-series&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-عربي/", "/category/مسلسلات-عربي/page/{page}/"),
        )
        "foreign_series" -> listOf(
            CategoryFeed("egydead", "/category/مسلسلات-اجنبي/", "/category/مسلسلات-اجنبي/page/{page}/"),
            CategoryFeed("moviewatcher", "/tv-shows/page/{page}/"),
            CategoryFeed("spacemov", "/tv-series/page/{page}/"),
        )
        "asian_series" -> listOf(
            CategoryFeed("egydead", "/category/مسلسلات-اسيوية/", "/category/مسلسلات-اسيوية/page/{page}/"),
        )
        "indian_series" -> listOf(
            CategoryFeed("egydead", "/category/مسلسلات-هندية/", "/category/مسلسلات-هندية/page/{page}/"),
        )
        "dubbed_series" -> listOf(
            CategoryFeed("egydead", "/category/مسلسلات-مدبلجة/", "/category/مسلسلات-مدبلجة/page/{page}/"),
        )
        "anime_series" -> listOf(
            CategoryFeed("egydead", "/category/مسلسلات-انمي/", "/category/مسلسلات-انمي/page/{page}/"),
            CategoryFeed("lodynet", "/category/anime/page/1/", "/category/anime/page/{page}/"),
        )
        "dubbed_movies" -> listOf(
            CategoryFeed("akwam", "/category/افلام-مدبلجة", "/category/افلام-مدبلجة/page/{page}"),
        )
        "asian_movies" -> listOf(
            CategoryFeed("egydead", "/category/افلام-اسيوية/", "/category/افلام-اسيوية/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-asiawiya/"),
        )
        "indian_movies" -> listOf(
            CategoryFeed("egydead", "/category/افلام-هندي/", "/category/افلام-هندي/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-hindiya/"),
        )
        "animation_movies" -> listOf(
            CategoryFeed("wecima", "/category.php?cat=anime-and-cartoons", "/category.php?cat=anime-and-cartoons&page={page}"),
            CategoryFeed("akwam", "/category/انمي", "/category/انمي/page/{page}"),
        )
        "anime_movies" -> listOf(
            CategoryFeed("wecima", "/category.php?cat=anime-and-cartoons", "/category.php?cat=anime-and-cartoons&page={page}"),
            CategoryFeed("akwam", "/category/انمي", "/category/انمي/page/{page}"),
            CategoryFeed("lodynet", "/category/anime/page/1/", "/category/anime/page/{page}/"),
        )
        "ramadan_2026" -> listOf(CategoryFeed("egydead", "/category/مسلسلات-رمضان-2026/", "/category/مسلسلات-رمضان-2026/page/{page}/"))
        "ramadan_2025" -> listOf(CategoryFeed("egydead", "/category/مسلسلات-رمضان-2025/", "/category/مسلسلات-رمضان-2025/page/{page}/"))
        "ramadan_2024" -> listOf(CategoryFeed("egydead", "/category/مسلسلات-رمضان-2024/", "/category/مسلسلات-رمضان-2024/page/{page}/"))
        "ramadan_2023" -> listOf(CategoryFeed("egydead", "/category/مسلسلات-رمضان-2023/", "/category/مسلسلات-رمضان-2023/page/{page}/"))
        else -> emptyList()
    }

    private fun categoryCacheKey(categoryId: String, page: Int): String = "category:v5:$categoryId:$page"

    fun loadCachedCategory(categoryId: String, page: Int = 1): List<ArabseedItem>? {
        val key = categoryCacheKey(categoryId, page)
        listCache[key]?.let { cached ->
            if (System.currentTimeMillis() - cached.first <= 14L * 24 * 60 * 60 * 1000) return cached.second
        }
        val snapshot = CatalogDiskCache.loadItems("providers:list:$key") ?: return null
        val expectedType = expectedCategoryType(categoryId)
        val normalized = deduplicateItems(snapshot.value, expectedType)
        if (normalized.isEmpty()) return null
        listCache[key] = snapshot.savedAt to normalized
        return normalized
    }

    fun fetchCategoryAll(
        categoryId: String,
        page: Int = 1,
        forceRefresh: Boolean = false,
    ): List<ArabseedItem> {
        val key = categoryCacheKey(categoryId, page)
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            listCache[key]?.let { (savedAt, items) ->
                if (now - savedAt < LIST_TTL && items.isNotEmpty()) return items
            }
            CatalogDiskCache.loadItems("providers:list:$key")?.let { snapshot ->
                listCache[key] = snapshot.savedAt to snapshot.value
                if (now - snapshot.savedAt < LIST_TTL) return snapshot.value
            }
        }

        val expectedType = expectedCategoryType(categoryId) ?: return emptyList()
        val configuredProviders = PROVIDERS.filter { it.enabled }.associateBy { it.id }
        val tasks = categoryFeeds(categoryId).mapNotNull { feed ->
            val provider = configuredProviders[feed.providerId] ?: return@mapNotNull null
            val path = if (page <= 1) feed.firstPagePath else feed.nextPagePath ?: return@mapNotNull null
            if (page > 1 && !path.contains("{page}")) return@mapNotNull null
            {
                try {
                    val encodedPath = path.replace("{page}", page.toString())
                    val rows = fetchProviderListing(provider, encodedPath, expectedType)
                    deduplicateItems(rows, expectedType)
                } catch (error: Exception) {
                    android.util.Log.w(TAG, "Category fetch failed: category=$categoryId provider=${provider.id} (${error.javaClass.simpleName})")
                    emptyList()
                }
            }
        }
        val report = collectParallelReport(tasks, timeoutSeconds = 8L, executor = providerPool)
        val result = deduplicateItems(report.items, expectedType)
        if (result.isNotEmpty() && report.completedTasks == report.totalTasks) {
            listCache[key] = System.currentTimeMillis() to result
            CatalogDiskCache.saveItems("providers:list:$key", result)
        }
        return result
    }

    private data class ParallelItemsReport(
        val items: List<ArabseedItem>,
        val completedTasks: Int,
        val totalTasks: Int,
    )

    private fun collectParallel(
        tasks: List<() -> List<ArabseedItem>>,
        timeoutSeconds: Long = 8L,
        executor: ExecutorService = pool,
        onPartial: ((List<ArabseedItem>) -> Unit)? = null,
    ): List<ArabseedItem> = collectParallelReport(
        tasks = tasks,
        timeoutSeconds = timeoutSeconds,
        executor = executor,
        onPartial = onPartial,
    ).items

    private fun collectParallelReport(
        tasks: List<() -> List<ArabseedItem>>,
        timeoutSeconds: Long = 8L,
        executor: ExecutorService = pool,
        onPartial: ((List<ArabseedItem>) -> Unit)? = null,
    ): ParallelItemsReport {
        if (tasks.isEmpty()) return ParallelItemsReport(emptyList(), 0, 0)
        val completion = ExecutorCompletionService<List<ArabseedItem>>(executor)
        val submitted = tasks.map { task -> completion.submit(Callable { task() }) }
        val results = mutableListOf<ArabseedItem>()
        var completedTasks = 0
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
            completedTasks++
            try {
                val batch = finished.get()
                results += batch
                if (batch.isNotEmpty()) onPartial?.invoke(batch)
            } catch (_: Exception) {
                // A failing source does not discard results from the others.
            }
        }
        submitted.filterNot { it.isDone }.forEach { it.cancel(true) }
        return ParallelItemsReport(results, completedTasks, tasks.size)
    }
    fun searchAll(query: String, forceRefresh: Boolean = false): List<ArabseedItem> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val key = "search:v7:${q.lowercase()}"
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
                    val basePath = provider.searchPath.replace("{q}", encodedQuery)
                    fetchProviderListing(provider, basePath, ArabseedType.MOVIE)
                } catch (_: Exception) { emptyList() }
            }
        }

        val report = collectParallelReport(tasks, timeoutSeconds = 12L, executor = providerPool)
        val results = deduplicateItems(report.items)
        if (results.isNotEmpty() && report.completedTasks == report.totalTasks) {
            searchCache[key] = System.currentTimeMillis() to results
            CatalogDiskCache.saveItems("providers:" + key, results)
        } else if (report.completedTasks < report.totalTasks) {
            android.util.Log.w(
                TAG,
                "Search incomplete queryLength=${q.length} " +
                    "completed=${report.completedTasks}/${report.totalTasks}; partial result not cached",
            )
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



    private fun absoluteServerUrl(rawUrl: String, baseUrl: String): String {
        val raw = rawUrl.trim()
        if (raw.isBlank()) return ""
        return runCatching { java.net.URI(baseUrl).resolve(raw).toString() }.getOrDefault(raw)
    }

    /** Accept plain public embed URLs and ordinary iframe markup stored in data attributes. */
    private fun serverUrlFromAttribute(rawValue: String, baseUrl: String): String {
        val decoded = Parser.unescapeEntities(rawValue.trim(), false)
            .replace("\\/", "/")
            .replace("\\\"", "\"")
        if (decoded.isBlank()) return ""
        val direct = absoluteServerUrl(decoded.trim().trim('"', '\''), baseUrl)
        if (isServerCandidateUrl(direct)) return direct

        val markup = Jsoup.parseBodyFragment(decoded, baseUrl)
        val embedded = markup.selectFirst("iframe[src], video[src], source[src], a[href]")
            ?.let { element ->
                element.attr("abs:src").ifBlank {
                    element.attr("abs:href").ifBlank {
                        element.attr("src").ifBlank { element.attr("href") }
                    }
                }
            }
            .orEmpty()
        val candidate = absoluteServerUrl(embedded, baseUrl)
        return candidate.takeIf(::isServerCandidateUrl).orEmpty()
    }

    private fun isAdServerUrl(url: String): Boolean {
        val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return false
        val host = uri.host.orEmpty().lowercase(java.util.Locale.ROOT)
        val adMarkers = listOf(
            "doubleclick", "googlead", "googlesyndication", "popads", "adsterra",
            "propellerads", "monetag", "exoclick", "adcash", "ad-maven", "taboola",
            "outbrain", "adservice", "ad-system", "onclick", "syndication",
        )
        if (adMarkers.any(host::contains)) return true
        return Regex("(?i)(^|/)(ads?|advert|banner|popunder|tracking|analytics)(/|$)")
            .containsMatchIn(uri.path.orEmpty())
    }

    private fun isServerCandidateUrl(url: String): Boolean {
        val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(java.util.Locale.ROOT).orEmpty()
        val path = uri.path.orEmpty()
        if (uri.scheme !in setOf("http", "https") || host.isBlank() || isAdServerUrl(url)) return false
        if (Regex("(?i)\\.(apng|avif|bmp|gif|jpe?g|png|svg|webp)$").containsMatchIn(path)) return false
        if (host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be") return false
        return true
    }

    private fun insideNonPlaybackContainer(element: org.jsoup.nodes.Element): Boolean {
        val markers = (listOf(element) + element.parents()).take(6)
            .flatMap { listOf(it.id(), it.className()) }
            .joinToString(" ")
        val contentNavigation = Regex(
            "(?i)(^|[\\s_-])(" +
                "season[-_ ]?(tabs|list|nav|selector)|episode[-_ ]?(list|grid|tabs|nav|pagination)|" +
                "episodes[-_ ]?(list|grid|nav)|eplist|eps-list|series[-_ ]?(list|grid|navigation)|" +
                "part[-_ ]?(list|tabs)|poster|thumbnail|thumb|trailer|teaser|related|recommend|" +
                "suggest|advert|banner|social)([\\s_-]|$)",
        )
        return contentNavigation
            .containsMatchIn(markers)
    }

    private fun isSeriesNavigationLink(element: org.jsoup.nodes.Element, url: String): Boolean {
        val label = listOf(
            element.text(),
            element.attr("title"),
            element.attr("aria-label"),
            element.attr("data-name"),
        ).filter(String::isNotBlank).joinToString(" ")
        return SeriesCatalogParser.isEpisode(label, "") ||
            SeriesCatalogParser.seasonNumber(label, "") != null ||
            SeriesCatalogParser.partNumber(label, "") != null ||
            insideNonPlaybackContainer(element)
    }

    /** Alternate, public watch routes exposed by some configured providers. */
    private fun providerPlaybackPageUrls(pageUrl: String, provider: ProviderConfig?): List<String> {
        val variants = mutableListOf<String>()
        when (provider?.id) {
            "egydead" -> {
                val parsed = pageUrl.toHttpUrlOrNull()
                if (parsed != null && parsed.queryParameter("watch") != "1") {
                    variants += parsed.newBuilder().setQueryParameter("watch", "1").build().toString()
                }
            }
            "cimanow" -> {
                if (pageUrl.contains("/film/")) variants += pageUrl.replaceFirst("/film/", "/watch/")
            }
        }
        variants += pageUrl
        return variants.distinct()
    }

    fun fetchServers(pageUrl: String): List<ArabseedServer> {

        val servers = mutableListOf<ArabseedServer>()

        val provider = getProviderForUrl(pageUrl)

        val baseUrl = pageOrigin(pageUrl, provider?.baseUrl.orEmpty())



        try {

            // 1. Akwam Servers (watch/.../servers)

            if (pageUrl.contains("akw")) {

                val watchUrl = pageUrl.replace("/akfilm-", "/watch/akfilm-")

                    .replace("/akseries-", "/watch/akseries-")

                    .trimEnd('/') + "/servers"

                val watchReferer = pageOrigin(watchUrl, "https://akw.cam")
                val watchHtml = fetchHtml(watchUrl, "$watchReferer/")

                val wDoc = Jsoup.parse(watchHtml, watchUrl)

                val primaryIframe = wDoc.select("iframe[src]")
                    .firstOrNull { !insideNonPlaybackContainer(it) }
                    ?.attr("abs:src")?.trim().orEmpty()

                if (isServerCandidateUrl(primaryIframe)) {

                    servers += ArabseedServer("سيرفر 1 (فائق السرعة)", primaryIframe)

                }

                for (el in wDoc.select("[data-url], [data-src], a.server, .watchServers a, iframe[src]")) {

                    val rawUrl = el.attr("data-url").ifBlank { el.attr("data-src").ifBlank { el.attr("src").ifBlank { el.attr("href") } } }.trim()
                    val sUrl = absoluteServerUrl(rawUrl, watchUrl)

                    val sName = el.text().trim().replace(Regex("(?i)(أكوام|akwam)"), "").trim()

                    if (isServerCandidateUrl(sUrl) && !isSeriesNavigationLink(el, sUrl) && servers.none { it.embedUrl == sUrl }) {

                        val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""

                        servers += ArabseedServer("سيرفر ${servers.size + 1}$label", sUrl)

                    }

                }

            }



            // 2. WeCima Servers (embed.php?vid=... & downloads.php?vid=...)

            if (pageUrl.contains("wecima") || pageUrl.contains("myweecima")) {

                val vid = Regex("vid=([a-zA-Z0-9]+)").find(pageUrl)?.groupValues?.get(1) ?: ""

                if (vid.isNotBlank()) {

                    val embedUrl = "${baseUrl.trimEnd('/')}/embed.php?vid=$vid"
                    if (isServerCandidateUrl(embedUrl)) {
                        servers += ArabseedServer("سيرفر 1 (فائق السرعة)", embedUrl)
                    }

                    try {

                        val downloadPage = "${baseUrl.trimEnd('/')}/downloads.php?vid=$vid"
                        val dlHtml = fetchHtml(downloadPage, pageUrl)

                        val dlDoc = Jsoup.parse(dlHtml, downloadPage)

                        for (a in dlDoc.select("a[href]")) {

                            val h = a.attr("href").trim()

                            val sName = a.text().trim().replace(Regex("(?i)(وي سيما|wecima)"), "").trim()

                            if (isServerCandidateUrl(h) && (

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

                val fHtml = fetchHtml(pageUrl, "$baseUrl/")

                val fDoc = Jsoup.parse(fHtml, pageUrl)

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

                                    .url(baseUrl.trimEnd('/') + "/ajax/embed")

                                    .post(formBody)

                                    .header("User-Agent", UA)

                                    .header("Referer", pageUrl)

                                    .header("X-Requested-With", "XMLHttpRequest")

                                    .build()

                                val respHtml = http.newCall(postReq).execute().use { it.body?.string() ?: "" }

                                val ifr = Jsoup.parse(respHtml).selectFirst("iframe")?.attr("src")?.trim() ?: ""

                                val absoluteIframe = absoluteServerUrl(ifr, pageUrl)
                                if (isServerCandidateUrl(absoluteIframe)) sName to absoluteIframe else null

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

            for (sourcePage in providerPlaybackPageUrls(pageUrl, provider)) {
                val html = fetchHtml(sourcePage, "$baseUrl/")
                if (html.isBlank()) continue

                // ── AdBypassHelper: Cloudflare guard + 10-pattern bypass ──────
                if (AdBypassHelper.isCloudflareChallenge(html)) {
                    // Page is behind Cloudflare — skip silently (we cannot solve the challenge)
                    continue
                }

                val doc = Jsoup.parse(html, sourcePage)

                // 4a. iframes and explicit video sources.
                for (element in doc.select("iframe[src], video[src], video source[src]")) {
                    val src = element.attr("abs:src").ifBlank { element.attr("src") }.trim()
                    val resolvedSrc = if (src.contains("shot90.com", ignoreCase = true)) {
                        AdBypassHelper.decodeShot90Url(src) ?: src
                    } else src
                    if (isServerCandidateUrl(resolvedSrc) && !isSeriesNavigationLink(element, resolvedSrc) &&
                        servers.none { it.embedUrl == resolvedSrc }
                    ) {
                        servers += ArabseedServer("سيرفر ${servers.size + 1}", resolvedSrc)
                    }
                }

                // 4b. Common server lists used by the configured public providers.
                // Constrain plain anchors to player/server containers so navigation
                // links from the page are never mistaken for playable sources.
                val serverElements = doc.select(
                    "[data-embed], [data-src], [data-link], [data-url], [data-video], " +
                        ".servers a, a.server-btn, .watch-buttons a, .ep-sv a, .episode-sv a, " +
                        ".server-item, .server-block a, li.server a, .tab-pane a[href], " +
                        "#serversList li, #serversList a[href], .list_servers li, .list_servers a[href], " +
                        ".download-servers-list a[href], .donwload-servers-list a[href], #playeroptionsul a[href]",
                )
                for (element in serverElements) {
                    val link = element.attr("data-embed").ifBlank {
                        element.attr("data-src").ifBlank {
                            element.attr("data-url").ifBlank {
                                element.attr("data-link").ifBlank {
                                    element.attr("data-video").ifBlank {
                                        element.attr("abs:href").ifBlank { element.attr("href") }
                                    }
                                }
                            }
                        }
                    }.trim()
                    val absoluteLink = serverUrlFromAttribute(link, sourcePage)
                    if (!isServerCandidateUrl(absoluteLink) || isSeriesNavigationLink(element, absoluteLink) ||
                        servers.any { it.embedUrl == absoluteLink }
                    ) continue
                    val serverName = element.text().trim()
                        .replace(Regex("(?i)(أكوام|فاصل|وي سيما|إيجي بست|لودي نت|مشاهدة|سيرفر|server)"), "")
                        .trim()
                    val label = if (serverName.isNotBlank() && serverName.length < 25) " ($serverName)" else ""
                    servers += ArabseedServer("سيرفر ${servers.size + 1}$label", absoluteLink)
                }

                // 4b-2. DooPlay AJAX player options (#playeroptionsul li[data-post])
                val dooLis = doc.select("#playeroptionsul li[data-post], ul.options li[data-post]")
                if (dooLis.isNotEmpty()) {
                    val ajaxUrl = baseUrl.trimEnd('/') + "/wp-admin/admin-ajax.php"
                    for (li in dooLis.take(6)) {
                        val post = li.attr("data-post").trim()
                        val nume = li.attr("data-nume").trim()
                        val type = li.attr("data-type").trim().ifBlank { "1" }
                        val sName = li.select(".title").text().trim().ifBlank { li.text().trim() }
                        if (post.isNotBlank() && nume.isNotBlank()) {
                            try {
                                val form = FormBody.Builder()
                                    .add("action", "doo_player_ajax")
                                    .add("post", post)
                                    .add("nume", nume)
                                    .add("type", type)
                                    .build()
                                val req = Request.Builder()
                                    .url(ajaxUrl)
                                    .post(form)
                                    .header("User-Agent", UA)
                                    .header("Referer", sourcePage)
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .build()
                                val respStr = http.newCall(req).execute().use { it.body?.string() ?: "" }
                                val ifr = Jsoup.parse(respStr).selectFirst("iframe[src]")?.attr("src")?.trim() ?: ""
                                val absIfr = absoluteServerUrl(ifr, sourcePage)
                                if (isServerCandidateUrl(absIfr) && servers.none { it.embedUrl == absIfr }) {
                                    val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""
                                    servers += ArabseedServer("سيرفر ${servers.size + 1}$label", absIfr)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                // 4c. Public HTML player declarations (do not execute page scripts).
                if (servers.isEmpty()) {
                    val jsPatterns = listOf(
                        Regex("""file\s*:\s*['"]([^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""", RegexOption.IGNORE_CASE),
                        Regex("""source\s*:\s*['"]([^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""", RegexOption.IGNORE_CASE),
                        Regex("""src\s*:\s*['"]([^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""", RegexOption.IGNORE_CASE),
                        Regex("""['"](?:file|url|stream)['"]:\s*['"]([^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE),
                    )
                    for (pattern in jsPatterns) {
                        pattern.findAll(html).forEach { match ->
                            val url = absoluteServerUrl(match.groupValues[1].replace("\\/", "/").trim(), sourcePage)
                            if (isServerCandidateUrl(url) && servers.none { it.embedUrl == url }) {
                                servers += ArabseedServer("سيرفر ${servers.size + 1}", url)
                            }
                        }
                        if (servers.isNotEmpty()) break
                    }
                }
                if (servers.isNotEmpty()) break
            }

            // A provider content page itself is never treated as a video URL.

        } catch (_: Exception) {}



        // External server pages may be valid WebView sources. Provider content
        // pages are not playback URLs; returning them here caused black players.
        if (servers.isEmpty() &&
            isServerCandidateUrl(pageUrl) && getProviderForUrl(pageUrl) == null
        ) {
            servers += ArabseedServer("مشاهدة مباشرة", pageUrl)
        }

        return servers  // سريع — الصفحة الحالية بس

    }

    /**
     * يبحث عن تطابق العمل لدى المصادر المهيأة بالتوازي
     * ويرجع سيرفرات من كل موقع لاقاه فيه
     * بيشتغل في الخلفية — النتيجة بتتضاف كـ backup sources للبلاير
     */
    fun fetchServersAllProviders(
        title: String,
        originalPageUrl: String,
        type: ArabseedType = ArabseedType.MOVIE,
        requestedYear: String = "",
    ): List<ArabseedServer> {
        if (title.isBlank()) return emptyList()
        val normalizedTitle = normalizeProviderMatchTitle(title)
        if (normalizedTitle.isBlank()) return emptyList()
        val searchQueries = listOf(normalizedTitle, title.trim()).distinct()

        val originalProvider = getProviderForUrl(originalPageUrl)
        val sourceKey = originalProvider?.id ?: if (isArabseedPage(originalPageUrl)) {
            "arabseed"
        } else {
            pageOrigin(originalPageUrl)
        }
        val cacheKey = listOf(
            type.name,
            normalizedTitle,
            requestedYear.trim(),
            sourceKey,
        ).joinToString(":")
        val now = System.currentTimeMillis()
        serverFallbackCache[cacheKey]?.let { snapshot ->
            val ttl = when {
                !snapshot.complete -> SERVER_FALLBACK_PARTIAL_TTL
                snapshot.servers.isEmpty() -> SERVER_FALLBACK_EMPTY_TTL
                else -> SERVER_FALLBACK_TTL
            }
            if (now - snapshot.savedAt < ttl) return snapshot.servers
        }

        val allServers = java.util.concurrent.ConcurrentHashMap<String, ArabseedServer>()
        val serverOrder = java.util.concurrent.ConcurrentLinkedQueue<String>()

        val tasks = PROVIDERS.filter { it.enabled && it.id != originalProvider?.id }.map { provider ->
            {
                try {
                    var match: ArabseedItem? = null
                    for (query in searchQueries) {
                        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                        val items = fetchProviderListing(
                            provider,
                            provider.searchPath.replace("{q}", encodedQuery),
                            type,
                        )
                        match = items.asSequence()
                            .filter { it.type == type }
                            .filter { requestedYear.isBlank() || it.year.isBlank() || it.year == requestedYear }
                            .filter { item -> matchesProviderTitle(normalizedTitle, item) }
                            .sortedByDescending { it.year.isNotBlank() && it.year == requestedYear }
                            .firstOrNull()
                        if (match != null) break
                    }
                    if (match != null && match.pageUrl != originalPageUrl) {
                        val providerBase = pageOrigin(match.pageUrl, provider.baseUrl)
                        val pageServers = fetchServersFromPage(
                            match.pageUrl,
                            "$providerBase/",
                            providerPageHttp,
                        )
                        pageServers.forEachIndexed { idx, server ->
                            if (server.embedUrl != originalPageUrl) {
                                val key = server.embedUrl
                                val tagged = server.copy(
                                    name = "${provider.name} - سيرفر ${idx + 1}",
                                    sourcePageUrl = match.pageUrl,
                                    sourceProviderName = provider.name,
                                )
                                if (allServers.putIfAbsent(key, tagged) == null) {
                                    serverOrder.add(key)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // Arabseed has its own mature catalog/parser and is intentionally kept
        // outside PROVIDERS. Include it in cross-provider playback failover so
        // it is not silently omitted when the selected item came from another site.
        val arabseedTask: (() -> Unit)? = if (isArabseedPage(originalPageUrl)) {
            null
        } else {
            {
                try {
                    val match = ArabseedRepository.search(title)
                        .asSequence()
                        .filter { it.type == type }
                        .filter { requestedYear.isBlank() || it.year.isBlank() || it.year == requestedYear }
                        .filter { item -> matchesProviderTitle(normalizedTitle, item) }
                        .sortedByDescending { it.year.isNotBlank() && it.year == requestedYear }
                        .firstOrNull()
                    if (match != null && match.pageUrl != originalPageUrl) {
                        ArabseedRepository.fetchServersFast(match.pageUrl)
                            .filter { isServerCandidateUrl(it.embedUrl) }
                            .forEachIndexed { index, server ->
                                if (server.embedUrl != originalPageUrl) {
                                    val tagged = server.copy(
                                        name = "عرب سيد - سيرفر ${index + 1}",
                                        sourcePageUrl = match.pageUrl,
                                        sourceProviderName = "عرب سيد",
                                    )
                                    if (allServers.putIfAbsent(tagged.embedUrl, tagged) == null) {
                                        serverOrder.add(tagged.embedUrl)
                                    }
                                }
                            }
                    }
                } catch (_: Exception) {}
            }
        }
        val lookupTasks = tasks + listOfNotNull(arabseedTask)

        val completion = ExecutorCompletionService<Unit>(backupServerPool)
        val futures = lookupTasks.map { task -> completion.submit(Callable { task() }) }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(14)
        var completedTasks = 0
        for (ignored in lookupTasks.indices) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) break
            val finished = try {
                completion.poll(remaining, TimeUnit.NANOSECONDS) ?: break
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            completedTasks++
            try { finished.get() } catch (_: Exception) {}
        }
        futures.filterNot { it.isDone }.forEach { it.cancel(true) }

        val ordered = serverOrder.mapNotNull { key -> allServers[key] }
        val complete = completedTasks == lookupTasks.size
        // Cache even empty partial sweeps briefly. Without this, tapping play
        // repeatedly on a weak connection re-queries every source and delays
        // each retry without adding new candidates.
        serverFallbackCache[cacheKey] = ServerFallbackSnapshot(
            savedAt = System.currentTimeMillis(),
            servers = ordered,
            complete = complete,
        )
        return ordered
    }

    private fun normalizeProviderMatchTitle(title: String): String = normalizeArabicTitle(title)
        .replace(Regex("\\b(19|20)\\d{2}\\b"), " ")
        .replace(Regex("\\b(فيلم|مسلسل|مترجم|مدبلج|اونلاين|online|movie|series)\\b"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun matchesProviderTitle(targetKey: String, candidate: ArabseedItem): Boolean {
        if (targetKey.isBlank()) return false
        val candidateKeys = listOfNotNull(
            candidate.parentSeriesTitle,
            candidate.title,
            candidate.arabicTitle,
        ).map(::normalizeProviderMatchTitle).filter { it.length >= 3 }.distinct()
        if (targetKey in candidateKeys) return true

        val targetTokens = targetKey.split(' ').filter { it.length > 1 }.toSet()
        if (targetTokens.size < 2) return false
        return candidateKeys.any { key ->
            val candidateTokens = key.split(' ').filter { it.length > 1 }.toSet()
            val shared = targetTokens.intersect(candidateTokens).size
            val smaller = minOf(targetTokens.size, candidateTokens.size)
            val larger = maxOf(targetTokens.size, candidateTokens.size)
            shared >= 2 && shared.toDouble() / smaller.coerceAtLeast(1) >= .8 &&
                shared.toDouble() / larger.coerceAtLeast(1) >= .6
        }
    }

    private fun isArabseedPage(url: String): Boolean =
        hostMatches(url, "https://myseed.tv") ||
            hostMatches(url, "https://arabseed.tv") ||
            hostMatches(url, "https://arabseed.show")

    /** جلب السيرفرات مباشرة من صفحة HTML مع متابعة صفحة المشاهدة إن وُجدت */
    private fun fetchServersFromPage(
        pageUrl: String,
        referer: String,
        client: OkHttpClient = http,
    ): List<ArabseedServer> {
        val result = mutableListOf<ArabseedServer>()
        try {
            val html = fetchHtmlWithClient(pageUrl, referer, client)
            if (html.isBlank()) return result
            val doc = Jsoup.parse(html, pageUrl)
            for (ifr in doc.select("iframe[src]")) {
                val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()
                if (isServerCandidateUrl(src) && result.none { it.embedUrl == src }) {
                    result += ArabseedServer("سيرفر ", src)
                }
            }
            for (el in doc.select("[data-embed],[data-src],[data-url],[data-video],.servers a,a.server-btn")) {
                val link = el.attr("data-embed").ifBlank { el.attr("data-src").ifBlank { el.attr("data-url").ifBlank { el.attr("data-video").ifBlank { el.attr("href") } } } }.trim()
                val abs = serverUrlFromAttribute(link, pageUrl)
                if (isServerCandidateUrl(abs) && result.none { it.embedUrl == abs }) {
                    result += ArabseedServer("سيرفر ", abs)
                }
            }
            // متابعة صفحة المشاهدة الفرعية إن لم نجد سيرفرات في الصفحة الرئيسية
            if (result.isEmpty()) {
                val watchLink = doc.selectFirst("a[href*='/watch/'], a[href*='/servers/'], a[href*='/view/'], a.btn-watch, a[href*='watch']")?.attr("abs:href")?.trim().orEmpty()
                if (watchLink.isNotBlank() && watchLink != pageUrl && isServerCandidateUrl(watchLink)) {
                    val watchHtml = fetchHtmlWithClient(watchLink, pageUrl, client)
                    if (watchHtml.isNotBlank()) {
                        val wDoc = Jsoup.parse(watchHtml, watchLink)
                        for (ifr in wDoc.select("iframe[src]")) {
                            val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()
                            if (isServerCandidateUrl(src) && result.none { it.embedUrl == src }) {
                                result += ArabseedServer("سيرفر ", src)
                            }
                        }
                        for (el in wDoc.select("[data-embed],[data-src],[data-url],[data-video],.servers a,a.server-btn")) {
                            val link = el.attr("data-embed").ifBlank { el.attr("data-src").ifBlank { el.attr("data-url").ifBlank { el.attr("data-video").ifBlank { el.attr("href") } } } }.trim()
                            val abs = serverUrlFromAttribute(link, watchLink)
                            if (isServerCandidateUrl(abs) && result.none { it.embedUrl == abs }) {
                                result += ArabseedServer("سيرفر ", abs)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result.take(10)
    }

    /** استخراج عنوان الفيلم من الـ URL */
    private fun extractTitleFromUrl(pageUrl: String): String {
        return try {
            val path = java.net.URI.create(pageUrl).path.orEmpty()
            val slug = path.trimEnd('/').substringAfterLast('/')
            // إزالة prefixes زي "movie-", "film-", "watch-" ثم أرقام في الآخر
            slug.replace(Regex("^(?:movie|film|watch|series|مسلسل|فيلم)-?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("-\\d{4,}$"), "")
                .replace("-", " ")
                .trim()
        } catch (_: Exception) { "" }
    }

    /** تطبيع العنوان العربي للمقارنة */
    private fun normalizeArabicTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[أإآا]"), "ا")
            .replace(Regex("[ةه]"), "ه")
            .replace(Regex("[يى]"), "ي")
            .replace(Regex("[^ا-يa-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun fetchDetail(item: ArabseedItem): ArabseedDetail {
        val now = System.currentTimeMillis()
        // Return cached detail only if it has a working pre-resolved stream
        detailCache[item.pageUrl]?.let { cached ->
            if (cached.servers.isNotEmpty() && !cached.preResolvedStream.isNullOrBlank()
                && now - (detailCacheAt[item.pageUrl] ?: now) < DETAIL_TTL) return cached
        }

        var pageMetadata: Pair<String, List<String>> = "" to emptyList()
        var servers = emptyList<ArabseedServer>()
        var preResolved: String? = null

        try { pageMetadata = fetchStoryAndCast(item.pageUrl) } catch (_: Exception) {}

        try { servers = fetchServers(item.pageUrl) } catch (_: Exception) {}

        // ── Smart pre-resolution: test servers in parallel, pick first working ──
        if (servers.isNotEmpty()) {
            preResolved = resolveFirstWorkingStream(servers, item.pageUrl)
        }

        val detail = ArabseedDetail(
            item = item.copy(overview = pageMetadata.first.ifBlank { item.overview }),
            servers = servers,
            preResolvedStream = preResolved,
            cast = pageMetadata.second,
        )

        if (servers.isNotEmpty()) {
            detailCache[item.pageUrl] = detail
            detailCacheAt[item.pageUrl] = System.currentTimeMillis()
            CatalogDiskCache.saveDetail("providers:detail:v3:" + item.pageUrl, detail)
        }

        return detail
    }

    /**
     * Tests all embed servers in parallel (up to 8 at once).
     * Returns the first direct-playable URL (.mp4 / .m3u8) found, or null.
     * Timeout per server: 10 seconds.
     */
    private fun resolveFirstWorkingStream(
        servers: List<ArabseedServer>,
        pageReferer: String,
    ): String? {
        if (servers.isEmpty()) return null

        // Already have a direct stream on a server? Return it immediately.
        servers.firstOrNull { s ->
            !s.directStreamUrl.isNullOrBlank() &&
                (s.directStreamUrl.contains(".mp4") || s.directStreamUrl.contains(".m3u8"))
        }?.directStreamUrl?.let { return it }

        val ecs = ExecutorCompletionService<String?>(pool)
        val futures = servers.take(8).map { server ->
            ecs.submit(Callable<String?> {
                try {
                    resolveEmbedToStream(server.embedUrl, pageReferer)
                } catch (_: Exception) { null }
            })
        }

        var found: String? = null
        repeat(futures.size) {
            try {
                val result = ecs.poll(10, TimeUnit.SECONDS)
                val stream = result?.get()
                if (!stream.isNullOrBlank() && found == null) {
                    found = stream
                }
            } catch (_: Exception) {}
        }
        // Cancel remaining
        futures.forEach { it.cancel(true) }
        return found
    }

    /**
     * Resolves a single embed URL to a direct .mp4 / .m3u8 stream.
     * Handles: direct .mp4/.m3u8, iframes, JWPlayer, Vidmoly, Voe, Doodstream,
     *          Streamtape, Vidsharing, Vidara, Uqload, Govid, Mixdrop, etc.
     */
    /** Public entry-point used by FlutterMediaBridge for the "media.resolve" channel. */
    fun resolveStream(embedUrl: String, referer: String): String? =
        resolveEmbedToStream(embedUrl, referer)
            ?.takeIf { isVerifiedPlayableStream(it, referer) }

    /**
     * Checks that an extracted URL responds as video/HLS rather than an error,
     * redirect-to-ad page, or HTML landing page. Results are deliberately kept
     * only briefly because many providers issue expiring stream URLs.
     */
    fun isVerifiedPlayableStream(streamUrl: String, referer: String): Boolean {
        val parsed = streamUrl.toHttpUrlOrNull() ?: return false
        if (parsed.scheme !in setOf("http", "https") || isAdServerUrl(streamUrl)) return false
        val cacheKey = "$streamUrl\n$referer"
        val now = System.currentTimeMillis()
        streamProbeCache[cacheKey]?.let { cached ->
            if (now - cached.checkedAt < STREAM_PROBE_TTL_MS) return cached.playable
        }

        val request = Request.Builder()
            .url(parsed)
            .get()
            .header("User-Agent", UA)
            .header("Accept", "video/*,application/vnd.apple.mpegurl,application/x-mpegURL,*/*;q=0.8")
            .header("Range", "bytes=0-2047")
            .apply { if (referer.isNotBlank()) header("Referer", referer) }
            .build()
        val playable = try {
            http.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (!response.isSuccessful || isAdServerUrl(finalUrl)) return@use false
                val body = response.body ?: return@use false
                val contentType = response.header("Content-Type")
                    .orEmpty()
                    .substringBefore(';')
                    .trim()
                    .lowercase(java.util.Locale.ROOT)
                val bytes = ByteArray(2048)
                val count = body.byteStream().use { input -> input.read(bytes).coerceAtLeast(0) }
                val sample = String(bytes, 0, count, Charsets.UTF_8)
                    .trimStart('\uFEFF', '\u0000', ' ', '\r', '\n', '\t')
                val lowerSample = sample.lowercase(java.util.Locale.ROOT)
                val isHtml = contentType in setOf("text/html", "application/xhtml+xml") ||
                    lowerSample.startsWith("<!doctype html") ||
                    lowerSample.startsWith("<html") ||
                    lowerSample.startsWith("<script") ||
                    lowerSample.startsWith("<body")
                val sampleBytes = bytes.copyOf(count)
                val printableByteCount = sampleBytes.count { byte ->
                    val value = byte.toInt() and 0xFF
                    value in 0x20..0x7E || value in setOf(0x09, 0x0A, 0x0D)
                }
                val looksLikeText = count > 0 && printableByteCount.toDouble() / count >= 0.85
                val isTextErrorPage = looksLikeText && listOf(
                    "404 not found", "page not found", "access denied", "request blocked",
                    "forbidden", "captcha", "enable javascript", "cloudflare",
                ).any { marker -> lowerSample.contains(marker) }
                val isHlsPlaylist = sample.startsWith("#EXTM3U") && listOf(
                    "#EXTINF", "#EXT-X-STREAM-INF", "#EXT-X-TARGETDURATION",
                    "#EXT-X-MEDIA-SEQUENCE", "#EXT-X-I-FRAME-STREAM-INF",
                ).any { marker -> sample.contains(marker) }
                val mediaPath = response.request.url.encodedPath.lowercase(java.util.Locale.ROOT)
                val isDashManifest =
                    (contentType == "application/dash+xml" || mediaPath.endsWith(".mpd")) &&
                        Regex("<\\s*MPD\\b", RegexOption.IGNORE_CASE).containsMatchIn(sample)
                val isIsoBmff = sampleBytes.size >= 8 &&
                    String(sampleBytes, 4, 4, Charsets.US_ASCII) in setOf("ftyp", "styp", "moof")
                val isWebmOrMatroska = sampleBytes.size >= 4 &&
                    sampleBytes[0] == 0x1A.toByte() && sampleBytes[1] == 0x45.toByte() &&
                    sampleBytes[2] == 0xDF.toByte() && sampleBytes[3] == 0xA3.toByte()
                val isTransportStream = sampleBytes.size >= 377 &&
                    sampleBytes[0] == 0x47.toByte() && sampleBytes[188] == 0x47.toByte() &&
                    sampleBytes[376] == 0x47.toByte()
                val isRealMedia = sampleBytes.size >= 4 &&
                    String(sampleBytes, 0, 4, Charsets.US_ASCII) == ".RMF"
                val hasRecognizedMediaPayload = isIsoBmff || isWebmOrMatroska ||
                    isTransportStream || isRealMedia
                val videoMime = contentType.startsWith("video/") ||
                    contentType in setOf("application/mp4", "application/vnd.rn-realmedia")
                !isHtml && !isTextErrorPage && count > 0 && (
                    isHlsPlaylist || isDashManifest || hasRecognizedMediaPayload ||
                        (videoMime && sampleBytes.any { byte ->
                            val value = byte.toInt() and 0xFF
                            value == 0 || value < 0x09 || value in 0x0E..0x1F
                        })
                    )
            }
        } catch (error: Exception) {
            android.util.Log.i(TAG, "Stream probe failed host=${parsed.host} (${error.javaClass.simpleName})")
            false
        }
        streamProbeCache[cacheKey] = StreamProbe(now, playable)
        return playable
    }

    private fun resolveEmbedToStream(embedUrl: String, referer: String): String? {
        if (embedUrl.isBlank()) return null
        if (isAdServerUrl(embedUrl)) return null

        // 1. Already a direct stream
        val path = embedUrl.substringBefore("?").lowercase()
        if (path.endsWith(".mp4") || path.endsWith(".m3u8")) return embedUrl

        // 2. Fetch the embed page HTML
        val html = try {
            fetchHtmlSafe(embedUrl, referer)
        } catch (_: Exception) { return null }
        if (html.isBlank()) return null

        // ── Provider-specific extractors ────────────────────────────

        // Voe.sx
        if (embedUrl.contains("voe.sx") || embedUrl.contains("voe.")) {
            Regex("""['"]hls['"]\s*:\s*['"]([^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(html)
                ?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }
            // base64 encoded sources
            Regex("""'e':\s*'([A-Za-z0-9+/=]{50,})'""").find(html)?.groupValues?.get(1)?.let { enc ->
                runCatching {
                    String(android.util.Base64.decode(enc, android.util.Base64.DEFAULT))
                }.getOrNull()?.let { decoded ->
                    Regex("""['"]hls['"]\s*:\s*['"]([^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE)
                        .find(decoded)?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }
                }
            }
        }

        // Doodstream
        if (embedUrl.contains("dood") || embedUrl.contains("ds2play") || embedUrl.contains("dooood")) {
            val passMatch = Regex("""\$\.get\s*\(\s*'(/pass_md5/[^']+)'""").find(html)
            passMatch?.groupValues?.get(1)?.let { passPath ->
                val tokenMatch = Regex("""token=([a-zA-Z0-9]+)""").find(html)
                val token = tokenMatch?.groupValues?.get(1) ?: ""
                val baseHost = try { java.net.URI(embedUrl).let { "${it.scheme}://${it.host}" } } catch (_: Exception) { "" }
                if (baseHost.isNotBlank()) {
                    try {
                        val passReq = Request.Builder().url("$baseHost$passPath")
                            .header("User-Agent", UA).header("Referer", embedUrl).build()
                        val passResult = http.newCall(passReq).execute().use { it.body?.string() ?: "" }
                        if (passResult.isNotBlank()) {
                            val streamUrl = "${passResult.trim()}${System.currentTimeMillis()}?token=$token&expiry=${System.currentTimeMillis() + 60000}"
                            return streamUrl
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Streamtape
        if (embedUrl.contains("streamtape")) {
            Regex("""id=([a-zA-Z0-9_-]+)&expires""").find(html)?.groupValues?.get(1)?.let { id ->
                Regex("""'[^']*'\s*\+\s*'([^']+)'""").find(html)?.groupValues?.get(1)?.let { token ->
                    return "https://streamtape.com/get_video?id=$id&expires=&ip=&token=$token&stream=1"
                }
            }
            // Alternative Streamtape pattern
            Regex("""document\.getElementById\(['"]\S+['"]\)\.innerHTML\s*=\s*["']([^"']+)["']""").find(html)
                ?.groupValues?.get(1)?.let { raw ->
                    if (raw.contains("streamtape")) return "https:$raw"
                }
        }

        // Vidmoly
        if (embedUrl.contains("vidmoly")) {
            Regex("""['"]?(https?://[^\s'"<>]+\.m3u8[^\s'"<>]*)['"]?""").find(html)
                ?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }
        }

        // Uqload / Govid / Vidsharing (sources: [...])
        Regex("""sources\s*:\s*\[\s*['"]([^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""").find(html)
            ?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }

        // Mixdrop
        if (embedUrl.contains("mixdrop")) {
            Regex("""(?:MDCore\.wurl|\"wurl\")\s*=\s*["']([^"']+)["']""").find(html)
                ?.groupValues?.get(1)?.let { wurl ->
                    return if (wurl.startsWith("//")) "https:$wurl" else wurl
                }
        }

        // Vidara / Sugbaioug API
        if (embedUrl.contains("vidara.") || embedUrl.contains("sugbaioug.")) {
            val fileCode = embedUrl.trimEnd('/').substringAfterLast('/')
            if (fileCode.isNotBlank()) {
                try {
                    val body = """{"filecode":"$fileCode","device":"android"}"""
                    val req = Request.Builder().url("https://vidara.to/api/stream")
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .header("User-Agent", UA).header("Referer", embedUrl).build()
                    http.newCall(req).execute().use { r ->
                        Regex(""""streaming_url"\s*:\s*"([^"]+)"""").find(r.body?.string() ?: "")
                            ?.groupValues?.get(1)?.replace("\\/", "/")?.takeIf { it.startsWith("http") }
                            ?.let { return it }
                    }
                } catch (_: Exception) {}
            }
        }

        // ── Generic extractors ────────────────────────────────────────

        // Direct <source src="..."> tags
        Regex("""<source\s+[^>]*src=['"]([^'"]+\.(?:mp4|m3u8)(?:\?[^'"]*)?)['"']""", RegexOption.IGNORE_CASE)
            .findAll(html).mapNotNull { it.groupValues.getOrNull(1)?.replace("&amp;", "&")?.trim() }
            .firstOrNull { it.startsWith("http") }?.let { return it }

        // Any URL containing .m3u8 or .mp4
        Regex("""['"]?(https?://[^\s'"<>]+\.(?:m3u8|mp4)(?:\?[^\s'"<>]*)?)['"]?""", RegexOption.IGNORE_CASE)
            .findAll(html).mapNotNull { it.groupValues.getOrNull(1) }
            .firstOrNull { it.startsWith("http") && !it.contains("thumb") && !it.contains("poster") }
            ?.let { return it }

        // JWPlayer / VideoJS sources
        Regex("""(?:file|src)\s*:\s*['"]([^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }

        // Follow iframe hop (one level deep)
        val ifrSrc = Jsoup.parse(html, embedUrl).select("iframe[src]")
            .firstOrNull { !isAdUrl(it.attr("src")) }
            ?.attr("abs:src")?.trim() ?: ""
        if (ifrSrc.isNotBlank() && ifrSrc != embedUrl) {
            return resolveEmbedToStream(ifrSrc, embedUrl)
        }

        // Hex-encoded stream URL
        Regex("""['"]?(68747470[0-9a-fA-F]{30,})['"]?""").findAll(html).forEach { m ->
            val dec = hexToStringMPR(m.groupValues[1])
            if (dec.startsWith("http") && (dec.contains(".m3u8") || dec.contains(".mp4"))) return dec
        }

        // atob() Base64 URLs
        Regex("""atob\(['"]([A-Za-z0-9+/=]{20,})['"]""").findAll(html).forEach { m ->
            val decoded = runCatching {
                String(android.util.Base64.decode(m.groupValues[1], android.util.Base64.DEFAULT))
            }.getOrNull() ?: return@forEach
            Regex("""['"]?(https?://[^\s'"<>]+\.(?:m3u8|mp4)[^\s'"<>]*)['"]?""")
                .find(decoded)?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }
        }

        return null
    }

    private fun fetchHtmlSafe(url: String, referer: String): String = try {
        fetchHtml(url, referer)
    } catch (_: Exception) { "" }

    private fun isAdUrl(url: String): Boolean {
        val adKeywords = listOf("ads.", "doubleclick", "googlesyndication", "adservice",
            "analytics", "tracking", "pixel.", "beacon.", "cdn.ad")
        return adKeywords.any { url.contains(it, ignoreCase = true) }
    }

    private fun hexToStringMPR(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < hex.length - 1) { sb.append(hex.substring(i, i + 2).toInt(16).toChar()); i += 2 }
        return sb.toString()
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
            // Season pages often need more time than catalog cards. Use the
            // bounded season client so slow mirrors do not look like empty data.
            val itemReferer = pageOrigin(item.pageUrl, baseUrl)
            val html = fetchSeasonHtml(item.pageUrl, "$itemReferer/")
            val doc = Jsoup.parse(html, item.pageUrl)
            doc.select("footer, script, style").remove()

            val rawEpisodes = mutableListOf<Triple<String, String, Int?>>()
            val pageSeasonNumber = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
            val requestedSeasonPageNumber = seasonNumbersOnly?.singleOrNull()
                ?.takeIf { pageSeasonNumber == it }
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
                                if (SeriesCatalogParser.isInsideUnrelatedContentContainer(el)) continue
                            val href = listOf("href", "data-url", "data-href", "data-link", "data-episode-link")
                                .firstNotNullOfOrNull { attribute ->
                                    val candidate = el.absUrl(attribute).ifBlank { el.attr(attribute).trim() }
                                    resolveProviderContentPageUrl(candidate, item.pageUrl)
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
                            val linkSeason = SeriesCatalogParser.seasonNumber(txt, href)
                                ?: SeriesCatalogParser.seasonNumberFromElement(el, txt, href)
                            if (seasonNumbersOnly != null && linkSeason != null &&
                                linkSeason !in seasonNumbersOnly
                            ) continue
                            // Prefer the episode's own season context over the fetched page hint.
                            val seasonNumber = linkSeason ?: requestedSeasonPageNumber
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

            // A contiguous first page is not proof that the season is complete.
            // Anime and long-running series commonly paginate cleanly (1, 2, 3…)
            // while still having hundreds of later episodes.
            if (SeriesCatalogParser.linkedEpisodePages(doc, item.pageUrl).isNotEmpty()) {
                val paginated = SeriesCatalogParser.collectPaginatedEpisodes(
                    firstDocument = doc,
                    firstPageUrl = item.pageUrl,
                    fetchDocument = { pageUrl ->
                        val pageHtml = if (forceRefresh) fetchSeasonHtml(pageUrl, itemReferer)
                            else fetchHtml(pageUrl, itemReferer)
                        pageHtml.takeIf(String::isNotBlank)?.let { Jsoup.parse(it, pageUrl) }
                    },
                    parseEpisodes = ::collectEpisodes,
                    maxPages = if (forceRefresh) 8 else 4,
                    timeoutSeconds = if (forceRefresh) 15 else 8,
                )
                for (ep in paginated) {
                    val existingIndex = rawEpisodes.indexOfFirst {
                        SeriesCatalogParser.normalizedUrl(it.second) == SeriesCatalogParser.normalizedUrl(ep.second)
                    }
                    if (existingIndex < 0) rawEpisodes += ep
                    else if (rawEpisodes[existingIndex].third == null && ep.third != null) rawEpisodes[existingIndex] = ep
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
                            && !SeriesCatalogParser.isInsideUnrelatedContentContainer(el)
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
                        val parentReferer = pageOrigin(parentHref, itemReferer)
                        val parentHtml = if (forceRefresh) fetchSeasonHtml(parentHref, parentReferer)
                            else fetchHtml(parentHref, parentReferer)
                        val parentDoc = Jsoup.parse(parentHtml, parentHref)
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
                .flatMap { document ->
                    SeriesCatalogParser.linkedSeasonPages(document, item.pageUrl) { pageUrl ->
                        isProviderContentUrl(pageUrl, item.pageUrl)
                    }
                }
                .distinctBy { it.first }
            val seasonPageUrlByNumber = seasonPages.groupBy { it.first }
                .mapValues { (_, rows) -> rows.first().second }
            val visibleEpisodeNumbers = rawEpisodes.mapNotNull { (title, href, seasonNumber) ->
                val episodeNumber = SeriesCatalogParser.episodeNumber(title, href) ?: return@mapNotNull null
                val resolvedSeason = SeriesCatalogParser.seasonNumber(title, href)
                    ?: seasonNumber ?: return@mapNotNull null
                resolvedSeason to episodeNumber
            }.groupBy({ it.first }, { it.second })
            val seasonPagesToFetch = seasonPages
                .filter { (number, _) ->
                    (seasonNumbersOnly == null || number in seasonNumbersOnly) &&
                        (forceRefresh || SeriesCatalogParser.needsEpisodeSequenceRepair(
                            visibleEpisodeNumbers[number].orEmpty(),
                        ))
                }
                .take(30)
            val seasonPageCompletion = ExecutorCompletionService<Pair<Int, List<Triple<String, String, Int?>>>>(seasonPagePool)
            val seasonPageFutures = seasonPagesToFetch.map { (seasonNumber, seasonUrl) ->
                seasonPageCompletion.submit(Callable {
                    val seasonReferer = pageOrigin(seasonUrl, itemReferer)
                    val seasonHtml = if (forceRefresh) fetchSeasonHtml(seasonUrl, seasonReferer)
                        else fetchHtml(seasonUrl, seasonReferer)
                    val seasonDoc = Jsoup.parse(seasonHtml, seasonUrl)
                    seasonDoc.select("footer, script, style").remove()
                    val paginated = SeriesCatalogParser.collectPaginatedEpisodes(
                        firstDocument = seasonDoc,
                        firstPageUrl = seasonUrl,
                        fetchDocument = { pageUrl ->
                            val pageHtml = if (forceRefresh) fetchSeasonHtml(pageUrl, seasonReferer)
                                else fetchHtml(pageUrl, seasonReferer)
                            pageHtml.takeIf(String::isNotBlank)?.let { Jsoup.parse(it, pageUrl) }
                        },
                        parseEpisodes = { page ->
                            collectEpisodes(page).map { (title, href, number) ->
                                Triple(title, href, number ?: seasonNumber)
                            }
                        },
                        maxPages = if (forceRefresh) 8 else 4,
                        timeoutSeconds = if (forceRefresh) 15 else 8,
                    )
                    seasonNumber to paginated
                })
            }
            // A series page is on the first paint path, so keep its initial
            // season lookup short. Background mirror lookups get enough time
            // to walk paginated episode lists instead of being cut off early.
            val seasonPageWaitSeconds = if (forceRefresh || seasonNumbersOnly != null) 22L else 5L
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
                for ((title, href, parsedSeason) in linkedEpisodes) {
                    val episodeNumber = SeriesCatalogParser.episodeNumber(title, href) ?: continue
                    val urlKey = SeriesCatalogParser.normalizedUrl(href)
                    val explicitSeason = SeriesCatalogParser.seasonNumber(title, href) ?: parsedSeason
                    if (explicitSeason != null && explicitSeason != linkedSeasonNumber) continue
                    val resolvedSeason = explicitSeason ?: linkedSeasonNumber
                    val existingIndex = rawEpisodes.indexOfFirst {
                        SeriesCatalogParser.normalizedUrl(it.second) == urlKey
                    }
                    if (existingIndex < 0) {
                        rawEpisodes += Triple(title, href, resolvedSeason)
                        seasonPageAssignments += urlKey
                    } else if (urlKey !in seasonPageAssignments) {
                        rawEpisodes[existingIndex] = Triple(title, href, resolvedSeason)
                        seasonPageAssignments += urlKey
                    }
                }
            }
            seasonPageFutures.forEach { if (!it.isDone) it.cancel(true) }

            val expectedSeasonNumber = pageSeasonNumber
                ?: if (seasonNumbersOnly == null && (isEpisodePage || seasonPages.isEmpty())) {
                    SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")
                } else null
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
                requestedSeasonPageNumber != null -> requestedSeasonPageNumber
                seasonNumbersOnly != null -> null
                expectedSeasonNumber != null -> expectedSeasonNumber
                !isEpisodePage && seasonPages.isEmpty() -> 1
                else -> null
            }
            val numberedEpisodes = parsedEpisodes.mapNotNull { (seasonNumber, episodeNumber, href) ->
                (seasonNumber ?: inferredSeasonNumber)?.takeIf { resolvedSeason ->
                    (seasonNumbersOnly == null || resolvedSeason in seasonNumbersOnly) &&
                        (seasonPages.isEmpty() || resolvedSeason in seasonPageUrlByNumber ||
                            pageSeasonNumber == resolvedSeason)
                }?.let { Triple(it, episodeNumber, href) }
            }
            val finalEpisodeLimits = rawEpisodes.mapNotNull { (title, href, domSeason) ->
                val finalNumber = SeriesCatalogParser.finalEpisodeNumber(title, href)
                    ?: return@mapNotNull null
                val seasonNumber = SeriesCatalogParser.seasonNumber(title, href)
                    ?: domSeason ?: requestedSeasonPageNumber ?: inferredSeasonNumber
                    ?: return@mapNotNull null
                seasonNumber to finalNumber
            }.groupBy({ it.first }, { it.second }).mapValues { (_, numbers) -> numbers.minOrNull()!! }
            val boundedEpisodes = numberedEpisodes.filter { (seasonNumber, episodeNumber) ->
                episodeNumber <= (finalEpisodeLimits[seasonNumber] ?: Int.MAX_VALUE)
            }
            val seasons = boundedEpisodes.groupBy { it.first }.toSortedMap().map { (seasonNumber, rows) ->
                val episodes = rows
                    .groupBy { it.second }
                    .toSortedMap()
                    .map { (episodeNumber, variants) ->
                        val href = variants.first().third
                        val alternativeUrls = variants.map { it.third }
                            .distinctBy(SeriesCatalogParser::normalizedUrl)
                            .filter { SeriesCatalogParser.normalizedUrl(it) != SeriesCatalogParser.normalizedUrl(href) }
                        ArabseedEpisode(
                            number = episodeNumber,
                            title = "الحلقة $episodeNumber",
                            pageUrl = href,
                            seasonNumber = seasonNumber,
                            providerName = getProviderForUrl(item.pageUrl)?.name ?: item.providerName,
                            alternatePageUrls = alternativeUrls,
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
                    if (SeriesCatalogParser.isInsideUnrelatedContentContainer(node)) continue
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
                CatalogDiskCache.saveSeasonBundle("providers:seasons:v24:" + item.pageUrl, bundle)
            }
            bundle
        } catch (_: Exception) {
            ArabseedSeasonBundle(emptyList())
        }
    }

    /**
     * Fetches the next small batch of catalog episodes on demand. This keeps the
     * first detail render bounded while allowing long anime/series to continue
     * past the first several HTML pages.
     */
    fun fetchMoreSeasonEpisodes(
        item: ArabseedItem,
        seasonUrl: String,
        cursorUrl: String,
        seasonNumber: Int,
        knownEpisodeUrls: Set<String>,
        pageLimit: Int = 4,
        episodeLimit: Int = 60,
    ): Pair<ArabseedSeasonBundle, String?> {
        val seasonUri = runCatching { java.net.URI(seasonUrl.trim()) }.getOrNull()
            ?: return ArabseedSeasonBundle(emptyList()) to null
        val provider = getProviderForUrl(seasonUrl) ?: return ArabseedSeasonBundle(emptyList()) to null
        if (seasonNumber <= 0 || seasonUri.scheme !in setOf("http", "https") || seasonUri.host.isNullOrBlank()) {
            return ArabseedSeasonBundle(emptyList()) to null
        }

        val known = knownEpisodeUrls.map(SeriesCatalogParser::normalizedUrl).filter(String::isNotBlank).toSet()
        val addedByUrl = linkedMapOf<String, ArabseedEpisode>()
        val visitedPages = linkedSetOf<String>()
        var pageUrl = cursorUrl.trim().ifBlank { seasonUrl.trim() }
        var pagesFetched = 0
        var nextPageUrl: String? = null

        while (pagesFetched < pageLimit.coerceIn(1, 8) && addedByUrl.size < episodeLimit.coerceIn(1, 120)) {
            val normalizedPage = SeriesCatalogParser.normalizedUrl(pageUrl)
            if (!visitedPages.add(normalizedPage)) break
            val uri = runCatching { java.net.URI(pageUrl) }.getOrNull() ?: break
            val sameConfiguredProvider = isConfiguredProviderHost(provider, pageUrl)
            if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank() ||
                (!sameConfiguredProvider && !isProviderContentUrl(pageUrl, seasonUrl))
            ) break

            val referer = pageOrigin(seasonUrl, provider.baseUrl)
            val html = runCatching { fetchSeasonHtml(pageUrl, referer) }.getOrDefault("")
            if (html.isBlank()) {
                nextPageUrl = pageUrl // keep the cursor retryable after a transient failure
                break
            }
            val document = Jsoup.parse(html, pageUrl).apply { select("footer, script, style").remove() }
            pagesFetched++
            SeriesCatalogParser.parseEpisodeEntries(
                document = document,
                currentPageUrl = pageUrl,
                defaultSeasonNumber = seasonNumber,
                isAllowedContentUrl = { candidate ->
                    isConfiguredProviderHost(provider, candidate) || isProviderContentUrl(candidate, seasonUrl)
                },
            ).forEach { (title, episodeUrl, parsedSeason) ->
                if (parsedSeason != null && parsedSeason != seasonNumber) return@forEach
                val key = SeriesCatalogParser.normalizedUrl(episodeUrl)
                if (key.isBlank() || key in known || key in addedByUrl) return@forEach
                val number = SeriesCatalogParser.episodeNumber(title, episodeUrl) ?: return@forEach
                addedByUrl[key] = ArabseedEpisode(
                    number = number,
                    title = "الحلقة $number",
                    pageUrl = episodeUrl,
                    seasonNumber = seasonNumber,
                    providerName = provider.name,
                )
            }

            val next = SeriesCatalogParser.nextEpisodePage(document, pageUrl)
            if (next.isNullOrBlank() || SeriesCatalogParser.normalizedUrl(next) in visitedPages) {
                nextPageUrl = null
                break
            }
            nextPageUrl = next
            pageUrl = next
        }

        val season = ArabseedSeason(
            number = seasonNumber,
            title = SeriesCatalogParser.seasonTitle(seasonNumber),
            episodes = addedByUrl.values.sortedBy { it.number },
            pageUrl = seasonUrl,
        )
        android.util.Log.i(
            TAG,
            "season.more provider=${provider.id} season=$seasonNumber pages=$pagesFetched added=${season.episodes.size} hasMore=${nextPageUrl != null}",
        )
        return ArabseedSeasonBundle(listOf(season)) to nextPageUrl
    }

    /** Merge the selected provider's season data with exact-title matches from the
     * other configured providers. Catalog cards are deduplicated for display, so this
     * lookup deliberately runs provider searches again to retain each source URL. */
    fun fetchSeasonBundleAcrossProviders(
        item: ArabseedItem,
        primaryBundle: ArabseedSeasonBundle,
        forceRefresh: Boolean = false,
    ): ArabseedSeasonBundle {
        val cacheKey = "combined:seasons:v23:${item.pageUrl}"
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

        // Persist the selected source's result before searching every mirror.
        // The detail screen can render these seasons immediately while the
        // slower cross-provider repair continues in the background.
        if (primaryBundle.seasons.isNotEmpty() || primaryBundle.relatedParts.isNotEmpty()) {
            CatalogDiskCache.saveSeasonBundle(cacheKey, primaryBundle)
        }

        val year = releaseYear(item)
        val searchTasks = PROVIDERS.filter { it.enabled }.map { provider ->
            {
                try {
                    val encoded = java.net.URLEncoder.encode(title, "UTF-8")
                    fetchProviderListing(
                        provider,
                        provider.searchPath.replace("{q}", encoded),
                        ArabseedType.SERIES,
                    )
                        .asSequence()
                        .filter { candidate ->
                            // Providers often title the same work differently (Arabic spelling,
                            // English transliteration, year/season suffixes). Use the shared
                            // conservative identity matcher so an alternate source can repair
                            // missing episodes instead of being discarded by strict equality.
                            val titleMatch = matchesSeriesTitle(titleKey, candidate)
                            val candidateYear = releaseYear(candidate)
                            val isSeasonPage = SeriesCatalogParser.seasonNumber(candidate.title, candidate.pageUrl) != null
                            val compatibleYear = year.isBlank() || year == candidateYear ||
                                (candidateYear.isBlank() && isSeasonPage)
                            candidate.type == ArabseedType.SERIES &&
                                !SeriesCatalogParser.isEpisode(candidate.title, candidate.pageUrl) &&
                                titleMatch && compatibleYear
                        }
                        .distinctBy { SeriesCatalogParser.normalizedUrl(it.pageUrl) }
                        .toList()
                } catch (error: Exception) {
                    android.util.Log.w(
                        TAG,
                        "Season search skipped provider=${provider.id} error=${error.javaClass.simpleName}",
                    )
                    emptyList()
                }
            }
        }

        val discovered = collectParallel(searchTasks, timeoutSeconds = 8)
        val primaryUrl = SeriesCatalogParser.normalizedUrl(item.pageUrl)
        val candidates = selectSeriesPages(discovered)
            .filter { SeriesCatalogParser.normalizedUrl(it.pageUrl) != primaryUrl }
            .distinctBy { SeriesCatalogParser.normalizedUrl(it.pageUrl) }
        val primarySeasonNumbers = primaryBundle.seasons.map { it.number }.filter { it > 0 }.distinct()
        val seasonPagesByNumber = candidates
            .mapNotNull { candidate ->
                SeriesCatalogParser.seasonNumber(candidate.title, candidate.pageUrl)?.let { it to candidate }
            }
            .groupBy({ it.first }, { it.second })
        // Recheck known seasons in the background; the merge routine keeps
        // episode identity scoped to its season and number.
        val knownSeasonNumbers = primarySeasonNumbers.sorted()
        val seasonsToRepair = knownSeasonNumbers
        val localSeasonMirrors = primaryBundle.seasons
            .filter { it.number in seasonsToRepair && it.pageUrl.startsWith("http", ignoreCase = true) }
            .sortedBy { it.number }
            .map { season ->
                val title = SeriesCatalogParser.seasonTitle(season.number)
                item.copy(
                    id = "${item.id}_season_${season.number}",
                    title = title,
                    arabicTitle = title,
                    pageUrl = season.pageUrl,
                    parentSeriesTitle = null,
                    parentSeriesPageUrl = null,
                    latestEpisodeLabel = "",
                ) to setOf(season.number)
            }
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
            .take(PROVIDERS.count { it.enabled })
            .map { it to null }
        // Probe every configured provider's canonical page first, then the
        // selected source's season pages, then cross-provider season mirrors.
        // This avoids truncating a configured provider when a show has several
        // seasons while keeping total background fan-out bounded.
        val mirrorLimit = (PROVIDERS.count { it.enabled } + knownSeasonNumbers.size * 3)
            .coerceIn(16, 48)
        val mirrors = (canonicalMirrors + localSeasonMirrors + seasonPageMirrors)
            .distinctBy { SeriesCatalogParser.normalizedUrl(it.first.pageUrl) }
            .take(mirrorLimit)

        val discoveredProviders = discovered
            .groupBy { getProviderForUrl(it.pageUrl)?.id ?: it.providerName }
            .mapValues { (_, rows) -> rows.size }
            .toSortedMap()
        android.util.Log.i(
            TAG,
            "Season enrichment discovered=${discovered.size} providers=$discoveredProviders " +
                "knownSeasons=${knownSeasonNumbers.size} mirrors=${mirrors.size}",
        )

        val bundles = mutableListOf(item to primaryBundle)
        // Publish the primary source immediately. The UI can show its current
        // season/episode list while mirror providers continue to repair older
        // seasons in parallel, even when a secondary site is slow or offline.
        if (primaryBundle.seasons.isNotEmpty() || primaryBundle.relatedParts.isNotEmpty()) {
            CatalogDiskCache.saveSeasonBundle(cacheKey, primaryBundle)
        }
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
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
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
            val (sourceItem, sourceBundle) = completedBundle
            val sourceCounts = sourceBundle.seasons.joinToString(",") {
                "${it.number}:${it.episodes.size}"
            }
            android.util.Log.i(
                TAG,
                "Season mirror parsed provider=${getProviderForUrl(sourceItem.pageUrl)?.id ?: "unknown"} " +
                    "seasonCounts=$sourceCounts relatedParts=${sourceBundle.relatedParts.size}",
            )
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
        // A sparse newest season is still incomplete: it may simply be the
        // latest season currently airing, but internal gaps must be retried.
        val unresolvedSparseSeason = merged.seasons.any {
            SeriesCatalogParser.needsEpisodeSequenceRepair(
                it.episodes.map { episode -> episode.number },
            )
        }
        val episodeCounts = merged.seasons.joinToString(",") { season ->
            "${season.number}:${season.episodes.size}"
        }
        android.util.Log.i(
            TAG,
            "Season enrichment complete sources=${bundles.size} seasons=${merged.seasons.size} " +
                "episodeCounts=$episodeCounts unresolvedSparse=$unresolvedSparseSeason",
        )
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
            seasonAggregationPool.submit {
                var result: ArabseedSeasonBundle? = null
                try {
                    result = fetchSeasonBundleAcrossProviders(item, primaryBundle, forceRefresh)
                } catch (error: Exception) {
                    android.util.Log.w(TAG, "Season enrichment failed for provider=${item.providerName}", error)
                } finally {
                    result?.let { recordSeasonEnrichmentAttempt(item, it) }
                    seasonAggregationInFlight.remove(key)
                }
            }
        }

        val latestCached = loadCachedMergedSeasonBundle(item.pageUrl) ?: cached
        return latestCached?.let { mergeProviderSeasonBundles(listOf(item to it, item to primaryBundle)) }
            ?: primaryBundle
    }

    /**
     * Return the saved season list immediately and fetch/repair the series in
     * the repository pool. This keeps a cold detail-page request from waiting
     * on multiple provider pages before Flutter can render.
     */
    fun loadSeasonBundleAndRefreshInBackground(
        item: ArabseedItem,
        forceRefresh: Boolean = false,
    ): ArabseedSeasonBundle {
        val cached = loadCachedMergedSeasonBundle(item.pageUrl)
            ?: if (isMultiProvider(item.pageUrl)) {
                loadCachedSeasonBundle(item.pageUrl)
            } else {
                ArabseedRepository.loadCachedSeasonBundle(item.pageUrl)
            }
        if (item.type != ArabseedType.SERIES || (!forceRefresh && !shouldRefreshSeasonBundle(item))) {
            return cached ?: ArabseedSeasonBundle(emptyList())
        }

        val key = SeriesCatalogParser.normalizedUrl(item.pageUrl)
        if (key.isNotBlank() && seasonAggregationInFlight.add(key)) {
            seasonAggregationPool.submit {
                try {
                    val primary = if (isMultiProvider(item.pageUrl)) {
                        fetchSeasonsAndEpisodes(item, forceRefresh = true)
                    } else {
                        ArabseedRepository.fetchSeasonsAndEpisodes(item, forceRefresh = true)
                    }
                    val merged = fetchSeasonBundleAcrossProviders(item, primary, forceRefresh = true)
                    recordSeasonEnrichmentAttempt(item, merged)
                } catch (error: Exception) {
                    android.util.Log.w(
                        TAG,
                        "Background season refresh failed provider=${item.providerName} " +
                            "error=${error.javaClass.simpleName}",
                    )
                } finally {
                    seasonAggregationInFlight.remove(key)
                }
            }
        }
        return cached ?: ArabseedSeasonBundle(emptyList())
    }

    fun isSeasonAggregationRunning(pageUrl: String): Boolean =
        SeriesCatalogParser.normalizedUrl(pageUrl) in seasonAggregationInFlight

    fun shouldRefreshSeasonBundle(item: ArabseedItem): Boolean {
        if (item.type != ArabseedType.SERIES) return false
        val normalizedPage = SeriesCatalogParser.normalizedUrl(item.pageUrl)
        if (normalizedPage in seasonAggregationInFlight) return false
        val cached = loadCachedMergedSeasonBundle(item.pageUrl)
        if (cached == null) return true
        val attempt = CatalogDiskCache.loadJson(
            seasonEnrichmentAttemptKey(item.pageUrl),
            SEASON_ENRICHMENT_ATTEMPT_TTL_MS,
        )?.value
        if (attempt != null) {
            val finishedAt = attempt.optLong("finishedAt", 0L)
            val incomplete = attempt.optBoolean("incomplete", true)
            val retryWindow = if (incomplete) {
                SEASON_ENRICHMENT_PARTIAL_RETRY_MS
            } else {
                SEASON_ENRICHMENT_RETRY_MS
            }
            if (finishedAt > 0L && System.currentTimeMillis() - finishedAt < retryWindow) return false
        }
        return needsSeasonEnrichment(item, cached)
    }

    private fun recordSeasonEnrichmentAttempt(item: ArabseedItem, bundle: ArabseedSeasonBundle) {
        val incomplete = bundle.seasons.isEmpty() || bundle.seasons.any { season ->
            SeriesCatalogParser.needsEpisodeSequenceRepair(season.episodes.map { it.number })
        }
        CatalogDiskCache.saveJson(
            seasonEnrichmentAttemptKey(item.pageUrl),
            JSONObject()
                .put("finishedAt", System.currentTimeMillis())
                .put("incomplete", incomplete),
        )
    }

    fun seasonNumbersNeedingRepair(bundle: ArabseedSeasonBundle?): Set<Int> {
        if (bundle == null) return emptySet()
        return bundle.seasons
            .filter {
                it.number > 0 && SeriesCatalogParser.needsEpisodeSequenceRepair(
                    it.episodes.map { episode -> episode.number },
                )
            }
            .mapTo(linkedSetOf()) { it.number }
    }

    private fun seasonEnrichmentAttemptKey(pageUrl: String): String =
        "combined:season-attempt:v19:$pageUrl"

    private fun needsSeasonEnrichment(item: ArabseedItem, bundle: ArabseedSeasonBundle): Boolean {
        if (item.type != ArabseedType.SERIES) return false
        // A contiguous list can still be truncated at the end (for example,
        // episodes 1..13 while the source has 24). Never cache a permanent
        // "complete" result based only on sequence gaps; retry periodically.
        return true
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
        val prioritizedBundles = bundles.sortedBy { (sourceItem, _) ->
            SeriesCatalogParser.seasonNumber(sourceItem.title, sourceItem.pageUrl) == null
        }
        for ((sourceItem, bundle) in prioritizedBundles) {
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
                    val normalizedEpisode = episode.copy(
                        providerName = episode.providerName.ifBlank { sourceName },
                    )
                    val existing = episodeMap[episode.number]
                    if (existing == null) {
                        episodeMap[episode.number] = normalizedEpisode
                    } else {
                        val pages = (listOf(existing.pageUrl) + existing.alternatePageUrls +
                            normalizedEpisode.pageUrl + normalizedEpisode.alternatePageUrls)
                            .filter { it.startsWith("http", ignoreCase = true) }
                            .distinctBy(SeriesCatalogParser::normalizedUrl)
                        episodeMap[episode.number] = existing.copy(
                            alternatePageUrls = (existing.alternatePageUrls + pages)
                                .filter { SeriesCatalogParser.normalizedUrl(it) !=
                                    SeriesCatalogParser.normalizedUrl(existing.pageUrl) }
                                .distinctBy(SeriesCatalogParser::normalizedUrl),
                        )
                    }
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
            alternatePageUrls = (preferred.alternatePageUrls + other.pageUrl + other.alternatePageUrls)
                .filter { it.startsWith("http", ignoreCase = true) }
                .filter { SeriesCatalogParser.normalizedUrl(it) != SeriesCatalogParser.normalizedUrl(preferred.pageUrl) }
                .distinctBy(SeriesCatalogParser::normalizedUrl)
                .take(8),
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
