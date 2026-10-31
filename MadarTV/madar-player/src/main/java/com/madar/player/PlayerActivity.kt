package com.madar.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private data class PlaybackSource(
    val url: String,
    val userAgent: String,
    val referer: String,
    val headers: Map<String, String> = emptyMap(),
)

class PlayerActivity : ComponentActivity() {

    private val currentStreamUrl = mutableStateOf("")
    private val currentTitle = mutableStateOf("زقزوقة بلاير")
    private val currentUserAgent = mutableStateOf("")
    private val currentReferer = mutableStateOf("")
    private val currentStreamHeaders = mutableStateOf<Map<String, String>>(emptyMap())
    private val currentFallbackUrl = mutableStateOf("")
    private val currentFallbackReferer = mutableStateOf("")
    private val currentBackupSources = androidx.compose.runtime.mutableStateListOf<PlaybackSource>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Immersive full-screen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        extractStreamFromIntent(intent)

        setContent {
            MaterialTheme {
                BackHandler {
                    finish()
                }

                val url = currentStreamUrl.value
                val title = currentTitle.value
                val userAgent = currentUserAgent.value
                val referer = currentReferer.value
                val streamHeaders = currentStreamHeaders.value
                val fallbackUrl = currentFallbackUrl.value
                val fallbackReferer = currentFallbackReferer.value

                if (isSupportedStream(url)) {
                    PlayerScreen(
                        streamUrl = url,
                        title = title,
                        userAgent = userAgent,
                        referer = referer,
                        streamHeaders = streamHeaders,
                        fallbackUrl = fallbackUrl,
                        fallbackReferer = fallbackReferer,
                        backupSources = currentBackupSources,
                        onSwitchStream = { next, nextUa, nextRef, nextHeaders ->
                            currentStreamUrl.value = next
                            currentUserAgent.value = nextUa.ifBlank { userAgent }
                            currentReferer.value = nextRef.ifBlank { referer }
                            currentStreamHeaders.value = nextHeaders
                        },
                        onClose = { finish() }
                    )
                } else {
                    InvalidStreamScreen(onClose = { finish() })
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractStreamFromIntent(intent)
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
    }
}

private fun isSupportedStream(url: String): Boolean =
    url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

private fun originForHeader(url: String): String = runCatching {
    val parsed = URI.create(url)
    "${parsed.scheme}://${parsed.host}" + (if (parsed.port >= 0) ":${parsed.port}" else "")
}.getOrDefault("")

private fun isDirectStreamUrl(url: String): Boolean {
    val clean = url.substringBefore('?')
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
    "adcolony", "inmobi", "taboola", "outbrain", "adnxs", "criteo"
)

private fun isBlockedAd(url: String): Boolean {
    val parsed = runCatching { URI.create(url) }.getOrNull() ?: return false
    val host = parsed.host.orEmpty().lowercase(Locale.ROOT)
    val path = parsed.path.orEmpty()
    if (AD_HOST_MARKERS.any(host::contains)) return true
    // Match whole path segments only. A substring check on the entire URL blocked valid
    // media paths such as /download/video.mp4 and URLs with unrelated query values.
    return Regex("(?i)(^|/)(ads?|advert|banner|popunder|onclick|tracking|analytics)(/|$)")
        .containsMatchIn(path)
}

private fun isMediaRequest(url: String): Boolean {
    val parsed = runCatching { URI.create(url) }.getOrNull() ?: return false
    val path = parsed.path.orEmpty().lowercase(Locale.ROOT)
    return listOf(".m3u8", ".mpd", ".mp4", ".mkv", ".webm", ".m4v", ".mov", ".avi", ".ts")
        .any(path::endsWith) || path.contains("/hls/")
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
                val conn = URL("https://vidara.to/api/stream").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.setRequestProperty("User-Agent", ua)
                conn.setRequestProperty("Referer", urlStr)
                conn.setRequestProperty("Origin", "https://vidara.to")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
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

        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 7000
        conn.readTimeout = 7000
        conn.setRequestProperty("User-Agent", ua)
        conn.setRequestProperty("Referer", origin)
        val html = conn.inputStream.bufferedReader().use { it.readText() }

        // Source tags (e.g. MySeed / player-gateway.php)
        val srcMatch = Regex("""<source\s+[^>]*src=["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE).find(html)
        srcMatch?.groupValues?.get(1)?.let { return it }

        // Vidmoly / m3u8 direct
        val m3u8Match = Regex("""["'](https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)["']""", RegexOption.IGNORE_CASE).find(html)
        m3u8Match?.groupValues?.get(1)?.let { return it }

        // Uqload mp4 direct
        val uqMatch = Regex("""sources\s*:\s*\[\s*["']([^"']+\.mp4[^"']*)["']""").find(html)
        uqMatch?.groupValues?.get(1)?.let { return it }

        // Iframe check (e.g. /vids.php -> player-gateway.php)
        val ifrMatch = Regex("""<iframe[^>]+src=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        ifrMatch?.groupValues?.get(1)?.let { ifrUrl ->
            val cleanIfr = ifrUrl.replace("&amp;", "&")
            if (!isBlockedAd(cleanIfr)) {
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
    backupSources: androidx.compose.runtime.snapshots.SnapshotStateList<PlaybackSource>,
    onSwitchStream: (String, String, String, Map<String, String>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val diagnosticScope = rememberCoroutineScope()
    val isDirectStream = isDirectStreamUrl(streamUrl)
    val isEmbedPage = !isDirectStream
    var useWebViewFallback by remember(streamUrl) { mutableStateOf(isEmbedPage) }
    var exoRetryKey by remember(streamUrl) { mutableIntStateOf(0) }
    var isBuffering by remember(streamUrl) { mutableStateOf(true) }
    var playbackError by remember(streamUrl) { mutableStateOf<String?>(null) }
    var sourceDiagnostic by remember(streamUrl) { mutableStateOf<String?>(null) }

    val cleanUa = if (userAgent.isNotBlank()) {
        userAgent
    } else {
        "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    val cleanRef = if (referer.isNotBlank()) referer else try {
        val u = URI.create(streamUrl)
        "${u.scheme}://${u.host}/"
    } catch (_: Exception) { "" }

    // ── Fast Real-Time Background Stream Extractor ─────────────────────────
    LaunchedEffect(streamUrl) {
        if (!isDirectStreamUrl(streamUrl)) {
            withContext(Dispatchers.IO) {
                val direct = extractDirectStream(streamUrl, cleanRef, cleanUa)
                if (!direct.isNullOrBlank() && direct != streamUrl) {
                    withContext(Dispatchers.Main) {
                    // A media URL extracted from an embed is normally hotlink-protected.
                    // Keep the embed page as Referer when handing that media URL to ExoPlayer.
                    onSwitchStream(direct, cleanUa, streamUrl.ifBlank { cleanRef }, streamHeaders)
                    }
                }
            }
        }
    }

    val trackSelector = remember(streamUrl) { DefaultTrackSelector(context) }
    val preferences = remember(context) { context.getSharedPreferences("madar_player", 0) }
    var selectedQuality by remember {
        mutableStateOf(preferences.getString("quality", "تلقائي") ?: "تلقائي")
    }
    var qualityMenuExpanded by remember { mutableStateOf(false) }
    var currentTrackInfo by remember(streamUrl) { mutableStateOf("جارٍ فحص مسارات المصدر") }
    var adaptiveTracksAvailable by remember(streamUrl) { mutableStateOf<Boolean?>(null) }
    var downloadedBytes by remember(streamUrl) { mutableLongStateOf(0L) }
    val qualityChoices = listOf("تلقائي", "توفير البيانات", "480p", "720p", "1080p", "أعلى جودة")

    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    val player = remember(streamUrl, streamHeaders, useWebViewFallback, exoRetryKey) {
        if (useWebViewFallback) return@remember null

        val headers = streamHeaders
            .filterKeys { key -> !key.equals("User-Agent", true) && !key.equals("Referer", true) && !key.equals("Origin", true) }
            .toMutableMap()
            .apply {
                put("User-Agent", cleanUa)
                if (cleanRef.isNotBlank()) {
                    put("Referer", cleanRef)
                    originForHeader(cleanRef).takeIf(String::isNotBlank)?.let { put("Origin", it) }
                }
            }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(cleanUa)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(30000)
            .setDefaultRequestProperties(headers)

        val isHls = isHlsStream(streamUrl)

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
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

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = (state == Player.STATE_BUFFERING)
                        if (state == Player.STATE_READY) {
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

                val heights = videoGroups.asSequence()
                    .flatMap { group -> (0 until group.length).map { group.getTrackFormat(it).height } }
                    .filter { it > 0 }
                    .distinct()
                    .sorted()
                    .toList()

                adaptiveTracksAvailable = heights.size > 1

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
                currentTrackInfo = "$streamType · $resText$bitrateText"
            }
        }
        player.addListener(tracksListener)
        onDispose { player.removeListener(tracksListener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
                if (showControls) lastInteractionTime = System.currentTimeMillis()
            }
    ) {
        if (useWebViewFallback) {
            // Fullscreen Hardware-Accelerated Ad-Blocked WebView Fallback
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    val mediaHandoffScheduled = AtomicBoolean(false)
                    val activePageUrl = AtomicReference(cleanRef.ifBlank { streamUrl })
                    WebView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = cleanUa
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
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                if (isBlockedAd(reqUrl)) return true
                                if (reqUrl.startsWith("intent:") || reqUrl.startsWith("market:") || reqUrl.startsWith("whatsapp:")) return true
                                return false
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: return null
                                if (isBlockedAd(reqUrl)) {
                                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                }
                                if (isMediaRequest(reqUrl) && mediaHandoffScheduled.compareAndSet(false, true)) {
                                    val pageReferer = activePageUrl.get().ifBlank { cleanRef.ifBlank { streamUrl } }
                                    Handler(Looper.getMainLooper()).post {
                                        onSwitchStream(reqUrl, cleanUa, pageReferer, streamHeaders)
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (!url.isNullOrBlank()) activePageUrl.set(url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var ads = document.querySelectorAll('.div-over, .overlay, #modal, .modalDialog, #status, [id^="ad-"], [class^="ad-"], [id*="-ad-"], [class*="-ad-"], [id*="banner"], [class*="banner"], iframe[src*="ads"]');
                                        ads.forEach(function(el) { try { el.remove(); } catch(e){} });
                                        var v = document.querySelector('video');
                                        if (v) { v.muted = false; v.play(); }
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                        }
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        if (cleanRef.isNotBlank()) {
                            loadUrl(streamUrl, mapOf("Referer" to cleanRef))
                        } else {
                            loadUrl(streamUrl)
                        }
                    }
                }
            )
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
                        this.player = player
                    }
                },
                update = { it.player = player },
            )
        }

        // Buffering Indicator
        if (isBuffering && !useWebViewFallback) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37), modifier = Modifier.size(48.dp))
            }
        }

        playbackError?.let { errorCode ->
            if (!useWebViewFallback) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("تعذر تشغيل هذا المصدر", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("رمز الخطأ: $errorCode", color = Color.LightGray, fontSize = 13.sp)
                    sourceDiagnostic?.let { diagnostic ->
                        Text(diagnostic, color = Color.LightGray, fontSize = 12.sp)
                    }
                    Button(onClick = {
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
                        }
                    }) {
                        Text("إعادة المحاولة")
                    }
                }
            }
        }

        // Top Navigation Bar (Close button + Quality Picker) - Auto-hides; no title blocking match scoreboard
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Close / Back button (clean, never obstructs the match score!)
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

                // Right side: Quality Picker
                if (!useWebViewFallback) {
                    Box {
                        Surface(
                            onClick = {
                                qualityMenuExpanded = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            color = Color(0xD9272C3D),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = "الجودة · " + selectedQuality,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = qualityMenuExpanded,
                            onDismissRequest = { qualityMenuExpanded = false },
                        ) {
                            qualityChoices.forEach { quality ->
                                DropdownMenuItem(
                                    text = { Text(quality) },
                                    onClick = {
                                        applyQuality(trackSelector, quality)
                                        selectedQuality = quality
                                        preferences.edit().putString("quality", quality).apply()
                                        qualityMenuExpanded = false
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Zakzouka TV Watermark (Bottom-Start corner) ──
        // Unobtrusive, semi-transparent, branded, never covers score or channel logo
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
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
private fun applyQuality(trackSelector: DefaultTrackSelector, choice: String) {
    val params = trackSelector.buildUponParameters()
    when (choice) {
        "توفير البيانات" -> {
            params.setMaxVideoSize(640, 360)
            params.setMaxVideoBitrate(800_000)
            params.setForceLowestBitrate(true)
        }
        "480p" -> {
            params.setMaxVideoSize(854, 480)
            params.setMaxVideoBitrate(1_500_000)
            params.setForceLowestBitrate(false)
        }
        "720p" -> {
            params.setMaxVideoSize(1280, 720)
            params.setMaxVideoBitrate(3_500_000)
            params.setForceLowestBitrate(false)
        }
        "1080p" -> {
            params.setMaxVideoSize(1920, 1080)
            params.setMaxVideoBitrate(8_000_000)
            params.setForceLowestBitrate(false)
        }
        "أعلى جودة" -> {
            params.clearVideoSizeConstraints()
            params.setMaxVideoBitrate(Int.MAX_VALUE)
            params.setForceHighestSupportedBitrate(true)
        }
        else -> {
            params.clearVideoSizeConstraints()
            params.setMaxVideoBitrate(Int.MAX_VALUE)
            params.setForceLowestBitrate(false)
            params.setForceHighestSupportedBitrate(false)
        }
    }
    trackSelector.setParameters(params)
}

@Composable
private fun InvalidStreamScreen(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("رابط البث غير صالح أو غير مدعوم", color = Color.White, fontSize = 16.sp)
            Button(onClick = onClose) {
                Text("إغلاق")
            }
        }
    }
}
