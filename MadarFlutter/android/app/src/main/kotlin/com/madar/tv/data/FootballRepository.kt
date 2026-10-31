package com.madar.tv.data

import com.madar.tv.core.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object FootballRepository {

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy encoded constants; values are recoverable from the client APK.
    // Pipeline: XOR(K1)→ROT-L(3)→XOR(K2)→Caesar(+17)→Nibble-Swap→PosXOR(K3)
    // ═══════════════════════════════════════════════════════════════════════

    private val _e1: ByteArray = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 196.toByte(), 194.toByte(), 83, 210.toByte(), 74, 226.toByte(), 109, 248.toByte(), 86, 12, 10, 144.toByte(), 146.toByte(), 139.toByte(), 46, 37, 57)
    private val _e2: ByteArray = byteArrayOf(166.toByte(), 158.toByte(), 25, 172.toByte(), 168.toByte(), 155.toByte(), 182.toByte(), 176.toByte(), 69, 196.toByte(), 248.toByte(), 85, 64, 195.toByte(), 238.toByte(), 116, 246.toByte(), 133.toByte(), 139.toByte(), 19, 50, 33, 44, 39, 60, 52, 194.toByte(), 97, 121, 89, 116, 103, 100, 127, 250.toByte(), 138.toByte(), 0, 155.toByte(), 157.toByte(), 31, 45)
    private val _uh: ByteArray = byteArrayOf(253.toByte(), 149.toByte(), 147.toByte(), 35, 133.toByte(), 202.toByte(), 176.toByte(), 182.toByte(), 78, 76)
    private val _uv: ByteArray = byteArrayOf(241.toByte(), 159.toByte(), 30, 172.toByte(), 37, 60, 189.toByte(), 149.toByte(), 229.toByte(), 103, 113)
    private val _ah: ByteArray = byteArrayOf(247.toByte(), 157.toByte(), 146.toByte(), 170.toByte(), 43, 48)
    private val _aj: ByteArray = byteArrayOf(135.toByte(), 22, 25, 46, 167.toByte(), 185.toByte(), 189.toByte(), 62, 203.toByte(), 199.toByte(), 88, 241.toByte(), 108, 233.toByte(), 228.toByte(), 125)

    // ── Decoys ────────────────────────────────────────────────────────────
    @Suppress("unused") private val _dc1 = byteArrayOf(19, 243.toByte(), 77, 91, 99, 235.toByte(), 161.toByte(), 73, 211.toByte())
    @Suppress("unused") private val _dc2 = byteArrayOf(92, 107, 139.toByte(), 3, 27, 115, 65, 51)

    // ── Lazily resolved ───────────────────────────────────────────────────
    private val EP: String by lazy { X.j(_e1, _e2) }
    private val UH: String by lazy { X.s(_uh) }
    private val UV: String by lazy { X.s(_uv) }
    private val AH: String by lazy { X.s(_ah) }
    private val AJ: String by lazy { X.s(_aj) }

    // ═══════════════════════════════════════════════════════════════════════

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val LEAGUES = listOf(
        Pair("eng.1", "الدوري الإنجليزي"),
        Pair("esp.1", "الدوري الإسباني"),
        Pair("ita.1", "الدوري الإيطالي"),
        Pair("ger.1", "الدوري الألماني"),
        Pair("fra.1", "الدوري الفرنسي"),
        Pair("uefa.champions", "دوري أبطال أوروبا"),
    )

    private var cachedMatches: List<FootballMatch> = emptyList()
    private var lastFetchTime = 0L
    private const val CACHE_TTL = 3 * 60 * 1000L // 3 minutes
    private const val DISK_STALE_MS = 4L * 60 * 60 * 1000

    fun loadCachedMatches(): List<FootballMatch>? {
        if (cachedMatches.isNotEmpty()) return cachedMatches
        val rows = CatalogDiskCache.loadJson("football:matches", DISK_STALE_MS)?.value?.optJSONArray("items") ?: return null
        val loaded = buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                add(FootballMatch(
                    id = row.optString("id"),
                    leagueName = row.optString("leagueName"),
                    leagueCode = row.optString("leagueCode"),
                    homeTeam = row.optString("homeTeam"),
                    homeTeamArabic = row.optString("homeTeamArabic"),
                    homeLogo = row.optString("homeLogo"),
                    homeScore = row.optString("homeScore"),
                    awayTeam = row.optString("awayTeam"),
                    awayTeamArabic = row.optString("awayTeamArabic"),
                    awayLogo = row.optString("awayLogo"),
                    awayScore = row.optString("awayScore"),
                    timeOrStatus = row.optString("timeOrStatus"),
                    state = row.optString("state", "pre"),
                    matchDate = row.optString("matchDate"),
                    kickoffEpochMs = row.optLong("kickoffEpochMs").takeIf { it > 0L },
                    league = row.optString("league"),
                ))
            }
        }
        if (loaded.isNotEmpty()) cachedMatches = loaded
        return loaded.takeIf { it.isNotEmpty() }
    }

    private fun persistMatches(matches: List<FootballMatch>) {
        val rows = org.json.JSONArray().apply {
            matches.forEach { match ->
                put(JSONObject()
                    .put("id", match.id).put("leagueName", match.leagueName).put("leagueCode", match.leagueCode)
                    .put("homeTeam", match.homeTeam).put("homeTeamArabic", match.homeTeamArabic).put("homeLogo", match.homeLogo)
                    .put("homeScore", match.homeScore).put("awayTeam", match.awayTeam).put("awayTeamArabic", match.awayTeamArabic)
                    .put("awayLogo", match.awayLogo).put("awayScore", match.awayScore).put("timeOrStatus", match.timeOrStatus)
                    .put("state", match.state).put("matchDate", match.matchDate)
                    .put("kickoffEpochMs", match.kickoffEpochMs ?: 0L).put("league", match.league))
            }
        }
        CatalogDiskCache.saveJson("football:matches", JSONObject().put("items", rows))
    }

    suspend fun fetchUpcomingMatches(forceRefresh: Boolean = false): List<FootballMatch> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedMatches.isNotEmpty() && (now - lastFetchTime < CACHE_TTL)) {
            return cachedMatches
        }

        return try {
            coroutineScope {
                val deferreds = LEAGUES.map { (code, name) ->
                    async(Dispatchers.IO) {
                        fetchLeagueMatches(code, name)
                    }
                }
                val all = deferreds.awaitAll().flatten()
                val sorted = all.sortedWith(
                    compareBy<FootballMatch> {
                        when (it.state) {
                            "in" -> 0
                            "pre" -> 1
                            else -> 2
                        }
                    }.thenBy { it.matchDate }
                )
                if (sorted.isNotEmpty()) {
                    cachedMatches = sorted
                    lastFetchTime = now
                    withContext(Dispatchers.IO) { persistMatches(sorted) }
                }
                if (sorted.isNotEmpty()) sorted else cachedMatches
            }
        } catch (_: Exception) {
            cachedMatches
        }
    }

    private fun fetchLeagueMatches(code: String, leagueName: String): List<FootballMatch> {
        val url = EP.format(code)
        val allMatches = mutableListOf<FootballMatch>()
        val seenIds = mutableSetOf<String>()

        try {
            val req = Request.Builder()
                .url(url)
                .header(UH, UV)
                .header(AH, AJ)
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val events = json.optJSONArray("events")
            if (events != null) {
                val parsed = parseEvents(events, code, leagueName)
                for (m in parsed) {
                    if (seenIds.add(m.id)) allMatches.add(m)
                }
            }

            // Calendar check for next upcoming matchday
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val todayIso = sdfUtc.format(Date())

            val leaguesArr = json.optJSONArray("leagues")
            val leagueObj = leaguesArr?.optJSONObject(0)
            val calendar = leagueObj?.optJSONArray("calendar")

            var nextDateStr: String? = null
            if (calendar != null) {
                for (i in 0 until calendar.length()) {
                    val d = calendar.optString(i, "")
                    if (d.length >= 10 && d.substring(0, 10) >= todayIso) {
                        nextDateStr = d.substring(0, 10).replace("-", "")
                        break
                    }
                }
            }

            if (!nextDateStr.isNullOrBlank()) {
                try {
                    val upUrl = if (url.contains("?")) "$url&dates=$nextDateStr" else "$url?dates=$nextDateStr"
                    val upReq = Request.Builder().url(upUrl).header(UH, UV).header(AH, AJ).build()
                    val upResp = client.newCall(upReq).execute()
                    val upBody = upResp.body?.string() ?: ""
                    val upJson = JSONObject(upBody)
                    val upEvents = upJson.optJSONArray("events")
                    if (upEvents != null) {
                        val parsedUp = parseEvents(upEvents, code, leagueName)
                        for (m in parsedUp) {
                            if (seenIds.add(m.id)) allMatches.add(m)
                        }
                    }
                } catch (_: Exception) {}
            } else if (code == "uefa.champions") {
                // Check next 4 Tuesdays for Champions League matchdays
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                for (i in 1..28) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
                        val dStr = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(cal.time)
                        try {
                            val uclUrl = if (url.contains("?")) "$url&dates=$dStr" else "$url?dates=$dStr"
                            val uclReq = Request.Builder().url(uclUrl).header(UH, UV).header(AH, AJ).build()
                            val uclResp = client.newCall(uclReq).execute()
                            val uclBody = uclResp.body?.string() ?: ""
                            val uclJson = JSONObject(uclBody)
                            val uclEvents = uclJson.optJSONArray("events")
                            if (uclEvents != null && uclEvents.length() > 0) {
                                val parsedUcl = parseEvents(uclEvents, code, leagueName)
                                for (m in parsedUcl) {
                                    if (seenIds.add(m.id)) allMatches.add(m)
                                }
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}

        return allMatches
    }

    private fun parseEvents(events: org.json.JSONArray, code: String, leagueName: String): List<FootballMatch> {
        val list = mutableListOf<FootballMatch>()
        for (i in 0 until events.length()) {
            val ev = events.getJSONObject(i)
            val id = ev.optString("id")
            val dateStr = ev.optString("date")
            val comps = ev.optJSONArray("competitions") ?: continue
            if (comps.length() == 0) continue
            val comp = comps.getJSONObject(0)

            val statusObj = comp.optJSONObject("status")
            val typeObj = statusObj?.optJSONObject("type")
            val state = typeObj?.optString("state", "pre") ?: "pre"
            val shortDetail = typeObj?.optString("shortDetail", "") ?: ""

            val competitors = comp.optJSONArray("competitors") ?: continue
            if (competitors.length() < 2) continue

            var homeTeam = ""
            var homeLogo = ""
            var homeScore = ""
            var awayTeam = ""
            var awayLogo = ""
            var awayScore = ""

            for (j in 0 until competitors.length()) {
                val c = competitors.getJSONObject(j)
                val homeAway = c.optString("homeAway")
                val t = c.optJSONObject("team")
                val name = t?.optString("displayName", "") ?: ""
                val logo = t?.optString("logo", "") ?: ""
                val score = c.optString("score", "")

                if (homeAway == "home") {
                    homeTeam = name
                    homeLogo = logo
                    homeScore = score
                } else {
                    awayTeam = name
                    awayLogo = logo
                    awayScore = score
                }
            }

            if (homeTeam.isBlank()) homeTeam = competitors.getJSONObject(0).optJSONObject("team")?.optString("displayName", "") ?: "فريق 1"
            if (awayTeam.isBlank()) awayTeam = competitors.getJSONObject(1).optJSONObject("team")?.optString("displayName", "") ?: "فريق 2"

            val timeDisplay = formatMatchTime(dateStr, state, shortDetail)

            // Parse kickoff time for notification scheduling and filtering
            val kickoffMs = parseIsoDate(dateStr)?.time

            list += FootballMatch(
                id = id,
                leagueName = leagueName,
                leagueCode = code,
                homeTeam = homeTeam,
                homeTeamArabic = toArabicTeam(homeTeam),
                homeLogo = homeLogo,
                homeScore = homeScore,
                awayTeam = awayTeam,
                awayTeamArabic = toArabicTeam(awayTeam),
                awayLogo = awayLogo,
                awayScore = awayScore,
                timeOrStatus = timeDisplay,
                state = state,
                matchDate = dateStr,
                kickoffEpochMs = kickoffMs,
            )
        }
        return list
    }

    fun parseIsoDate(isoDate: String): Date? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mmXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val d = sdf.parse(isoDate)
                if (d != null) return d
            } catch (_: Exception) {}
        }
        return null
    }

    private fun formatMatchTime(isoDate: String, state: String, shortDetail: String): String {
        return when (state) {
            "in" -> {
                val min = shortDetail.ifBlank { "مباشر 🔴" }
                "مباشر $min"
            }
            "post" -> {
                "انتهت"
            }
            else -> {
                try {
                    val date = parseIsoDate(isoDate)
                    if (date != null) {
                        val localFormat = SimpleDateFormat("h:mm a", Locale.forLanguageTag("ar")).apply {
                            timeZone = TimeZone.getDefault()
                        }
                        val timeStr = localFormat.format(date).replace("AM", "ص").replace("PM", "م")

                        val calMatch = Calendar.getInstance().apply { time = date }
                        val calNow = Calendar.getInstance()

                        if (calMatch.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) &&
                            calMatch.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {
                            "اليوم $timeStr"
                        } else {
                            val dayName = when (calMatch.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.SATURDAY -> "السبت"
                                Calendar.SUNDAY -> "الأحد"
                                Calendar.MONDAY -> "الاثنين"
                                Calendar.TUESDAY -> "الثلاثاء"
                                Calendar.WEDNESDAY -> "الأربعاء"
                                Calendar.THURSDAY -> "الخميس"
                                Calendar.FRIDAY -> "الجمعة"
                                else -> ""
                            }
                            "$dayName $timeStr"
                        }
                    } else {
                        shortDetail.ifBlank { "قريباً" }
                    }
                } catch (_: Exception) {
                    shortDetail.ifBlank { "قريباً" }
                }
            }
        }
    }

    private val ARABIC_TEAMS = mapOf(
        // Premier League
        "Arsenal" to "آرسنال",
        "Manchester City" to "مانشستر سيتي",
        "Liverpool" to "ليفربول",
        "Manchester United" to "مانشستر يونايتد",
        "Chelsea" to "تشيلسي",
        "Tottenham Hotspur" to "توتنهام",
        "Tottenham" to "توتنهام",
        "Newcastle United" to "نيوكاسل",
        "Newcastle" to "نيوكاسل",
        "Aston Villa" to "أستون فيلا",
        "West Ham United" to "وست هام",
        "West Ham" to "وست هام",
        "Brighton & Hove Albion" to "برايتون",
        "Brighton" to "برايتون",
        "Everton" to "إيفرتون",
        "Wolverhampton Wanderers" to "وولفرهامبتون",
        "Wolves" to "وولفرهامبتون",
        "Crystal Palace" to "كريستال بالاس",
        "Brentford" to "برينتفورد",
        "Fulham" to "فولهام",
        "Bournemouth" to "بورنموث",
        "AFC Bournemouth" to "بورنموث",
        "Nottingham Forest" to "نوتينغهام",
        "Leicester City" to "ليستر سيتي",
        "Ipswich Town" to "إبسويتش",
        "Southampton" to "ساوثهامبتون",
        "Leeds United" to "ليدز يونايتد",
        "Sunderland" to "سندرلاند",
        "Coventry City" to "كوفنتري",
        "Hull City" to "هال سيتي",

        // La Liga
        "Real Madrid" to "ريال مدريد",
        "FC Barcelona" to "برشلونة",
        "Barcelona" to "برشلونة",
        "Atlético Madrid" to "أتلتيكو مدريد",
        "Atletico Madrid" to "أتلتيكو مدريد",
        "Sevilla" to "إشبيلية",
        "Real Betis" to "ريال بيتيس",
        "Real Sociedad" to "ريال سوسيداد",
        "Athletic Club" to "أتلتيك بيلباو",
        "Athletic Bilbao" to "أتلتيك بيلباو",
        "Villarreal" to "فياريال",
        "Valencia" to "فالنسيا",
        "Girona" to "جيرونا",
        "Osasuna" to "أوساسونا",
        "Celta Vigo" to "سلتا فيغو",
        "Mallorca" to "مايوركا",
        "Getafe" to "خيتافي",
        "Las Palmas" to "لاس بالماس",
        "Rayo Vallecano" to "رايو فاليكانو",
        "Alavés" to "ديبورتيفو ألافيس",
        "Alaves" to "ديبورتيفو ألافيس",
        "Espanyol" to "إسبانيول",
        "Leganés" to "ليغانيس",
        "Leganes" to "ليغانيس",
        "Real Valladolid" to "بلد الوليد",

        // Serie A
        "Inter Milan" to "إنتر ميلان",
        "Internazionale" to "إنتر ميلان",
        "Inter" to "إنتر ميلان",
        "AC Milan" to "ميلان",
        "Milan" to "ميلان",
        "Juventus" to "يوفنتوس",
        "Napoli" to "نابولي",
        "AS Roma" to "روما",
        "Roma" to "روما",
        "Lazio" to "لاتسيو",
        "Atalanta" to "أتالانتا",
        "Fiorentina" to "فيورنتينا",
        "Bologna" to "بولونيا",
        "Torino" to "تورينو",
        "Udinese" to "أودينيزي",
        "Genoa" to "جنوى",
        "Parma" to "بارما",
        "Cagliari" to "كالياري",
        "Empoli" to "إمبولي",
        "Verona" to "فيرونا",
        "Monza" to "مونزا",
        "Lecce" to "ليتشي",
        "Como" to "كومو",
        "Venezia" to "فينيسيا",

        // Bundesliga
        "Bayern Munich" to "بايرن ميونخ",
        "Borussia Dortmund" to "بوروسيا دورتموند",
        "Bayer Leverkusen" to "باير ليفركوزن",
        "RB Leipzig" to "لايبزيغ",
        "Eintracht Frankfurt" to "فرانكفورت",
        "VfB Stuttgart" to "شتوتغارت",
        "Borussia Mönchengladbach" to "مونشنغلادباخ",
        "VfL Wolfsburg" to "فولفسبورغ",
        "SC Freiburg" to "فرايبورغ",
        "TSG Hoffenheim" to "هوفنهايم",
        "Werder Bremen" to "فيردر بريمن",
        "FC Augsburg" to "أوغسبورغ",
        "Augsburg" to "أوغسبورغ",
        "Mainz 05" to "ماينز",
        "Mainz" to "ماينز",
        "FSV Mainz 05" to "ماينز",
        "SV Elversberg" to "إيلفيرسبيرج",
        "Elversberg" to "إيلفيرسبيرج",
        "Union Berlin" to "يونيون برلين",
        "FC St. Pauli" to "سانت باولي",
        "St. Pauli" to "سانت باولي",
        "Holstein Kiel" to "هولشتاين كيل",
        "VfL Bochum" to "بوخوم",
        "Bochum" to "بوخوم",
        "FC Heidenheim" to "هايدنهايم",
        "Heidenheim" to "هايدنهايم",

        // Ligue 1
        "Paris Saint-Germain" to "باريس سان جيرمان",
        "Paris SG" to "باريس سان جيرمان",
        "PSG" to "باريس سان جيرمان",
        "Marseille" to "مارسيليا",
        "Olympique Marseille" to "مارسيليا",
        "Monaco" to "موناكو",
        "AS Monaco" to "موناكو",
        "Lyon" to "ليون",
        "Olympique Lyonnais" to "ليون",
        "Lille" to "ليل",
        "LOSC Lille" to "ليل",
        "Nice" to "نيس",
        "OGC Nice" to "نيس",
        "Lens" to "لانس",
        "RC Lens" to "لانس",
        "Rennes" to "رين",
        "Stade Rennais" to "رين",
        "Stade Rennais FC" to "رين",
        "Strasbourg" to "ستراسبورغ",
        "RC Strasbourg Alsace" to "ستراسبورغ",
        "Toulouse" to "تولوز",
        "Toulouse FC" to "تولوز",
        "Nantes" to "نانت",
        "FC Nantes" to "نانت",
        "Reims" to "ستاد ريمس",
        "Stade de Reims" to "ستاد ريمس",
        "Brest" to "ستاد بريست",
        "Stade Brestois 29" to "ستاد بريست",
        "Montpellier" to "مونبلييه",
        "Montpellier HSC" to "مونبلييه",
        "Auxerre" to "أوكسير",
        "AJ Auxerre" to "أوكسير",
        "Saint-Étienne" to "سانت إتيان",
        "Saint-Etienne" to "سانت إتيان",
        "Angers" to "أنجيه",
        "Angers SCO" to "أنجيه",
        "Le Havre" to "لوهافر",
        "Le Havre AC" to "لوهافر",
    )

    private fun toArabicTeam(teamName: String): String {
        val trimmed = teamName.trim()
        ARABIC_TEAMS[trimmed]?.let { return it }
        for ((k, v) in ARABIC_TEAMS) {
            if (trimmed.contains(k, ignoreCase = true) || k.contains(trimmed, ignoreCase = true)) {
                return v
            }
        }
        return trimmed
    }
}
