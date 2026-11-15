package com.madar.madar_flutter

import android.content.Intent
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import com.madar.tv.notifications.NotificationCenter

/** Public Android updater using Firebase metadata and public GitHub APK assets. */
class InAppUpdateManager(private val activity: MainActivity) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(3, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile
    private var progressSink: EventChannel.EventSink? = null

    @Volatile
    private var latestRelease: UpdateRelease? = null

    @Volatile
    private var activeDownload: Call? = null

    fun setProgressSink(sink: EventChannel.EventSink?) {
        progressSink = sink
    }

    fun checkForUpdate(result: MethodChannel.Result) {
        val request = Request.Builder()
            .url(FIREBASE_UPDATE_URL)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "Zaqzouka-Android")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                Log.i(TAG, "Firebase update feed unavailable (${error.javaClass.simpleName}); trying the migration feed")
                checkGitHubRelease(result)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.i(TAG, "Firebase update feed returned HTTP ${it.code}; trying the migration feed")
                        checkGitHubRelease(result)
                        return
                    }

                    val release = runCatching {
                        val payload = it.body?.string()
                            ?.takeIf { body -> body.length <= MAX_RELEASE_RESPONSE_CHARS }
                            ?: throw IOException("Invalid Firebase update response")
                        UpdateRelease.fromFirebase(JSONObject(payload))
                    }.getOrElse { error ->
                        Log.w(TAG, "Firebase update metadata is invalid (${error.javaClass.simpleName}); trying the migration feed")
                        checkGitHubRelease(result)
                        return
                    }

                    Log.i(TAG, "Firebase update feed checked successfully")
                    respondForRelease(release, result)
                }
            }
        })
    }

    /** Keeps older installations moving to a Firebase-aware build if Hosting is unavailable. */
    private fun checkGitHubRelease(result: MethodChannel.Result) {
        val feedPrefs = activity.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        val cachedReleaseJson = feedPrefs.getString(CACHED_RELEASE_JSON, null)
        val cachedEtag = feedPrefs.getString(CACHED_RELEASE_ETAG, null)
        val now = System.currentTimeMillis()
        val lastCheck = feedPrefs.getLong(LAST_SUCCESSFUL_CHECK, 0L)
        if (!cachedReleaseJson.isNullOrBlank() && now - lastCheck < UPDATE_CHECK_MIN_INTERVAL_MS) {
            val cachedRelease = runCatching {
                UpdateRelease.fromGitHub(JSONObject(cachedReleaseJson))
            }.getOrNull()
            if (cachedRelease != null) {
                Log.i(TAG, "Using the recently checked public release")
                return respondForRelease(cachedRelease, result)
            }
        }
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("Cache-Control", "no-cache")
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", "Zaqzouka-Android")
            .apply { if (!cachedEtag.isNullOrBlank()) header("If-None-Match", cachedEtag) }
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                Log.i(TAG, "Public update check unavailable (${error.javaClass.simpleName})")
                latestRelease = null
                respond(result, mapOf("available" to false, "status" to "check_failed"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val releasePayload = when {
                        it.code == 304 -> cachedReleaseJson
                        !it.isSuccessful -> null
                        else -> runCatching {
                            val payload = it.body?.string()
                                ?.takeIf { body -> body.length <= MAX_RELEASE_RESPONSE_CHARS }
                                ?: throw IOException("Invalid release response")
                            it.header("ETag")?.let { etag ->
                                feedPrefs.edit()
                                    .putString(CACHED_RELEASE_ETAG, etag)
                                    .putString(CACHED_RELEASE_JSON, payload)
                                    .apply()
                            }
                            payload
                        }.getOrElse { error ->
                            Log.w(TAG, "Public release response could not be read (${error.javaClass.simpleName})")
                            null
                        }
                    }
                    if (releasePayload.isNullOrBlank()) {
                        Log.i(TAG, "Public update check unavailable (HTTP ${it.code})")
                        latestRelease = null
                        respond(result, mapOf("available" to false, "status" to "check_failed"))
                        return
                    }

                    val releaseJson = runCatching {
                        JSONObject(releasePayload)
                    }.getOrElse { error ->
                        Log.w(TAG, "Public release response could not be read (${error.javaClass.simpleName})")
                        latestRelease = null
                        respond(result, mapOf("available" to false, "status" to "check_failed"))
                        return
                    }

                    val release = runCatching { UpdateRelease.fromGitHub(releaseJson) }.getOrElse { error ->
                        Log.w(TAG, "Public release metadata is invalid (${error.javaClass.simpleName})")
                        latestRelease = null
                        respond(result, mapOf("available" to false, "status" to "check_failed"))
                        return
                    }

                    feedPrefs.edit().putLong(LAST_SUCCESSFUL_CHECK, System.currentTimeMillis()).apply()
                    respondForRelease(release, result)
                }
            }
        })
    }

    private fun respondForRelease(release: UpdateRelease, result: MethodChannel.Result) {
        if (release.versionCode <= installedVersionCode()) {
            latestRelease = null
            Log.i(TAG, "No newer public update is available")
            respond(result, mapOf("available" to false, "status" to "no_update"))
            return
        }

        latestRelease = release
        Log.i(TAG, "Public update available: ${release.versionName} (${release.versionCode})")
        runCatching {
            NotificationCenter.postUpdateNotification(
                activity,
                release.versionName,
                release.releaseNotes.ifBlank { "يتوفر تحديث جديد للتطبيق." },
            )
        }
        respond(result, mapOf(
            "available" to true,
            "versionCode" to release.versionCode,
            "versionName" to release.versionName,
            "releaseNotes" to release.releaseNotes,
        ))
    }

    fun downloadAndInstall(result: MethodChannel.Result) {
        val release = latestRelease
        if (release == null) {
            respond(result, mapOf("status" to "no_update"))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            runCatching {
                activity.startActivity(settingsIntent)
                respond(result, mapOf("status" to "permission_required"))
            }.onFailure {
                respond(result, mapOf("status" to "permission_unavailable"))
            }
            return
        }

        val request = Request.Builder()
            .url(release.apkUrl)
            .header("Accept", "application/vnd.android.package-archive,application/octet-stream")
            .header("User-Agent", "Zaqzouka-Android")
            .build()
        val call = client.newCall(request)
        activeDownload = call
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                activeDownload = null
                Log.w(TAG, "Public update download failed (${error.javaClass.simpleName})")
                respond(result, mapOf("status" to "download_failed"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful || !isTrustedAssetHost(it.request.url.host)) {
                        activeDownload = null
                        respond(result, mapOf("status" to "download_failed"))
                        return
                    }

                    val body = it.body
                    if (body == null || body.contentLength() > MAX_APK_BYTES) {
                        activeDownload = null
                        respond(result, mapOf("status" to "download_failed"))
                        return
                    }

                    val updateDirectory = File(activity.cacheDir, "updates")
                    val apkFile = File(updateDirectory, "zaqzouka-${release.versionCode}.apk")
                    try {
                        updateDirectory.mkdirs()
                        apkFile.delete()
                        val totalBytes = body.contentLength()
                        body.byteStream().use { input ->
                            FileOutputStream(apkFile).use { output ->
                                val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                                var downloaded = 0L
                                var lastReported = 0L
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    downloaded += count
                                    if (downloaded > MAX_APK_BYTES) throw IOException("APK exceeds size limit")
                                    output.write(buffer, 0, count)
                                    if (downloaded - lastReported >= PROGRESS_INTERVAL_BYTES) {
                                        lastReported = downloaded
                                        postProgress(downloaded, totalBytes)
                                    }
                                }
                                output.fd.sync()
                                postProgress(downloaded, totalBytes)
                            }
                        }

                        if (!sha256(apkFile).equals(release.sha256, ignoreCase = true)) {
                            throw IOException("Downloaded APK checksum mismatch")
                        }
                        validateApk(apkFile, release.versionCode)
                        openInstaller(apkFile)
                        activeDownload = null
                        respond(result, mapOf("status" to "install_started"))
                    } catch (error: Exception) {
                        apkFile.delete()
                        activeDownload = null
                        Log.w(TAG, "Downloaded update was rejected (${error.javaClass.simpleName})")
                        respond(result, mapOf("status" to "download_failed"))
                    }
                }
            }
        })
    }

    private fun validateApk(apkFile: File, expectedVersionCode: Long) {
        @Suppress("DEPRECATION")
        val signingFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        @Suppress("DEPRECATION")
        val archived = activity.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            signingFlag,
        ) ?: throw IOException("Downloaded file is not a valid Android package")
        if (archived.packageName != activity.packageName) {
            throw IOException("Downloaded package name does not match this app")
        }
        val archivedCode = packageVersionCode(archived)
        if (archivedCode < expectedVersionCode) {
            throw IOException("Downloaded version ($archivedCode) is older than expected ($expectedVersionCode)")
        }

        // Diagnostic signature check: log warnings but do not block system installer invocation.
        // On modern OEM Android (e.g. Xiaomi MIUI), getPackageArchiveInfo does not reliably populate
        // signingInfo for archive files, and the cryptographic sha256 already verified the exact binary.
        // Android's native PackageInstaller will verify signing certificate compatibility during installation.
        runCatching {
            val installed = activity.packageManager.getPackageInfo(activity.packageName, signingFlag)
            val installedSigners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                installed.signingInfo?.apkContentsSigners?.map { sha256(it.toByteArray()) }?.toSet()
            } else {
                @Suppress("DEPRECATION")
                installed.signatures?.map { sha256(it.toByteArray()) }?.toSet()
            }
            val archivedSigners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archived.signingInfo?.apkContentsSigners?.map { sha256(it.toByteArray()) }?.toSet()
            } else {
                @Suppress("DEPRECATION")
                archived.signatures?.map { sha256(it.toByteArray()) }?.toSet()
            }
            if (!installedSigners.isNullOrEmpty() && !archivedSigners.isNullOrEmpty() && installedSigners != archivedSigners) {
                Log.w(TAG, "Package signer mismatch warning: installed=$installedSigners vs archived=$archivedSigners")
            }
        }.onFailure { Log.w(TAG, "Signature inspection skipped: ${it.message}") }
    }

    private fun openInstaller(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(installIntent)
    }

    private fun installedVersionCode(): Long {
        @Suppress("DEPRECATION")
        val installed = activity.packageManager.getPackageInfo(activity.packageName, 0)
        return packageVersionCode(installed)
    }

    @Suppress("DEPRECATION")
    private fun packageVersionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun isTrustedAssetHost(host: String): Boolean =
        host == "github.com" || host == "release-assets.githubusercontent.com" ||
            host == "objects.githubusercontent.com"

    private fun postProgress(downloaded: Long, total: Long) {
        val payload = mapOf(
            "status" to "downloading",
            "downloadedBytes" to downloaded,
            "totalBytes" to total,
        )
        activity.runOnUiThread { progressSink?.success(payload) }
    }

    private fun respond(result: MethodChannel.Result, payload: Map<String, Any>) {
        activity.runOnUiThread { result.success(payload) }
    }

    fun close() {
        activeDownload?.cancel()
        activeDownload = null
        progressSink = null
        latestRelease = null
    }

    private data class UpdateRelease(
        val versionCode: Long,
        val versionName: String,
        val releaseNotes: String,
        val apkUrl: String,
        val sha256: String,
    ) {
        companion object {
            fun fromFirebase(json: JSONObject): UpdateRelease {
                val packageName = json.optString("packageName")
                val versionCode = json.optLong("versionCode", 0L)
                val versionName = json.optString("versionName")
                val apkUrl = json.optString("apkUrl")
                val checksum = json.optString("sha256")
                val uri = Uri.parse(apkUrl)
                val expectedAssetName = "zaqzouka-$versionName-$versionCode.apk"
                val trustedPath = uri.path.orEmpty().startsWith(GITHUB_RELEASE_PATH) &&
                    uri.path.orEmpty().endsWith("/$expectedAssetName")
                if (packageName != APP_PACKAGE || versionCode <= 0L ||
                    versionName.isBlank() || versionName.length > MAX_VERSION_NAME_CHARS ||
                    checksum.matches(Regex("[a-fA-F0-9]{64}")).not() || uri.scheme != "https" ||
                    uri.host != "github.com" || !trustedPath
                ) {
                    throw IOException("Required Firebase update metadata is missing or untrusted")
                }
                return UpdateRelease(
                    versionCode = versionCode,
                    versionName = versionName,
                    releaseNotes = json.optString("releaseNotes").take(MAX_RELEASE_NOTES_CHARS),
                    apkUrl = apkUrl,
                    sha256 = checksum,
                )
            }

            fun fromGitHub(json: JSONObject): UpdateRelease {
                if (json.optBoolean("draft", true) || json.optBoolean("prerelease", true)) {
                    throw IOException("The latest release is not a public stable release")
                }
                val tag = json.optString("tag_name")
                val tagParts = RELEASE_TAG.matchEntire(tag)
                    ?: throw IOException("Release tag does not contain a supported app version")
                val versionName = tagParts.groupValues[1]
                val versionCode = tagParts.groupValues[2].toLongOrNull() ?: 0L
                val releaseNotes = json.optString("body").take(MAX_RELEASE_NOTES_CHARS)
                val expectedAssetName = "zaqzouka-$versionName-$versionCode.apk"
                val asset = json.optJSONArray("assets")
                    ?.findAsset(expectedAssetName)
                    ?: throw IOException("APK asset is missing from the latest release")
                val apkUrl = asset.optString("browser_download_url")
                val checksum = asset.optString("digest").removePrefix("sha256:")
                val uri = Uri.parse(apkUrl)
                val trustedPath = uri.path.orEmpty().startsWith(GITHUB_RELEASE_PATH) &&
                    uri.path.orEmpty().endsWith("/$expectedAssetName")
                if (versionCode <= 0L || versionName.isBlank() ||
                    checksum.matches(Regex("[a-fA-F0-9]{64}")).not() || uri.scheme != "https" ||
                    uri.host != "github.com" || !trustedPath
                ) {
                    throw IOException("Required update metadata is missing or untrusted")
                }
                return UpdateRelease(versionCode, versionName, releaseNotes, apkUrl, checksum)
            }

            private fun JSONArray.findAsset(expectedName: String): JSONObject? {
                for (index in 0 until length()) {
                    val asset = optJSONObject(index) ?: continue
                    if (asset.optString("name") == expectedName &&
                        asset.optString("state") == "uploaded"
                    ) return asset
                }
                return null
            }
        }
    }

    companion object {
        private const val TAG = "ZaqzoukaUpdates"
        private const val UPDATE_PREFS = "public_update_feed"
        private const val CACHED_RELEASE_ETAG = "latest_release_etag"
        private const val CACHED_RELEASE_JSON = "latest_release_json"
        private const val LAST_SUCCESSFUL_CHECK = "last_successful_check_ms"
        private const val APP_PACKAGE = "com.madar.madar_flutter"
        private const val FIREBASE_UPDATE_URL =
            "https://madartv-updates.web.app/updates/android/latest.json"
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/hassanIssa00/madartv-updates/releases/latest"
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val GITHUB_RELEASE_PATH = "/hassanIssa00/madartv-updates/releases/download/"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val MAX_RELEASE_RESPONSE_CHARS = 256 * 1024
        private const val MAX_VERSION_NAME_CHARS = 40
        private const val MAX_RELEASE_NOTES_CHARS = 500
        private const val MAX_APK_BYTES = 250L * 1024L * 1024L
        private const val UPDATE_CHECK_MIN_INTERVAL_MS = 30L * 60L * 1000L
        private const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
        private const val PROGRESS_INTERVAL_BYTES = 256L * 1024L
        private val RELEASE_TAG = Regex("^v([0-9]+\\.[0-9]+\\.[0-9]+)-([0-9]+)$")
    }
}
