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
) {
    override fun toString(): String = name
}

// ── Seasons & Episodes ────────────────────────────────────────────────────────

data class ArabseedEpisode(
    val number: Int,
    val title: String,
    val pageUrl: String,
    val seasonNumber: Int = 1,
)

data class ArabseedSeason(
    val number: Int,
    val title: String,
    val episodes: List<ArabseedEpisode>,
)

data class ArabseedSeasonBundle(
    val seasons: List<ArabseedSeason>,
    val relatedParts: List<ArabseedItem> = emptyList(), // e.g. "الجزء الثاني", "الموسم 2"
)

object ArabseedCategories {
    val MAIN_TABS = listOf(
        ArabCategory("home", "الرئيسية", "home/"),
        ArabCategory("series", "المسلسلات", "%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/"),
        ArabCategory("movies", "الافلام", "%d8%a7%d9%81%d9%84%d8%a7%d9%85/"),
        ArabCategory("anime", "انمي", "category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%86%d9%85%d9%8a/"),
        ArabCategory("ramadan", "رمضان", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2026/"),
        ArabCategory("variety", "منوعات", "category/%d8%a8%d8%b1%d8%a7%d9%85%d8%ac-%d8%aa%d9%84%d9%81%d8%b2%d9%8a%d9%88%d9%86%d9%8a%d8%a9/"),
    )

    fun getSubCategories(mainTabId: String): List<ArabCategory> = when (mainTabId) {
        "home" -> listOf(
            ArabCategory("all", "تصفح الكل", "home/"),
            ArabCategory("trending", "الأكثر مشاهدة", "trend/"),
            ArabCategory("recent", "أضيف حديثاً", "recently/"),
        )
        "series" -> listOf(
            ArabCategory("all_series", "كل المسلسلات", "%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/"),
            ArabCategory("foreign_series", "مسلسلات اجنبي", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/"),
            ArabCategory("turkish_series", "مسلسلات تركية", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%aa%d8%b1%d9%83%d9%8a%d8%a9/"),
            ArabCategory("arabic_series", "مسلسلات عربي", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b9%d8%b1%d8%a8%d9%8a/"),
            ArabCategory("asian_series", "مسلسلات اسيوية", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/"),
            ArabCategory("indian_series", "مسلسلات هندية", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d9%87%d9%86%d8%af%d9%8a%d8%a9/"),
            ArabCategory("dubbed_series", "مسلسلات مدبلجة", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d9%85%d8%af%d8%a8%d9%84%d8%ac%d8%a9/"),
        )
        "movies" -> listOf(
            ArabCategory("all_movies", "كل الأفلام", "%d8%a7%d9%81%d9%84%d8%a7%d9%85/"),
            ArabCategory("foreign_movies", "افلام اجنبي", "category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/"),
            ArabCategory("arabic_movies", "افلام عربي", "category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%b9%d8%b1%d8%a8%d9%8a/"),
            ArabCategory("turkish_movies", "افلام تركية", "category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%aa%d8%b1%d9%83%d9%8a%d8%a9/"),
            ArabCategory("asian_movies", "افلام اسيوية", "category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/"),
            ArabCategory("indian_movies", "افلام هندي", "category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d9%87%d9%86%d8%af%d9%8a/"),
        )
        "anime" -> listOf(
            ArabCategory("anime_movies", "افلام انمي", "category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d9%86%d9%85%d9%8a/"),
            ArabCategory("anime_series", "مسلسلات انمي", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d9%86%d9%85%d9%8a/"),
        )
        "ramadan" -> listOf(
            ArabCategory("ramadan_2026", "مسلسلات رمضان 2026", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2026/"),
            ArabCategory("ramadan_2025", "مسلسلات رمضان 2025", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2025/"),
            ArabCategory("ramadan_2024", "مسلسلات رمضان 2024", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2024/"),
            ArabCategory("ramadan_2023", "مسلسلات رمضان 2023", "category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2023/"),
            ArabCategory("ramadan_2022", "مسلسلات رمضان 2022", "category/%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2022/"),
        )
        "variety" -> listOf(
            ArabCategory("tv_shows", "برامج تلفزيونية", "category/%d8%a8%d8%b1%d8%a7%d9%85%d8%ac-%d8%aa%d9%84%d9%81%d8%b2%d9%8a%d9%88%d9%86%d9%8a%d8%a9/"),
            ArabCategory("wrestling", "عروض مصارعة", "category/%d8%b9%d8%b1%d9%88%d8%b6-%d9%85%d8%b5%d8%a7%d8%b1%d8%b9%d8%a9/"),
        )
        else -> listOf(
            ArabCategory("all", "تصفح الكل", "home/"),
        )
    }
}
