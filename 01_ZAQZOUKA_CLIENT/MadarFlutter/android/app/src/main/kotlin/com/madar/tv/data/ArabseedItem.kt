package com.madar.tv.data


data class ArabCategory(
    val id: String,
    val title: String,
    val path: String,
    val icon: String = "",
)

data class ArabseedItem(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val posterUrl: String?,
    val year: String,
    val quality: String,
    val rating: String,
    val type: ArabseedType,
    val pageUrl: String,
    val category: String,
    val overview: String = "",
    val providerName: String = "عرب سيد",
    val parentSeriesTitle: String? = null,
    val parentSeriesPageUrl: String? = null,
    val latestEpisodeLabel: String = "",
    val alternatePageUrls: List<String> = emptyList(),
) {
    override fun toString(): String = id
}

enum class ArabseedType { MOVIE, SERIES }

data class ArabseedDetail(
    val item: ArabseedItem,
    val servers: List<ArabseedServer>,
    val preResolvedStream: String? = null,
    val cast: List<String> = emptyList(),
) {
    override fun toString(): String = item.id
}

data class ArabseedServer(
    val name: String,
    val embedUrl: String,
    val directStreamUrl: String? = null,
    val sourcePageUrl: String = "",
    val sourceProviderName: String = "",
) {
    override fun toString(): String = name
}

// ── Seasons & Episodes ────────────────────────────────────────────────────────

data class ArabseedEpisode(
    val number: Int,
    val title: String,
    val pageUrl: String,
    val seasonNumber: Int = 1,
    val providerName: String = "",
    val alternatePageUrls: List<String> = emptyList(),
)

data class ArabseedSeason(
    val number: Int,
    val title: String,
    val episodes: List<ArabseedEpisode>,
    val pageUrl: String = "",
    val hasMorePages: Boolean? = null,
)

data class ArabseedSeasonBundle(
    val seasons: List<ArabseedSeason>,
    val relatedParts: List<ArabseedItem> = emptyList(), // e.g. "الجزء الثاني", "الموسم 2"
)

object ArabseedCategories {
    val MAIN_TABS = listOf(
        ArabCategory("home", "الرئيسية", "main/"),
        ArabCategory("series", "المسلسلات", "series/"),
        ArabCategory("movies", "الافلام", "movies/"),
        ArabCategory("anime", "انمي", "category/animation-movies/"),
        ArabCategory("ramadan", "رمضان", "category/مسلسلات-رمضان/"),
        ArabCategory("variety", "منوعات", "category/%d8%a8%d8%b1%d8%a7%d9%85%d8%ac-%d8%aa%d9%84%d9%81%d8%b2%d9%8a%d9%88%d9%86%d9%8a%d8%a9/"),
    )

    fun getSubCategories(mainTabId: String): List<ArabCategory> = when (mainTabId) {
        "home" -> listOf(
            ArabCategory("all", "تصفح الكل", "main/"),
            ArabCategory("trending", "الأكثر مشاهدة", "trend/"),
            ArabCategory("recent", "أضيف حديثاً", "recently/"),
        )
        "series" -> listOf(
            ArabCategory("all_series", "كل المسلسلات", "series/"),
            ArabCategory("foreign_series", "مسلسلات اجنبي", "category/foreign-series/"),
            ArabCategory("turkish_series", "مسلسلات تركي", "category/turkish-series/"),
            ArabCategory("arabic_series", "مسلسلات عربي", "category/arabic-series/"),
            ArabCategory("asian_series", "مسلسلات اسيوية", "category/مسلسلات-كوريه/"),
            ArabCategory("indian_series", "مسلسلات هندية", "category/مسلسلات-هندية/"),
            ArabCategory("dubbed_series", "مسلسلات مدبلجة", "category/dubbed-series/"),
        )
        "movies" -> listOf(
            ArabCategory("all_movies", "كل الافلام", "movies/"),
            ArabCategory("foreign_movies", "افلام اجنبي", "category/foreign-movies/"),
            ArabCategory("arabic_movies", "افلام عربي", "category/arabic-movies/"),
            ArabCategory("turkish_movies", "افلام تركي", "category/turkish-movies/"),
            ArabCategory("asian_movies", "افلام اسيوية", "category/asian-movies/"),
            ArabCategory("indian_movies", "افلام هندي", "category/indian-movies/"),
            ArabCategory("animation_movies", "افلام انميشن", "category/animation-movies/"),
            ArabCategory("dubbed_movies", "افلام مدبلجة", "category/dubbed-movies/"),
        )
        "anime" -> listOf(
            ArabCategory("anime_movies", "افلام انمي", "category/animation-movies/"),
            ArabCategory("anime_series", "مسلسلات انمي", "category/cartoon-series/"),
        )
        "ramadan" -> listOf(
            ArabCategory("ramadan_2026", "مسلسلات رمضان 2026", "category/مسلسلات-رمضان/ramadan-series-2026/"),
            ArabCategory("ramadan_2025", "مسلسلات رمضان 2025", "category/مسلسلات-رمضان/ramadan-series-2025/"),
            ArabCategory("ramadan_2024", "مسلسلات رمضان 2024", "category/مسلسلات-رمضان/ramadan-series-2024/"),
            ArabCategory("ramadan_2023", "مسلسلات رمضان 2023", "category/مسلسلات-رمضان/ramadan-series-2023/"),
        )
        "variety" -> listOf(
            ArabCategory("tv_shows", "برامج تلفزيونية", "category/%d8%a8%d8%b1%d8%a7%d9%85%d8%ac-%d8%aa%d9%84%d9%81%d8%b2%d9%8a%d9%88%d9%86%d9%8a%d8%a9/"),
            ArabCategory("wrestling", "عروض مصارعة", "category/wwe-shows/"),
        )
        else -> listOf(
            ArabCategory("all", "تصفح الكل", "main/"),
        )
    }
}
