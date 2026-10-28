package com.madar.tv.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.madar.tv.MainActivity
import com.madar.tv.R

object NotificationHelper {

    const val CHANNEL_NEW_CONTENT = "ch_new_content"
    const val CHANNEL_MATCHES     = "ch_matches"

    private var notifId = 1000

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
                    description = "إشعارات المباريات القادمة"
                    enableVibration(true)
                }
            )
        }
    }

    // ── Send notification for new movie/episode ───────────────────────────────

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
            context, notifId,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = if (isMovie) "🎬" else "📺"
        val notif = NotificationCompat.Builder(context, CHANNEL_NEW_CONTENT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$emoji $title")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (_: SecurityException) { /* permission not granted */ }
    }

    // ── Send notification for upcoming match ──────────────────────────────────

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
            context, notifId,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚽ $homeTeam vs $awayTeam"
        val body  = "يبدأ الماتش الساعة $timeStr | $league"

        val notif = NotificationCompat.Builder(context, CHANNEL_MATCHES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (_: SecurityException) { /* permission not granted */ }
    }

    // ── Batch: notify multiple new items grouped ──────────────────────────────

    fun notifyNewBatch(context: Context, count: Int, type: String) {
        if (count <= 0) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, notifId,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = "🆕 $count $type جديد${if (count > 1) "ة" else ""} أُضيف${if (count > 1) "ت" else ""}"
        val body  = "اضغط لمشاهدة المحتوى الجديد"

        val notif = NotificationCompat.Builder(context, CHANNEL_NEW_CONTENT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (_: SecurityException) {}
    }
}
