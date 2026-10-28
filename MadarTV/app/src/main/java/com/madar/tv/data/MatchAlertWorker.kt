package com.madar.tv.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.madar.tv.core.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Runs every hour — checks upcoming football matches within the next 3 hours
 * and fires a notification 30 minutes before kick-off.
 */
class MatchAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val matches = FootballRepository.fetchUpcomingMatches()
            val now = System.currentTimeMillis()

            for (match in matches) {
                val kickoffMs = match.kickoffEpochMs ?: continue
                val diffMin   = (kickoffMs - now) / 60_000

                // Notify if match starts in 25–35 minutes (30 min window)
                if (diffMin in 25..35) {
                    val timeStr = formatKickoff(kickoffMs)
                    NotificationHelper.notifyMatch(
                        context,
                        homeTeam = match.homeTeam,
                        awayTeam = match.awayTeam,
                        timeStr  = timeStr,
                        league   = match.league
                    )
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun formatKickoff(epochMs: Long): String {
        return try {
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(epochMs),
                ZoneId.of("Africa/Cairo")
            )
            DateTimeFormatter.ofPattern("HH:mm").format(zdt)
        } catch (_: Exception) { "—" }
    }
}
