package com.madar.tv.notifications

import android.content.Context
import android.os.Build
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
            val isSeries = type == "series"
            // Send focused notification for each single item (up to 2 newest items)
            val itemsToNotify = fresh.take(2)
            for ((_, item) in itemsToNotify) {
                val notifTitle: String
                val notifBody: String
                if (isSeries) {
                    val epLabel = item.latestEpisodeLabel.trim()
                    if (epLabel.isNotBlank()) {
                        notifTitle = "📺 حلقة جديدة: ${item.title}"
                        notifBody = "$epLabel · شاهد الحلقة الآن بجودة عالية"
                    } else {
                        notifTitle = "📺 مسلسل جديد: ${item.title}"
                        notifBody = "بدأ عرض المسلسل الآن · متاح للمشاهدة والتحميل"
                    }
                } else {
                    notifTitle = "🎬 فيلم جديد: ${item.title}"
                    val yearStr = if (item.year.isNotBlank()) "(${item.year})" else ""
                    notifBody = "وصل الآن $yearStr للمشاهدة والتحميل بدون إعلانات"
                }
                val notifId = (item.pageUrl.hashCode() and 0x7fffffff) % 90000 + 10000
                NotificationCenter.postMediaNotification(
                    context = applicationContext,
                    id = notifId,
                    channel = NotificationCenter.CONTENT_CHANNEL,
                    title = notifTitle,
                    body = notifBody,
                    itemId = item.id.ifBlank { item.pageUrl },
                    itemTitle = item.title,
                    pageUrl = item.pageUrl,
                    isSeries = isSeries,
                    poster = item.posterUrl.orEmpty(),
                    year = item.year,
                    highPriority = true,
                )
            }
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

internal class AppUpdateNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!NotificationCenter.isChannelAllowed(applicationContext, NotificationCenter.UPDATE_CHANNEL)) return Result.success()
        return runCatching {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(FIREBASE_UPDATE_URL)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "Zaqzouka-Android")
                .build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            response.use {
                if (!it.isSuccessful) return@runCatching Result.retry()
                val body = it.body?.string() ?: return@runCatching Result.success()
                val json = JSONObject(body)
                val remoteVersionCode = json.optLong("versionCode", 0L)
                val versionName = json.optString("versionName", "")
                val releaseNotes = json.optString("releaseNotes", "")

                @Suppress("DEPRECATION")
                val installed = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) installed.longVersionCode else installed.versionCode.toLong()

                if (remoteVersionCode > currentVersionCode) {
                    val prefs = applicationContext.getSharedPreferences("app_update_notifications", Context.MODE_PRIVATE)
                    val lastNotifiedCode = prefs.getLong("last_notified_version_code", 0L)
                    if (remoteVersionCode > lastNotifiedCode) {
                        val posted = NotificationCenter.postUpdateNotification(
                            applicationContext,
                            versionName,
                            releaseNotes.ifBlank { "يتوفر تحديث جديد لتحسين استقرار التطبيق والمشاهدة." },
                        )
                        if (posted) {
                            prefs.edit().putLong("last_notified_version_code", remoteVersionCode).apply()
                        }
                    }
                }
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val FIREBASE_UPDATE_URL = "https://madartv-updates.web.app/updates/android/latest.json"
    }
}
