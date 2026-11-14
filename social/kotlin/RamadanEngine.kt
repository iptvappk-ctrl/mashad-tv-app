package com.madar.tv.social

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * RamadanEngine
 * ─────────────
 * Database and logic for Ramadan 2026 drama season.
 * Covers Egyptian, Syrian, Gulf & Comedy categories.
 *
 * Usage:
 *   val engine = RamadanEngine()
 *   val series  = engine.getByCategory("egyptian")
 *   val eps     = engine.getEpisodes("باب الحارة")
 *   val next    = engine.getNextAirTime("كلبش 5")
 */
class RamadanEngine {

    // ── Data model ────────────────────────────────────────────────────────────
    data class RamadanSeries(
        val id: String,
        val title: String,
        val arabicTitle: String,
        val category: String,          // egyptian | syrian | gulf | comedy
        val channel: String,
        val airTimeHour: Int,          // 24h, local Egypt time (UTC+2)
        val airTimeMinute: Int,
        val totalEpisodes: Int,
        val year: Int = 2026,
        val posterUrl: String = "",
        val overview: String = "",
        val cast: List<String> = emptyList(),
        val rating: Double = 0.0,
        val sourceUrls: List<String> = emptyList(),
    )

    // ── Database: Ramadan 2026 ────────────────────────────────────────────────
    val DATABASE: List<RamadanSeries> = listOf(

        // ═══ EGYPTIAN ════════════════════════════════════════════════════════
        RamadanSeries(
            id = "ra2026-eg-01",
            title = "kelabsh-5",
            arabicTitle = "كلبش 5",
            category = "egyptian",
            channel = "MBC مصر",
            airTimeHour = 21, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "الموسم الخامس من المسلسل البوليسي الأكثر مشاهدة",
            cast = listOf("أمير كرارة", "أحمد داود"),
            rating = 8.5,
        ),
        RamadanSeries(
            id = "ra2026-eg-02",
            title = "al-ikhtiyar-4",
            arabicTitle = "الاختيار 4",
            category = "egyptian",
            channel = "ON E",
            airTimeHour = 20, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "الجزء الرابع من ملحمة الشهداء والوطن",
            cast = listOf("كريم عبد العزيز", "أحمد مكي"),
            rating = 9.1,
        ),
        RamadanSeries(
            id = "ra2026-eg-03",
            title = "nasser-aw-wa7ed",
            arabicTitle = "ناصر أو واحد",
            category = "egyptian",
            channel = "النهار",
            airTimeHour = 22, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "مسلسل اجتماعي درامي عن الحياة اليومية",
            cast = listOf("يسرا", "ياسمين صبري"),
            rating = 7.8,
        ),
        RamadanSeries(
            id = "ra2026-eg-04",
            title = "al-haref",
            arabicTitle = "الحرفي",
            category = "egyptian",
            channel = "CBC",
            airTimeHour = 21, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "مسلسل أكشن عن عالم الجريمة المنظمة",
            cast = listOf("محمد سامي", "دينا الشربيني"),
            rating = 8.0,
        ),
        RamadanSeries(
            id = "ra2026-eg-05",
            title = "bawab-al-halawany",
            arabicTitle = "بواب الحلواني",
            category = "egyptian",
            channel = "MBC مصر",
            airTimeHour = 23, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "كوميديا اجتماعية بمشاركة نجوم الكوميديا",
            cast = listOf("أحمد حلمي", "منى زكي"),
            rating = 8.3,
        ),
        RamadanSeries(
            id = "ra2026-eg-06",
            title = "ibrahim-al-abyad-2",
            arabicTitle = "إبراهيم الأبيض 2",
            category = "egyptian",
            channel = "ON E",
            airTimeHour = 20, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "الجزء الثاني من الملحمة الكلاسيكية",
            cast = listOf("أحمد السقا"),
            rating = 8.7,
        ),
        RamadanSeries(
            id = "ra2026-eg-07",
            title = "nessf-rabena",
            arabicTitle = "نص ربنا",
            category = "egyptian",
            channel = "النهار",
            airTimeHour = 22, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "مسلسل ديني اجتماعي",
            cast = listOf("عمرو عبد الجليل"),
            rating = 7.5,
        ),
        RamadanSeries(
            id = "ra2026-eg-08",
            title = "kabour-w-bakhit-3",
            arabicTitle = "كابور وبخيت 3",
            category = "comedy",
            channel = "MBC مصر",
            airTimeHour = 23, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "الجزء الثالث من المسلسل الكوميدي الشهير",
            cast = listOf("مصطفى خاطر", "محمد ثروت"),
            rating = 8.1,
        ),

        // ═══ SYRIAN ══════════════════════════════════════════════════════════
        RamadanSeries(
            id = "ra2026-sy-01",
            title = "bab-al-hara-16",
            arabicTitle = "باب الحارة 16",
            category = "syrian",
            channel = "MBC",
            airTimeHour = 20, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "الموسم السادس عشر من العمل الخالد باب الحارة",
            cast = listOf("بسام كوسا", "وائل شرف"),
            rating = 7.9,
        ),
        RamadanSeries(
            id = "ra2026-sy-02",
            title = "al-hayba-al-hasad",
            arabicTitle = "الهيبة - الحصاد",
            category = "syrian",
            channel = "MBC",
            airTimeHour = 22, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "الموسم الجديد من مسلسل الهيبة اللبناني السوري",
            cast = listOf("تيم حسن", "نادين نسيب نجيم"),
            rating = 8.6,
        ),
        RamadanSeries(
            id = "ra2026-sy-03",
            title = "ma-malakat-aymankum",
            arabicTitle = "ما ملكت أيمانكم",
            category = "syrian",
            channel = "Abu Dhabi TV",
            airTimeHour = 21, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "دراما تاريخية سورية",
            cast = listOf("قصي خولي"),
            rating = 7.7,
        ),
        RamadanSeries(
            id = "ra2026-sy-04",
            title = "al-ghareeb-3",
            arabicTitle = "الغريب 3",
            category = "syrian",
            channel = "Rotana Drama",
            airTimeHour = 20, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "الجزء الثالث من مسلسل الغريب",
            cast = listOf("مكسيم خليل"),
            rating = 8.2,
        ),

        // ═══ GULF ═════════════════════════════════════════════════════════════
        RamadanSeries(
            id = "ra2026-gf-01",
            title = "selfie-5",
            arabicTitle = "سيلفي 5",
            category = "gulf",
            channel = "MBC",
            airTimeHour = 23, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "الموسم الخامس من مسلسل سيلفي",
            cast = listOf("فهد المطيري"),
            rating = 7.6,
        ),
        RamadanSeries(
            id = "ra2026-gf-02",
            title = "abu-al-banat-5",
            arabicTitle = "أبو البنات 5",
            category = "gulf",
            channel = "Rotana Khalijia",
            airTimeHour = 21, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "الجزء الخامس من المسلسل الخليجي الكوميدي",
            cast = listOf("علي الهويريني"),
            rating = 7.4,
        ),
        RamadanSeries(
            id = "ra2026-gf-03",
            title = "al-ithm",
            arabicTitle = "الإثم",
            category = "gulf",
            channel = "Saudi TV",
            airTimeHour = 22, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "دراما خليجية اجتماعية",
            cast = listOf("خالد سامي"),
            rating = 7.8,
        ),

        // ═══ COMEDY ══════════════════════════════════════════════════════════
        RamadanSeries(
            id = "ra2026-co-01",
            title = "yalla-nedhak",
            arabicTitle = "يلا نضحك",
            category = "comedy",
            channel = "MBC مصر",
            airTimeHour = 23, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "برنامج كوميدي مع نجوم الكوميديا",
            cast = listOf("محمد هنيدي", "أشرف عبد الباقي"),
            rating = 7.5,
        ),
        RamadanSeries(
            id = "ra2026-co-02",
            title = "dokan-shahar",
            arabicTitle = "دكان شهر",
            category = "comedy",
            channel = "CBC",
            airTimeHour = 24, airTimeMinute = 0,
            totalEpisodes = 30,
            overview = "كوميديا موقفية خفيفة",
            cast = listOf("علي ربيع", "أكرم حسني"),
            rating = 7.9,
        ),
        RamadanSeries(
            id = "ra2026-co-03",
            title = "noss-msa2la",
            arabicTitle = "نص مسألة",
            category = "comedy",
            channel = "Al Hayat",
            airTimeHour = 22, airTimeMinute = 30,
            totalEpisodes = 30,
            overview = "كوميديا اجتماعية",
            cast = listOf("بيومي فؤاد"),
            rating = 7.3,
        ),
    )

    // ── Public API ────────────────────────────────────────────────────────────
    /** Get all series as JSONArray */
    fun getAll(): JSONArray = seriesToJsonArray(DATABASE)

    /** Get by category: egyptian | syrian | gulf | comedy */
    fun getByCategory(category: String): JSONArray =
        seriesToJsonArray(DATABASE.filter {
            it.category.equals(category, ignoreCase = true)
        })

    /** Find a series by title (fuzzy) */
    fun findByTitle(query: String): RamadanSeries? {
        val q = query.lowercase().trim()
        return DATABASE.firstOrNull {
            it.arabicTitle.contains(q) || it.title.contains(q) ||
                    q.contains(it.arabicTitle) || q.contains(it.title)
        }
    }

    /** Get mock episodes for a series (dynamic — real data comes from site crawl) */
    fun getEpisodes(seriesTitle: String, count: Int = 30): JSONArray {
        val series = findByTitle(seriesTitle) ?: return JSONArray()
        val arr = JSONArray()
        for (ep in 1..minOf(count, series.totalEpisodes)) {
            arr.put(
                JSONObject()
                    .put("number", ep)
                    .put("title", "الحلقة $ep")
                    .put("seriesId", series.id)
                    .put("seriesTitle", series.arabicTitle)
                    .put("airDate", getEpisodeAirDate(series, ep))
                    .put("isAvailable", ep <= getCurrentEpisodeCount(series))
            )
        }
        return arr
    }

    /**
     * Returns seconds until next episode airs.
     * Negative = episode already aired.
     */
    fun getSecondsToNextEpisode(seriesTitle: String): Long {
        val series = findByTitle(seriesTitle) ?: return -1L
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, series.airTimeHour % 24)
            set(Calendar.MINUTE, series.airTimeMinute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (next.timeInMillis - now.timeInMillis) / 1000L
    }

    /** Summary JSON for one series */
    fun seriesToJson(s: RamadanSeries): JSONObject {
        val castArr = JSONArray().also { arr -> s.cast.forEach { arr.put(it) } }
        val srcArr = JSONArray().also { arr -> s.sourceUrls.forEach { arr.put(it) } }
        return JSONObject()
            .put("id", s.id)
            .put("title", s.arabicTitle)
            .put("category", s.category)
            .put("channel", s.channel)
            .put("airTime", String.format("%02d:%02d", s.airTimeHour % 24, s.airTimeMinute))
            .put("totalEpisodes", s.totalEpisodes)
            .put("currentEpisode", getCurrentEpisodeCount(s))
            .put("secondsToNext", getSecondsToNextEpisode(s.arabicTitle))
            .put("overview", s.overview)
            .put("cast", castArr)
            .put("rating", s.rating)
            .put("year", s.year)
            .put("posterUrl", s.posterUrl)
            .put("sourceUrls", srcArr)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun seriesToJsonArray(list: List<RamadanSeries>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(seriesToJson(it)) }
        return arr
    }

    private fun getCurrentEpisodeCount(s: RamadanSeries): Int {
        // Ramadan 2026 starts ~March 1 (approximate)
        val ramadanStart = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 1, 20, 0, 0)
        }
        val now = Calendar.getInstance()
        if (now.before(ramadanStart)) return 0
        val daysSinceStart = ((now.timeInMillis - ramadanStart.timeInMillis) /
                (24 * 3600 * 1000L)).toInt()
        return minOf(daysSinceStart + 1, s.totalEpisodes)
    }

    private fun getEpisodeAirDate(s: RamadanSeries, episodeNumber: Int): String {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 1 + (episodeNumber - 1))
            set(Calendar.HOUR_OF_DAY, s.airTimeHour % 24)
            set(Calendar.MINUTE, s.airTimeMinute)
        }
        return String.format(
            "%04d-%02d-%02d %02d:%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            s.airTimeHour % 24,
            s.airTimeMinute,
        )
    }

    companion object {
        // Category constants
        const val CAT_EGYPTIAN = "egyptian"
        const val CAT_SYRIAN   = "syrian"
        const val CAT_GULF     = "gulf"
        const val CAT_COMEDY   = "comedy"

        val ALL_CATEGORIES = listOf(CAT_EGYPTIAN, CAT_SYRIAN, CAT_GULF, CAT_COMEDY)
    }
}
