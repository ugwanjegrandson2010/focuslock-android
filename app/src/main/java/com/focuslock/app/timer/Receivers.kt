package com.focuslock.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.focuslock.app.FocusLockApp

class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as? FocusLockApp
        if (app == null) { pending.finish(); return }
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try { app.container.sessionManager.onDeadline() } finally { pending.finish() }
        }
    }
}

class SessionStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        val pending = goAsync()
        val app = context.applicationContext as? FocusLockApp
        if (app == null) { pending.finish(); return }
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try { app.container.sessionManager.startScheduled(id) } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as? FocusLockApp
        if (app == null) { pending.finish(); return }
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try { app.container.sessionManager.restoreAfterBoot() } finally { pending.finish() }
        }
    }
}
