new_player_activity = '''package com.madar.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.mediarouter.app.MediaRouteButton
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import java.net.URI
import java.util.Locale

class PlayerActivity : ComponentActivity() {
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

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "زقزوقة بلاير" }
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()
        val referer = intent.getStringExtra(EXTRA_REFERER).orEmpty()

        setContent {
            MaterialTheme {
                BackHandler {
                    finish()
                }

                if (isSupportedStream(streamUrl)) {
                    PlayerScreen(
                        streamUrl = streamUrl,
                        title = title,
                        userAgent = userAgent,
                        referer = referer,
                        onClose = { finish() }
                    )
                } else {
                    InvalidStreamScreen(onClose = { finish() })
                }
            }
        }
    }

    companion object {
        const val EXTRA_STREAM_URL = "com.madar.player.extra.STREAM_URL"
        const val EXTRA_TITLE = "com.madar.player.extra.TITLE"
        const val EXTRA_USER_AGENT = "com.madar.player.extra.USER_AGENT"
        const val EXTRA_REFERER = "com.madar.player.extra.REFERER"
    }
}

private fun isSupportedStream(url: String): Boolean =
    url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

private fun isDirectStreamUrl(url: String): Boolean {
    val clean = url.substringBefore('?')
    return clean.endsWith(".m3u8", ignoreCase = true) ||
           clean.endsWith(".mp4", ignoreCase = true) ||
           clean.endsWith(".mkv", ignoreCase = true) ||
           clean.endsWith(".ts", ignoreCase = true) ||
           url.contains("/live/", ignoreCase = true) ||
           url.contains("video-", ignoreCase = true)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(
    streamUrl: String,
    title: String,
    userAgent: String,
    referer: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var useWebViewFallback by remember(streamUrl) {
        mutableStateOf(!isDirectStreamUrl(streamUrl) && (streamUrl.contains("embed") || streamUrl.contains("iframe") || streamUrl.contains("play/=")))
    }
    var isBuffering by remember(streamUrl) { mutableStateOf(true) }
    var playbackError by remember(streamUrl) { mutableStateOf<String?>(null) }

    val cleanUa = if (userAgent.isNotBlank() && !userAgent.startsWith("okhttp", ignoreCase = true)) {
        userAgent
    } else {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    val cleanRef = if (referer.isNotBlank()) referer else try {
        val u = URI.create(streamUrl)
        "${u.scheme}://${u.host}/"
    } catch (_: Exception) { "" }

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

    val player = remember(streamUrl, useWebViewFallback) {
        if (useWebViewFallback) return@remember null

        val headers = buildMap {
            put("User-Agent", cleanUa)
            if (cleanRef.isNotBlank()) {
                put("Referer", cleanRef)
                put("Origin", cleanRef.trimEnd('/'))
            }
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(cleanUa)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(15000)
            .setDefaultRequestProperties(headers)

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .apply {
                if (streamUrl.contains(".m3u8", ignoreCase = true)) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
            .build()

        val mediaSource = if (streamUrl.contains(".m3u8", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(false)
                .createMediaSource(mediaItem)
        } else {
            DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
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
                        // Switch automatically to WebView fallback on player failure
                        playbackError = error.errorCodeName
                        isBuffering = false
                        useWebViewFallback = true
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
                        (0 until group.length).asSequence()
                            .filter(group::isTrackSelected)
                            .map(group::getTrackFormat)
                    }
                    .firstOrNull()
                adaptiveTracksAvailable = videoGroups.any { group ->
                    (0 until group.length).count(group::isTrackSupported) > 1
                }
                currentTrackInfo = selectedFormat?.let(::formatDescription)
                    ?: "جاري التشغيل"
            }
        }
        val analyticsListener = object : AnalyticsListener {
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
            ) {
                downloadedBytes += loadEventInfo.bytesLoaded
            }
        }
        player.addListener(tracksListener)
        player.addAnalyticsListener(analyticsListener)
        onDispose {
            player.removeListener(tracksListener)
            player.removeAnalyticsListener(analyticsListener)
        }
    }

    LaunchedEffect(trackSelector, selectedQuality) {
        applyQuality(trackSelector, selectedQuality)
    }

    CastStreamEffect(streamUrl, title)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (useWebViewFallback) {
            // Fullscreen Hardware-Accelerated WebView Fallback
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
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
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = cleanUa
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        loadUrl(streamUrl)
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

        // Top Navigation Bar (Close button + Title + Quality Picker + Cast)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Close / Back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.widthIn(max = 300.dp),
                )
            }

            // Right side: Quality Picker & Cast Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!useWebViewFallback) {
                    Box {
                        Surface(
                            onClick = { qualityMenuExpanded = true },
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
                                    },
                                )
                            }
                        }
                    }
                }

                AndroidView(
                    modifier = Modifier.size(44.dp),
                    factory = { viewContext ->
                        MediaRouteButton(viewContext).apply {
                            CastButtonFactory.setUpMediaRouteButton(viewContext, this)
                        }
                    },
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun applyQuality(trackSelector: DefaultTrackSelector, label: String) {
    val builder = trackSelector.buildUponParameters()
        .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        .setForceLowestBitrate(false)
        .setForceHighestSupportedBitrate(false)
        .setExceedVideoConstraintsIfNecessary(true)

    when (label) {
        "توفير البيانات" -> builder
            .setMaxVideoSize(854, 480)
            .setForceLowestBitrate(true)
        "480p" -> builder.setMaxVideoSize(854, 480)
        "720p" -> builder.setMaxVideoSize(1280, 720)
        "1080p" -> builder.setMaxVideoSize(1920, 1080)
        "أعلى جودة" -> builder.setForceHighestSupportedBitrate(true)
    }
    trackSelector.setParameters(builder)
}

private fun formatDescription(format: Format): String {
    val height = format.height.takeIf { it != Format.NO_VALUE && it > 0 }
        ?.let { "${it}p" } ?: "دقة غير معلنة"
    val bitrate = format.bitrate.takeIf { it != Format.NO_VALUE && it > 0 }
        ?.let { " · معدل المصدر ${String.format(Locale.US, "%.1f", it / 1_000_000f)} Mbps" }
        .orEmpty()
    return height + bitrate
}

private fun formatMegabytes(bytes: Long): String =
    String.format(Locale.US, "%.1f", bytes / (1024f * 1024f))

@Composable
private fun InvalidStreamScreen(onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF10131B)), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("افتح المشغل من تطبيق زقزوقة برابط تشغيل صالح", color = Color.White)
            Button(onClick = onClose) {
                Text("إغلاق")
            }
        }
    }
}

@Composable
private fun CastStreamEffect(streamUrl: String, title: String) {
    val context = LocalContext.current
    val castContext = remember {
        runCatching { CastContext.getSharedInstance(context) }.getOrNull()
    }
    DisposableEffect(castContext, streamUrl, title) {
        if (castContext == null) return@DisposableEffect onDispose {}
        val listener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) = sendToReceiver(session.remoteMediaClient, streamUrl, title)
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = sendToReceiver(session.remoteMediaClient, streamUrl, title)
            override fun onSessionEnded(session: CastSession, error: Int) = Unit
            override fun onSessionStarting(session: CastSession) = Unit
            override fun onSessionStartFailed(session: CastSession, error: Int) = Unit
            override fun onSessionEnding(session: CastSession) = Unit
            override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
            override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit
            override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
        }
        castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        onDispose { castContext.sessionManager.removeSessionManagerListener(listener, CastSession::class.java) }
    }
}

private fun sendToReceiver(remote: RemoteMediaClient?, streamUrl: String, title: String) {
    if (remote == null) return
    val metadata = com.google.android.gms.cast.MediaMetadata(com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MOVIE)
        .apply { putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE, title) }
    val contentType = if (streamUrl.contains(".m3u8", ignoreCase = true)) "application/x-mpegURL" else "video/mp4"
    val info = MediaInfo.Builder(streamUrl)
        .setContentType(contentType)
        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        .setMetadata(metadata)
        .build()
    remote.load(info, true)
}
'''

target_file = 'c:/Users/hassa/Desktop/all project/iptv app/MadarTV/madar-player/src/main/java/com/madar/player/PlayerActivity.kt'
with open(target_file, 'w', encoding='utf-8') as f:
    f.write(new_player_activity)
print('Updated PlayerActivity.kt successfully')
