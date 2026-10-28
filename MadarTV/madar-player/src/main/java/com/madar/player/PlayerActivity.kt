package com.madar.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import androidx.mediarouter.app.MediaRouteButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import java.util.Locale

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "مشغل مشهد" }
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()
        val referer = intent.getStringExtra(EXTRA_REFERER).orEmpty()
        setContent {
            MaterialTheme {
                if (isSupportedStream(streamUrl)) PlayerScreen(streamUrl, title, userAgent, referer)
                else InvalidStreamScreen()
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
    url.startsWith("https://", ignoreCase = true)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(streamUrl: String, title: String, userAgent: String, referer: String) {
    val context = LocalContext.current
    val trackSelector = remember(streamUrl) { DefaultTrackSelector(context) }
    val player = remember(streamUrl) {
        val headers = buildMap {
            referer.takeIf { it.startsWith("https://", ignoreCase = true) }?.let { put("Referer", it) }
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .apply { userAgent.takeIf { it.isNotBlank() }?.let(::setUserAgent) }
            .setDefaultRequestProperties(headers)
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
                playWhenReady = true
            }
    }
    val preferences = remember(context) { context.getSharedPreferences("madar_player", 0) }
    var selectedQuality by remember {
        mutableStateOf(preferences.getString("quality", "تلقائي") ?: "تلقائي")
    }
    var qualityMenuExpanded by remember { mutableStateOf(false) }
    var currentTrackInfo by remember(streamUrl) { mutableStateOf("جارٍ فحص مسارات المصدر") }
    var adaptiveTracksAvailable by remember(streamUrl) { mutableStateOf<Boolean?>(null) }
    var downloadedBytes by remember(streamUrl) { mutableLongStateOf(0L) }
    val qualityChoices = listOf("تلقائي", "توفير البيانات", "480p", "720p", "1080p", "أعلى جودة")

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(player) {
        val tracksListener = object : androidx.media3.common.Player.Listener {
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
                    ?: "لم تُحدد جودة فيديو بعد"
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

    androidx.compose.runtime.LaunchedEffect(trackSelector, selectedQuality) {
        applyQuality(trackSelector, selectedQuality)
    }

    CastStreamEffect(streamUrl, title)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    keepScreenOn = true
                    useController = true
                    this.player = player
                }
            },
            update = { it.player = player },
        )

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(max = 240.dp).padding(top = 10.dp),
            )
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
                    DropdownMenuItem(
                        text = {
                            Text(
                                "التلقائي يكيّف الجودة مع الشبكة؛ توفير البيانات يختار أقل مسار متاح حتى 480p. يلزم أن يوفّر المصدر أكثر من جودة.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                            )
                        },
                        enabled = false,
                        onClick = {},
                    )
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

        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            color = Color(0xD9272C3D),
            shape = RoundedCornerShape(14.dp),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("الجودة الحالية: $currentTrackInfo", color = Color.White, fontSize = 12.sp)
                val sourceStatus = when (adaptiveTracksAvailable) {
                    true -> when (selectedQuality) {
                        "تلقائي" -> "تلقائي: الجودة تتكيف مع سرعة الشبكة"
                        "توفير البيانات" -> "توفير البيانات: أقل مسار متاح حتى 480p"
                        else -> "اختيار يدوي؛ الجودات تتبع ما يوفّره المصدر"
                    }
                    false -> "المصدر بجودة واحدة؛ لا يمكن ضغط بياناته على الهاتف"
                    null -> "جارٍ فحص الجودات التي يوفّرها المصدر"
                }
                Text(sourceStatus, color = Color(0xFFD5D8E0), fontSize = 10.sp)
                Text(
                    "بيانات مستلمة في الجلسة: ${formatMegabytes(downloadedBytes)} MB تقريبًا",
                    color = Color(0xFFD5D8E0),
                    fontSize = 10.sp,
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
private fun InvalidStreamScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF10131B)), contentAlignment = Alignment.Center) {
        Text("افتح المشغل من تطبيق مشهد برابط تشغيل صالح", color = Color.White)
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
