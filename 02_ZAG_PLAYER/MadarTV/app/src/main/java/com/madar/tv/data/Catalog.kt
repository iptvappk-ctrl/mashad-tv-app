package com.madar.tv.data

import androidx.compose.ui.graphics.Color

data class LiveMatch(
    val id: String,
    val league: String,
    val home: String,
    val away: String,
    val homeMark: String,
    val awayMark: String,
    val homeScore: Int,
    val awayScore: Int,
    val minute: String,
)

enum class MediaType { MOVIE, SERIES }

data class MediaItem(
    val id: String,
    val title: String,
    val category: String,
    val year: String,
    val type: MediaType,
    val accent: Color,
    val posterUrl: String? = null,
    val overview: String = "",
    val rating: String = "",
)

data class Channel(
    val id: String,
    val name: String,
    val category: String,
    val mark: String,
    val accent: Color,
    val streamUrl: String? = null,
    val mimeType: String? = null,
    val logoUrl: String? = null,
)

object DemoCatalog {
    val topFiveLeagues = listOf(
        "الدوري الإنجليزي",
        "الدوري الإسباني",
        "الدوري الإيطالي",
        "الدوري الألماني",
        "الدوري الفرنسي",
    )

    // Content cards are ready for a future catalog API; no invented titles are shown.
    val movies = emptyList<MediaItem>()
    val series = emptyList<MediaItem>()

    // Loaded from the Free-TV public/free channel playlist at runtime.
    val channels = emptyList<Channel>()
}
