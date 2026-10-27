package com.madar.tv.data

import com.madar.tv.core.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object FootballRepository {

    // ═══════════════════════════════════════════════════════════════════════
    // ENCRYPTED CONSTANTS — 6-layer obfuscation via X.s() / X.j() (v6)
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
                }
                if (sorted.isNotEmpty()) sorted else cachedMatches
            }
        } catch (_: Exception) {
            cachedMatches
        }
    }

    private fun fetchLeagueMatches(code: String, leagueName: String): List<FootballMatch> {
        val url = EP.format(code)
        val req = Request.Builder()
            .url(url)
            .header(UH, UV)
            .header(AH, AJ)
            .build()

        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val events = json.optJSONArray("events") ?: return emptyList()

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
            )
        }
        return list
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
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(isoDate)
                    if (date != null) {
                        val localFormat = SimpleDateFormat("HH:mm", Locale.US)
                        localFormat.timeZone = TimeZone.getDefault()
                        val timeStr = localFormat.format(date)

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
        "Newcastle United" to "نيوكاسل",
        "Aston Villa" to "أستون فيلا",
        "West Ham United" to "وست هام",
        "Brighton & Hove Albion" to "برايتون",
        "Everton" to "إيفرتون",
        "Wolverhampton Wanderers" to "وولفرهامبتون",
        "Crystal Palace" to "كريستال بالاس",
        "Brentford" to "برينتفورد",
        "Fulham" to "فولهام",
        "Bournemouth" to "بورنموث",
        "Nottingham Forest" to "نوتينغهام",
        "Leicester City" to "ليستر سيتي",
        "Ipswich Town" to "إبسويتش",
        "Southampton" to "ساوثهامبتون",

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
        "Espanyol" to "إسبانيول",
        "Leganés" to "ليغانيس",
        "Real Valladolid" to "بلد الوليد",

        // Serie A
        "Inter Milan" to "إنتر ميلان",
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
        "Mainz 05" to "ماينز",
        "Union Berlin" to "يونيون برلين",
        "FC St. Pauli" to "سانت باولي",
        "Holstein Kiel" to "هولشتاين كيل",
        "VfL Bochum" to "بوخوم",
        "FC Heidenheim" to "هايدنهايم",

        // Ligue 1
        "Paris Saint-Germain" to "باريس سان جيرمان",
        "Marseille" to "مارسيليا",
        "Monaco" to "موناكو",
        "Lyon" to "ليون",
        "Lille" to "ليل",
        "Nice" to "نيس",
        "Lens" to "لانس",
        "Rennes" to "رين",
        "Strasbourg" to "ستراسبورغ",
        "Toulouse" to "تولوز",
        "Nantes" to "نانت",
        "Reims" to "ستاد ريمس",
        "Brest" to "ستاد بريست",
        "Montpellier" to "مونبلييه",
        "Auxerre" to "أوكسير",
        "Saint-Étienne" to "سانت إتيان",
        "Angers" to "أنجيه",
        "Le Havre" to "لوهافر",
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
