package com.madar.tv.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.madar.tv.MainActivity
import com.madar.tv.R

object NotificationHelper {

    const val CHANNEL_NEW_CONTENT = "ch_new_content"
    const val CHANNEL_MATCHES     = "ch_matches"

    private var notifId = 1000

    private fun getAppLogoBitmap(context: Context): Bitmap? {
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.zakzouka_logo)
        }.getOrNull()
    }

    // ── Create channels (call once at app start) ──────────────────────────────

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_NEW_CONTENT,
                    "محتوى جديد",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "إشعارات الأفلام والمسلسلات الجديدة"
                }
            )

            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MATCHES,
                    "مباريات الكرة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات المباريات المباشرة والقادمة"
                    enableVibration(true)
                }
            )
        }
    }

    // ── Send notification for new movie/episode with its actual name ──────────

    fun notifyNewContent(
        context: Context,
        title: String,
        body: String,
        isMovie: Boolean = true
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, notifId++,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = if (isMovie) "🎬" else "📺"
        val header = if (isMovie) "فيلم جديد" else "مسلسل جديد"
        val notifTitle = "$emoji $header: $title"
        val largeIcon = getAppLogoBitmap(context)

        val notif = NotificationCompat.Builder(context, CHANNEL_NEW_CONTENT)
            .setSmallIcon(R.drawable.ic_notification)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle(notifTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$notifTitle\n$body"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (_: SecurityException) { }
    }

    // ── Send notification for MATCH START (when match kicks off) ──────────────

    fun notifyMatchStart(
        context: Context,
        homeTeam: String,
        awayTeam: String,
        league: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("tab", 1) // go to matches tab
        }
        val pending = PendingIntent.getActivity(
            context, notifId++,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🔴 بدأت الآن: $homeTeam vs $awayTeam"
        val body  = "انطلقت المباراة الآن | $league ⚽ شاهد البث المباشر على زقزوقة"
        val largeIcon = getAppLogoBitmap(context)

        val notif = NotificationCompat.Builder(context, CHANNEL_MATCHES)
            .setSmallIcon(R.drawable.ic_notification)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setColor(0xFFE50914.toInt()) // Red for Live
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (_: SecurityException) { }
    }

    // ── Send notification for upcoming match (15–30 min before kick-off) ─────

    fun notifyMatch(
        context: Context,
        homeTeam: String,
        awayTeam: String,
        timeStr: String,
        league: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("tab", 1) // go to football tab
        }
        val pending = PendingIntent.getActivity(
            context, notifId++,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚽ اقتربت المباراة: $homeTeam vs $awayTeam"
        val body  = "تنطلق الساعة $timeStr | $league ⏰ اضغط للتفاصيل والقنوات الناقلة"
        val largeIcon = getAppLogoBitmap(context)

        val notif = NotificationCompat.Builder(context, CHANNEL_MATCHES)
            .setSmallIcon(R.drawable.ic_notification)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (_: SecurityException) { }
    }

    // ── Batch: notify multiple new items with their actual titles ─────────────

    fun notifyNewBatch(
        context: Context,
        titles: List<String>,
        isMovie: Boolean
    ) {
        if (titles.isEmpty()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, notifId++,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = if (isMovie) "🎬" else "📺"
        val typeLabel = if (isMovie) "أفلام جديدة" else "مسلسلات جديدة"
        val primaryTitle = titles.first()
        val notifTitle = if (titles.size == 1) {
            "$emoji $typeLabel: $primaryTitle"
        } else {
            "$emoji $typeLabel: $primaryTitle و ${titles.size - 1} آخرين"
        }

        val bigText = buildString {
            append("أحدث الإضافات على تطبيق زقزوقة:\n")
            titles.take(5).forEach { t ->
                append("• $t\n")
            }
            if (titles.size > 5) {
                append("+ ${titles.size - 5} أعمال أخرى جديدة")
            }
        }.trimEnd()

        val largeIcon = getAppLogoBitmap(context)

        val notif = NotificationCompat.Builder(context, CHANNEL_NEW_CONTENT)
            .setSmallIcon(R.drawable.ic_notification)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle(notifTitle)
            .setContentText(if (titles.size == 1) primaryTitle else "$primaryTitle و ${titles.size - 1} أعمال جديدة")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (_: SecurityException) { }
    }
}
