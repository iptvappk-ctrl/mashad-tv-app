package com.madar.madar_flutter

import android.content.Context
import com.madar.tv.data.ArabseedCategories
import com.madar.tv.data.ArabseedDetail
import com.madar.tv.data.ArabseedEpisode
import com.madar.tv.data.ArabseedItem
import com.madar.tv.data.ArabseedRepository
import com.madar.tv.data.ArabseedSeasonBundle
import com.madar.tv.data.ArabseedServer
import com.madar.tv.data.ArabseedType
import com.madar.tv.data.CatalogDiskCache
import com.madar.tv.data.FootballRepository
import com.madar.tv.data.MultiProviderRepository
import com.madar.tv.data.SeriesCatalogParser
import com.madar.tv.data.YacineCategory
import com.madar.tv.data.YacineChannel
import com.madar.tv.data.YacineEvent
import com.madar.tv.data.YacineRepository
import com.madar.tv.data.YacineStream
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Flutter adapter for the same on-device repositories and disk cache used by MadarTV. */
class FlutterMediaBridge(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init { CatalogDiskCache.initialize(appContext) }

    fun handle(call: MethodCall, result: MethodChannel.Result): Boolean {
        val method = call.method
        if (!method.startsWith("catalog.") && !method.startsWith("media.") &&
            !method.startsWith("live.") && !method.startsWith("football.")) return false
        scope.launch {
            try {
                val value = withContext(Dispatchers.IO) { dispatch(method, call) }
                result.success(value)
            } catch (error: Throwable) {
                result.error("MADAR_DATA", error.message ?: "تعذر تحميل البيانات", null)
            }
        }
        return true
    }

    private suspend fun dispatch(method: String, call: MethodCall): Any? = when (method) {
        "catalog.tabs" -> ArabseedCategories.MAIN_TABS.map { tab ->
            mapOf("id" to tab.id, "title" to tab.title, "path" to tab.path,
                "subcategories" to ArabseedCategories.getSubCategories(tab.id).map {
                    mapOf("id" to it.id, "title" to it.title, "path" to it.path)
                })
        }
        "catalog.cached", "catalog.fetch" -> {
            val path = call.argument<String>("path").orEmpty().ifBlank { "main/" }
            val tab = call.argument<String>("tab").orEmpty().ifBlank { "home" }
            val categoryId = call.argument<String>("categoryId").orEmpty()
            val page = (call.argument<Number>("page")?.toInt() ?: 1).coerceAtLeast(1)
            val force = call.argument<Boolean>("force") ?: false
            catalog(path, tab, categoryId, page, method.endsWith("fetch"), force)
        }
        "catalog.primary.fetch" -> {
            val path = call.argument<String>("path").orEmpty().ifBlank { "main/" }
            val page = (call.argument<Number>("page")?.toInt() ?: 1).coerceAtLeast(1)
            ArabseedRepository.fetchCategory(path, page, forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        }
        "catalog.providers.fetch" -> {
            val path = call.argument<String>("path").orEmpty().ifBlank { "main/" }
            val tab = call.argument<String>("tab").orEmpty().ifBlank { "home" }
            val categoryId = call.argument<String>("categoryId").orEmpty()
            val page = (call.argument<Number>("page")?.toInt() ?: 1).coerceAtLeast(1)
            val force = call.argument<Boolean>("force") ?: false
            providerCatalog(path, tab, categoryId, page, force).map { it.toMap() }
        }
        "catalog.search.cached", "catalog.search" -> {
            val query = call.argument<String>("query").orEmpty().trim()
            if (query.isBlank()) emptyList<Map<String, Any?>>()
            else search(query, method == "catalog.search", call.argument<Boolean>("force") ?: false)
        }
        "catalog.search.primary" -> ArabseedRepository.search(call.argument<String>("query").orEmpty(), forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        "catalog.search.providers" -> MultiProviderRepository.searchAll(call.argument<String>("query").orEmpty(), forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        "media.detail.cached", "media.detail.fetch" -> {
            val item = call.argument<Map<String, Any?>>("item")?.toItem()
                ?: throw IllegalArgumentException("العنصر غير صالح")
            val cached = method.endsWith("cached")
            val detail = if (cached) loadCachedDetail(item) else fetchDetail(item)
            detail?.toMap()
        }
        "media.story.fetch" -> {
            val item = call.argument<Map<String, Any?>>("item")?.toItem()
                ?: throw IllegalArgumentException("العنصر غير صالح")
            val (overview, cast) = if (item.isArabseedSource()) {
                ArabseedRepository.fetchStoryFast(item.pageUrl) to emptyList()
            } else {
                MultiProviderRepository.fetchStoryAndCast(item.pageUrl)
            }
            mapOf("overview" to overview, "cast" to cast)
        }
        "media.seasons.cached", "media.seasons.fetch" -> {
            val item = call.argument<Map<String, Any?>>("item")?.toItem()
                ?: throw IllegalArgumentException("المسلسل غير صالح")
            val seriesItem = item.asParentSeries()
            val force = call.argument<Boolean>("force") ?: false
            val bundle = if (method.endsWith("cached")) {
                loadCachedSeasons(seriesItem)
            } else {
                fetchSeasons(seriesItem, force)
            }
            if (!method.endsWith("cached")) {
                val sourceHost = safeHttpUri(seriesItem.pageUrl)?.host.orEmpty()
                val seasons = bundle?.seasons.orEmpty()
                val counts = seasons.joinToString(",") { season ->
                    "${season.number}:${season.episodes.size}"
                }
                val missingEpisodePages = seasons.sumOf { season ->
                    season.episodes.count { it.pageUrl.isBlank() }
                }
                android.util.Log.i(
                    "FlutterMediaBridge",
                    "media.seasons url=${seriesItem.pageUrl} host=$sourceHost seasons=${seasons.size} episodeCounts=$counts missingPages=$missingEpisodePages",
                )
            }
            bundle?.toMap()?.let { mapped ->
                if (method.endsWith("cached")) mapped
                else mapped + ("backgroundRefreshing" to
                    MultiProviderRepository.isSeasonAggregationRunning(seriesItem.pageUrl))
            }
        }
        "media.seasons.status" -> {
            val item = call.argument<Map<String, Any?>>("item")?.toItem()
                ?: throw IllegalArgumentException("المسلسل غير صالح")
            val seriesItem = item.asParentSeries()
            mapOf(
                "backgroundRefreshing" to
                    MultiProviderRepository.isSeasonAggregationRunning(seriesItem.pageUrl),
            )
        }
        "media.season.page.fetch" -> {
            val item = call.argument<Map<String, Any?>>("item")?.toItem()
                ?: throw IllegalArgumentException("المسلسل غير صالح")
            val seriesItem = item.asParentSeries()
            val seasonUrl = call.argument<String>("seasonUrl").orEmpty().trim()
            val seasonNumber = call.argument<Number>("seasonNumber")?.toInt() ?: 0
            val uri = safeHttpUri(seasonUrl)
                ?: throw IllegalArgumentException("رابط الموسم غير صالح")
            val host = uri.host?.lowercase().orEmpty()
            val isArabseed = isArabseedUrl(seasonUrl)
            if (seasonNumber <= 0 || uri.scheme !in setOf("http", "https") || host.isBlank() ||
                (!isArabseed && !MultiProviderRepository.isMultiProvider(seasonUrl))
            ) throw IllegalArgumentException("رابط الموسم غير تابع لمصدر مدعوم")

            val seasonTitle = SeriesCatalogParser.seasonTitle(seasonNumber)
            val seasonItem = seriesItem.copy(
                id = "${seriesItem.id}_season_$seasonNumber",
                title = seriesItem.title.takeIf { it.isNotBlank() && it != seasonTitle }
                    ?: seriesItem.parentSeriesTitle ?: seasonTitle,
                arabicTitle = seriesItem.arabicTitle.takeIf { it.isNotBlank() && it != seasonTitle }
                    ?: seriesItem.parentSeriesTitle ?: seasonTitle,
                pageUrl = seasonUrl,
                providerName = if (isArabseed) "عرب سيد" else seriesItem.providerName,
                parentSeriesTitle = seriesItem.parentSeriesTitle ?: seriesItem.title,
                parentSeriesPageUrl = seriesItem.pageUrl,
                latestEpisodeLabel = "",
            )
            val rawBundle = if (isArabseed) {
                ArabseedRepository.fetchSeasonsAndEpisodes(
                    seasonItem,
                    forceRefresh = true,
                    seasonNumbersOnly = setOf(seasonNumber),
                )
            } else {
                MultiProviderRepository.fetchSeasonsAndEpisodes(
                    seasonItem,
                    forceRefresh = true,
                    seasonNumbersOnly = setOf(seasonNumber),
                )
            }
            val bundle = MultiProviderRepository.fetchSeasonBundleAcrossProviders(
                seriesItem,
                rawBundle,
                forceRefresh = false,
            )
            MultiProviderRepository.cacheSeasonPageEnrichment(seriesItem, seasonItem, bundle)
            bundle.toMap()
        }
        "media.season.episodes.more" -> {
            val seasonUrl = call.argument<String>("seasonUrl").orEmpty().trim()
            val cursorUrl = call.argument<String>("cursorUrl").orEmpty().trim().ifBlank { seasonUrl }
            val seasonNumber = call.argument<Number>("seasonNumber")?.toInt() ?: 0
            val knownEpisodeUrls = call.argument<List<String>>("knownEpisodeUrls")
                ?.map(String::trim)?.filter(String::isNotBlank)?.toSet().orEmpty()

            val (bundle, nextPageUrl) = if (isArabseedUrl(seasonUrl) || isArabseedUrl(cursorUrl)) {
                val rawSlug = cursorUrl.trimEnd('/').substringAfterLast('/')
                    .replace('-', ' ')
                    .replace('_', ' ')
                val cleanTitle = SeriesCatalogParser.cleanSeriesTitle(rawSlug)
                val dummyItem = ArabseedItem(
                    id = "season_$seasonNumber",
                    title = cleanTitle,
                    arabicTitle = cleanTitle,
                    posterUrl = null,
                    year = "",
                    quality = "",
                    rating = "",
                    type = ArabseedType.SERIES,
                    pageUrl = seasonUrl,
                    parentSeriesTitle = cleanTitle,
                    parentSeriesPageUrl = seasonUrl,
                    category = "",
                    providerName = "عرب سيد",
                )
                val primary = ArabseedRepository.fetchSeasonsAndEpisodes(dummyItem, forceRefresh = true)
                val enriched = MultiProviderRepository.fetchSeasonBundleAcrossProviders(dummyItem, primary, forceRefresh = true)
                enriched to null
            } else {
                if (!MultiProviderRepository.isMultiProvider(seasonUrl) ||
                    !MultiProviderRepository.isMultiProvider(cursorUrl)
                ) throw IllegalArgumentException("رابط صفحات الحلقات غير تابع لمصدر مدعوم")

                MultiProviderRepository.fetchMoreSeasonEpisodes(
                    seasonUrl = seasonUrl,
                    cursorUrl = cursorUrl,
                    seasonNumber = seasonNumber,
                    knownEpisodeUrls = knownEpisodeUrls,
                )
            }
            mapOf(
                "seasons" to bundle.toMap()["seasons"],
                "relatedParts" to emptyList<Any>(),
                "nextPageUrl" to nextPageUrl.orEmpty(),
                "hasMoreEpisodes" to !nextPageUrl.isNullOrBlank(),
            )
        }
        "media.servers" -> {
            val rawUrl = call.argument<String>("url").orEmpty()
            // Provider episode slugs occasionally contain literal whitespace.
            // Java URI rejects those before the server parser can even issue a
            // request; encode them once while preserving the URL delimiters.
            val url = rawUrl.trim().replace(Regex("\\s+"), "%20")
            val provider = call.argument<String>("provider").orEmpty()
            val arabseedSource = when {
                isArabseedUrl(url) -> true
                MultiProviderRepository.isMultiProvider(url) -> false
                safeHttpUri(url)?.host?.isNotBlank() == true -> false
                else -> provider.isArabseedProviderName()
            }
            val safeUri = safeHttpUri(url)
            android.util.Log.i(
                "FlutterMediaBridge",
                "media.servers scheme=${safeUri?.scheme.orEmpty()} host=${safeUri?.host.orEmpty()} " +
                    "authority=${safeUri?.authority.orEmpty()} urlLength=${url.length} " +
                    "rawWhitespace=${rawUrl.any { it.isWhitespace() }} " +
                    "parser=${if (arabseedSource) "primary" else "multi"}",
            )
            if (safeUri == null || safeUri.scheme !in setOf("http", "https") || safeUri.host.isNullOrBlank()) {
                android.util.Log.w("FlutterMediaBridge", "media.servers rejected invalid page URL")
                return emptyList<Map<String, Any?>>()
            }
            val rows = if (arabseedSource) ArabseedRepository.fetchEpisodeServers(url)
                else MultiProviderRepository.fetchServers(url)
            android.util.Log.i("FlutterMediaBridge", "media.servers result=${rows.size}")
            rows.filter { row ->
                isAllowedResolverInput(row.embedUrl) &&
                    (row.directStreamUrl.isNullOrBlank() || isAllowedResolverInput(row.directStreamUrl))
            }.map { it.toMap() }
        }
        // ── بحث مصادر مشاهدة احتياطية من المصادر المهيأة ──
        "media.servers.all" -> {
            val url = call.argument<String>("url").orEmpty().trim()
            val title = call.argument<String>("title").orEmpty().trim()
            val type = if (call.argument<String>("type").equals("series", ignoreCase = true)) {
                ArabseedType.SERIES
            } else {
                ArabseedType.MOVIE
            }
            val year = call.argument<String>("year").orEmpty().trim()
            if (title.isBlank() || url.isBlank()) return emptyList<Map<String, Any?>>()
            val rows = MultiProviderRepository.fetchServersAllProviders(title, url, type, year)
            android.util.Log.i("FlutterMediaBridge", "media.servers.all result=${rows.size} for title=$title")
            rows.filter { row -> isAllowedResolverInput(row.embedUrl) }.map { it.toMap() }
        }
        "media.resolve" -> {
            val source = call.argument<String>("url").orEmpty()
            val referer = call.argument<String>("referer").orEmpty()
            if (!isAllowedResolverInput(source)) null
            else {
                // Route to the right resolver based on URL origin
                val isArabseedEmbed = isArabseedUrl(source) ||
                    source.contains("myseed", ignoreCase = true) ||
                    source.contains("arabseed", ignoreCase = true) ||
                    source.contains("vids.php", ignoreCase = true)
                val resolved = if (isArabseedEmbed) {
                    val direct = ArabseedRepository.resolveDirectStream(source, referer)
                    if (!direct.isNullOrBlank() && isAllowedResolverInput(direct)) direct
                    else null
                } else {
                    // Try MultiProvider resolver first (handles Voe, Dood, Streamtape, etc.)
                    val multi = MultiProviderRepository.resolveStream(source, referer)
                    if (!multi.isNullOrBlank() && isAllowedResolverInput(multi)) multi
                    else {
                        val direct = ArabseedRepository.resolveDirectStream(source, referer)
                        if (!direct.isNullOrBlank() && isAllowedResolverInput(direct)) direct
                        else null
                    }
                }
                resolved?.takeIf(::isAllowedResolverInput)
            }
        }
        "live.events.cached" -> YacineRepository.getCachedEvents().map { it.toMap() }
        "live.events.fetch" -> YacineRepository.getEvents(forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        "live.categories.cached" -> YacineRepository.getCachedCategories().map { it.toMap() }
        "live.categories.fetch" -> YacineRepository.getCategories(forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        "live.channels.cached" -> {
            val catId = call.argument<String>("categoryId").orEmpty()
            YacineRepository.getCachedChannels(catId).map { it.toMap() }
        }
        "live.channels.fetch" -> {
            val catId = call.argument<String>("categoryId").orEmpty()
            val force = call.argument<Boolean>("force") ?: false
            YacineRepository.getChannels(catId, forceRefresh = force).map { it.toMap() }
        }
        "live.eventStreams" -> YacineRepository.getEventStreams(call.argument<String>("id").orEmpty()).map { it.toMap() }
        "live.matchStreams" -> YacineRepository.getMatchStreams(
            eventId = call.argument<String>("id").orEmpty(),
            channelHint = call.argument<String>("channel").orEmpty(),
            leagueHint = call.argument<String>("league").orEmpty(),
        ).map { it.toMap() }
        "live.channelStreams" -> YacineRepository.getChannelStreams(call.argument<String>("id").orEmpty()).map { it.toMap() }
        "football.cached" -> (FootballRepository.loadCachedMatches() ?: emptyList()).map { it.toMap() }
        "football.fetch" -> FootballRepository.fetchUpcomingMatches(forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        else -> null
    }

    private fun catalog(
        path: String,
        tab: String,
        categoryId: String,
        page: Int,
        fetch: Boolean,
        force: Boolean,
    ): List<Map<String, Any?>> {
        val expected = when (tab) {
            "series" -> ArabseedType.SERIES
            "movies" -> ArabseedType.MOVIE
            "anime" -> if (categoryId == "anime_series") ArabseedType.SERIES else ArabseedType.MOVIE
            "ramadan" -> ArabseedType.SERIES
            else -> null
        }
        val primary = when {
            !fetch -> ArabseedRepository.loadCachedCategory(path, page).orEmpty()
            else -> ArabseedRepository.fetchCategory(path, page, forceRefresh = force)
        }
        val providerRows = when (tab) {
            "series" -> if (categoryId == "all_series" || path.trim('/') == "series") {
                if (fetch) MultiProviderRepository.fetchSeriesAll(page, forceRefresh = force)
                else MultiProviderRepository.loadCachedCatalog(ArabseedType.SERIES, page).orEmpty()
            } else if (fetch) {
                MultiProviderRepository.fetchCategoryAll(categoryId, page, forceRefresh = force)
            } else {
                MultiProviderRepository.loadCachedCategory(categoryId, page).orEmpty()
            }
            "movies" -> if (categoryId == "all_movies" || path.trim('/') == "movies") {
                if (fetch) MultiProviderRepository.fetchMoviesAll(page, forceRefresh = force)
                else MultiProviderRepository.loadCachedCatalog(ArabseedType.MOVIE, page).orEmpty()
            } else if (fetch) {
                MultiProviderRepository.fetchCategoryAll(categoryId, page, forceRefresh = force)
            } else {
                MultiProviderRepository.loadCachedCategory(categoryId, page).orEmpty()
            }
            "anime", "ramadan" -> if (fetch) {
                MultiProviderRepository.fetchCategoryAll(categoryId, page, forceRefresh = force)
            } else {
                MultiProviderRepository.loadCachedCategory(categoryId, page).orEmpty()
            }
            "home" -> if (path == "main/" || path == "main") {
                if (fetch) MultiProviderRepository.fetchCatalogAll(page, forceRefresh = force)
                else MultiProviderRepository.loadCachedCatalog(null, page).orEmpty()
            } else emptyList()
            else -> emptyList()
        }
        return MultiProviderRepository.mergeCatalogItems(primary + providerRows, expected)
            .map { it.toMap() }
    }

    private fun search(query: String, fetch: Boolean, force: Boolean): List<Map<String, Any?>> {
        val primary = if (fetch) ArabseedRepository.search(query, forceRefresh = force) else ArabseedRepository.loadCachedSearch(query).orEmpty()
        val providers = if (fetch) MultiProviderRepository.searchAll(query, forceRefresh = force) else MultiProviderRepository.loadCachedSearch(query).orEmpty()
        return MultiProviderRepository.mergeCatalogItems(primary + providers).map { it.toMap() }
    }

    private fun providerCatalog(path: String, tab: String, categoryId: String, page: Int, force: Boolean): List<ArabseedItem> = when (tab) {
        "series" -> if (categoryId == "all_series" || path.trim('/') == "series") {
            MultiProviderRepository.fetchSeriesAll(page, forceRefresh = force)
        } else MultiProviderRepository.fetchCategoryAll(categoryId, page, forceRefresh = force)
        "movies" -> if (categoryId == "all_movies" || path.trim('/') == "movies") {
            MultiProviderRepository.fetchMoviesAll(page, forceRefresh = force)
        } else MultiProviderRepository.fetchCategoryAll(categoryId, page, forceRefresh = force)
        "anime", "ramadan" -> MultiProviderRepository.fetchCategoryAll(categoryId, page, forceRefresh = force)
        "home" -> if (path == "main/" || path == "main") MultiProviderRepository.fetchCatalogAll(page, forceRefresh = force) else emptyList()
        else -> emptyList()
    }

    private fun loadCachedDetail(item: ArabseedItem): ArabseedDetail? =
        if (item.isArabseedSource()) ArabseedRepository.loadCachedDetail(item.pageUrl)
        else MultiProviderRepository.loadCachedDetail(item.pageUrl)

    private fun fetchDetail(item: ArabseedItem): ArabseedDetail =
        if (item.isArabseedSource()) ArabseedRepository.fetchDetail(item)
        else MultiProviderRepository.fetchDetail(item)

    private fun loadCachedSeasons(item: ArabseedItem): ArabseedSeasonBundle? =
        MultiProviderRepository.loadCachedMergedSeasonBundle(item.pageUrl)
            ?: if (item.isArabseedSource()) ArabseedRepository.loadCachedSeasonBundle(item.pageUrl)
            else MultiProviderRepository.loadCachedSeasonBundle(item.pageUrl)

    private fun fetchSeasons(item: ArabseedItem, forceRefresh: Boolean): ArabseedSeasonBundle {
        val seriesItem = item.asParentSeries().copy(type = com.madar.tv.data.ArabseedType.SERIES)
        val cached = MultiProviderRepository.loadCachedMergedSeasonBundle(seriesItem.pageUrl)
            ?: if (seriesItem.isArabseedSource()) ArabseedRepository.loadCachedSeasonBundle(seriesItem.pageUrl)
            else MultiProviderRepository.loadCachedSeasonBundle(seriesItem.pageUrl)
        val cachedEpisodeCount = cached?.seasons?.sumOf { it.episodes.size } ?: 0
        val minSeason = cached?.seasons?.minOfOrNull { it.number } ?: 0
        val maxSeason = cached?.seasons?.maxOfOrNull { it.number } ?: 0
        val detectedSeason = SeriesCatalogParser.seasonNumber(seriesItem.title, seriesItem.pageUrl)
            ?: SeriesCatalogParser.seasonNumber(seriesItem.latestEpisodeLabel, "")
        val hasSeasonGaps = cached?.seasons?.map { it.number }?.sorted()?.zipWithNext()?.any { (a, b) -> b - a > 1 } ?: false
        val isMissingLower = minSeason > 1
        val isMissingDetected = detectedSeason != null && cached?.seasons?.none { it.number == detectedSeason } == true
        val isMissingIntermediate = maxSeason > (cached?.seasons?.size ?: 0)
        val hasEpisodeGaps = cached?.seasons?.any { season ->
            SeriesCatalogParser.needsEpisodeSequenceRepair(season.episodes.map { it.number })
        } ?: false
        val isCacheIncomplete = cached == null || isMissingLower || isMissingDetected || isMissingIntermediate || hasSeasonGaps || hasEpisodeGaps

        if (!forceRefresh && !isCacheIncomplete && cached != null && cachedEpisodeCount >= 10) return cached

        return if (seriesItem.isArabseedSource()) {
            val primary = ArabseedRepository.fetchSeasonsAndEpisodes(seriesItem, forceRefresh = true)
            MultiProviderRepository.fetchSeasonBundleAcrossProviders(seriesItem, primary, forceRefresh = true)
        } else {
            val primary = MultiProviderRepository.fetchSeasonsAndEpisodes(seriesItem, forceRefresh = true)
            MultiProviderRepository.fetchSeasonBundleAcrossProviders(seriesItem, primary, forceRefresh = true)
        }
    }
}

private fun ArabseedItem.isArabseedSource(): Boolean = when {
    // The page URL is the source of truth. Catalog rows can inherit a display
    // label from their parent even when an episode/season link belongs to a
    // different configured provider.
    isArabseedUrl(pageUrl) -> true
    MultiProviderRepository.isMultiProvider(pageUrl) -> false
    else -> providerName.isArabseedProviderName()
}

private fun String.isArabseedProviderName(): Boolean =
    trim().equals("عرب سيد", ignoreCase = true) ||
        trim().equals("المصدر الأساسي", ignoreCase = true) ||
        trim().equals("المصدر الاساسي", ignoreCase = true)

private fun isArabseedUrl(value: String): Boolean {
    val host = safeHttpUri(value)?.host?.lowercase().orEmpty()
    return host == "myseed.tv" || host.endsWith(".myseed.tv")
}

private fun safeHttpUri(value: String): android.net.Uri? {
    val trimmed = value.trim()
    val uri = runCatching { android.net.Uri.parse(trimmed) }.getOrNull() ?: return null
    if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
    return uri
}

private fun isAllowedResolverInput(value: String): Boolean {
    val uri = safeHttpUri(value) ?: return false
    val host = uri.host?.lowercase().orEmpty()
    val redirectDomains = listOf("google.com", "googleadservices.com", "doubleclick.net", "googlesyndication.com")
    return redirectDomains.none { host == it || host.endsWith(".$it") }
}

private fun ArabseedItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "title" to arabicTitle.ifBlank { title }, "originalTitle" to title,
    "arabicTitle" to arabicTitle, "poster" to posterUrl.orEmpty(), "posterUrl" to posterUrl.orEmpty(),
    "pageUrl" to pageUrl, "isSeries" to (type == ArabseedType.SERIES), "year" to year,
    "quality" to quality, "rating" to rating, "provider" to providerName,
    "providerName" to providerName, "overview" to overview, "category" to category,
    "parentSeriesTitle" to parentSeriesTitle.orEmpty(), "parentSeriesPageUrl" to parentSeriesPageUrl.orEmpty(),
    "latestEpisodeLabel" to latestEpisodeLabel,
    "alternatePageUrls" to alternatePageUrls,
)

private fun Map<String, Any?>.toItem(): ArabseedItem = ArabseedItem(
    id = this["id"].stringValue(), title = (this["originalTitle"] ?: this["title"]).stringValue(),
    arabicTitle = (this["arabicTitle"] ?: this["title"]).stringValue(),
    posterUrl = (this["posterUrl"] ?: this["poster"]).stringValue().ifBlank { null },
    year = this["year"].stringValue(), quality = this["quality"].stringValue(),
    rating = this["rating"].stringValue(),
    type = if (this["isSeries"] == true) ArabseedType.SERIES else ArabseedType.MOVIE,
    pageUrl = this["pageUrl"].stringValue(), category = this["category"].stringValue(),
    overview = this["overview"].stringValue(),
    providerName = (this["providerName"] ?: this["provider"]).stringValue().ifBlank { "عرب سيد" },
    parentSeriesTitle = this["parentSeriesTitle"].stringValue().ifBlank { null },
    parentSeriesPageUrl = this["parentSeriesPageUrl"].stringValue().ifBlank { null },
    latestEpisodeLabel = this["latestEpisodeLabel"].stringValue(),
    alternatePageUrls = (this["alternatePageUrls"] as? List<*>)
        ?.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }.orEmpty(),
)

/** Episode cards carry the canonical series URL. Season discovery must use that page. */
private fun ArabseedItem.asParentSeries(): ArabseedItem {
    val parentUrl = parentSeriesPageUrl?.takeIf(String::isNotBlank)
    val parentTitle = parentSeriesTitle?.takeIf(String::isNotBlank) ?: title
    // Always ensure type=SERIES so season aggregation runs regardless of card type
    if (parentUrl == null) return copy(type = com.madar.tv.data.ArabseedType.SERIES)
    return copy(
        title = parentTitle,
        arabicTitle = parentTitle,
        pageUrl = parentUrl,
        type = com.madar.tv.data.ArabseedType.SERIES,
        parentSeriesTitle = null,
        parentSeriesPageUrl = null,
        alternatePageUrls = emptyList(),
    )
}

private fun ArabseedDetail.toMap(): Map<String, Any?> = mapOf(
    "item" to item.toMap(), "servers" to servers.map { it.toMap() },
    "stream" to preResolvedStream.orEmpty(), "cast" to cast,
    "overview" to item.overview,
)

private fun ArabseedSeasonBundle.toMap(): Map<String, Any?> {
    val normalized = SeriesCatalogParser.normalizeSeasonBundle(this)
    return mapOf(
    "seasons" to normalized.seasons.map { season -> mapOf(
        "number" to season.number, "title" to season.title,
        "pageUrl" to season.pageUrl,
        "episodes" to season.episodes.sortedWith(compareBy<ArabseedEpisode> { it.seasonNumber }.thenBy { it.number }).map { episode ->
            mapOf("number" to episode.number, "title" to episode.title, "pageUrl" to episode.pageUrl, "seasonNumber" to episode.seasonNumber, "providerName" to episode.providerName, "alternatePageUrls" to episode.alternatePageUrls)
        },
    ) },
    "relatedParts" to normalized.relatedParts.map { it.toMap() },
)
}

private fun ArabseedServer.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "embedUrl" to embedUrl,
    "directStreamUrl" to directStreamUrl.orEmpty(),
    "sourcePageUrl" to sourcePageUrl,
    "sourceProviderName" to sourceProviderName,
)

private fun YacineCategory.toMap(): Map<String, Any?> = mapOf("id" to id, "name" to name, "logo" to logo, "childCount" to childCount)
private fun YacineChannel.toMap(): Map<String, Any?> = mapOf("id" to id, "name" to name, "logo" to logo, "isHide" to isHide, "priority" to priority)
private fun YacineEvent.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "champions" to champions, "channel" to channel, "startTime" to startTime,
    "endTime" to endTime, "commentary" to commentary,
    "team1" to mapOf("id" to team1.id, "name" to team1.name, "logo" to team1.logo),
    "team2" to mapOf("id" to team2.id, "name" to team2.name, "logo" to team2.logo),
)
private fun YacineStream.toMap(): Map<String, Any?> = mapOf(
    "name" to name, "url" to url, "userAgent" to userAgent, "referer" to referer,
    "urlType" to urlType, "headers" to headers,
)

private fun com.madar.tv.data.FootballMatch.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "leagueName" to leagueName, "leagueCode" to leagueCode,
    "homeTeam" to homeTeam, "homeTeamArabic" to homeTeamArabic, "homeLogo" to homeLogo,
    "homeScore" to homeScore, "awayTeam" to awayTeam, "awayTeamArabic" to awayTeamArabic,
    "awayLogo" to awayLogo, "awayScore" to awayScore, "timeOrStatus" to timeOrStatus,
    "state" to state, "matchDate" to matchDate, "kickoffEpochMs" to (kickoffEpochMs ?: 0L),
    "league" to league,
)

private fun Any?.stringValue(): String = this?.toString().orEmpty()
