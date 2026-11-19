package com.madar.tv.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.madar.madar_flutter.R

internal object NotificationCenter {
    const val CONTENT_CHANNEL = "new_content_v1"
    const val MATCH_CHANNEL = "match_alerts_v1"
    const val UPDATE_CHANNEL = "app_updates_v2"
    const val UPDATE_NOTIFICATION_ID = 7001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(UPDATE_CHANNEL, "تحديثات التطبيق", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "إشعارات التحديثات والإصدارات الجديدة والتطويرات لتطبيق زقزوقة"
                    enableVibration(true)
                    enableLights(true)
                },
                NotificationChannel(CONTENT_CHANNEL, "أعمال جديدة", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "تنبيه عند ظهور أفلام أو مسلسلات جديدة في المصادر المتاحة"
                    enableVibration(true)
                },
                NotificationChannel(MATCH_CHANNEL, "المباريات", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "تنبيه قبل بداية المباريات وعند رصد بدايتها"
                    enableVibration(true)
                    enableLights(true)
                },
            ),
        )
    }

    fun areNotificationsAllowed(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun isChannelAllowed(context: Context, channel: String): Boolean {
        if (!areNotificationsAllowed(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun postUpdateNotification(context: Context, versionName: String, releaseNotes: String): Boolean {
        val title = "🚀 تحديث جديد متوفر: زقزوقة $versionName"
        val body = if (releaseNotes.isNotBlank()) releaseNotes else "يتوفر إصدار أحدث لتحسين استقرار التطبيق والمشاهدة."
        return post(
            context = context,
            id = UPDATE_NOTIFICATION_ID,
            channel = UPDATE_CHANNEL,
            title = title,
            body = body,
            highPriority = true,
        )
    }

    fun post(context: Context, id: Int, channel: String, title: String, body: String, highPriority: Boolean = false): Boolean {
        if (!isChannelAllowed(context, channel)) return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(
                context,
                id,
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_brand)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        }.getOrDefault(false)
    }
}
