package com.focuslock.app

import android.app.Application
import android.content.Context
import com.focuslock.app.audio.MusicController
import com.focuslock.app.blocking.BlockingCoordinator
import com.focuslock.app.data.FocusDatabase
import com.focuslock.app.data.FocusRepository
import com.focuslock.app.data.PinManager
import com.focuslock.app.data.Prefs
import com.focuslock.app.domain.SessionManager
import com.focuslock.app.timer.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database = FocusDatabase.get(appContext)
    val repository = FocusRepository(database)
    val prefs = Prefs(appContext)
    val pinManager = PinManager()
    val blocker = BlockingCoordinator(appContext, repository)
    val alarms = AlarmScheduler(appContext)
    val musicController = MusicController(appContext)
    val sessionManager = SessionManager(appContext, repository, prefs, pinManager, blocker, alarms)
}

class FocusLockApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            try {
                container.sessionManager.prepare()
            } catch (e: Exception) {
                // Recovery will be retried by the boot/deadline receivers.
            }
        }
    }
}
