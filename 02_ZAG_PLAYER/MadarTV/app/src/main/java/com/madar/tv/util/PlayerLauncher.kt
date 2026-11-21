package com.madar.tv.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.madar.tv.data.ArabseedRepository
import org.json.JSONArray
import org.json.JSONObject

data class PlayerSource(
    val url: String,
    val userAgent: String = "",
    val referer: String = "",
    val headers: Map<String, String> = emptyMap(),
)

object PlayerLauncher {

    const val PLAYER_PACKAGE = "com.mader.player"
    const val PLAYER_ACTIVITY = "com.madar.player.PlayerActivity"

    fun launchPlayer(
        context: Context,
        streamUrl: String,
        title: String,
        userAgent: String = ArabseedRepository.getPlayerUserAgent(),
        referer: String = "",
        headers: Map<String, String> = emptyMap(),
        backupUrls: List<String> = emptyList(),
        backupSources: List<PlayerSource> = emptyList(),
        fallbackUrl: String = "",
        fallbackReferer: String = referer,
        onNotInstalled: (() -> Unit)? = null
    ): Boolean {
        if (streamUrl.isBlank()) {
            Toast.makeText(context, "لا يوجد رابط تشغيل متاح", Toast.LENGTH_SHORT).show()
            return false
        }

        val cleanUa = userAgent.ifBlank { ArabseedRepository.getPlayerUserAgent() }
        val normalizedBackups = (backupSources + backupUrls.map { PlayerSource(it, cleanUa, referer, headers) })
            .filter { it.url.isNotBlank() && it.url != streamUrl }
            .distinctBy { it.url }
        val backupSourceJson = JSONArray().apply {
            normalizedBackups.forEach { source ->
                put(JSONObject()
                    .put("url", source.url)
                    .put("userAgent", source.userAgent.ifBlank { cleanUa })
                    .put("referer", source.referer.ifBlank { referer })
                    .put("headers", JSONObject(source.headers)))
            }
        }.toString()
        val normalizedBackupUrls = ArrayList(normalizedBackups.map { it.url })

        // 1. Try explicit component intent directly to Zakzouka Player
        try {
            val explicitIntent = Intent(Intent.ACTION_VIEW).apply {
                setClassName(PLAYER_PACKAGE, PLAYER_ACTIVITY)
                putExtra("com.madar.player.extra.STREAM_URL", streamUrl)
                putExtra("stream_url", streamUrl)
                putExtra("url", streamUrl)
                putExtra("video_url", streamUrl)
                putExtra("com.madar.player.extra.TITLE", title)
                putExtra("title", title)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra("com.madar.player.extra.USER_AGENT", cleanUa)
                putExtra("com.madar.player.extra.REFERER", referer)
                putExtra("com.madar.player.extra.STREAM_HEADERS", JSONObject(headers).toString())
                putExtra("com.madar.player.extra.FALLBACK_URL", fallbackUrl)
                putExtra("com.madar.player.extra.FALLBACK_REFERER", fallbackReferer)
                putExtra("com.madar.player.extra.BACKUP_SOURCES", backupSourceJson)
                putStringArrayListExtra("com.madar.player.extra.BACKUP_URLS", normalizedBackupUrls)
                putStringArrayListExtra("backup_stream_urls", normalizedBackupUrls)
                data = Uri.parse(streamUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(explicitIntent)
            return true
        } catch (_: Exception) {}

        // 2. Try launch intent for package
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(PLAYER_PACKAGE)
            if (launchIntent != null) {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.putExtra("com.madar.player.extra.STREAM_URL", streamUrl)
                launchIntent.putExtra("stream_url", streamUrl)
                launchIntent.putExtra("url", streamUrl)
                launchIntent.putExtra("video_url", streamUrl)
                launchIntent.putExtra("com.madar.player.extra.TITLE", title)
                launchIntent.putExtra("title", title)
                launchIntent.putExtra(Intent.EXTRA_TITLE, title)
                launchIntent.putExtra("com.madar.player.extra.USER_AGENT", cleanUa)
                launchIntent.putExtra("com.madar.player.extra.REFERER", referer)
                launchIntent.putExtra("com.madar.player.extra.STREAM_HEADERS", JSONObject(headers).toString())
                launchIntent.putExtra("com.madar.player.extra.FALLBACK_URL", fallbackUrl)
                launchIntent.putExtra("com.madar.player.extra.FALLBACK_REFERER", fallbackReferer)
                launchIntent.putExtra("com.madar.player.extra.BACKUP_SOURCES", backupSourceJson)
                launchIntent.putStringArrayListExtra("com.madar.player.extra.BACKUP_URLS", normalizedBackupUrls)
                launchIntent.putStringArrayListExtra("backup_stream_urls", normalizedBackupUrls)
                launchIntent.data = Uri.parse(streamUrl)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launchIntent)
                return true
            }
        } catch (_: Exception) {}

        if (onNotInstalled != null) {
            onNotInstalled()
            return false
        }
        // Try fallback to any installed video player
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(streamUrl), "video/*")
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra("title", title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
            return true
        } catch (_: Exception) {}

        return false
    }
}
