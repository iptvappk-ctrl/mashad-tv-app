package com.madar.tv.data

import android.net.Uri
import com.madar.tv.core.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LiveScoreSnapshot(
    val matches: List<LiveMatch>,
    val failedLeagues: Int,
)

object LiveScoreRepository {

    // ═══════════════════════════════════════════════════════════════════════
    // ALL CONSTANTS — 6-layer encrypted via X.s() (v6 engine)
    // ═══════════════════════════════════════════════════════════════════════

    private val _ab = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 202.toByte(), 199.toByte(), 82, 86, 105, 225.toByte(), 239.toByte(), 116, 92, 171.toByte(), 160.toByte(), 28, 152.toByte(), 11, 32, 38, 181.toByte(), 23, 66, 65, 88, 81, 87, 231.toByte())
    private val _ua = byteArrayOf(241.toByte(), 158.toByte(), 19, 168.toByte(), 40, 64, 72, 149.toByte(), 103, 103, 241.toByte(), 112, 155.toByte(), 99, 103, 119, 246.toByte(), 10, 131.toByte())
    private val _ah = byteArrayOf(247.toByte(), 157.toByte(), 146.toByte(), 170.toByte(), 43, 48)
    private val _aj = byteArrayOf(135.toByte(), 22, 25, 46, 167.toByte(), 185.toByte(), 189.toByte(), 62, 203.toByte(), 199.toByte(), 88, 241.toByte(), 108, 233.toByte(), 228.toByte(), 125)
    private val _uh = byteArrayOf(253.toByte(), 149.toByte(), 147.toByte(), 35, 133.toByte(), 202.toByte(), 176.toByte(), 182.toByte(), 78, 76)

    // ── Decoys ────────────────────────────────────────────────────────────
    @Suppress("unused") private val _dc1 = byteArrayOf(42, 201.toByte(), 77, 13, 58, 190.toByte())

    // ── Lazily resolved ───────────────────────────────────────────────────
    private val API_BASE: String by lazy { X.s(_ab) }
    private val UA_VAL: String by lazy { X.s(_ua) }
    private val ACCEPT_HDR: String by lazy { X.s(_ah) }
    private val ACCEPT_JSON: String by lazy { X.s(_aj) }
    private val UA_HDR: String by lazy { X.s(_uh) }

    private val leagues = listOf(
        LeagueEndpoint("eng.1", "الدوري الإنجليزي"),
        LeagueEndpoint("esp.1", "الدوري الإسباني"),
        LeagueEndpoint("ita.1", "الدوري الإيطالي"),
        LeagueEndpoint("ger.1", "الدوري الألماني"),
        LeagueEndpoint("fra.1", "الدوري الفرنسي"),
    )

    suspend fun fetchLiveMatches(): LiveScoreSnapshot = coroutineScope {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val responses = leagues.map { league ->
            async(Dispatchers.IO) {
                runCatching { fetchLeague(league, date) }
            }
        }.awaitAll()

        val successful = responses.mapNotNull { it.getOrNull() }
        if (successful.isEmpty()) throw IOException("Live score service is unavailable")

        LiveScoreSnapshot(
            matches = successful.flatten().sortedBy { leagues.indexOfFirst { league -> league.name == it.league } },
            failedLeagues = responses.count { it.isFailure },
        )
    }

    private fun fetchLeague(league: LeagueEndpoint, date: String): List<LiveMatch> {
        val uri = Uri.parse("$API_BASE/${league.slug}/scoreboard")
            .buildUpon()
            .appendQueryParameter("dates", date)
            .build()
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty(ACCEPT_HDR, ACCEPT_JSON)
            setRequestProperty(UA_HDR, UA_VAL)
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Live scores response: ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val events = JSONObject(body).optJSONArray("events") ?: return emptyList()
            return buildList {
                for (index in 0 until events.length()) {
                    val event = events.optJSONObject(index) ?: continue
                    val competition = event.optJSONArray("competitions")?.optJSONObject(0) ?: continue
                    val status = event.optJSONObject("status")
                        ?: competition.optJSONObject("status")
                        ?: continue
                    val state = status.optJSONObject("type")?.optString("state").orEmpty()
                    if (state != "in") continue

                    val competitors = competition.optJSONArray("competitors") ?: continue
                    var home: JSONObject? = null
                    var away: JSONObject? = null
                    for (teamIndex in 0 until competitors.length()) {
                        val competitor = competitors.optJSONObject(teamIndex) ?: continue
                        when (competitor.optString("homeAway")) {
                            "home" -> home = competitor
                            "away" -> away = competitor
                        }
                    }
                    val homeTeam = home?.optJSONObject("team") ?: continue
                    val awayTeam = away?.optJSONObject("team") ?: continue
                    val id = event.optString("id").takeIf(String::isNotBlank) ?: continue

                    add(
                        LiveMatch(
                            id = "${league.slug}_$id",
                            league = league.name,
                            home = homeTeam.optString("displayName").ifBlank { homeTeam.optString("name") },
                            away = awayTeam.optString("displayName").ifBlank { awayTeam.optString("name") },
                            homeMark = homeTeam.optString("abbreviation").ifBlank { "؟" },
                            awayMark = awayTeam.optString("abbreviation").ifBlank { "؟" },
                            homeScore = home?.optString("score")?.toIntOrNull() ?: 0,
                            awayScore = away?.optString("score")?.toIntOrNull() ?: 0,
                            minute = status.optString("displayClock").ifBlank { "مباشر" },
                        ),
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private data class LeagueEndpoint(val slug: String, val name: String)
}
