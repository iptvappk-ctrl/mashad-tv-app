package com.madar.tv.data

// ============================================================
// HiTV Models — بيانات المحتوى الكوري/الآسيوي
// ============================================================

/** فئة المحتوى (Drama, Movie, Anime, etc.) */
data class HiTvCategory(
    val id: String,
    val name: String,           // "Korean Drama", "Chinese Drama", etc.
    val nameAr: String,         // "مسلسلات كورية", "مسلسلات صينية"
    val poster: String = "",
    val count: Int = 0
)

/** مسلسل أو فيلم */
data class HiTvContent(
    val id: String,
    val title: String,
    val titleAr: String = "",
    val poster: String,
    val backdrop: String = "",
    val year: Int = 0,
    val country: String = "",   // "KR", "CN", "TH", "JP"
    val genres: List<String> = emptyList(),
    val totalEpisodes: Int = 0,
    val latestEpisode: Int = 0,
    val rating: Float = 0f,
    val description: String = "",
    val language: String = "",
    val subtitles: List<String> = emptyList(),
    val isVip: Boolean = false,
    val isCompleted: Boolean = false,
    val type: HiTvContentType = HiTvContentType.DRAMA,
    val tags: List<String> = emptyList()
)

enum class HiTvContentType {
    DRAMA, MOVIE, ANIME, VARIETY, LIVE
}

/** حلقة */
data class HiTvEpisode(
    val id: String,
    val contentId: String,
    val number: Int,
    val title: String = "",
    val thumbnail: String = "",
    val duration: Int = 0,       // بالثواني
    val streamUrl: String = "",
    val isVip: Boolean = false
)

/** رابط البث */
data class HiTvStream(
    val url: String,
    val quality: String = "Auto",  // "1080p", "720p", "480p", "Auto"
    val type: String = "hls",      // "hls", "mp4", "dash"
    val headers: Map<String, String> = emptyMap(),
    val subtitleUrl: String = ""
)

/** نتيجة البحث */
data class HiTvSearchResult(
    val items: List<HiTvContent>,
    val total: Int,
    val page: Int,
    val hasMore: Boolean
)

/** الصفحة الرئيسية */
data class HiTvHome(
    val banners: List<HiTvContent> = emptyList(),
    val trending: List<HiTvContent> = emptyList(),
    val newEpisodes: List<HiTvContent> = emptyList(),
    val koreanDramas: List<HiTvContent> = emptyList(),
    val chineseDramas: List<HiTvContent> = emptyList(),
    val thaiDramas: List<HiTvContent> = emptyList(),
    val movies: List<HiTvContent> = emptyList(),
    val anime: List<HiTvContent> = emptyList()
)

// ============================================================
// الفئات المضمنة (للعرض بدون API)
// ============================================================
object HiTvStaticData {

    val CATEGORIES = listOf(
        HiTvCategory("KR_DRAMA", "Korean Drama", "مسلسلات كورية", count = 500),
        HiTvCategory("CN_DRAMA", "Chinese Drama", "مسلسلات صينية", count = 300),
        HiTvCategory("TH_DRAMA", "Thai Drama", "مسلسلات تايلاندية", count = 150),
        HiTvCategory("JP_ANIME", "Japanese Anime", "أنيمي ياباني", count = 400),
        HiTvCategory("MOVIE",    "Movies", "الأفلام", count = 200),
        HiTvCategory("VARIETY",  "Variety Shows", "برامج ترفيهية", count = 100),
    )

    // أعلام البلدان
    fun countryFlag(code: String): String = when(code) {
        "KR" -> "🇰🇷"
        "CN" -> "🇨🇳"
        "TH" -> "🇹🇭"
        "JP" -> "🇯🇵"
        else -> "🌏"
    }

    fun countryName(code: String): String = when(code) {
        "KR" -> "كوريا"
        "CN" -> "الصين"
        "TH" -> "تايلاند"
        "JP" -> "اليابان"
        else -> "آسيا"
    }
}
