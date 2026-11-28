package com.madar.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.Intent
import android.util.Base64
import android.util.Log
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
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
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import okhttp3.ConnectionPool
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

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

object PlayerAdManager {
    // Smart Direct ad endpoint (new ad network)
    const val DEFAULT_SMART_DIRECT_URL =
        "https://asiafilm.org/4/01e00aeb7c4b214f9e9469bcfc2b0486"

    private const val PREFS_NAME = "madar_player"
    private const val KEY_AD_ENABLED = "zag_ad_enabled"
    private const val KEY_AD_URL = "zag_ad_url"
    private const val KEY_AD_HTML = "zag_ad_html"
    private const val KEY_AD_DELAY = "zag_ad_delay"

    private val legacyMarkers = listOf(
        "uplcm.com",
        "nap5k.com",
        "3nbf4.com",
        "monetag",
        "propellerads",
        "12335206",
        "12333968",
        "12335194",
        "12335228",
        "12335268",
        "12335274",
    )

    var adHtmlSnippet by mutableStateOf("")
    var adUrl by mutableStateOf(DEFAULT_SMART_DIRECT_URL)
    var isAdEnabled by mutableStateOf(true)
    var skipDelaySeconds by mutableIntStateOf(5)
    @Volatile var lastStatusSyncMs: Long = 0L

    fun loadFromPrefs(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.contains(KEY_AD_ENABLED)) return
            isAdEnabled = prefs.getBoolean(KEY_AD_ENABLED, true)
            skipDelaySeconds = prefs.getInt(KEY_AD_DELAY, 5).coerceIn(3, 15)
            val savedHtml = sanitizeAdValue(prefs.getString(KEY_AD_HTML, "").orEmpty())
            val savedUrl = sanitizeAdValue(prefs.getString(KEY_AD_URL, "").orEmpty())
            adHtmlSnippet = savedHtml
            adUrl = if (!isAdEnabled) "" else savedUrl.ifBlank { DEFAULT_SMART_DIRECT_URL }
        }
    }

    fun saveToPrefs(context: Context) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_AD_ENABLED, isAdEnabled)
                .putString(KEY_AD_URL, adUrl)
                .putString(KEY_AD_HTML, adHtmlSnippet)
                .putInt(KEY_AD_DELAY, skipDelaySeconds)
                .apply()
        }
    }

    fun sanitizeAdValue(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return ""
        val lower = cleaned.lowercase(Locale.ROOT)
        return if (legacyMarkers.any(lower::contains)) "" else cleaned
    }

    fun resolvedDirectAdUrl(): String {
        if (!isAdEnabled) return ""
        return sanitizeAdValue(adUrl).ifBlank { DEFAULT_SMART_DIRECT_URL }
    }

    fun applyRemoteConfig(
        context: Context,
        rootJson: JSONObject,
        ownerAd: JSONObject?,
        partnerAd: JSONObject?,
        systemStatus: String,
    ) {
        val rootActiveSource = rootJson.optString("active_source", "").lowercase(Locale.ROOT)
        val ownerActiveSource = ownerAd?.optString("active_source", "").orEmpty().lowercase(Locale.ROOT)
        val activeSource = ownerActiveSource.ifBlank { rootActiveSource }.ifBlank { "owner" }
        val overridePartner = ownerAd?.optBoolean("override_partner", false) ?: false

        val selectedAd = when {
            systemStatus != "active" || activeSource == "disabled" -> null
            overridePartner -> ownerAd ?: partnerAd
            activeSource == "partner" -> partnerAd
            activeSource == "owner" -> ownerAd ?: partnerAd
            else -> ownerAd ?: partnerAd
        }

        val rootVideoEnabled = rootJson.optBoolean("video_enabled", true)
        val adEnabled = selectedAd != null &&
            selectedAd.optBoolean("enabled", true) &&
            selectedAd.optBoolean("video_enabled", rootVideoEnabled)

        if (!adEnabled) {
            isAdEnabled = false
            adHtmlSnippet = ""
            adUrl = ""
        } else {
            val activeObj = selectedAd!!
            isAdEnabled = true
            skipDelaySeconds = activeObj.optInt("skip_delay_seconds", 5).coerceIn(3, 15)
            val cleanHtml = sanitizeAdValue(activeObj.optString("ad_html").orEmpty())
            val fallbackPartnerUrl = sanitizeAdValue(partnerAd?.optString("ad_url").orEmpty())
            val cleanUrl = sanitizeAdValue(activeObj.optString("ad_url").orEmpty())
                .ifBlank { fallbackPartnerUrl }
            adHtmlSnippet = cleanHtml
            adUrl = cleanUrl.ifBlank { DEFAULT_SMART_DIRECT_URL }
        }
        saveToPrefs(context)
    }
}

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
    private val playbackSessionToken = mutableIntStateOf(0)
    private var launchedFromExternalIntent = false
    private val isPlayerBlocked = mutableStateOf(false)
    private val blockMessage = mutableStateOf("عفواً، المشغل متوقف حالياً للصيانة.")
    private var statusPollingJob: Job? = null
    private val activityScope = CoroutineScope(Dispatchers.Main)

    private fun checkSystemStatus(force: Boolean = false) {
        val nowMs = System.currentTimeMillis()
        if (!force && PlayerAdManager.lastStatusSyncMs > 0L && (nowMs - PlayerAdManager.lastStatusSyncMs) < 120_000L) {
            return
        }
        activityScope.launch(Dispatchers.IO) {
            val endpoints = listOf(
                "https://dashboard-api-indol-eight.vercel.app/api/system_control",
                "https://dashboard-api-amber-chi.vercel.app/api/system_control",
                "https://zaafilm.com/api/status",
                "https://madartv-default-rtdb.firebaseio.com/system_control.json"
            )
            for (endpoint in endpoints) {
                try {
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3000
                        readTimeout = 3000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", MOBILE_CHROME_UA)
                    }
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        val status = json.optString("status", "active").lowercase()
                        var msg = json.optString("message", "عفواً، المشغل متوقف حالياً للصيانة.")
                        val comps = json.optJSONObject("components")
                        val playerEnabled = comps?.optBoolean("player_enabled", true) ?: true

                        if (!playerEnabled && status == "active") {
                            msg = "عفواً، مشغل زاغ متوقف حالياً بأمر الإدارة."
                        }

                        // ── Ad Config: Sovereign Owner override vs Partner ad ──
                        var ownerAd = json.optJSONObject("owner_ad_config")
                        if (ownerAd == null) {
                            try {
                                val adConn = (URL("https://madartv-default-rtdb.firebaseio.com/system_control/owner_ad_config.json").openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 2500
                                    readTimeout = 2500
                                    setRequestProperty("User-Agent", MOBILE_CHROME_UA)
                                }
                                if (adConn.responseCode == 200) {
                                    val adText = adConn.inputStream.bufferedReader().use { it.readText() }
                                    if (adText.isNotBlank() && adText != "null") {
                                        ownerAd = JSONObject(adText)
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                        var partnerAd = json.optJSONObject("ad_config")
                        if (partnerAd == null) {
                            try {
                                val pConn = (URL("https://madartv-default-rtdb.firebaseio.com/system_control/ad_config.json").openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 2500
                                    readTimeout = 2500
                                    setRequestProperty("User-Agent", MOBILE_CHROME_UA)
                                }
                                if (pConn.responseCode == 200) {
                                    val pText = pConn.inputStream.bufferedReader().use { it.readText() }
                                    if (pText.isNotBlank() && pText != "null") {
                                        partnerAd = JSONObject(pText)
                                    }
                                }
                            } catch (e: Exception) {}
                        }

                        withContext(Dispatchers.Main) {
                            PlayerAdManager.applyRemoteConfig(
                                context = applicationContext,
                                rootJson = json,
                                ownerAd = ownerAd,
                                partnerAd = partnerAd,
                                systemStatus = status,
                            )
                            PlayerAdManager.lastStatusSyncMs = System.currentTimeMillis()
                            if (status == "wiped" || status == "frozen" || !playerEnabled) {
                                isPlayerBlocked.value = true
                                blockMessage.value = msg
                            } else {
                                isPlayerBlocked.value = false
                            }
                        }
                        break
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private fun startStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = activityScope.launch(Dispatchers.IO) {
            checkSystemStatus(force = false)
            while (isActive) {
                delay(300_000L)
                checkSystemStatus(force = true)
            }
        }
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
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
        playbackSessionToken.intValue += 1
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        try {
            @Suppress("DEPRECATION")
            setTaskDescription(android.app.ActivityManager.TaskDescription("zag player"))
        } catch (_: Throwable) {}
        overridePendingTransition(R.anim.player_enter, R.anim.player_exit)

        PlayerAdManager.loadFromPrefs(applicationContext)
        extractStreamFromIntent(intent)

        startStatusPolling()

        if (isSupportedStream(currentStreamUrl.value)) {
            launchedFromExternalIntent = true
            enterFullscreen()
        } else {
            launchedFromExternalIntent = false
            exitFullscreen()
        }

        setContent {
            MaterialTheme {
                if (isPlayerBlocked.value) {
                    BackHandler {
                        finish()
                    }
                    PlayerBlockedScreen(
                        message = blockMessage.value,
                        onRefresh = { checkSystemStatus() },
                        onClose = { finish() }
                    )
                } else {
                    val url = currentStreamUrl.value
                    val isPlaying = isSupportedStream(url)

                    if (isPlaying) {
                        val sessionKey = playbackSessionToken.intValue
                        val isRemoteStream = url.startsWith("http://", ignoreCase = true) ||
                            url.startsWith("https://", ignoreCase = true)
                        val directAdUrl = PlayerAdManager.resolvedDirectAdUrl()
                        val htmlAdSnippet = PlayerAdManager.sanitizeAdValue(PlayerAdManager.adHtmlSnippet)
                        val adSkipSeconds = PlayerAdManager.skipDelaySeconds
                        var isPreRollShowing by remember(sessionKey) {
                            mutableStateOf(
                                isRemoteStream &&
                                    PlayerAdManager.isAdEnabled &&
                                    (directAdUrl.isNotBlank() || htmlAdSnippet.isNotBlank())
                            )
                        }
                        var inAppWebAdUrl by remember(sessionKey) { mutableStateOf<String?>(null) }

                        LaunchedEffect(sessionKey, PlayerAdManager.isAdEnabled, directAdUrl, htmlAdSnippet) {
                            if (!PlayerAdManager.isAdEnabled || !isRemoteStream) {
                                isPreRollShowing = false
                            }
                        }

                        BackHandler {
                            if (inAppWebAdUrl != null) {
                                inAppWebAdUrl = null
                            } else if (isPreRollShowing) {
                                // Do not let Hardware Back bypass the pre-roll ad into the video;
                                // either close the playback session cleanly or let user finish watching.
                                if (launchedFromExternalIntent) {
                                    finish()
                                } else {
                                    currentStreamUrl.value = ""
                                    isPreRollShowing = false
                                    exitFullscreen()
                                }
                            } else if (launchedFromExternalIntent) {
                                finish()
                            } else {
                                currentStreamUrl.value = ""
                                exitFullscreen()
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
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
                                isAdOverlayActive = isPreRollShowing || (inAppWebAdUrl != null),
                                onOpenInAppAdUrl = { adUrl -> inAppWebAdUrl = adUrl },
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

                            if (isPreRollShowing) {
                                PreRollAdOverlay(
                                    adHtml = htmlAdSnippet,
                                    adUrl = directAdUrl,
                                    skipDelaySeconds = adSkipSeconds,
                                    onSkipAd = { isPreRollShowing = false },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (inAppWebAdUrl != null) {
                                InAppBrowserOverlay(
                                    url = inAppWebAdUrl!!,
                                    onClose = { inAppWebAdUrl = null }
                                )
                            }
                        }
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
    }

    override fun onDestroy() {
        statusPollingJob?.cancel()
        super.onDestroy()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        try {
            @Suppress("DEPRECATION")
            setTaskDescription(android.app.ActivityManager.TaskDescription("zag player"))
        } catch (_: Throwable) {}
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
            ?: intent.getStringExtra("title")).orEmpty().ifBlank { "ZAG Player" }
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
            playbackSessionToken.intValue += 1
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
    val parsed = URI.create(url.trim())
    val scheme = parsed.scheme?.takeIf(String::isNotBlank) ?: return@runCatching ""
    val host = parsed.host?.takeIf(String::isNotBlank) ?: return@runCatching ""
    "$scheme://$host" + (if (parsed.port >= 0) ":${parsed.port}" else "")
}.getOrDefault("")

private fun isYouTubeStream(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("youtube-nocookie.com")
}

// YouTube IFrame API: use https://www.youtube.com as the base URL so YouTube
// treats the embed as first-party (avoids Error 153 / origin restriction).
private const val YOUTUBE_EMBED_ORIGIN = "https://www.youtube.com"
private const val MOBILE_CHROME_UA =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

private fun isAsciiHeaderValue(value: String): Boolean =
    value.all { ch -> ch.code in 0x20..0x7E }

/**
 * Custom Media3 HttpDataSource backed by OkHttp with raw UTF-8 header forwarding
 * (`addUnsafeNonAscii`), preserving invisible UTF-8 tokens required by live CDNs
 * while HttpURLConnection/DefaultHttpDataSource corrupts them to ISO-8859-1.
 */
@UnstableApi
private class Utf8OkHttpDataSource(
    private val client: OkHttpClient,
    private val userAgent: String,
    private val defaultRequestProperties: Map<String, String>
) : BaseDataSource(true), HttpDataSource {

    class Factory(
        private val client: OkHttpClient = sharedHttpClient,
        private val userAgent: String = "Mozilla/5.0"
    ) : HttpDataSource.Factory {
        private var defaultProperties: Map<String, String> = emptyMap()

        override fun setDefaultRequestProperties(
            defaultRequestProperties: MutableMap<String, String>
        ): HttpDataSource.Factory {
            this.defaultProperties = defaultRequestProperties.toMap()
            return this
        }

        override fun createDataSource(): HttpDataSource =
            Utf8OkHttpDataSource(client, userAgent, defaultProperties)
    }

    companion object {
        val sharedHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(ConnectionPool(16, 5, java.util.concurrent.TimeUnit.MINUTES))
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private val instanceProperties = LinkedHashMap<String, String>()
    private var currentDataSpec: DataSpec? = null
    private var currentResponse: Response? = null
    private var responseStream: java.io.InputStream? = null
    private var opened = false
    private var bytesToRead: Long = C.LENGTH_UNSET.toLong()
    private var bytesRead: Long = 0L

    override fun setRequestProperty(name: String, value: String) {
        synchronized(instanceProperties) {
            instanceProperties[name] = value
        }
    }

    override fun clearRequestProperty(name: String) {
        synchronized(instanceProperties) {
            instanceProperties.remove(name)
        }
    }

    override fun clearAllRequestProperties() {
        synchronized(instanceProperties) {
            instanceProperties.clear()
        }
    }

    override fun getResponseCode(): Int = currentResponse?.code ?: -1

    override fun getResponseHeaders(): MutableMap<String, MutableList<String>> {
        val resp = currentResponse ?: return mutableMapOf()
        val out = LinkedHashMap<String, MutableList<String>>()
        for ((k, v) in resp.headers.toMultimap()) {
            out[k] = v.toMutableList()
        }
        return out
    }

    override fun getUri(): Uri? =
        currentResponse?.request?.url?.toString()?.let(Uri::parse) ?: currentDataSpec?.uri

    override fun open(dataSpec: DataSpec): Long {
        this.currentDataSpec = dataSpec
        this.bytesRead = 0L
        this.bytesToRead = C.LENGTH_UNSET.toLong()
        transferInitializing(dataSpec)

        val mergedHeaders = LinkedHashMap<String, String>()
        fun putCaseInsensitive(k: String, v: String) {
            val cleanV = v.replace("\r", "").replace("\n", "")
            val existing = mergedHeaders.keys.firstOrNull { it.equals(k, ignoreCase = true) }
            if (existing != null) mergedHeaders.remove(existing)
            if (cleanV.isNotBlank()) mergedHeaders[k] = cleanV
        }
        defaultRequestProperties.forEach { (k, v) -> putCaseInsensitive(k, v) }
        synchronized(instanceProperties) {
            instanceProperties.forEach { (k, v) -> putCaseInsensitive(k, v) }
        }
        dataSpec.httpRequestHeaders.forEach { (k, v) -> putCaseInsensitive(k, v) }
        if (mergedHeaders.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            mergedHeaders["User-Agent"] = userAgent
        }
        if (dataSpec.position != 0L || dataSpec.length != C.LENGTH_UNSET.toLong()) {
            val rangeEnd = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                (dataSpec.position + dataSpec.length - 1).toString()
            } else ""
            putCaseInsensitive("Range", "bytes=${dataSpec.position}-$rangeEnd")
        }

        val headersBuilder = Headers.Builder()
        mergedHeaders.forEach { (k, v) ->
            headersBuilder.addUnsafeNonAscii(k, v)
        }

        val reqBuilder = Request.Builder()
            .url(dataSpec.uri.toString())
            .headers(headersBuilder.build())
        val body = dataSpec.httpBody?.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        when (dataSpec.httpMethod) {
            DataSpec.HTTP_METHOD_POST -> reqBuilder.post(body ?: ByteArray(0).toRequestBody(null))
            DataSpec.HTTP_METHOD_HEAD -> reqBuilder.head()
            else -> reqBuilder.get()
        }

        val response = try {
            client.newCall(reqBuilder.build()).execute()
        } catch (ioe: java.io.IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                ioe,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }
        currentResponse = response
        val code = response.code
        val responseHeaders = getResponseHeaders()
        if (!response.isSuccessful) {
            val errBody = runCatching { response.body?.bytes() }.getOrNull() ?: ByteArray(0)
            closeConnectionQuietly()
            throw HttpDataSource.InvalidResponseCodeException(
                code,
                response.message,
                null,
                responseHeaders,
                dataSpec,
                errBody
            )
        }

        val respBody = response.body ?: run {
            closeConnectionQuietly()
            throw HttpDataSource.HttpDataSourceException(
                "Empty response body",
                dataSpec,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }
        responseStream = respBody.byteStream()
        val contentLength = respBody.contentLength()
        bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (contentLength >= 0L) {
            contentLength
        } else {
            C.LENGTH_UNSET.toLong()
        }
        opened = true
        transferStarted(dataSpec)
        return bytesToRead
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val rem = bytesToRead - bytesRead
            if (rem <= 0L) return C.RESULT_END_OF_INPUT
        }
        val toRead = if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            minOf(length.toLong(), bytesToRead - bytesRead).toInt()
        } else length
        val readCount = try {
            responseStream?.read(buffer, offset, toRead) ?: -1
        } catch (ioe: java.io.IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                ioe,
                currentDataSpec!!,
                HttpDataSource.HttpDataSourceException.TYPE_READ
            )
        }
        if (readCount == -1) return C.RESULT_END_OF_INPUT
        bytesRead += readCount
        bytesTransferred(readCount)
        return readCount
    }

    override fun close() {
        currentDataSpec = null
        closeConnectionQuietly()
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun closeConnectionQuietly() {
        runCatching { responseStream?.close() }
        runCatching { currentResponse?.close() }
        responseStream = null
        currentResponse = null
    }
}

private fun extractYouTubeId(url: String): String {
    return when {
        url.contains("/embed/") -> url.substringAfter("/embed/").substringBefore("?").substringBefore("&")
        url.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
        url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        else -> ""
    }.substringBefore("#").trim()
}

private fun isYouTubeVideoEmbeddable(videoId: String): Boolean {
    if (videoId.isBlank()) return false
    return runCatching {
        val conn = (URL("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json").openConnection() as HttpURLConnection).apply {
            connectTimeout = 2000
            readTimeout = 2000
            setRequestProperty("User-Agent", MOBILE_CHROME_UA)
        }
        val code = conn.responseCode
        conn.disconnect()
        code == 200
    }.getOrDefault(true)
}

private fun buildYouTubeIFrameHtml(
    videoId: String,
    subtitlesEnabled: Boolean = true,
    pausedForAd: Boolean = false,
): String {
    val ccPolicy = if (subtitlesEnabled) 1 else 0
    val autoplayFlag = if (pausedForAd) 0 else 1
    val pausedJs = if (pausedForAd) "true" else "false"
    return """
        <!DOCTYPE html>
        <html lang="ar">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>
          * { margin:0; padding:0; box-sizing:border-box; }
          html, body { width:100vw; height:100vh; overflow:hidden; background:#000; }
          #player { position:fixed; inset:0; width:100vw !important; height:100vh !important; border:none; }
        </style>
        </head>
        <body>
        <div id="player"></div>
        <script>
          var tag = document.createElement('script');
          tag.src = "https://www.youtube.com/iframe_api";
          var firstScriptTag = document.getElementsByTagName('script')[0];
          firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

          var player = null;
          var wantCaptions = ${if (subtitlesEnabled) "true" else "false"};
          window.__zagPausedForAd = $pausedJs;
          window.__zagGetPlayer = function() { return player; };
          window.__zagPlay = function() {
            try { if (player && player.unMute) player.unMute(); if (player && player.playVideo) player.playVideo(); } catch(e) {}
          };
          window.__zagPause = function() {
            try { if (player && player.pauseVideo) player.pauseVideo(); } catch(e) {}
          };
          window.__zagSeekTo = function(sec) {
            try { if (player && player.seekTo) { player.seekTo(sec, true); player.playVideo(); } } catch(e) {}
          };
          window.__zagSetSpeed = function(rate) {
            try { if (player && player.setPlaybackRate) player.setPlaybackRate(rate); } catch(e) {}
          };
          window.__zagSetQuality = function(q) {
            try {
              if (player) {
                if (typeof player.setPlaybackQualityRange === 'function') player.setPlaybackQualityRange(q, q);
                if (typeof player.setPlaybackQuality === 'function') player.setPlaybackQuality(q);
              }
            } catch(e) {}
          };
          window.__zagSetCaptions = function(enable) {
            wantCaptions = !!enable;
            try {
              if (!player) return;
              if (!wantCaptions) {
                if (player.unloadModule) player.unloadModule('captions');
                if (player.setOption) player.setOption('captions', 'track', {});
              } else {
                if (player.loadModule) player.loadModule('captions');
                if (player.setOption) player.setOption('captions', 'track', { languageCode: 'ar' });
              }
            } catch(e) {}
          };

          function reportQualities() {
            try {
              if (!player || !window.MadarBridge || !window.MadarBridge.onQualityLevels) return;
              var levels = (typeof player.getAvailableQualityLevels === 'function') ? player.getAvailableQualityLevels() : [];
              var cur = (typeof player.getPlaybackQuality === 'function') ? player.getPlaybackQuality() : '';
              if (levels && levels.length) {
                window.MadarBridge.onQualityLevels(JSON.stringify(levels), cur || '');
              }
            } catch(e) {}
          }

          function onYouTubeIframeAPIReady() {
            player = new YT.Player('player', {
              width: '100%',
              height: '100%',
              videoId: '$videoId',
              host: 'https://www.youtube.com',
              playerVars: {
                autoplay: $autoplayFlag,
                playsinline: 1,
                controls: 0,
                rel: 0,
                modestbranding: 1,
                iv_load_policy: 3,
                fs: 0,
                disablekb: 1,
                enablejsapi: 1,
                cc_load_policy: $ccPolicy,
                cc_lang_pref: 'ar',
                hl: 'ar',
                origin: 'https://www.youtube.com'
              },
              events: {
                'onReady': function(e) {
                  try {
                    e.target.unMute();
                    e.target.setVolume(100);
                    if (!window.__zagPausedForAd) {
                      e.target.playVideo();
                    }
                  } catch(x) {}
                  reportQualities();
                },
                'onStateChange': function(e) {
                  reportQualities();
                  if (!window.MadarBridge) return;
                  if (e.data === 1) window.MadarBridge.onStateChange('playing');
                  else if (e.data === 2) window.MadarBridge.onStateChange('paused');
                  else if (e.data === 3) window.MadarBridge.onStateChange('buffering');
                  else if (e.data === 0) window.MadarBridge.onStateChange('ended');
                },
                'onPlaybackQualityChange': function() { reportQualities(); },
                'onError': function(e) {
                  if (window.MadarBridge) window.MadarBridge.onPlayerError(e.data);
                }
              }
            });
          }

          setInterval(function() {
            try {
              if (player && typeof player.getCurrentTime === 'function') {
                var cur = player.getCurrentTime() || 0;
                var dur = player.getDuration() || 0;
                var state = (typeof player.getPlayerState === 'function') ? player.getPlayerState() : -1;
                if (window.MadarBridge) {
                  window.MadarBridge.onTimeUpdate(cur, dur);
                  if (state === 1 && cur > 0.15) window.MadarBridge.onStateChange('playing');
                }
              }
            } catch(e) {}
          }, 350);
        </script>
        </body>
        </html>
    """.trimIndent()
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
    // Preserve URL intact so watch and embed types are handled appropriately
    return url
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

private fun isYouTubeAd(url: String): Boolean {
    val lower = url.lowercase(Locale.ROOT)
    // Never block essential playback chunks, player base scripts, or fonts
    if (lower.contains("videoplayback") || lower.contains("/s/player/") || lower.contains("base.js")) {
        return false
    }
    return lower.contains("doubleclick") ||
           lower.contains("googleads") ||
           lower.contains("googlesyndication") ||
           lower.contains("/pagead/") ||
           lower.contains("/api/stats/ads") ||
           lower.contains("youtubei/v1/player/ad_break") ||
           lower.contains("/get_midroll_info") ||
           lower.contains("/ptracking") ||
           lower.contains("ad_type=") ||
           lower.contains("adformat=") ||
           lower.contains("adunit=") ||
           lower.contains("ad_break") ||
           lower.contains("pubads") ||
           lower.contains("safeframe") ||
           lower.contains("amazon-adsystem")
}

private fun isBlockedAd(url: String): Boolean {
    if (isYouTubeAd(url)) return true
    val parsed = runCatching { URI.create(url) }.getOrNull() ?: return false
    val host = parsed.host.orEmpty().lowercase(Locale.ROOT)
    val path = parsed.path.orEmpty()
    // YouTube host check: only non-ad YouTube requests pass through (ads caught above by isYouTubeAd)
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
        "streamtape", "dood", "voe", "mixdrop", "mxdrop", "filemoon", "upstream", "upvid",
        "streamwish", "mega4up", "govid", "player", "akwam", "arabseed", "fasel",
        "youtube", "googlevideo", "fastly", "cloudflare", "bunny", "ok.ru", "mail.ru",
        "arabhd", "uqload", "luluv", "bysevepoin", "dhtpre", "doood", "iplayer",
        "makeup", "97bf1", "s1q2105", "dsvplay", "vkvideo", "playmogo", "minochinos",
        "hgcloud", "turboviplay", "usersdrive", "gofile", "vidspeed", "forafile", "vidoba",
        "vidlo", "vidtube", "liiivideo", "hdup400", "hdup200", "vhdup", "vinovo",
        "upnshare", "updown", "firestream", "byse", "megamax", "mxcontent", "visitmycity"
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

    // مسارات بث HLS/DASH شائعة فقط لو مش صفحة ويب (مع استثناء بوابات JSON مثل /api/hls/movie/)
    if (!path.contains("/api/hls/movie/") && !path.contains("/api/hls/tv/") && (
        path.contains("/api/hls/stream/") ||
        path.contains("/hls/") || path.contains("/dash/") || path.contains("/stream/") ||
        path.contains("/video/") || path.contains("/videos/") || path.contains("/media/") ||
        path.contains("index.m3u8") || path.contains("master.m3u8") || path.contains("playlist.m3u8") ||
        path.contains("video.mp4") || path.contains("manifest.mpd")
    )) {
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
private data class ExtractedDirectStream(
    val streamUrl: String,
    val referer: String,
)

private fun normalizeExtractedUrl(raw: String, baseUrl: String): String {
    val clean = raw.replace("&amp;", "&").replace("\\/", "/").trim()
    if (clean.isBlank()) return ""
    if (clean.startsWith("//")) return "https:$clean"
    if (clean.startsWith("http://", true) || clean.startsWith("https://", true)) return clean
    return runCatching { URI.create(baseUrl).resolve(clean).toString() }.getOrDefault("")
}

private fun unpackDeanEdwards(html: String): String {
    val packedRegex = Regex(
        """eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[dr]\s*\)\s*\{.*?\}\s*\(\s*(['"])((?:\\.|[^\\])*?)\1\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(['"])((?:\\.|[^\\])*?)\5\.split\s*\(\s*['"]\|['"]\s*\)""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    val sb = StringBuilder()
    for (match in packedRegex.findAll(html)) {
        val rawPayload = match.groupValues[2]
            .replace("\\'", "'")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
        val radix = match.groupValues[3].toIntOrNull()?.coerceIn(2, 62) ?: 36
        val count = match.groupValues[4].toIntOrNull() ?: 0
        val symtab = match.groupValues[6].split('|')
        fun encodeRadix(c: Int): String {
            val prefix = if (c < radix) "" else encodeRadix(c / radix)
            val rem = c % radix
            val ch = if (rem > 35) (rem + 29).toChar().toString() else rem.toString(36)
            return prefix + ch
        }
        val dict = HashMap<String, String>(count)
        for (i in (count - 1) downTo 0) {
            val sym = symtab.getOrNull(i).orEmpty()
            dict[encodeRadix(i)] = sym.ifEmpty { encodeRadix(i) }
        }
        val unpacked = Regex("""\b\w+\b""").replace(rawPayload) { tok ->
            dict[tok.value] ?: tok.value
        }
        sb.append(unpacked).append('\n')
    }
    return sb.toString()
}

private fun decodeBase64UrlBytes(raw: String): ByteArray {
    val normalized = raw.trim().replace('-', '+').replace('_', '/')
    val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
    return Base64.decode(padded, Base64.DEFAULT)
}

private fun decodeByseStream(embedUrl: String, ua: String): ExtractedDirectStream? {
    val uri = runCatching { URI.create(embedUrl) }.getOrNull() ?: return null
    val scheme = uri.scheme?.takeIf(String::isNotBlank) ?: return null
    val host = uri.host?.takeIf(String::isNotBlank) ?: return null
    val origin = "$scheme://$host/"
    val code = embedUrl.substringBefore('?').trimEnd('/').substringAfterLast('/')
    if (code.length < 4) return null
    return runCatching {
        val conn = (URL("${origin}api/videos/$code").openConnection() as HttpURLConnection).apply {
            connectTimeout = 4500
            readTimeout = 4500
            setRequestProperty("User-Agent", ua)
            setRequestProperty("Referer", embedUrl)
            setRequestProperty("Accept", "application/json")
        }
        val apiJson = conn.inputStream.bufferedReader().use { it.readText() }
        val playback = JSONObject(apiJson).optJSONObject("playback") ?: return@runCatching null
        val keyParts = playback.optJSONArray("key_parts") ?: return@runCatching null
        val version = playback.optString("version").trim().toIntOrNull()
        val chosen = if (version != null && version in 1..20) {
            val i1 = version - 1
            val i2 = (31 - version) - 1
            if (i1 in 0 until keyParts.length() && i2 in 0 until keyParts.length()) {
                listOf(keyParts.optString(i1), keyParts.optString(i2))
            } else emptyList()
        } else emptyList()
        val parts = if (chosen.isNotEmpty()) chosen else (0 until keyParts.length()).map { keyParts.optString(it) }
        val keyBuf = ByteArrayOutputStream()
        for (p in parts) {
            if (p.isNotBlank()) keyBuf.write(decodeBase64UrlBytes(p))
        }
        val keyBytes = keyBuf.toByteArray()
        val ivBytes = decodeBase64UrlBytes(playback.optString("iv"))
        val cipherBytes = decodeBase64UrlBytes(playback.optString("payload"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, ivBytes))
        val plain = String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        val sources = JSONObject(plain).optJSONArray("sources") ?: return@runCatching null
        val streamUrl = (0 until sources.length()).mapNotNull { idx ->
            sources.optJSONObject(idx)?.optString("url")?.replace("\\/", "/")?.trim()
                ?.takeIf { it.startsWith("http", true) }
        }.firstOrNull() ?: return@runCatching null
        ExtractedDirectStream(streamUrl, origin)
    }.getOrNull()
}

private fun decodeVoeHtmlStream(html: String): String? {
    val jsonBlock = Regex(
        """<script[^>]+type=["']application/json["'][^>]*>\s*\[\s*"([^"]+)"\s*\]\s*</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).find(html)?.groupValues?.get(1) ?: return null
    return runCatching {
        val rot13 = buildString(jsonBlock.length) {
            for (ch in jsonBlock) {
                when (ch) {
                    in 'a'..'z' -> append((((ch - 'a') + 13) % 26 + 'a'.code).toChar())
                    in 'A'..'Z' -> append((((ch - 'A') + 13) % 26 + 'A'.code).toChar())
                    else -> append(ch)
                }
            }
        }
        val stripped = rot13
            .replace("@$", "_")
            .replace("^^", "_")
            .replace("~@", "_")
            .replace("#%", "_")
            .replace("*!", "_")
            .replace("!!", "_")
            .replace("%%", "_")
            .replace("_", "")
        fun padB64(s: String): String = s + "=".repeat((4 - (s.length % 4)) % 4)
        val decoded1 = String(Base64.decode(padB64(stripped), Base64.DEFAULT), Charsets.ISO_8859_1)
        val shifted = buildString(decoded1.length) {
            for (ch in decoded1) {
                append((ch.code - 3).toChar())
            }
        }
        val reversed = shifted.reversed()
        val decoded2 = String(Base64.decode(padB64(reversed), Base64.DEFAULT), Charsets.UTF_8)
        val obj = JSONObject(decoded2)
        val src = obj.optString("source").ifBlank { obj.optString("hls") }
        src.takeIf { it.startsWith("http", true) }
    }.getOrNull()
}

private fun extractDirectStreamWithReferer(
    urlStr: String,
    referer: String,
    ua: String,
    depth: Int = 0,
): ExtractedDirectStream? {
    if (depth > 3) return null
    try {
        if (isDirectStreamUrl(urlStr)) {
            return ExtractedDirectStream(urlStr, referer)
        }

        // 1. Vidara / Sugbaioug API
        if (urlStr.contains("vidara.") || urlStr.contains("sugbaioug.")) {
            val fileCode = urlStr.trimEnd('/').substringAfterLast('/').substringBefore('?')
            if (fileCode.isNotBlank()) {
                val conn = (URL("https://vidara.to/api/stream").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 4500
                    readTimeout = 4500
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
                    val clean = normalizeExtractedUrl(stream, urlStr)
                    if (clean.startsWith("http")) {
                        return ExtractedDirectStream(clean, "https://vidara.to/")
                    }
                }
            }
        }

        // 2. Fetch page HTML (following HTTP + cross-protocol redirects manually up to 4 hops)
        var currentUrl = urlStr
        var currentRef = referer
        var finalEmbedUrl = urlStr
        var responseCode = 0
        var html = ""
        for (hop in 0..3) {
            val hopOrigin = runCatching {
                val u = URI.create(currentUrl)
                "${u.scheme}://${u.host}/"
            }.getOrDefault(currentRef.ifBlank { "https://m.myseed.tv/" })
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", ua)
                setRequestProperty("Referer", currentRef.ifBlank { hopOrigin })
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }
            responseCode = conn.responseCode
            if (responseCode in 300..399) {
                val loc = conn.getHeaderField("Location")?.trim().orEmpty()
                conn.disconnect()
                if (loc.isBlank()) break
                val resolvedLoc = normalizeExtractedUrl(loc, currentUrl)
                if (resolvedLoc.isBlank() || resolvedLoc == currentUrl) break
                if (isDirectStreamUrl(resolvedLoc)) {
                    return ExtractedDirectStream(resolvedLoc, currentUrl)
                }
                currentRef = hopOrigin
                currentUrl = resolvedLoc
                finalEmbedUrl = resolvedLoc
                continue
            }
            if (responseCode == 403 || responseCode == 404 || responseCode == 410 || responseCode >= 500) {
                conn.disconnect()
                return null
            }
            finalEmbedUrl = conn.url?.toString()?.takeIf { it.startsWith("http") } ?: currentUrl
            html = conn.inputStream.bufferedReader().use { it.readText() }
            break
        }

        if (html.isBlank()) return null

        val origin = runCatching {
            val u = URI.create(finalEmbedUrl)
            "${u.scheme}://${u.host}/"
        }.getOrDefault(referer.ifBlank { "https://m.myseed.tv/" })

        if (html.trimStart().startsWith("#EXTM3U")) {
            return ExtractedDirectStream(finalEmbedUrl, origin)
        }
        // Shahid4U.si JSON HLS gatekeeper (`/api/hls/movie/...` -> `{"available":true,"url":"/api/hls/stream/..."}`)
        if (finalEmbedUrl.contains("/api/hls/") && html.trimStart().startsWith("{")) {
            val streamPath = runCatching { JSONObject(html).optString("url") }.getOrDefault("")
            if (streamPath.isNotBlank()) {
                val resolved = normalizeExtractedUrl(streamPath, finalEmbedUrl)
                if (resolved.startsWith("http")) return ExtractedDirectStream(resolved, origin)
            }
        }
        // Byse / Vidsharing AES-256-GCM SPA (`/api/videos/{code}`)
        val lowerFinal = finalEmbedUrl.lowercase(Locale.ROOT)
        if (lowerFinal.contains("byse") || lowerFinal.contains("vidsharing") || html.contains("Byse Frontend")) {
            decodeByseStream(finalEmbedUrl, ua)?.let { return it }
        }

        // Fast 404 / Cloudflare challenge check inside response body
        val headSnippet = html.take(4000)
        if (Regex("(?i)(404\\s*not\\s*found|video\\s*(?:not\\s*found|deleted|removed)|file\\s*(?:not\\s*found|deleted)|just a moment|attention required|cf-browser-verification)").containsMatchIn(headSnippet)) {
            return null
        }

        // JS redirect wrapper (e.g. voe.sx/e/... -> mirror domain)
        val jsRedirect = Regex(
            """(?:window\.location(?:\.href)?\s*=\s*|<meta[^>]+http-equiv=["']refresh["'][^>]+url=)['"]?(https?://[^'"\s>]+)['"]?""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1)
        if (!jsRedirect.isNullOrBlank() && jsRedirect != finalEmbedUrl && !isBlockedAd(jsRedirect)) {
            extractDirectStreamWithReferer(normalizeExtractedUrl(jsRedirect, finalEmbedUrl), origin, ua, depth + 1)?.let {
                return it
            }
        }

        // Unpack Dean Edwards eval(p,a,c,k,e) if present (Mixdrop, Vidtube, Earnvids, Filemoon, Streamwish, Minochinos, Liiivideo)
        val unpacked = if (html.contains("eval") && html.contains("function(p,a,c,k,e")) unpackDeanEdwards(html) else ""
        val searchScope = if (unpacked.isNotBlank()) "$html\n$unpacked" else html

        // MyCima-MY ArtPlayer direct MP4 (`const videoUrl = "...mp4"`)
        Regex("""videoUrl\s*=\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(searchScope)?.groupValues?.get(1)?.let { raw ->
                val clean = normalizeExtractedUrl(raw.replace("\\/", "/"), finalEmbedUrl)
                if (clean.startsWith("http") && !isBlockedAd(clean)) return ExtractedDirectStream(clean, origin)
            }

        // XFileSharing unpacked `var links={"hls4":"...","hls2":"...","hls3":"..."}` (Minochinos, Callistanise, Fastvid, Earnvids)
        val xfsUrls = Regex("""["']hls[234]?["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(searchScope).mapNotNull { m ->
                val clean = normalizeExtractedUrl(m.groupValues[1].replace("\\/", "/"), finalEmbedUrl)
                clean.takeIf { it.startsWith("http") && !isBlockedAd(it) }
            }.toList()
        (xfsUrls.firstOrNull { it.contains(".m3u8", true) } ?: xfsUrls.firstOrNull())?.let {
            return ExtractedDirectStream(it, origin)
        }

        // 2a. VOE MKGMa encoded JSON or direct HLS
        decodeVoeHtmlStream(searchScope)?.let { voeStream ->
            return ExtractedDirectStream(voeStream, origin)
        }
        val voeMatch = Regex("""['"]hls['"]\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(searchScope)
            ?: Regex("""['"](https?://[a-zA-Z0-9.-]+\.voe-network\.[a-z]+/[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(searchScope)
        voeMatch?.groupValues?.get(1)?.let {
            return ExtractedDirectStream(normalizeExtractedUrl(it, finalEmbedUrl), origin)
        }

        // 2b. Mixdrop / Mxdrop unpacked MDCore.wurl
        val mixdropMatch = Regex("""MDCore\.wurl\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(searchScope)
        mixdropMatch?.groupValues?.get(1)?.let { raw ->
            val stream = normalizeExtractedUrl(raw, finalEmbedUrl)
            if (stream.startsWith("http")) {
                return ExtractedDirectStream(stream, origin)
            }
        }

        // 2c. Dood / Playmogo / D000d / Myvidplay / Doply / Ds2play pass_md5
        val passMd5Match = Regex("""['"](/pass_md5/[^'"]+)['"]""").find(searchScope)
        if (passMd5Match != null) {
            val passPath = passMd5Match.groupValues[1]
            val passUrl = URI.create(finalEmbedUrl).resolve(passPath).toString()
            val token = passPath.trimEnd('/').substringAfterLast('/')
            val passConn = (URL(passUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", ua)
                setRequestProperty("Referer", finalEmbedUrl)
            }
            if (passConn.responseCode == 200) {
                val baseStream = passConn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (baseStream.startsWith("http")) {
                    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                    val rnd = (1..10).map { chars.random() }.joinToString("")
                    val fullDood = "$baseStream$rnd?token=$token&expiry=${System.currentTimeMillis()}"
                    return ExtractedDirectStream(fullDood, origin)
                }
            } else {
                passConn.disconnect()
            }
        }

        // 2d. Prefer an adaptive HLS manifest over a progressive MP4 in <source> tags
        val sourcePattern = Regex(
            """<source\s+[^>]*src=["']([^"']+\.(?:mp4|m3u8)(?:\?[^"']*)?)["']""",
            RegexOption.IGNORE_CASE,
        )
        val sourceCandidates = sourcePattern.findAll(searchScope).mapNotNull {
            normalizeExtractedUrl(it.groupValues.getOrNull(1).orEmpty(), finalEmbedUrl).takeIf(String::isNotBlank)
        }.toList()
        (sourceCandidates.firstOrNull { it.substringBefore('?').endsWith(".m3u8", true) }
            ?: sourceCandidates.firstOrNull())?.let {
            return ExtractedDirectStream(it, origin)
        }

        // 2e. Vidtube / Vidmoly / JWPlayer sources
        val vidmolyMatch = Regex("""(?:sources|file)\s*:\s*\[?\s*\{?\s*['"]?file['"]?\s*:\s*['"]((?:https?:)?//[^'"]+\.(?:m3u8|mp4)[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(searchScope)
            ?: Regex("""['"](https?://[a-zA-Z0-9.-]+\.vidmoly\.[a-z]+/[^'"]+\.m3u8[^'"]*)['"]""", RegexOption.IGNORE_CASE).find(searchScope)
        vidmolyMatch?.groupValues?.get(1)?.let {
            return ExtractedDirectStream(normalizeExtractedUrl(it, finalEmbedUrl), origin)
        }

        // 2f. Uqload / generic mp4 array
        val uqMatch = Regex("""sources\s*:\s*\[\s*["']((?:https?:)?//[^"']+\.mp4[^"']*)["']""").find(searchScope)
        uqMatch?.groupValues?.get(1)?.let {
            return ExtractedDirectStream(normalizeExtractedUrl(it, finalEmbedUrl), origin)
        }

        // 2g. General m3u8 / mp4 manifest in scripts (including unpacked JS)
        val m3u8Match = Regex("""["']((?:https?:)?//[^\s"'<>]+\.m3u8[^\s"'<>]*)["']""", RegexOption.IGNORE_CASE).find(searchScope)
        m3u8Match?.groupValues?.get(1)?.let {
            val clean = normalizeExtractedUrl(it, finalEmbedUrl)
            if (clean.startsWith("http") && !isBlockedAd(clean)) {
                return ExtractedDirectStream(clean, origin)
            }
        }
        val mp4Match = Regex("""["']((?:https?:)?//[^\s"'<>]+\.mp4(?:\?[^\s"'<>]*)?)["']""", RegexOption.IGNORE_CASE).find(unpacked)
        mp4Match?.groupValues?.get(1)?.let {
            val clean = normalizeExtractedUrl(it, finalEmbedUrl)
            if (clean.startsWith("http") && !isBlockedAd(clean)) {
                return ExtractedDirectStream(clean, origin)
            }
        }

        // 2h. Visible iframe check (e.g. govid -> voe.sx, skipping hidden 0x0 tracking iframes)
        Regex("""<iframe\b([^>]+)>""", RegexOption.IGNORE_CASE).findAll(html).forEach { ifrTag ->
            val attrs = ifrTag.groupValues[1]
            val lowerAttrs = attrs.lowercase(Locale.ROOT)
            val isHidden = lowerAttrs.contains("display:none") || lowerAttrs.contains("display: none") ||
                lowerAttrs.contains("visibility:hidden") ||
                Regex("""\b(?:width|height)=["']?0["']?""").containsMatchIn(lowerAttrs)
            if (isHidden) return@forEach
            val ifrUrl = Regex("""\bsrc=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(attrs)?.groupValues?.get(1)?.trim().orEmpty()
            val cleanIfr = normalizeExtractedUrl(ifrUrl, finalEmbedUrl)
            if (cleanIfr.startsWith("http") && !isBlockedAd(cleanIfr) &&
                !cleanIfr.contains("mycima.help", true) && cleanIfr != finalEmbedUrl
            ) {
                extractDirectStreamWithReferer(cleanIfr, finalEmbedUrl, ua, depth + 1)?.let { return it }
            }
        }
    } catch (_: Exception) {}
    return null
}

private fun extractDirectStream(urlStr: String, referer: String, ua: String): String? =
    extractDirectStreamWithReferer(urlStr, referer, ua)?.streamUrl

@Composable
private fun CreativeLoadingScreen(
    title: String,
    elapsedSeconds: Long,
    backupCount: Int,
    onSwitchBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "creative-loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loader-rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loader-pulse"
    )

    val statusText = when {
        elapsedSeconds < 3L -> "جاري الاتصال بأسرع سيرفر متوفر... ⚡"
        elapsedSeconds < 7L -> "جاري تهيئة البث والاتصال بالسيرفر... 🎬"
        elapsedSeconds < 12L -> "جاري فك تشفير وتجهيز أول مشهد بجودة فائقة... 🍿"
        else -> "لحظات ويبدأ العرض بأعلى جودة... ✨"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1428),
                        Color(0xFF0F0B17),
                        Color(0xFF06040A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x40D4AF37), Color(0x10D4AF37), Color.Transparent)
                            )
                        )
                )

                CircularProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(rotation),
                    color = Color(0xFFD4AF37),
                    trackColor = Color(0x22D4AF37),
                    strokeWidth = 3.5.dp
                )

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF191224))
                        .border(1.dp, Color(0x66D4AF37), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎬", fontSize = 24.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33D4AF37))
                    .border(1.dp, Color(0x44D4AF37), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusText,
                    color = Color(0xFFFFE082),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "جاري التحضير: ${formatTime(elapsedSeconds)}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal
            )

            if (backupCount > 0 && elapsedSeconds >= 4L) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onSwitchBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("⚡ جرب سيرفر آخر ($backupCount)", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun handleInAppAdWebNavigation(view: WebView?, rawUrl: String): Boolean {
    val reqUrl = rawUrl.trim()
    if (reqUrl.isEmpty()) return true
    if (reqUrl.startsWith("http://", ignoreCase = true) || reqUrl.startsWith("https://", ignoreCase = true)) {
        // Let WebView handle standard HTTP/HTTPS redirects naturally so Referer/cookies stay intact
        return false
    }
    if (reqUrl.startsWith("intent://", ignoreCase = true)) {
        val marker = "S.browser_fallback_url="
        val idx = reqUrl.indexOf(marker)
        if (idx >= 0) {
            val encoded = reqUrl.substring(idx + marker.length).substringBefore(';')
            val fallback = Uri.decode(encoded).trim()
            if (fallback.startsWith("http://", ignoreCase = true) || fallback.startsWith("https://", ignoreCase = true)) {
                view?.loadUrl(fallback)
                return true
            }
        }
    }
    // Keep user inside the in-app WebView; swallow Play Store / market / deep links
    return true
}

private fun isWebViewVisuallyPainted(webView: WebView, sampleBitmap: Bitmap): Boolean {
    return runCatching {
        if (webView.width < 32 || webView.height < 32) return@runCatching false
        sampleBitmap.eraseColor(AndroidColor.BLACK)
        val canvas = Canvas(sampleBitmap)
        canvas.scale(sampleBitmap.width.toFloat() / webView.width, sampleBitmap.height.toFloat() / webView.height)
        webView.draw(canvas)

        val corner = sampleBitmap.getPixel(0, 0)
        val cR = AndroidColor.red(corner)
        val cG = AndroidColor.green(corner)
        val cB = AndroidColor.blue(corner)
        var diffPixels = 0
        for (y in 0 until sampleBitmap.height) {
            for (x in 0 until sampleBitmap.width) {
                val px = sampleBitmap.getPixel(x, y)
                val dr = kotlin.math.abs(AndroidColor.red(px) - cR)
                val dg = kotlin.math.abs(AndroidColor.green(px) - cG)
                val db = kotlin.math.abs(AndroidColor.blue(px) - cB)
                if (dr + dg + db >= 28) {
                    diffPixels++
                    if (diffPixels >= 14) return@runCatching true
                }
            }
        }
        false
    }.getOrDefault(false)
}

@Composable
private fun PreRollAdOverlay(
    adHtml: String,
    adUrl: String,
    skipDelaySeconds: Int,
    onSkipAd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDelay = skipDelaySeconds.coerceAtLeast(1)
    var countdown by remember(adUrl, adHtml, totalDelay) { mutableIntStateOf(totalDelay) }
    var canSkip by remember(adUrl, adHtml) { mutableStateOf(false) }
    var adPageFinished by remember(adUrl, adHtml) { mutableStateOf(false) }
    var hasCommittedVisual by remember(adUrl, adHtml) { mutableStateOf(false) }
    var lastPageStartMs by remember(adUrl, adHtml) { mutableLongStateOf(System.currentTimeMillis()) }
    var lastPageFinishMs by remember(adUrl, adHtml) { mutableLongStateOf(0L) }
    var adVisualReady by remember(adUrl, adHtml) { mutableStateOf(false) }
    var adLoadFailed by remember(adUrl, adHtml) { mutableStateOf(false) }
    var userInteracted by remember(adUrl, adHtml) { mutableStateOf(false) }
    var adWebView by remember { mutableStateOf<WebView?>(null) }
    val sampleBitmap = remember { Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888) }
    DisposableEffect(sampleBitmap) {
        onDispose {
            if (!sampleBitmap.isRecycled) sampleBitmap.recycle()
        }
    }

    // Wait strictly until redirect trampolines finish AND real ad visuals render on screen before starting countdown
    LaunchedEffect(adWebView, adLoadFailed) {
        if (adVisualReady) return@LaunchedEffect
        if (adLoadFailed) {
            kotlinx.coroutines.delay(250)
            onSkipAd()
            return@LaunchedEffect
        }
        val wv = adWebView ?: return@LaunchedEffect
        var domVisible = false
        var elapsed = 0L
        while (!adVisualReady && !adLoadFailed && elapsed < 14_000L) {
            val nowMs = System.currentTimeMillis()
            val settledAfterNav = (nowMs - lastPageStartMs) >= 350L
            if (settledAfterNav && (adPageFinished || hasCommittedVisual || adHtml.isNotBlank())) {
                wv.evaluateJavascript(
                    """(function(){
                        if (!document || !document.body) return 'loading';
                        if (document.readyState !== 'complete') return 'loading';
                        var textLen = (document.body.innerText || '').trim().length;
                        if (document.getElementById('submit-form') && textLen < 20) return 'redirect';
                        var w = window.innerWidth || document.documentElement.clientWidth || 0;
                        var h = window.innerHeight || document.documentElement.clientHeight || 0;
                        if (w <= 0 || h <= 0) return 'loading';
                        if (textLen >= 30) return 'visible';
                        var els = document.querySelectorAll('img,iframe,video,canvas,svg,button,a,input,div,section');
                        for (var i = 0; i < els.length && i < 60; i++) {
                            var el = els[i];
                            var st = window.getComputedStyle(el);
                            if (!st || st.display === 'none' || st.visibility === 'hidden' || parseFloat(st.opacity || '1') <= 0.05) continue;
                            var r = el.getBoundingClientRect();
                            if (r.width >= 32 && r.height >= 24 && r.bottom > 0 && r.right > 0 && r.top < h && r.left < w) {
                                var tag = el.tagName;
                                if (tag === 'IMG' || tag === 'IFRAME' || tag === 'VIDEO' || tag === 'CANVAS' || tag === 'SVG' || tag === 'BUTTON' || tag === 'A') return 'visible';
                                if ((st.backgroundImage && st.backgroundImage !== 'none') || ((el.innerText || '').trim().length >= 8)) return 'visible';
                            }
                        }
                        return 'blank';
                    })();""".trimIndent()
                ) { raw ->
                    val state = raw?.trim()?.trim('"') ?: "loading"
                    domVisible = (state == "visible")
                }

                val pixelsPainted = isWebViewVisuallyPainted(wv, sampleBitmap)
                val elapsedAfterFinish = if (lastPageFinishMs > 0L) (nowMs - lastPageFinishMs) else 0L
                if ((pixelsPainted && domVisible) ||
                    (adPageFinished && domVisible && elapsedAfterFinish >= 1_500L)
                ) {
                    adVisualReady = true
                    break
                }
            }
            kotlinx.coroutines.delay(200)
            elapsed += 200L
        }
        if (!adVisualReady) {
            // Ad failed to paint any visible content within timeout — proceed cleanly instead of wasting countdown on a black screen
            onSkipAd()
        }
    }

    // Start countdown strictly AFTER the ad screen visually appears to the user
    LaunchedEffect(adVisualReady, totalDelay) {
        if (!adVisualReady) return@LaunchedEffect
        countdown = totalDelay
        for (sec in totalDelay downTo 1) {
            countdown = sec
            kotlinx.coroutines.delay(1000)
        }
        countdown = 0
        canSkip = true
        if (!userInteracted) {
            onSkipAd()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    adWebView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = MOBILE_CHROME_UA
                    }
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setOnTouchListener { _, event ->
                        if (event?.action == android.view.MotionEvent.ACTION_DOWN) {
                            userInteracted = true
                        }
                        false
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress >= 95 && !view?.url.isNullOrBlank() && view?.url != "about:blank") {
                                adPageFinished = true
                                if (lastPageFinishMs == 0L) lastPageFinishMs = System.currentTimeMillis()
                            }
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?
                        ): Boolean {
                            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                            val popupWebView = WebView(ctx).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(popup: WebView?, req: WebResourceRequest?): Boolean {
                                        val popupUrl = req?.url?.toString().orEmpty()
                                        if (popupUrl.startsWith("http://", true) || popupUrl.startsWith("https://", true)) {
                                            view?.loadUrl(popupUrl)
                                        } else {
                                            handleInAppAdWebNavigation(view, popupUrl)
                                        }
                                        return true
                                    }
                                }
                            }
                            transport.webView = popupWebView
                            resultMsg.sendToTarget()
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return handleInAppAdWebNavigation(view, request?.url?.toString().orEmpty())
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            val loaded = url.orEmpty().trim()
                            if (loaded.isNotEmpty() && loaded != "about:blank") {
                                adPageFinished = false
                                hasCommittedVisual = false
                                lastPageStartMs = System.currentTimeMillis()
                                lastPageFinishMs = 0L
                            }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            val loaded = url.orEmpty().trim()
                            if (loaded.isNotEmpty() && loaded != "about:blank") {
                                hasCommittedVisual = true
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val loaded = url.orEmpty().trim()
                            if (loaded.isNotEmpty() && loaded != "about:blank") {
                                adPageFinished = true
                                lastPageFinishMs = System.currentTimeMillis()
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                adLoadFailed = true
                            }
                        }
                    }
                    if (adUrl.isNotBlank()) {
                        loadUrl(adUrl)
                    } else if (adHtml.isNotBlank()) {
                        loadDataWithBaseURL("https://zaafilm.com", adHtml, "text/html", "UTF-8", null)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Bottom-Start: Subtle Sponsored Ad Badge (never covers top header UI of the ad) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 14.dp, start = 16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x99000000))
                .border(0.8.dp, Color(0x55D4AF37), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = "إعلان مدعوم",
                color = Color(0xFFFFE082).copy(alpha = 0.88f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Center-Right: Loading indicator BEFORE ad appears, then strict countdown ──
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            if (canSkip) {
                Button(
                    onClick = onSkipAd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
                    border = BorderStroke(2.dp, Color(0xFFFFE082))
                ) {
                    Text(
                        text = "تخطي الإعلان ⏩",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xE012151C))
                        .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFD4AF37),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = if (!adVisualReady) "جاري تحميل الإعلان..." else "تخطي بعد ( $countdown )",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppBrowserOverlay(
    url: String,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(200f)
            .background(Color(0xFF0E0B0E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color(0xFF161218))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF251D25))
                        .border(1.dp, Color(0xFFE5A958), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "إعلان مدعوم • Ad",
                        color = Color(0xFFE5A958),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // [✕ إغلاق] Button
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB94E3C)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "✕  إغلاق",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // In-app WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        setBackgroundColor(android.graphics.Color.BLACK)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return handleInAppAdWebNavigation(view, request?.url?.toString().orEmpty())
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
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
    isAdOverlayActive: Boolean = false,
    onOpenInAppAdUrl: (String) -> Unit = {},
    onSwitchStream: (String, String, String, Map<String, String>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("madar_player", 0) }
    var subtitlesEnabled by remember {
        mutableStateOf(preferences.getBoolean("subtitles_enabled", true))
    }
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
    var inAppBrowserUrl by remember { mutableStateOf<String?>(null) }

    // YouTube custom native player states
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var ytCurrentSec by remember(effectiveStreamUrl) { mutableFloatStateOf(0f) }
    var ytDurationSec by remember(effectiveStreamUrl) { mutableFloatStateOf(0f) }
    var ytSliderValue by remember(effectiveStreamUrl) { mutableStateOf<Float?>(null) }
    var ytIsPlaying by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var ytIsBuffering by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var ytEnded by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var ytAvailableQualities by remember(effectiveStreamUrl) { mutableStateOf<List<String>>(emptyList()) }
    var ytSelectedQuality by remember(effectiveStreamUrl) { mutableStateOf("auto") }
    var ytEmbedChecked by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var useWatchPage by remember(effectiveStreamUrl) {
        mutableStateOf(false) // false = clean IFrame API; true = clean mobile m.youtube.com watch mode
    }
    var ytSpeed by remember(effectiveStreamUrl) { mutableFloatStateOf(1.0f) }
    var ytDoubleTapSide by remember { mutableStateOf<String?>(null) }
    var ytDoubleTapKey by remember { mutableIntStateOf(0) }
    var showDoubleTapIndicator by remember { mutableStateOf(false) }

    val cleanCinemaJs = remember(subtitlesEnabled) {
        val hideSubsCss = if (!subtitlesEnabled) {
            " .ytp-caption-window-container, .caption-window, ytm-caption-window-renderer, .ytp-caption-segment, .caption-visual-line, div[class*=\"caption-window\"], div[class*=\"ytp-caption\"], .player-subtitles-content { display: none !important; visibility: hidden !important; opacity: 0 !important; }"
        } else ""
        """
            (function() {
                var adCss = '.ytp-ad-overlay-container, .ytp-ad-player-overlay, .ytp-ad-player-overlay-instream-info, ' +
                    '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-text, .ytp-ad-preview-container, ' +
                    '.ytm-promoted-sparkles-web-renderer, ytm-companion-ad-renderer, .ytp-suggested-action, ' +
                    'ytd-promoted-video-renderer, ytd-display-ad-renderer, .ytp-ad-action-interstitial, ' +
                    'ytd-action-companion-ad-renderer, .ytp-ad-image-overlay, .ytp-paid-content-overlay, ' +
                    '.ytp-ad-progress, .ytp-ad-progress-list, .ytm-skip-ad-button, .ytp-ad-message-container { ' +
                    'display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; width: 0 !important; height: 0 !important; } ';

                var ytChromeCss = '.ytp-chrome-top, .ytp-chrome-bottom, .ytp-gradient-top, .ytp-gradient-bottom, ' +
                    '.ytp-large-play-button, .ytp-cued-thumbnail-overlay, .ytp-pause-overlay, .ytp-ce-element, ' +
                    '.ytp-endscreen-content, .ytp-watermark, .ytp-show-cards-title, .ytp-unmute, .ytm-unmute, ' +
                    '[class*="unmute" i], .ytp-volume-area, .ytp-mute-button, ytm-mobile-topbar-renderer, ' +
                    'ytm-pivot-bar-renderer, ytm-item-section-renderer, ytm-comment-section-renderer, ' +
                    '.watch-below-the-player, ytm-reel-shelf-renderer, #related, #comments, #secondary, ' +
                    '#masthead-container, ytm-engagement-panel-section-list-renderer, ' +
                    'ytm-single-column-watch-next-results-renderer, ytm-upsell-dialog-renderer, ' +
                    '.fullscreen-icon, .player-controls-top, .player-controls-middle, .player-controls-bottom, ' +
                    '.player-controls-content, .ytm-player-controls-background { ' +
                    'display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; width: 0 !important; height: 0 !important; } ';

                var css = 'html, body { overflow: hidden !important; background: #000 !important; margin: 0 !important; padding: 0 !important; width: 100vw !important; height: 100vh !important; direction: ltr !important; } ' +
                    '.player-container, #player-container-id, #movie_player, .html5-video-player, .html5-video-container, #player { ' +
                    'position: fixed !important; inset: 0 !important; width: 100vw !important; height: 100vh !important; z-index: 999999 !important; background: #000 !important; margin: 0 !important; padding: 0 !important; } ' +
                    'video.html5-main-video, video { position: fixed !important; inset: 0 !important; margin: auto !important; width: 100vw !important; height: 100vh !important; max-width: 100vw !important; max-height: 100vh !important; object-fit: contain !important; background: #000 !important; } ' +
                    ytChromeCss + adCss + '$hideSubsCss';

                var style = document.getElementById('zag-yt-cinema-css');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'zag-yt-cinema-css';
                    (document.head || document.documentElement).appendChild(style);
                }
                style.textContent = css;

                window.__zagGetPlayer = function() {
                    var p = document.getElementById('movie_player');
                    if (p && typeof p.seekTo === 'function') return p;
                    var f = document.querySelector('.html5-video-player');
                    if (f && typeof f.seekTo === 'function') return f;
                    return null;
                };

                window.__zagPlay = function() {
                    var p = window.__zagGetPlayer();
                    var v = document.querySelector('video');
                    try {
                        if (p && typeof p.unMute === 'function') p.unMute();
                        if (v) { v.muted = false; v.volume = 1.0; }
                        if (p && typeof p.playVideo === 'function') { p.playVideo(); return; }
                    } catch(e) {}
                    if (v) { try { v.play(); } catch(e2) {} }
                };

                window.__zagPause = function() {
                    var p = window.__zagGetPlayer();
                    try { if (p && typeof p.pauseVideo === 'function') p.pauseVideo(); } catch(e) {}
                    var v = document.querySelector('video');
                    if (v && !v.paused) { try { v.pause(); } catch(e2) {} }
                };

                window.__zagSeekTo = function(sec) {
                    try {
                        var p = window.__zagGetPlayer();
                        if (p && typeof p.seekTo === 'function') {
                            p.seekTo(sec, true);
                            if (typeof p.playVideo === 'function') p.playVideo();
                            return;
                        }
                    } catch(e) {}
                    var v = document.querySelector('video');
                    if (v) {
                        try { v.currentTime = sec; v.play(); } catch(e2) {}
                    }
                };

                window.__zagSetSpeed = function(rate) {
                    var p = window.__zagGetPlayer();
                    if (p && typeof p.setPlaybackRate === 'function') { try { p.setPlaybackRate(rate); } catch(e) {} }
                    var v = document.querySelector('video');
                    if (v) { try { v.playbackRate = rate; } catch(e2) {} }
                };

                window.__zagSetQuality = function(q) {
                    var p = window.__zagGetPlayer();
                    if (!p) return;
                    try {
                        if (typeof p.setPlaybackQualityRange === 'function') p.setPlaybackQualityRange(q, q);
                        if (typeof p.setPlaybackQuality === 'function') p.setPlaybackQuality(q);
                    } catch(e) {}
                };

                function reportYtQualities() {
                    try {
                        var p = window.__zagGetPlayer();
                        if (p && window.MadarBridge && window.MadarBridge.onQualityLevels && typeof p.getAvailableQualityLevels === 'function') {
                            var levels = p.getAvailableQualityLevels() || [];
                            var cur = (typeof p.getPlaybackQuality === 'function') ? p.getPlaybackQuality() : '';
                            if (levels.length) window.MadarBridge.onQualityLevels(JSON.stringify(levels), cur || '');
                        }
                    } catch(e) {}
                }

                // ── Safe Active-Only Ad Skipper (NEVER match static .video-ads shell) ──
                function purgeYouTubeAds() {
                    var skipButtons = document.querySelectorAll(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .videoAdUiSkipButton, button.ytp-ad-skip-button-text, .ytp-ad-overlay-close-button, .ytm-skip-ad-button'
                    );
                    for (var i = 0; i < skipButtons.length; i++) {
                        try { skipButtons[i].click(); } catch(e) {}
                    }

                    var p = window.__zagGetPlayer() || document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                    var hasAdClass = !!(p && (p.classList.contains('ad-showing') || p.classList.contains('ad-interrupting')));
                    var hasAdUi = !!document.querySelector('.ytp-ad-player-overlay-instream-info, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-duration-remaining');
                    var v = document.querySelector('video');
                    if (hasAdClass && hasAdUi && v && isFinite(v.duration) && v.duration > 0 && v.duration < 180) {
                        try {
                            v.muted = true;
                            v.currentTime = v.duration;
                        } catch(e) {}
                    } else if (v && !hasAdClass && v.playbackRate > 2.5) {
                        v.playbackRate = 1.0;
                        v.muted = false;
                    }
                }

                purgeYouTubeAds();
                if (!window.__zagAdKillerRunning) {
                    window.__zagAdKillerRunning = true;
                    setInterval(purgeYouTubeAds, 200);
                    setInterval(function() {
                        reportYtQualities();
                        var v2 = document.querySelector('video');
                        if (v2 && window.MadarBridge) {
                            var p2 = window.__zagGetPlayer() || document.getElementById('movie_player');
                            var inAd = !!(p2 && (p2.classList.contains('ad-showing') || p2.classList.contains('ad-interrupting')));
                            if (!inAd) {
                                window.MadarBridge.onTimeUpdate(v2.currentTime || 0, v2.duration || 0);
                                if (v2.readyState >= 2 && !v2.paused && !v2.ended && v2.currentTime > 0.15) {
                                    window.MadarBridge.onStateChange('playing');
                                }
                            }
                        }
                    }, 350);
                }

                // Dismiss consent / sign-in / app upsell prompts cleanly if any appear on m.youtube.com
                try {
                    var dismissBtns = document.querySelectorAll('ytm-upsell-dialog-renderer button, .upsell-dialog-dismiss-button, tp-yt-paper-dialog button[aria-label*="No" i], tp-yt-paper-dialog button[aria-label*="Dismiss" i]');
                    for (var d = 0; d < dismissBtns.length; d++) { try { dismissBtns[d].click(); } catch(e) {} }
                } catch(e) {}

                var v = document.querySelector('video');
                if (v) {
                    v.muted = false;
                    v.volume = 1.0;
                    if (v.paused && !window.__zagPausedForAd) {
                        try {
                            var pl = v.play();
                            if (pl !== undefined) {
                                pl.catch(function() {
                                    v.muted = true;
                                    v.play().then(function() {
                                        setTimeout(function() { v.muted = false; }, 120);
                                    }).catch(function(){});
                                });
                            }
                        } catch(e) {}
                    }
                    if (v.textTracks) {
                        for (var i = 0; i < v.textTracks.length; i++) {
                            try {
                                v.textTracks[i].mode = $subtitlesEnabled ? 'showing' : 'disabled';
                            } catch(e) {}
                        }
                    }
                    if (!v.__madar_hooked) {
                        v.__madar_hooked = true;
                        v.addEventListener('playing', function() {
                            if (window.MadarBridge) window.MadarBridge.onStateChange('playing');
                        });
                        v.addEventListener('canplay', function() {
                            if (window.MadarBridge && !v.paused) window.MadarBridge.onStateChange('playing');
                        });
                        v.addEventListener('seeked', function() {
                            if (window.MadarBridge) window.MadarBridge.onStateChange('playing');
                        });
                        v.addEventListener('seeking', function() {
                            if (window.MadarBridge) window.MadarBridge.onStateChange('buffering');
                        });
                        v.addEventListener('pause', function() {
                            if (window.MadarBridge) window.MadarBridge.onStateChange('paused');
                        });
                        v.addEventListener('waiting', function() {
                            if (window.MadarBridge) window.MadarBridge.onStateChange('buffering');
                        });
                        v.addEventListener('timeupdate', function() {
                            if (window.MadarBridge) window.MadarBridge.onTimeUpdate(v.currentTime || 0, v.duration || 0);
                        });
                        v.addEventListener('ended', function() {
                            if (window.MadarBridge) window.MadarBridge.onStateChange('ended');
                        });
                    }
                }
            })();
        """.trimIndent()
    }

    LaunchedEffect(ytDoubleTapKey) {
        if (ytDoubleTapKey > 0) {
            showDoubleTapIndicator = true
            kotlinx.coroutines.delay(650)
            showDoubleTapIndicator = false
        }
    }

    // Pre-flight check: decide IFrame vs mobile Watch in <1.8s with zero wasted 7s Error 150 wait!
    LaunchedEffect(isYouTube, effectiveStreamUrl, webViewRef, exoRetryKey) {
        if (!isYouTube) return@LaunchedEffect
        val ytId = extractYouTubeId(effectiveStreamUrl)
        val wv = webViewRef ?: return@LaunchedEffect
        if (ytId.isBlank()) {
            playbackError = "رابط فيديو YouTube غير صالح."
            isBuffering = false
            return@LaunchedEffect
        }
        val embeddable = withContext(Dispatchers.IO) { isYouTubeVideoEmbeddable(ytId) }
        useWatchPage = !embeddable
        ytEmbedChecked = true
        if (embeddable) {
            val html = buildYouTubeIFrameHtml(ytId, subtitlesEnabled, pausedForAd = isAdOverlayActive)
            wv.loadDataWithBaseURL(YOUTUBE_EMBED_ORIGIN, html, "text/html", "UTF-8", null)
        } else {
            wv.settings.userAgentString = MOBILE_CHROME_UA
            wv.evaluateJavascript("window.__zagPausedForAd = ${isAdOverlayActive};", null)
            wv.loadUrl("https://m.youtube.com/watch?v=$ytId&app=m")
        }
    }

    // Pause YouTube video cleanly while PreRoll Ad is showing; resume immediately on ad skip!
    LaunchedEffect(isYouTube, isAdOverlayActive, webViewRef, ytEmbedChecked) {
        if (!isYouTube) return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        if (isAdOverlayActive) {
            wv.evaluateJavascript("window.__zagPausedForAd = true; if (window.__zagPause) window.__zagPause();", null)
        } else if (ytEmbedChecked) {
            wv.evaluateJavascript("window.__zagPausedForAd = false; if (window.__zagPlay) window.__zagPlay();", null)
        }
    }

    // Fallback: if IFrame fails to start after 7s AFTER PreRoll ad closes, switch cleanly to m.youtube.com watch
    LaunchedEffect(isYouTube, effectiveStreamUrl, ytIsPlaying, useWatchPage, ytEmbedChecked, isAdOverlayActive) {
        if (!isYouTube || !ytEmbedChecked || useWatchPage || isAdOverlayActive) return@LaunchedEffect
        kotlinx.coroutines.delay(7000)
        if (!ytIsPlaying) {
            Log.w("MadarPlayer", "YT IFrame not playing after 7s -> switching to mobile watch mode")
            useWatchPage = true
            val ytId = extractYouTubeId(effectiveStreamUrl)
            if (ytId.isNotBlank()) {
                webViewRef?.settings?.userAgentString = MOBILE_CHROME_UA
                webViewRef?.loadUrl("https://m.youtube.com/watch?v=$ytId&app=m")
            }
        }
    }

    // Watch-page cinema CSS + ad cleanup loop
    LaunchedEffect(isYouTube, effectiveStreamUrl, webViewRef, useWatchPage, ytEmbedChecked) {
        if (!isYouTube || !ytEmbedChecked || !useWatchPage) return@LaunchedEffect
        for (i in 1..20) {
            kotlinx.coroutines.delay(400)
            webViewRef?.evaluateJavascript(cleanCinemaJs, null)
        }
    }

    LaunchedEffect(subtitlesEnabled, webViewRef) {
        val js = """
            (function(enable) {
                var existingStyle = document.getElementById('zag-hide-subtitles-css');
                if (!enable) {
                    if (!existingStyle) {
                        var s = document.createElement('style');
                        s.id = 'zag-hide-subtitles-css';
                        s.textContent = '.ytp-caption-window-container, .caption-window, ytm-caption-window-renderer, .ytp-caption-segment, .caption-visual-line, div[class*="caption-window"], div[class*="ytp-caption"], .player-subtitles-content { display: none !important; visibility: hidden !important; opacity: 0 !important; }';
                        (document.head || document.documentElement).appendChild(s);
                    }
                } else {
                    if (existingStyle && existingStyle.parentNode) {
                        existingStyle.parentNode.removeChild(existingStyle);
                    }
                }
                var v = document.querySelector('video');
                if (v && v.textTracks) {
                    for (var i = 0; i < v.textTracks.length; i++) {
                        try {
                            v.textTracks[i].mode = enable ? 'showing' : 'disabled';
                        } catch(e) {}
                    }
                }
                var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                if (p) {
                    try {
                        if (!enable) {
                            if (typeof p.unloadModule === 'function') p.unloadModule('captions');
                            if (typeof p.setOption === 'function') p.setOption('captions', 'track', {});
                        } else {
                            if (typeof p.loadModule === 'function') p.loadModule('captions');
                            if (typeof p.setOption === 'function') p.setOption('captions', 'track', {'languageCode': 'ar'});
                            if (typeof p.toggleSubtitlesOn === 'function') p.toggleSubtitlesOn();
                        }
                    } catch(e) {}
                }
                var ccBtn = document.querySelector('button[aria-label*="subtitles" i], button[aria-label*="captions" i], .ytp-subtitles-button');
                if (ccBtn) {
                    var isPressed = ccBtn.getAttribute('aria-pressed') === 'true';
                    if (enable !== isPressed) {
                        ccBtn.click();
                    }
                }
            })($subtitlesEnabled);
        """.trimIndent()
        webViewRef?.evaluateJavascript(js, null)
    }

    val ytSeekBy: (Float) -> Unit = remember(webViewRef) {
        { deltaSeconds ->
            val js = """
                (function() {
                    var p = window.__zagGetPlayer ? window.__zagGetPlayer() : (document.getElementById('movie_player') || document.querySelector('.html5-video-player'));
                    if (p && typeof p.seekTo === 'function') {
                        var cur = (typeof p.getCurrentTime === 'function' ? p.getCurrentTime() : 0) || 0;
                        var dur = (typeof p.getDuration === 'function' ? p.getDuration() : 999999) || 999999;
                        var target = Math.max(0, Math.min(cur + ($deltaSeconds), dur));
                        p.seekTo(target, true);
                        if (typeof p.playVideo === 'function') p.playVideo();
                    } else if (window.__zagSeekTo) {
                        var v = document.querySelector('video');
                        var cur = v ? (v.currentTime || 0) : 0;
                        window.__zagSeekTo(cur + ($deltaSeconds));
                    } else {
                        var v = document.querySelector('video');
                        if (v) {
                            var target = Math.max(0, Math.min((v.currentTime || 0) + ($deltaSeconds), v.duration || 999999));
                            v.currentTime = target;
                            v.play();
                        }
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
    } catch (e: Exception) { "" }

    // ── Fast Real-Time Background Stream Extractor ─────────────────────────
    LaunchedEffect(effectiveStreamUrl) {
        if (!isDirectStreamUrl(effectiveStreamUrl) && !isYouTubeStream(effectiveStreamUrl)) {
            withContext(Dispatchers.IO) {
                val resolved = extractDirectStreamWithReferer(effectiveStreamUrl, cleanRef, cleanUa)
                if (resolved != null && resolved.streamUrl.isNotBlank() && resolved.streamUrl != effectiveStreamUrl) {
                    withContext(Dispatchers.Main) {
                        // Use the resolved embed origin/page Referer so hotlink-protected CDNs accept ExoPlayer.
                        onSwitchStream(
                            resolved.streamUrl,
                            cleanUa,
                            resolved.referer.ifBlank { effectiveStreamUrl.ifBlank { cleanRef } },
                            streamHeaders,
                        )
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
    var selectedQuality by remember {
        mutableStateOf(preferences.getString("quality", "أعلى جودة") ?: "أعلى جودة")
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
    var webViewInitialPlaybackWaitMs by remember(effectiveStreamUrl) { mutableLongStateOf(0L) }
    var bufferingFallbackUsed by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var hasStartedPlaying by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var isExoPlaying by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var pauseAdDismissed by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var resumeAfterAd by remember(effectiveStreamUrl) { mutableStateOf(true) }
    var wasAdOverlayActive by remember(effectiveStreamUrl) { mutableStateOf(isAdOverlayActive) }
    var liveRetryAttempted by remember(effectiveStreamUrl) { mutableStateOf(false) }
    var chunklessPreparationEnabled by remember(effectiveStreamUrl) { mutableStateOf(true) }
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

    LaunchedEffect(subtitlesEnabled, trackSelector) {
        val params = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
        trackSelector.setParameters(params.build())
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
                // Preserve explicit Origin, or derive from clean ASCII Referer; NEVER emit
                // a synthesized Origin with zero-width token chars that live CDNs reject.
                val inferredOrigin = originForHeader(cleanRef).takeIf { it.isNotBlank() && isAsciiHeaderValue(it) }
                val origin = sourceOrigin?.takeIf { it.isNotBlank() && isAsciiHeaderValue(it) } ?: inferredOrigin
                if (origin != null && isAsciiHeaderValue(cleanRef)) {
                    put("Origin", origin)
                } else if (sourceOrigin != null && isAsciiHeaderValue(sourceOrigin)) {
                    put("Origin", sourceOrigin)
                }
            }
        val httpDataSourceFactory = Utf8OkHttpDataSource.Factory(userAgent = cleanUa)
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
                .setAllowChunklessPreparation(chunklessPreparationEnabled)
                .createMediaSource(mediaItem)
        } else {
            DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_500,
                /* maxBufferMs = */ 25_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_200
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

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isExoPlaying = playing
                        if (playing) {
                            hasRenderedFirstFrame = true
                            hasStartedPlaying = true
                            pauseAdDismissed = false
                        }
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
                        Log.w("MadarPlayer", "ExoPlayer error host=${safeHost(streamUrl)} code=${error.errorCodeName} msg=${error.cause?.message}")
                        if (isHls && chunklessPreparationEnabled &&
                            (error.errorCodeName.contains("PARSING") ||
                                error.errorCodeName.contains("DECODER"))
                        ) {
                            Log.i("MadarPlayer", "Retrying HLS with segment-based preparation")
                            chunklessPreparationEnabled = false
                            exoRetryKey++
                            return
                        }
                        if (isLive && !liveRetryAttempted) {
                            liveRetryAttempted = true
                            exoRetryKey++
                            return
                        }
                        if (backupSources.isNotEmpty()) {
                            val directIdx = backupSources.indexOfFirst { isDirectStreamUrl(it.url) }
                            val next = backupSources.removeAt(if (directIdx >= 0) directIdx else 0)
                            liveRetryAttempted = false
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
                if (!isLive || !isAdOverlayActive) {
                    prepare()
                }
                playWhenReady = !isAdOverlayActive
            }
    }

    LaunchedEffect(player, isAdOverlayActive) {
        val p = player ?: return@LaunchedEffect
        if (isAdOverlayActive && !wasAdOverlayActive) {
            resumeAfterAd = p.playWhenReady || !hasRenderedFirstFrame
            p.playWhenReady = false
        } else if (!isAdOverlayActive && wasAdOverlayActive) {
            if (isLive) {
                p.seekToDefaultPosition()
                p.prepare()
            }
            if (resumeAfterAd) {
                p.playWhenReady = true
            }
        }
        wasAdOverlayActive = isAdOverlayActive
    }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        var activePlayWaitMs = 0L
        while (true) {
            kotlinx.coroutines.delay(1000)
            val format = p.videoFormat
            val br = if (format != null && format.bitrate > 0) format.bitrate else if (format != null && format.averageBitrate > 0) format.averageBitrate else null
            if (br != null) currentBitrateKbps = br / 1000

            if (p.playWhenReady && !isAdOverlayActive) {
                activePlayWaitMs += 1000L
            }
            val elapsedMs = System.currentTimeMillis() - streamStartedMs
            val bufferingForMs = bufferingSinceMs.takeIf { it > 0L }
                ?.let { System.currentTimeMillis() - it } ?: 0L

            // ── Auto Black Screen Detection (initial active stall: 5s; stuck buffering during playback: 6s) ──
            val isBlackScreen = !hasRenderedFirstFrame && !isAdOverlayActive && p.playWhenReady && activePlayWaitMs >= 5_000L && p.currentPosition == 0L
            val isEndlessBuffering = !isAdOverlayActive && p.playWhenReady && p.playbackState == Player.STATE_BUFFERING && bufferingForMs >= 6_000L

            if (!bufferingFallbackUsed && (isBlackScreen || isEndlessBuffering)) {
                bufferingFallbackUsed = true
                Log.w("MadarPlayer", "Auto Black Screen / Buffer Recovery triggered (elapsed=${elapsedMs}ms, buffering=${bufferingForMs}ms). Backups left: ${backupSources.size}")
                val directIdx = backupSources.indexOfFirst { isDirectStreamUrl(it.url) }
                val canSwitch = backupSources.isNotEmpty() && (!hasRenderedFirstFrame || directIdx >= 0)
                if (canSwitch) {
                    val next = backupSources.removeAt(if (directIdx >= 0) directIdx else 0)
                    Log.i("MadarPlayer", "Switching to next backup: ${safeHost(next.url)}")
                    onSwitchStream(
                        next.url,
                        next.userAgent.ifBlank { cleanUa },
                        next.referer.ifBlank { cleanRef },
                        next.headers,
                    )
                } else if (!hasRenderedFirstFrame) {
                    playbackError = "تعذر تشغيل هذا السيرفر (شاشة سوداء). اضغط لإعادة المحاولة أو اختر سيرفرًا آخر."
                    isBuffering = false
                }
            }
        }
    }

    // A WebView embed can fail before onPageStarted/onReceivedError fires.
    // In that case the normal page watchdog never begins and the creative
    // loader can remain on screen indefinitely. Bound that silent state and
    // move to the next verified source, or show the retry UI when none remain.
    LaunchedEffect(effectiveStreamUrl, useWebViewFallback, isAdOverlayActive) {
        if (!useWebViewFallback) return@LaunchedEffect
        while (!hasRenderedFirstFrame && playbackError == null) {
            kotlinx.coroutines.delay(500)
            if (!isAdOverlayActive) webViewInitialPlaybackWaitMs += 500L
            if (webViewInitialPlaybackWaitMs < 12_000L || bufferingFallbackUsed) continue

            bufferingFallbackUsed = true
            if (backupSources.isNotEmpty()) {
                val directIdx = backupSources.indexOfFirst { isDirectStreamUrl(it.url) }
                val currentHost = safeHost(effectiveStreamUrl)
                val differentHostIdx = backupSources.indexOfFirst {
                    val candidateHost = safeHost(it.url)
                    candidateHost.isNotBlank() && candidateHost != currentHost
                }
                val nextIdx = when {
                    directIdx >= 0 -> directIdx
                    differentHostIdx >= 0 -> differentHostIdx
                    else -> 0
                }
                val next = backupSources.removeAt(nextIdx)
                Log.w(
                    "MadarPlayer",
                    "WebView made no playback progress after ${webViewInitialPlaybackWaitMs}ms; " +
                        "switching host=${safeHost(next.url)} backups=${backupSources.size}",
                )
                onSwitchStream(
                    next.url,
                    next.userAgent.ifBlank { cleanUa },
                    next.referer.ifBlank { cleanRef },
                    next.headers,
                )
            } else {
                Log.w(
                    "MadarPlayer",
                    "WebView made no playback progress after ${webViewInitialPlaybackWaitMs}ms; no backups remain",
                )
                playbackError = "لم يبدأ الفيديو من هذا المصدر خلال 12 ثانية. اختر سيرفرًا آخر أو أعد المحاولة."
                isBuffering = false
            }
            return@LaunchedEffect
        }
    }

    LaunchedEffect(subtitlesEnabled, player) {
        player?.let { p ->
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
                .build()
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                        layoutDirection = View.LAYOUT_DIRECTION_LTR
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
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
                                            if (currentTime > 0.25f && !hasRenderedFirstFrame) {
                                                hasRenderedFirstFrame = true
                                                ytIsPlaying = true
                                                ytIsBuffering = false
                                                ytEnded = false
                                                isBuffering = false
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onQualityLevels(levelsJson: String, currentQuality: String) {
                                        pageProbeHandler.post {
                                            val parsed = runCatching {
                                                val arr = JSONArray(levelsJson)
                                                buildList {
                                                    for (i in 0 until arr.length()) {
                                                        val q = arr.optString(i).trim()
                                                        if (q.isNotBlank() && q != "auto" && q != "tiny") add(q)
                                                    }
                                                }
                                            }.getOrDefault(emptyList())
                                            if (parsed.isNotEmpty()) {
                                                ytAvailableQualities = parsed
                                            }
                                            if (currentQuality.isNotBlank()) {
                                                ytSelectedQuality = currentQuality
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
                                                    ytEnded = false
                                                    isBuffering = false
                                                    hasRenderedFirstFrame = true
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
                                                    ytEnded = true
                                                    showControls = true
                                                }
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onPlayerError(errorCode: Int) {
                                        pageProbeHandler.post {
                                            Log.w("MadarPlayer", "YouTube IFrame Error: $errorCode -> switching to mobile watch stream")
                                            ytIsBuffering = true
                                            if (!useWatchPage) {
                                                useWatchPage = true
                                                val ytId = extractYouTubeId(effectiveStreamUrl)
                                                if (ytId.isNotBlank()) {
                                                    webViewRef?.settings?.userAgentString = MOBILE_CHROME_UA
                                                    webViewRef?.loadUrl("https://m.youtube.com/watch?v=$ytId&app=m")
                                                }
                                            } else if (errorCode != 0) {
                                                playbackError = "تعذر تشغيل فيديو يوتيوب (رمز $errorCode). تأكد من الإنترنت أو افتحه بالمتصفح."
                                                isBuffering = false
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
                            userAgentString = if (isYouTube) {
                                MOBILE_CHROME_UA
                            } else {
                                cleanUa
                            }
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
                                    val currentHost = safeHost(activePageUrl.get())
                                        .ifBlank { safeHost(effectiveStreamUrl) }
                                    val directIndex = backupSources.indexOfFirst { isDirectStreamUrl(it.url) }
                                    val differentHostIndex = backupSources.indexOfFirst {
                                        val candidateHost = safeHost(it.url)
                                        candidateHost.isNotBlank() && candidateHost != currentHost
                                    }
                                    val nextIndex = when {
                                        directIndex >= 0 -> directIndex
                                        differentHostIndex >= 0 -> differentHostIndex
                                        else -> 0
                                    }
                                    val next = backupSources.removeAt(nextIndex)
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

                                if (reqUrl.startsWith("intent:", true) || reqUrl.startsWith("market:", true) ||
                                    reqUrl.startsWith("vnd.youtube:", true) || reqUrl.startsWith("whatsapp:", true) ||
                                    reqUrl.startsWith("tg:", true)
                                ) return true

                                if (isYouTube) {
                                    // Allow YouTube sub-resources to load normally.
                                    // Only block external navigation that leaves YouTube.
                                    val ytHost = host
                                    val isYtSubdomain = ytHost.endsWith("youtube.com") ||
                                        ytHost.endsWith("youtu.be") ||
                                        ytHost.endsWith("ytimg.com") ||
                                        ytHost.endsWith("ggpht.com") ||
                                        ytHost.endsWith("googlevideo.com") ||
                                        ytHost.endsWith("googleapis.com") ||
                                        ytHost.endsWith("gstatic.com") ||
                                        ytHost.endsWith("google.com")
                                    if (request?.isForMainFrame == true && !isYtSubdomain) {
                                        return true
                                    }
                                    return false
                                }

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
                                            handleMainFrameFailure("تم إيقاف توجيه غير صالح، جارٍ تجربة سيرفر آخر...")
                                            return true
                                        }
                                    }
                                }
                                return false
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: return null
                                if (isBlockedAd(reqUrl)) {
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
                                    // ── Black Screen Watchdog 1: 7.5s when backups exist, else 10s (ignore while PreRoll Ad covers screen) ──
                                    val watchdog1Ms = if (backupSources.isNotEmpty()) 7_500L else 10_000L
                                    pageProbeHandler.postDelayed({
                                        if (pageStartedAt.get() != startedAt || pageFailureScheduled.get() ||
                                            pagePlayableObserved.get() || !webViewActive.get() || isAdOverlayActive
                                        ) return@postDelayed
                                        Log.w("MadarPlayer", "WebView black screen watchdog ${watchdog1Ms}ms. Backups left: ${backupSources.size}")
                                        handleMainFrameFailure(
                                            "شاشة سوداء: السيرفر لم يبث فيديو خلال المهلة. جارٍ التبديل لسيرفر آخر تلقائياً..."
                                        )
                                    }, watchdog1Ms)

                                    // ── Black Screen Watchdog 2: 18 seconds (maximum fallback threshold) ──
                                    pageProbeHandler.postDelayed({
                                        if (pageStartedAt.get() != startedAt || pageFailureScheduled.get() ||
                                            !webViewActive.get() || isAdOverlayActive
                                        ) return@postDelayed
                                        if (!pagePlayableObserved.get()) {
                                            Log.w("MadarPlayer", "WebView black screen watchdog 14s. Backups left: ${backupSources.size}")
                                            handleMainFrameFailure(
                                                "السيرفر لم يبدأ فيديو قابلًا للتشغيل خلال المهلة. جارٍ الانتقال لمصدر آخر..."
                                            )
                                        }
                                    }, 14_000L)
                                } else if (useWatchPage) {
                                    view?.evaluateJavascript("window.__zagPausedForAd = ${isAdOverlayActive};", null)
                                    view?.evaluateJavascript(cleanCinemaJs, null)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!webViewActive.get()) return
                                if (isYouTube) {
                                    if (useWatchPage) {
                                        view?.evaluateJavascript(cleanCinemaJs, null)
                                        pageProbeHandler.postDelayed({ view?.evaluateJavascript(cleanCinemaJs, null) }, 300L)
                                        pageProbeHandler.postDelayed({ view?.evaluateJavascript(cleanCinemaJs, null) }, 800L)
                                        pageProbeHandler.postDelayed({ view?.evaluateJavascript(cleanCinemaJs, null) }, 1800L)
                                    }
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
                                       var docTitle=(document.title||'').toLowerCase();
                                       var challenge=/captcha|human verification|verify.{0,40}(human|you are)|checking your browser|security check|just a moment|attention required|cf-browser-verification|enable javascript and cookies/.test(body) || /just a moment|attention required/.test(docTitle);
                                       var frames=Array.prototype.slice.call(document.querySelectorAll('iframe')).map(function(x){return x.src||x.getAttribute('data-src')||'';});
                                       var challengeFrame=frames.some(function(src){return /captcha|challenge|watchadsontape\.com|lumbarssweepup\.com/i.test(src);});
                                       var adLanding=videos.length===0&&frames.length===0&&/welcome bonus|free spins|بونص ترحيبي|مكافأة الترحيب|روابط الموقع الجديدة/.test(body);
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
                                            !hasJsPlayer &&
                                            mediaUrl.isBlank() &&
                                            System.currentTimeMillis() - pageStartedAt.get() >= (if (backupSources.isNotEmpty()) 4_000L else 25_000L)
                                        ) {
                                            handleMainFrameFailure(
                                                "صفحة المصدر لا تحتوي مشغل فيديو. جارٍ تجربة سيرفر آخر."
                                            )
                                        } else if (payload.optBoolean("playing", false) && payload.optInt("ready", 0) >= 2) {
                                            pagePlayableObserved.set(true)
                                            hasRenderedFirstFrame = true
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
                            // Single authoritative load is driven by LaunchedEffect after oEmbed check
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
                        layoutDirection = View.LAYOUT_DIRECTION_LTR
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
                        subtitleView?.visibility = if (subtitlesEnabled) View.VISIBLE else View.GONE
                    }
                },
                update = {
                    it.player = player
                    it.subtitleView?.visibility = if (subtitlesEnabled) View.VISIBLE else View.GONE
                },
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

        // ── Creative Loading Screen (For Movies, Series, and Initial Stream Startup) ──
        val showCreativeLoader = !hasRenderedFirstFrame && !ytIsPlaying && playbackError == null

        AnimatedVisibility(
            visible = showCreativeLoader,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(400))
        ) {
            val elapsedSec = ((System.currentTimeMillis() - streamStartedMs) / 1000).coerceAtLeast(0)
            CreativeLoadingScreen(
                title = title,
                elapsedSeconds = elapsedSec,
                backupCount = backupSources.size,
                onSwitchBackup = {
                    if (backupSources.isNotEmpty()) {
                        val next = backupSources.removeAt(0)
                        onSwitchStream(
                            next.url,
                            next.userAgent.ifBlank { cleanUa },
                            next.referer.ifBlank { cleanRef },
                            next.headers,
                        )
                    }
                }
            )
        }

        // ── Mid-Stream Seeking / Buffering Indicator (Shows after first frame is already rendered) ──
        if (hasRenderedFirstFrame && (isBuffering || (isYouTube && ytIsBuffering))) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA000000))
                        .border(1.dp, Color(0x55D4AF37), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFFD4AF37),
                            strokeWidth = 2.8.dp,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (isYouTube) "تقديم سريع..." else "جاري التحميل...",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                                val urlToOpen = effectiveStreamUrl
                                if (isDirectStreamUrl(urlToOpen)) {
                                    // Direct stream → open with video player chooser
                                    try {
                                        val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(urlToOpen), "video/*")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(Intent.createChooser(videoIntent, "فتح بـ..."))
                                    } catch (e: Exception) {
                                        // Fallback: open in in-app browser
                                        inAppBrowserUrl = urlToOpen
                                    }
                                } else {
                                    // Embed/webpage URL → open in in-app WebView
                                    inAppBrowserUrl = urlToOpen
                                }
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

        // In-app browser overlay (opened from "بالمتصفح" button or ad tap)
        inAppBrowserUrl?.let { browserUrl ->
            InAppBrowserOverlay(
                url = browserUrl,
                onClose = { inAppBrowserUrl = null }
            )
        }

        // Keep custom controls in the same tap-to-show/hide cycle as the
        // Media3 controller. Tapping the video only changes overlays; it never
        // pauses or recreates the active player.
        AnimatedVisibility(
            visible = showControls && !isAdOverlayActive,
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
                val ytQualityChip = when (ytSelectedQuality) {
                    "highres", "hd2160" -> "4K"
                    "hd1440" -> "1440p"
                    "hd1080" -> "1080p HD"
                    "hd720" -> "720p HD"
                    "large" -> "480p"
                    "medium" -> "360p"
                    "small" -> "240p"
                    else -> "HD"
                }
                val bandwidthLabel = when {
                    isYouTube -> "🎬 يوتيوب · $ytQualityChip"
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

                // ── Subtitles / CC Toggle button (زر الترجمة والكلام المكتوب) ──
                Surface(
                    onClick = {
                        subtitlesEnabled = !subtitlesEnabled
                        preferences.edit().putBoolean("subtitles_enabled", subtitlesEnabled).apply()
                        lastInteractionTime = System.currentTimeMillis()
                        Toast.makeText(
                            context,
                            if (subtitlesEnabled) "تم تفعيل الترجمة (تظهر إذا كان الفيديو يدعم الترجمة)" else "تم إيقاف الترجمة",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    color = if (subtitlesEnabled) Color(0xD92E7D32) else Color(0xD9272C3D),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                    ) {
                        Text(
                            if (subtitlesEnabled) "💬 الترجمة: مفعّلة" else "💬 الترجمة: معطّلة",
                            color = if (subtitlesEnabled) Color.White else Color(0xFFB0B3C0),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                ytAvailableQualities = ytAvailableQualities,
                ytSelectedQuality = ytSelectedQuality,
                onSelectYtQuality = { ytQ ->
                    ytSelectedQuality = ytQ
                    webViewRef?.evaluateJavascript("if (window.__zagSetQuality) window.__zagSetQuality('$ytQ');", null)
                    showBandwidthSheet = false
                    lastInteractionTime = System.currentTimeMillis()
                },
                streamUrl = effectiveStreamUrl,
                currentBitrateKbps = currentBitrateKbps,
                currentTrackInfo = currentTrackInfo,
                onSelect = { quality ->
                    bufferingSinceMs = 0L
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

        // ── YouTube Clean Replay Overlay (covers YouTube end-cards cleanly) ──
        if (isYouTube && ytEnded && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD90E0A12)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                ytEnded = false
                                ytIsPlaying = true
                                ytCurrentSec = 0f
                                webViewRef?.evaluateJavascript(
                                    "if (window.__zagSeekTo) window.__zagSeekTo(0); if (window.__zagPlay) window.__zagPlay();",
                                    null
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("🔄 إعادة المشاهدة", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = { onClose() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272C3D)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("◀ رجوع", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── YouTube Native Center Controls ──────────────────────────────────
        if (isYouTube && !ytEnded) {
            AnimatedVisibility(
                visible = showControls && !isAdOverlayActive,
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
                                    webViewRef?.evaluateJavascript(
                                        """(function(){
                                            var p = window.__zagGetPlayer ? window.__zagGetPlayer() : (document.getElementById('movie_player') || document.querySelector('.html5-video-player'));
                                            if (p && typeof p.pauseVideo === 'function') { p.pauseVideo(); }
                                            else { var v = document.querySelector('video'); if (v) v.pause(); }
                                        })();""".trimIndent(),
                                        null
                                    )
                                } else {
                                    if (ytDurationSec > 0f && ytCurrentSec >= ytDurationSec - 1f) {
                                        webViewRef?.evaluateJavascript(
                                            """(function(){
                                                if (window.__zagSeekTo) { window.__zagSeekTo(0); }
                                                else {
                                                    var p = window.__zagGetPlayer ? window.__zagGetPlayer() : (document.getElementById('movie_player') || document.querySelector('.html5-video-player'));
                                                    if (p && typeof p.seekTo === 'function') { p.seekTo(0, true); if (p.playVideo) p.playVideo(); }
                                                    else { var v = document.querySelector('video'); if (v) { v.currentTime = 0; v.play(); } }
                                                }
                                            })();""".trimIndent(),
                                            null
                                        )
                                    } else {
                                        webViewRef?.evaluateJavascript(
                                            """(function(){
                                                var p = window.__zagGetPlayer ? window.__zagGetPlayer() : (document.getElementById('movie_player') || document.querySelector('.html5-video-player'));
                                                if (p && typeof p.playVideo === 'function') { p.playVideo(); }
                                                else { var v = document.querySelector('video'); if (v) v.play(); }
                                            })();""".trimIndent(),
                                            null
                                        )
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
                visible = showControls && !isAdOverlayActive,
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
                                        webViewRef?.evaluateJavascript(
                                            """(function(){
                                                if (window.__zagSeekTo) {
                                                    window.__zagSeekTo($target);
                                                } else {
                                                    var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                                                    if (p && typeof p.seekTo === 'function') {
                                                        p.seekTo($target, true);
                                                        if (typeof p.playVideo === 'function') p.playVideo();
                                                    } else {
                                                        var v = document.querySelector('video');
                                                        if (v) { v.currentTime = $target; v.play(); }
                                                    }
                                                }
                                            })();""".trimIndent(),
                                            null
                                        )
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
                                text = "ZAG Player",
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
                                    webViewRef?.evaluateJavascript(
                                        """(function(){
                                            var p = window.__zagGetPlayer ? window.__zagGetPlayer() : (document.getElementById('movie_player') || document.querySelector('.html5-video-player'));
                                            if (p && typeof p.setPlaybackRate === 'function') { p.setPlaybackRate($nextSpeed); }
                                            else { var v = document.querySelector('video'); if (v) v.playbackRate = $nextSpeed; }
                                        })();""".trimIndent(),
                                        null
                                    )
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
            visible = showControls && !isYouTube && !isAdOverlayActive,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 18.dp, bottom = 54.dp)
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
                        text = "ZAG Player",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
    ytAvailableQualities: List<String> = emptyList(),
    ytSelectedQuality: String = "auto",
    onSelectYtQuality: (String) -> Unit = {},
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
                            val qualityChoices = if (ytAvailableQualities.isNotEmpty()) {
                                listOf("auto") + ytAvailableQualities.distinct()
                            } else {
                                listOf("auto", "hd1080", "hd720", "large", "medium", "small")
                            }
                            fun ytLabelFor(code: String): String = when (code) {
                                "auto" -> "⚙ تلقائي (حسب سرعة النت)"
                                "highres", "hd2160" -> "✦ 4K (2160p)"
                                "hd1440" -> "✦ 2K (1440p)"
                                "hd1080" -> "✦ Full HD (1080p)"
                                "hd720" -> "🎬 HD (720p)"
                                "large" -> "⚖️ جودة متوسطة (480p)"
                                "medium" -> "⚡ توفير بيانات (360p)"
                                "small" -> "⚡ توفير أقصى (240p)"
                                else -> code
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "🎬 اختر جودة فيديو يوتيوب داخل المشغل",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                qualityChoices.forEach { qCode ->
                                    val isSel = qCode == ytSelectedQuality || (qCode == "auto" && ytSelectedQuality.isBlank())
                                    Surface(
                                        onClick = { onSelectYtQuality(qCode) },
                                        color = if (isSel) PlayerBrick else PlayerSheetSurf,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isSel) PlayerGold else Color(0x33FFFFFF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ytLabelFor(qCode),
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                            )
                                            if (isSel) {
                                                Text("✓", color = PlayerGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF2D2230))
                                        .clickable {
                                            val ytId = extractYouTubeId(streamUrl)
                                            val url = if (ytId.isNotBlank()) "https://www.youtube.com/watch?v=$ytId" else streamUrl
                                            try {
                                                val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                sheetContext.startActivity(ytIntent)
                                            } catch (e: Exception) {}
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("فتح في تطبيق YouTube ↗", color = PlayerMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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
        } catch (e: Exception) {}
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
            params.setMaxVideoBitrate(2_500_000)
            val hdTracks = tracks.filter { it.height in 480..720 }
            val selected = if (hdTracks.isNotEmpty()) {
                hdTracks.maxByOrNull { it.bitrate.takeIf { rate -> rate > 0 } ?: 0 }
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
            params.setMaxVideoBitrate(2_000_000)
            val midTracks = tracks.filter { it.height in 480..720 }
            val selected = midTracks.maxByOrNull {
                it.bitrate.takeIf { rate -> rate > 0 } ?: 0
            } ?: tracks.filter { it.height <= 720 }.maxByOrNull { it.height }
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
                params.setMaxVideoBitrate(Int.MAX_VALUE)
            } else {
                params.setMaxVideoBitrate(Int.MAX_VALUE)
            }
            // Match by label index first (most precise), then by height
            val selected = tracks.indices.firstOrNull { i -> qualityLabels.getOrNull(i) == choice }
                ?.let { tracks[it] }
                ?: wantedHeight?.let { wanted ->
                    tracks.filter { it.height == wanted }.maxByOrNull { it.bitrate }
                        ?: tracks.filter { it.height <= wanted }.maxWithOrNull(compareBy<VideoQualityTrack> { it.height }.thenBy { it.bitrate })
                        ?: tracks.minByOrNull { it.height }
                }
            if (selected != null) {
                params.addOverride(TrackSelectionOverride(selected.group, selected.index))
            }
        }
    }

    trackSelector.setParameters(params.build())
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

@Composable
private fun PlayerBlockedScreen(
    message: String,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080C)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF14141E))
                .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4AF37).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔒", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                color = Color(0xFFD4AF37).copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "صيانة مؤقتة",
                    color = Color(0xFFD4AF37),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "مشغل زقزوقة (ZAG Player)",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message.ifBlank { "عفواً، المشغل متوقف حالياً للصيانة والتحديث." },
                color = Color(0xFF9E9EB2),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD4AF37),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = "إعادة التحقق الآن", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onClose,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = "إغلاق المشغل", fontSize = 14.sp)
            }
        }
    }
}
