package com.madar.tv.ui

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.madar.tv.R
import com.madar.tv.data.YacineStream
import com.madar.tv.ui.theme.ZakzoukaColors

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun YacineStreamPlayer(
    stream: YacineStream,
    modifier: Modifier = Modifier,
    onToggleFullscreen: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBuffering by remember(stream.url) { mutableStateOf(true) }
    var playbackError by remember(stream.url) { mutableStateOf<String?>(null) }
    var retryKey by remember(stream.url) { mutableIntStateOf(0) }

    val exoPlayer = remember(stream.url, retryKey) {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(4_000, 20_000, 500, 1_000)
            .build()

        val cleanUa = if (stream.userAgent.isNotBlank() && !stream.userAgent.startsWith("okhttp", ignoreCase = true)) {
            stream.userAgent
        } else {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        val cleanRef = if (stream.referer.isNotBlank()) stream.referer else "https://yacinelive.com/"

        val requestHeaders = stream.headers
            .filterKeys { key -> !key.equals("User-Agent", true) && !key.equals("Referer", true) && !key.equals("Origin", true) }
            .toMutableMap()
            .apply {
                put("User-Agent", cleanUa)
                put("Referer", cleanRef)
                runCatching {
                    val uri = java.net.URI(cleanRef)
                    if (!uri.scheme.isNullOrBlank() && !uri.rawAuthority.isNullOrBlank()) {
                        put("Origin", "${uri.scheme}://${uri.rawAuthority}")
                    }
                }
            }

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(cleanUa)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(15000)
            .setDefaultRequestProperties(requestHeaders)

        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .apply {
                if (stream.url.contains(".m3u8", ignoreCase = true)) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
            .build()

        val mediaSource = if (stream.url.contains(".m3u8", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(false)
                .createMediaSource(mediaItem)
        } else {
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                var playbackStartedLogged = false
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                        if (playbackState == Player.STATE_READY) {
                            playbackError = null
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && !playbackStartedLogged) {
                            playbackStartedLogged = true
                            com.madar.tv.core.ZakzoukaTelemetry.trackPlaybackStarted("live")
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        com.madar.tv.core.ZakzoukaTelemetry.trackPlaybackError("live")
                        playbackError = "تعذر تشغيل هذا الرابط حالياً (${error.errorCodeName})"
                        isBuffering = false
                    }
                })
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                Lifecycle.Event.ON_PAUSE  -> exoPlayer.pause()
                Lifecycle.Event.ON_STOP   -> exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Force LTR alignment for player container so BottomStart is strictly the Bottom-Left
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        keepScreenOn = true
                        if (onToggleFullscreen != null) {
                            setFullscreenButtonClickListener {
                                onToggleFullscreen()
                            }
                        }
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView -> playerView.player = exoPlayer }
            )

            // Buffering Indicator
            if (isBuffering && playbackError == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(32.dp))
                    Text("جاري تحميل البث المباشر...", color = Color.White, fontSize = 12.sp)
                }
            }

            // Error Overlay with Retry
            if (playbackError != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .padding(20.dp),
                ) {
                    Spacer(Modifier.weight(1f))
                    Text("⚠️", fontSize = 32.sp)
                    Text(
                        playbackError!!,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "اختر سيرفر أو جودة أخرى من القائمة",
                        color = ZakzoukaColors.TextMuted,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZakzoukaColors.Gold)
                            .clickable {
                                playbackError = null
                                isBuffering = true
                                retryKey++
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("إعادة المحاولة 🔄", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            // Dynamic Zakzouka Watermark Badge — Placed over upstream watermark
            ZakzoukaWatermarkBadge(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 14.dp)
            )
        }
    }
}

/**
 * Sleek, luxury branding badge that completely covers external/upstream stream watermarks
 */
@Composable
fun ZakzoukaWatermarkBadge(
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .width(175.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A0E17))
                .border(1.2.dp, Color(0xFFD4AF37), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, Color(0x66D4AF37), RoundedCornerShape(6.dp))
                .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zakzouka_logo),
                    contentDescription = "زقزوقة TV",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Arabic Branding Text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "زَقْـزُوقَـة TV",
                        color = Color(0xFFD4AF37),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "بث مباشر رسمي",
                        color = Color(0xFFE6EDF3),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
