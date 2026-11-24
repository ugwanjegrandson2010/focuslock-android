package com.focuslock.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focuslock.app.FocusLockApp
import com.focuslock.app.R
import com.focuslock.app.ui.SessionsActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as FocusLockApp
        when (intent?.action) {
            ACTION_STOP -> {
                app.container.musicController.stopWithFade()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val profileId = intent?.getLongExtra(EXTRA_PROFILE_ID, -1L) ?: -1L
                startForeground(NOTIFICATION_ID, buildNotification())
                startMusic(app, profileId)
            }
        }
        return START_STICKY
    }

    private fun startMusic(app: FocusLockApp, profileId: Long) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()).launch {
            val profile = if (profileId > 0) app.container.repository.getProfile(profileId) else null
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                app.container.musicController.play(profile)
            }
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.notification_channel_audio), NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, SessionsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.session_active))
            .setContentText(getString(R.string.music))
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.stop_music), stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "focus_audio"
        private const val NOTIFICATION_ID = 1002
        const val EXTRA_PROFILE_ID = "profile_id"
        const val ACTION_STOP = "com.focuslock.app.STOP_MUSIC"

        fun start(context: Context, profileId: Long?) {
            val intent = Intent(context, MusicService::class.java).apply {
                if (profileId != null) putExtra(EXTRA_PROFILE_ID, profileId)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MusicService::class.java).setAction(ACTION_STOP))
        }
    }
}
