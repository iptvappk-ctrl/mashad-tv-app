package com.madar.tv

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.work.*
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.madar.tv.core.ZakzoukaTelemetry
import com.madar.tv.core.NotificationHelper
import com.madar.tv.data.CatalogDiskCache
import com.madar.tv.data.ContentSyncWorker
import com.madar.tv.data.MatchAlertWorker
import com.madar.tv.ui.MadarApp
import com.madar.tv.ui.theme.MadarTheme
import java.io.File
import java.util.concurrent.TimeUnit
import okio.Path.Companion.toPath

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) scheduleWorkers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CatalogDiskCache.initialize(applicationContext)
        val imageClient = okhttp3.OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                val referer = when {
                    url.contains("myseed") -> "https://m.myseed.tv/"
                    url.contains("arabseed") -> "https://arabseed.health/"
                    url.contains("akw") -> "https://akw.cam/"
                    url.contains("wecima") || url.contains("myweecima") -> "https://myweecima.casa/"
                    url.contains("fasel") -> "https://wb49584.fasel4k.site/"
                    else -> "https://m.myseed.tv/"
                }
                val newReq = req.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.6668.100 Mobile Safari/537.36")
                    .header("Referer", referer)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(newReq)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        SingletonImageLoader.setSafe { imageContext ->
            ImageLoader.Builder(imageContext)
                .components {
                    add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(imageContext, 0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(imageContext.cacheDir, "poster-cache").absolutePath.toPath())
                        .maxSizeBytes(120L * 1024 * 1024)
                        .build()
                }
                .build()
        }
        ZakzoukaTelemetry.init(this)

        // Create notification channels
        NotificationHelper.createChannels(this)

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                scheduleWorkers()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleWorkers()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            MadarTheme {
                MadarApp()
            }
        }
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // ── Content Sync: every 60 minutes ───────────────────────────────────
        val contentSync = PeriodicWorkRequestBuilder<ContentSyncWorker>(
            60, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "content_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            contentSync
        )

        // ── Match Alerts: every 15 minutes ───────────────────────────────────
        val matchAlert = PeriodicWorkRequestBuilder<MatchAlertWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "match_alerts",
            ExistingPeriodicWorkPolicy.KEEP,
            matchAlert
        )

    }

}
