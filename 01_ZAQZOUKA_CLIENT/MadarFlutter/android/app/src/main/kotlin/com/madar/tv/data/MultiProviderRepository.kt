package com.madar.tv.data

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec



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
    private val seasonPagePool = Executors.newFixedThreadPool(10)



    // ── HTTP Client ────────────────────────────────────────────────────

    private val sharedConnectionPool = okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES)
    private val sharedDispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 16
    }

    private val http = OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .connectionPool(sharedConnectionPool)
        .dispatcher(sharedDispatcher)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val probeHttp = http.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .build()

    private val providerPageHttp = http.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
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
    data class ResolvedStreamResult(val url: String, val referer: String)
    private val streamProbeCache = ConcurrentHashMap<String, StreamProbe>()
    private val embedAliveCache = ConcurrentHashMap<String, StreamProbe>()
    private val resolvedStreamCache = ConcurrentHashMap<String, Pair<Long, ResolvedStreamResult>>()
    private const val STREAM_PROBE_TTL_MS = 30_000L
    private const val STREAM_RESOLVE_TTL_MS = 45_000L
    private const val SERVER_FALLBACK_TTL = 10 * 60 * 1000L
    private const val SERVER_FALLBACK_PARTIAL_TTL = 45 * 1000L
    private const val SERVER_FALLBACK_EMPTY_TTL = 90 * 1000L
    private val seasonCacheAt = ConcurrentHashMap<String, Long>()
    private const val SEASON_TTL = 30 * 60 * 1000L
    private const val SEASON_ENRICHMENT_RETRY_MS = 5 * 60 * 1000L
    private const val SEASON_ENRICHMENT_PARTIAL_RETRY_MS = 5 * 60 * 1000L   // was 30s — give enrichment more time between retries
    private const val SEASON_ENRICHMENT_ATTEMPT_TTL_MS = 24 * 60 * 60 * 1000L
    private val seasonMirrorPool = Executors.newFixedThreadPool(10)
    private val seasonAggregationPool = Executors.newFixedThreadPool(4)
    private val seasonAggregationInFlight = ConcurrentHashMap.newKeySet<String>()


    private val seasonCache = ConcurrentHashMap<String, ArabseedSeasonBundle>()



    private fun catalogCacheKey(type: ArabseedType?, page: Int): String = when (type) {
        // Bumped to v6 so fixed mirrors and Arab/Turkish/Korean/Asian feeds refresh immediately.
        ArabseedType.MOVIE -> "movies:v6:$page"
        ArabseedType.SERIES -> "series:v6:$page"
        null -> "catalog:v6:$page"
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
        val key = "search:v9:" + query.trim().lowercase()
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
        val snapshot = CatalogDiskCache.loadDetail("providers:detail:v4:" + pageUrl) ?: return null
        if (snapshot.value.servers.isEmpty()) return null
        detailCache[pageUrl] = snapshot.value
        detailCacheAt[pageUrl] = snapshot.savedAt
        return snapshot.value
    }

    fun getCachedDetail(pageUrl: String): ArabseedDetail? = detailCache[pageUrl]

    fun loadCachedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        seasonCache[pageUrl]?.let { return it }
        val snapshot = CatalogDiskCache.loadSeasonBundle("providers:seasons:v26:" + pageUrl) ?: return null
        val normalized = SeriesCatalogParser.normalizeSeasonBundle(snapshot.value)
        seasonCache[pageUrl] = normalized
        seasonCacheAt[pageUrl] = snapshot.savedAt
        return normalized
    }

    fun loadCachedMergedSeasonBundle(pageUrl: String): ArabseedSeasonBundle? {
        val snapshot = CatalogDiskCache.loadSeasonBundle("combined:seasons:v26:$pageUrl") ?: return null
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
            CatalogDiskCache.saveSeasonBundle("combined:seasons:v26:${seriesItem.pageUrl}", merged)
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
        val isPrimary: Boolean = false,
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

        // ── 1. عرب سيد / ماي سيد (MySeed / ArabSeed) ─────────────────────────
        // PRIMARY #1: https://m.myseed.tv (سيرفر مباشر فائق السرعة + سيرفرات بديلة)
        ProviderConfig(
            id = "arabseed",
            name = "عرب سيد",
            baseUrl = "https://m.myseed.tv",
            mirrorDomains = emptyList(),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/find/?word={q}",
            cardSelectors = listOf(
                "a[href*='film-']", "a[href*='%d9%81%d9%8a%d9%84%d9%85-']",
                ".innov-series-card a", "a.movie__block", ".item__contents a",
                "article a", ".GridItem a",
            ),
            titleSelectors = listOf("h3", "h2", ".title", "img[alt]", "[title]"),
            isPrimary = true,
        ),

        // ── 2. عرب سيد الأصلي (ArabSeed Here - merged into myseed.name under cimanow) ──
        ProviderConfig(
            id = "arabseed_here",
            name = "عرب سيد الأصلي",
            baseUrl = "https://arabseed.here",
            mirrorDomains = emptyList(),
            moviesPath = "/category/%D8%A7%D9%81%D9%84%D8%A7%D9%85-%D8%A7%D8%AC%D9%86%D8%A8%D9%8A/page/{page}/",
            seriesPath = "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A7%D8%AC%D9%86%D8%A8%D9%8A/page/{page}/",
            searchPath = "/find?word={q}",
            cardSelectors = listOf(
                "a[href*='/film/']", "a[href*='/episode/']",
                "article a", ".post-item a", ".item-details a",
            ),
            titleSelectors = listOf("h2.title", "h2", "h3", ".title", "[title]"),
            enabled = false,
        ),

        // ── 3. وي سيما (WeCima) ────────────────────────────────────────
        ProviderConfig(
            id = "wecima",
            name = "وي سيما",
            baseUrl = "https://wecima.cc",
            mirrorDomains = emptyList(),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/arabic-series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "li.video-grid div.thumb a", "li.video-grid a",
                "a[href*='/video/']", "a[href*='watch-']",
                ".Thumb--GridItem a", ".GridItem a", "div.Block--Item a", "article a",
            ),
            titleSelectors = listOf("img[alt]", "h3", "h2", "strong.hasyear", ".Title", "[title]"),
            isPrimary = true,
        ),

        // ── 4. أكوام (Akwam) ─────────────────────────────────────────
        ProviderConfig(
            id = "akwam",
            name = "أكوام",
            baseUrl = "https://akw.cam",
            mirrorDomains = emptyList(),
            moviesPath = "/movies?page={page}",
            seriesPath = "/series?page={page}",
            searchPath = "/search?q={q}",
            cardSelectors = listOf(
                "a.br-card-link[href*='/akfilm-']",
                "a.br-card-link[href*='/akdrama-']",
                "a.br-card-link[href*='/akseries-']",
                "a.br-card-link",
                "a[href*='/akfilm-']",
                "a[href*='/akdrama-']",
                "a[href*='/akseries-']",
                ".br-card a",
            ),
            titleSelectors = listOf("h3.br-card-title", "h3", "h2", ".title", "img[alt]", "[title]"),
            isPrimary = true,
        ),

        // ── 5. فاصل إعلاني (FaselHD - faselhd.online -> vd2734.fasel4hdx.space) ──
        ProviderConfig(
            id = "faselhd",
            name = "فاصل إعلاني",
            baseUrl = "https://faselhd.online",
            mirrorDomains = listOf("https://fasel4hdx.space"),
            moviesPath = "/movies?page={page}",
            seriesPath = "/series?page={page}",
            searchPath = "/search?q={q}",
            cardSelectors = listOf(
                "a[href*='/%D9%81%D9%8A%D9%84%D9%85-']",
                "a[href*='/%d9%81%d9%8a%d9%84%d9%85-']",
                "a[href*='/%D9%85%D8%B3%D9%84%D8%B3%D9%84-']",
                "a[href*='/%d9%85%d8%b3%d9%84%d8%b3%d9%84-']",
                ".postDiv a", "div.list-movie a", "a.list-title",
                "a[href*='/movie/']", "a[href*='/serie/']", "article a",
            ),
            titleSelectors = listOf(".list-title", ".post__title", ".h1", "h2", "h3", "img[alt]", "[title]"),
            isPrimary = true,
        ),

        // ── 6. إيجي بست (EgyBest.live disabled — fake CPA survey wall) ──
        ProviderConfig(
            id = "egybest",
            name = "إيجي بست",
            baseUrl = "https://egybest.live",
            mirrorDomains = emptyList(),
            moviesPath = "/browse.php?type=movie&page={page}&lang=ar",
            seriesPath = "/browse.php?type=series&page={page}&lang=ar",
            searchPath = "/search.php?q={q}&lang=ar",
            cardSelectors = listOf("a.movie-card"),
            titleSelectors = listOf("h2", "h3", ".title"),
            enabled = false,
            isPrimary = false,
        ),

        // ── 6b. إيجي بست الأصلي (iEgyBest Classic & New Archive) ─────────
        ProviderConfig(
            id = "iegybest",
            name = "إيجي بست الأصلي",
            baseUrl = "https://iegybest.cimawbas.tv",
            mirrorDomains = emptyList(),
            moviesPath = "/movies.php?page={page}",
            seriesPath = "/all-series.php?page={page}",
            searchPath = "/search.php?keywords={q}",
            cardSelectors = listOf(
                "a[href*='watch.php?vid=']",
                "a[href*='category.php?cat=']",
                ".pm-video-thumb a",
                "article a",
            ),
            titleSelectors = listOf("h3", "h2", "img[alt]", "[title]"),
            isPrimary = true,
        ),

        // ── 7. توب سينما (TopCinema - أفلام ومسلسلات أجنبي وآسيوي وأنمي) ─
        ProviderConfig(
            id = "topcinema",
            name = "توب سينما",
            baseUrl = "https://topcinemaa.tv",
            mirrorDomains = listOf("https://topcinema.cam"),
            moviesPath = "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a-8/page/{page}/",
            seriesPath = "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(".Small--Box a", ".Block--Item a", "article a"),
            titleSelectors = listOf("h3.title", "h3", "strong.hasyear", "[title]", "img[alt]"),
            isPrimary = true,
        ),

        // ── 8. شاهد فور يو (Shahid4U - shahid4u.bid / shahid4u.food) ───
        ProviderConfig(
            id = "shahid4u",
            name = "شاهد فور يو",
            baseUrl = "https://shahid4u.bid",
            mirrorDomains = listOf("https://shahid4u.food"),
            moviesPath = "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/page/{page}/",
            seriesPath = "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "a[href*='%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-']",
                ".MovieItem a", "article.MovieItem a",
                ".GridItem a", "article a",
                "a[href*='/movie/']", "a[href*='/series/']",
            ),
            titleSelectors = listOf("h3", "h2", ".Title", "strong", "[title]", "img[alt]"),
            enabled = true,
            isPrimary = true,
        ),

        // ── 8b. شاهد فور يو VIP (Shahid4U.si - direct HLS + multi-server API) ──
        ProviderConfig(
            id = "shahid4u_si",
            name = "شاهد فور يو VIP",
            baseUrl = "https://www.shahid4u.si",
            mirrorDomains = emptyList(),
            moviesPath = "/movies/?page={page}",
            seriesPath = "/series/?page={page}",
            searchPath = "/search?q={q}",
            cardSelectors = listOf(
                "div.grid a.pc",
                "a.pc[href*='/series/'], a.pc[href*='/movie/']",
                "a[href*='/series/'], a[href*='/movie/']",
                "article a",
            ),
            titleSelectors = listOf("h3", "h2", ".title", "[title]", "img[alt]"),
            enabled = true,
            isPrimary = true,
        ),

        // ── 9. سيما فور يو (Cima4U) ────────────────────────────────────
        ProviderConfig(
            id = "cima4u",
            name = "سيما فور يو",
            baseUrl = "https://c4u.cam",
            mirrorDomains = listOf("https://cima4u.watch", "https://cima4u4k.site"),
            moviesPath = "/films?page={page}",
            seriesPath = "/seriestv?page={page}",
            searchPath = "/search?q={q}",
            cardSelectors = listOf(
                "a.br-card-link", "article.br-card a",
                ".MovieBlock a", ".Block--Movie a",
                "article.MovieBlock a", "a[href*='/watch/']",
                ".movies-list a", "article a",
            ),
            titleSelectors = listOf("h3", "h2", ".Title", "img[alt]", "[title]"),
            isPrimary = true,
        ),

        // ── 10. فشار (Fushaar - live mirror fushaar.cam) ─────────────────
        ProviderConfig(
            id = "fushaar",
            name = "فشار",
            baseUrl = "https://fushaar.cam",
            mirrorDomains = emptyList(),
            moviesPath = "/films?page={page}",
            seriesPath = "/seriestv?page={page}",
            searchPath = "/search?q={q}",
            cardSelectors = listOf(
                "a.br-card-link", "article.poster a", "article.movie a", "article a", ".poster a",
            ),
            titleSelectors = listOf("h3", "h2", ".title", "img[alt]", "[title]"),
        ),

        // ── 11. عرب ليونز (ArabLionz) ───────────────────────────────────
        ProviderConfig(
            id = "arablionz",
            name = "عرب ليونز",
            baseUrl = "https://arablionz.cam",
            mirrorDomains = emptyList(),
            moviesPath = "/films?page={page}",
            seriesPath = "/seriestv?page={page}",
            searchPath = "/search?q={q}",
            cardSelectors = listOf(
                "a.tt-card-link", "article.tt-card a",
                "article a", ".MovieBlock a", "h2.entry-title a", "a[href*='/movie/']",
            ),
            titleSelectors = listOf("h2", "h3", ".Title", ".entry-title", "img[alt]", "[title]"),
            isPrimary = true,
        ),

        // ── 12. لودي نت (Lodynet - كوري وتركي وهندي وصيني قديم وجديد) ─────
        ProviderConfig(
            id = "lodynet",
            name = "لودي نت",
            baseUrl = "https://lodynet.watch",
            mirrorDomains = emptyList(),
            moviesPath = "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9-a/page/{page}/",
            seriesPath = "/korean-series-b/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                ".ItemNewly a", ".SuggestionsItems",
                "a[href*='movie-']", "a[href*='video-']", "a[href*='episode-']",
                "article.bt a", ".content-box article a", ".post-item a", "article a",
            ),
            titleSelectors = listOf(".NewlyTitle", ".SuggestionsTitle", "[title]", "h2", "h3", ".entry-title", "img[alt]"),
            isPrimary = true,
        ),

        // ── آرا دراما (Aradrama - كوري وأسيوي أساسي) ──────────────────
        ProviderConfig(
            id = "aradrama",
            name = "آرا دراما",
            baseUrl = "https://aradramatv.cc",
            mirrorDomains = listOf("https://aradramatv.net"),
            moviesPath = "/category/%d8%a7%d9%84%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%84%d8%a2%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/page/{page}/",
            seriesPath = "/category/serie/korea/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article a", ".post-item a", "div.post a", "article.post a", "h2.entry-title a",
            ),
            titleSelectors = listOf("h2.entry-title", "h2", "h3", ".title", "[title]", "img[alt]"),
            isPrimary = true,
        ),

        // ── اسيا تو تي في (Asia2tv - موقع مغلق يتطلب اشتراك/تسجيل دخول) ────────────────
        ProviderConfig(
            id = "asia2tv",
            name = "اسيا تو تي في",
            baseUrl = "https://asia2tv.com",
            mirrorDomains = listOf("https://asia2tv.com"),
            moviesPath = "/catalog?country=legacy-term-125&page={page}",
            seriesPath = "/catalog?country=legacy-term-125&page={page}",
            searchPath = "/catalog?search={q}",
            cardSelectors = listOf(
                "a[href*='/movie/']", "a[href*='/series/']", "a[href*='/drama/']",
                ".movie-item a", ".post-item a", "article a",
            ),
            titleSelectors = listOf("h2", "h3", ".title", "[title]"),
            enabled = false,
            isPrimary = false,
        ),

        // ── قصة عشق (Esheeq - تركي أساسي قديم وجديد) ────────────────────
        ProviderConfig(
            id = "esheeq",
            name = "قصة عشق",
            baseUrl = "https://qesset.net",
            mirrorDomains = listOf("https://qeseh.net", "https://3sk.tv", "https://3sk.biz"),
            moviesPath = "/category/yeni-filmler/page/{page}/",
            seriesPath = "/discover/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article a", "a[href*='/clarus/']", "article a[href*='shot90']",
                "div.thumb a", ".video-card a",
            ),
            titleSelectors = listOf("h2", "h3", ".title", ".entry-title", "[title]", "img[alt]"),
            isPrimary = true,
        ),

        // ── موفيز لاند (Movizland - POST view=1 supported) ─────────────
        ProviderConfig(
            id = "movizland",
            name = "موفيز لاند",
            baseUrl = "https://movizland.life",
            mirrorDomains = emptyList(),
            moviesPath = "/category/movies/page/{page}/",
            seriesPath = "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d9%88%d9%86-%d9%84%d8%a7%d9%8a%d9%86/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "article.MovieItem a", ".MovieItem a",
                ".Block--Item a", "article a",
                "a[href*='/movie/']", "a[href*='/series/']",
            ),
            titleSelectors = listOf("h3", "h2", ".Title", "strong.hasyear", "[title]", "img[alt]"),
            enabled = true,
            isPrimary = false,
        ),

        // ── بريستيج (Prestige - عربي ومصري وشامي وخليجي وتركي وآسيوي 2021-2026) ─
        ProviderConfig(
            id = "prestige",
            name = "بريستيج",
            baseUrl = "https://hd1.brstej.com",
            mirrorDomains = listOf("https://prstej.asia", "https://prstej.lat", "https://brstej.com"),
            moviesPath = "/cat03.php?cat=aflam02-2024&page={page}",
            seriesPath = "/cat03.php?cat=arab8-2025&page={page}",
            searchPath = "/search.php?keywords={q}",
            cardSelectors = listOf(
                "a.pcg-series-card",
                "article.pmc-card a.pmc-photo",
                "article.prs-card a.prs-image",
                ".pgh-card a", ".pgh-series-card a", ".ptoday-card a",
                ".movie__block a", "article a", "a[href*='video.php']", "a[href*='watch.php']",
            ),
            titleSelectors = listOf("[aria-label]", "[title]", ".pmc-title", ".prs-title", ".pgh-card-title", ".pgh-series-name", "h2", "h3", "img[alt]"),
            isPrimary = true,
        ),

        // ── سيما كلوب (CimaClub - cimaclub.live + cimaclub1.ink) ───────
        ProviderConfig(
            id = "cimaclub",
            name = "سيما كلوب",
            baseUrl = "https://cimaclub.live",
            mirrorDomains = listOf("https://cimaclub1.ink", "https://cimaclub.living"),
            moviesPath = "/category/movies/page/{page}/",
            seriesPath = "/category/series/page/{page}/",
            searchPath = "/search?q={q}",
            cardSelectors = listOf(
                "a.card", "article.MovieItem a", ".MovieItem a",
                "div.GridItem a", "article a",
                "h3.Title a", "a[href*='/watch/']", "a[href*='watch.php?vid=']",
            ),
            titleSelectors = listOf("h3.Title", "h3", "h2", ".Title", "[title]", "img[alt]"),
            enabled = true,
            isPrimary = true,
        ),

        // ── إيجي ديد (EgyDead) ─────────────────────────────────────────
        ProviderConfig(
            id = "egydead",
            name = "إيجي ديد",
            baseUrl = "https://egydead.blog",
            mirrorDomains = listOf("https://egidead.cam", "https://egydead.surf"),
            moviesPath = "/movies/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf(
                "li.movieItem a", "article.post-item a",
                "div.movie-card a", "a.box", "article a",
            ),
            titleSelectors = listOf("h2.title", "span.movie-title", "h2", "h3", "[title]", "img[alt]"),
        ),
        // ── سيما ناو / ماي سيد نايم (CimaNow via myseed.name) ──────────
        ProviderConfig(
            id = "cimanow",
            name = "سيما ناو",
            baseUrl = "https://myseed.name",
            mirrorDomains = emptyList(),
            moviesPath = "/category/%D8%A7%D9%81%D9%84%D8%A7%D9%85-%D8%A7%D8%AC%D9%86%D8%A8%D9%8A/page/{page}/",
            seriesPath = "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A7%D8%AC%D9%86%D8%A8%D9%8A/page/{page}/",
            searchPath = "/search?s={q}",
            cardSelectors = listOf(
                "a.show-card", "a[href*='/film/']", "a[href*='/episode/']", "a[href*='/series/']",
                "a[href*='film-']", "div.card a", "article a", "div.movie-box a",
            ),
            titleSelectors = listOf("h3.card-title", "div.title", "h3", "h2", "[title]", "img[alt]"),
            enabled = true,
            isPrimary = true,
        ),
        // ── سيما وبس (CimaWbas.rest - PHPMelody .php routes) ───────────
        ProviderConfig(
            id = "cimawbas",
            name = "سيما وبس",
            baseUrl = "https://cimawbas.rest",
            mirrorDomains = emptyList(),
            moviesPath = "/movies.php?page={page}",
            seriesPath = "/all-series.php?page={page}",
            searchPath = "/search.php?keywords={q}",
            cardSelectors = listOf(
                "a[href*='watch.php?vid=']", "a[href*='category.php?cat=']",
                ".pm-video-thumb a", "li.pm-li-video a", "div.pm-li-category a",
                "a[href*='film-']", "article a",
            ),
            titleSelectors = listOf("h3 a", "span.pm-video-title", "h3", "h2", "[title]", "img[alt]"),
            enabled = true,
            isPrimary = true,
        ),
        // ── سيما فور أب (disabled — metadata-only JustWatch clone, no video player) ─
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
            enabled = false,
            isPrimary = false,
        ),
        // ── موفيز فور يو (disabled 404 / non-Arabic) ───────────────────
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
            enabled = false,
        ),
        // ── موفي واتشر (disabled — metadata-only TMDB clone, no video player) ─
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
            enabled = false,
            isPrimary = false,
        ),
        // ── سبيس موف (disabled 404) ────────────────────────────────────
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
            enabled = false,
        ),
        // ── موفي فليكس (disabled empty title / SPA block) ──────────────
        ProviderConfig(
            id = "moviflex",
            name = "موفي فليكس",
            baseUrl = "https://moviflex.net",
            mirrorDomains = emptyList(),
            moviesPath = "/movie/page/{page}/",
            seriesPath = "/series/page/{page}/",
            searchPath = "/?s={q}",
            cardSelectors = listOf("div.movie-item a.film-poster", "div.flw-item a.film-poster", "a.film-poster"),
            titleSelectors = listOf("h3.film-name a", "h3", "h2", "[title]"),
            enabled = false,
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
            || pageUrl.contains("egybest") || pageUrl.contains("iegybest") || pageUrl.contains("cimawbas")
            || pageUrl.contains("lodynet") || pageUrl.contains("topcinema") || pageUrl.contains("aradrama")
            || pageUrl.contains("qesset") || pageUrl.contains("qeseh") || pageUrl.contains("q-ask") || pageUrl.contains("3sk")
            || pageUrl.contains("brstej") || pageUrl.contains("prstej")
            || pageUrl.contains("c4u.cam") || pageUrl.contains("cima4u")
            || pageUrl.contains("fushaar")
            || pageUrl.contains("arablionz")
            || pageUrl.contains("egidead") || pageUrl.contains("egydead")
            || pageUrl.contains("mycima") || pageUrl.contains("my-cima")
            || pageUrl.contains("aflam") || pageUrl.contains("cima")
    }

    private val providerContentHostRoots: List<String> by lazy {
        PROVIDERS.flatMap { provider -> listOf(provider.baseUrl) + provider.mirrorDomains }
            .mapNotNull { domain -> runCatching { java.net.URI(domain).host }.getOrNull() } + listOf(
            "myseed.tv", "wecima.casa", "fasel4k.site", "topcinemaa.tv",
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
            if (value.startsWith("aHR0", ignoreCase = false)) {
                runCatching {
                    String(android.util.Base64.decode(value, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
                }.getOrNull()?.takeIf { it.startsWith("http") }?.let { value = it }
            }
            value
        }
        if (candidate.contains("shot90.com", ignoreCase = true)) {
            AdBypassHelper.decodeShot90Url(candidate)?.takeIf { isProviderContentUrl(it, referencePageUrl) }?.let { return it }
        }
        return values.firstOrNull { isProviderContentUrl(it, referencePageUrl) }
    }



    internal fun providerIdForUrl(url: String): String? = getProviderForUrl(url)?.id

    private fun getProviderForUrl(url: String): ProviderConfig? {
        return PROVIDERS.firstOrNull { p ->
            (listOf(p.baseUrl) + p.mirrorDomains).any { hostMatches(url, it) }
        } ?: when {
            url.contains("arabseed.here") -> PROVIDERS.firstOrNull { it.id == "arabseed_here" }
            url.contains("myseed") || url.contains("arabseed") -> PROVIDERS.firstOrNull { it.id == "arabseed" }
            url.contains("akw") -> PROVIDERS.firstOrNull { it.id == "akwam" }

            url.contains("wecima") || url.contains("myweecima") || url.contains("mycima") -> PROVIDERS.firstOrNull { it.id == "wecima" }

            url.contains("fasel") -> PROVIDERS.firstOrNull { it.id == "faselhd" }

            url.contains("iegybest") || url.contains("cimawbas") -> PROVIDERS.firstOrNull { it.id == "iegybest" }
            url.contains("egybest") || url.contains("ivbestt") -> PROVIDERS.firstOrNull { it.id == "egybest" }
            url.contains("topcinema") -> PROVIDERS.firstOrNull { it.id == "topcinema" }
            url.contains("lodynet") -> PROVIDERS.firstOrNull { it.id == "lodynet" }

            url.contains("brstej") || url.contains("prstej") -> PROVIDERS.firstOrNull { it.id == "prestige" }

            url.contains("aradrama") -> PROVIDERS.firstOrNull { it.id == "aradrama" }
            url.contains("asia2tv") -> PROVIDERS.firstOrNull { it.id == "asia2tv" }
            url.contains("qesset") || url.contains("qeseh") || url.contains("q-ask") || url.contains("3sk") -> PROVIDERS.firstOrNull { it.id == "esheeq" }

            url.contains("c4u") -> PROVIDERS.firstOrNull { it.id == "cima4u" }

            url.contains("cimaclub") -> PROVIDERS.firstOrNull { it.id == "cimaclub" }

            url.contains("shahid4u.si") -> PROVIDERS.firstOrNull { it.id == "shahid4u_si" }

            url.contains("shahid4u") -> PROVIDERS.firstOrNull { it.id == "shahid4u" }

            url.contains("movizland") -> PROVIDERS.firstOrNull { it.id == "movizland" }
            url.contains("fushaar") -> PROVIDERS.firstOrNull { it.id == "fushaar" }
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

    private fun fetchHtmlAndFinalUrl(url: String, referer: String = ""): Pair<String, String> {
        var finalUrl = url
        val body = fetchHtmlWithClient(
            url,
            referer,
            http,
            onFinalUrl = { redirected -> finalUrl = redirected },
        )
        return body to finalUrl
    }

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
        val isSearchRequest = path.contains("search", ignoreCase = true) ||
            path.contains("?s=", ignoreCase = true) ||
            path.contains("?q=", ignoreCase = true) ||
            path.contains("keywords=", ignoreCase = true)
        for (base in bases) {
            val root = base.trimEnd('/')
            var effectiveBase = root
            var finalUrl = "$root$path"
            var statusCode = 0
            val html = fetchHtmlWithClient(
                url = "$root$path",
                referer = "$root/",
                client = providerPageHttp,
                onFinalUrl = { redirectedUrl ->
                    finalUrl = redirectedUrl
                    effectiveBase = pageOrigin(redirectedUrl, root)
                },
                onHttpStatus = { code -> statusCode = code },
            )
            if (html.isBlank()) {
                when {
                    statusCode == 403 -> coolDownProviderRoute(provider, root, PROVIDER_BAD_ROUTE_COOLDOWN_MS)
                    statusCode == 404 && isSearchRequest -> return emptyList()
                    else -> coolDownProviderRoute(provider, root, PROVIDER_TRANSIENT_COOLDOWN_MS)
                }
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
            val doc = try { Jsoup.parse(html, effectiveBase) } catch (_: Exception) { null }
            if (doc != null && isSearchEmptyResultPage(html, path, finalUrl, doc)) {
                android.util.Log.i(TAG, "Search returned 0 results for provider=${provider.id} path=$path")
                return emptyList()
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
        onHttpStatus: ((Int) -> Unit)? = null,
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
                onHttpStatus?.invoke(r.code)
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
                val rawCandidateUrl = when {
                    rawHref.startsWith("//") -> "https:$rawHref"
                    rawHref.startsWith("http") -> rawHref
                    rawHref.startsWith("/") -> "${pageBaseUrl.trimEnd('/')}$rawHref"
                    rawHref.isNotBlank() -> "${pageBaseUrl.trimEnd('/')}/$rawHref"
                    else -> ""
                }
                val href = if (rawCandidateUrl.contains("shot90.com", ignoreCase = true)) {
                    AdBypassHelper.decodeShot90Url(rawCandidateUrl) ?: rawCandidateUrl
                } else {
                    rawCandidateUrl
                }
                if (href.isBlank() || !href.startsWith("http")) continue

                val decodedHrefLower = runCatching {
                    java.net.URLDecoder.decode(href, "UTF-8").lowercase(java.util.Locale.ROOT)
                }.getOrDefault(href.lowercase(java.util.Locale.ROOT))

                val isLodynetSeriesCard = provider.id == "lodynet" && decodedHrefLower.contains("/category/مسلسل-")
                val isIegybestSeriesCard = provider.id == "iegybest" && decodedHrefLower.contains("category.php?cat=") && !decodedHrefLower.contains("aflam-") && !decodedHrefLower.contains("mslslat") && !decodedHrefLower.contains("top-250")
                if (!isLodynetSeriesCard && !isIegybestSeriesCard &&
                    (href.contains("/category") || href.contains("/tag/") || href.contains("/page/") || href.contains("category.php") || href.contains("topvideos") || href.contains("favicons"))
                ) continue



                // Title
                var title = ""
                for (ts in provider.titleSelectors) {
                    val matched = when {
                        ts == "[aria-label]" -> el.attr("aria-label").ifBlank { el.selectFirst("[aria-label]")?.attr("aria-label").orEmpty() }
                        ts == "[title]" -> el.attr("title").ifBlank { el.selectFirst("[title]")?.attr("title").orEmpty() }
                        ts == "img[alt]" -> el.selectFirst("img")?.attr("alt").orEmpty()
                        else -> el.selectFirst(ts)?.text()?.trim().orEmpty().ifBlank { el.parent()?.selectFirst(ts)?.text()?.trim().orEmpty() }
                    }.trim()
                    if (matched.isNotBlank()) {
                        title = matched
                        break
                    }
                }
                if (title.isBlank()) title = el.attr("aria-label").trim()
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

                fun normalizePosterUrl(raw: String): String? {
                    val clean = raw.trim()
                    if (clean.isBlank() || clean.startsWith("data:", ignoreCase = true)) return null
                    return when {
                        clean.startsWith("//") -> "https:$clean"
                        clean.startsWith("http") -> clean
                        clean.startsWith("/") -> "${pageBaseUrl.trimEnd('/')}$clean"
                        else -> "${pageBaseUrl.trimEnd('/')}/$clean"
                    }
                }

                // 1. Try img attributes (data-src, src, etc.)
                for (attr in provider.posterAttr) {
                    val raw = img?.attr(attr) ?: continue
                    poster = normalizePosterUrl(raw)
                    if (poster != null) break
                }

                // 2. Try <picture><source srcset="..."> & abs:src fallback
                if (poster == null) {
                    val srcset = el.selectFirst("picture source[srcset], source[srcset]")?.attr("srcset")
                        ?.substringBefore(",")?.trim()?.substringBefore(" ")
                    poster = srcset?.let(::normalizePosterUrl)
                        ?: img?.attr("abs:src")?.let(::normalizePosterUrl)
                }

                // 3. Try background-image in style attribute (WeCima, Akwam)
                if (poster == null) {
                    val styleEl = el.selectFirst("[style*='background-image']")
                        ?: el.selectFirst(".br-thumb, .ak-poster, .imgSer, .poster, .pm-video-thumb")
                    val style = styleEl?.attr("style") ?: ""
                    val bgMatch = Regex("""url\(['"]?((?:https?:)?//[^'")\s]+)['"]?\)""").find(style)
                    if (bgMatch != null) poster = normalizePosterUrl(bgMatch.groupValues[1])
                }

                // 4. Try any child element's background-image style
                if (poster == null) {
                    for (child in el.allElements) {
                        val style = child.attr("style")
                        if (style.contains("background-image")) {
                            val bgMatch = Regex("""url\(['"]?((?:https?:)?//[^'")\s]+)['"]?\)""").find(style)
                            if (bgMatch != null) { poster = normalizePosterUrl(bgMatch.groupValues[1]); break }
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
            return if (page <= 1) "/discover/" else "/discover/page/$page/"
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
            CategoryFeed("esheeq", "/discover/", "/discover/page/{page}/"),
            CategoryFeed("esheeq", "/category/alarshif/", "/category/alarshif/page/{page}/"),
            CategoryFeed("esheeq", "/son-bolumler/", "/son-bolumler/page/{page}/"),
            CategoryFeed("lodynet", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%aa%d8%b1%d9%83%d9%8a/", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%aa%d8%b1%d9%83%d9%8a/page/{page}/"),
            CategoryFeed("lodynet", "/dubbed-turkish-series-g/", "/dubbed-turkish-series-g/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=ty9-2025", "/cat03.php?cat=ty9-2025&page={page}"),
            CategoryFeed("akwam", "/category/مسلسلات-تركي", "/category/مسلسلات-تركي?page={page}"),
            CategoryFeed("arablionz", "/category/مسلسلات-تركي", "/category/مسلسلات-تركي/page/{page}"),
            CategoryFeed("cima4u", "/category/مسلسلات-تركي", "/category/مسلسلات-تركي/page/{page}"),
            CategoryFeed("egydead", "/category/مسلسلات-تركية/", "/category/مسلسلات-تركية/page/{page}/"),
        )
        "turkish_movies" -> listOf(
            CategoryFeed("esheeq", "/category/yeni-filmler/", "/category/yeni-filmler/page/{page}/"),
            CategoryFeed("lodynet", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%aa%d8%b1%d9%83%d9%8a%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%aa%d8%b1%d9%83%d9%8a%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=turkish3-movies2024", "/cat03.php?cat=turkish3-movies2024&page={page}"),
            CategoryFeed("akwam", "/category/افلام-تركي", "/category/افلام-تركي?page={page}"),
            CategoryFeed("arablionz", "/category/افلام-تركي", "/category/افلام-تركي/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-تركي", "/category/افلام-تركي/page/{page}"),
            CategoryFeed("egydead", "/category/افلام-تركية/", "/category/افلام-تركية/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-turki/"),
        )
        "foreign_movies" -> listOf(
            CategoryFeed("akwam", "/category/افلام-اجنبي", "/category/افلام-اجنبي?page={page}"),
            CategoryFeed("faselhd", "/movies", "/movies?page={page}"),
            CategoryFeed("faselhd", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A3%D8%AC%D9%86%D8%A8%D9%8A%D8%A9/", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A3%D8%AC%D9%86%D8%A8%D9%8A%D8%A9/page/{page}/"),
            CategoryFeed("topcinema", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a-8/", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a-8/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=aflamajnby3-2024", "/cat03.php?cat=aflamajnby3-2024&page={page}"),
            CategoryFeed("arablionz", "/category/افلام-اجنبي", "/category/افلام-اجنبي/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-اجنبي", "/category/افلام-اجنبي/page/{page}"),
            CategoryFeed("iegybest", "/category.php?cat=aflam-ajnbe", "/category.php?cat=aflam-ajnbe&page={page}"),
            CategoryFeed("egydead", "/category/افلام-اجنبي/", "/category/افلام-اجنبي/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-ajnabi/"),
        )
        "arabic_movies" -> listOf(
            CategoryFeed("prestige", "/cat03.php?cat=aflam02-2024", "/cat03.php?cat=aflam02-2024&page={page}"),
            CategoryFeed("iegybest", "/category.php?cat=aflam-arbe", "/category.php?cat=aflam-arbe&page={page}"),
            CategoryFeed("akwam", "/category/افلام-عربي", "/category/افلام-عربي?page={page}"),
            CategoryFeed("arablionz", "/category/افلام-عربي", "/category/افلام-عربي/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-عربي", "/category/افلام-عربي/page/{page}"),
            CategoryFeed("egydead", "/category/افلام-عربي/", "/category/افلام-عربي/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-arabi/"),
        )
        "arabic_series" -> listOf(
            CategoryFeed("prestige", "/cat03.php?cat=arab8-2025", "/cat03.php?cat=arab8-2025&page={page}"),
            CategoryFeed("prestige", "/cat03.php?cat=eg8-2025", "/cat03.php?cat=eg8-2025&page={page}"),
            CategoryFeed("prestige", "/cat03.php?cat=syy5-2025", "/cat03.php?cat=syy5-2025&page={page}"),
            CategoryFeed("prestige", "/cat03.php?cat=5a7-2024", "/cat03.php?cat=5a7-2024&page={page}"),
            CategoryFeed("prestige", "/cat03.php?cat=prss7-2025", "/cat03.php?cat=prss7-2025&page={page}"),
            CategoryFeed("prestige", "/cat03.php?cat=rm42-2022", "/cat03.php?cat=rm42-2022&page={page}"),
            CategoryFeed("wecima", "/arabic-series/", "/arabic-series/page/{page}/"),
            CategoryFeed("akwam", "/category/مسلسلات-عربية", "/category/مسلسلات-عربية?page={page}"),
            CategoryFeed("arablionz", "/category/مسلسلات-عربية", "/category/مسلسلات-عربية/page/{page}"),
            CategoryFeed("cima4u", "/category/مسلسلات-عربية", "/category/مسلسلات-عربية/page/{page}"),
            CategoryFeed("iegybest", "/category.php?cat=mslslat-arbe", "/category.php?cat=mslslat-arbe&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-عربي/", "/category/مسلسلات-عربي/page/{page}/"),
        )
        "foreign_series" -> listOf(
            CategoryFeed("faselhd", "/series", "/series?page={page}"),
            CategoryFeed("faselhd", "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A3%D8%AC%D9%86%D8%A8%D9%8A%D8%A9/", "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A3%D8%AC%D9%86%D8%A8%D9%8A%D8%A9/page/{page}/"),
            CategoryFeed("topcinema", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/page/{page}/"),
            CategoryFeed("akwam", "/category/مسلسلات-أجنبي", "/category/مسلسلات-أجنبي?page={page}"),
            CategoryFeed("arablionz", "/category/مسلسلات-أجنبي", "/category/مسلسلات-أجنبي/page/{page}"),
            CategoryFeed("cima4u", "/category/مسلسلات-أجنبي", "/category/مسلسلات-أجنبي/page/{page}"),
            CategoryFeed("prestige", "/cat03.php?cat=english1-2025", "/cat03.php?cat=english1-2025&page={page}"),
            CategoryFeed("iegybest", "/category.php?cat=mslslat-ajnbeh", "/category.php?cat=mslslat-ajnbeh&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-اجنبي/", "/category/مسلسلات-اجنبي/page/{page}/"),
            CategoryFeed("moviewatcher", "/tv-shows/page/{page}/"),
        )
        "asian_series" -> listOf(
            CategoryFeed("aradrama", "/category/serie/korea/", "/category/serie/korea/page/{page}/"),
            CategoryFeed("aradrama", "/category/serie/", "/category/serie/page/{page}/"),
            CategoryFeed("aradrama", "/category/recent-completed/", "/category/recent-completed/page/{page}/"),
            CategoryFeed("aradrama", "/category/%d8%af%d8%b1%d8%a7%d9%85%d8%a7-%d8%aa%d9%85-%d8%a7%d8%b9%d8%a7%d8%af%d8%a9-%d8%b1%d9%81%d8%b9%d9%87%d8%a7/", "/category/%d8%af%d8%b1%d8%a7%d9%85%d8%a7-%d8%aa%d9%85-%d8%a7%d8%b9%d8%a7%d8%af%d8%a9-%d8%b1%d9%81%d8%b9%d9%87%d8%a7/page/{page}/"),
            CategoryFeed("lodynet", "/korean-series-b/", "/korean-series-b/page/{page}/"),
            CategoryFeed("lodynet", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b5%d9%8a%d9%86%d9%8a%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85%d8%a9/", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b5%d9%8a%d9%86%d9%8a%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85%d8%a9/page/{page}/"),
            CategoryFeed("faselhd", "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A2%D8%B3%D9%8A%D9%88%D9%8A%D8%A9/", "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A2%D8%B3%D9%8A%D9%88%D9%8A%D8%A9/page/{page}/"),
            CategoryFeed("topcinema", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/", "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=asia", "/cat03.php?cat=asia&page={page}"),
            CategoryFeed("akwam", "/category/مسلسلات-أسيوي", "/category/مسلسلات-أسيوي?page={page}"),
            CategoryFeed("arablionz", "/category/مسلسلات-أسيوي", "/category/مسلسلات-أسيوي/page/{page}"),
            CategoryFeed("cima4u", "/category/مسلسلات-أسيوي", "/category/مسلسلات-أسيوي/page/{page}"),
            CategoryFeed("egydead", "/category/مسلسلات-اسيوية/", "/category/مسلسلات-اسيوية/page/{page}/"),
        )
        "indian_series" -> listOf(
            CategoryFeed("lodynet", "/b%d8%a7%d9%84%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d9%87%d9%86%d8%af%d9%8a%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85%d8%a9/", "/b%d8%a7%d9%84%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d9%87%d9%86%d8%af%d9%8a%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85%d8%a9/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=2ind2-2025", "/cat03.php?cat=2ind2-2025&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-هندية/", "/category/مسلسلات-هندية/page/{page}/"),
        )
        "dubbed_series" -> listOf(
            CategoryFeed("lodynet", "/dubbed-turkish-series-g/", "/dubbed-turkish-series-g/page/{page}/"),
            CategoryFeed("arablionz", "/category/مسلسلات-تركية-مدبلجة", "/category/مسلسلات-تركية-مدبلجة/page/{page}"),
            CategoryFeed("cima4u", "/category/مسلسلات-تركية-مدبلجة", "/category/مسلسلات-تركية-مدبلجة/page/{page}"),
            CategoryFeed("egydead", "/category/مسلسلات-مدبلجة/", "/category/مسلسلات-مدبلجة/page/{page}/"),
        )
        "anime_series" -> listOf(
            CategoryFeed("faselhd", "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A3%D9%86%D9%85%D9%8A/", "/category/%D9%85%D8%B3%D9%84%D8%B3%D9%84%D8%A7%D8%AA-%D8%A3%D9%86%D9%85%D9%8A/page/{page}/"),
            CategoryFeed("topcinema", "/category/anime-series/", "/category/anime-series/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=anmei", "/cat03.php?cat=anmei&page={page}"),
            CategoryFeed("akwam", "/category/مسلسلات-الأنميات", "/category/مسلسلات-الأنميات?page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-انمي/", "/category/مسلسلات-انمي/page/{page}/"),
        )
        "dubbed_movies" -> listOf(
            CategoryFeed("akwam", "/category/افلام-مدبلجة", "/category/افلام-مدبلجة?page={page}"),
            CategoryFeed("arablionz", "/category/افلام-مدبلجة", "/category/افلام-مدبلجة/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-مدبلجة", "/category/افلام-مدبلجة/page/{page}"),
            CategoryFeed("egydead", "/category/افلام-مدبلجة/", "/category/افلام-مدبلجة/page/{page}/"),
        )
        "asian_movies" -> listOf(
            CategoryFeed("aradrama", "/category/%d8%a7%d9%84%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%84%d8%a2%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/", "/category/%d8%a7%d9%84%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%84%d8%a2%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/page/{page}/"),
            CategoryFeed("lodynet", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9-a/", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9-a/page/{page}/"),
            CategoryFeed("faselhd", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A2%D8%B3%D9%8A%D9%88%D9%8A%D8%A9/", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A2%D8%B3%D9%8A%D9%88%D9%8A%D8%A9/page/{page}/"),
            CategoryFeed("topcinema", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a/", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a/page/{page}/"),
            CategoryFeed("akwam", "/category/افلام-اسيوي", "/category/افلام-اسيوي?page={page}"),
            CategoryFeed("arablionz", "/category/افلام-اسيوي", "/category/افلام-اسيوي/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-اسيوي", "/category/افلام-اسيوي/page/{page}"),
            CategoryFeed("egydead", "/category/افلام-اسيوية/", "/category/افلام-اسيوية/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-asiawiya/"),
        )
        "indian_movies" -> listOf(
            CategoryFeed("lodynet", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d9%87%d9%86%d8%af%d9%8a%d8%a9/", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d9%87%d9%86%d8%af%d9%8a%d8%a9/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=hindi1-moviess", "/cat03.php?cat=hindi1-moviess&page={page}"),
            CategoryFeed("iegybest", "/category.php?cat=aflam-hnde", "/category.php?cat=aflam-hnde&page={page}"),
            CategoryFeed("akwam", "/category/افلام-هندي", "/category/افلام-هندي?page={page}"),
            CategoryFeed("arablionz", "/category/افلام-هندي", "/category/افلام-هندي/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-هندي", "/category/افلام-هندي/page/{page}"),
            CategoryFeed("egydead", "/category/افلام-هندي/", "/category/افلام-هندي/page/{page}/"),
            CategoryFeed("cima4up", "/collections/aflam-hindiya/"),
        )
        "animation_movies" -> listOf(
            CategoryFeed("faselhd", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A3%D9%86%D9%85%D9%8A/", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A3%D9%86%D9%85%D9%8A/page/{page}/"),
            CategoryFeed("topcinema", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%86%d9%85%d9%8a-2/", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%86%d9%85%d9%8a-2/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=anime1", "/cat03.php?cat=anime1&page={page}"),
            CategoryFeed("akwam", "/category/افلام-انمي", "/category/افلام-انمي?page={page}"),
            CategoryFeed("arablionz", "/category/افلام-كرتون", "/category/افلام-كرتون/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-كرتون", "/category/افلام-كرتون/page/{page}"),
        )
        "anime_movies" -> listOf(
            CategoryFeed("faselhd", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A3%D9%86%D9%85%D9%8A/", "/category/%D8%A3%D9%81%D9%84%D8%A7%D9%85-%D8%A3%D9%86%D9%85%D9%8A/page/{page}/"),
            CategoryFeed("topcinema", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%86%d9%85%d9%8a-2/", "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%86%d9%85%d9%8a-2/page/{page}/"),
            CategoryFeed("prestige", "/cat03.php?cat=anime1", "/cat03.php?cat=anime1&page={page}"),
            CategoryFeed("akwam", "/category/افلام-انمي", "/category/افلام-انمي?page={page}"),
            CategoryFeed("arablionz", "/category/افلام-انمي", "/category/افلام-انمي/page/{page}"),
            CategoryFeed("cima4u", "/category/افلام-انمي", "/category/افلام-انمي/page/{page}"),
        )
        "ramadan_2026" -> listOf(
            CategoryFeed("prestige", "/cat03.php?cat=ramdan2026", "/cat03.php?cat=ramdan2026&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-رمضان-2026/", "/category/مسلسلات-رمضان-2026/page/{page}/"),
        )
        "ramadan_2025" -> listOf(
            CategoryFeed("prestige", "/cat03.php?cat=ramadan2-2025", "/cat03.php?cat=ramadan2-2025&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-رمضان-2025/", "/category/مسلسلات-رمضان-2025/page/{page}/"),
        )
        "ramadan_2024" -> listOf(
            CategoryFeed("prestige", "/cat03.php?cat=ramdan1-2024", "/cat03.php?cat=ramdan1-2024&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-رمضان-2024/", "/category/مسلسلات-رمضان-2024/page/{page}/"),
        )
        "ramadan_2023" -> listOf(
            CategoryFeed("prestige", "/cat03.php?cat=ramda1-2023", "/cat03.php?cat=ramda1-2023&page={page}"),
            CategoryFeed("prestige", "/cat03.php?cat=rm42-2022", "/cat03.php?cat=rm42-2022&page={page}"),
            CategoryFeed("egydead", "/category/مسلسلات-رمضان-2023/", "/category/مسلسلات-رمضان-2023/page/{page}/"),
        )
        else -> emptyList()
    }

    private fun categoryCacheKey(categoryId: String, page: Int): String = "category:v7:$categoryId:$page"

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
    private fun normalizeSearchQueryPunctuation(value: String): String = value
        .replace('\u2018', '\'')
        .replace('\u2019', '\'')
        .replace('\u201A', '\'')
        .replace('\u201B', '\'')
        .replace('\u201C', '"')
        .replace('\u201D', '"')
        .replace('\u2013', '-')
        .replace('\u2014', '-')
        .trim()

    fun searchAll(query: String, forceRefresh: Boolean = false): List<ArabseedItem> {
        val q = normalizeSearchQueryPunctuation(query)
        if (q.isBlank()) return emptyList()

        val key = "search:v9:${q.lowercase()}"
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

        val normalizedArabic = SeriesCatalogParser.normalizeArabicSearchQuery(q)
        val queryToRun = q
        val tasks = PROVIDERS.filter { it.enabled }.map { provider ->
            {
                try {
                    val encodedQuery = java.net.URLEncoder.encode(queryToRun, "UTF-8")
                    val basePath = provider.searchPath.replace("{q}", encodedQuery)
                    var rows = fetchProviderListing(provider, basePath, ArabseedType.MOVIE)
                    if (rows.isEmpty() && normalizedArabic.isNotBlank() && normalizedArabic != queryToRun) {
                        val encNorm = java.net.URLEncoder.encode(normalizedArabic, "UTF-8")
                        rows = fetchProviderListing(provider, provider.searchPath.replace("{q}", encNorm), ArabseedType.MOVIE)
                    }
                    rows
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

    private fun tryDecodeBase64HttpUrl(candidate: String): String? {
        val raw = candidate.trim()
        if (raw.length < 12) return null
        for (variant in listOf(raw, raw.reversed())) {
            val cleaned = variant.replace('-', '+').replace('_', '/')
            val padded = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
            val decoded = runCatching {
                String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
            }.getOrNull() ?: continue
            if (decoded.startsWith("http://", true) || decoded.startsWith("https://", true)) {
                return decoded
            }
        }
        return null
    }

    /** Accept plain public embed URLs, reversed-Base64 values, wrapper links, and iframe markup stored in data attributes. */
    private fun serverUrlFromAttribute(rawValue: String, baseUrl: String): String {
        val decoded = Parser.unescapeEntities(rawValue.trim(), false)
            .replace("\\/", "/")
            .replace("\\\"", "\"")
        if (decoded.isBlank()) return ""
        val trimmed = decoded.trim().trim('"', '\'')
        tryDecodeBase64HttpUrl(trimmed)?.let { b64Url ->
            val unwrapped = decodeIdWrapperUrl(b64Url)
            if (isServerCandidateUrl(unwrapped)) return unwrapped
        }
        val unwrappedDirect = decodeIdWrapperUrl(trimmed)
        val direct = absoluteServerUrl(unwrappedDirect, baseUrl)
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
        val candidate = decodeIdWrapperUrl(absoluteServerUrl(embedded, baseUrl))
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

    private fun isServerCandidateUrl(url: String, pageUrl: String? = null): Boolean {
        val clean = url.trim()
        val uri = runCatching { java.net.URI(clean) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(java.util.Locale.ROOT).orEmpty()
        val path = uri.path.orEmpty().lowercase(java.util.Locale.ROOT)
        if (uri.scheme !in setOf("http", "https") || host.isBlank() || isAdServerUrl(clean)) return false
        val deadOrScamHosts = listOf(
            "egybest.live", "shahid4u.cam", "shahid4u.tv", "shahid4u.to",
            "cimanow.cc", "cimanow.info", "cimanow.net", "movies4u.icu", "spacemov.xyz",
            "moviflex.net", "shot90.com", "cima4up-official.cyou",
            "moviewatcher-offcial.cyou", "asia2tv.com", "mycima.help",
            "govid.live", "luluvdo.com", "vidplayer.live", "shahied4u.vidplayer.live",
        )
        if (deadOrScamHosts.any { host == it || host.endsWith(".$it") }) return false
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

        // Informational content pages (movies, series, dramas) on provider sites must not be treated as video servers
        // unless they explicitly have a playback marker (/watch, /servers, /embed, /player, vids.php, etc.)
        val hasPlaybackMarker = path.contains("/watch") || path.contains("/servers") ||
            path.contains("/embed") || path.contains("/player") || path.contains("/play") ||
            path.contains("/video") || path.contains("/v/") || path.contains("/e/") ||
            path.endsWith(".mp4") || path.endsWith(".m3u8") ||
            path.contains("embed.php") || path.contains("play.php") || path.contains("downloads.php") ||
            clean.contains("?watch=") || clean.contains("?embed=") || clean.contains("?play=") ||
            clean.contains("&watch=") || clean.contains("&embed=") || clean.contains("&play=") ||
            clean.contains("vid=") || clean.contains("vids.php") || clean.contains("video_player")
        val isContentDetailRoute = path.startsWith("/movie/") || path.startsWith("/drama/") ||
            path.startsWith("/series/") || path.startsWith("/selary/") || path.startsWith("/post/") ||
            path.startsWith("/show/") || path.startsWith("/title/") || path.startsWith("/film/") ||
            path.startsWith("/episode/")
        if ((isContentDetailRoute || getProviderForUrl(clean) != null) && !hasPlaybackMarker) {
            return false
        }

        return true
    }

    private fun insideNonPlaybackContainer(element: org.jsoup.nodes.Element): Boolean {
        if (element.hasAttr("data-profile-share") || element.hasClass("share") ||
            element.attr("aria-label").contains("مشاركة") || element.attr("aria-label").contains("نسخ")
        ) return true
        val markers = (listOf(element) + element.parents()).take(6)
            .flatMap { listOf(it.id(), it.className(), it.tagName()) }
            .joinToString(" ")
        val contentNavigation = Regex(
            "(?i)(^|[\\s_-])(" +
                "season[-_ ]?(tabs|list|nav|selector)|episode[-_ ]?(list|grid|tabs|nav|pagination)|" +
                "episodes[-_ ]?(list|grid|nav)|eplist|eps-list|series[-_ ]?(list|grid|navigation)|" +
                "part[-_ ]?(list|tabs)|poster|thumbnail|thumb|trailer|teaser|related|recommend|" +
                "suggest|advert|banner|social|share|profile-share|nav|navbar|menu|header|footer|" +
                "sidebar|login|auth|account|plans|pricing|comment|review|report)([\\s_-]|$)",
        )
        return contentNavigation.containsMatchIn(markers)
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
            "wecima" -> {
                val parsed = pageUrl.toHttpUrlOrNull()
                if (parsed != null && parsed.queryParameter("do") != "watch") {
                    variants += parsed.newBuilder().setQueryParameter("do", "watch").build().toString()
                }
            }
            "cimanow", "arabseed_here" -> {
                val slug = pageUrl.trimEnd('/').substringAfterLast('/')
                val origin = pageOrigin(pageUrl, provider.baseUrl).trimEnd('/')
                if (slug.isNotBlank() && !pageUrl.contains("/watch")) {
                    variants += "$origin/watch/$slug"
                }
            }
            "shahid4u", "cimaclub", "topcinema" -> {
                val trimmed = pageUrl.trimEnd('/')
                if (!trimmed.endsWith("/watch")) variants += "$trimmed/watch/"
            }
            "arablionz", "fushaar", "cima4u" -> {
                val parsed = pageUrl.trim().toHttpUrlOrNull()
                if (parsed != null) {
                    val segs = parsed.pathSegments.filter { it.isNotBlank() }
                    if (segs.size == 1) {
                        val root = "${parsed.scheme}://${parsed.host}"
                        variants += "$root/watch/${segs[0]}/servers"
                    }
                }
                val trimmed = pageUrl.trimEnd('/')
                if (!trimmed.endsWith("/servers") && !trimmed.endsWith("/watch")) {
                    variants += "$trimmed/watch"
                }
            }
            "prestige", "iegybest", "cimawbas" -> {
                val vid = Regex("""vid=([A-Za-z0-9]+)""").find(pageUrl)?.groupValues?.getOrNull(1).orEmpty()
                if (vid.isNotBlank()) {
                    val origin = pageOrigin(pageUrl, provider.baseUrl).trimEnd('/')
                    variants += "$origin/play.php?vid=$vid"
                    variants += "$origin/embed.php?vid=$vid"
                }
            }
        }
        variants += pageUrl
        return variants.distinct()
    }

    private fun extractServerAttr(el: org.jsoup.nodes.Element): String = listOf(
        "data-embed-url", "data-embed", "data-watch", "data-src", "data-url",
        "data-link", "data-video", "data-srcout", "abs:src", "src", "abs:href", "href"
    ).firstNotNullOfOrNull { attr ->
        el.attr(attr).trim().takeIf(String::isNotBlank)
    }.orEmpty()

    private fun extractCookiesHeader(headers: List<String>): String =
        headers.mapNotNull { h -> h.substringBefore(';').trim().takeIf { it.contains('=') } }
            .distinct()
            .joinToString("; ")

    fun fetchServers(pageUrl: String): List<ArabseedServer> {

        val servers = mutableListOf<ArabseedServer>()

        val provider = getProviderForUrl(pageUrl)

        val baseUrl = pageOrigin(pageUrl, provider?.baseUrl.orEmpty())



        try {

            // 0. MySeed.name / ArabSeed.here dedicated Laravel /watch/resolve extractor (with session cookies)
            if (pageUrl.contains("arabseed.here") || pageUrl.contains("myseed.name")) {
                val origin = pageOrigin(pageUrl, if (pageUrl.contains("myseed.name")) "https://myseed.name" else "https://arabseed.here").trimEnd('/')
                val slug = pageUrl.trimEnd('/').substringAfterLast('/')
                val watchUrl = if (pageUrl.contains("/watch")) pageUrl else "$origin/watch/$slug"
                try {
                    val getReq = Request.Builder().url(watchUrl)
                        .header("User-Agent", UA).header("Referer", pageUrl).build()
                    var wHtml = ""
                    var cookieHeader = ""
                    http.newCall(getReq).execute().use { res ->
                        cookieHeader = extractCookiesHeader(res.headers("Set-Cookie"))
                        wHtml = res.body?.string().orEmpty()
                    }
                    val keyMatch = Regex("""(?:const|let|var)\s+watchResolveKey\s*=\s*["']([^"']+)["']""").find(wHtml)
                    val csrfMatch = Regex("""(?:const|let|var)\s+csrfToken\s*=\s*["']([^"']+)["']|name=["']csrf-token["']\s+content=["']([^"']+)["']""").find(wHtml)
                    val serversJsonMatch = Regex("""(?:const|let|var)\s+servers\s*=\s*(\[.*?\]);""", RegexOption.DOT_MATCHES_ALL).find(wHtml)
                    if (keyMatch != null && csrfMatch != null && serversJsonMatch != null) {
                        val watchKey = keyMatch.groupValues[1]
                        val csrfToken = csrfMatch.groupValues[1].ifBlank { csrfMatch.groupValues[2] }
                        val serversArray = org.json.JSONArray(serversJsonMatch.groupValues[1])
                        val futures = (0 until serversArray.length().coerceAtMost(6)).map { i ->
                            val serverObj = serversArray.getJSONObject(i)
                            val token = serverObj.optString("resolve_token")
                            val sName = serverObj.optString("name", "سيرفر ${i + 1}")
                            pool.submit<Pair<String, String>?> {
                                if (token.isBlank()) return@submit null
                                runCatching {
                                    val postJson = JSONObject().apply {
                                        put("key", watchKey)
                                        put("token", token)
                                        put("human_signal", JSONObject.NULL)
                                    }
                                    val reqB = Request.Builder()
                                        .url("$origin/watch/resolve")
                                        .post(postJson.toString().toRequestBody("application/json".toMediaType()))
                                        .header("User-Agent", UA)
                                        .header("Referer", watchUrl)
                                        .header("Origin", origin)
                                        .header("Accept", "application/json")
                                        .header("X-CSRF-TOKEN", csrfToken)
                                        .header("X-Requested-With", "XMLHttpRequest")
                                    if (cookieHeader.isNotBlank()) reqB.header("Cookie", cookieHeader)
                                    http.newCall(reqB.build()).execute().use { res ->
                                        val resJson = JSONObject(res.body?.string().orEmpty())
                                        if (!resJson.optBoolean("ok", false)) return@submit null
                                        val mPath = resJson.optString("url")
                                        val mFull = absoluteServerUrl(mPath, origin)
                                        val mReqB = Request.Builder().url(mFull)
                                            .header("User-Agent", UA).header("Referer", watchUrl)
                                        if (cookieHeader.isNotBlank()) mReqB.header("Cookie", cookieHeader)
                                        val mHtml = http.newCall(mReqB.build()).execute().use { it.body?.string().orEmpty() }
                                        val ifrSrc = Regex("""<iframe\b[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                                            .find(mHtml)?.groupValues?.get(1)?.trim().orEmpty()
                                        val finalEmbed = absoluteServerUrl(ifrSrc.ifBlank { mFull }, origin)
                                        if (isServerCandidateUrl(finalEmbed)) sName to finalEmbed else null
                                    }
                                }.getOrNull()
                            }
                        }
                        for (f in futures) {
                            val pair = runCatching { f.get(4, TimeUnit.SECONDS) }.getOrNull() ?: continue
                            if (servers.none { it.embedUrl == pair.second }) {
                                servers += ArabseedServer("سيرفر ${servers.size + 1} (${pair.first})", pair.second)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 0b. Aradrama dedicated server extractor
            if (pageUrl.contains("aradrama")) {
                try {
                    val html = fetchHtml(pageUrl, baseUrl)
                    val doc = Jsoup.parse(html, pageUrl)
                    for (a in doc.select("a[href], [data-embed], [data-url], [data-src], iframe[src], li[data-url]")) {
                        val href = a.attr("abs:src").ifBlank { a.attr("data-embed").ifBlank { a.attr("data-url").ifBlank { a.attr("data-src").ifBlank { a.attr("abs:href") } } } }.trim()
                        val text = a.text().trim()
                        if (isServerCandidateUrl(href, pageUrl)) {
                            if (servers.none { it.embedUrl == href }) {
                                val sName = text.ifBlank { "سيرفر ${servers.size + 1}" }
                                servers += ArabseedServer("آرا دراما - $sName", href)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 0c. Esheeq (قصة عشق) dedicated server extractor
            if (pageUrl.contains("qesset") || pageUrl.contains("qeseh") || pageUrl.contains("3sk") || pageUrl.contains("qesen")) {
                try {
                    val html = fetchHtml(pageUrl, baseUrl)
                    val doc = Jsoup.parse(html, pageUrl)
                    val watchLink = doc.selectFirst("a[href*='watch?post='], a[href*='post=']")?.attr("href")?.trim().orEmpty()
                    val targetUrl = watchLink.ifBlank { pageUrl }

                    val postParam = targetUrl.substringAfter("post=", "").substringBefore("&")
                    if (postParam.isNotBlank()) {
                        try {
                            val jsonStr = String(android.util.Base64.decode(postParam, android.util.Base64.DEFAULT), Charsets.UTF_8)
                            val jsonObj = org.json.JSONObject(jsonStr)
                            val sArray = jsonObj.optJSONArray("servers")
                            if (sArray != null) {
                                for (i in 0 until sArray.length()) {
                                    val sObj = sArray.getJSONObject(i)
                                    val sName = sObj.optString("name", "سيرفر ${i + 1}")
                                    val sId = sObj.optString("id")
                                    val embedUrl = when {
                                        sId.startsWith("http") -> sId
                                        sName.contains("ok", ignoreCase = true) -> "https://ok.ru/videoembed/$sId"
                                        sName.contains("Arab", ignoreCase = true) -> "https://arabhd.onl/embed-$sId.html"
                                        sName.contains("turk", ignoreCase = true) || sName.contains("estream", ignoreCase = true) -> "https://vidmoly.to/embed-$sId.html"
                                        else -> ""
                                    }
                                    if (embedUrl.isNotBlank() && isServerCandidateUrl(embedUrl, pageUrl) && servers.none { it.embedUrl == embedUrl }) {
                                        servers += ArabseedServer("قصة عشق - $sName", embedUrl)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    if (watchLink.isNotBlank()) {
                        val wHtml = fetchHtml(watchLink, pageUrl)
                        val wDoc = Jsoup.parse(wHtml, watchLink)
                        for (ifr in wDoc.select("iframe[src]")) {
                            val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()
                            if (isServerCandidateUrl(src, pageUrl) && servers.none { it.embedUrl == src }) {
                                servers += ArabseedServer("قصة عشق - سيرفر أساسي", src)
                            }
                        }
                        for (li in wDoc.select("[data-server]")) {
                            val sServer = li.attr("data-server").trim()
                            val sName = li.attr("data-name").ifBlank { li.text().trim() }
                            val embedUrl = when {
                                sServer.startsWith("http") -> sServer
                                sName.contains("ok", ignoreCase = true) -> "https://ok.ru/videoembed/$sServer"
                                sName.contains("Arab", ignoreCase = true) -> "https://arabhd.onl/embed-$sServer.html"
                                else -> ""
                            }
                            if (embedUrl.isNotBlank() && isServerCandidateUrl(embedUrl, pageUrl) && servers.none { it.embedUrl == embedUrl }) {
                                servers += ArabseedServer("قصة عشق - $sName", embedUrl)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 0d. Lodynet Base64 JSON extractor (PostData.ServersWatch + PageData tokens)
            if (pageUrl.contains("lodynet")) {
                try {
                    val lHtml = fetchHtml(pageUrl, baseUrl.ifBlank { "https://lodynet.watch/" })
                    val tokenPlus1 = Regex(""""TokenPlus1"\s*:\s*"([^"]*)"""").find(lHtml)?.groupValues?.get(1).orEmpty()
                    val tokenVidlo = Regex(""""TokenVidlo"\s*:\s*"([^"]*)"""").find(lHtml)?.groupValues?.get(1).orEmpty()
                    val jsonArrMatch = Regex("""ServersWatch\s*:\s*(\[\{.*?\}\])""", RegexOption.DOT_MATCHES_ALL).find(lHtml)
                    if (jsonArrMatch != null) {
                        val sArray = org.json.JSONArray(jsonArrMatch.groupValues[1])
                        for (i in 0 until sArray.length()) {
                            val obj = sArray.getJSONObject(i)
                            val sName = obj.optString("Name", "سيرفر ${i + 1}").trim()
                            val sid = obj.optInt("Id", -1)
                            val b64 = obj.optString("Embed").trim()
                            if (b64.isBlank()) continue
                            val decodedBase = runCatching {
                                String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
                            }.getOrNull().orEmpty()
                            val withToken = when {
                                sid == 73 && tokenPlus1.isNotBlank() && !decodedBase.contains("st=") -> decodedBase + tokenPlus1
                                sid == 116413 && tokenVidlo.isNotBlank() && !decodedBase.contains("st=") -> decodedBase + tokenVidlo
                                else -> decodedBase
                            }
                            val resolvedEmbed = absoluteServerUrl(withToken, pageUrl)
                            if (resolvedEmbed.startsWith("http") && isServerCandidateUrl(resolvedEmbed, pageUrl) &&
                                servers.none { it.embedUrl == resolvedEmbed }
                            ) {
                                val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""
                                servers += ArabseedServer("لودي نت$label", resolvedEmbed)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 0f. Shahid4U.si API extractor (/api/play/movie/{id} and /api/play/tv/{id}/{s}/{e})
            if (pageUrl.contains("shahid4u.si")) {
                try {
                    val siOrigin = pageOrigin(pageUrl, "https://www.shahid4u.si").trimEnd('/')
                    val tvMatch = Regex("""/series/[^/]*-(\d+)/season-(\d+)/episode-(\d+)""").find(pageUrl)
                    val movMatch = Regex("""/movie/[^/]*-(\d+)""").find(pageUrl)
                    val watchPageMatch = Regex("""/watch/[^/]*-(\d+)""").find(pageUrl)
                    val playApiPath = when {
                        tvMatch != null -> "/api/play/tv/${tvMatch.groupValues[1]}/${tvMatch.groupValues[2]}/${tvMatch.groupValues[3]}"
                        movMatch != null -> "/api/play/movie/${movMatch.groupValues[1]}"
                        watchPageMatch != null -> "/api/play/movie/${watchPageMatch.groupValues[1]}"
                        else -> ""
                    }
                    if (playApiPath.isNotBlank()) {
                        val apiBody = fetchHtml("$siOrigin$playApiPath", pageUrl)
                        val lines = JSONObject(apiBody).optJSONArray("lines")
                        if (lines != null) {
                            for (i in 0 until lines.length()) {
                                val line = lines.getJSONObject(i)
                                val sName = line.optString("name", "سيرفر ${i + 1}").trim()
                                var rawU = line.optString("url").trim()
                                if (rawU.startsWith("/api/hls/")) {
                                    val hlsMeta = fetchHtml("$siOrigin$rawU", pageUrl)
                                    val streamPath = runCatching { JSONObject(hlsMeta).optString("url") }.getOrDefault("")
                                    if (streamPath.isNotBlank()) rawU = "$siOrigin$streamPath"
                                }
                                val abs = absoluteServerUrl(rawU, siOrigin)
                                if (isServerCandidateUrl(abs, pageUrl) && servers.none { it.embedUrl == abs }) {
                                    servers += ArabseedServer("شاهد فور يو VIP ($sName)", abs)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 0g. Movizland POST view=1 server extractor
            if (pageUrl.contains("movizland")) {
                try {
                    val postReq = Request.Builder().url(pageUrl)
                        .post(FormBody.Builder().add("view", "1").build())
                        .header("User-Agent", UA).header("Referer", pageUrl).build()
                    val mHtml = http.newCall(postReq).execute().use { it.body?.string().orEmpty() }
                    if (mHtml.isNotBlank() && !AdBypassHelper.isCloudflareChallenge(mHtml)) {
                        val mDoc = Jsoup.parse(mHtml, pageUrl)
                        for (el in mDoc.select("iframe[src], [data-src], [data-embed], [data-link], [data-srcout]")) {
                            if (insideNonPlaybackContainer(el)) continue
                            val abs = serverUrlFromAttribute(extractServerAttr(el), pageUrl)
                            if (isServerCandidateUrl(abs, pageUrl) && servers.none { it.embedUrl == abs }) {
                                servers += ArabseedServer("موفيز لاند - سيرفر ${servers.size + 1}", abs)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 0h. EgyDead / Egidead POST watch=1 extractor
            if ((pageUrl.contains("egidead") || pageUrl.contains("egydead")) && servers.isEmpty()) {
                try {
                    val postReq = Request.Builder().url(pageUrl)
                        .post(FormBody.Builder().add("watch", "1").build())
                        .header("User-Agent", UA).header("Referer", pageUrl).build()
                    val eHtml = http.newCall(postReq).execute().use { it.body?.string().orEmpty() }
                    if (eHtml.isNotBlank() && !AdBypassHelper.isCloudflareChallenge(eHtml)) {
                        val eDoc = Jsoup.parse(eHtml, pageUrl)
                        for (el in eDoc.select("ul.serversList li[data-link], [data-watch], [data-link], [data-embed], iframe[src]")) {
                            if (insideNonPlaybackContainer(el)) continue
                            val abs = serverUrlFromAttribute(extractServerAttr(el), pageUrl)
                            if (isServerCandidateUrl(abs, pageUrl) && servers.none { it.embedUrl == abs }) {
                                servers += ArabseedServer("إيجي ديد - سيرفر ${servers.size + 1}", abs)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 0e. TopCinema AJAX Server extractor (/watch/ -> Ajaxat/Single/Server.php)
            if (pageUrl.contains("topcinema")) {
                try {
                    val watchUrl = if (pageUrl.trimEnd('/').endsWith("/watch")) pageUrl else "${pageUrl.trimEnd('/')}/watch/"
                    val tcHtml = fetchHtml(watchUrl, pageUrl)
                    val tcDoc = Jsoup.parse(tcHtml, watchUrl)
                    for (ifr in tcDoc.select("iframe[src]")) {
                        val src = absoluteServerUrl(ifr.attr("abs:src").ifBlank { ifr.attr("src") }, watchUrl)
                        if (isServerCandidateUrl(src, watchUrl) && servers.none { it.embedUrl == src }) {
                            servers += ArabseedServer("توب سينما - سيرفر 1", src)
                        }
                    }
                    val ajaxBase = Regex("""MyAjaxURL\s*=\s*["']([^"']+)["']""").find(tcHtml)?.groupValues?.get(1)
                        ?: "${baseUrl.trimEnd('/')}/wp-content/themes/movies2023/Ajaxat/"
                    val items = tcDoc.select("li.server--item[data-id][data-server], li[data-watch]")
                    val futures = items.take(7).map { li ->
                        val pid = li.attr("data-id").ifBlank { li.attr("data-watch") }.trim()
                        val srv = li.attr("data-server").ifBlank { "0" }.trim()
                        val sName = li.text().trim()
                        pool.submit<Pair<String, String>?> {
                            if (pid.isBlank()) return@submit null
                            try {
                                val form = FormBody.Builder().add("id", pid).add("i", srv).build()
                                val req = Request.Builder()
                                    .url("${ajaxBase.trimEnd('/')}/Single/Server.php")
                                    .post(form)
                                    .header("User-Agent", UA)
                                    .header("Referer", watchUrl)
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .build()
                                val rHtml = http.newCall(req).execute().use { it.body?.string() ?: "" }
                                val ifr = Jsoup.parse(rHtml).selectFirst("iframe[src]")?.let {
                                    it.attr("abs:src").ifBlank { it.attr("src") }
                                }?.trim().orEmpty()
                                val absIfr = absoluteServerUrl(ifr, watchUrl)
                                if (isServerCandidateUrl(absIfr, watchUrl)) sName to absIfr else null
                            } catch (_: Exception) { null }
                        }
                    }
                    for (f in futures) {
                        val pair = runCatching { f.get(3, TimeUnit.SECONDS) }.getOrNull() ?: continue
                        if (servers.none { it.embedUrl == pair.second }) {
                            val label = if (pair.first.isNotBlank() && pair.first.length < 25) " (${pair.first})" else ""
                            servers += ArabseedServer("توب سينما$label", pair.second)
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1. Akwam Servers (watch/.../servers and download/...)
            if (pageUrl.contains("akw")) {
                val watchUrl = pageUrl
                    .replace("/akfilm-", "/watch/akfilm-")
                    .replace("/akdrama-", "/watch/akdrama-")
                    .replace("/akseries-", "/watch/akseries-")
                    .trimEnd('/') + "/servers"

                val watchReferer = pageOrigin(watchUrl, "https://akw.cam")
                val watchHtml = fetchHtml(watchUrl, "$watchReferer/")

                val wDoc = Jsoup.parse(watchHtml, watchUrl)
                if (isHomePageOrListingPage(wDoc, watchUrl)) {
                    android.util.Log.w(TAG, "Rejected Akwam home page in fetchServers for: $watchUrl")
                } else {
                    val primaryIframe = wDoc.select("iframe[src]")
                        .firstOrNull { !insideNonPlaybackContainer(it) }
                        ?.attr("abs:src")?.trim().orEmpty()

                    if (isServerCandidateUrl(primaryIframe)) {
                        servers += ArabseedServer("سيرفر 1 (فائق السرعة)", primaryIframe)
                    }

                    for (el in wDoc.select("[data-url], [data-src], a.server, .watchServers a, iframe[src]")) {
                        if (insideNonPlaybackContainer(el)) continue
                        val rawUrl = el.attr("data-url").ifBlank { el.attr("data-src").ifBlank { el.attr("src").ifBlank { el.attr("href") } } }.trim()
                        val sUrl = absoluteServerUrl(rawUrl, watchUrl)
                        val sName = el.text().trim().replace(Regex("(?i)(أكوام|akwam)"), "").trim()
                        if (isServerCandidateUrl(sUrl) && !isSeriesNavigationLink(el, sUrl) && servers.none { it.embedUrl == sUrl }) {
                            val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""
                            servers += ArabseedServer("سيرفر ${servers.size + 1}$label", sUrl)
                        }
                    }
                }

                // 1b. Akwam Download Page: scrape alternative & direct download host servers (mixdrop, streamruby, lulustream, etc.)
                val dlUrl = pageUrl
                    .replace("/watch/", "/")
                    .replace("/akfilm-", "/download/akfilm-")
                    .replace("/akdrama-", "/download/akdrama-")
                    .replace("/akseries-", "/download/akseries-")
                    .replace(Regex("/servers/?$"), "")
                    .trimEnd('/')
                if (dlUrl != pageUrl || pageUrl.contains("/download/")) {
                    try {
                        val dlHtml = fetchHtml(dlUrl, "$watchReferer/")
                        val dlDoc = Jsoup.parse(dlHtml, dlUrl)
                        for (a in dlDoc.select("a[href]")) {
                            val rawHref = a.attr("abs:href").trim()
                            val parentText = a.parent()?.text().orEmpty()
                            val sName = parentText.replace("تحميل", "").trim()
                            val isKnownHost = rawHref.contains("mixdrop") || rawHref.contains("streamruby") ||
                                rawHref.contains("lulustream") || rawHref.contains("vikingfile") ||
                                rawHref.contains("dood") || rawHref.contains("filemoon") ||
                                rawHref.contains("streamtape") || rawHref.contains("vidmoly") ||
                                rawHref.endsWith(".mp4") || rawHref.endsWith(".m3u8")
                            if (isKnownHost && isServerCandidateUrl(rawHref)) {
                                val embedUrl = when {
                                    rawHref.contains("mixdrop") -> rawHref.replace("/f/", "/e/")
                                    rawHref.contains("streamruby") -> rawHref.replace("/d/", "/e/")
                                    rawHref.contains("lulustream") -> rawHref.replace("/d/", "/e/")
                                    else -> rawHref
                                }
                                if (servers.none { it.embedUrl == embedUrl || it.embedUrl == rawHref }) {
                                    val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""
                                    servers += ArabseedServer("سيرفر ${servers.size + 1}$label", embedUrl)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }



            // 2. WeCima Servers (?do=watch -> /embed/{id}/ OR legacy embed.php?vid=...)

            if (pageUrl.contains("wecima") || pageUrl.contains("myweecima")) {
                val origin = baseUrl.trimEnd('/').ifBlank { "https://wecima.cc" }
                val vid = Regex("vid=([a-zA-Z0-9]+)").find(pageUrl)?.groupValues?.get(1) ?: ""

                if (vid.isNotBlank()) {
                    val embedUrl = "$origin/embed.php?vid=$vid"
                    if (isServerCandidateUrl(embedUrl)) {
                        servers += ArabseedServer("سيرفر 1 (فائق السرعة)", embedUrl)
                    }
                    try {
                        val downloadPage = "$origin/downloads.php?vid=$vid"
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
                } else {
                    try {
                        val watchUrl = if (pageUrl.contains("?do=watch")) pageUrl else {
                            if (pageUrl.contains("?")) "$pageUrl&do=watch" else "${pageUrl.trimEnd('/')}/?do=watch"
                        }
                        val wHtml = fetchHtml(watchUrl, pageUrl)
                        val wDoc = Jsoup.parse(wHtml, watchUrl)
                        for (ifr in wDoc.select("iframe[src], iframe[data-src]")) {
                            var src = ifr.attr("abs:src").ifBlank { ifr.attr("abs:data-src").ifBlank { ifr.attr("src") } }.trim()
                            if (src.contains("wecima", ignoreCase = true) && src.contains("/embed/")) {
                                val innerHtml = fetchHtml(src, watchUrl)
                                val innerIfr = Jsoup.parse(innerHtml, src).selectFirst("iframe[src], iframe[data-src]")
                                    ?.let { it.attr("abs:src").ifBlank { it.attr("src") } }?.trim().orEmpty()
                                if (innerIfr.isNotBlank()) src = innerIfr
                            }
                            if (isServerCandidateUrl(src, pageUrl) && servers.none { it.embedUrl == src }) {
                                servers += ArabseedServer("وي سيما - سيرفر ${servers.size + 1}", src)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            // 3. FaselHD servers (.tabs-ul li[onclick*='video_player'], iframe[data-src], or /ajax/embed)

            if (pageUrl.contains("fasel")) {
                var fHtml = ""
                var finalFaselUrl = pageUrl
                var faselCookies = ""
                runCatching {
                    val fReq = Request.Builder().url(pageUrl)
                        .header("User-Agent", UA).header("Referer", "$baseUrl/").build()
                    http.newCall(fReq).execute().use { res ->
                        finalFaselUrl = res.request.url.toString()
                        faselCookies = extractCookiesHeader(res.headers("Set-Cookie"))
                        fHtml = res.body?.string().orEmpty()
                    }
                }
                if (fHtml.isBlank()) {
                    val pair = fetchHtmlAndFinalUrl(pageUrl, "$baseUrl/")
                    fHtml = pair.first
                    finalFaselUrl = pair.second
                }
                val faselBase = pageOrigin(finalFaselUrl, baseUrl).trimEnd('/')
                val fDoc = Jsoup.parse(fHtml, finalFaselUrl)

                // Modern faselhd.cc player_iframe + tabs-ul onclick='player_iframe.location.href = ...'
                for (tab in fDoc.select(".tabs-ul li[onclick], ul.tabs-ul li[onclick], [onclick*='video_player']")) {
                    val onclick = tab.attr("onclick")
                    val vpUrl = Regex("""['"](https?://[^'"]*video_player[^'"]*)['"]""").find(onclick)?.groupValues?.get(1).orEmpty()
                    val sName = tab.text().trim().replace(Regex("(?i)(فاصل|fasel|سيرفر المشاهدة)"), "").trim()
                    if (vpUrl.isNotBlank() && servers.none { it.embedUrl == vpUrl }) {
                        val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""
                        servers += ArabseedServer("فاصل إعلاني$label", vpUrl)
                    }
                }
                for (ifr in fDoc.select("iframe[name='player_iframe'], iframe[data-src*='video_player']")) {
                    val vpUrl = ifr.attr("abs:src").ifBlank { ifr.attr("abs:data-src").ifBlank { ifr.attr("data-src") } }.trim()
                    if (vpUrl.isNotBlank() && isServerCandidateUrl(vpUrl, finalFaselUrl) && servers.none { it.embedUrl == vpUrl }) {
                        servers += ArabseedServer("فاصل إعلاني - أساسي", vpUrl)
                    }
                }

                // AJAX embed buttons (POST {faselBase}/ajax/embed with id)
                val embedButtons = fDoc.select("button.btn-service[data-embed], [data-embed]")
                val futures = mutableListOf<Future<Pair<String, String>?>>()
                for (btn in embedButtons.take(6)) {
                    val embedId = btn.attr("data-embed").trim()
                    val sName = btn.text().trim().replace(Regex("(?i)(فاصل|fasel)"), "").trim()
                    if (embedId.isNotBlank()) {
                        futures += pool.submit<Pair<String, String>?> {
                            try {
                                val formBody = FormBody.Builder().add("id", embedId).build()
                                val postBuilder = Request.Builder()
                                    .url("$faselBase/ajax/embed")
                                    .post(formBody)
                                    .header("User-Agent", UA)
                                    .header("Referer", finalFaselUrl)
                                    .header("Origin", faselBase)
                                    .header("X-Requested-With", "XMLHttpRequest")
                                if (faselCookies.isNotBlank()) postBuilder.header("Cookie", faselCookies)
                                val respHtml = http.newCall(postBuilder.build()).execute().use { it.body?.string() ?: "" }
                                val ifr = Jsoup.parse(respHtml).selectFirst("iframe")?.attr("src")?.trim() ?: ""
                                val absoluteIframe = absoluteServerUrl(ifr, finalFaselUrl)
                                if (isServerCandidateUrl(absoluteIframe, finalFaselUrl)) sName to absoluteIframe else null
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
                    if (isServerCandidateUrl(resolvedSrc, sourcePage) && !isSeriesNavigationLink(element, resolvedSrc) &&
                        servers.none { it.embedUrl == resolvedSrc }
                    ) {
                        servers += ArabseedServer("سيرفر ${servers.size + 1}", resolvedSrc)
                    }
                }

                // 4b. Common server lists used by the configured public providers.
                // Constrain plain anchors to player/server containers so navigation
                // links from the page are never mistaken for playable sources.
                val serverElements = doc.select(
                    "[data-embed-url], [data-embed], [data-watch], [data-src], [data-link], [data-url], [data-video], [data-srcout], " +
                        ".servers a, a.server-btn, .watch-buttons a, .ep-sv a, .episode-sv a, " +
                        ".server-item, .server-block a, li.server a, .tab-pane a[href], " +
                        "#serversList li, #serversList a[href], .list_servers li, .list_servers a[href], " +
                        ".download-servers-list a[href], .donwload-servers-list a[href], #playeroptionsul a[href]",
                )
                for (element in serverElements) {
                    val link = extractServerAttr(element)
                    val absoluteLink = serverUrlFromAttribute(link, sourcePage)
                    if (!isServerCandidateUrl(absoluteLink, sourcePage) || isSeriesNavigationLink(element, absoluteLink) ||
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
                                if (isServerCandidateUrl(absIfr, sourcePage) && servers.none { it.embedUrl == absIfr }) {
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
                            if (isServerCandidateUrl(url, sourcePage) && servers.none { it.embedUrl == url }) {
                                servers += ArabseedServer("سيرفر ${servers.size + 1}", url)
                            }
                        }
                        if (servers.isNotEmpty()) break
                    }
                }
                if (servers.isNotEmpty()) break

                // 4d. If page still has no servers, follow explicit /servers or /watch subpage link
                val watchSubLink = doc.selectFirst("a[href*='/servers'], a[href*='/watch/'], a[href*='do=watch'], a.btn-watch")
                    ?.attr("abs:href")?.trim().orEmpty()
                if (watchSubLink.isNotBlank() && watchSubLink != sourcePage && watchSubLink != pageUrl &&
                    isConfiguredProviderHostOrSame(provider, watchSubLink, pageUrl)
                ) {
                    val subHtml = fetchHtml(watchSubLink, sourcePage)
                    if (subHtml.isNotBlank() && !AdBypassHelper.isCloudflareChallenge(subHtml)) {
                        val subDoc = Jsoup.parse(subHtml, watchSubLink)
                        for (el in subDoc.select("iframe[src], [data-embed-url], [data-embed], [data-watch], [data-src], [data-link], [data-url], [data-video]")) {
                            val raw = extractServerAttr(el)
                            val abs = serverUrlFromAttribute(raw, watchSubLink)
                            if (isServerCandidateUrl(abs, watchSubLink) && !isSeriesNavigationLink(el, abs) &&
                                servers.none { it.embedUrl == abs }
                            ) {
                                val sName = el.text().trim()
                                    .replace(Regex("(?i)(أكوام|فاصل|وي سيما|إيجي بست|لودي نت|عرب ليونز|فشار|مشاهدة|سيرفر|server)"), "")
                                    .trim()
                                val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""
                                servers += ArabseedServer("سيرفر ${servers.size + 1}$label", abs)
                            }
                        }
                    }
                    if (servers.isNotEmpty()) break
                }
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
        episodeNumber: Int? = null,
        seasonNumber: Int? = null,
    ): List<ArabseedServer> {
        if (title.isBlank()) return emptyList()
        val normalizedTitle = normalizeTitleForTokenization(title)
        if (normalizedTitle.isBlank()) return emptyList()

        val cleanSeriesTitle = SeriesCatalogParser.cleanSeriesTitle(title).ifBlank {
            title
                .replace(Regex("""(?i)(?:الموسم|موسم|الحلقة|حلقة|season|episode|ep|s\d+)\s*\d+"""), "")
                .replace(Regex("""\s{2,}"""), " ")
                .trim()
        }
        val searchQueries = mutableListOf<String>()
        if (cleanSeriesTitle.isNotBlank()) {
            searchQueries += cleanSeriesTitle
            searchQueries += normalizeTitleForTokenization(cleanSeriesTitle)
            if (cleanSeriesTitle.contains(":")) {
                val beforeColon = cleanSeriesTitle.substringBefore(":").trim()
                val afterColon = cleanSeriesTitle.substringAfter(":").trim()
                if (beforeColon.isNotBlank()) searchQueries += beforeColon
                if (afterColon.isNotBlank()) searchQueries += afterColon
            }
            if (episodeNumber != null && episodeNumber > 0) {
                searchQueries += "$cleanSeriesTitle الحلقة $episodeNumber"
                searchQueries += "$cleanSeriesTitle $episodeNumber"
            }
        }
        searchQueries += normalizedTitle
        searchQueries += title.trim()
        val distinctQueries = searchQueries
            .map(::normalizeSearchQueryPunctuation)
            .filter(String::isNotBlank)
            .distinct()

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
            episodeNumber?.toString() ?: "",
            seasonNumber?.toString() ?: "",
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

        val isEpisodeTarget = (episodeNumber != null && episodeNumber > 0) || (type == ArabseedType.SERIES && 
            (SeriesCatalogParser.isEpisode(title, originalPageUrl) || originalPageUrl.contains("-ep") || originalPageUrl.contains("/watch/")))
        val targetEpNum = episodeNumber ?: SeriesCatalogParser.episodeNumber(title, originalPageUrl)
        val targetSeasonNum = seasonNumber ?: SeriesCatalogParser.seasonNumber(title, originalPageUrl) ?: 1

        val enabledProviders = PROVIDERS.filter { it.enabled && it.id != originalProvider?.id }
        val sortedProviders = enabledProviders.sortedByDescending { if (it.isPrimary) 1 else 0 }
        val tasks = sortedProviders.map { provider ->
            {
                try {
                    var match: ArabseedItem? = null
                    for (query in distinctQueries) {
                        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                        val items = fetchProviderListing(
                            provider,
                            provider.searchPath.replace("{q}", encodedQuery),
                            type,
                        )
                        match = items.asSequence()
                            .filter { it.type == type }
                            .filter {
                                requestedYear.isBlank() || it.year.isBlank() ||
                                    Math.abs((it.year.toIntOrNull() ?: 0) - (requestedYear.toIntOrNull() ?: 0)) <= 1
                            }
                            .filter { item -> matchesProviderTitle(title, item) || (cleanSeriesTitle.isNotBlank() && matchesProviderTitle(cleanSeriesTitle, item)) }
                            .sortedByDescending { it.year.isNotBlank() && it.year == requestedYear }
                            .firstOrNull()
                        if (match != null) break
                    }
                    if (match != null && match.pageUrl != originalPageUrl) {
                        val targetEpisodeUrl = if (isEpisodeTarget && targetEpNum != null) {
                            if (SeriesCatalogParser.isEpisode(match.title, match.pageUrl) && SeriesCatalogParser.episodeNumber(match.title, match.pageUrl) == targetEpNum) {
                                match.pageUrl
                            } else {
                                val bundle = fetchSeasonsAndEpisodes(match, forceRefresh = false, seasonNumbersOnly = setOf(targetSeasonNum))
                                val targetSeason = bundle.seasons.firstOrNull { it.number == targetSeasonNum } ?: bundle.seasons.firstOrNull()
                                targetSeason?.episodes?.firstOrNull { it.number == targetEpNum }?.pageUrl
                            }
                        } else match.pageUrl

                        if (!targetEpisodeUrl.isNullOrBlank() && targetEpisodeUrl != originalPageUrl) {
                            val pageServers = fetchServers(targetEpisodeUrl)
                            pageServers.forEachIndexed { idx, server ->
                                if (server.embedUrl != originalPageUrl) {
                                    val key = server.embedUrl
                                    val sLabel = server.name.ifBlank { "سيرفر ${idx + 1}" }
                                    val tagged = server.copy(
                                        name = "${provider.name} - $sLabel",
                                        sourcePageUrl = targetEpisodeUrl,
                                        sourceProviderName = provider.name,
                                    )
                                    if (allServers.putIfAbsent(key, tagged) == null) {
                                        serverOrder.add(key)
                                    }
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
                    val searchSeq = if (cleanSeriesTitle.isNotBlank() && cleanSeriesTitle != title) {
                        (ArabseedRepository.search(cleanSeriesTitle).ifEmpty { ArabseedRepository.search(title) })
                    } else {
                        ArabseedRepository.search(title)
                    }
                    val match = searchSeq
                        .asSequence()
                        .filter { it.type == type }
                        .filter { requestedYear.isBlank() || it.year.isBlank() || it.year == requestedYear }
                        .filter { item -> matchesProviderTitle(title, item) || (cleanSeriesTitle.isNotBlank() && matchesProviderTitle(cleanSeriesTitle, item)) }
                        .sortedByDescending { it.year.isNotBlank() && it.year == requestedYear }
                        .firstOrNull()
                    if (match != null && match.pageUrl != originalPageUrl) {
                        val targetEpisodeUrl = if (isEpisodeTarget && targetEpNum != null) {
                            if (SeriesCatalogParser.isEpisode(match.title, match.pageUrl) && SeriesCatalogParser.episodeNumber(match.title, match.pageUrl) == targetEpNum) {
                                match.pageUrl
                            } else {
                                val bundle = ArabseedRepository.fetchSeasonsAndEpisodes(match, forceRefresh = false, seasonNumbersOnly = setOf(targetSeasonNum))
                                val targetSeason = bundle.seasons.firstOrNull { it.number == targetSeasonNum } ?: bundle.seasons.firstOrNull()
                                targetSeason?.episodes?.firstOrNull { it.number == targetEpNum }?.pageUrl
                            }
                        } else match.pageUrl

                        if (!targetEpisodeUrl.isNullOrBlank() && targetEpisodeUrl != originalPageUrl) {
                            val arabseedServers = ArabseedRepository.fetchEpisodeServers(targetEpisodeUrl)
                            arabseedServers
                                .filter { isServerCandidateUrl(it.embedUrl, targetEpisodeUrl) }
                                .forEachIndexed { index, server ->
                                    if (server.embedUrl != originalPageUrl) {
                                        val tagged = server.copy(
                                            name = "عرب سيد - ${server.name.ifBlank { "سيرفر ${index + 1}" }}",
                                            sourcePageUrl = targetEpisodeUrl,
                                            sourceProviderName = "عرب سيد",
                                        )
                                        if (allServers.putIfAbsent(tagged.embedUrl, tagged) == null) {
                                            serverOrder.add(tagged.embedUrl)
                                        }
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
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
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
            if (allServers.size >= 4 && System.nanoTime() > (deadline - TimeUnit.SECONDS.toNanos(6))) {
                // If primary providers already provided ample backup servers, return fast
                break
            }
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

    val TITLE_STOP_WORDS = setOf(
        "موسم", "الموسم", "حلقة", "الحلقة", "جزء", "الجزء", "مسلسل", "فيلم", "سلسلة",
        "انمي", "أنمي", "كرتون", "برنامج", "مترجم", "مدبلج", "اونلاين", "مشاهدة", "تحميل",
        "كامل", "كاملة", "حصريا", "حصري", "جديد", "عربي", "اجنبي", "تركي", "كوري", "اسيوي",
        "هندي", "مدبلجة", "مترجمة", "نسخة", "اصلية", "اصلي", "جودة", "عالية", "فيديو", "مشاهده",
        "بجودة", "بالجودة", "عاليه", "بدقة", "والاخيرة", "والاخير",
        "الاول", "الاولى", "الثاني", "الثانية", "الثالث", "الثالثة", "الرابع", "الرابعة",
        "الخامس", "الخامسة", "السادس", "السادسة", "السابع", "السابعة", "الثامن", "الثامنة",
        "التاسع", "التاسعة", "العاشر", "العاشرة", "الاخير", "الاخيرة",
        "season", "episode", "episodes", "ep", "series", "movie", "film", "watch",
        "download", "online", "full", "hd", "fhd", "4k", "1080p", "720p", "480p",
        "bluray", "blueray", "webdl", "web-dl", "web", "dl", "dvd", "rip", "cam",
        "sub", "dub", "arabic", "english", "korean", "turkish", "anime", "action",
        "the", "of", "and", "in", "on", "at", "to", "a", "an", "is", "vs",
        "akwam", "arabseed", "wecima", "fasel", "faselhd", "shahid4u", "cima4u", "egybest"
    )

    fun normalizeTitleForTokenization(title: String): String {
        return normalizeArabicTitle(title)
            .replace(Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)"""), " ")
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
    }

    data class TitleTokens(val all: Set<String>, val latin: Set<String>, val arabic: Set<String>)

    fun extractTitleTokens(title: String): TitleTokens {
        val norm = normalizeTitleForTokenization(title)
        val tokens = mutableListOf<String>()
        val latinRegex = Regex("^[a-z0-9]+$")
        for (rawWord in norm.split(" ")) {
            val w = rawWord.trim()
            if (w.isEmpty() || w.toIntOrNull() != null || w in TITLE_STOP_WORDS) continue
            if (w.length < 2 && w !in setOf("x", "z", "v", "y")) continue
            if ((w.startsWith("ب") || w.startsWith("و")) && w.length > 2 && w.substring(1) in TITLE_STOP_WORDS) continue
            tokens.add(w)
        }
        val allSet = tokens.toSet()
        val latSet = tokens.filter { latinRegex.matches(it) }.toSet()
        val arSet = tokens.filter { !latinRegex.matches(it) }.toSet()
        return TitleTokens(allSet, latSet, arSet)
    }

    fun titlesStrictlyMatch(target: String, candidate: String): Boolean {
        if (target.isBlank() || candidate.isBlank()) return false
        val normTarget = normalizeTitleForTokenization(target)
        val normCandidate = normalizeTitleForTokenization(candidate)
        if (normTarget == normCandidate) return true
        if (normCandidate.length >= 6 && normTarget.contains(normCandidate) && normCandidate.length >= (normTarget.length * 0.85).toInt()) return true
        if (normTarget.length >= 6 && normCandidate.contains(normTarget) && normTarget.length >= (normCandidate.length * 0.85).toInt()) return true

        val (tAll, tLat, tAr) = extractTitleTokens(target)
        val (cAll, cLat, cAr) = extractTitleTokens(candidate)
        if (tAll.isEmpty() || cAll.isEmpty()) return false

        // 1. Latin tokens matching (Foreign, English, Anime, Korean titles)
        if (tLat.isNotEmpty() && cLat.isNotEmpty()) {
            val sharedLat = tLat.intersect(cLat)
            if (sharedLat.size == tLat.size && (cLat.size == tLat.size || (sharedLat.size.toDouble() / cLat.size) >= 0.70)) return true
            if (tLat.size >= 3 && sharedLat.size >= tLat.size - 1 && (sharedLat.size.toDouble() / cLat.size) >= 0.70) return true
            if (tLat.size == 1 && sharedLat.size == 1 && cLat.size == 1) return true
        }

        // 2. Arabic tokens matching (Arabic, Turkish, Dubbed titles)
        if (tAr.isNotEmpty() && cAr.isNotEmpty()) {
            val sharedAr = tAr.intersect(cAr)
            if (sharedAr.size == tAr.size && (cAr.size == tAr.size || (sharedAr.size.toDouble() / cAr.size) >= 0.70)) return true
            if (tAr.size >= 3 && sharedAr.size >= tAr.size - 1 && (sharedAr.size.toDouble() / cAr.size) >= 0.70) return true
            if (tAr.size == 1 && sharedAr.size == 1 && cAr.size == 1) return true
        }

        // 3. Combined exact token match
        if (tAll == cAll) return true

        return false
    }

    private fun matchesProviderTitle(targetTitle: String, candidate: ArabseedItem): Boolean {
        if (targetTitle.isBlank()) return false
        val candidateTitles = listOfNotNull(
            candidate.parentSeriesTitle,
            candidate.title,
            candidate.arabicTitle,
        )
        return candidateTitles.any { cand -> titlesStrictlyMatch(targetTitle, cand) }
    }

    /** تحقق مما إذا كانت الصفحة هي الصفحة الرئيسية أو صفحة تصنيفات بدلاً من صفحة المادة المطلوبة */
    fun isHomePageOrListingPage(doc: Document, pageUrl: String): Boolean {
        val title = doc.title().lowercase().trim()
        val homeMarkers = listOf(
            "موقع التحميل و المشاهدة العربي الاول",
            "موقع التحميل والمشاهدة العربي الأول",
            "السينما للجميع",
            "عرب سيد - الرئيسية",
            "الصفحة الرئيسية",
            "اكوام - akwam",
            "wecima",
            "cima4u",
            "shahid4u",
            "faselhd",
            "egybest"
        )
        if (homeMarkers.any { title == it || (title.startsWith(it) && !title.contains("مشاهدة") && !title.contains("فيلم") && !title.contains("مسلسل") && !title.contains("حلقة")) }) {
            return true
        }

        if (pageUrl.isNotBlank()) {
            val uriPath = try { java.net.URI.create(pageUrl).path.orEmpty().trim('/') } catch (_: Exception) { "" }
            if (uriPath.isEmpty() || uriPath.equals("home", ignoreCase = true) || uriPath.equals("main", ignoreCase = true) || uriPath.equals("index.html", ignoreCase = true) || uriPath.equals("index.php", ignoreCase = true)) {
                return true
            }
        }

        val cardsCount = doc.select(".br-card, .MovieBlock, .GridItem, .postDiv, article.MovieItem, a.movie-card").size
        val hasPlayer = doc.select(
            "iframe, .watch-box, #watch-player, .player-container, .watchServers, a.btn-watch, button.btn-service, " +
            "[data-link], [data-url], [data-src], [data-embed], [data-watch], [data-embed-url], " +
            "ul.servers, ul.serversList, .list_servers, .servers, #servers, .watch-servers"
        ).isNotEmpty()
        if (cardsCount > 25 && !hasPlayer) {
            return true
        }

        return false
    }

    /** تحقق مما إذا كانت صفحة نتائج البحث فارغة (0 نتائج) وتعود بمقترحات/شائع فقط */
    fun isSearchEmptyResultPage(html: String, path: String, finalUrl: String, doc: Document): Boolean {
        val isSearch = path.contains("search") || path.contains("?s=") || path.contains("?q=") || path.contains("find")
        if (!isSearch) return false

        val finalUri = try { java.net.URI.create(finalUrl) } catch (_: Exception) { null }
        val hasQuery = !finalUri?.query.isNullOrBlank()
        val finalPath = finalUri?.path.orEmpty().trim('/')
        if (!hasQuery && (finalPath.isEmpty() || finalPath == "home" || finalPath == "main")) return true

        val text = doc.body()?.text().orEmpty()
        val emptyMarkers = listOf(
            "لم يتم العثور على أي نتائج",
            "لا توجد نتائج",
            "لم نجد أي نتائج",
            "0 نتائج",
            "0 results",
            "no results found",
            "not found any",
            "عذرا لا توجد نتائج",
            "عذراً، لم يتم العثور",
            "لا يوجد نتائج للبحث",
            "لا توجد اي نتائج تطابق",
        )
        return emptyMarkers.any { text.contains(it, ignoreCase = true) }
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
        if (getProviderForUrl(pageUrl) != null) {
            val dedicated = fetchServers(pageUrl)
            if (dedicated.isNotEmpty()) return dedicated.take(10)
        }
        val result = mutableListOf<ArabseedServer>()
        try {
            val html = fetchHtmlWithClient(pageUrl, referer, client)
            if (html.isBlank()) return result
            val doc = Jsoup.parse(html, pageUrl)
            if (isHomePageOrListingPage(doc, pageUrl)) {
                android.util.Log.w(TAG, "Rejected home/listing page in fetchServersFromPage: $pageUrl")
                return result
            }
            for (ifr in doc.select("iframe[src]")) {
                if (insideNonPlaybackContainer(ifr)) continue
                val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()
                if (isServerCandidateUrl(src, pageUrl) && result.none { it.embedUrl == src }) {
                    result += ArabseedServer("سيرفر ", src)
                }
            }
            for (el in doc.select("[data-embed-url],[data-embed],[data-watch],[data-src],[data-link],[data-url],[data-video],.servers a,a.server-btn")) {
                if (insideNonPlaybackContainer(el)) continue
                val abs = serverUrlFromAttribute(extractServerAttr(el), pageUrl)
                if (isServerCandidateUrl(abs, pageUrl) && result.none { it.embedUrl == abs }) {
                    result += ArabseedServer("سيرفر ", abs)
                }
            }
            // متابعة صفحة المشاهدة الفرعية إن لم نجد سيرفرات في الصفحة الرئيسية
            if (result.isEmpty()) {
                val watchLink = doc.selectFirst("a[href*='/watch/'], a[href*='/servers/'], a[href*='/view/'], a.btn-watch, a[href*='watch']")?.attr("abs:href")?.trim().orEmpty()
                if (watchLink.isNotBlank() && watchLink != pageUrl && isServerCandidateUrl(watchLink, pageUrl)) {
                    val watchHtml = fetchHtmlWithClient(watchLink, pageUrl, client)
                    if (watchHtml.isNotBlank()) {
                        val wDoc = Jsoup.parse(watchHtml, watchLink)
                        if (!isHomePageOrListingPage(wDoc, watchLink)) {
                            for (ifr in wDoc.select("iframe[src]")) {
                                if (insideNonPlaybackContainer(ifr)) continue
                                val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()
                                if (isServerCandidateUrl(src, pageUrl) && result.none { it.embedUrl == src }) {
                                    result += ArabseedServer("سيرفر ", src)
                                }
                            }
                            for (el in wDoc.select("[data-embed],[data-src],[data-url],[data-video],.servers a,a.server-btn")) {
                                if (insideNonPlaybackContainer(el)) continue
                                val link = el.attr("data-embed").ifBlank { el.attr("data-src").ifBlank { el.attr("data-url").ifBlank { el.attr("data-video").ifBlank { el.attr("href") } } } }.trim()
                                val abs = serverUrlFromAttribute(link, watchLink)
                                if (isServerCandidateUrl(abs, pageUrl) && result.none { it.embedUrl == abs }) {
                                    result += ArabseedServer("سيرفر ", abs)
                                }
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

        // ── Failover: if the primary provider returned 0 servers, search across other providers ──
        if (servers.isEmpty()) {
            try {
                servers = fetchServersAllProviders(
                    title = item.title,
                    originalPageUrl = item.pageUrl,
                    type = item.type,
                    requestedYear = item.year
                )
            } catch (_: Exception) {}
        }

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
            CatalogDiskCache.saveDetail("providers:detail:v4:" + item.pageUrl, detail)
        }

        return detail
    }

    /**
     * Tests embed servers in parallel (up to 8 at once).
     * Returns IMMEDIATELY on the first direct-playable URL (.mp4 / .m3u8) found.
     */
    private fun resolveFirstWorkingStream(
        servers: List<ArabseedServer>,
        pageReferer: String,
    ): String? {
        if (servers.isEmpty()) return null

        // Already have a verified direct stream on a server? Return it immediately.
        servers.firstOrNull { s ->
            val direct = s.directStreamUrl.orEmpty()
            direct.isNotBlank() &&
                (direct.contains(".mp4") || direct.contains(".m3u8")) &&
                isVerifiedPlayableStream(direct, pageReferer)
        }?.directStreamUrl?.let { return it }

        val ecs = ExecutorCompletionService<String?>(pool)
        val futures = servers.take(8).map { server ->
            ecs.submit(Callable<String?> {
                try {
                    resolveStream(server.embedUrl, pageReferer)
                } catch (_: Exception) { null }
            })
        }

        var found: String? = null
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
        for (ignored in futures.indices) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) break
            try {
                val result = ecs.poll(remaining, TimeUnit.NANOSECONDS) ?: break
                val stream = result.get()
                if (!stream.isNullOrBlank()) {
                    found = stream
                    break
                }
            } catch (_: Exception) {}
        }
        // Cancel remaining immediately once found or timed out
        futures.forEach { it.cancel(true) }
        return found
    }

    private fun isConfiguredProviderHostOrSame(provider: ProviderConfig?, candidateUrl: String, pageUrl: String): Boolean {
        if (provider != null && isConfiguredProviderHost(provider, candidateUrl)) return true
        return hostMatches(candidateUrl, pageUrl)
    }

    /** Unwrap wrapper URLs like `vid/?id=...`, `?mycimafsd=...`, or `sayyarh.com?url=...`. */
    fun decodeIdWrapperUrl(rawUrl: String, depth: Int = 0): String {
        val clean = rawUrl.trim()
        if (depth > 3 || clean.isBlank()) return clean
        val parsed = runCatching { clean.toHttpUrlOrNull() }.getOrNull()
        val idParam = parsed?.queryParameter("id")
            ?: parsed?.queryParameter("mycimafsd")
            ?: when {
                clean.contains("mycimafsd=") -> clean.substringAfter("mycimafsd=").substringBefore("&")
                clean.contains("id=") -> clean.substringAfter("id=").substringBefore("&")
                else -> null
            }
        if (!idParam.isNullOrBlank()) {
            tryDecodeBase64HttpUrl(idParam)?.let { decoded ->
                return decodeIdWrapperUrl(decoded, depth + 1)
            }
        }
        val nestedUrl = parsed?.queryParameter("url")
        if (!nestedUrl.isNullOrBlank() && nestedUrl.startsWith("http", true)) {
            return decodeIdWrapperUrl(nestedUrl, depth + 1)
        }
        return clean
    }

    /** Decode VOE's modern MKGMa JSON script payload or legacy 'hls' / 'e' properties. */
    fun decodeVoeHtmlStream(html: String): String? {
        if (html.isBlank()) return null
        Regex("""['"]hls['"]\s*:\s*['"]([^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(html)
            ?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }

        Regex("""'e':\s*'([A-Za-z0-9+/=]{50,})'""").find(html)?.groupValues?.get(1)?.let { enc ->
            runCatching { String(android.util.Base64.decode(enc, android.util.Base64.DEFAULT)) }
                .getOrNull()?.let { decoded ->
                    Regex("""['"]hls['"]\s*:\s*['"]([^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE)
                        .find(decoded)?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return it }
                }
        }

        // Modern VOE MKGMa <script type="application/json">["..."]</script> encoding
        val jsonScripts = Regex("""<script\s+type=["']application/json["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html)
        for (match in jsonScripts) {
            val rawArray = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (rawArray.length < 40) continue
            val decodedUrl = runCatching {
                val arr = org.json.JSONArray(rawArray)
                val enc = arr.optString(0).takeIf { it.length >= 40 } ?: return@runCatching null
                val rot = buildString(enc.length) {
                    for (ch in enc) {
                        append(when (ch) {
                            in 'A'..'Z' -> ('A'.code + (ch.code - 'A'.code + 13) % 26).toChar()
                            in 'a'..'z' -> ('a'.code + (ch.code - 'a'.code + 13) % 26).toChar()
                            else -> ch
                        })
                    }
                }
                var cleaned = rot
                for (tok in arrayOf("@$", "^^", "~@", "%?", "*~", "!!", "#&")) {
                    cleaned = cleaned.replace(tok, "_")
                }
                cleaned = cleaned.replace("_", "")
                val step1 = String(android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT), Charsets.ISO_8859_1)
                val step2Reversed = buildString(step1.length) {
                    for (i in step1.length - 1 downTo 0) {
                        append(((step1[i].code - 3 + 256) % 256).toChar())
                    }
                }
                val jsonStr = String(android.util.Base64.decode(step2Reversed, android.util.Base64.DEFAULT), Charsets.UTF_8)
                val obj = JSONObject(jsonStr)
                listOf("source", "hls", "mp4")
                    .map { obj.optString(it).trim() }
                    .firstOrNull { it.startsWith("http", ignoreCase = true) }
            }.getOrNull()
            if (!decodedUrl.isNullOrBlank()) return decodedUrl
        }
        return null
    }

    /** Public entry-point used by FlutterMediaBridge for the "media.resolve" channel. */
    fun resolveStream(embedUrl: String, referer: String): String? =
        resolveStreamWithReferer(embedUrl, referer)?.url

    /** Resolves an embed URL and returns both the direct stream and the exact Referer accepted by its CDN. */
    fun resolveStreamWithReferer(embedUrl: String, referer: String): ResolvedStreamResult? {
        val normalizedEmbed = decodeIdWrapperUrl(embedUrl)
        val cacheKey = "$normalizedEmbed\n$referer"
        val now = System.currentTimeMillis()
        resolvedStreamCache[cacheKey]?.let { (savedAt, cachedResult) ->
            if (now - savedAt < STREAM_RESOLVE_TTL_MS && cachedResult.url.isNotBlank()) return cachedResult
        }
        val (candidateStream, finalEmbedUrl) = resolveEmbedToStreamWithMeta(normalizedEmbed, referer, depth = 0)
            ?: return null
        val finalOrigin = pageOrigin(finalEmbedUrl).let { if (it.isNotBlank()) "$it/" else "" }
        val origOrigin = pageOrigin(normalizedEmbed).let { if (it.isNotBlank()) "$it/" else "" }
        val refererCandidates = listOf(
            finalOrigin,
            finalEmbedUrl,
            origOrigin,
            normalizedEmbed,
            referer.trim(),
            "",
        ).distinct()
        val workingReferer = verifyPlayableStreamWithReferer(candidateStream, refererCandidates) ?: return null
        val result = ResolvedStreamResult(url = candidateStream, referer = workingReferer)
        resolvedStreamCache[cacheKey] = now to result
        return result
    }

    /** Probes candidate Referer headers in priority order and returns the first accepted Referer, or null. */
    fun verifyPlayableStreamWithReferer(streamUrl: String, refererCandidates: List<String>): String? {
        for (candidate in refererCandidates.distinct()) {
            if (isVerifiedPlayableStream(streamUrl, candidate)) return candidate
        }
        return null
    }

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
            probeHttp.newCall(request).execute().use { response ->
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

    /**
     * Verifies whether an embed webpage is actually alive and not a deleted-file,
     * 404/5xx, parked, homepage, or CPA trap page before falling back to WebView playback.
     */
    fun isEmbedUrlAlive(embedUrl: String, referer: String): Boolean {
        val parsed = embedUrl.toHttpUrlOrNull() ?: return false
        if (parsed.scheme !in setOf("http", "https") || !isServerCandidateUrl(embedUrl)) return false
        val cacheKey = "$embedUrl\n$referer"
        val now = System.currentTimeMillis()
        embedAliveCache[cacheKey]?.let { cached ->
            if (now - cached.checkedAt < STREAM_PROBE_TTL_MS) return cached.playable
        }

        val request = Request.Builder()
            .url(parsed)
            .get()
            .header("User-Agent", UA)
            .header("Accept", "text/html,application/xhtml+xml,video/*,*/*;q=0.8")
            .apply { if (referer.isNotBlank()) header("Referer", referer) }
            .build()

        val alive = try {
            probeHttp.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (isAdServerUrl(finalUrl) || !isServerCandidateUrl(finalUrl)) return@use false
                if (response.code == 403 || response.code == 404 || response.code == 410 || response.code >= 500) {
                    return@use false
                }

                val bodyBytes = ByteArray(4096)
                val count = response.body?.byteStream()?.use { it.read(bodyBytes).coerceAtLeast(0) } ?: 0
                val snippet = String(bodyBytes, 0, count, Charsets.UTF_8).lowercase(java.util.Locale.ROOT)

                val deadMarkers = listOf(
                    "file was deleted", "file has been deleted", "file not found",
                    "video not found", "this video has been removed", "video has expired",
                    "no longer available", "404 not found", "page not found",
                    "opencpamodal", "sorry, the file you requested"
                )
                if (deadMarkers.any(snippet::contains)) return@use false

                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
        embedAliveCache[cacheKey] = StreamProbe(now, alive)
        return alive
    }

    private fun decodeBase64UrlBytes(raw: String): ByteArray {
        val normalized = raw.trim().replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
    }

    /** Decrypt Byse/Vidsharing SPA `/api/videos/{code}` AES-256-GCM playback payloads. */
    private fun decodeByseStream(embedUrl: String): String? {
        val origin = pageOrigin(embedUrl).trimEnd('/')
        val code = embedUrl.substringBefore('?').trimEnd('/').substringAfterLast('/')
        if (origin.isBlank() || code.isBlank() || code.length < 4) return null
        return runCatching {
            val req = Request.Builder().url("$origin/api/videos/$code")
                .header("User-Agent", UA).header("Referer", embedUrl).header("Accept", "application/json").build()
            val apiJson = probeHttp.newCall(req).execute().use { it.body?.string().orEmpty() }
            if (apiJson.isBlank()) return@runCatching null
            val playback = JSONObject(apiJson).optJSONObject("playback") ?: return@runCatching null
            val keyParts = playback.optJSONArray("key_parts") ?: return@runCatching null
            val version = playback.optString("version").trim().toIntOrNull()
            val chosenParts = if (version != null && version in 1..20) {
                val i1 = version - 1
                val i2 = (31 - version) - 1
                if (i1 in 0 until keyParts.length() && i2 in 0 until keyParts.length()) {
                    listOf(keyParts.optString(i1), keyParts.optString(i2))
                } else emptyList()
            } else emptyList()
            val parts = if (chosenParts.isNotEmpty()) chosenParts else (0 until keyParts.length()).map { keyParts.optString(it) }
            val keyOut = java.io.ByteArrayOutputStream()
            for (p in parts) {
                if (p.isNotBlank()) keyOut.write(decodeBase64UrlBytes(p))
            }
            val keyBytes = keyOut.toByteArray()
            val ivBytes = decodeBase64UrlBytes(playback.optString("iv"))
            val cipherBytes = decodeBase64UrlBytes(playback.optString("payload"))
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, ivBytes))
            val plain = String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            val sources = JSONObject(plain).optJSONArray("sources") ?: return@runCatching null
            (0 until sources.length()).mapNotNull { idx ->
                sources.optJSONObject(idx)?.optString("url")?.replace("\\/", "/")?.trim()
                    ?.takeIf { it.startsWith("http", true) }
            }.firstOrNull()
        }.getOrNull()
    }

    private fun unpackDeanEdwards(script: String): String {
        return try {
            val regex = Regex(
                """eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[rd]\s*\)\s*\{.*?\}\s*\(\s*(['"])((?:\\.|[^\\])*?)\1\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(['"])((?:\\.|[^\\])*?)\5\.split\s*\(\s*['"]\|['"]\s*\)""",
                RegexOption.DOT_MATCHES_ALL,
            )
            val sb = StringBuilder()
            for (match in regex.findAll(script)) {
                val p = match.groupValues[2]
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                val a = match.groupValues[3].toIntOrNull()?.coerceIn(2, 62) ?: continue
                val c = match.groupValues[4].toIntOrNull() ?: continue
                val k = match.groupValues[6].split('|')
                fun baseN(num: Int): String {
                    val prefix = if (num < a) "" else baseN(num / a)
                    val rem = num % a
                    val ch = if (rem > 35) (rem + 29).toChar().toString() else rem.toString(36)
                    return prefix + ch
                }
                val dict = HashMap<String, String>(c)
                for (i in (c - 1) downTo 0) {
                    val key = if (a <= 10) i.toString() else baseN(i)
                    val word = if (i < k.size && k[i].isNotEmpty()) k[i] else key
                    dict[key] = word
                }
                sb.append(Regex("""\b\w+\b""").replace(p) { m -> dict[m.value] ?: m.value }).append('\n')
            }
            sb.toString()
        } catch (_: Exception) { "" }
    }

    private fun resolveEmbedToStream(embedUrl: String, referer: String): String? =
        resolveEmbedToStreamWithMeta(embedUrl, referer, 0)?.first

    /** Resolves an embed page and returns (directStreamUrl, finalRedirectedEmbedUrl). */
    fun resolveEmbedToStreamWithMeta(
        embedUrl: String,
        referer: String,
        depth: Int = 0,
    ): Pair<String, String>? {
        if (depth > 3 || embedUrl.isBlank()) return null
        val targetUrl = decodeIdWrapperUrl(embedUrl)
        if (isAdServerUrl(targetUrl)) return null

        // 1. Already a direct stream (including shahid4u.si /api/hls/stream/...)
        val path = targetUrl.substringBefore("?").lowercase()
        if (path.endsWith(".mp4") || path.endsWith(".m3u8") || targetUrl.contains("/api/hls/stream/")) {
            return targetUrl to referer.ifBlank { targetUrl }
        }

        // 2. Fetch the embed page HTML + capture final redirect URL
        val (html, finalEmbedUrl) = try {
            fetchHtmlAndFinalUrl(targetUrl, referer)
        } catch (_: Exception) { return null }
        if (html.isBlank()) return null

        val lowerFinal = finalEmbedUrl.lowercase(java.util.Locale.ROOT)
        if (html.trimStart().startsWith("#EXTM3U")) {
            return finalEmbedUrl to referer.ifBlank { pageOrigin(finalEmbedUrl) }
        }

        val unpackedJs = if (html.contains("eval") && html.contains("function(p,a,c,k,e")) unpackDeanEdwards(html) else ""
        val searchScope = if (unpackedJs.isNotBlank()) "$html\n$unpackedJs" else html

        fun normalizeStreamCandidate(raw: String): String? {
            val cleaned = raw.replace("\\/", "/").replace("&amp;", "&").trim()
            val withScheme = when {
                cleaned.startsWith("//") -> "https:$cleaned"
                cleaned.startsWith("/") -> absoluteServerUrl(cleaned, finalEmbedUrl)
                else -> cleaned
            }
            return withScheme.takeIf { it.startsWith("http") && !isAdUrl(it) && !it.contains("thumb") && !it.contains("poster") }
        }

        fun hit(url: String): Pair<String, String> = url to finalEmbedUrl

        // Byse / Vidsharing SPA (AES-256-GCM /api/videos/{code})
        if (lowerFinal.contains("byse") || lowerFinal.contains("vidsharing") || html.contains("Byse Frontend")) {
            decodeByseStream(finalEmbedUrl)?.let { return hit(it) }
        }

        // MyCima-MY ArtPlayer (shahid4u.bid data-watch)
        Regex("""videoUrl\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(searchScope)?.groupValues?.get(1)?.let(::normalizeStreamCandidate)?.let { return hit(it) }

        // XFileSharing unpacked `var links={"hls4":"...","hls2":"...","hls3":"..."}` (Minochinos, Callistanise, Fastvid, Earnvids)
        val xfsUrls = Regex("""["']hls[234]?["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(searchScope).mapNotNull { normalizeStreamCandidate(it.groupValues[1]) }.toList()
        (xfsUrls.firstOrNull { it.contains(".m3u8", true) } ?: xfsUrls.firstOrNull())?.let { return hit(it) }

        // ── Follow JS redirects on embed landing pages (e.g. voe.sx -> mirror) ──
        if (html.length < 4096 && (html.contains("Redirecting") || html.contains("window.location"))) {
            val jsRedirect = Regex("""window\.location(?:\.href)?\s*=\s*['"](https?://[^'"]+)['"]""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.trim()
            if (!jsRedirect.isNullOrBlank() && jsRedirect != finalEmbedUrl && jsRedirect != targetUrl) {
                resolveEmbedToStreamWithMeta(jsRedirect, finalEmbedUrl, depth + 1)?.let { return it }
            }
        }

        // ── Provider-specific extractors ────────────────────────────

        // Voe.sx / Voe mirrors (MKGMa JSON + legacy patterns)
        if (finalEmbedUrl.contains("voe.") || targetUrl.contains("voe.") || html.contains("MKGMa")) {
            decodeVoeHtmlStream(searchScope)?.let { return hit(it) }
        }

        // Doodstream / Playmogo / D000d / Myvidplay
        if (lowerFinal.contains("dood") || lowerFinal.contains("ds2play") || lowerFinal.contains("dooood") ||
            lowerFinal.contains("d000d") || lowerFinal.contains("playmogo") || lowerFinal.contains("doodstream") ||
            lowerFinal.contains("myvidplay") || lowerFinal.contains("doply") || searchScope.contains("/pass_md5/")
        ) {
            val passMatch = Regex("""\$\.get\s*\(\s*'(/pass_md5/[^']+)'""").find(searchScope)
                ?: Regex("""['"](/pass_md5/[^'"]+)['"]""").find(searchScope)
            passMatch?.groupValues?.get(1)?.let { passPath ->
                val tokenMatch = Regex("""token=([a-zA-Z0-9]+)""").find(searchScope)
                val token = tokenMatch?.groupValues?.get(1) ?: ""
                val baseHost = pageOrigin(finalEmbedUrl).ifBlank { pageOrigin(targetUrl) }
                if (baseHost.isNotBlank()) {
                    try {
                        val passReq = Request.Builder().url("$baseHost$passPath")
                            .header("User-Agent", UA).header("Referer", finalEmbedUrl).build()
                        val passResult = probeHttp.newCall(passReq).execute().use { it.body?.string() ?: "" }
                        if (passResult.trim().startsWith("http")) {
                            val nowMs = System.currentTimeMillis()
                            val streamUrl = "${passResult.trim()}$nowMs?token=$token&expiry=${nowMs + 60000}"
                            return hit(streamUrl)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Streamtape
        if (lowerFinal.contains("streamtape")) {
            Regex("""id=([a-zA-Z0-9_-]+)&expires""").find(searchScope)?.groupValues?.get(1)?.let { id ->
                Regex("""'[^']*'\s*\+\s*'([^']+)'""").find(searchScope)?.groupValues?.get(1)?.let { token ->
                    return hit("https://streamtape.com/get_video?id=$id&expires=&ip=&token=$token&stream=1")
                }
            }
            // Alternative Streamtape pattern
            Regex("""document\.getElementById\(['"]\S+['"]\)\.innerHTML\s*=\s*["']([^"']+)["']""").find(searchScope)
                ?.groupValues?.get(1)?.let { raw ->
                    if (raw.contains("streamtape")) return hit(if (raw.startsWith("//")) "https:$raw" else raw)
                }
        }

        // Vidmoly
        if (lowerFinal.contains("vidmoly")) {
            Regex("""['"]?(https?://[^\s'"<>]+\.m3u8[^\s'"<>]*)['"]?""").find(searchScope)
                ?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return hit(it) }
        }

        // Uqload / Govid / Vidsharing (sources: [...])
        Regex("""sources\s*:\s*\[\s*['"]([^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""").find(searchScope)
            ?.groupValues?.get(1)?.takeIf { it.startsWith("http") }?.let { return hit(it) }

        // Mixdrop / Mxdrop
        if (lowerFinal.contains("mixdrop") || lowerFinal.contains("mxdrop")) {
            Regex("""(?:MDCore\.wurl|\"wurl\"|wurl)\s*[:=]\s*["']([^"']+)["']""").find(searchScope)
                ?.groupValues?.get(1)?.let { wurl ->
                    normalizeStreamCandidate(wurl)?.let { return hit(it) }
                }
        }

        // Vidara / Sugbaioug API
        if (lowerFinal.contains("vidara.") || lowerFinal.contains("sugbaioug.")) {
            val fileCode = finalEmbedUrl.trimEnd('/').substringAfterLast('/')
            if (fileCode.isNotBlank()) {
                try {
                    val body = """{"filecode":"$fileCode","device":"android"}"""
                    val req = Request.Builder().url("https://vidara.to/api/stream")
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .header("User-Agent", UA).header("Referer", finalEmbedUrl).build()
                    probeHttp.newCall(req).execute().use { r ->
                        Regex(""""streaming_url"\s*:\s*"([^"]+)"""").find(r.body?.string() ?: "")
                            ?.groupValues?.get(1)?.replace("\\/", "/")?.takeIf { it.startsWith("http") }
                            ?.let { return hit(it) }
                    }
                } catch (_: Exception) {}
            }
        }

        // ── Generic extractors (search both HTML and unpacked JS) ─────

        // Direct <source src="..."> tags
        Regex("""<source\s+[^>]*src=['"]([^'"]+\.(?:mp4|m3u8)(?:\?[^'"]*)?)['"]""", RegexOption.IGNORE_CASE)
            .findAll(searchScope).mapNotNull { normalizeStreamCandidate(it.groupValues.getOrNull(1).orEmpty()) }
            .firstOrNull()?.let { return hit(it) }

        // Any URL containing .m3u8 or .mp4 (including protocol-relative // urls from unpacked players)
        Regex("""['"]?((?:https?:)?//[^\s'"<>\\]+\.(?:m3u8|mp4)(?:\?[^\s'"<>\\]*)?)['"]?""", RegexOption.IGNORE_CASE)
            .findAll(searchScope).mapNotNull { normalizeStreamCandidate(it.groupValues.getOrNull(1).orEmpty()) }
            .firstOrNull()?.let { return hit(it) }

        // JWPlayer / VideoJS sources
        Regex("""(?:file|src)\s*:\s*['"]((?:https?:)?//[^'"]+\.(?:mp4|m3u8)[^'"]*)['"]""", RegexOption.IGNORE_CASE)
            .findAll(searchScope).mapNotNull { normalizeStreamCandidate(it.groupValues.getOrNull(1).orEmpty()) }
            .firstOrNull()?.let { return hit(it) }

        // Follow visible iframe hop (one level deep; skip hidden 0x0 tracking iframes like govid's mycima.help)
        val ifrSrc = Jsoup.parse(searchScope, finalEmbedUrl).select("iframe[src]")
            .firstOrNull { ifr ->
                val rawSrc = ifr.attr("src").trim()
                val style = ifr.attr("style").lowercase()
                val w = ifr.attr("width").trim()
                val h = ifr.attr("height").trim()
                val isHidden = w == "0" || h == "0" || style.contains("display:none") ||
                    style.contains("display: none") || style.contains("visibility:hidden")
                !isHidden && !isAdUrl(rawSrc) && !isAdServerUrl(rawSrc) && !rawSrc.contains("mycima.help", true)
            }
            ?.attr("abs:src")?.trim() ?: ""
        if (ifrSrc.isNotBlank() && ifrSrc != finalEmbedUrl && ifrSrc != targetUrl) {
            return resolveEmbedToStreamWithMeta(ifrSrc, finalEmbedUrl, depth + 1)
        }

        // Hex-encoded stream URL
        Regex("""['"]?(68747470[0-9a-fA-F]{30,})['"]?""").findAll(searchScope).forEach { m ->
            val dec = hexToStringMPR(m.groupValues[1])
            if (dec.startsWith("http") && (dec.contains(".m3u8") || dec.contains(".mp4"))) return hit(dec)
        }

        // atob() Base64 URLs
        Regex("""atob\(['"]([A-Za-z0-9+/=]{20,})['"]""").findAll(searchScope).forEach { m ->
            val decoded = runCatching {
                String(android.util.Base64.decode(m.groupValues[1], android.util.Base64.DEFAULT))
            }.getOrNull() ?: return@forEach
            Regex("""['"]?((?:https?:)?//[^\s'"<>]+\.(?:m3u8|mp4)[^\s'"<>]*)['"]?""")
                .find(decoded)?.groupValues?.get(1)?.let(::normalizeStreamCandidate)?.let { return hit(it) }
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
                ".pwr-season-panel a[href]", ".epslist a[href]", "a.episode-card",
                "a.sh-episode-tile", ".Small--Box a", ".episode a", ".episodes a",
                ".episodes-list a", ".EpisodesList li a", ".eplister li a",
                ".eps-list li a", ".eps a", "a.ep", "a.episode__item", ".episode__item a", "a.ep-item",
                "div.block-post a[href*='/clarus/']", "a[href*='/clarus/']",
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
                                listOf("episode", "episodes", "eplist", "eps-list", "episodeslist", "epslist", "eps", "episodios", "pwr-season-panel")
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
                    maxPages = if (forceRefresh) 30 else 15,
                    timeoutSeconds = if (forceRefresh) 16 else 9,
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
                .take(50)
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
                        maxPages = if (forceRefresh) 24 else 12,
                        timeoutSeconds = if (forceRefresh) 14 else 9,
                    )
                    seasonNumber to paginated
                })
            }
            // A series page is on the first paint path, so keep short series fast while
            // giving 12+ season shows enough budget to collect all seasons cleanly.
            val seasonPageWaitSeconds = when {
                seasonPagesToFetch.size >= 12 -> 16L
                seasonPagesToFetch.size >= 4 -> 12L
                seasonNumbersOnly != null -> 10L
                else -> 9L
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
            val baseSeasonsWithLinks = (seasons + seasonPages.mapNotNull { (number, url) ->
                if (seasons.any { it.number == number }) null
                else ArabseedSeason(number, SeriesCatalogParser.seasonTitle(number), emptyList(), url)
            }).map { season ->
                if (season.pageUrl.isNotBlank()) season
                else season.copy(pageUrl = seasonPageUrlByNumber[season.number].orEmpty())
            }

            val seasonsWithLinks = if ((baseSeasonsWithLinks.isEmpty() || baseSeasonsWithLinks.all { it.episodes.isEmpty() }) && isEpisodePage) {
                val epNum = SeriesCatalogParser.episodeNumber(item.title, item.pageUrl) ?: 1
                val sNum = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl) ?: 1
                listOf(
                    ArabseedSeason(
                        number = sNum,
                        title = SeriesCatalogParser.seasonTitle(sNum),
                        episodes = listOf(
                            ArabseedEpisode(
                                number = epNum,
                                title = "الحلقة $epNum",
                                pageUrl = item.pageUrl,
                                seasonNumber = sNum,
                                providerName = getProviderForUrl(item.pageUrl)?.name ?: item.providerName,
                            )
                        ),
                        pageUrl = item.pageUrl,
                    )
                )
            } else baseSeasonsWithLinks

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
                CatalogDiskCache.saveSeasonBundle("providers:seasons:v26:" + item.pageUrl, bundle)
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
        val cacheKey = "combined:seasons:v26:${item.pageUrl}"
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

        // Build robust multi-query variants: strip colons, subtitles, and punctuation
        // because sites like Akwam, FaselHD, etc. fail on "%3A" or index only the main title.
        val colonPrefix = title.substringBefore(':').trim()
        val dashPrefix = title.substringBefore('-').trim()
        val cleanPunct = title.replace(Regex("[:|\\-_]"), " ").replace(Regex("\\s+"), " ").trim()
        val arTitle = item.arabicTitle.takeIf { it.isNotBlank() && it != title }
        val candidateSearchQueries = buildList {
            if (colonPrefix.length >= 3 && colonPrefix != title) add(colonPrefix)
            if (dashPrefix.length >= 3 && dashPrefix != title && dashPrefix != colonPrefix) add(dashPrefix)
            if (cleanPunct.length >= 3) add(cleanPunct)
            if (title.isNotBlank()) add(title)
            if (arTitle != null) {
                val cleanAr = SeriesCatalogParser.cleanSeriesTitle(arTitle)
                val arColon = cleanAr.substringBefore(':').substringBefore('-').trim()
                if (arColon.length >= 3) add(arColon)
                if (cleanAr.length >= 3) add(cleanAr)
            }
        }
            .map(::normalizeSearchQueryPunctuation)
            .filter(String::isNotBlank)
            .distinct()
            .take(2)

        val targetIdentityKeys = buildList {
            add(titleKey)
            if (!item.parentSeriesTitle.isNullOrBlank()) add(seriesIdentityKey(item.parentSeriesTitle!!))
            if (arTitle != null) add(seriesIdentityKey(arTitle))
            if (colonPrefix.length >= 3 && colonPrefix.length >= (titleKey.length * 0.8).toInt()) add(seriesIdentityKey(colonPrefix))
            if (dashPrefix.length >= 3 && dashPrefix.length >= (titleKey.length * 0.8).toInt()) add(seriesIdentityKey(dashPrefix))
        }.filter { it.length >= 3 }.distinct()

        val directDiscoveredEpisodes = java.util.concurrent.ConcurrentLinkedQueue<ArabseedEpisode>()
        val year = releaseYear(item)
        val originalProviderId = getProviderForUrl(item.pageUrl)?.id
        val allEnabledProviders = PROVIDERS.filter { it.enabled && it.id != originalProviderId }
        val primaryProviders = allEnabledProviders.filter { it.isPrimary }
        val backupProviders = allEnabledProviders.filter { !it.isPrimary }

        fun makeSearchTask(provider: ProviderConfig): () -> List<ArabseedItem> = {
            try {
                var providerCandidates = emptyList<ArabseedItem>()
                for (query in candidateSearchQueries) {
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    val results = fetchProviderListing(
                        provider,
                        provider.searchPath.replace("{q}", encoded),
                        ArabseedType.SERIES,
                    )
                    val seriesPages = mutableListOf<ArabseedItem>()
                    for (candidate in results) {
                        val titleMatch = targetIdentityKeys.any { key -> matchesSeriesTitle(key, candidate) }
                        if (!titleMatch) continue
                        val isEp = SeriesCatalogParser.isEpisode(candidate.title, candidate.pageUrl)
                        if (isEp) {
                            val strictlyMatches = titlesStrictlyMatch(item.title, candidate.title) ||
                                (!item.parentSeriesTitle.isNullOrBlank() && titlesStrictlyMatch(item.parentSeriesTitle!!, candidate.title))
                            if (!strictlyMatches) continue
                            val epNum = SeriesCatalogParser.episodeNumber(candidate.title, candidate.pageUrl)
                            if (epNum != null && epNum > 0) {
                                val sNum = SeriesCatalogParser.seasonNumber(candidate.title, candidate.pageUrl)
                                    ?: SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
                                    ?: 1
                                directDiscoveredEpisodes.add(
                                    ArabseedEpisode(
                                        number = epNum,
                                        title = "الحلقة $epNum",
                                        pageUrl = candidate.pageUrl,
                                        seasonNumber = sNum,
                                        providerName = provider.name,
                                    )
                                )
                            }
                        } else {
                            val candidateYear = releaseYear(candidate)
                            val isSeasonPage = SeriesCatalogParser.seasonNumber(candidate.title, candidate.pageUrl) != null
                            val compatibleYear = year.isBlank() || year == candidateYear ||
                                (candidateYear.isBlank() && isSeasonPage)
                            if (candidate.type == ArabseedType.SERIES && compatibleYear) {
                                seriesPages.add(candidate)
                            }
                        }
                    }
                    val foundDirect = directDiscoveredEpisodes.any { it.providerName == provider.name }
                    if (seriesPages.isNotEmpty() || foundDirect) {
                        android.util.Log.i(
                            TAG,
                            "Season search provider=${provider.id} matched ${seriesPages.size} series pages and ${directDiscoveredEpisodes.count { it.providerName == provider.name }} direct episodes with query='$query'",
                        )
                        providerCandidates = seriesPages.distinctBy { SeriesCatalogParser.normalizedUrl(it.pageUrl) }
                        break
                    }
                }
                providerCandidates
            } catch (error: Exception) {
                android.util.Log.w(
                    TAG,
                    "Season search skipped provider=${provider.id} error=${error.javaClass.simpleName}",
                )
                emptyList()
            }
        }

        // Fast Stage 1: Parallel priority search across active primary providers
        val primaryTasks = primaryProviders.map { makeSearchTask(it) }
        val primaryDiscovered = collectParallel(primaryTasks, timeoutSeconds = 3)

        // Stage 2: Only fallback to backup providers if primary providers returned zero results
        val discovered = if (primaryDiscovered.isNotEmpty() || directDiscoveredEpisodes.isNotEmpty()) {
            primaryDiscovered
        } else {
            val backupTasks = backupProviders.map { makeSearchTask(it) }
            collectParallel(backupTasks, timeoutSeconds = 3)
        }
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
                item.copy(
                    id = "${item.id}_season_${season.number}",
                    pageUrl = season.pageUrl,
                    parentSeriesTitle = item.parentSeriesTitle ?: item.title,
                    parentSeriesPageUrl = item.pageUrl,
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
        // Prioritize external canonical mirrors first (e.g. Shahid4U.si, WeCima, Esheeq)
        // before local season-tab fallback links so one provider's extra season tabs never
        // crowd out complete multi-season mirrors.
        val mirrorLimit = 8
        val primaryMirrorHost = runCatching { java.net.URI.create(item.pageUrl).host.orEmpty().lowercase().removePrefix("www.") }.getOrDefault("")
        val externalMirrors = (canonicalMirrors + seasonPageMirrors)
            .distinctBy { SeriesCatalogParser.normalizedUrl(it.first.pageUrl) }
            .sortedWith(
                compareByDescending<Pair<ArabseedItem, Set<Int>?>> { (mirror, _) ->
                    val mirrorHost = runCatching { java.net.URI.create(mirror.pageUrl).host.orEmpty().lowercase().removePrefix("www.") }.getOrDefault("")
                    if (mirrorHost.isNotBlank() && mirrorHost != primaryMirrorHost) 1 else 0
                }.thenByDescending { (mirror, _) ->
                    val provider = getProviderForUrl(mirror.pageUrl)
                    if (provider?.isPrimary == true) 2 else 0
                }.thenByDescending { (mirror, _) ->
                    catalogMetadataScore(mirror)
                }
            )
        val remainingSlots = (mirrorLimit - externalMirrors.size).coerceAtLeast(1)
        val mirrors = (externalMirrors.take(mirrorLimit) + localSeasonMirrors.take(remainingSlots))
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
        if (directDiscoveredEpisodes.isNotEmpty()) {
            val directEpisodesList = directDiscoveredEpisodes.toList()
            val directSeasons = directEpisodesList.groupBy { it.seasonNumber }
                .map { (sNum, eps) ->
                    ArabseedSeason(
                        number = sNum,
                        title = SeriesCatalogParser.seasonTitle(sNum),
                        episodes = eps.distinctBy { it.number }.sortedBy { it.number },
                        pageUrl = "",
                    )
                }
            val directBundle = ArabseedSeasonBundle(directSeasons)
            val syntheticItem = item.copy(
                id = "${item.id}_direct_episodes",
                providerName = "البحث المتعدد",
            )
            bundles += syntheticItem to directBundle
            android.util.Log.i(TAG, "Injected directDiscoveredEpisodes: totalEpisodes=${directEpisodesList.size} seasons=${directSeasons.size}")
        }
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
                    forceRefresh = false,
                    seasonNumbersOnly = seasonNumbers,
                )
            })
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12)
        var unfinished = futures.size
        while (unfinished > 0) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) break
            val finished = try {
                completion.poll(remaining, TimeUnit.NANOSECONDS) ?: break
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            unfinished--
            val completedBundle = runCatching { finished.get() }.getOrNull() ?: continue
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
            val existingCached = loadCachedMergedSeasonBundle(item.pageUrl)
            val partialBundles = if (existingCached != null && existingCached.seasons.isNotEmpty()) {
                bundles + (item.copy(id = "${item.id}_cached_preserved") to existingCached)
            } else {
                bundles
            }
            val partial = mergeProviderSeasonBundles(partialBundles)
            if (partial.seasons.isNotEmpty() || partial.relatedParts.isNotEmpty()) {
                CatalogDiskCache.saveSeasonBundle(cacheKey, partial)
            }
            val allSeasonsContiguous = partial.seasons.isNotEmpty() && !needsSeasonEnrichment(item, partial)
            if (allSeasonsContiguous) {
                android.util.Log.i(TAG, "Early break in season enrichment: contiguous seasons ready")
                break
            }
        }
        futures.filterNot { it.isDone }.forEach { it.cancel(true) }

        val existingCached = loadCachedMergedSeasonBundle(item.pageUrl)
        val allBundles = if (existingCached != null && existingCached.seasons.isNotEmpty()) {
            bundles + (item.copy(id = "${item.id}_cached_preserved") to existingCached)
        } else {
            bundles
        }
        val merged = mergeProviderSeasonBundles(allBundles)
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

    /** Fetch all mirrors synchronously so incomplete seasons/episodes never flash partial lists. */
    fun fetchFullSeasonBundleSync(
        item: ArabseedItem,
        primaryBundle: ArabseedSeasonBundle,
        forceRefresh: Boolean = false,
    ): ArabseedSeasonBundle {
        val cached = loadCachedMergedSeasonBundle(item.pageUrl)
        val combinedCurrent = cached?.let { mergeProviderSeasonBundles(listOf(item to it, item to primaryBundle)) }
            ?: primaryBundle
        if (!forceRefresh && combinedCurrent.seasons.isNotEmpty() && !needsSeasonEnrichment(item, combinedCurrent)) {
            return combinedCurrent
        }
        val key = SeriesCatalogParser.normalizedUrl(item.pageUrl)
        return try {
            if (key.isNotBlank()) seasonAggregationInFlight.add(key)
            val result = fetchSeasonBundleAcrossProviders(item, combinedCurrent, forceRefresh)
            recordSeasonEnrichmentAttempt(item, result)
            result
        } catch (error: Exception) {
            android.util.Log.w(TAG, "Full season sync failed for provider=${item.providerName}", error)
            combinedCurrent
        } finally {
            if (key.isNotBlank()) seasonAggregationInFlight.remove(key)
        }
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

    fun cacheCompletePrimarySeasonBundle(item: ArabseedItem, bundle: ArabseedSeasonBundle): ArabseedSeasonBundle {
        if (bundle.seasons.isNotEmpty() || bundle.relatedParts.isNotEmpty()) {
            CatalogDiskCache.saveSeasonBundle("combined:seasons:v26:${item.pageUrl}", bundle)
            recordSeasonEnrichmentAttempt(item, bundle)
        }
        return bundle
    }

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
        val totalEpisodes = bundle.seasons.sumOf { it.episodes.size }
        val minSeason = bundle.seasons.minOfOrNull { it.number } ?: 0
        val maxSeason = bundle.seasons.maxOfOrNull { it.number } ?: 0
        val hasSeasonGaps = bundle.seasons.map { it.number }.sorted().zipWithNext().any { (a, b) -> b - a > 1 }
        val isMissingLower = minSeason > 1
        val isMissingIntermediate = maxSeason > bundle.seasons.size
        val incomplete = bundle.seasons.isEmpty() || totalEpisodes < 5 || isMissingLower || isMissingIntermediate || hasSeasonGaps || bundle.seasons.any { season ->
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
        "combined:season-attempt:v22:$pageUrl"

    private fun needsSeasonEnrichment(item: ArabseedItem, bundle: ArabseedSeasonBundle): Boolean {
        if (item.type != ArabseedType.SERIES) return false
        val allEpisodes = bundle.seasons.flatMap { it.episodes }
        if (allEpisodes.isEmpty()) return true
        for (season in bundle.seasons) {
            val epNumbers = season.episodes.map { it.number }.filter { it > 0 }.distinct().sorted()
            if (epNumbers.isEmpty()) return true
            for (i in 1 until epNumbers.size) {
                if (epNumbers[i] - epNumbers[i - 1] > 1) return true
            }
        }
        return false
    }

    private fun matchesSeriesTitle(targetTitle: String, candidate: ArabseedItem): Boolean {
        if (targetTitle.isBlank()) return false
        val candidateTitles = listOfNotNull(
            candidate.parentSeriesTitle,
            candidate.title,
            candidate.arabicTitle,
        )
        return candidateTitles.any { cand -> titlesStrictlyMatch(targetTitle, cand) }
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

            if (sameProviderSeriesPage == null && entries.size > 1) {
                val episodeEntries = entries.map { it.second }.filter { SeriesCatalogParser.isEpisode(it.title, it.pageUrl) }
                if (episodeEntries.isNotEmpty()) {
                    val seasonMap = mutableMapOf<Int, MutableList<ArabseedEpisode>>()
                    for (entry in episodeEntries) {
                        val epNum = SeriesCatalogParser.episodeNumber(entry.title, entry.pageUrl) ?: continue
                        val sNum = SeriesCatalogParser.seasonNumber(entry.title, entry.pageUrl) ?: 1
                        val list = seasonMap.getOrPut(sNum) { mutableListOf() }
                        if (list.none { it.number == epNum }) {
                            list.add(ArabseedEpisode(
                                number = epNum,
                                title = "الحلقة $epNum",
                                pageUrl = entry.pageUrl,
                                seasonNumber = sNum,
                                providerName = entry.providerName
                            ))
                        }
                    }
                    val synthesizedSeasons = seasonMap.toSortedMap().map { (sNum, eps) ->
                        eps.sortBy { it.number }
                        ArabseedSeason(
                            number = sNum,
                            title = SeriesCatalogParser.seasonTitle(sNum),
                            episodes = eps,
                            pageUrl = selected.pageUrl
                        )
                    }
                    if (synthesizedSeasons.isNotEmpty()) {
                        ArabseedRepository.saveSynthesizedBundle(selected.pageUrl, ArabseedSeasonBundle(seasons = synthesizedSeasons))
                    }
                }
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
