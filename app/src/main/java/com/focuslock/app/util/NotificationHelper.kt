package com.focuslock.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.focuslock.app.R
import com.focuslock.app.ui.SessionsActivity

object NotificationHelper {
    const val TIMER_ID = 1001
    private const val TIMER_CHANNEL = "focus_timer"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TIMER_CHANNEL,
                context.getString(R.string.notification_channel_timer),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.notification_channel_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun build(context: Context, name: String, remainingMs: Long): Notification {
        ensureChannel(context)
        val intent = PendingIntent.getActivity(
            context, 2, Intent(context, SessionsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val seconds = (remainingMs / 1000).coerceAtLeast(0)
        val time = "%02d:%02d".format(seconds / 60, seconds % 60)
        return NotificationCompat.Builder(context, TIMER_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(name.ifBlank { context.getString(R.string.session_active) })
            .setContentText("$time · ${context.getString(R.string.session_active)}")
            .setContentIntent(intent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun update(context: Context, name: String, remainingMs: Long) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.notify(TIMER_ID, build(context, name, remainingMs))
        } catch (e: Exception) {
            // Notification channel may not be ready yet.
        }
    }
}
