package com.madar.tv.data

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.madar.tv.core.X
import java.io.IOException
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Fetches the Free-TV playlist, whose stated inclusion rules exclude paid channels. */
object PublicChannelRepository {

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy encoded constants; values are recoverable from the client APK.
    // ═══════════════════════════════════════════════════════════════════════

    private val _pu = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 68, 198.toByte(), 220.toByte(), 113, 230.toByte(), 230.toByte(), 111, 120, 253.toByte(), 141.toByte(), 11, 19, 17, 161.toByte(), 46, 37, 190.toByte(), 188.toByte(), 75, 193.toByte(), 209.toByte(), 243.toByte(), 86, 109, 97, 95, 140.toByte(), 3, 129.toByte(), 152.toByte(), 187.toByte(), 110, 90, 135.toByte(), 205.toByte(), 72, 49, 55, 228.toByte(), 218.toByte(), 215.toByte(), 229.toByte(), 107, 250.toByte(), 120, 43, 133.toByte(), 130.toByte(), 31, 26, 175.toByte(), 36, 48, 184.toByte(), 148.toByte(), 74, 108, 84, 253.toByte())
    private val _at = byteArrayOf(122, 21, 147.toByte(), 170.toByte(), 133.toByte(), 64, 72, 149.toByte(), 187.toByte(), 62, 35, 37)
    private val _ua = byteArrayOf(241.toByte(), 158.toByte(), 19, 168.toByte(), 40, 64, 72, 149.toByte(), 103, 103, 241.toByte(), 112, 155.toByte(), 99, 103, 119, 246.toByte(), 10, 131.toByte())
    private val _uh = byteArrayOf(253.toByte(), 149.toByte(), 147.toByte(), 35, 133.toByte(), 202.toByte(), 176.toByte(), 182.toByte(), 78, 76)
    private val _p_https = byteArrayOf(3, 20, 27, 32, 168.toByte())

    // ── Decoys ────────────────────────────────────────────────────────────
    @Suppress("unused") private val _dc1 = byteArrayOf(91, 211.toByte(), 99, 123, 19, 65, 73, 59)

    // ── Lazily resolved ───────────────────────────────────────────────────
    val PLAYLIST_URL: String by lazy { X.s(_pu) }
    val ATTRIBUTION: String by lazy { X.s(_at) }
    private val UA_VAL: String by lazy { X.s(_ua) }
    private val UA_HDR: String by lazy { X.s(_uh) }
    private val P_HTTPS: String by lazy { X.s(_p_https) }

    fun readCachedChannels(context: Context): List<Channel> = runCatching {
        val cache = cacheFile(context)
        if (cache.isFile) parse(cache.readText(Charsets.UTF_8)) else emptyList()
    }.getOrDefault(emptyList())

    fun fetchChannels(context: Context): List<Channel> {
        val connection = (URL(PLAYLIST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty(UA_HDR, UA_VAL)
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Playlist response: ${connection.responseCode}")
            }
            val playlist = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val out = StringBuilder()
                val buffer = CharArray(8_192)
                var total = 0
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_PLAYLIST_CHARS) throw IOException("Playlist is too large")
                    out.append(buffer, 0, count)
                }
                out.toString()
            }
            val cache = cacheFile(context)
            runCatching { cache.writeText(playlist, Charsets.UTF_8) }
            return parse(playlist)
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(context: Context) = File(context.cacheDir, "public-channels.m3u")

    fun parse(playlist: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var attributes = emptyMap<String, String>()
        var displayName = ""

        playlist.lineSequence().forEach { rawLine ->
            val line = rawLine.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val comma = line.indexOf(',')
                    val metadata = if (comma >= 0) line.substring(0, comma) else line
                    displayName = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    attributes = ATTRIBUTE_REGEX.findAll(metadata)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                }

                line.isNotEmpty() && !line.startsWith("#") && displayName.isNotBlank() -> {
                    val uri = Uri.parse(line)
                    val mimeType = supportedMimeType(uri.path.orEmpty())
                    val name = displayName.ifBlank { attributes["tvg-name"].orEmpty() }
                    val streamUrl = line

                    if (
                        uri.scheme.equals(P_HTTPS, ignoreCase = true) &&
                        !uri.host.isNullOrBlank() &&
                        mimeType != null &&
                        !isPaidChannel(name)
                    ) {
                        val group = attributes["group-title"].orEmpty().ifBlank { "General" }
                        val id = attributes["tvg-id"].orEmpty().ifBlank {
                            "${name.hashCode()}-${streamUrl.hashCode()}"
                        }
                        result += Channel(
                            id = id,
                            name = name,
                            category = group,
                            mark = name.take(2).uppercase().ifBlank { "TV" },
                            accent = accentFor(group),
                            streamUrl = streamUrl,
                            mimeType = mimeType,
                            logoUrl = attributes["tvg-logo"]?.takeIf {
                                Uri.parse(it).scheme.equals(P_HTTPS, ignoreCase = true)
                            },
                        )
                    }

                    attributes = emptyMap()
                    displayName = ""
                }
            }
        }

        return result.distinctBy { it.id }.sortedBy { it.name.lowercase() }
    }

    private fun supportedMimeType(path: String): String? = when {
        path.endsWith(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
        path.endsWith(".mpd", ignoreCase = true) -> "application/dash+xml"
        path.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
        path.endsWith(".webm", ignoreCase = true) -> "video/webm"
        path.endsWith(".ts", ignoreCase = true) -> "video/mp2t"
        else -> null
    }

    private fun isPaidChannel(name: String): Boolean {
        val normalized = name.lowercase().replace(" ", "")
        return listOf("bein", "osn", "tod").any(normalized::contains)
    }

    private fun accentFor(group: String): Color = when {
        group.contains("sport", ignoreCase = true) -> Color(0xFF248A72)
        group.contains("news", ignoreCase = true) -> Color(0xFF4777B8)
        group.contains("kids", ignoreCase = true) || group.contains("family", ignoreCase = true) -> Color(0xFFB4607B)
        group.contains("movie", ignoreCase = true) || group.contains("entertain", ignoreCase = true) -> Color(0xFFC07548)
        else -> Color(0xFF587E8A)
    }

    private val ATTRIBUTE_REGEX = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")
    private const val MAX_PLAYLIST_CHARS = 4_000_000
}
