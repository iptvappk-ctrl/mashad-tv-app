package com.madar.madar_flutter

import android.content.Intent
import android.net.Uri
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject

class MainActivity : FlutterActivity() {
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
                    "download.start" -> startDownload(call.argument("url"), call.argument("title"), call.argument("userAgent"), call.argument("referer"), result)
                    else -> if (!mediaBridge.handle(call, result)) result.notImplemented()
                }
            }
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
}
