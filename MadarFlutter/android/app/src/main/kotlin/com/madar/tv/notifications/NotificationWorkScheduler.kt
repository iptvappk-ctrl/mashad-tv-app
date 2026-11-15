package com.madar.tv.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object NotificationWorkScheduler {
    private const val CONTENT_WORK = "madar-new-content-notifications-v1"
    private const val MATCH_WORK = "madar-match-notifications-v1"
    private const val UPDATE_WORK = "madar-app-update-notifications-v1"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val updateWork = PeriodicWorkRequestBuilder<AppUpdateNotificationWorker>(2, TimeUnit.HOURS)
            .setInitialDelay(2, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        val content = PeriodicWorkRequestBuilder<NewContentNotificationWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        val matches = PeriodicWorkRequestBuilder<MatchAlertNotificationWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UPDATE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            updateWork,
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CONTENT_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            content,
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MATCH_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            matches,
        )
    }
}
