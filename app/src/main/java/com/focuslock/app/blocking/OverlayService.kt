package com.focuslock.app.blocking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.focuslock.app.R

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra(EXTRA_NAME).orEmpty()
        showOverlay(name)
        return START_STICKY
    }

    private fun showOverlay(name: String) {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) return
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)
        val label = view.findViewById<TextView>(R.id.overlay_message)
        label.text = if (name.isBlank()) {
            getString(R.string.session_active)
        } else {
            getString(R.string.session_active) + "\n" + name
        }
        view.setOnTouchListener { _, _ -> true }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        try {
            windowManager?.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            overlayView = null
        }
    }

    override fun onDestroy() {
        overlayView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }
        overlayView = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NAME = "overlay_session_name"
        fun start(context: Context, name: String) {
            val intent = Intent(context, OverlayService::class.java).putExtra(EXTRA_NAME, name)
            context.startService(intent)
        }
    }
}
