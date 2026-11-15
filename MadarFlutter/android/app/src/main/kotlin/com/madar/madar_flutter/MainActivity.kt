package com.madar.madar_flutter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import android.content.res.Configuration
import java.util.Locale
import java.util.UUID
import org.json.JSONObject
import com.madar.tv.notifications.NotificationCenter
import com.madar.tv.notifications.NotificationWorkScheduler

class MainActivity : FlutterActivity() {
    private lateinit var inAppUpdateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inAppUpdateManager = InAppUpdateManager(this)
        runCatching { NotificationCenter.createChannels(applicationContext) }
            .onFailure { android.util.Log.e(STARTUP_TAG, "Notification channels unavailable", it) }
        runCatching { NotificationWorkScheduler.schedule(applicationContext) }
            .onFailure { android.util.Log.e(STARTUP_TAG, "Background notification scheduling unavailable", it) }
        runCatching { requestNotificationPermissionOnce() }
            .onFailure { android.util.Log.e(STARTUP_TAG, "Notification permission request failed", it) }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val mediaBridge = FlutterMediaBridge(applicationContext)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "madar/native")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "cache.read" -> {
                        val key = call.argument<String>("key").orEmpty()
                        result.success(getSharedPreferences("madar_flutter_cache", MODE_PRIVATE).getString(key, null))
                    }
                    "cache.write" -> {
                        val key = call.argument<String>("key").orEmpty()
                        val value = call.argument<String>("value").orEmpty()
                        getSharedPreferences("madar_flutter_cache", MODE_PRIVATE).edit().putString(key, value).apply()
                        result.success(null)
                    }
                    "player.open" -> openPlayer(call.argument("url"), call.argument("title"), call.argument("userAgent"), call.argument("referer"), call.argument("headers"), call.argument("backupSourcesJson"), call.argument<Boolean>("live") ?: false, result)
                    "player.store" -> openPlayerStore(result)
                    "player.status" -> {
                        val code = inAppUpdateManager.getInstalledPlayerVersionCode()
                        result.success(
                            mapOf(
                                "installed" to (code > 0L),
                                "versionCode" to code,
                            )
                        )
                    }
                    "player.install" -> {
                        val apkUrl = call.argument<String>("apkUrl")
                        val sha256 = call.argument<String>("sha256")
                        inAppUpdateManager.downloadAndInstallPlayer(apkUrl, sha256, result)
                    }
                    "youtube.open" -> openYouTube(call.argument("videoId"), call.argument("watchUrl"), result)
                    "download.start" -> startDownload(call.argument("url"), call.argument("title"), call.argument("userAgent"), call.argument("referer"), result)
                    "updates.check" -> inAppUpdateManager.checkForUpdate(result)
                    "updates.install" -> inAppUpdateManager.downloadAndInstall(result)
                    "notifications.status" -> result.success(
                        mapOf("enabled" to runCatching {
                            NotificationCenter.areNotificationsAllowed(this)
                        }.getOrDefault(false)),
                    )
                    "notifications.request" -> requestNotificationPermissionFromUser(result)
                    "notifications.settings" -> openNotificationSettings(result)
                    "telemetry.deviceInfo" -> {
                        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                        }
                        val model = Build.MODEL
                        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
                        val osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
                        val uiModeManager = getSystemService(UI_MODE_SERVICE) as? android.app.UiModeManager
                        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
                        val country = detectRealCountry()
                        val language = Locale.getDefault().language
                        val tzId = java.util.TimeZone.getDefault().id

                        val prefs = getSharedPreferences("madar_telemetry", MODE_PRIVATE)
                        var deviceId = prefs.getString("device_uuid", null)
                        var isFirstOpen = false
                        if (deviceId == null) {
                            deviceId = UUID.randomUUID().toString()
                            prefs.edit().putString("device_uuid", deviceId).apply()
                            isFirstOpen = true
                        }

                        result.success(
                            mapOf(
                                "deviceId" to deviceId,
                                "device" to deviceName,
                                "deviceType" to if (isTv) "tv" else "phone",
                                "osVersion" to osVersion,
                                "countryCode" to country,
                                "timezone" to tzId,
                                "language" to language,
                                "brand" to Build.BRAND,
                                "manufacturer" to Build.MANUFACTURER,
                                "model" to Build.MODEL,
                                "isNewInstall" to isFirstOpen
                            )
                        )
                    }
                    else -> if (!mediaBridge.handle(call, result)) result.notImplemented()
                }
            }
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, "madar/updates/progress")
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    inAppUpdateManager.setProgressSink(events)
                }

                override fun onCancel(arguments: Any?) {
                    inAppUpdateManager.setProgressSink(null)
                }
            })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        if (::inAppUpdateManager.isInitialized) inAppUpdateManager.close()
        super.onDestroy()
    }

    private fun detectRealCountry(): String {
        // 1. Check SIM Operator Country (e.g. Vodafone Egypt, Orange, Etisalat, WE)
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val simCountry = tm?.simCountryIso?.trim()?.uppercase(Locale.ROOT)
            if (!simCountry.isNullOrEmpty() && simCountry.length == 2) {
                return simCountry
            }
            val networkCountry = tm?.networkCountryIso?.trim()?.uppercase(Locale.ROOT)
            if (!networkCountry.isNullOrEmpty() && networkCountry.length == 2) {
                return networkCountry
            }
        } catch (_: Exception) {}

        // 2. Check Device TimeZone (e.g. "Africa/Cairo", "Egypt", "Asia/Riyadh")
        val tzId = java.util.TimeZone.getDefault().id.lowercase(Locale.ROOT)
        if (tzId.contains("cairo") || tzId.contains("egypt")) return "EG"
        if (tzId.contains("riyadh") || tzId.contains("saudi")) return "SA"
        if (tzId.contains("dubai")) return "AE"
        if (tzId.contains("baghdad")) return "IQ"
        if (tzId.contains("casablanca")) return "MA"
        if (tzId.contains("kuwait")) return "KW"
        if (tzId.contains("algiers")) return "DZ"
        if (tzId.contains("qatar") || tzId.contains("doha")) return "QA"
        if (tzId.contains("amman")) return "JO"
        if (tzId.contains("beirut")) return "LB"
        if (tzId.contains("tunis")) return "TN"
        if (tzId.contains("khartoum")) return "SD"
        if (tzId.contains("tripoli")) return "LY"
        if (tzId.contains("damascus")) return "SY"
        if (tzId.contains("muscat")) return "OM"
        if (tzId.contains("bahrain") || tzId.contains("manama")) return "BH"
        if (tzId.contains("jerusalem") || tzId.contains("gaza")) return "PS"

        // 3. Fallback: check GMT Offset for Egypt (GMT+2 / GMT+3 with daylight saving)
        val rawOffsetHours = java.util.TimeZone.getDefault().rawOffset / (1000 * 60 * 60)
        val localeCountry = Locale.getDefault().country.trim().uppercase(Locale.ROOT)
        if (localeCountry == "US" && (rawOffsetHours == 2 || rawOffsetHours == 3)) {
            return "EG"
        }

        return if (localeCountry.isNotEmpty()) localeCountry else "EG"
    }

    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return

        val prefs = getSharedPreferences("notification_permission", MODE_PRIVATE)
        if (prefs.getBoolean("requested_v2", false)) return
        prefs.edit().putBoolean("requested_v2", true).apply()
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS,
        )
    }

    private fun requestNotificationPermissionFromUser(result: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            result.success(true)
            return
        }

        val prefs = getSharedPreferences("notification_permission", MODE_PRIVATE)
        val requestedBefore = prefs.getBoolean("requested_v2", false)
        if (requestedBefore &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        ) {
            openNotificationSettings(result)
            return
        }
        prefs.edit().putBoolean("requested_v2", true).apply()
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS,
        )
        result.success(true)
    }

    private fun openNotificationSettings(result: MethodChannel.Result) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        runCatching { startActivity(intent) }
            .onSuccess { result.success(true) }
            .onFailure { result.success(false) }
    }

    private fun startDownload(url: String?, title: String?, userAgent: String?, referer: String?, result: MethodChannel.Result) {
        val target = url.orEmpty()
        if (!isSafeMediaUrl(target)) {
            result.success(false)
            return
        }
        runCatching {
            com.madar.tv.download.ZakzoukaDownloadService.startDownload(
                context = this,
                title = title.orEmpty(),
                url = target,
                userAgent = userAgent.orEmpty(),
                referer = referer.orEmpty(),
            )
        }.onSuccess { result.success(true) }
            .onFailure { result.success(false) }
    }

    private fun openPlayer(
        url: String?, title: String?, userAgent: String?, referer: String?,
        headers: Map<String, String>?, backupSourcesJson: String?, isLive: Boolean, result: MethodChannel.Result,
    ) {
        val stream = url.orEmpty()
        if (!isSafeMediaUrl(stream)) {
            result.success(false)
            return
        }
        try {
            packageManager.getPackageInfo("com.madar.player", 0)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setClassName("com.madar.player", "com.madar.player.PlayerActivity")
                data = Uri.parse(stream)
                putExtra("com.madar.player.extra.STREAM_URL", stream)
                putExtra("com.madar.player.extra.TITLE", title.orEmpty())
                putExtra("com.madar.player.extra.USER_AGENT", userAgent.orEmpty())
                putExtra("com.madar.player.extra.REFERER", referer.orEmpty())
                putExtra("com.madar.player.extra.STREAM_HEADERS", JSONObject(headers ?: emptyMap<String, String>()).toString())
                putExtra("com.madar.player.extra.BACKUP_SOURCES", backupSourcesJson.orEmpty())
                putExtra("com.madar.player.extra.IS_LIVE", isLive)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            result.success(true)
        } catch (_: Exception) {
            result.success(false)
        }
    }

    private fun isSafeMediaUrl(value: String): Boolean {
        val uri = runCatching { Uri.parse(value.trim()) }.getOrNull() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        if ((uri.scheme != "http" && uri.scheme != "https") || host.isBlank()) return false
        val redirectDomains = listOf(
            "google.com",
            "googleadservices.com",
            "doubleclick.net",
            "googlesyndication.com",
        )
        return redirectDomains.none { host == it || host.endsWith(".$it") }
    }

    private fun openYouTube(videoId: String?, watchUrl: String?, result: MethodChannel.Result) {
        val id = videoId.orEmpty()
        val fallback = watchUrl.orEmpty().ifBlank { "https://www.youtube.com/watch?v=$id" }
        // 1. Try YouTube app via vnd.youtube URI
        try {
            val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$id"))
            ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(ytIntent)
            result.success(true)
            return
        } catch (_: Exception) {}
        // 2. Fall back to browser / Chrome
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallback))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(browserIntent)
            result.success(true)
        } catch (e: Exception) {
            result.success(false)
        }
    }
    private fun openPlayerStore(result: MethodChannel.Result) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.madar.player")))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.madar.player")))
            } catch (_: Exception) {
                result.success(false)
                return
            }
        }
        result.success(true)
    }

    companion object {
        private const val STARTUP_TAG = "ZaqzoukaStartup"
        private const val REQUEST_NOTIFICATIONS = 7001
    }
}
