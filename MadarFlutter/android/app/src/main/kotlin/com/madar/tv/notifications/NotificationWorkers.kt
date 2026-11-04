package com.madar.tv.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.madar.tv.data.ArabseedItem
import com.madar.tv.data.ArabseedRepository
import com.madar.tv.data.ArabseedType
import com.madar.tv.data.CatalogDiskCache
import com.madar.tv.data.FootballRepository
import com.madar.tv.data.MultiProviderRepository
import com.madar.tv.data.SeriesCatalogParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal class NewContentNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!NotificationCenter.isChannelAllowed(applicationContext, NotificationCenter.CONTENT_CHANNEL)) return Result.success()
        return runCatching {
            CatalogDiskCache.initialize(applicationContext)
            val (movies, series) = withContext(Dispatchers.IO) {
                coroutineScope {
                    val primaryMovies = async { runCatching { ArabseedRepository.fetchMovies(1) }.getOrDefault(emptyList()) }
                    val providerMovies = async { runCatching { MultiProviderRepository.fetchMoviesAll(1) }.getOrDefault(emptyList()) }
                    val primarySeries = async { runCatching { ArabseedRepository.fetchSeries(1) }.getOrDefault(emptyList()) }
                    val providerSeries = async { runCatching { MultiProviderRepository.fetchSeriesAll(1) }.getOrDefault(emptyList()) }
                    val allMovies = primaryMovies.await() + providerMovies.await()
                    val allSeries = primarySeries.await() + providerSeries.await()
                    MultiProviderRepository.mergeCatalogItems(allMovies, ArabseedType.MOVIE) to
                        MultiProviderRepository.mergeCatalogItems(allSeries, ArabseedType.SERIES)
                }
            }
            notifyNewRows("movies", movies.map { it to false })
            notifyNewRows("series", series.map { it to true })
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun notifyNewRows(type: String, rows: List<Pair<ArabseedItem, Boolean>>) {
        if (rows.isEmpty()) return
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "seen_$type"
        val seen = prefs.getString(key, null)?.let { raw ->
            runCatching {
                JSONArray(raw).let { array ->
                    buildSet { for (index in 0 until array.length()) add(array.optString(index)) }
                }
            }.getOrNull()
        }
        val keyed = rows.mapNotNull { (item, isSeries) -> contentKey(item, isSeries)?.let { it to item } }
        if (keyed.isEmpty()) return
        val currentKeys = keyed.map { it.first }.toSet()
        if (seen == null) {
            saveKeys(prefs, key, currentKeys)
            return
        }
        val fresh = keyed.filter { it.first !in seen }.distinctBy { it.first }
        if (fresh.isNotEmpty()) {
            val kind = if (type == "series") "مسلسل أو حلقة جديدة" else "فيلم جديد"
            val names = fresh.take(3).joinToString("، ") { it.second.title }
            val extra = if (fresh.size > 3) " وأعمال أخرى" else ""
            val posted = NotificationCenter.post(
                applicationContext,
                if (type == "series") CONTENT_SERIES_ID else CONTENT_MOVIES_ID,
                NotificationCenter.CONTENT_CHANNEL,
                "وصل $kind",
                "$names$extra",
            )
            if (!posted) return
        }
        saveKeys(prefs, key, seen + currentKeys)
    }

    private fun contentKey(item: ArabseedItem, isSeries: Boolean): String? {
        val title = SeriesCatalogParser.normalizedSeriesTitle(item.title)
        if (title.isBlank()) return null
        val year = item.year.filter(Char::isDigit)
        if (!isSeries) return "m|$title|$year"
        val label = item.latestEpisodeLabel.ifBlank { item.title }
        val season = SeriesCatalogParser.seasonNumber(label, item.pageUrl) ?: 0
        val episode = SeriesCatalogParser.episodeNumber(label, item.pageUrl) ?: 0
        val episodeKey = if (episode > 0) "$season|$episode" else "${SeriesCatalogParser.normalizedSeriesTitle(label)}"
        return "s|$title|$year|$episodeKey"
    }

    private fun saveKeys(prefs: android.content.SharedPreferences, key: String, keys: Set<String>) {
        val json = JSONArray()
        keys.toList().takeLast(MAX_SEEN_KEYS).forEach { json.put(it) }
        prefs.edit().putString(key, json.toString()).apply()
    }

    companion object {
        private const val PREFS = "background_notifications_v1"
        private const val MAX_SEEN_KEYS = 1200
        private const val CONTENT_MOVIES_ID = 8201
        private const val CONTENT_SERIES_ID = 8202
    }
}

internal class MatchAlertNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!NotificationCenter.isChannelAllowed(applicationContext, NotificationCenter.MATCH_CHANNEL)) return Result.success()
        return runCatching {
            CatalogDiskCache.initialize(applicationContext)
            val matches = withContext(Dispatchers.IO) { FootballRepository.fetchUpcomingMatches(forceRefresh = true) }
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            matches.forEach { match ->
                val isLive = match.state == "in"
                val kickoff = match.kickoffEpochMs ?: 0L
                val minutesToKickoff = if (kickoff > 0) ((kickoff - now) / 60_000L).toInt() else Int.MAX_VALUE
                if (!isLive && (kickoff <= 0 || minutesToKickoff !in 0..30)) return@forEach
                val key = "sent_${match.id}_${if (kickoff > 0) kickoff else match.matchDate}"
                if (prefs.getBoolean(key, false)) return@forEach
                val teams = "${match.homeTeamArabic.ifBlank { match.homeTeam }} × ${match.awayTeamArabic.ifBlank { match.awayTeam }}"
                val title = if (isLive) "بدأت المباراة" else "مباراة بعد $minutesToKickoff دقيقة"
                val body = "$teams · ${match.leagueName}"
                val posted = NotificationCenter.post(
                    applicationContext,
                    9000 + match.id.hashCode().and(0x7fffffff) % 100000,
                    NotificationCenter.MATCH_CHANNEL,
                    title,
                    body,
                    highPriority = true,
                )
                if (posted) prefs.edit().putBoolean(key, true).apply()
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val PREFS = "background_notifications_v1"
    }
}
