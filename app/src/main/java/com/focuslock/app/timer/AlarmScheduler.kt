package com.focuslock.app.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.focuslock.app.util.Permissions

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDeadline(sessionId: Long, deadlineElapsedRealtime: Long) {
        val triggerAt = System.currentTimeMillis() +
            (deadlineElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        val intent = Intent(context, DeadlineReceiver::class.java).putExtra(EXTRA_ID, sessionId)
        val pi = PendingIntent.getBroadcast(
            context, sessionId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExact(triggerAt, pi)
    }

    fun scheduleStart(sessionId: Long, startAtWallClock: Long) {
        val intent = Intent(context, SessionStartReceiver::class.java).putExtra(EXTRA_ID, sessionId)
        val pi = PendingIntent.getBroadcast(
            context, sessionId.toInt() + 1_000_000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExact(startAtWallClock, pi)
    }

    fun cancel(sessionId: Long) {
        val deadline = PendingIntent.getBroadcast(
            context, sessionId.toInt(), Intent(context, DeadlineReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val start = PendingIntent.getBroadcast(
            context, sessionId.toInt() + 1_000_000, Intent(context, SessionStartReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(deadline)
        alarmManager.cancel(start)
    }

    private fun scheduleExact(triggerAt: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Permissions.canScheduleExactAlarms(context)) {
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000, pi)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    companion object {
        const val EXTRA_ID = "session_id"
    }
}
