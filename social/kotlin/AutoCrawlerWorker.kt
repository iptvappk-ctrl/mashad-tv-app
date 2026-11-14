package com.madar.tv.social

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AutoCrawlerWorker
 * ─────────────────
 * Android WorkManager periodic worker.
 * Runs every 3 minutes, scans 6 major Arabic streaming sites for
 * new episodes/movies, and fires a notification when something new is found.
 *
 * HOW TO REGISTER (paste in Application.onCreate or MainActivity.onCreate):
 * ─────────────────────────────────────────────────────────────────────────
 *   AutoCrawlerWorker.schedulePeriodicWork(context)
 *
 * HOW TO STOP:
 *   AutoCrawlerWorker.cancelWork(context)
 *
 * NEW RELEASES CALLBACK:
 *   AutoCrawlerWorker.onNewRelease = { release ->
 *       // release is a JSONObject: { title, pageUrl, type, provider, quality }
 *   }
 */
class AutoCrawlerWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_TAG          = "zakzouka_auto_crawler"
        const val CHANNEL_ID        = "zakzouka_new_releases"
        const val CHANNEL_NAME      = "زقزوقة — إصدارات جديدة"
        private const val PREF_KEY  = "auto_crawler_seen"

        // Callback — set this before scheduling to receive new releases
        var onNewRelease: ((JSONObject) -> Unit)? = null

        // ── WorkManager registration ──────────────────────────────────────
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoCrawlerWorker>(
                15, TimeUnit.MINUTES,          // minimum is 15 min on Android
                5, TimeUnit.MINUTES,           // flex interval
            )
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 2, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }

    // ── Sites to crawl ────────────────────────────────────────────────────────
    private val CRAWL_TARGETS = listOf(
        CrawlTarget("arabseed",  "https://arabseed1.ink/"),
        CrawlTarget("wecima",    "https://wecima.show/"),
        CrawlTarget("faselhd",   "https://www.faselhd.club/"),
        CrawlTarget("egybest",   "https://www.egybest.in/"),
        CrawlTarget("akwam",     "https://akwam.to/"),
        CrawlTarget("cima4u",    "https://www.cima4u.pw/"),
    )

    data class CrawlTarget(val name: String, val baseUrl: String)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val linker = SmartMediaLinker()

    // ── Main work ─────────────────────────────────────────────────────────────
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val newReleases = mutableListOf<JSONObject>()
            val seen = loadSeenIds()

            for (target in CRAWL_TARGETS) {
                try {
                    val items = crawlSite(target)
                    for (item in items) {
                        val id = item.optString("id")
                        if (id.isNotEmpty() && !seen.contains(id)) {
                            seen.add(id)
                            newReleases.add(item)
                            linker.registerItem(item)
                            onNewRelease?.invoke(item)
                        }
                    }
                } catch (_: Exception) {
                    // Site unreachable — skip, don't fail entire job
                }
            }

            saveSeenIds(seen)

            if (newReleases.isNotEmpty()) {
                showNotification(newReleases)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    // ── Crawl a single site ───────────────────────────────────────────────────
    private fun crawlSite(target: CrawlTarget): List<JSONObject> {
        val html = fetchHtml(target.baseUrl) ?: return emptyList()
        return when (target.name) {
            "arabseed" -> parseArabseedLatest(html, target.baseUrl)
            "wecima"   -> parseWecimaLatest(html, target.baseUrl)
            "faselhd"  -> parseFaselhdLatest(html, target.baseUrl)
            "egybest"  -> parseEgybestLatest(html, target.baseUrl)
            "akwam"    -> parseAkwamLatest(html, target.baseUrl)
            "cima4u"   -> parseCima4uLatest(html, target.baseUrl)
            else       -> parseGenericLatest(html, target.baseUrl, target.name)
        }
    }

    // ── Generic HTML parser (works for most Arabic streaming sites) ───────────
    private fun parseGenericLatest(html: String, baseUrl: String, provider: String): List<JSONObject> {
        val items = mutableListOf<JSONObject>()
        // Most sites use <article class="..."> or <div class="...episode...">
        val articleRx = Regex(
            """<a[^>]+href="(https?://[^"]*(?:episode|حلقة|ep[-_]\d|s\d+e\d+)[^"]*)"[^>]*>""" +
                    """.*?(?:alt|title)="([^"]{5,100})"""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        articleRx.findAll(html).take(20).forEach { m ->
            val url   = m.groupValues[1]
            val title = m.groupValues[2].trim()
            if (title.isNotEmpty() && url.isNotEmpty()) {
                items.add(
                    JSONObject()
                        .put("id", "$provider:${url.takeLast(60)}")
                        .put("title", title)
                        .put("pageUrl", url)
                        .put("providerName", provider)
                        .put("type", if (title.containsEpisodeKeyword()) "series" else "movie")
                )
            }
        }
        return items.distinctBy { it.optString("id") }
    }

    // ── Site-specific parsers ─────────────────────────────────────────────────
    private fun parseArabseedLatest(html: String, base: String) =
        parseGenericLatest(html, base, "arabseed")

    private fun parseWecimaLatest(html: String, base: String) =
        parseGenericLatest(html, base, "wecima")

    private fun parseFaselhdLatest(html: String, base: String) =
        parseGenericLatest(html, base, "faselhd")

    private fun parseEgybestLatest(html: String, base: String) =
        parseGenericLatest(html, base, "egybest")

    private fun parseAkwamLatest(html: String, base: String) =
        parseGenericLatest(html, base, "akwam")

    private fun parseCima4uLatest(html: String, base: String) =
        parseGenericLatest(html, base, "cima4u")

    // ── Notification ──────────────────────────────────────────────────────────
    private fun showNotification(releases: List<JSONObject>) {
        createNotificationChannel()

        val bigText = releases.take(5).joinToString("\n") { r ->
            "• ${r.optString("title", "جديد")} (${r.optString("providerName")})"
        }
        val more = if (releases.size > 5) "\n+${releases.size - 5} عناصر أخرى" else ""

        // Intent to open the app (adjust package / activity as needed)
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("زقزوقة — ${releases.size} إصدار جديد")
            .setContentText(releases.firstOrNull()?.optString("title") ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText + more))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    // ── Persistence: track seen item IDs ─────────────────────────────────────
    private fun loadSeenIds(): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE)
        return prefs.getStringSet("ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveSeenIds(ids: Set<String>) {
        val prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE)
        // Keep at most 2000 entries to avoid bloat
        val trimmed = if (ids.size > 2000) ids.takeLast(2000).toSet() else ids
        prefs.edit().putStringSet("ids", trimmed).apply()
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────
    private fun fetchHtml(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile)")
                .header("Accept-Language", "ar,en;q=0.9")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) resp.body?.string() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun String.containsEpisodeKeyword(): Boolean {
        val lower = lowercase()
        return lower.contains("حلقة") || lower.contains("حلقه") ||
                lower.contains("episode") || Regex("""s\d+e\d+""").containsMatchIn(lower)
    }

    private val NOTIFICATION_ID = 7777
}
