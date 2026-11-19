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
 * Checks football matches periodically:
 * 1. Fires an alert WHEN THE MATCH STARTS (live kick-off)!
 * 2. Fires an alert 15–35 minutes before kick-off.
 */
class MatchAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val matches = FootballRepository.fetchUpcomingMatches(forceRefresh = true)
            val now = System.currentTimeMillis()
            val prefs = context.getSharedPreferences("match_alert_sent", Context.MODE_PRIVATE)

            for (match in matches) {
                val homeName = match.homeTeamArabic.ifBlank { match.homeTeam }
                val awayName = match.awayTeamArabic.ifBlank { match.awayTeam }
                val league = match.leagueName.ifBlank { match.league }
                val kickoffMs = match.kickoffEpochMs
                val diffMin = if (kickoffMs != null) (kickoffMs - now) / 60_000 else null

                // ── 1. MATCH START ALERT (Live Now / Kicking off) ────────────
                val isStartingNow = match.state == "in" || (diffMin != null && diffMin in -15..5)
                val startSentKey = "started_${match.id}"
                if (isStartingNow && !prefs.getBoolean(startSentKey, false)) {
                    NotificationHelper.notifyMatchStart(
                        context = context,
                        homeTeam = homeName,
                        awayTeam = awayName,
                        league = league
                    )
                    prefs.edit().putBoolean(startSentKey, true).apply()
                }

                // ── 2. UPCOMING ALERT (15–35 minutes before kickoff) ─────────
                val upcomingSentKey = "upcoming_${match.id}"
                if (kickoffMs != null && diffMin != null && diffMin in 15..35 && !prefs.getBoolean(upcomingSentKey, false)) {
                    val timeStr = formatKickoff(kickoffMs)
                    NotificationHelper.notifyMatch(
                        context = context,
                        homeTeam = homeName,
                        awayTeam = awayName,
                        timeStr = timeStr,
                        league = league
                    )
                    prefs.edit().putBoolean(upcomingSentKey, true).apply()
                }
            }

            // Cleanup old keys if prefs grows large
            val keys = prefs.all.keys
            if (keys.size > 200) {
                prefs.edit().clear().apply()
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
