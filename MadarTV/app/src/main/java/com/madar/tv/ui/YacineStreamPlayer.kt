package com.madar.tv.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.madar.tv.data.YacineStream

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun YacineStreamPlayer(
    stream: YacineStream,
    modifier: Modifier = Modifier,
    onToggleFullscreen: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(stream.url) {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(3_000, 15_000, 500, 1_000)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(stream.userAgent)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(8000)
                    .setReadTimeoutMs(10000)
                    .setDefaultRequestProperties(
                        buildMap {
                            put("User-Agent", stream.userAgent)
                            if (stream.referer.isNotEmpty()) put("Referer", stream.referer)
                        }
                    )
                val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(stream.url))
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    DisposableEffect(lifecycleOwner) {
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

    AndroidView(
        modifier = modifier,
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
}
