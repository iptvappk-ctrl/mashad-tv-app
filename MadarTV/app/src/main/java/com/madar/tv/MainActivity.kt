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
import com.madar.tv.core.MashadTelemetry
import com.madar.tv.core.NotificationHelper
import com.madar.tv.data.ContentSyncWorker
import com.madar.tv.data.MatchAlertWorker
import com.madar.tv.ui.MadarApp
import com.madar.tv.ui.theme.MadarTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) scheduleWorkers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MashadTelemetry.init(this)

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

        // ── Content Sync: every 30 minutes ───────────────────────────────────
        val contentSync = PeriodicWorkRequestBuilder<ContentSyncWorker>(
            30, TimeUnit.MINUTES
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
