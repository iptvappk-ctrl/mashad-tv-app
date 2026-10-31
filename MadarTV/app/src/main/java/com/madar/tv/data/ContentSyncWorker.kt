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
        val freshItems = MultiProviderRepository.mergeCatalogItems(
            ArabseedRepository.fetchMovies(page = 1) + MultiProviderRepository.fetchMoviesAll(page = 1),
            ArabseedType.MOVIE,
        )

        if (seenUrls.isEmpty()) {
            // First run — just save and don't notify
            FollowRepository.saveSeenMovies(context, freshItems.map { it.pageUrl }.toSet())
            return
        }

        val newItems = freshItems.filter { it.pageUrl !in seenUrls }

        if (newItems.isNotEmpty()) {
            if (newItems.size <= 2) {
                for (item in newItems) {
                    val yearStr = item.year.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                    NotificationHelper.notifyNewContent(
                        context = context,
                        title = item.title,
                        body = "فيلم جديد متاح الآن للمشاهدة على زقزوقة$yearStr",
                        isMovie = true
                    )
                }
            } else {
                NotificationHelper.notifyNewBatch(
                    context = context,
                    titles = newItems.map { it.title },
                    isMovie = true
                )
            }
        }

        // Update seen list (keep last 200)
        val allSeen = (seenUrls + freshItems.map { it.pageUrl }).toList().takeLast(200).toSet()
        FollowRepository.saveSeenMovies(context, allSeen)
    }

    // ── Check new series episodes ─────────────────────────────────────────────

    private suspend fun checkNewSeries() {
        val seenUrls   = FollowRepository.getSeenSeriesUrls(context)
        val freshItems = MultiProviderRepository.mergeCatalogItems(
            ArabseedRepository.fetchSeries(page = 1) + MultiProviderRepository.fetchSeriesAll(page = 1),
            ArabseedType.SERIES,
        )

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
            if (otherNew.size <= 2) {
                for (item in otherNew) {
                    NotificationHelper.notifyNewContent(
                        context = context,
                        title = item.title,
                        body = "مسلسل/حلقة جديدة متاحة الآن للمشاهدة على زقزوقة",
                        isMovie = false
                    )
                }
            } else {
                NotificationHelper.notifyNewBatch(
                    context = context,
                    titles = otherNew.map { it.title },
                    isMovie = false
                )
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
