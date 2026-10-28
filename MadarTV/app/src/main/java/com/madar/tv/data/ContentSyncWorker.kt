package com.madar.tv.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.madar.tv.core.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Runs every 30 minutes in the background.
 * Fetches latest movies & series from all providers,
 * compares with cached seen items, notifies on NEW content.
 */
class ContentSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            checkNewMovies()
            checkNewSeries()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    // ── Check new movies ──────────────────────────────────────────────────────

    private suspend fun checkNewMovies() {
        val seenUrls   = FollowRepository.getSeenMovieUrls(context)
        val freshItems = MultiProviderRepository.fetchMoviesAll(page = 1)

        if (seenUrls.isEmpty()) {
            // First run — just save and don't notify
            FollowRepository.saveSeenMovies(context, freshItems.map { it.pageUrl }.toSet())
            return
        }

        val newItems = freshItems.filter { it.pageUrl !in seenUrls }

        if (newItems.isNotEmpty()) {
            if (newItems.size == 1) {
                NotificationHelper.notifyNewContent(
                    context,
                    newItems.first().title,
                    buildString {
                        append("فيلم جديد")
                        newItems.first().year.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    isMovie = true
                )
            } else {
                NotificationHelper.notifyNewBatch(context, newItems.size, "فيلم")
            }
        }

        // Update seen list (keep last 200)
        val allSeen = (seenUrls + freshItems.map { it.pageUrl }).toList().takeLast(200).toSet()
        FollowRepository.saveSeenMovies(context, allSeen)
    }

    // ── Check new series episodes ─────────────────────────────────────────────

    private suspend fun checkNewSeries() {
        val seenUrls   = FollowRepository.getSeenSeriesUrls(context)
        val freshItems = MultiProviderRepository.fetchSeriesAll(page = 1)

        if (seenUrls.isEmpty()) {
            FollowRepository.saveSeenSeries(context, freshItems.map { it.pageUrl }.toSet())
            return
        }

        val newItems = freshItems.filter { it.pageUrl !in seenUrls }

        // Also check followed series for new episodes
        val followedUrls = FollowRepository.getFollowedSeries(context).first()

        // Notify about new episodes of followed series
        val followedNew = newItems.filter { item ->
            followedUrls.any { followed ->
                // Match by series base URL (without episode-specific parts)
                val seriesBase = followed.substringBeforeLast("/").substringBeforeLast("-")
                item.pageUrl.contains(seriesBase, ignoreCase = true) ||
                normalizeForMatch(item.title) == normalizeForMatch(
                    // We don't have the followed title here, compare URLs
                    followed.substringAfterLast("/").replace("-", " ")
                )
            }
        }

        for (item in followedNew) {
            NotificationHelper.notifyNewContent(
                context,
                item.title,
                "حلقة جديدة متاحة الآن 📺",
                isMovie = false
            )
        }

        // Notify about other new series (not followed)
        val otherNew = newItems - followedNew.toSet()
        if (otherNew.isNotEmpty()) {
            if (otherNew.size == 1) {
                NotificationHelper.notifyNewContent(
                    context,
                    otherNew.first().title,
                    "مسلسل/حلقة جديدة أُضيفت",
                    isMovie = false
                )
            } else {
                NotificationHelper.notifyNewBatch(context, otherNew.size, "مسلسل")
            }
        }

        val allSeen = (seenUrls + freshItems.map { it.pageUrl }).toList().takeLast(200).toSet()
        FollowRepository.saveSeenSeries(context, allSeen)
    }

    private fun normalizeForMatch(s: String) = s
        .lowercase()
        .replace(Regex("[^\\w\\u0600-\\u06FF]"), "")
        .trim()
}
