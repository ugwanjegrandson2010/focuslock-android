package com.focuslock.app.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.focuslock.app.util.NotificationHelper

class TimerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra(EXTRA_NAME).orEmpty()
        val remaining = intent?.getLongExtra(EXTRA_REMAINING, 0L) ?: 0L
        NotificationHelper.ensureChannel(this)
        startForeground(NotificationHelper.TIMER_ID, NotificationHelper.build(this, name, remaining))
        return START_STICKY
    }

    companion object {
        const val EXTRA_NAME = "session_name"
        const val EXTRA_REMAINING = "remaining_ms"
        fun start(context: Context, name: String, remainingMs: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_REMAINING, remainingMs)
            }
            context.startForegroundService(intent)
        }
    }
}
