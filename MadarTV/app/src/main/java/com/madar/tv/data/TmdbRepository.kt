package com.madar.tv.data

import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.madar.tv.core.X
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class TmdbCatalog(
    val movies: List<MediaItem>,
    val series: List<MediaItem>,
)

object TmdbRepository {

    // ═══════════════════════════════════════════════════════════════════════
    // ALL CONSTANTS — 6-layer encrypted via X.s() (v6 engine)
    // ═══════════════════════════════════════════════════════════════════════

    private val _tab = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 207.toByte(), 78, 221.toByte(), 113, 97, 102, 231.toByte(), 250.toByte(), 246.toByte(), 139.toByte(), 5, 26, 145.toByte(), 169.toByte(), 140.toByte(), 37, 180.toByte(), 59, 104, 107)
    private val _tib = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 203.toByte(), 192.toByte(), 217.toByte(), 221.toByte(), 233.toByte(), 67, 111, 250.toByte(), 117, 141.toByte(), 160.toByte(), 25, 152.toByte(), 39, 12, 174.toByte(), 30, 190.toByte(), 104, 65, 208.toByte(), 86, 88, 104, 230.toByte(), 126, 247.toByte())
    private val _tah = byteArrayOf(247.toByte(), 157.toByte(), 146.toByte(), 170.toByte(), 43, 48)
    private val _taj = byteArrayOf(135.toByte(), 22, 25, 46, 167.toByte(), 185.toByte(), 189.toByte(), 62, 203.toByte(), 199.toByte(), 88, 241.toByte(), 108, 233.toByte(), 228.toByte(), 125)
    private val _tuh = byteArrayOf(253.toByte(), 149.toByte(), 147.toByte(), 35, 133.toByte(), 202.toByte(), 176.toByte(), 182.toByte(), 78, 76)
    private val _tuv = byteArrayOf(241.toByte(), 158.toByte(), 19, 168.toByte(), 40, 64, 72, 149.toByte(), 103, 103, 241.toByte(), 112, 155.toByte(), 99, 103, 119, 246.toByte(), 10, 131.toByte())
    private val _tla = byteArrayOf(135.toByte(), 21, 183.toByte(), 218.toByte(), 222.toByte())
    private val _tsb = byteArrayOf(15, 159.toByte(), 25, 162.toByte(), 37, 186.toByte(), 54, 176.toByte(), 69, 202.toByte(), 120, 82, 233.toByte(), 233.toByte(), 230.toByte())

    // API_KEY
    private val _tk = byteArrayOf(173.toByte(), 29, 57, 130.toByte(), 9, 18, 62, 145.toByte(), 99, 236.toByte(), 84, 248.toByte(), 102, 225.toByte(), 192.toByte(), 127, 117, 38, 164.toByte(), 155.toByte(), 145.toByte(), 130.toByte(), 160.toByte(), 166.toByte(), 61, 158.toByte(), 97, 104, 113, 80, 253.toByte(), 192.toByte())

    // ── Decoys ────────────────────────────────────────────────────────────
    @Suppress("unused") private val _dc1 = byteArrayOf(19, 243.toByte(), 107, 91, 99)

    // ── Lazily resolved ───────────────────────────────────────────────────
    private val API_BASE: String by lazy { X.s(_tab) }
    private val IMAGE_BASE: String by lazy { X.s(_tib) }
    private val ACCEPT_HDR: String by lazy { X.s(_tah) }
    private val ACCEPT_JSON: String by lazy { X.s(_taj) }
    private val UA_HDR: String by lazy { X.s(_tuh) }
    private val UA_VAL: String by lazy { X.s(_tuv) }
    private val LANG: String by lazy { X.s(_tla) }
    private val SORT_BY: String by lazy { X.s(_tsb) }
    private val API_KEY: String by lazy { X.s(_tk) }

    fun loadDiscoverCatalog(): TmdbCatalog {
        if (API_KEY.isBlank()) {
            throw IOException("TMDB API key is not configured")
        }
        return TmdbCatalog(
            movies = fetchDiscover("movie", MediaType.MOVIE),
            series = fetchDiscover("tv", MediaType.SERIES),
        )
    }

    private fun fetchDiscover(mediaPath: String, type: MediaType): List<MediaItem> {
        val uri = Uri.parse("$API_BASE/discover/$mediaPath")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("language", LANG)
            .appendQueryParameter("page", "1")
            .appendQueryParameter("sort_by", SORT_BY)
            .appendQueryParameter("include_adult", "false")
            .appendQueryParameter("include_video", "false")
            .build()

        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            instanceFollowRedirects = true
            setRequestProperty(ACCEPT_HDR, ACCEPT_JSON)
            setRequestProperty(UA_HDR, UA_VAL)
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("TMDB response: ${connection.responseCode}")
            }
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val results = JSONObject(response).optJSONArray("results") ?: return emptyList()
            return buildList {
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    if (item.optBoolean("adult", false)) continue
                    val tmdbId = item.optInt("id", 0)
                    val title = item.optString(if (type == MediaType.MOVIE) "title" else "name")
                        .trim()
                    if (tmdbId <= 0 || title.isBlank()) continue

                    val date = item.optString(if (type == MediaType.MOVIE) "release_date" else "first_air_date")
                    val posterPath = item.optString("poster_path").takeIf { it.startsWith('/') }
                    add(
                        MediaItem(
                            id = "${mediaPath}_$tmdbId",
                            title = title,
                            category = "الأكثر رواجًا",
                            year = date.take(4).ifBlank { "—" },
                            type = type,
                            accent = if (type == MediaType.MOVIE) Color(0xFFC07548) else Color(0xFF6F66B6),
                            posterUrl = posterPath?.let { "$IMAGE_BASE$it" },
                            overview = item.optString("overview").trim(),
                            rating = item.optDouble("vote_average", 0.0).takeIf { it > 0.0 }
                                ?.let { String.format(java.util.Locale.US, "%.1f", it) }
                                .orEmpty(),
                        ),
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
