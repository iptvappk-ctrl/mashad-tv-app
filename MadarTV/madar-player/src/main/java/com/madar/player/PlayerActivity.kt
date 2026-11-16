package com.madar.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.Intent
import android.util.Log
import android.net.Uri
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.RenderProcessGoneDetail
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.text.BasicTextField
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.datasource.DefaultDataSource
import android.content.Context

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private data class PlaybackSource(
    val url: String,
    val userAgent: String,
    val referer: String,
    val headers: Map<String, String> = emptyMap(),
)

private data class VideoQualityTrack(
    val group: TrackGroup,
    val index: Int,
    val height: Int,
    val bitrate: Int,
)

class PlayerActivity : ComponentActivity() {

    private val currentStreamUrl = mutableStateOf("")
    private val currentTitle = mutableStateOf("ZAG Player")
    private val currentUserAgent = mutableStateOf("")
    private val currentReferer = mutableStateOf("")
    private val currentStreamHeaders = mutableStateOf<Map<String, String>>(emptyMap())
    private val currentFallbackUrl = mutableStateOf("")
    private val currentFallbackReferer = mutableStateOf("")
    private val currentIsLive = mutableStateOf(false)
    private val currentBackupSources = androidx.compose.runtime.mutableStateListOf<PlaybackSource>()
    private var launchedFromExternalIntent = false

    private val openVideoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val displayName = runCatching {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
                }
            }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "فيديو محلي"

            playStreamFromDashboard(
                title = displayName,
                url = uri.toString(),
                userAgent = ""
            )
        }
    }

    private fun enterFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    private fun exitFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun playStreamFromDashboard(
        title: String,
        url: String,
        userAgent: String = "",
        referer: String = ""
    ) {
        val cleanUrl = url.trim()
        if (!isSupportedStream(cleanUrl)) {
            Toast.makeText(this, "رابط الفيديو غير صالح", Toast.LENGTH_SHORT).show()
            return
        }
        val cleanTitle = title.trim().ifBlank { "ZAG Player" }
        PlayerStorage.addToHistory(
            this,
            SavedStreamItem(
                title = cleanTitle,
                url = cleanUrl,
                userAgent = userAgent.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
        currentStreamUrl.value = cleanUrl
        currentTitle.value = cleanTitle
        currentUserAgent.value = userAgent.trim()
        currentReferer.value = referer.trim()
        currentStreamHeaders.value = emptyMap()
        currentFallbackUrl.value = ""
        currentFallbackReferer.value = ""
        currentIsLive.value = cleanUrl.contains(".m3u8") || cleanUrl.contains("/live/")
        currentBackupSources.clear()
        enterFullscreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.player_enter, R.anim.player_exit)

        extractStreamFromIntent(intent)

        if (isSupportedStream(currentStreamUrl.value)) {
            launchedFromExternalIntent = true
            enterFullscreen()
        } else {
            launchedFromExternalIntent = false
            exitFullscreen()
        }

        setContent {
            MaterialTheme {
                val url = currentStreamUrl.value
                val isPlaying = isSupportedStream(url)

                if (isPlaying) {
                    BackHandler {
                        if (launchedFromExternalIntent) {
                            finish()
                        } else {
                            currentStreamUrl.value = ""
                            exitFullscreen()
                        }
                    }

                    PlayerScreen(
                        streamUrl = url,
                        title = currentTitle.value,
                        userAgent = currentUserAgent.value,
                        referer = currentReferer.value,
                        streamHeaders = currentStreamHeaders.value,
                        fallbackUrl = currentFallbackUrl.value,
                        fallbackReferer = currentFallbackReferer.value,
                        isLive = currentIsLive.value,
                        backupSources = currentBackupSources,
                        onSwitchStream = { next, nextUa, nextRef, nextHeaders ->
                            Log.i("MadarPlayer", "switch host=${safeHost(currentStreamUrl.value)} -> ${safeHost(next)}")
                            currentStreamUrl.value = next
                            currentUserAgent.value = nextUa.ifBlank { currentUserAgent.value }
                            currentReferer.value = nextRef.ifBlank { currentReferer.value }
                            currentStreamHeaders.value = nextHeaders
                        },
                        onClose = {
                            if (launchedFromExternalIntent) {
                                finish()
                            } else {
                                currentStreamUrl.value = ""
                                exitFullscreen()
                            }
                        }
                    )
                } else {
                    BackHandler {
                        finish()
                    }

                    PlayerDashboardScreen(
                        onPlay = { t, u, ua ->
                            playStreamFromDashboard(title = t, url = u, userAgent = ua)
                        },
                        onPickLocalFile = {
                            openVideoLauncher.launch("video/*")
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractStreamFromIntent(intent)
        if (isSupportedStream(currentStreamUrl.value)) {
            launchedFromExternalIntent = true
            enterFullscreen()
        }
    }

    private fun extractStreamFromIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val url = (intent.getStringExtra(EXTRA_STREAM_URL)
            ?: intent.getStringExtra("stream_url")
            ?: intent.dataString
            ?: intent.getStringExtra("url")
            ?: intent.getStringExtra("video_url")).orEmpty()
        val title = (intent.getStringExtra(EXTRA_TITLE)
            ?: intent.getStringExtra(android.content.Intent.EXTRA_TITLE)
            ?: intent.getStringExtra("title")).orEmpty().ifBlank { "زقزوقة بلاير" }
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()
        val referer = intent.getStringExtra(EXTRA_REFERER).orEmpty()
        val streamHeaders = runCatching {
            val headers = JSONObject(intent.getStringExtra(EXTRA_STREAM_HEADERS).orEmpty())
            buildMap {
                headers.keys().forEach { key ->
                    val value = headers.optString(key).trim()
                    if (key.isNotBlank() && value.isNotBlank()
                        && !key.contains("\r") && !key.contains("\n")
                        && !value.contains("\r") && !value.contains("\n")
                    ) put(key, value)
                }
            }
        }.getOrDefault(emptyMap())
        val fallbackUrl = intent.getStringExtra(EXTRA_FALLBACK_URL).orEmpty()
        val fallbackReferer = intent.getStringExtra(EXTRA_FALLBACK_REFERER).orEmpty()
        val isLive = intent.getBooleanExtra(EXTRA_IS_LIVE, false)

        val backupUrls = intent.getStringArrayListExtra("com.madar.player.extra.BACKUP_URLS")
            ?: intent?.getStringArrayListExtra("backup_stream_urls")
            ?: arrayListOf()

        val backupJson = intent.getStringExtra(EXTRA_BACKUP_SOURCES).orEmpty()
        val backupSources = runCatching {
            val json = JSONArray(backupJson)
            buildList {
                for (index in 0 until json.length()) {
                    val source = json.optJSONObject(index) ?: continue
                    val backupUrl = source.optString("url").orEmpty()
                    if (backupUrl.isNotBlank()) {
                        add(
                            PlaybackSource(
                                url = backupUrl,
                                userAgent = source.optString("userAgent").orEmpty().ifBlank { userAgent },
                                referer = source.optString("referer").orEmpty().ifBlank { referer },
                                headers = source.optJSONObject("headers")?.let { headers ->
                                    buildMap {
                                        headers.keys().forEach { key ->
                                            val value = headers.optString(key).trim()
                                            if (key.isNotBlank() && value.isNotBlank()) put(key, value)
                                        }
                                    }
                                }.orEmpty(),
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList()).ifEmpty {
            backupUrls.map { PlaybackSource(it, userAgent, referer) }
        }

        if (url.isNotBlank()) {
            currentStreamUrl.value = url
            currentTitle.value = title
            currentUserAgent.value = userAgent
            currentReferer.value = referer
            currentStreamHeaders.value = streamHeaders
            currentFallbackUrl.value = fallbackUrl
            currentFallbackReferer.value = fallbackReferer
            currentIsLive.value = isLive || (isHlsStream(url) &&
                (title.contains("مباشر") || title.contains("مباراة") || title.contains("beIN", true)))
            currentBackupSources.clear()
            currentBackupSources.addAll(
                backupSources.filter { it.url.isNotBlank() && it.url != url }.distinctBy { it.url }
            )
        }
    }

    companion object {
        const val EXTRA_STREAM_URL = "com.madar.player.extra.STREAM_URL"
        const val EXTRA_TITLE = "com.madar.player.extra.TITLE"
        const val EXTRA_USER_AGENT = "com.madar.player.extra.USER_AGENT"
        const val EXTRA_REFERER = "com.madar.player.extra.REFERER"
        const val EXTRA_STREAM_HEADERS = "com.madar.player.extra.STREAM_HEADERS"
        const val EXTRA_FALLBACK_URL = "com.madar.player.extra.FALLBACK_URL"
        const val EXTRA_FALLBACK_REFERER = "com.madar.player.extra.FALLBACK_REFERER"
        const val EXTRA_BACKUP_SOURCES = "com.madar.player.extra.BACKUP_SOURCES"
        const val EXTRA_IS_LIVE = "com.madar.player.extra.IS_LIVE"
    }
}

private fun isSupportedStream(url: String): Boolean =
    url.startsWith("http://", ignoreCase = true) ||
    url.startsWith("https://", ignoreCase = true) ||
    url.startsWith("content://", ignoreCase = true) ||
    url.startsWith("file://", ignoreCase = true)

private fun originForHeader(url: String): String = runCatching {
    val parsed = URI.create(url)
    "${parsed.scheme}://${parsed.host}" + (if (parsed.port >= 0) ":${parsed.port}" else "")
}.getOrDefault("")

private fun isYouTubeStream(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("youtube-nocookie.com")
}

// YouTube requires a stable app identity as the WebView document origin so its
// embedded player can identify the client (otherwise it returns Error 153).
private const val YOUTUBE_EMBED_ORIGIN = "https://com.madar.player"

private fun extractYouTubeId(url: String): String {
    return when {
        url.contains("/embed/") -> url.substringAfter("/embed/").substringBefore("?").substringBefore("&")
        url.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
        url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        else -> ""
    }
}

private fun formatTime(seconds: Long): String {
    val s = if (seconds < 0) 0L else seconds
    val m = s / 60
    val remSec = s % 60
    val h = m / 60
    val remMin = m % 60
    return if (h > 0) {
        "%02d:%02d:%02d".format(h, remMin, remSec)
    } else {
        "%02d:%02d".format(remMin, remSec)
    }
}

private fun toCleanEmbedUrl(url: String): String {
    if (!isYouTubeStream(url)) return url
    val id = extractYouTubeId(url)
    return if (id.isNotBlank()) {
        "https://www.youtube.com/embed/$id?autoplay=1&controls=0&rel=0&playsinline=1&enablejsapi=1&modestbranding=1&iv_load_policy=3&fs=0&cc_load_policy=0&origin=https%3A%2F%2Fcom.madar.player"
    } else url
}

private fun isDirectStreamUrl(url: String): Boolean {
    if (url.startsWith("content://", ignoreCase = true) || url.startsWith("file://", ignoreCase = true)) {
        return true
    }
    val clean = url.substringBefore('?')
    // Never hand a poster, thumbnail, or site icon to ExoPlayer as a video.
    if (clean.matches(Regex("(?i).*\\.(apng|avif|bmp|gif|jpe?g|png|svg|webp)$"))) return false
    return clean.endsWith(".m3u8", ignoreCase = true) ||
           clean.endsWith(".mpd", ignoreCase = true) ||
           clean.endsWith(".mp4", ignoreCase = true) ||
           clean.endsWith(".mkv", ignoreCase = true) ||
           clean.endsWith(".webm", ignoreCase = true) ||
           clean.endsWith(".m4v", ignoreCase = true) ||
           clean.endsWith(".mov", ignoreCase = true) ||
           clean.endsWith(".avi", ignoreCase = true) ||
           clean.endsWith(".ts", ignoreCase = true) ||
           url.contains("/live/", ignoreCase = true) ||
           url.contains("index.m3u8", ignoreCase = true) ||
           url.contains(".m3u8", ignoreCase = true) ||
           url.contains("video.mp4", ignoreCase = true) ||
           url.contains("/hls/", ignoreCase = true) ||
           url.contains("master.m3u8", ignoreCase = true)
}

// ── Complete Ad Blocker Domain Filter ──────────────────────────────────────────
private val AD_HOST_MARKERS = listOf(
    "doubleclick", "googlead", "googlesyndication", "popads", "adsterra",
    "propellerads", "monetag", "trafficjunky", "histats", "onclickmega",
    "ad-maven", "adcash", "exoclick", "juicyads", "bet365", "1xbet", "melbet",
    "facebook.com", "twitter.com", "instagram.com", "llvpn.com", "syndication",
    "vidoomy", "adservice", "adsystem", "coinhive", "adtrue", "revenuehits",
    "adcolony", "inmobi", "taboola", "outbrain", "adnxs", "criteo",
    "google.com", "googleleadservices", "google-analytics", "bonus", "casino",
    "777", "jackpot", "winner", "reward", "lottery", "survey", "yango", "yandex",
    "redirect", "affiliate", "landing", "mostbet", "linebet", "winline", "bet"
)

private fun isBlockedAd(url: String): Boolean {
    val parsed = runCatching { URI.create(url) }.getOrNull() ?: return false
    val host = parsed.host.orEmpty().lowercase(Locale.ROOT)
    val path = parsed.path.orEmpty()
    // Do not block legitimate YouTube requests
    if (host.contains("youtube.com") || host.contains("googlevideo.com") || host.contains("ytimg.com")) {
        return false
    }
    if (AD_HOST_MARKERS.any(host::contains)) return true
    // Match whole path segments only. A substring check on the entire URL blocked valid
    // media paths such as /download/video.mp4 and URLs with unrelated query values.
    return Regex("(?i)(^|/)(ads?|advert|banner|popunder|onclick|tracking|analytics|bonus|casino)(/|$)")
        .containsMatchIn(path)
}

private fun isKnownVideoHost(host: String): Boolean {
    val h = host.lowercase(Locale.ROOT)
    val videoHosts = listOf(
        "vidmoly", "vidplay", "vidsrc", "vidhide", "vidguard", "vido", "vidcdn",
        "streamtape", "dood", "voe", "mixdrop", "filemoon", "upstream", "upvid",
        "streamwish", "mega4up", "govid", "player2", "akwam", "arabseed", "fasel",
        "youtube", "googlevideo", "fastly", "cloudflare", "bunny"
    )
    return videoHosts.any { h.contains(it) }
}

private fun safeHost(url: String): String =
    runCatching { URI.create(url).host.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")

private fun isMediaRequest(url: String): Boolean {
    val parsed = runCatching { URI.create(url) }.getOrNull() ?: return false
    val host = parsed.host.orEmpty().lowercase(Locale.ROOT)
    val path = parsed.path.orEmpty().lowercase(Locale.ROOT)
    val query = parsed.query.orEmpty().lowercase(Locale.ROOT)
    val full = url.lowercase(Locale.ROOT)

    // Ignore non-media web documents, scripts, and styling immediately
    val ignoredExts = listOf(".html", ".htm", ".php", ".js", ".css", ".json", ".xml", ".png", ".jpg", ".jpeg", ".webp", ".svg", ".gif", ".ico", ".woff", ".woff2", ".ttf")
    if (ignoredExts.any { path.endsWith(it) || query.endsWith(it) }) return false

    // امتدادات الـ video الحقيقية المؤكدة
    val videoExts = listOf(".m3u8", ".mpd", ".mp4", ".mkv", ".webm", ".m4v", ".mov", ".ts")
    if (videoExts.any { path.substringBefore('?').endsWith(it) }) return true
    if (videoExts.any { query.contains(it) }) return true

    // مسارات بث HLS/DASH شائعة فقط لو مش صفحة ويب
    if (path.contains("/hls/") || path.contains("/dash/") || path.contains("/stream/") ||
        path.contains("/video/") || path.contains("/videos/") || path.contains("/media/") ||
        path.contains("index.m3u8") || path.contains("master.m3u8") || path.contains("playlist.m3u8") ||
        path.contains("video.mp4") || path.contains("manifest.mpd")) {
        return true
    }

    // Direct video endpoints for specific video hosts (with actual video tokens/endpoints)
    if ((host.contains("dood") || host.contains("ds2play")) && (path.contains("/pass_md5/") || path.contains("/cdn/"))) return true
    if ((host.contains("streamtape") || host.contains("tapecontent")) && (path.contains("/get_video") || path.contains("/getvideo"))) return true
    if ((host.contains("voe.sx") || host.contains("voe-network")) && path.contains(".m3u8")) return true
    if (host.contains("mixdrop") && (path.contains("/stream/") || path.contains("/delivery/"))) return true
    if ((host.contains("filemoon") || host.contains("moonembed")) && path.contains(".m3u8")) return true
    if ((host.contains("vidmoly") || host.contains("vidhide") || host.contains("vidguard")) && (path.contains(".m3u8") || path.contains(".mp4"))) return true
    if ((host.contains("upstream") || host.contains("upvid") || host.contains("uqload")) && (path.contains(".mp4") || path.contains(".m3u8"))) return true
    if (host.contains("streamwish") && (path.contains(".m3u8") || path.contains(".mp4"))) return true

    return false
}

private fun inspectMediaResponse(url: String, userAgent: String, referer: String): String {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "*/*")
            if (referer.isNotBlank()) {
                setRequestProperty("Referer", referer)
                originForHeader(referer).takeIf(String::isNotBlank)?.let { setRequestProperty("Origin", it) }
            }
        }
        val status = connection.responseCode
        val contentType = connection.contentType.orEmpty().substringBefore(';').ifBlank { "content-type לא נُشر" }
        val input = if (status in 200..299) connection.inputStream else connection.errorStream
        val prefix = input?.use { stream ->
            val bytes = ByteArray(512)
            val count = stream.read(bytes)
            if (count > 0) bytes.copyOf(count) else ByteArray(0)
        } ?: ByteArray(0)
        val textPrefix = String(prefix, Charsets.UTF_8).trimStart('\uFEFF', '\u0000', ' ', '\t', '\r', '\n')
        val kind = when {
            textPrefix.startsWith("#EXTM3U", ignoreCase = true) -> "HLS"
            textPrefix.startsWith("<!DOCTYPE", ignoreCase = true) || textPrefix.startsWith("<html", ignoreCase = true) -> "HTML"
            textPrefix.startsWith("{") || textPrefix.startsWith("[") -> "JSON"
            prefix.firstOrNull()?.toInt()?.and(0xff) == 0x47 -> "MPEG-TS"
            else -> "محتوى غير معروف"
        }
        "رد السيرفر: HTTP $status · $kind · $contentType"
    } catch (error: Exception) {
        "تعذر فحص رد السيرفر (${error.javaClass.simpleName})."
    } finally {
        connection?.disconnect()
    }
}

// ── High Speed Background Stream Extractor ────────────────────────────────────
private fun extractDirectStream(urlStr: String, referer: String, ua: String): String? {
    try {
        if (isDirectStreamUrl(urlStr)) return urlStr

        // 1. Vidara / Sugbaioug API
        if (urlStr.contains("vidara.") || urlStr.contains("sugbaioug.")) {
            val fileCode = urlStr.trimEnd('/').substringAfterLast('/')
            if (fileCode.isNotBlank()) {
                val conn = (URL("https://vidara.to/api/stream").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", ua)
                    setRequestProperty("Referer", urlStr)
                    setRequestProperty("Origin", "https://vidara.to")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                val payload = """{"filecode":"$fileCode","device":"android","codecs":["av01.0.09M.10"]}"""
                conn.outputStream.use { it.write(payload.toByteArray()) }
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val match = Regex(""""streaming_url"\s*:\s*"([^"]+)"""").find(resp)
                match?.groupValues?.get(1)?.let { stream ->
                    val clean = stream.replace("\\/", "/")
                    if (clean.startsWith("http")) return clean
                }
            }
        }

        // 2. Fetch page HTML to find direct video or gateway
        val origin = try {
            val u = URI.create(urlStr)
            "${u.scheme}://${u.host}/"
        } catch (_: Exception) { referer.ifBlank { "https://m.myseed.tv/" } }

        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", ua)
            setRequestProperty("Referer", referer.ifBlank { origin })
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }

        val responseCode = conn.responseCode
        if (responseCode == 404 || responseCode == 410 || responseCode >= 500) {
            return null // File is dead, skip immediately
        }

        val html = conn.inputStream.bufferedReader().use { it.readText() }

        // Fast 404 check inside response body
        if (Regex("(?i)(404\\s*not\\s*found|video\\s*(?:not\\s*found|deleted|removed)|file\\s*(?:not\\s*found|deleted))").containsMatchIn(html.take(3000))) {
            return null // Deleted or not found
        }

        // Prefer an adaptive HLS manifest over a progressive MP4
        val sourcePattern = Regex(
            """<source\s+[^>]*src=["']([^"']+\.(?:mp4|m3u8)(?:\?[^"']*)?)["']""",
            RegexOption.IGNORE_CASE,
        )
        val sourceCandidates = sourcePattern.findAll(html).mapNotNull {
            it.groupValues.getOrNull(1)?.replace("&amp;", "&")?.trim()
        }.toList()
        (sourceCandidates.firstOrNull { it.substringBefore('?').endsWith(".m3u8", true) }
            ?: sourceCandidates.firstOrNull())?.let { return it }

        // Vidmoly direct master.m3u8 pattern
        val vidmolyMatch = Regex("""(?:sources|file)\s*:\s*\[?\s*\{?\s*['"]?file['"]?\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""['"](https?://[a-zA-Z0-9.-]+\.vidmoly\.[a-z]+/[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(html)
        vidmolyMatch?.groupValues?.get(1)?.let { return it.replace("&amp;", "&") }

        // Voe direct HLS manifest
        val voeMatch = Regex("""['"]hls['"]\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""['"](https?://[a-zA-Z0-9.-]+\.voe-network\.[a-z]+/[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(html)
        voeMatch?.groupValues?.get(1)?.let { return it.replace("&amp;", "&") }

        // Uqload mp4 direct
        val uqMatch = Regex("""sources\s*:\s*\[\s*["']([^"']+\.mp4[^"']*)["']""").find(html)
        uqMatch?.groupValues?.get(1)?.let { return it }

        // General m3u8 manifest in scripts
        val m3u8Match = Regex("""["'](https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)["']""", RegexOption.IGNORE_CASE).find(html)
        m3u8Match?.groupValues?.get(1)?.let { return it.replace("&amp;", "&") }

        // Iframe check (e.g. gateway -> real player)
        val ifrMatch = Regex("""<iframe[^>]+src=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        ifrMatch?.groupValues?.get(1)?.let { ifrUrl ->
            val cleanIfr = ifrUrl.replace("&amp;", "&")
            if (!isBlockedAd(cleanIfr) && cleanIfr != urlStr) {
                return extractDirectStream(cleanIfr, origin, ua)
            }
        }
    } catch (_: Exception) {}
    return null
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(
    streamUrl: String,
    title: String,
    userAgent: String,
    referer: String,
    streamHeaders: Map<String, String>,
    fallbackUrl: String,
    fallbackReferer: String,
    isLive: Boolean,
    backupSources: androidx.compose.runtime.snapshots.SnapshotStateList<PlaybackSource>,
    onSwitchStream: (String, String, String, Map<String, String>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val diagnosticScope = rememberCoroutineScope()
    val effectiveStreamUrl = remember(streamUrl) { toCleanEmbedUrl(streamUrl) }
    val isYouTube = remember(effectiveStreamUrl) { isYouTubeStream(effectiveStreamUrl) }
    val isDirectStream = isDirectStreamUrl(effectiveStreamUrl)
    val isEmbedPage = !isDirectStream
    var useWebViewFallback by remember(effectiveStreamUrl) { mutableStateOf(isEmbedPage) }
    var exoRetryKey by remember(effectiveStreamUrl) { mutableIntStateOf(0) }
    var isBuffering by remember(effectiveStreamUrl) { mutableStateOf(!isYouTube) }
    var playbackError by remember(effectiveStreamUrl) { mutableStateOf<String?>(null) }
    var sourceDiagnostic by remember(effectiveStreamUrl) { mutableStateOf<String?>(null) }

    // YouTube custom native player states
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var ytCurrentSec by remember(effectiveStreamUrl) { mutableFloatStateOf(0f) }
    var ytDurationSec by remember(effectiveStreamUrl) { mutableFloatStateOf(0f) }
    var ytSliderValue by remember(effectiveStreamUrl) { mutableStateOf<Float?>(null) }
    var ytIsPlaying by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var ytIsBuffering by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var showYouTubeFallbackPrompt by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var ytSpeed by remember(effectiveStreamUrl) { mutableFloatStateOf(1.0f) }
    var ytDoubleTapSide by remember { mutableStateOf<String?>(null) }
    var ytDoubleTapKey by remember { mutableIntStateOf(0) }
    var showDoubleTapIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(ytDoubleTapKey) {
        if (ytDoubleTapKey > 0) {
            showDoubleTapIndicator = true
            kotlinx.coroutines.delay(650)
            showDoubleTapIndicator = false
        }
    }

    LaunchedEffect(isYouTube, effectiveStreamUrl, ytIsPlaying) {
        if (!isYouTube) return@LaunchedEffect
        kotlinx.coroutines.delay(3500)
        if (!ytIsPlaying) {
            showYouTubeFallbackPrompt = true
        }
    }

    LaunchedEffect(isYouTube, effectiveStreamUrl, webViewRef) {
        if (!isYouTube) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(400)
            webViewRef?.evaluateJavascript(
                "(function(){ try { if (window.__madar_clean) window.__madar_clean(); } catch(e){} })();",
                null
            )
        }
    }

    val ytSeekBy: (Float) -> Unit = remember(webViewRef) {
        { deltaSeconds ->
            val js = """
                (function() {
                    var v = document.querySelector('video');
                    if (v) {
                        var target = Math.max(0, Math.min((v.currentTime || 0) + ($deltaSeconds), v.duration || 999999));
                        v.currentTime = target;
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    val cleanUa = if (userAgent.isNotBlank()) {
        userAgent
    } else {
        "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    val cleanRef = if (referer.isNotBlank()) referer else try {
        val u = URI.create(effectiveStreamUrl)
        "${u.scheme}://${u.host}/"
    } catch (_: Exception) { "" }

    // ── Fast Real-Time Background Stream Extractor ─────────────────────────
    LaunchedEffect(effectiveStreamUrl) {
        if (!isDirectStreamUrl(effectiveStreamUrl) && !isYouTubeStream(effectiveStreamUrl)) {
            withContext(Dispatchers.IO) {
                val direct = extractDirectStream(effectiveStreamUrl, cleanRef, cleanUa)
                if (!direct.isNullOrBlank() && direct != effectiveStreamUrl) {
                    withContext(Dispatchers.Main) {
                    // A media URL extracted from an embed is normally hotlink-protected.
                    // Keep the embed page as Referer when handing that media URL to ExoPlayer.
                    onSwitchStream(direct, cleanUa, effectiveStreamUrl.ifBlank { cleanRef }, streamHeaders)
                    }
                }
            }
        }
    }

    // Media3 TrackSelector instances are attached to exactly one ExoPlayer.
    // The player can be recreated when the embed fallback or retry key changes;
    // key the selector to the same lifecycle so a second player never calls
    // TrackSelector.init() on an instance already owned by the previous player.
    val trackSelector = remember(effectiveStreamUrl, streamHeaders, useWebViewFallback, exoRetryKey) {
        DefaultTrackSelector(context)
    }
    val preferences = remember(context) { context.getSharedPreferences("madar_player", 0) }
    var selectedQuality by remember {
        mutableStateOf(preferences.getString("quality", "ضغط ذكي (HD موفّر)") ?: "ضغط ذكي (HD موفّر)")
    }
    var showBandwidthSheet by remember { mutableStateOf(false) }
    var showTvConnectDialog by remember { mutableStateOf(false) }
    var currentTrackInfo by remember(streamUrl) { mutableStateOf("جارٍ فحص مسارات المصدر") }
    var availableVideoTracks by remember(streamUrl) { mutableStateOf<List<VideoQualityTrack>>(emptyList()) }
    // Live bitrate read from ExoPlayer for real-time display
    var currentBitrateKbps by remember(streamUrl) { mutableIntStateOf(0) }
    val hasQualityAlternatives = availableVideoTracks.size > 1
    val qualityLabels = remember(availableVideoTracks) {
        val repeatedHeights = availableVideoTracks.groupingBy { it.height }.eachCount()
        availableVideoTracks.map { option ->
            val base = "${option.height}p"
            if ((repeatedHeights[option.height] ?: 0) > 1 && option.bitrate > 0) {
                "$base · ${option.bitrate / 1000} kbps"
            } else base
        }
    }
    var downloadedBytes by remember(streamUrl) { mutableLongStateOf(0L) }
    var bufferingSinceMs by remember(effectiveStreamUrl) { mutableLongStateOf(0L) }
    var bufferingFallbackUsed by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(effectiveStreamUrl) { mutableStateOf(false) }
    val streamStartedMs = remember(effectiveStreamUrl) { System.currentTimeMillis() }

    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(showControls, lastInteractionTime, showBandwidthSheet, showTvConnectDialog, hasRenderedFirstFrame, ytIsPlaying) {
        val isPlayingVideo = hasRenderedFirstFrame || ytIsPlaying
        if (showControls && isPlayingVideo && !showBandwidthSheet && !showTvConnectDialog) {
            kotlinx.coroutines.delay(5000)
            showControls = false
        }
    }

    LaunchedEffect(trackSelector, selectedQuality, availableVideoTracks, qualityLabels) {
        val effectiveQuality = selectedQuality
        applyQuality(trackSelector, effectiveQuality, availableVideoTracks, qualityLabels)
    }

    val player = remember(effectiveStreamUrl, streamHeaders, useWebViewFallback, exoRetryKey) {
        if (useWebViewFallback) return@remember null

        val sourceOrigin = streamHeaders.entries
            .firstOrNull { it.key.equals("Origin", ignoreCase = true) }
            ?.value
            ?.trim()
        val headers = streamHeaders
            .filterKeys { key -> !key.equals("User-Agent", true) && !key.equals("Referer", true) && !key.equals("Origin", true) }
            .toMutableMap()
            .apply {
                put("User-Agent", cleanUa)
                if (cleanRef.isNotBlank()) put("Referer", cleanRef)
                // Preserve the provider's explicit origin; some live streams reject a
                // request when Origin is guessed from a different Referer host.
                val origin = sourceOrigin?.takeIf(String::isNotBlank)
                    ?: originForHeader(cleanRef).takeIf(String::isNotBlank)
                if (origin != null) put("Origin", origin)
            }
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(cleanUa)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(30000)
            .setDefaultRequestProperties(headers)
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        val isHls = isHlsStream(effectiveStreamUrl)

        val mediaItem = MediaItem.Builder()
            .setUri(effectiveStreamUrl)
            .apply {
                if (isHls) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
            .build()

        val mediaSource = if (isHls) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(false)
                .createMediaSource(mediaItem)
        } else {
            DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 8_000,
                /* maxBufferMs = */ 20_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        hasRenderedFirstFrame = true
                        isBuffering = false
                        playbackError = null
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = (state == Player.STATE_BUFFERING)
                        if (state == Player.STATE_BUFFERING) {
                            if (bufferingSinceMs == 0L) bufferingSinceMs = System.currentTimeMillis()
                        } else {
                            bufferingSinceMs = 0L
                        }
                        if (state == Player.STATE_READY) {
                            if (currentPosition > 0L) {
                                hasRenderedFirstFrame = true
                            }
                            playbackError = null
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (backupSources.isNotEmpty()) {
                            val next = backupSources.removeAt(0)
                            onSwitchStream(
                                next.url,
                                next.userAgent.ifBlank { cleanUa },
                                next.referer.ifBlank { cleanRef },
                                next.headers,
                            )
                            return
                        }
                        val shouldOpenSourcePage = fallbackUrl.isNotBlank()
                            && fallbackUrl != streamUrl
                            && (isEmbedPage || error.errorCodeName.contains("PARSING_MANIFEST_MALFORMED"))
                        if (shouldOpenSourcePage) {
                            onSwitchStream(fallbackUrl, cleanUa, fallbackReferer.ifBlank { cleanRef }, streamHeaders)
                            return
                        }
                        playbackError = when {
                            error.errorCodeName.contains("PARSING_MANIFEST_MALFORMED") ->
                                "قائمة البث التي أرسلها السيرفر غير صالحة. جرّب سيرفرًا آخر."
                            error.errorCodeName.contains("NETWORK") ->
                                "تعذر الاتصال بالسيرفر. تحقق من الإنترنت أو اختر سيرفرًا آخر."
                            else -> "تعذر تشغيل هذا السيرفر (${error.errorCodeName}). اختر سيرفرًا آخر."
                        }
                        isBuffering = false
                        diagnosticScope.launch {
                            sourceDiagnostic = withContext(Dispatchers.IO) {
                                inspectMediaResponse(streamUrl, cleanUa, cleanRef)
                            }
                        }
                        if (isEmbedPage) {
                            useWebViewFallback = true
                        }
                    }
                })
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(1000)
            val format = p.videoFormat
            val br = if (format != null && format.bitrate > 0) format.bitrate else if (format != null && format.averageBitrate > 0) format.averageBitrate else null
            if (br != null) currentBitrateKbps = br / 1000

            val elapsedMs = System.currentTimeMillis() - streamStartedMs
            val bufferingForMs = bufferingSinceMs.takeIf { it > 0L }
                ?.let { System.currentTimeMillis() - it } ?: 0L

            // ── Auto Black Screen Detection (no frame rendered after 7s or stuck buffering) ──
            val isBlackScreen = !hasRenderedFirstFrame && elapsedMs >= 7_000L && p.currentPosition == 0L
            val isEndlessBuffering = p.playWhenReady && p.playbackState == Player.STATE_BUFFERING && bufferingForMs >= 7_000L

            if (!bufferingFallbackUsed && (isBlackScreen || isEndlessBuffering)) {
                bufferingFallbackUsed = true
                Log.w("MadarPlayer", "Auto Black Screen / Buffer Recovery triggered (elapsed=${elapsedMs}ms, buffering=${bufferingForMs}ms). Backups left: ${backupSources.size}")
                if (backupSources.isNotEmpty()) {
                    val next = backupSources.removeAt(0)
                    Log.i("MadarPlayer", "Switching to next backup: ${safeHost(next.url)}")
                    onSwitchStream(
                        next.url,
                        next.userAgent.ifBlank { cleanUa },
                        next.referer.ifBlank { cleanRef },
                        next.headers,
                    )
                } else {
                    playbackError = "تعذر تشغيل هذا السيرفر (شاشة سوداء). اضغط لإعادة المحاولة أو اختر سيرفرًا آخر."
                    isBuffering = false
                }
            }
        }
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val tracksListener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val videoGroups = tracks.groups.filter {
                    it.type == C.TRACK_TYPE_VIDEO && it.isSupported
                }
                val selectedFormat = videoGroups.asSequence()
                    .flatMap { group ->
                        (0 until group.length).asSequence().filter { group.isTrackSelected(it) }.map { group.getTrackFormat(it) }
                    }
                    .firstOrNull()

                val options = videoGroups.flatMap { group ->
                    (0 until group.length).mapNotNull { index ->
                        val format = group.getTrackFormat(index)
                        if (format.height <= 0) null else VideoQualityTrack(
                            group = group.mediaTrackGroup,
                            index = index,
                            height = format.height,
                            bitrate = format.bitrate,
                        )
                    }
                }.distinctBy { it.height to it.bitrate }
                    .sortedWith(compareBy<VideoQualityTrack> { it.height }.thenBy { it.bitrate })
                availableVideoTracks = options

                if (selectedFormat != null && selectedFormat.bitrate > 0) {
                    currentBitrateKbps = selectedFormat.bitrate / 1000
                }

                val resText = when {
                    selectedFormat == null -> "تلقائي"
                    selectedFormat.height > 0 -> "${selectedFormat.height}p"
                    else -> "مباشر"
                }
                val bitrateText = when {
                    selectedFormat == null || selectedFormat.bitrate <= 0 -> ""
                    selectedFormat.bitrate >= 1_000_000 -> " · ${(selectedFormat.bitrate / 1_000_000f).format(1)} Mbps"
                    else -> " · ${selectedFormat.bitrate / 1000} kbps"
                }
                val streamType = if (isHlsStream(streamUrl)) "HLS" else "MP4"
                val trackCount = if (options.size > 1) " · ${options.size} مسارات جودة" else " · جودة واحدة"
                currentTrackInfo = "$streamType · $resText$bitrateText$trackCount"
            }
        }
        player.addListener(tracksListener)
        onDispose { player.removeListener(tracksListener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (useWebViewFallback) {
            // Fullscreen hardware-accelerated WebView for supported embed pages.
            key(effectiveStreamUrl, exoRetryKey) {
                val webViewActive = remember(effectiveStreamUrl) { AtomicBoolean(true) }
                val pageProbeHandler = remember(effectiveStreamUrl) { Handler(Looper.getMainLooper()) }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                    val mediaHandoffScheduled = AtomicBoolean(false)
                    val pagePlayableObserved = AtomicBoolean(false)
                    val pageFailureScheduled = AtomicBoolean(false)
                    val activePageUrl = AtomicReference(cleanRef.ifBlank { effectiveStreamUrl })
                    val pageStartedAt = AtomicLong(0L)
                    WebView(viewContext).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        if (isYouTube) {
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onTimeUpdate(currentTime: Float, duration: Float) {
                                        pageProbeHandler.post {
                                            if (ytSliderValue == null) {
                                                ytCurrentSec = currentTime
                                            }
                                            if (duration > 0f) {
                                                ytDurationSec = duration
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onStateChange(state: String) {
                                        pageProbeHandler.post {
                                            when (state) {
                                                "playing" -> {
                                                    ytIsPlaying = true
                                                    ytIsBuffering = false
                                                    isBuffering = false
                                                }
                                                "paused" -> {
                                                    ytIsPlaying = false
                                                }
                                                "buffering" -> {
                                                    ytIsBuffering = true
                                                }
                                                "ended" -> {
                                                    ytIsPlaying = false
                                                    ytIsBuffering = false
                                                    showControls = true
                                                }
                                            }
                                        }
                                    }
                                },
                                "MadarBridge"
                            )
                        }
                        setOnTouchListener { _, event ->
                            if (isYouTube) {
                                true
                            } else {
                                if (event.action == android.view.MotionEvent.ACTION_UP) {
                                    showControls = !showControls
                                    if (showControls) lastInteractionTime = System.currentTimeMillis()
                                }
                                false
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            if (!isYouTube) userAgentString = cleanUa
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                return false // Block any popup window attempts
                            }

                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                Log.i("MadarYtDOM", "${consoleMessage?.message()}")
                                return true
                            }

                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            private fun handoffMedia(url: String, requestHeaders: Map<String, String> = emptyMap()) {
                                if (isYouTube || !isMediaRequest(url) ||
                                    !mediaHandoffScheduled.compareAndSet(false, true)
                                ) return
                                val pageReferer = activePageUrl.get().ifBlank {
                                    cleanRef.ifBlank { effectiveStreamUrl }
                                }
                                val handoffHeaders = requestHeaders
                                    .filterKeys { key ->
                                        key.equals("cookie", true) || key.equals("authorization", true) ||
                                            key.equals("origin", true) || key.equals("referer", true)
                                    }
                                    .toMutableMap()
                                CookieManager.getInstance().getCookie(url)
                                    ?.takeIf(String::isNotBlank)
                                    ?.let { handoffHeaders["Cookie"] = it }
                                if (handoffHeaders.keys.none { it.equals("Referer", true) }) {
                                    handoffHeaders["Referer"] = pageReferer
                                }
                                pageProbeHandler.post {
                                    onSwitchStream(url, cleanUa, pageReferer, streamHeaders + handoffHeaders)
                                }
                            }

                            private fun handleMainFrameFailure(message: String) {
                                if (!pageFailureScheduled.compareAndSet(false, true)) return
                                Log.w("MadarPlayer", "source failed host=${safeHost(activePageUrl.get())} backups=${backupSources.size}")
                                if (backupSources.isNotEmpty()) {
                                    val next = backupSources.removeAt(0)
                                    Log.i("MadarPlayer", "trying backup host=${safeHost(next.url)}")
                                    pageFailureScheduled.set(false)
                                    mediaHandoffScheduled.set(false)
                                    pagePlayableObserved.set(false)
                                    onSwitchStream(
                                        next.url,
                                        next.userAgent.ifBlank { cleanUa },
                                        next.referer.ifBlank { cleanRef },
                                        next.headers,
                                    )
                                } else {
                                    playbackError = message
                                    isBuffering = false
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?,
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request?.isForMainFrame == true) {
                                    val status = errorResponse?.statusCode ?: 0
                                    handleMainFrameFailure("تعذر فتح صفحة السيرفر (HTTP $status). جرّب مصدرًا آخر.")
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: android.webkit.SslErrorHandler?,
                                error: android.net.http.SslError?,
                            ) {
                                handler?.proceed()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?,
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    handleMainFrameFailure("تعذر الاتصال بصفحة السيرفر. تحقق من الاتصال أو جرّب مصدرًا آخر.")
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?,
                            ): Boolean {
                                if (isYouTube) {
                                    // Renderer memory/GPU reset; reload gracefully
                                    view?.reload()
                                    return true
                                }
                                view?.let { webView ->
                                    webViewActive.set(false)
                                    pageProbeHandler.removeCallbacksAndMessages(null)
                                    (webView.parent as? ViewGroup)?.removeView(webView)
                                    webView.destroy()
                                }
                                handleMainFrameFailure("توقف عارض صفحة السيرفر أثناء التشغيل. جارٍ تجربة سيرفر آخر.")
                                return true
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return true
                                val uri = request.url ?: return true
                                val host = uri.host?.lowercase(Locale.ROOT).orEmpty()

                                if (isYouTube) {
                                    // Block navigation away from embed, keep playback strictly inside native player
                                    return true
                                }
                                if (reqUrl.startsWith("intent:", true) || reqUrl.startsWith("market:", true) ||
                                    reqUrl.startsWith("whatsapp:", true) || reqUrl.startsWith("tg:", true)
                                ) return true

                                if (isBlockedAd(reqUrl)) {
                                    if (request.isForMainFrame) {
                                        handleMainFrameFailure("تم إيقاف صفحة إعلانية، جارٍ تجربة سيرفر آخر.")
                                    }
                                    return true
                                }

                                // Prevent ad redirects from hijacking the main player frame to Google or external sites
                                if (request.isForMainFrame) {
                                    val originalHost = safeHost(effectiveStreamUrl)
                                    if (originalHost.isNotBlank() && host.isNotBlank()) {
                                        val isSameHost = host.endsWith(originalHost) || originalHost.endsWith(host)
                                        if (!isSameHost && !isMediaRequest(reqUrl) && !isKnownVideoHost(host)) {
                                            Log.w("MadarPlayer", "Blocked main frame navigation to foreign host: $host")
                                            return true
                                        }
                                    }
                                }
                                return false
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: return null
                                if (!isYouTube && isBlockedAd(reqUrl)) {
                                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                }
                                if (!isYouTube && isMediaRequest(reqUrl)) {
                                    handoffMedia(reqUrl, request?.requestHeaders.orEmpty())
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (!webViewActive.get()) return
                                val startedAt = System.currentTimeMillis()
                                pageStartedAt.set(startedAt)
                                pagePlayableObserved.set(false)
                                pageFailureScheduled.set(false)
                                if (!url.isNullOrBlank()) activePageUrl.set(url)
                                if (!isYouTube) {
                                    view?.evaluateJavascript(
                                        "(function(){ try { window.open=function(){return null;}; window.alert=function(){}; window.confirm=function(){return false;}; } catch(e){} })();",
                                        null
                                    )
                                    // ── Black Screen Watchdog 1: 8 seconds (if no video playing) ──
                                    pageProbeHandler.postDelayed({
                                        if (pageStartedAt.get() != startedAt || pageFailureScheduled.get() ||
                                            pagePlayableObserved.get() || !webViewActive.get()
                                        ) return@postDelayed
                                        Log.w("MadarPlayer", "WebView black screen watchdog 8s. Backups left: ${backupSources.size}")
                                        handleMainFrameFailure(
                                            "شاشة سوداء: السيرفر لم يبث فيديو خلال 8 ثوانٍ. جارٍ التبديل لسيرفر آخر تلقائياً..."
                                        )
                                    }, 8_000L)

                                    // ── Black Screen Watchdog 2: 14 seconds (maximum fallback threshold) ──
                                    pageProbeHandler.postDelayed({
                                        if (pageStartedAt.get() != startedAt || pageFailureScheduled.get() ||
                                            !webViewActive.get()
                                        ) return@postDelayed
                                        if (!pagePlayableObserved.get()) {
                                            Log.w("MadarPlayer", "WebView black screen watchdog 14s. Backups left: ${backupSources.size}")
                                            handleMainFrameFailure(
                                                "السيرفر لم يبدأ فيديو قابلًا للتشغيل خلال المهلة. جارٍ الانتقال لمصدر آخر..."
                                            )
                                        }
                                    }, 14_000L)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!webViewActive.get()) return
                                if (isYouTube) {
                                    val cleanScript = """
                                        (function() {
                                            var selectors = [
                                                '#player-controls',
                                                '#player-control-overlay',
                                                '.player-controls-content',
                                                '.player-controls-background',
                                                '.player-controls-background-container',
                                                'creator-endscreen',
                                                'expanding-endscreen-element',
                                                'ytm-custom-control',
                                                'ytm-watch-player-controls',
                                                'ytm-endscreen-video-renderer',
                                                'ytm-video-with-context-renderer',
                                                'video-cover',
                                                'cued-overlay',
                                                '.ytmCuedOverlayPlayButton',
                                                '.ytmWatchPlayerControlsBackgroundActionItems',
                                                '.ytmExpandingEndscreenElementScrim',
                                                '.ytmExpandingEndscreenElementOverlay',
                                                '.ytFullscreenVideoRecommendationsRecommendation',
                                                '#bottom-sheet-wrapper',
                                                '.ytp-chrome-top',
                                                '.ytp-chrome-top-buttons',
                                                '.ytp-chrome-bottom',
                                                '.ytp-watermark',
                                                '.ytp-watermark-small',
                                                '.ytp-muted-autoplay-watermark',
                                                'a.ytp-watermark',
                                                '.ytp-pause-overlay',
                                                '.ytp-pause-overlay-backdrop',
                                                '.ytp-pause-overlay-container',
                                                '.ytp-expand-pause-overlay',
                                                '.ytp-scroll-min',
                                                '.ytp-endscreen-content',
                                                '.ytp-ce-element',
                                                '.ytp-ce-covering-overlay',
                                                '.ytp-ce-element-show',
                                                '.ytp-ce-video',
                                                '.ytp-ce-channel',
                                                '.ytp-gradient-top',
                                                '.ytp-gradient-bottom',
                                                '.ytp-show-cards-title',
                                                '.ytp-cards-teaser',
                                                '.ytp-cards-button',
                                                '.ytp-title',
                                                '.ytp-title-link',
                                                '.ytp-title-text',
                                                '.ytp-title-channel',
                                                '.ytp-title-channel-logo',
                                                '.ytp-title-channel-name',
                                                '.ytp-shorts-title-channel',
                                                '.ytp-shorts-title-channel-logo',
                                                '.ytp-cued-thumbnail-overlay',
                                                '.ytp-cued-thumbnail-overlay-image',
                                                '.ytp-large-play-button',
                                                '.html5-endscreen',
                                                '.autonav-endscreen',
                                                '.ytp-more-videos-view',
                                                '.ytp-more-videos-view-container',
                                                '.ytp-watch-again-on-youtube-endscreen-more-videos-container',
                                                '.ytp-watch-on-youtube-button',
                                                '.ytp-youtube-button',
                                                '.ytp-contextmenu',
                                                '.ytp-popup',
                                                '.ytp-bezel',
                                                '.ytp-spinner',
                                                'a[href*="youtube.com"]',
                                                'a[href*="youtu.be"]'
                                            ];

                                            function injectCss() {
                                                try {
                                                    var s = document.getElementById('madar-clean-style');
                                                    if (!s) {
                                                        s = document.createElement('style');
                                                        s.id = 'madar-clean-style';
                                                        s.type = 'text/css';
                                                        var target = document.head || document.documentElement;
                                                        if (target) target.appendChild(s);
                                                    }
                                                    if (s && s.parentNode) {
                                                        s.parentNode.appendChild(s);
                                                    }
                                                    var selList = selectors.join(', ');
                                                    var css = selList + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; width: 0 !important; height: 0 !important; max-width: 0 !important; max-height: 0 !important; position: absolute !important; left: -9999px !important; top: -9999px !important; z-index: -9999 !important; } ' +
                                                              'html body #player-controls, html body #player-control-overlay, html body creator-endscreen, html body expanding-endscreen-element, html body ytm-custom-control, html body ytm-watch-player-controls, html body ytm-endscreen-video-renderer, html body ytm-video-with-context-renderer, html body .ytFullscreenVideoRecommendationsRecommendation, html body #bottom-sheet-wrapper, html body video-cover, html body cued-overlay, html body .player-controls-content, html body .player-controls-background, html body div.html5-video-player .ytp-chrome-top, html body div.html5-video-player .ytp-chrome-bottom, html body div.html5-video-player .ytp-watermark, html body div.html5-video-player a.ytp-watermark, html body div.html5-video-player .ytp-pause-overlay, html body div.html5-video-player .ytp-title, html body div.html5-video-player .ytp-title-channel, html body div.html5-video-player a[href*="youtube.com"] { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; width: 0 !important; height: 0 !important; position: absolute !important; left: -9999px !important; top: -9999px !important; z-index: -9999 !important; } ' +
                                                              'html, body, #player, #movie_player, .html5-video-player { margin: 0 !important; padding: 0 !important; background: #000000 !important; overflow: hidden !important; width: 100% !important; height: 100% !important; } ' +
                                                              '.html5-video-container { width: 100% !important; height: 100% !important; display: flex !important; align-items: center !important; justify-content: center !important; background: #000000 !important; } ' +
                                                              'video.video-stream.html5-main-video { width: 100% !important; height: 100% !important; max-width: 100% !important; max-height: 100% !important; object-fit: contain !important; top: 0 !important; left: 0 !important; }';
                                                    if (s.textContent !== css) {
                                                        s.textContent = css;
                                                    }
                                                } catch(e) {}
                                            }

                                            function removeJunk() {
                                                try {
                                                    var junk = document.querySelectorAll('#player-controls, #player-control-overlay, .player-controls-content, .player-controls-background, creator-endscreen, expanding-endscreen-element, ytm-custom-control, ytm-watch-player-controls, ytm-endscreen-video-renderer, ytm-video-with-context-renderer, video-cover, cued-overlay, .ytFullscreenVideoRecommendationsRecommendation, #bottom-sheet-wrapper, .ytp-chrome-top, .ytp-chrome-bottom, .ytp-watermark, .ytp-pause-overlay, .ytp-pause-overlay-container, .ytp-gradient-top, .ytp-gradient-bottom');
                                                    for (var i = 0; i < junk.length; i++) {
                                                        var el = junk[i];
                                                        if (el && el.parentNode && el.tagName !== 'VIDEO') {
                                                            el.style.setProperty('display', 'none', 'important');
                                                            el.style.setProperty('visibility', 'hidden', 'important');
                                                            el.style.setProperty('opacity', '0', 'important');
                                                            el.style.setProperty('pointer-events', 'none', 'important');
                                                            el.parentNode.removeChild(el);
                                                        }
                                                    }
                                                } catch(e) {}
                                            }

                                            window.__madar_clean = function() {
                                                injectCss();
                                                removeJunk();
                                            };

                                            injectCss();
                                            removeJunk();

                                            function attachVideo() {
                                                var v = document.querySelector('video');
                                                if (!v) { setTimeout(attachVideo, 250); return; }
                                                if (v.__madar_attached) return;
                                                v.__madar_attached = true;

                                                var notify = function() {
                                                    if (window.MadarBridge) {
                                                        window.MadarBridge.onTimeUpdate(v.currentTime || 0, v.duration || 0);
                                                    }
                                                    removeJunk();
                                                };
                                                v.addEventListener('timeupdate', notify);
                                                v.addEventListener('durationchange', notify);
                                                v.addEventListener('play', function() {
                                                    if (window.MadarBridge) window.MadarBridge.onStateChange('playing');
                                                    removeJunk();
                                                });
                                                v.addEventListener('playing', function() {
                                                    if (window.MadarBridge) window.MadarBridge.onStateChange('playing');
                                                    removeJunk();
                                                });
                                                v.addEventListener('pause', function() {
                                                    if (window.MadarBridge) window.MadarBridge.onStateChange('paused');
                                                    removeJunk();
                                                });
                                                v.addEventListener('waiting', function() {
                                                    if (window.MadarBridge) window.MadarBridge.onStateChange('buffering');
                                                    removeJunk();
                                                });
                                                v.addEventListener('ended', function() {
                                                    if (window.MadarBridge) window.MadarBridge.onStateChange('ended');
                                                    removeJunk();
                                                });

                                                notify();
                                                if (v.paused) {
                                                    try { v.play(); } catch(e) {}
                                                }
                                            }
                                            attachVideo();
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(cleanScript, null)
                                    return
                                }
                                val pageView = view ?: return
                                val pageUrl = url ?: activePageUrl.get()
                                val probe = Runnable {
                                    if (!webViewActive.get() || pageFailureScheduled.get() || mediaHandoffScheduled.get()) return@Runnable
                                    val script = """(function(){
                                       var videos=Array.prototype.slice.call(document.querySelectorAll('video'));
                                       try{Array.prototype.forEach.call(document.querySelectorAll('iframe'),function(fr){try{var fd=fr.contentDocument||(fr.contentWindow&&fr.contentWindow.document);if(fd){var fvids=Array.prototype.slice.call(fd.querySelectorAll('video'));videos=videos.concat(fvids);}}catch(e){}});}catch(e){}
                                       var v=videos.find(function(x){return !!(x.currentSrc||x.src);})||videos[0];
                                       var body=(document.body&&document.body.innerText||'').slice(0,5000).toLowerCase();
                                       var challenge=/captcha|human verification|verify.{0,40}(human|you are)|checking your browser|security check/.test(body);
                                       var frames=Array.prototype.slice.call(document.querySelectorAll('iframe')).map(function(x){return x.src||x.getAttribute('data-src')||'';});
                                       var challengeFrame=frames.some(function(src){return /captcha|challenge|watchadsontape\.com|lumbarssweepup\.com/i.test(src);});
                                       var adLanding=videos.length===0&&frames.length===0&&/welcome bonus|free spins|بونص ترحيبي|مكافأة الترحيب|روابط الموقع الجديدة/.test(body);
                                       var docTitle=(document.title||'').toLowerCase();
                                       var is404Text=/404\s*(not\s*found)?|page\s*not\s*found|video\s*(?:not\s*found|unavailable|removed|deleted)|file\s*(?:not\s*found|deleted|removed)|no\s*such\s*file|couldn'?t\s*find|video\s*has\s*been\s*removed|تم\s*حذف|الملف\s*غير\s*موجود|تعذر\s*العثور/i.test(body) || /404|not found|deleted|removed/i.test(docTitle);
                                       var unavailable=videos.length===0 && is404Text;
                                       var hasJsPlayer=!!(window.jwplayer||window.videojs||window.Plyr||document.querySelector('.jw-video,.video-js,.plyr,.vjs-tech,div[id*=player],div[class*=player]'));
                                       if(!hasJsPlayer){try{Array.prototype.forEach.call(document.querySelectorAll('iframe'),function(fr){try{var fw=fr.contentWindow;if(fw&&(fw.jwplayer||fw.videojs||fw.Plyr))hasJsPlayer=true;}catch(e){}});}catch(e){}}
                                       try{var ads=document.querySelectorAll('.div-over, .overlay, #modal, .modalDialog, #status, [id^="ad-"], [class^="ad-"], [id*="-ad-"], [class*="-ad-"], [id*="banner"], [class*="banner"], iframe[src*="ads"]');ads.forEach(function(el){try{el.remove();}catch(e){}});}catch(e){}
                                       try{var playBtns=document.querySelectorAll('.play-btn, .jw-display-icon-container, .vjs-big-play-button, button[aria-label="Play"], #play, .play-button, .playbutton, .play_btn, .video-click, #overlay-play, .jw-preview, .jw-controls');playBtns.forEach(function(btn){try{btn.click();}catch(e){}});}catch(e){}
                                       videos.forEach(function(vid){if(vid){if(vid.paused){try{vid.play();}catch(e){try{vid.muted=true;vid.play();}catch(e2){}}}}});
                                       var jwUrl='';
                                       try{var jw=window.jwplayer&&window.jwplayer();if(jw&&jw.getPlaylistItem){var pi=jw.getPlaylistItem();jwUrl=pi&&(pi.file||pi.sources&&pi.sources[0]&&pi.sources[0].file)||'';}}catch(e){}
                                       if(!jwUrl){try{Array.prototype.forEach.call(document.querySelectorAll('iframe'),function(fr){try{var fw=fr.contentWindow;if(fw&&fw.jwplayer){var jw2=fw.jwplayer();if(jw2&&jw2.getPlaylistItem){var pi2=jw2.getPlaylistItem();var f2=pi2&&(pi2.file||pi2.sources&&pi2.sources[0]&&pi2.sources[0].file)||'';if(f2)jwUrl=f2;}}}catch(e){}});}catch(e){}}
                                       var interceptedUrl=window.__madar_intercepted_url||'';
                                       var mediaUrl=v?(v.currentSrc||v.src||jwUrl||interceptedUrl):(jwUrl||interceptedUrl);
                                       return JSON.stringify({url:mediaUrl,ready:v?v.readyState:0,playing:!!(v&&!v.paused&&!v.ended&&v.currentTime>0),count:videos.length,frameCount:frames.length,challenge:challenge,challengeFrame:challengeFrame,adLanding:adLanding,unavailable:unavailable,hasJsPlayer:hasJsPlayer});
                                     })();"""
                                    pageView.evaluateJavascript(script) { result ->
                                        if (!webViewActive.get() || pageFailureScheduled.get() || mediaHandoffScheduled.get() ||
                                            activePageUrl.get() != pageUrl
                                        ) return@evaluateJavascript
                                        val payload = runCatching {
                                            val encoded = JSONTokener(result ?: "null").nextValue() as? String
                                            encoded?.let(::JSONObject)
                                        }.getOrNull() ?: return@evaluateJavascript
                                        val mediaUrl = payload.optString("url").takeIf(String::isNotBlank).orEmpty()
                                        val hasJsPlayer = payload.optBoolean("hasJsPlayer", false)
                                        if (payload.optBoolean("challenge", false) || payload.optBoolean("challengeFrame", false)) {
                                            handleMainFrameFailure(
                                                "مصدر المشاهدة يطلب تحققًا يدويًا. جارٍ تجربة سيرفر آخر."
                                            )
                                        } else if (payload.optBoolean("adLanding", false)) {
                                            handleMainFrameFailure(
                                                "تم إيقاف صفحة ترويجية بدل الفيديو، جارٍ تجربة سيرفر آخر."
                                            )
                                        } else if (payload.optBoolean("unavailable", false)) {
                                            handleMainFrameFailure(
                                                "هذا الخادم لا يملك فيديو متاحًا (404). جارٍ تجربة سيرفر آخر."
                                            )
                                        } else if (payload.optInt("count", 0) == 0 &&
                                            payload.optInt("frameCount", 0) == 0 &&
                                            !hasJsPlayer && // لو فيه JS player نستنى أكتر
                                            System.currentTimeMillis() - pageStartedAt.get() >= 25_000L
                                        ) {
                                            handleMainFrameFailure(
                                                "صفحة المصدر لا تحتوي مشغل فيديو. جارٍ تجربة سيرفر آخر."
                                            )
                                        } else if (payload.optBoolean("playing", false) && payload.optInt("ready", 0) >= 2) {
                                            pagePlayableObserved.set(true)
                                            isBuffering = false
                                            playbackError = null
                                        }
                                        if (!pageFailureScheduled.get() &&
                                            (mediaUrl.startsWith("https://", true) || mediaUrl.startsWith("http://", true))
                                        ) {
                                            handoffMedia(mediaUrl)
                                        }
                                    }
                                }
                                // Discover standard HTML5 media after embed scripts initialize.
                                // Do not synthesize taps or bypass provider verification or DRM.
                                // مواقع البث العربية محتاجة وقت أطول — بنفحص على 3 / 7 / 12 / 18 ثانية
                                pageProbeHandler.postDelayed(probe, 1_200L)
                                pageProbeHandler.postDelayed(probe, 3_000L)
                                pageProbeHandler.postDelayed(probe, 7_000L)
                                pageProbeHandler.postDelayed(probe, 12_000L)
                                pageProbeHandler.postDelayed(probe, 18_000L)
                                pageProbeHandler.postDelayed(probe, 25_000L)
                                pageProbeHandler.postDelayed(probe, 35_000L)
                            }
                        }
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        if (isYouTube) {
                            val ytId = extractYouTubeId(effectiveStreamUrl)
                            if (ytId.isNotBlank()) {
                                // YouTube requires an HTTP Referer in Android WebViews.
                                // Loading the embed as a nested iframe can lose the app
                                // identity and produces player error 153. The package ID
                                // is the stable app identity registered with Android.
                                val youtubeUrl = Uri.Builder()
                                    .scheme("https")
                                    .authority("www.youtube.com")
                                    .appendPath("embed")
                                    .appendPath(ytId)
                                    .appendQueryParameter("autoplay", "1")
                                    .appendQueryParameter("controls", "0")
                                    .appendQueryParameter("rel", "0")
                                    .appendQueryParameter("modestbranding", "1")
                                    .appendQueryParameter("iv_load_policy", "3")
                                    .appendQueryParameter("fs", "0")
                                    .appendQueryParameter("disablekb", "1")
                                    .appendQueryParameter("playsinline", "1")
                                    .appendQueryParameter("enablejsapi", "1")
                                    .appendQueryParameter("cc_load_policy", "0")
                                    .appendQueryParameter("origin", YOUTUBE_EMBED_ORIGIN)
                                    .build()
                                    .toString()
                                loadUrl(
                                    youtubeUrl,
                                    mapOf("Referer" to "$YOUTUBE_EMBED_ORIGIN/"),
                                )
                            } else {
                                playbackError = "رابط فيديو YouTube غير صالح."
                                isBuffering = false
                            }
                        } else if (cleanRef.isNotBlank()) {
                            loadUrl(effectiveStreamUrl, mapOf("Referer" to cleanRef))
                        } else {
                            loadUrl(effectiveStreamUrl)
                        }
                    }
                    },
                    onRelease = { webView ->
                        if (webViewRef == webView) webViewRef = null
                        if (webViewActive.getAndSet(false)) {
                            pageProbeHandler.removeCallbacksAndMessages(null)
                            webView.stopLoading()
                            webView.destroy()
                        }
                    },
                )
            }
        } else if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        keepScreenOn = true
                        useController = true
                        setControllerHideOnTouch(true)
                        setControllerShowTimeoutMs(5_000)
                        setControllerVisibilityListener(
                            PlayerView.ControllerVisibilityListener { visibility ->
                                val controlsVisible = visibility == View.VISIBLE
                                if (showControls != controlsVisible) {
                                    showControls = controlsVisible
                                    if (controlsVisible) {
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                }
                            },
                        )
                        this.player = player
                    }
                },
                update = { it.player = player },
            )
        }

        // ── YouTube Transparent Gesture Overlay ─────────────────────────────
        if (isYouTube) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                                if (showControls) lastInteractionTime = System.currentTimeMillis()
                            },
                            onDoubleTap = { offset ->
                                val isRight = offset.x > (size.width / 2)
                                val delta = if (isRight) 10f else -10f
                                ytSeekBy(delta)
                                ytDoubleTapSide = if (isRight) "right" else "left"
                                ytDoubleTapKey++
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        )
                    }
            )
        }

        // Buffering Indicator
        if ((isBuffering && !useWebViewFallback) || (isYouTube && (isBuffering || ytIsBuffering))) {
            val pulseTransition = rememberInfiniteTransition(label = "player-buffer-pulse")
            val pulse by pulseTransition.animateFloat(
                initialValue = .86f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "player-buffer-scale",
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(96.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Color(0x22D4AF37))
                        .border(1.dp, Color(0x55D4AF37), CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFFD4AF37),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(43.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isYouTube) "جاري تحميل الملخص..." else "جاري تجهيز المشاهدة",
                        color = Color.White.copy(alpha = .88f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ── YouTube Copyright / Embed Restriction Banner ───────────────────
        if (isYouTube && showYouTubeFallbackPrompt && !ytIsPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF0181A20))
                    .border(1.dp, Color(0xFF580F8B), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠️ حقوق البث الرسمية لـ beIN SPORTS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("بعض الملخصات تمنع تشغيلها كفيديو مدمج داخل التطبيقات وتتطلب المشاهدة الرسمية", color = Color(0xFFB0B3C1), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Button(
                        onClick = {
                            val ytId = extractYouTubeId(effectiveStreamUrl)
                            val watchUrl = "https://www.youtube.com/watch?v=$ytId"
                            try {
                                val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$ytId")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(ytIntent)
                            } catch (_: Exception) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(browserIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("▶ فتح فوراً في YouTube بجودة فائقة (1080p)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Black Screen Recovery Dialog ────────────────────────────────────
        playbackError?.let { errorCode ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF0181A20))
                    .border(1.dp, Color(0xFFC0392B), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("⚠️ تعذر تشغيل هذا المصدر (شاشة سوداء)", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(errorCode, color = Color(0xFFB0B3C1), fontSize = 13.sp, textAlign = TextAlign.Center)
                    sourceDiagnostic?.let { diagnostic ->
                        Text(diagnostic, color = Color.LightGray, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                playbackError = null
                                sourceDiagnostic = null
                                isBuffering = true
                                if (backupSources.isNotEmpty()) {
                                    val next = backupSources.removeAt(0)
                                    onSwitchStream(
                                        next.url,
                                        next.userAgent.ifBlank { cleanUa },
                                        next.referer.ifBlank { cleanRef },
                                        next.headers,
                                    )
                                } else {
                                    exoRetryKey++
                                    webViewRef?.reload()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (backupSources.isNotEmpty()) "⚡ سيرفر بديل (${backupSources.size})" else "🔄 إعادة المحاولة", color = Color.White)
                        }

                        Button(
                            onClick = {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveStreamUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🌐 بالمتصفح", color = Color.White)
                        }

                        Button(
                            onClick = { onClose() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34495E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("◀ عودة", color = Color.White)
                        }
                    }
                }
            }
        }

        // Keep custom controls in the same tap-to-show/hide cycle as the
        // Media3 controller. Tapping the video only changes overlays; it never
        // pauses or recreates the active player.
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xD9272C3D))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {

                // ── Smart Bandwidth / Quality button ────────────────────────
                val bandwidthLabel = when {
                    isYouTube -> "🎬 ملخص · HD"
                    useWebViewFallback -> "جودة · المصدر"
                    selectedQuality == "أقصى توفير" || selectedQuality == "توفير البيانات" -> "⚡ أقصى توفير"
                    selectedQuality == "جودة متوازنة" || selectedQuality == "متوسط" -> "⚖️ متوازن (متوسط)"
                    selectedQuality == "أعلى جودة" -> "✦ أعلى جودة"
                    selectedQuality == "ضغط ذكي (HD موفّر)" || selectedQuality == "ضغط ذكي" -> "🧠 HD اقتصادي"
                    selectedQuality == "تلقائي" -> "⚙ تلقائي"
                    selectedQuality == "جودة المصدر" -> "🎞 جودة المصدر"
                    else -> "🎞 $selectedQuality"
                }
                Surface(
                    onClick = {
                        showBandwidthSheet = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    color = Color(0xD9272C3D),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(bandwidthLabel, color = Color.White, fontSize = 13.sp)
                        if (currentBitrateKbps > 0 && !useWebViewFallback) {
                            Spacer(Modifier.width(6.dp))
                            val bwText = if (currentBitrateKbps >= 1000) {
                                "${"%.1f".format(currentBitrateKbps / 1000f)} Mbps"
                            } else "$currentBitrateKbps kbps"
                            Text(
                                "· $bwText",
                                color = Color(0xFFD4AF37),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── Quick Switch to Next Backup Server button ─────────────────
                if (backupSources.isNotEmpty()) {
                    Surface(
                        onClick = {
                            val next = backupSources.removeAt(0)
                            onSwitchStream(
                                next.url,
                                next.userAgent.ifBlank { cleanUa },
                                next.referer.ifBlank { cleanRef },
                                next.headers,
                            )
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        color = Color(0xD9C0392B),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text("⚡", fontSize = 13.sp)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "تبديل السيرفر (${backupSources.size})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── Universal Smart TV Connect Button ───────────────────────
                Surface(
                    onClick = {
                        showTvConnectDialog = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    color = Color(0xD9272C3D),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                    ) {
                        Text("📺", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "توصيل بالشاشة",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }

        // ── Smart Bandwidth Control Sheet ────────────────────────────────────
        if (showBandwidthSheet) {
            BandwidthControlSheet(
                selectedQuality = selectedQuality,
                availableTracks = availableVideoTracks,
                qualityLabels = qualityLabels,
                isWebView = useWebViewFallback,
                isYouTube = isYouTube,
                streamUrl = effectiveStreamUrl,
                currentBitrateKbps = currentBitrateKbps,
                currentTrackInfo = currentTrackInfo,
                onSelect = { quality ->
                    applyQuality(trackSelector, quality, availableVideoTracks, qualityLabels)
                    selectedQuality = quality
                    preferences.edit().putString("quality", quality).apply()
                    showBandwidthSheet = false
                    lastInteractionTime = System.currentTimeMillis()
                },
                onDismiss = { showBandwidthSheet = false }
            )
        }

        // ── Universal Smart TV Connect Dialog ────────────────────────────────
        if (showTvConnectDialog) {
            TvConnectDialog(
                title = title,
                streamUrl = effectiveStreamUrl,
                isLive = isLive,
                canCast = isDirectStream,
                player = player,
                needsSourceHeaders = referer.isNotBlank() || streamHeaders.isNotEmpty(),
                onDismiss = { showTvConnectDialog = false }
            )
        }

        // ── YouTube Native Center Controls ──────────────────────────────────
        if (isYouTube) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(animationSpec = tween(180)) + scaleIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(180)) + scaleOut(animationSpec = tween(180)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC1E1420))
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable {
                                ytSeekBy(-10f)
                                lastInteractionTime = System.currentTimeMillis()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("↺", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("10s", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Play / Pause
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE1E1420))
                            .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
                            .clickable {
                                if (ytIsPlaying) {
                                    webViewRef?.evaluateJavascript("var v=document.querySelector('video'); if(v) v.pause();", null)
                                } else {
                                    if (ytDurationSec > 0f && ytCurrentSec >= ytDurationSec - 1f) {
                                        webViewRef?.evaluateJavascript("var v=document.querySelector('video'); if(v) { v.currentTime=0; v.play(); }", null)
                                    } else {
                                        webViewRef?.evaluateJavascript("var v=document.querySelector('video'); if(v) v.play();", null)
                                    }
                                }
                                lastInteractionTime = System.currentTimeMillis()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (ytIsPlaying) {
                            Text("❚❚", color = Color(0xFFD4AF37), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("▶", color = Color(0xFFD4AF37), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Forward 10s
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC1E1420))
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable {
                                ytSeekBy(10f)
                                lastInteractionTime = System.currentTimeMillis()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("↻", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("10s", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── YouTube Native Bottom Controls (Seekbar + Speed + Rotate) ──────
        if (isYouTube) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(animationSpec = tween(180)) + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xF5000000))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displaySec = ytSliderValue ?: ytCurrentSec
                            Text(
                                text = formatTime(displaySec.toLong()),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = displaySec.coerceIn(0f, maxOf(ytDurationSec, 1f)),
                                onValueChange = { newSec ->
                                    ytSliderValue = newSec
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onValueChangeFinished = {
                                    ytSliderValue?.let { target ->
                                        webViewRef?.evaluateJavascript("var v=document.querySelector('video'); if(v) v.currentTime = $target;", null)
                                        ytCurrentSec = target
                                    }
                                    ytSliderValue = null
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                valueRange = 0f..maxOf(ytDurationSec, 1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD4AF37),
                                    activeTrackColor = Color(0xFFD4AF37),
                                    inactiveTrackColor = Color(0x44FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            Text(
                                text = formatTime(ytDurationSec.toLong()),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Text(
                                text = "زقزوقة TV",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = {
                                    val nextSpeed = when (ytSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                    ytSpeed = nextSpeed
                                    webViewRef?.evaluateJavascript("var v=document.querySelector('video'); if(v) v.playbackRate = $nextSpeed;", null)
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                color = Color(0xCC1E1420),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, Color(0x33D4AF37))
                            ) {
                                Text(
                                    text = "${ytSpeed}x",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    val act = context as? ComponentActivity
                                    if (act != null) {
                                        val current = act.requestedOrientation
                                        if (current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                                            current == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        }
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                color = Color(0xCC1E1420),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, Color(0x33D4AF37))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("⛶", fontSize = 11.sp, color = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "تدوير",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Double-Tap Animated Indicator ──────────────────────────────────
        if (isYouTube) {
            AnimatedVisibility(
                visible = showDoubleTapIndicator,
                enter = fadeIn(animationSpec = tween(140)) + scaleIn(animationSpec = tween(140)),
                exit = fadeOut(animationSpec = tween(250)) + scaleOut(animationSpec = tween(250)),
                modifier = Modifier
                    .align(if (ytDoubleTapSide == "left") Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD1E1420))
                        .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (ytDoubleTapSide == "left") "↺" else "↻",
                            color = Color(0xFFD4AF37),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (ytDoubleTapSide == "left") "-10 ثواني" else "+10 ثواني",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Zakzouka TV Watermark (Bottom-Start corner) ──
        // Unobtrusive, semi-transparent, branded, never covers score or channel logo
        AnimatedVisibility(
            visible = showControls && !isYouTube,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 18.dp, bottom = 18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(0.5.dp, Color(0xFFD4AF37).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Text(
                        text = "زقزوقة TV",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Smart Bandwidth Control Sheet — with animated slider and live cost display
// ═══════════════════════════════════════════════════════════════════════════

// App brand colours matching the Flutter/Compose theme
private val PlayerBrick    = Color(0xFFB94E3C)
private val PlayerInk      = Color(0xFF342633)
private val PlayerPaper    = Color(0xFFF2E6D7)
private val PlayerCard     = Color(0xFFFFFBF5)
private val PlayerMuted    = Color(0xFF806D78)
private val PlayerGold     = Color(0xFFD4AF37)
private val PlayerSheet    = Color(0xFF1E1420)
private val PlayerSheetSurf= Color(0xFF2A1D27)

/** Returns a human-readable estimate of data consumed per hour at a given kbps rate */
private fun dataPerHourLabel(kbps: Int): String {
    val mb = kbps * 3600L / 8 / 1024
    return if (mb >= 1024) "${"%.1f".format(mb / 1024f)} GB/ساعة" else "$mb MB/ساعة"
}

/** Returns approx cost per hour label (Egypt-reference: ~1 GB ≈ 5 LE) */
private fun costPerHourLabel(kbps: Int): String {
    val mb = kbps * 3600L / 8 / 1024
    val le = mb / 1024f * 5f
    return if (le < 0.1f) "< 10 قروش/ساعة" else "${"%.1f".format(le)} جنيه/ساعة"
}

/** Returns a colour from green (cheap) → amber → red (expensive) for a 0..1 fraction */
private fun bandwidthColor(fraction: Float): Color {
    return when {
        fraction < 0.35f -> Color(0xFF22C55E)
        fraction < 0.65f -> Color(0xFFF59E0B)
        else             -> PlayerBrick
    }
}

data class QualityOption(
    val label: String,
    val displayName: String,
    val estimatedKbps: Int,  // 0 = unknown / auto
    val isSpecial: Boolean = false,
)

@Composable
private fun BandwidthControlSheet(
    selectedQuality: String,
    availableTracks: List<VideoQualityTrack>,
    qualityLabels: List<String>,
    isWebView: Boolean,
    isYouTube: Boolean = false,
    streamUrl: String = "",
    currentBitrateKbps: Int,
    currentTrackInfo: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetContext = LocalContext.current
    // Bandwidth presets provide clear, distinct data consumption estimates
    // and match the source renditions when multiple tracks exist.
    val options: List<QualityOption> = buildList {
        val sourceTracks = availableTracks.distinctBy { it.height to it.bitrate }
        val hasMultiTracks = sourceTracks.size > 1

        val track360 = sourceTracks.filter { it.height in 280..420 }
            .minByOrNull { it.bitrate.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE }
            ?: sourceTracks.filter { it.height <= 420 }.minByOrNull { it.bitrate }

        val trackMid = sourceTracks.filter { it.height in 480..720 }
            .minByOrNull { it.bitrate.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE }

        val trackHd = sourceTracks.filter { it.height in 720..1079 }
            .minByOrNull { it.bitrate.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE }

        val trackFhd = sourceTracks.filter { it.height >= 1080 }
            .maxByOrNull { it.bitrate.takeIf { rate -> rate > 0 } ?: 0 }

        // 1. تلقائي (Adaptive)
        add(QualityOption(
            label = "تلقائي",
            displayName = "⚙ تلقائي · تعديل ديناميكي حسب سرعة النت",
            estimatedKbps = currentBitrateKbps.takeIf { it > 0 } ?: 1800,
            isSpecial = true,
        ))

        // 2. أقصى توفير للنت (Data Saver)
        add(QualityOption(
            label = "أقصى توفير",
            displayName = "⚡ أقصى توفير (360p / باقات محدودة)",
            estimatedKbps = track360?.bitrate?.takeIf { it > 0 }?.div(1000) ?: 400,
            isSpecial = true,
        ))

        // 3. جودة متوازنة (Medium / متوسط)
        add(QualityOption(
            label = "جودة متوازنة",
            displayName = "⚖️ جودة متوسطة متوازنة (480p / 720p)",
            estimatedKbps = trackMid?.bitrate?.takeIf { it > 0 }?.div(1000) ?: 900,
            isSpecial = true,
        ))

        // 4. ضغط ذكي (Smart HD)
        add(QualityOption(
            label = "ضغط ذكي (HD موفّر)",
            displayName = "🧠 ضغط ذكي · جودة HD موفّرة للنت",
            estimatedKbps = trackHd?.bitrate?.takeIf { it > 0 }?.div(1000) ?: 1800,
            isSpecial = true,
        ))

        // 5. أعلى جودة (Highest Quality)
        add(QualityOption(
            label = "أعلى جودة",
            displayName = "✦ أعلى جودة متاحة (1080p / FHD)",
            estimatedKbps = trackFhd?.bitrate?.takeIf { it > 0 }?.div(1000)
                ?: if (sourceTracks.isNotEmpty() && sourceTracks.maxOf { it.height } >= 1080)
                    sourceTracks.maxOf { it.bitrate } / 1000
                else 4500,
            isSpecial = true,
        ))

        // 6. مسارات الجودة المكتشفة من البث (فقط إذا توافر أكثر من مسار، وللدقة >= 360p فقط)
        if (hasMultiTracks) {
            val highTracks = availableTracks.filter { it.height >= 360 }
            highTracks.forEachIndexed { _, track ->
                val lbl = qualityLabels.getOrNull(availableTracks.indexOf(track)) ?: "${track.height}p"
                if (none { it.label == lbl }) {
                    add(QualityOption(
                        label = lbl,
                        displayName = "🎞 $lbl",
                        estimatedKbps = if (track.bitrate > 0) track.bitrate / 1000 else
                            when {
                                track.height <= 360  -> 400
                                track.height <= 480  -> 800
                                track.height <= 720  -> 1800
                                track.height <= 1080 -> 4500
                                else                 -> 8000
                            }
                    ))
                }
            }
        }

        // 7. جودة المصدر
        val sourceRate = currentBitrateKbps.takeIf { it > 0 }
            ?: sourceTracks.firstOrNull()?.bitrate?.takeIf { it > 0 }?.div(1000)
            ?: 1200
        val maxSourceH = sourceTracks.maxByOrNull { it.height }?.height
        val srcLabel = if (maxSourceH != null && maxSourceH > 0) "🎞 جودة المصدر (${maxSourceH}p)" else "🎞 جودة المصدر"
        add(QualityOption(
            label = "جودة المصدر",
            displayName = if (currentTrackInfo.isNotBlank()) "$srcLabel · $currentTrackInfo" else srcLabel,
            estimatedKbps = sourceRate,
            isSpecial = true,
        ))
    }

    val maxKbps = options.maxOf { it.estimatedKbps }.coerceAtLeast(1)
    val currentIndex = options.indexOfFirst { it.label == selectedQuality }.coerceAtLeast(0)
    var sliderIndex by remember(options, selectedQuality) { mutableIntStateOf(currentIndex) }
    val highlighted = options.getOrNull(sliderIndex) ?: options.first()
    val fraction = (highlighted.estimatedKbps.toFloat() / maxKbps).coerceIn(0.06f, 1f)
    val barColor = bandwidthColor(fraction)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 660.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(PlayerSheet)
                .border(1.dp, PlayerBrick.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3C1A2E), Color(0xFF1E1420))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(barColor)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "التحكم في الجودة وضغط النت",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (isWebView) "هذا المصدر صفحة ويب · تحكّم بالجودة من المصدر مباشرة"
                                    else currentTrackInfo,
                                    color = PlayerMuted,
                                    fontSize = 11.sp
                                )
                                if (currentBitrateKbps > 0 && !isWebView) {
                                    Spacer(Modifier.height(2.dp))
                                    val liveTxt = if (currentBitrateKbps >= 1000)
                                        "${"%.1f".format(currentBitrateKbps / 1000f)} Mbps"
                                    else "$currentBitrateKbps kbps"
                                    Text(
                                        "⬇ سرعة التدفق الحالية: $liveTxt",
                                        color = PlayerGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF382333))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isWebView) {
                    Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isYouTube) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎬 ملخص يوتيوب فائق الدقة (HD)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "اختيارات الجودة والإعلانات يحددها YouTube ومصدر المقطع. افتح المقطع في تطبيق YouTube الرسمي للتحكم بالجودة المتاحة.",
                                    color = PlayerMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PlayerBrick)
                                        .clickable {
                                            val ytId = extractYouTubeId(streamUrl)
                                            val url = if (ytId.isNotBlank()) "https://www.youtube.com/watch?v=$ytId" else streamUrl
                                            try {
                                                val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                sheetContext.startActivity(ytIntent)
                                            } catch (_: Exception) {}
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("فتح في تطبيق YouTube ↗", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                "هذا السيرفر يعرض صفحة ويب.\nاختر سيرفر فيديو مباشر للتحكم الكامل في الجودة وتوفير النت.",
                                color = PlayerMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {

                        val sourceTracks = availableTracks.distinctBy { it.height to it.bitrate }
                        if (sourceTracks.size <= 1) {
                            val h = sourceTracks.firstOrNull()?.height ?: 0
                            if (h in 1..400) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x2EF59E0B))
                                        .border(0.5.dp, Color(0x66F59E0B), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "ℹ️ ملاحظة: هذا السيرفر يبث بجودة وحيدة (${h}p) من المصدر. اختر سيرفرًا آخر من تفاصيل الفيلم/المسلسل للوصول لجودات أعلى (1080p).",
                                        color = Color(0xFFFFD580),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // ── Live cost summary card ───────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PlayerSheetSurf)
                                .padding(14.dp)
                        ) {
                            Column {
                                val selLabel = highlighted.displayName
                                Text(selLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                // Bandwidth bar
                                val animFraction by androidx.compose.animation.core.animateFloatAsState(
                                    fraction, label = "bw-bar"
                                )
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color(0xFF3A2535))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(animFraction.coerceIn(0.04f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(barColor)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                          Text("استهلاك تقديري", color = PlayerMuted, fontSize = 10.sp)
                                        val kbps = highlighted.estimatedKbps
                                        Text(
                                            dataPerHourLabel(kbps),
                                            color = barColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                          Text("تكلفة تقديرية", color = PlayerMuted, fontSize = 10.sp)
                                        Text(
                                            costPerHourLabel(highlighted.estimatedKbps),
                                            color = PlayerGold,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                          Text("معدل المسار", color = PlayerMuted, fontSize = 10.sp)
                                        val needed = highlighted.estimatedKbps
                                        val neededTxt = if (needed >= 1000) "${"%.1f".format(needed/1000f)} Mbps" else "$needed kbps"
                                        Text(neededTxt, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // ── Slider ───────────────────────────────────────────
                          Text("اختيار مسار الجودة", color = PlayerMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))

                        Slider(
                            value = sliderIndex.toFloat(),
                            onValueChange = { sliderIndex = it.toInt() },
                            valueRange = 0f..(options.size - 1).toFloat().coerceAtLeast(0f),
                            steps = (options.size - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = PlayerBrick,
                                activeTrackColor = PlayerBrick,
                                inactiveTrackColor = Color(0xFF3A2535),
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (options.size > 1) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("⚡ أقل مسار يقدمه المصدر", color = PlayerMuted, fontSize = 10.sp)
                                Text("✦ أعلى جودة متاحة", color = PlayerMuted, fontSize = 10.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Quality chip list ────────────────────────────────
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(options) { idx, opt ->
                                val isSelected = idx == sliderIndex
                                val chipColor = if (isSelected) PlayerBrick else PlayerSheetSurf
                                val textColor = if (isSelected) Color.White else PlayerMuted
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(chipColor)
                                        .border(
                                            if (isSelected) 0.dp else 1.dp,
                                            Color(0xFF3A2535),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { sliderIndex = idx }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (opt.label) {
                                            "ضغط ذكي (HD موفّر)" -> "🧠\nHD اقتصادي"
                                            "أقصى توفير", "توفير البيانات" -> "⚡\nأقصى توفير"
                                            "جودة متوازنة", "متوسط" -> "⚖️\nمتوسط"
                                            "تلقائي" -> "⚙\nتلقائي"
                                            "أعلى جودة" -> "✦\nأعلى جودة"
                                            "جودة المصدر" -> "🎞\nالمصدر"
                                            else -> opt.label.replace(" · ", "\n")
                                        },
                                        color = textColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Confirm button ───────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PlayerBrick)
                                .clickable { onSelect(highlighted.label) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "تطبيق الجودة (${highlighted.label})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Universal Smart TV Connect System — 1-click connection to ANY screen
// ═══════════════════════════════════════════════════════════════════════════

private fun openWirelessDisplay(context: android.content.Context) {
    val intents = listOf(
        Intent(Settings.ACTION_CAST_SETTINGS),
        Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
        Intent("android.settings.CAST_SETTINGS"),
        Intent().apply {
            setClassName("com.samsung.android.smartmirroring", "com.samsung.android.smartmirroring.CastingActivity")
        },
        Intent().apply {
            action = "com.samsung.intent.action.CAST_MIRRORING"
        },
        Intent().apply {
            setClassName("com.android.settings", "com.android.settings.wfd.WifiDisplaySettings")
        },
    )
    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(context, "جارٍ البحث عن شاشات التلفزيون القريبة...", Toast.LENGTH_SHORT).show()
            return
        } catch (_: Exception) {}
    }
    runCatching {
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Toast.makeText(context, "اختَر 'بث الشاشة' أو 'عرض لاسلكي' للتوصيل بالتلفزيون.", Toast.LENGTH_LONG).show()
    }
}

private fun openExternalCaster(context: android.content.Context, streamUrl: String, title: String, isLive: Boolean) {
    val mimeType = castContentType(streamUrl, isLive)
    val sendIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(streamUrl), mimeType)
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra("title", title)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(sendIntent, "اختر تطبيق التوصيل بالتلفزيون (مثل Web Video Caster)")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        context.startActivity(chooser)
    }.onFailure {
        Toast.makeText(context, "تعذر فتح قائمة تطبيقات البث.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun TvConnectDialog(
    title: String,
    streamUrl: String,
    isLive: Boolean,
    canCast: Boolean,
    player: ExoPlayer?,
    needsSourceHeaders: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var castUiUnavailable by remember(streamUrl) { mutableStateOf(false) }
    val castContext = remember(context) {
        runCatching { CastContext.getSharedInstance(context.applicationContext) }.getOrNull()
    }
    val sessionManager = castContext?.sessionManager

    val loadToReceiver: (CastSession) -> Unit = remember(streamUrl, title, isLive, canCast, player, needsSourceHeaders) {
        { session ->
            val remote = session.remoteMediaClient
            if (!canCast) {
                Toast.makeText(context, "هذا السيرفر صفحة موقع، اختر رابط فيديو مباشر لإرساله للتلفزيون.", Toast.LENGTH_LONG).show()
            } else if (remote == null) {
                Toast.makeText(context, "اتصل التلفزيون، لكن مشغل الوسائط غير جاهز.", Toast.LENGTH_LONG).show()
            } else {
                if (needsSourceHeaders) {
                    Toast.makeText(
                        context,
                        "قد لا يعمل هذا السيرفر على التلفزيون لأنه يحتاج بيانات دخول خاصة بالمصدر.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                val metadata = MediaMetadata(
                    if (isLive) MediaMetadata.MEDIA_TYPE_TV_SHOW else MediaMetadata.MEDIA_TYPE_MOVIE,
                ).apply {
                    putString(MediaMetadata.KEY_TITLE, title)
                }
                val mediaInfo = MediaInfo.Builder(streamUrl)
                    .setContentType(castContentType(streamUrl, isLive))
                    .setStreamType(if (isLive) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(metadata)
                    .build()
                val request = MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaInfo)
                    .setAutoplay(true)
                    .build()
                runCatching {
                    remote.load(request).setResultCallback { result ->
                        if (result.status.isSuccess) {
                            player?.pause()
                            Toast.makeText(context, "بدأ تشغيل البث على الشاشة بنجاح! 📺", Toast.LENGTH_LONG).show()
                        } else {
                            val message = if (needsSourceHeaders) {
                                "التلفزيون لم يستطع فتح الرابط الذي يحتاج بيانات المصدر؛ استخدم خيار البث اللاسلكي لعرضه."
                            } else {
                                "تعذر تشغيل هذا الرابط على الشاشة. استخدم خيار البث اللاسلكي المباشر."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }.onFailure {
                    Toast.makeText(context, "تعذر إرسال الرابط إلى الشاشة؛ يستمر تشغيل الهاتف.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val listener = remember(loadToReceiver) {
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarting(session: CastSession) = Unit
            override fun onSessionStarted(session: CastSession, sessionId: String) = loadToReceiver(session)
            override fun onSessionStartFailed(session: CastSession, error: Int) {
                Toast.makeText(context, "تعذر الاتصال بجهاز البث.", Toast.LENGTH_LONG).show()
            }
            override fun onSessionEnding(session: CastSession) = Unit
            override fun onSessionEnded(session: CastSession, error: Int) = Unit
            override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
            override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = Unit
            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                Toast.makeText(context, "تعذر استعادة الاتصال بجهاز البث.", Toast.LENGTH_LONG).show()
            }
        }
    }

    DisposableEffect(sessionManager, listener) {
        var listenerAdded = false
        runCatching {
            sessionManager?.addSessionManagerListener(listener, CastSession::class.java)
            listenerAdded = sessionManager != null
            sessionManager?.currentCastSession?.takeIf { it.isConnected }?.let(loadToReceiver)
        }.onFailure {
            castUiUnavailable = true
        }
        onDispose {
            if (listenerAdded) {
                runCatching { sessionManager?.removeSessionManagerListener(listener, CastSession::class.java) }
            }
        }
    }

    val mediaRouteButtonHolder = remember { AtomicReference<MediaRouteButton?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 660.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(PlayerSheet)
                .border(1.dp, PlayerBrick.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with title and close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3C1A2E), Color(0xFF1E1420))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📺", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "توصيل الشاشة والتلفزيون",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "شاهد الفيديو أو البث المباشر على أي شاشة تلفزيون بسهولة",
                                    color = PlayerMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF382333))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✕",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (title.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF160E18))
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "🎬 $title",
                            color = PlayerGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Wireless Display / Smart View (Recommended for ALL TVs, Hero card)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4A1F2D), Color(0xFF2A1D27))
                                )
                            )
                            .border(1.2.dp, PlayerBrick.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .clickable {
                                onDismiss()
                                openWirelessDisplay(context)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PlayerBrick),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "بث لاسلكي مباشر (Smart View)",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PlayerGold.copy(alpha = 0.22f))
                                            .border(0.5.dp, PlayerGold.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "أسرع طريقة ⚡",
                                            color = PlayerGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "يتصل فوراً بأي شاشة (سامسونج، LG، توشيبا، سوني، هايسنس، أندرويد TV...)",
                                    color = PlayerMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Options 2 & 3 in horizontal Row for landscape screens!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 2: Google Cast / Chromecast
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PlayerSheetSurf)
                                .border(1.dp, Color(0xFF3A2535), RoundedCornerShape(14.dp))
                                .clickable {
                                    onDismiss()
                                    val btn = mediaRouteButtonHolder.get()
                                    if (btn != null && castContext != null && !castUiUnavailable) {
                                        btn.performClick()
                                    } else {
                                        openWirelessDisplay(context)
                                    }
                                }
                                .padding(11.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF382937)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📡", fontSize = 17.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Google Cast",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "شاشات Android TV و Chromecast",
                                        color = PlayerMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Option 3: External Caster Apps (Web Video Caster / DLNA / TV Cast)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PlayerSheetSurf)
                                .border(1.dp, Color(0xFF3A2535), RoundedCornerShape(14.dp))
                                .clickable {
                                    onDismiss()
                                    openExternalCaster(context, streamUrl, title, isLive)
                                }
                                .padding(11.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF382937)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📲", fontSize = 17.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "تطبيقات البث (DLNA)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Web Video Caster وشاشات DLNA",
                                        color = PlayerMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // WiFi Hint Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2222C55E))
                            .border(0.5.dp, Color(0x4422C55E), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "تأكد أن الهاتف وشاشة التلفزيون متصلان بنفس شبكة الواي فاي (Wi-Fi).",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            // Hidden MediaRouteButton for background Google Cast support
            if (castContext != null && !castUiUnavailable) {
                Box(modifier = Modifier.size(1.dp).clip(CircleShape)) {
                    AndroidView(
                        modifier = Modifier.size(1.dp),
                        factory = { viewContext ->
                            val buttonResult = runCatching {
                                MediaRouteButton(viewContext).apply {
                                    setBackgroundColor(AndroidColor.TRANSPARENT)
                                    minimumWidth = 1
                                    minimumHeight = 1
                                }
                            }
                            val button = buttonResult.getOrElse {
                                castUiUnavailable = true
                                View(viewContext)
                            }
                            if (button is MediaRouteButton) {
                                mediaRouteButtonHolder.set(button)
                                runCatching {
                                    CastButtonFactory.setUpMediaRouteButton(viewContext, button)
                                }.onFailure {
                                    castUiUnavailable = true
                                }
                            }
                            button
                        }
                    )
                }
            }
        }
    }
}

private fun castContentType(url: String, isLive: Boolean): String {
    val path = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
    return when {
        path.endsWith(".m3u8") || path.contains("/hls/") || path.contains("/live/") || (isLive && !path.endsWith(".mp4") && !path.endsWith(".mpd")) -> "application/x-mpegURL"
        path.endsWith(".mpd") -> "application/dash+xml"
        path.endsWith(".mp4") || path.endsWith(".m4v") -> "video/mp4"
        path.endsWith(".webm") -> "video/webm"
        path.endsWith(".ts") -> "video/mp2t"
        else -> "video/*"
    }
}

private fun isHlsStream(url: String): Boolean {
    val clean = url.substringBefore('?').substringBefore('#')
    return clean.endsWith(".m3u8", ignoreCase = true) ||
           url.contains("index.m3u8", ignoreCase = true) ||
           url.contains("/live/", ignoreCase = true) ||
           url.contains("/hls/", ignoreCase = true) ||
           url.contains("master.m3u8", ignoreCase = true)
}

private fun Float.format(digits: Int): String = String.format(Locale.US, "%.${digits}f", this)

@OptIn(UnstableApi::class)
private fun applyQuality(
    trackSelector: DefaultTrackSelector,
    choice: String,
    tracks: List<VideoQualityTrack>,
    qualityLabels: List<String>,
) {
    val params = trackSelector.buildUponParameters()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .clearVideoSizeConstraints()
        .setForceLowestBitrate(false)
        .setForceHighestSupportedBitrate(false)

    when {
        // ── 1. Smart selection: choose an efficient HD source rendition. ──
        choice == "ضغط ذكي (HD موفّر)" || choice == "ضغط ذكي" -> {
            params.setMaxVideoSize(1280, 720)
            params.setMaxVideoBitrate(2_000_000)
            val hdTracks = tracks.filter { it.height in 480..720 }
            val selected = if (hdTracks.isNotEmpty()) {
                hdTracks.minByOrNull { it.bitrate.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE }
            } else {
                tracks.filter { it.height <= 720 }.maxWithOrNull(
                    compareBy<VideoQualityTrack> { it.height }
                        .thenByDescending { it.bitrate.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE },
                )
            }
            if (selected != null) {
                params.addOverride(TrackSelectionOverride(selected.group, selected.index))
            }
        }

        // ── 2. أقصى توفير للنت (Ultra Data Saver): للشبكات الضعيفة والباقات المحدودة ──
        choice == "توفير البيانات" || choice == "أقصى توفير" -> {
            params.setForceLowestBitrate(true)
            params.setMaxVideoSize(640, 360)
            params.setMaxVideoBitrate(600_000)
            val selected = tracks.minWithOrNull(
                compareBy<VideoQualityTrack> { it.bitrate.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE }
                    .thenBy { it.height },
            )
            if (selected != null) {
                params.addOverride(TrackSelectionOverride(selected.group, selected.index))
            }
        }

        // ── 3. جودة متوازنة (Medium Saver): توازن بين الوضوح وسرعة البث ──
        choice == "جودة متوازنة" || choice == "متوسط" -> {
            params.setMaxVideoSize(1280, 720)
            params.setMaxVideoBitrate(1_500_000)
            val midTracks = tracks.filter { it.height in 480..720 }
            val selected = midTracks.minByOrNull {
                it.bitrate.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE
            } ?: tracks.minByOrNull { it.height }
            if (selected != null) {
                params.addOverride(TrackSelectionOverride(selected.group, selected.index))
            }
        }

        // ── 4. أعلى جودة بدون أي قيود ──────────────────────────────────────────
        choice == "أعلى جودة" -> {
            params.clearVideoSizeConstraints()
            params.setMaxVideoBitrate(Int.MAX_VALUE)
            params.setForceHighestSupportedBitrate(true)
            val selected = tracks.maxWithOrNull(compareBy<VideoQualityTrack> { it.height }.thenBy { it.bitrate })
            if (selected != null) {
                params.addOverride(TrackSelectionOverride(selected.group, selected.index))
            }
        }

        choice == "جودة المصدر" -> {
            params.clearVideoSizeConstraints()
            params.setMaxVideoBitrate(Int.MAX_VALUE)
            val selected = tracks.maxWithOrNull(compareBy<VideoQualityTrack> { it.height }.thenBy { it.bitrate })
            if (selected != null) params.addOverride(TrackSelectionOverride(selected.group, selected.index))
        }

        // ── 5. تلقائي (Adaptive Bitrate) ──────────────────────────────────────
        choice == "تلقائي" -> {
            params.clearVideoSizeConstraints()
            params.setMaxVideoBitrate(Int.MAX_VALUE)
        }

        // ── 6. جودة محددة (مثل 1080p, 720p, 480p...) ──────────────────────────
        else -> {
            val wantedHeight = Regex("(\\d{3,4})p").find(choice)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (wantedHeight != null) {
                params.setMaxVideoSize((wantedHeight * 16) / 9, wantedHeight)
            } else {
                params.setMaxVideoBitrate(Int.MAX_VALUE)
            }
            val selected = tracks.firstOrNull { qualityLabels.getOrNull(tracks.indexOf(it)) == choice }
                ?: wantedHeight?.let { wanted ->
                    tracks.filter { it.height == wanted }.minByOrNull { it.bitrate }
                        ?: tracks.filter { it.height <= wanted }.maxWithOrNull(compareBy<VideoQualityTrack> { it.height }.thenBy { it.bitrate })
                        ?: tracks.minByOrNull { it.height }
                }
            if (selected != null) {
                params.addOverride(TrackSelectionOverride(selected.group, selected.index))
            }
        }
    }

    trackSelector.setParameters(params)
}

data class SavedStreamItem(
    val title: String,
    val url: String,
    val userAgent: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

private object PlayerStorage {
    private const val PREFS_NAME = "madar_player_prefs"
    private const val KEY_HISTORY = "playback_history"
    private const val KEY_FAVORITES = "playback_favorites"

    fun getHistory(context: Context): List<SavedStreamItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(jsonStr)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(SavedStreamItem(
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        userAgent = obj.optString("userAgent"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addToHistory(context: Context, item: SavedStreamItem) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.url == item.url }
        current.add(0, item)
        if (current.size > 50) current.removeAt(current.size - 1)
        saveHistoryList(context, current)
    }

    fun deleteHistoryItem(context: Context, url: String) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.url == url }
        saveHistoryList(context, current)
    }

    fun clearHistory(context: Context) {
        saveHistoryList(context, emptyList())
    }

    private fun saveHistoryList(context: Context, list: List<SavedStreamItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("userAgent", item.userAgent)
            obj.put("timestamp", item.timestamp)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, arr.toString())
            .apply()
    }

    fun getFavorites(context: Context): List<SavedStreamItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(jsonStr)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(SavedStreamItem(
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        userAgent = obj.optString("userAgent"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun toggleFavorite(context: Context, item: SavedStreamItem): Boolean {
        val current = getFavorites(context).toMutableList()
        val exists = current.any { it.url == item.url }
        if (exists) {
            current.removeAll { it.url == item.url }
        } else {
            current.add(0, item)
        }
        saveFavoritesList(context, current)
        return !exists
    }

    private fun saveFavoritesList(context: Context, list: List<SavedStreamItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("userAgent", item.userAgent)
            obj.put("timestamp", item.timestamp)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAVORITES, arr.toString())
            .apply()
    }
}

@Composable
private fun PlayerDashboardScreen(
    onPlay: (title: String, url: String, userAgent: String) -> Unit,
    onPickLocalFile: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: المشغل, 1: السجل, 2: المفضلة

    var titleInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var uaInput by remember { mutableStateOf("") }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    var historyList by remember { mutableStateOf(PlayerStorage.getHistory(context)) }
    var favoritesList by remember { mutableStateOf(PlayerStorage.getFavorites(context)) }

    // Animations: Infinite pulse & shimmer transitions
    val infiniteTransition = rememberInfiniteTransition(label = "zag_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_pulse"
    )

    // Animated visualizer wave bars
    val wave1 by infiniteTransition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w1")
    val wave2 by infiniteTransition.animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(750, delayMillis = 100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w2")
    val wave3 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(480, delayMillis = 200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w3")
    val wave4 by infiniteTransition.animateFloat(0.4f, 0.85f, infiniteRepeatable(tween(680, delayMillis = 150, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w4")
    val wave5 by infiniteTransition.animateFloat(0.3f, 0.95f, infiniteRepeatable(tween(600, delayMillis = 250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w5")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090D16),
                            Color(0xFF0F172A),
                            Color(0xFF1E1B4B)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right: App Brand Title & Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Z",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ZAG PLAYER",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "مشغل الوسائط والبث المباشر",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Left: 3-dots Menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⋮",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            DropdownMenuItem(
                                text = { Text("📁 فتح ملف من الهاتف", color = Color(0xFFF1F5F9), fontSize = 14.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onPickLocalFile()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚡ تجربة بث حي (HLS)", color = Color(0xFF38BDF8), fontSize = 14.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onPlay("بث تجريبي HLS", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "")
                                }
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E293B)))
                            DropdownMenuItem(
                                text = { Text("🔒 سياسة الخصوصية", color = Color(0xFFCBD5E1), fontSize = 14.sp) },
                                onClick = {
                                    menuExpanded = false
                                    showPrivacyDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ℹ️ عن المشغل", color = Color(0xFFCBD5E1), fontSize = 14.sp) },
                                onClick = {
                                    menuExpanded = false
                                    showAboutDialog = true
                                }
                            )
                        }
                    }
                }

                // Sub-bar Cyber Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x331E293B))
                        .border(1.dp, Color(0x4D6366F1), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "جاهز للتشغيل الفائق",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "4K Ultra HD • HLS Engine",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hero Visualizer Video Container (Cyber Glow Box)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(205.dp)
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF070B14), Color(0xFF0F172A))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFF06B6D4), Color(0xFFA855F7))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated Wave Visualizer Bars + Play Center
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Left wave bars
                            Box(modifier = Modifier.width(4.dp).height((32 * wave1).dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF06B6D4)))
                            Box(modifier = Modifier.width(4.dp).height((42 * wave2).dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF6366F1)))

                            // Center 3D Glowing Play Icon
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "▶",
                                    color = Color.White,
                                    fontSize = 24.sp
                                )
                            }

                            // Right wave bars
                            Box(modifier = Modifier.width(4.dp).height((40 * wave3).dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF8B5CF6)))
                            Box(modifier = Modifier.width(4.dp).height((30 * wave4).dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF06B6D4)))
                            Box(modifier = Modifier.width(4.dp).height((22 * wave5).dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF38BDF8)))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x801E293B))
                                .border(1.dp, Color(0x66334155), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "أضف رابط فيديو أو اختر ملفاً للتشغيل الفوري",
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Slate Surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    color = Color(0xFF0B0F19),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        // 3 Modern Tabs with Glowing Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF111827))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isTab0 = selectedTab == 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isTab0) Brush.horizontalGradient(
                                            listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                        ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { selectedTab = 0 }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "المشغل",
                                    color = if (isTab0) Color.White else Color(0xFF94A3B8),
                                    fontWeight = if (isTab0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }

                            val isTab1 = selectedTab == 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isTab1) Brush.horizontalGradient(
                                            listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                        ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable {
                                        historyList = PlayerStorage.getHistory(context)
                                        selectedTab = 1
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "السجل",
                                    color = if (isTab1) Color.White else Color(0xFF94A3B8),
                                    fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }

                            val isTab2 = selectedTab == 2
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isTab2) Brush.horizontalGradient(
                                            listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                        ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable {
                                        favoritesList = PlayerStorage.getFavorites(context)
                                        selectedTab = 2
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "المفضلة",
                                    color = if (isTab2) Color.White else Color(0xFF94A3B8),
                                    fontWeight = if (isTab2) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab Contents with Animated Transitions
                        when (selectedTab) {
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Field 1: Title
                                    DashboardCustomInputField(
                                        value = titleInput,
                                        onValueChange = { titleInput = it },
                                        placeholder = "عنوان الفيديو (اختياري)",
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x336366F1)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "T",
                                                    color = Color(0xFF818CF8),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    )

                                    // Field 2: URL with Paste
                                    DashboardCustomInputField(
                                        value = urlInput,
                                        onValueChange = { urlInput = it },
                                        placeholder = "رابط الفيديو (HLS, MP4, MKV...)",
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x3306B6D4)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "🔗",
                                                    fontSize = 16.sp
                                                )
                                            }
                                        },
                                        actionButton = {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF312E81))
                                                    .clickable {
                                                        val pasteText = clipboardManager.getText()?.text
                                                        if (!pasteText.isNullOrBlank()) {
                                                            urlInput = pasteText.trim()
                                                            Toast.makeText(context, "تم لصق الرابط", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "لصق",
                                                    color = Color(0xFFA5B4FC),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    )

                                    // Field 3: User-Agent
                                    DashboardCustomInputField(
                                        value = uaInput,
                                        onValueChange = { uaInput = it },
                                        placeholder = "وكيل المستخدم User-Agent (اختياري)",
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x338B5CF6)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "🌐",
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Pulsing Animated Action Button: ▶ حفظ وتشغيل
                                    Button(
                                        onClick = {
                                            val cleanUrl = urlInput.trim()
                                            if (cleanUrl.isBlank()) {
                                                Toast.makeText(context, "يرجى إدخال رابط الفيديو أولاً", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val validUrl = if (!cleanUrl.startsWith("http://", true) &&
                                                    !cleanUrl.startsWith("https://", true) &&
                                                    !cleanUrl.startsWith("content://", true) &&
                                                    !cleanUrl.startsWith("file://", true)) {
                                                    "https://$cleanUrl"
                                                } else cleanUrl

                                                onPlay(titleInput.trim().ifBlank { "ZAG Player" }, validUrl, uaInput.trim())
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .scale(pulseScale),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF06B6D4))
                                                    ),
                                                    RoundedCornerShape(14.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "تشغيل الفيديو الآن",
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "▶",
                                                    color = Color.White,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }

                                    // Fast Utilities Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onPickLocalFile,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155))
                                        ) {
                                            Text("📁 فتح ملف من الهاتف", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                titleInput = "ZAG Demo Stream"
                                                urlInput = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                                                onPlay("ZAG Demo Stream", urlInput, "")
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF4F46E5))
                                        ) {
                                            Text("⚡ تجربة بث HLS", fontSize = 12.sp, color = Color(0xFF818CF8))
                                        }
                                    }

                                    if (urlInput.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                PlayerStorage.toggleFavorite(
                                                    context,
                                                    SavedStreamItem(
                                                        title = titleInput.ifBlank { "ZAG Bookmark" },
                                                        url = urlInput.trim(),
                                                        userAgent = uaInput.trim()
                                                    )
                                                )
                                                Toast.makeText(context, "تمت الإضافة إلى المفضلة", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("⭐ حفظ الرابط في المفضلة", color = Color(0xFF38BDF8), fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            1 -> {
                                if (historyList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🕒", fontSize = 42.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("لا يوجد سجل تشغيل حالياً", color = Color(0xFF94A3B8), fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("أي فيديو تشغله سيظهر هنا للوصول السريع", color = Color(0xFF64748B), fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("عدد العناصر المحفوظة: ${historyList.size}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                            TextButton(onClick = {
                                                PlayerStorage.clearHistory(context)
                                                historyList = emptyList()
                                                Toast.makeText(context, "تم مسح السجل", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("مسح الكل", color = Color(0xFFF43F5E), fontSize = 13.sp)
                                            }
                                        }
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(historyList, key = { it.url + it.timestamp }) { item ->
                                                SavedStreamCard(
                                                    item = item,
                                                    onPlay = {
                                                        onPlay(item.title, item.url, item.userAgent)
                                                    },
                                                    onDelete = {
                                                        PlayerStorage.deleteHistoryItem(context, item.url)
                                                        historyList = PlayerStorage.getHistory(context)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                if (favoritesList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("⭐", fontSize = 42.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("لا توجد روابط مفضلة بعد", color = Color(0xFF94A3B8), fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("احفظ روابطك المفضلة للوصول إليها بنقرة واحدة", color = Color(0xFF64748B), fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(favoritesList, key = { it.url }) { item ->
                                            SavedStreamCard(
                                                item = item,
                                                onPlay = {
                                                    onPlay(item.title, item.url, item.userAgent)
                                                },
                                                onDelete = {
                                                    PlayerStorage.toggleFavorite(context, item)
                                                    favoritesList = PlayerStorage.getFavorites(context)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    title = { Text("🔒 سياسة الخصوصية - ZAG Player", fontWeight = FontWeight.Bold, color = Color.White) },
                    containerColor = Color(0xFF0F172A),
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = "تطبيق ZAG Player يحترم خصوصية المستخدم بالكامل:\n\n" +
                                    "1. عدم جمع البيانات: التطبيق لا يقوم بجمع أو إرسال أو بيع أي بيانات شخصية أو معلومات عن جهازك.\n\n" +
                                    "2. التخزين المحلي: يتم تخزين سجل المشاهدة والمفضلة محلياً فقط على هاتفك.\n\n" +
                                    "3. أداة مشغل وسائط: هذا التطبيق أداة مشغل فيديو نظيفة وحيادية تماماً، ولا يحتوي على أي محتوى مدمج مسبقاً.\n\n" +
                                    "4. حقوق الملكية: المستخدم مسؤول مسؤولية كاملة عن الروابط ومصادر البث التي يضيفها بما يتوافق مع القوانين الدولية وحقوق النشر.",
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showPrivacyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("موافق", color = Color.White)
                        }
                    }
                )
            }

            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = { Text("ZAG Player v1.1.20", fontWeight = FontWeight.Bold, color = Color.White) },
                    containerColor = Color(0xFF0F172A),
                    text = {
                        Column {
                            Text(
                                text = "مشغل وسائط متعدد الصيغ عالي الأداء يدعم بروتوكولات HLS و DASH وملفات الفيديو المحلية.\n\n" +
                                    "الإصدار: 1.1.20\n" +
                                    "الاسم التجاري: ZAG Player (zag player)\n" +
                                    "الحالة: جاهز للنشر على Google Play\n" +
                                    "التوافق: Android 6.0 حتى Android 16 (API 36)",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showAboutDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("إغلاق", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardCustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: @Composable () -> Unit,
    actionButton: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFFF8FAFC),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (actionButton != null) {
                Spacer(modifier = Modifier.width(8.dp))
                actionButton()
            }
        }
    }
}

@Composable
private fun SavedStreamCard(
    item: SavedStreamItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        color = Color(0xFF111827)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "فيديو بدون عنوان" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFF1F5F9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.url,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlay) {
                    Text("▶", color = Color(0xFF38BDF8), fontSize = 16.sp)
                }
                IconButton(onClick = onDelete) {
                    Text("🗑", fontSize = 14.sp)
                }
            }
        }
    }
}