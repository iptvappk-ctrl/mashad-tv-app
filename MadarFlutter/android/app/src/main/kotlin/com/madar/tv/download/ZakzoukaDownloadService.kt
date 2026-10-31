package com.madar.tv.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.madar.madar_flutter.MainActivity
import com.madar.tv.data.ArabseedRepository
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ZakzoukaDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private val activeOutputs = ConcurrentHashMap<Int, OutputStream>()
    private val activeUris = ConcurrentHashMap<Int, Uri>()
    private val activeFiles = ConcurrentHashMap<Int, File>()

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, 0)

        when (action) {
            ACTION_CANCEL -> {
                handleCancelDownload(downloadId)
            }
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "فيديو"
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val userAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()
                val referer = intent.getStringExtra(EXTRA_REFERER).orEmpty()
                handleStartDownload(downloadId, title, url, userAgent, referer)
            }
        }

        return START_NOT_STICKY
    }

    private fun handleCancelDownload(downloadId: Int) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()

        try {
            activeOutputs.remove(downloadId)?.close()
        } catch (_: Exception) {}

        // Delete partial downloaded file / uri
        try {
            val uri = activeUris.remove(downloadId)
            if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.delete(uri, null, null)
            }
            val file = activeFiles.remove(downloadId)
            if (file != null && file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}

        notificationManager.cancel(downloadId)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "تم إلغاء التنزيل", Toast.LENGTH_SHORT).show()
        }

        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleStartDownload(downloadId: Int, title: String, url: String, userAgent: String, referer: String) {
        val cleanTitle = title
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(70)

        val cancelIntent = Intent(this, ZakzoukaDownloadService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            downloadId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            downloadId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val initialNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done).setColor(0xFFD4AF37.toInt())
            .setContentTitle("تنزيل: $cleanTitle")
            .setContentText("جاري الاتصال وتجهيز التحميل...")
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء التنزيل", cancelPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(downloadId, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(downloadId, initialNotification)
        }

        val job = serviceScope.launch {
            try {
                performDownload(downloadId, cleanTitle, url, userAgent, referer, cancelPendingIntent, contentPendingIntent)
            } catch (e: CancellationException) {
                // User cancelled - cleanup already handled
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                showErrorNotification(downloadId, cleanTitle, msg)
            } finally {
                activeJobs.remove(downloadId)
                if (activeJobs.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }

        activeJobs[downloadId] = job
    }

    private suspend fun performDownload(
        downloadId: Int,
        cleanTitle: String,
        targetUrl: String,
        userAgent: String,
        referer: String,
        cancelPendingIntent: PendingIntent,
        contentPendingIntent: PendingIntent,
    ) = withContext(Dispatchers.IO) {
        // Resolve source pages before creating a visible download file. A failed resolver
        // is still checked by response MIME below, so HTML can never be saved as a movie.
        var streamUrl = targetUrl
        if (!isHlsUrl(streamUrl) && !hasVideoExtension(streamUrl)) {
            val resolved = ArabseedRepository.resolveDirectStream(targetUrl)
            if (!resolved.isNullOrBlank()) {
                streamUrl = resolved
            }
        }

        val isHls = isHlsUrl(streamUrl)
        val extension = if (isHls) "ts" else videoExtension(streamUrl) ?: "mp4"
        val mimeType = mimeTypeForExtension(extension)
        val fileName = "$cleanTitle.$extension"
        val (outputStream, uri, file) = createDownloadOutputStream(fileName, mimeType)
        activeOutputs[downloadId] = outputStream
        if (uri != null) activeUris[downloadId] = uri
        if (file != null) activeFiles[downloadId] = file

        var lastProgress = 0
        var lastNotifyTime = 0L

        fun updateProgress(current: Int) {
            val now = System.currentTimeMillis()
            if (current != lastProgress && (now - lastNotifyTime > 800 || current == 100)) {
                lastProgress = current
                lastNotifyTime = now
                val notif = NotificationCompat.Builder(this@ZakzoukaDownloadService, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done).setColor(0xFFD4AF37.toInt())
                    .setContentTitle("تنزيل: $cleanTitle")
                    .setContentText("جاري التنزيل: $current%")
                    .setProgress(100, current, false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(contentPendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء التنزيل", cancelPendingIntent)
                    .build()
                notificationManager.notify(downloadId, notif)
            }
        }

        try {
            if (isHls) {
                // HLS Stream Download
                downloadHlsStream(streamUrl, outputStream, userAgent, referer) { progress ->
                    ensureActive()
                    updateProgress(progress)
                }
            } else {
                // Progressive video download (MIME validation rejects source pages).
                downloadDirectStream(streamUrl, outputStream, userAgent, referer) { progress ->
                    ensureActive()
                    updateProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()

            // Mark completed in MediaStore
            if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                }
                contentResolver.update(uri, values, null, null)
            } else if (file != null) {
                MediaScannerConnection.scanFile(
                    this@ZakzoukaDownloadService,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            showCompleteNotification(downloadId, cleanTitle, uri, file, mimeType)
        } catch (e: Exception) {
            try { outputStream.close() } catch (_: Exception) {}
            if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { contentResolver.delete(uri, null, null) }
            }
            if (file != null) runCatching { file.delete() }
            if (e is CancellationException) throw e
            throw e
        } finally {
            activeOutputs.remove(downloadId)
            activeUris.remove(downloadId)
            activeFiles.remove(downloadId)
        }
    }

    private fun downloadDirectStream(
        url: String,
        outputStream: OutputStream,
        userAgent: String,
        referer: String,
        onProgress: (Int) -> Unit
    ) {
        val fallbackOrigin = try {
            val u = java.net.URI.create(url)
            val h = u.host
            if (!h.isNullOrBlank()) "${u.scheme ?: "https"}://$h/" else "https://arabseed.health/"
        } catch (_: Exception) { "https://arabseed.health/" }
        val requestReferer = referer.ifBlank { fallbackOrigin }
        val requestOrigin = originForHeader(requestReferer).ifBlank { originForHeader(url) }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent.ifBlank { ArabseedRepository.getPlayerUserAgent() })
            .header("Referer", requestReferer)
            .header("Origin", requestOrigin)
            .build()

        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("HTTP ${response.code}: تعذر الوصول لملف الفيديو")
            }

            val body = response.body ?: throw java.io.IOException("السيرفر لم يُرجع ملف فيديو")
            val contentType = body.contentType()?.toString()?.substringBefore(';')?.lowercase().orEmpty()
            if (contentType.contains("text/html") || contentType.contains("application/json")
                || contentType.contains("mpegurl") || contentType.startsWith("text/")) {
                throw java.io.IOException("السيرفر أعاد صفحة ويب وليس ملف فيديو مباشر")
            }
            if (contentType.isNotBlank() && !contentType.startsWith("video/")
                && !contentType.startsWith("audio/") && contentType != "application/octet-stream"
                && contentType != "binary/octet-stream") {
                throw java.io.IOException("نوع الملف غير مدعوم للتنزيل: $contentType")
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val buffer = ByteArray(16384)
            var bytesRead: Int

            body.byteStream().use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                        onProgress(percent.coerceIn(0, 99))
                    }
                }
            }
            onProgress(100)
        }
    }

    private fun downloadHlsStream(
        masterUrl: String,
        outputStream: OutputStream,
        userAgent: String,
        referer: String,
        onProgress: (Int) -> Unit
    ) {
        val fallbackOrigin = try {
            val u = java.net.URI.create(masterUrl)
            val h = u.host
            if (!h.isNullOrBlank()) "${u.scheme ?: "https"}://$h/" else "https://arabseed.health/"
        } catch (_: Exception) { "https://arabseed.health/" }
        val requestReferer = referer.ifBlank { fallbackOrigin }
        val requestUa = userAgent.ifBlank { ArabseedRepository.getPlayerUserAgent() }
        val requestOrigin = originForHeader(requestReferer).ifBlank { originForHeader(masterUrl) }

        val masterReq = Request.Builder()
            .url(masterUrl)
            .header("User-Agent", requestUa)
            .header("Referer", requestReferer)
            .header("Origin", requestOrigin)
            .build()

        val masterText = downloadClient.newCall(masterReq).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}: تعذر فتح قائمة HLS")
            response.body?.string().orEmpty()
        }
        if (!masterText.trimStart().startsWith("#EXTM3U")) {
            throw java.io.IOException("السيرفر لم يُرجع قائمة HLS صالحة")
        }

        // Choose the highest advertised variant, then download a complete, clear VOD playlist.
        var mediaUrl = masterUrl
        val lines = masterText.lines()
        val variants = mutableListOf<Pair<Long, String>>()
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                val bandwidth = Regex("(?i)BANDWIDTH=(\\d+)").find(line)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
                val candidate = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.trim().startsWith("#") }?.trim()
                if (!candidate.isNullOrBlank()) variants += bandwidth to candidate
            }
        }
        variants.maxByOrNull { it.first }?.second?.let { selected ->
            mediaUrl = resolveRelativeUrl(masterUrl, selected)
        }

        val mediaText = downloadClient.newCall(
            Request.Builder()
                .url(mediaUrl)
                .header("User-Agent", requestUa)
                .header("Referer", requestReferer)
                .header("Origin", requestOrigin)
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}: تعذر فتح قائمة حلقات الفيديو")
            response.body?.string().orEmpty()
        }
        if (!mediaText.trimStart().startsWith("#EXTM3U")) {
            throw java.io.IOException("قائمة HLS الفرعية غير صالحة")
        }
        if (Regex("(?im)^#EXT-X-KEY:(?![^\\r\\n]*METHOD=NONE)[^\\r\\n]*").containsMatchIn(mediaText)) {
            throw java.io.IOException("هذا المصدر يستخدم تشفير HLS ولا يدعم التنزيل المباشر")
        }
        if (mediaText.contains("#EXT-X-MAP", ignoreCase = true)) {
            throw java.io.IOException("صيغة HLS المجزأة غير مدعومة للتنزيل في هذا الإصدار")
        }
        if (mediaText.contains("#EXT-X-BYTERANGE", ignoreCase = true)) {
            throw java.io.IOException("تقسيم HLS بنطاقات بايت غير مدعوم للتنزيل في هذا الإصدار")
        }
        if (!mediaText.contains("#EXT-X-ENDLIST", ignoreCase = true)) {
            throw java.io.IOException("هذه قائمة بث مباشر وليست حلقة مكتملة للتنزيل")
        }

        val segmentUrls = mediaText.lineSequence()
            .map(String::trim)
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { resolveRelativeUrl(mediaUrl, it) }
            .toList()
        if (segmentUrls.isEmpty()) {
            throw java.io.IOException("قائمة HLS لا تحتوي على مقاطع فيديو")
        }

        val total = segmentUrls.size
        val buffer = ByteArray(16384)
        for ((index, segmentUrl) in segmentUrls.withIndex()) {
            downloadClient.newCall(
                Request.Builder()
                    .url(segmentUrl)
                    .header("User-Agent", requestUa)
                    .header("Referer", requestReferer)
                    .header("Origin", requestOrigin)
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}: فشل تنزيل مقطع HLS")
                val body = response.body ?: throw java.io.IOException("مقطع HLS فارغ")
                val contentType = body.contentType()?.toString()?.substringBefore(';')?.lowercase().orEmpty()
                if (contentType.contains("text/html") || contentType.contains("application/json") || contentType.startsWith("text/")) {
                    throw java.io.IOException("رابط مقطع HLS أعاد صفحة ويب بدلاً من الفيديو")
                }
                var written = 0L
                body.byteStream().use { input ->
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        outputStream.write(buffer, 0, count)
                        written += count
                    }
                }
                if (written == 0L) throw java.io.IOException("مقطع HLS فارغ")
            }
            val percent = (((index + 1) * 100) / total).coerceIn(0, 99)
            onProgress(percent)
        }
        onProgress(100)
    }

    private fun resolveRelativeUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                relativeUrl
            } else if (relativeUrl.startsWith("/")) {
                val uri = URI.create(baseUrl)
                "${uri.scheme}://${uri.host}$relativeUrl"
            } else {
                URI.create(baseUrl).resolve(relativeUrl).toString()
            }
        } catch (_: Exception) {
            relativeUrl
        }
    }

    private fun originForHeader(url: String): String = runCatching {
        val parsed = URI.create(url)
        val host = parsed.host.orEmpty()
        if (host.isBlank()) "" else "${parsed.scheme ?: "https"}://$host" +
            if (parsed.port >= 0) ":${parsed.port}" else ""
    }.getOrDefault("")

    private fun isHlsUrl(url: String): Boolean = url.contains(".m3u8", ignoreCase = true)

    private fun hasVideoExtension(url: String): Boolean = videoExtension(url) != null

    private fun videoExtension(url: String): String? {
        val path = runCatching { URI.create(url).path.orEmpty() }.getOrDefault("")
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension.takeIf { it in setOf("mp4", "mkv", "webm", "m4v", "mov", "avi", "ts") }
    }

    private fun mimeTypeForExtension(extension: String): String = when (extension.lowercase()) {
        "ts" -> "video/mp2t"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        else -> "video/mp4"
    }

    private fun createDownloadOutputStream(fileName: String, mimeType: String): Triple<OutputStream, Uri?, File?> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    val stream = contentResolver.openOutputStream(uri)
                    if (stream != null) {
                        return Triple(stream, uri, null)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ZakzoukaDownload", "MediaStore failed, using direct file fallback", e)
            }
        }

        // Direct file fallback
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads.exists() || publicDownloads.mkdirs()) {
                val file = File(publicDownloads, fileName)
                val stream = FileOutputStream(file)
                return Triple(stream, null, file)
            }
        } catch (_: Exception) {}

        val appDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        val file = File(appDir, fileName)
        val stream = FileOutputStream(file)
        return Triple(stream, null, file)
    }

    private fun showCompleteNotification(
        downloadId: Int,
        cleanTitle: String,
        uri: Uri?,
        file: File?,
        mimeType: String,
    ) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            if (uri != null) {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else if (file != null) {
                setDataAndType(Uri.fromFile(file), mimeType)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val openPendingIntent = PendingIntent.getActivity(
            this,
            downloadId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeNotif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done).setColor(0xFFD4AF37.toInt())
            .setContentTitle("تم اكتمال التنزيل بنجاح ✔")
            .setContentText("$cleanTitle جاهز في مجلد التنزيلات (Downloads)")
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .build()

        notificationManager.notify(downloadId, completeNotif)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "✔ تم تنزيل $cleanTitle بنجاح في مجلد التنزيلات!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showErrorNotification(downloadId: Int, cleanTitle: String, error: String) {
        val msg = if (error.isNotBlank() && error != "null") error else "حدث خطأ غير متوقع أثناء التنزيل"
        val errorNotif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done).setColor(0xFFD4AF37.toInt())
            .setContentTitle("تعذر تنزيل: $cleanTitle")
            .setContentText(msg)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        notificationManager.notify(downloadId, errorNotif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنزيلات زقزوقة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم وتنزيل الفيديوهات والحلقات"
                enableVibration(false)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "zakzouka_video_downloads"
        const val ACTION_START = "com.madar.tv.action.START_DOWNLOAD"
        const val ACTION_CANCEL = "com.madar.tv.action.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_USER_AGENT = "extra_user_agent"
        const val EXTRA_REFERER = "extra_referer"

        private val idGenerator = AtomicInteger(1000)

        fun startDownload(
            context: Context,
            title: String,
            url: String,
            userAgent: String = "",
            referer: String = "",
        ) {
            val downloadId = idGenerator.incrementAndGet()
            val intent = Intent(context, ZakzoukaDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_USER_AGENT, userAgent)
                putExtra(EXTRA_REFERER, referer)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
