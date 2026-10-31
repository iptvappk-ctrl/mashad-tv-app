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
            val page = (call.argument<Number>("page")?.toInt() ?: 1).coerceAtLeast(1)
            val force = call.argument<Boolean>("force") ?: false
            catalog(path, tab, page, method.endsWith("fetch"), force)
        }
        "catalog.primary.fetch" -> {
            val path = call.argument<String>("path").orEmpty().ifBlank { "main/" }
            val page = (call.argument<Number>("page")?.toInt() ?: 1).coerceAtLeast(1)
            ArabseedRepository.fetchCategory(path, page, forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        }
        "catalog.providers.fetch" -> {
            val path = call.argument<String>("path").orEmpty().ifBlank { "main/" }
            val tab = call.argument<String>("tab").orEmpty().ifBlank { "home" }
            val page = (call.argument<Number>("page")?.toInt() ?: 1).coerceAtLeast(1)
            val force = call.argument<Boolean>("force") ?: false
            providerCatalog(path, tab, page, force).map { it.toMap() }
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
        "media.seasons.cached", "media.seasons.fetch" -> {
            val item = call.argument<Map<String, Any?>>("item")?.toItem()
                ?: throw IllegalArgumentException("المسلسل غير صالح")
            val seriesItem = item.asParentSeries()
            val bundle = if (method.endsWith("cached")) loadCachedSeasons(seriesItem) else fetchSeasons(seriesItem)
            bundle?.toMap()
        }
        "media.servers" -> {
            val url = call.argument<String>("url").orEmpty()
            val provider = call.argument<String>("provider").orEmpty()
            val rows = if (provider == "عرب سيد") ArabseedRepository.fetchEpisodeServers(url)
                else MultiProviderRepository.fetchServers(url)
            rows.map { it.toMap() }
        }
        "media.resolve" -> ArabseedRepository.resolveDirectStream(call.argument<String>("url").orEmpty())
        "live.events.cached" -> (YacineRepository.getCachedEvents() ?: emptyList()).map { it.toMap() }
        "live.events.fetch" -> YacineRepository.getEvents(forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        "live.categories.cached" -> (YacineRepository.getCachedCategories() ?: emptyList()).map { it.toMap() }
        "live.categories.fetch" -> YacineRepository.getCategories(forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        "live.channels.cached" -> (YacineRepository.getCachedChannels(call.argument<String>("categoryId").orEmpty()) ?: emptyList()).map { it.toMap() }
        "live.channels.fetch" -> YacineRepository.getChannels(call.argument<String>("categoryId").orEmpty(), forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        "live.eventStreams" -> YacineRepository.getEventStreams(call.argument<String>("id").orEmpty()).map { it.toMap() }
        "live.channelStreams" -> YacineRepository.getChannelStreams(call.argument<String>("id").orEmpty()).map { it.toMap() }
        "football.cached" -> (FootballRepository.loadCachedMatches() ?: emptyList()).map { it.toMap() }
        "football.fetch" -> FootballRepository.fetchUpcomingMatches(forceRefresh = call.argument<Boolean>("force") ?: false).map { it.toMap() }
        else -> null
    }

    private fun catalog(path: String, tab: String, page: Int, fetch: Boolean, force: Boolean): List<Map<String, Any?>> {
        val expected = when (tab) {
            "series" -> ArabseedType.SERIES
            "movies" -> ArabseedType.MOVIE
            else -> null
        }
        val primary = when {
            !fetch -> ArabseedRepository.loadCachedCategory(path, page).orEmpty()
            else -> ArabseedRepository.fetchCategory(path, page, forceRefresh = force)
        }
        val providerRows = when (tab) {
            "series" -> if (fetch) MultiProviderRepository.fetchSeriesAll(page, forceRefresh = force) else MultiProviderRepository.loadCachedCatalog(ArabseedType.SERIES, page).orEmpty()
            "movies" -> if (fetch) MultiProviderRepository.fetchMoviesAll(page, forceRefresh = force) else MultiProviderRepository.loadCachedCatalog(ArabseedType.MOVIE, page).orEmpty()
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

    private fun providerCatalog(path: String, tab: String, page: Int, force: Boolean): List<ArabseedItem> = when (tab) {
        "series" -> MultiProviderRepository.fetchSeriesAll(page, forceRefresh = force)
        "movies" -> MultiProviderRepository.fetchMoviesAll(page, forceRefresh = force)
        "home" -> if (path == "main/" || path == "main") MultiProviderRepository.fetchCatalogAll(page, forceRefresh = force) else emptyList()
        else -> emptyList()
    }

    private fun loadCachedDetail(item: ArabseedItem): ArabseedDetail? =
        if (item.providerName == "عرب سيد") ArabseedRepository.loadCachedDetail(item.pageUrl)
        else MultiProviderRepository.loadCachedDetail(item.pageUrl)

    private fun fetchDetail(item: ArabseedItem): ArabseedDetail =
        if (item.providerName == "عرب سيد") ArabseedRepository.fetchDetail(item)
        else MultiProviderRepository.fetchDetail(item)

    private fun loadCachedSeasons(item: ArabseedItem): ArabseedSeasonBundle? =
        if (item.providerName == "عرب سيد") ArabseedRepository.loadCachedSeasonBundle(item.pageUrl)
        else MultiProviderRepository.loadCachedSeasonBundle(item.pageUrl)

    private fun fetchSeasons(item: ArabseedItem): ArabseedSeasonBundle =
        if (item.providerName == "عرب سيد") ArabseedRepository.fetchSeasonsAndEpisodes(item)
        else MultiProviderRepository.fetchSeasonsAndEpisodes(item)
}

private fun ArabseedItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "title" to arabicTitle.ifBlank { title }, "originalTitle" to title,
    "arabicTitle" to arabicTitle, "poster" to posterUrl.orEmpty(), "posterUrl" to posterUrl.orEmpty(),
    "pageUrl" to pageUrl, "isSeries" to (type == ArabseedType.SERIES), "year" to year,
    "quality" to quality, "rating" to rating, "provider" to providerName,
    "providerName" to providerName, "overview" to overview, "category" to category,
    "parentSeriesTitle" to parentSeriesTitle.orEmpty(), "parentSeriesPageUrl" to parentSeriesPageUrl.orEmpty(),
    "latestEpisodeLabel" to latestEpisodeLabel,
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
)

/** Episode cards carry the canonical series URL. Season discovery must use that page. */
private fun ArabseedItem.asParentSeries(): ArabseedItem {
    val parentUrl = parentSeriesPageUrl?.takeIf(String::isNotBlank) ?: return this
    val parentTitle = parentSeriesTitle?.takeIf(String::isNotBlank) ?: title
    return copy(
        title = parentTitle,
        arabicTitle = parentTitle,
        pageUrl = parentUrl,
        parentSeriesTitle = null,
        parentSeriesPageUrl = null,
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
        "episodes" to season.episodes.sortedWith(compareBy<ArabseedEpisode> { it.seasonNumber }.thenBy { it.number }).map { episode ->
            mapOf("number" to episode.number, "title" to episode.title, "pageUrl" to episode.pageUrl, "seasonNumber" to episode.seasonNumber)
        },
    ) },
    "relatedParts" to normalized.relatedParts.map { it.toMap() },
)
}

private fun ArabseedServer.toMap(): Map<String, Any?> = mapOf(
    "name" to name, "embedUrl" to embedUrl, "directStreamUrl" to directStreamUrl.orEmpty(),
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
