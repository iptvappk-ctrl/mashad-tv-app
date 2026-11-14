package com.madar.tv.social

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * QualityTrackExtractor
 * ─────────────────────
 * Parses HLS / DASH master playlists and extracts quality tracks.
 * Returns a JSON structure compatible with the Madar player's
 * backupSourcesJson format so quality switching works immediately.
 *
 * Usage (drop into MadarFlutter/android/.../kotlin/com/madar/tv/data/):
 *   val pkg = QualityTrackExtractor.buildQualityPackage(masterUrl, referer)
 *   // pkg is a JSONObject with keys: tracks (array), bestUrl, autoUrl
 */
object QualityTrackExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // ── Quality labels ───────────────────────────────────────────────────────
    data class QualityTrack(
        val label: String,       // "1080p", "720p", etc.
        val resolution: String,  // "1920x1080"
        val bandwidth: Long,     // bits per second
        val streamUrl: String,   // absolute .m3u8 URL for this track
        val codecs: String = "",
        val frameRate: Double = 0.0,
    )

    // ── Main entry ───────────────────────────────────────────────────────────
    /**
     * Fetches [masterUrl], parses it, and returns a JSONObject:
     * {
     *   "autoUrl":  "https://...",        // best quality stream
     *   "bestUrl":  "https://...",        // 1080p or highest available
     *   "tracks": [
     *     { "label":"1080p", "resolution":"1920x1080", "bandwidth":5000000,
     *       "streamUrl":"https://...", "codecs":"...", "frameRate":30.0 }
     *     ...
     *   ]
     * }
     */
    suspend fun buildQualityPackage(
        masterUrl: String,
        referer: String = "",
    ): JSONObject = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            val body = fetchText(masterUrl, referer)
            val tracks = parseM3u8(body, masterUrl)

            val arr = JSONArray()
            tracks.forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("label", t.label)
                        put("resolution", t.resolution)
                        put("bandwidth", t.bandwidth)
                        put("streamUrl", t.streamUrl)
                        put("codecs", t.codecs)
                        put("frameRate", t.frameRate)
                    }
                )
            }
            result.put("tracks", arr)

            // Auto = highest bandwidth; best = highest but ≤ 1080p
            val sorted = tracks.sortedByDescending { it.bandwidth }
            result.put("autoUrl", sorted.firstOrNull()?.streamUrl ?: masterUrl)
            val best = sorted.firstOrNull { it.resolution.startsWith("1920") }
                ?: sorted.firstOrNull()
            result.put("bestUrl", best?.streamUrl ?: masterUrl)

        } catch (e: Exception) {
            // If parsing fails, fall back to the master URL itself
            result.put("autoUrl", masterUrl)
            result.put("bestUrl", masterUrl)
            result.put("tracks", JSONArray())
            result.put("error", e.message ?: "unknown")
        }
        result
    }

    // ── HLS master playlist parser ───────────────────────────────────────────
    fun parseM3u8(content: String, baseUrl: String): List<QualityTrack> {
        val tracks = mutableListOf<QualityTrack>()
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val attrs = parseAttributes(line.removePrefix("#EXT-X-STREAM-INF:"))
                val bandwidth = attrs["BANDWIDTH"]?.toLongOrNull() ?: 0L
                val resolution = attrs["RESOLUTION"] ?: ""
                val codecs = attrs["CODECS"] ?: ""
                val frameRate = attrs["FRAME-RATE"]?.toDoubleOrNull() ?: 0.0

                val streamLine = lines.getOrNull(i + 1)?.trim() ?: ""
                if (streamLine.isNotEmpty() && !streamLine.startsWith("#")) {
                    val streamUrl = resolveUrl(streamLine, baseUrl)
                    val label = resolutionToLabel(resolution, bandwidth)
                    tracks.add(
                        QualityTrack(
                            label = label,
                            resolution = resolution,
                            bandwidth = bandwidth,
                            streamUrl = streamUrl,
                            codecs = codecs,
                            frameRate = frameRate,
                        )
                    )
                }
                i += 2
                continue
            }
            i++
        }
        // Sort: highest quality first
        return tracks.sortedByDescending { it.bandwidth }
            .distinctBy { it.label }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun resolutionToLabel(resolution: String, bandwidth: Long): String {
        val height = resolution.substringAfter("x", "").toIntOrNull()
            ?: resolution.substringBefore("x", "").toIntOrNull()
            ?: 0
        return when {
            height >= 2160 -> "4K"
            height >= 1080 -> "1080p"
            height >= 720  -> "720p"
            height >= 480  -> "480p"
            height >= 360  -> "360p"
            height > 0     -> "${height}p"
            bandwidth >= 8_000_000 -> "4K"
            bandwidth >= 4_000_000 -> "1080p"
            bandwidth >= 2_000_000 -> "720p"
            bandwidth >= 800_000   -> "480p"
            else                   -> "360p"
        }
    }

    private fun resolveUrl(path: String, base: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val baseUri = base.substringBeforeLast("/")
        return "$baseUri/$path"
    }

    private fun parseAttributes(attrs: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""(\w[\w-]*)=(?:"([^"]*)"|([^,]*))""")
        regex.findAll(attrs).forEach { m ->
            map[m.groupValues[1]] = m.groupValues[2].ifEmpty { m.groupValues[3] }
        }
        return map
    }

    private fun fetchText(url: String, referer: String = ""): String {
        val req = Request.Builder()
            .url(url)
            .apply {
                if (referer.isNotEmpty()) header("Referer", referer)
                header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:109.0)")
            }
            .build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
        return resp.body?.string() ?: throw IOException("Empty body")
    }

    // ── Quality label list (for UI DropDown) ─────────────────────────────────
    val ALL_QUALITY_LABELS = listOf("Auto", "4K", "1080p", "720p", "480p", "360p")
}
