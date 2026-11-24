package com.focuslock.app.domain

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.focuslock.app.audio.MusicService
import com.focuslock.app.blocking.BlockingCoordinator
import com.focuslock.app.data.BlockedAppEntity
import com.focuslock.app.data.FocusRepository
import com.focuslock.app.data.PinManager
import com.focuslock.app.data.Prefs
import com.focuslock.app.data.SessionEntity
import com.focuslock.app.data.SessionStatus
import com.focuslock.app.timer.AlarmScheduler
import com.focuslock.app.timer.TimerService
import com.focuslock.app.ui.FocusActivity
import com.focuslock.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionManager(
    private val context: Context,
    val repository: FocusRepository,
    private val prefs: Prefs,
    private val pinManager: PinManager,
    private val blocker: BlockingCoordinator,
    private val alarms: AlarmScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(ActiveState())
    val state: StateFlow<ActiveState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    private var currentSessionId: Long = -1L
    private var currentDeadline: Long = 0L
    private var activeSessionName: String = ""
    private val finishGuard = OnceGuard()
    private val activationGuard = OnceGuard()
    private var tickJob: Job? = null
    private var rules: List<BlockedAppEntity> = emptyList()
    private var markers: List<String> = emptyList()
    private var pinHash: String? = null
    private var pinSalt: String? = null

    init {
        scope.launch {
            prefs.markers.collect { markers = it.toList() }
        }
        scope.launch {
            prefs.pinInfo.collect { pair ->
                pinHash = pair.first
                pinSalt = pair.second
            }
        }
    }

    fun isActiveNow(): Boolean = _state.value.isActive

    fun currentSessionName(): String = _state.value.session?.name.orEmpty()

    fun isFullAppBlocked(packageName: String): Boolean =
        rules.any { it.packageName == packageName && it.kind == "FULL" }

    fun hasFeatureTargets(): Boolean = rules.any { it.kind == "FEATURE" }

    fun isFeatureTarget(packageName: String): Boolean =
        rules.any { it.packageName == packageName && it.kind == "FEATURE" }

    fun featureMarkers(): List<String> = markers

    suspend fun prepare() {
        restoreAfterBoot()
    }

    suspend fun start(config: SessionConfig): Long {
        require(config.name.isNotBlank()) { "Session name is required" }
        require(config.durationMs > 0) { "Duration must be positive" }

        val now = System.currentTimeMillis()
        val scheduled = config.scheduledStartAt?.takeIf { it > now }
        val session = SessionEntity(
            name = config.name.trim(),
            scheduledStartAt = scheduled,
            startedAt = if (scheduled == null) now else null,
            deadlineElapsed = if (scheduled == null) SystemClock.elapsedRealtime() + config.durationMs else null,
            plannedDurationMs = config.durationMs,
            status = if (scheduled == null) SessionStatus.ACTIVE else SessionStatus.SCHEDULED,
            screensaverStyle = config.screensaverStyle.name,
            musicProfileId = config.musicProfileId,
            createdAt = now
        )
        val id = repository.createSession(session)

        val blocked = config.blockedPackages.distinct().map {
            BlockedAppEntity(sessionId = id, packageName = it, kind = "FULL")
        }
        val feature = config.featureTargets.distinct().map {
            BlockedAppEntity(sessionId = id, packageName = it, kind = "FEATURE")
        }
        repository.saveBlockedApps(blocked + feature)

        if (!config.pin.isNullOrBlank()) {
            val salt = pinManager.newSalt()
            prefs.setPin(pinManager.hash(config.pin, salt), salt)
        }

        if (scheduled != null) {
            alarms.scheduleStart(id, scheduled)
        } else {
            activate(id, session.copy(id = id))
        }
        return id
    }

    suspend fun startScheduled(id: Long) {
        val session = repository.getSession(id) ?: return
        if (session.status != SessionStatus.SCHEDULED) return
        val updated = session.copy(
            status = SessionStatus.ACTIVE,
            startedAt = System.currentTimeMillis(),
            deadlineElapsed = SystemClock.elapsedRealtime() + session.plannedDurationMs
        )
        repository.updateSession(updated)
        activate(id, updated)
    }

    suspend fun onDeadline() {
        val active = repository.activeSession() ?: return
        val deadline = active.deadlineElapsed ?: return
        if (FocusMath.isDeadlineReached(deadline, SystemClock.elapsedRealtime())) {
            finish(SessionStatus.COMPLETED)
        } else {
            alarms.scheduleDeadline(active.id, deadline)
        }
    }

    suspend fun restoreAfterBoot() {
        prefs.activeSessionId.first()?.let { id ->
            val session = repository.getSession(id)
            if (session != null && session.status == SessionStatus.ACTIVE) {
                val deadline = session.deadlineElapsed ?: 0L
                if (deadline <= SystemClock.elapsedRealtime()) {
                    finish(SessionStatus.COMPLETED)
                } else {
                    activate(id, session)
                }
                return
            }
        }
        // Catch scheduled sessions whose start time passed while the device was off.
        repository.allSessions.first()
            .filter { it.status == SessionStatus.SCHEDULED && (it.scheduledStartAt ?: Long.MAX_VALUE) <= System.currentTimeMillis() }
            .forEach { startScheduled(it.id) }
    }

    private suspend fun activate(id: Long, session: SessionEntity) {
        if (_state.value.isActive && currentSessionId == id) return
        if (!activationGuard.runOnceSuspend { }) return
        try {
        finishGuard.reset()
        currentSessionId = id
        currentDeadline = session.deadlineElapsed ?: 0L
        activeSessionName = session.name
        rules = repository.getBlockedApps(id)
        markers = prefs.markers.first().toList()

        prefs.setActiveSessionId(id)
        val remaining = (session.deadlineElapsed ?: SystemClock.elapsedRealtime()) - SystemClock.elapsedRealtime()

        _state.value = ActiveState(
            session = session,
            remainingMs = remaining,
            isActive = true,
            isFullMode = blocker.isFullMode()
        )

        TimerService.start(context, session.name, remaining)
        MusicService.start(context, session.musicProfileId)
        blocker.apply(session)
        session.deadlineElapsed?.let { alarms.scheduleDeadline(id, it) }

        val focusIntent = Intent(context, FocusActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(FocusActivity.EXTRA_SESSION_ID, id)
        }
        context.startActivity(focusIntent)

        _events.tryEmit(SessionEvent.Started)
        startTicking()
        } catch (e: Exception) {
            throw e
        } finally {
            activationGuard.reset()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val remaining = FocusMath.remainingMs(currentDeadline, SystemClock.elapsedRealtime())
                _state.value = _state.value.copy(remainingMs = remaining)
                NotificationHelper.update(context, activeSessionName, remaining)
                if (remaining == 0L) {
                    finish(SessionStatus.COMPLETED)
                    break
                }
                delay(1000)
            }
        }
    }

    suspend fun finish(status: String = SessionStatus.COMPLETED, byPin: Boolean = false) {
        val session = repository.getSession(currentSessionId)
            ?: repository.activeSession()
            ?: return
        if (session.status != SessionStatus.ACTIVE) return
        if (session.finishStarted) return
        if (!finishGuard.runOnceSuspend { }) return

        try {
        repository.updateSession(session.copy(finishStarted = true))
        tickJob?.cancel()

        MusicService.stop(context)
        blocker.restore(session)
        alarms.cancel(session.id)
        prefs.setActiveSessionId(null)
        context.stopService(Intent(context, TimerService::class.java))

        val actual = FocusMath.actualDurationMs(
            session.plannedDurationMs,
            session.deadlineElapsed ?: SystemClock.elapsedRealtime(),
            SystemClock.elapsedRealtime()
        )

        repository.updateSession(
            session.copy(
                status = status,
                actualDurationMs = actual,
                completedAt = System.currentTimeMillis(),
                finishStarted = true,
                abortedByPin = byPin
            )
        )

        _state.value = ActiveState()
        _events.tryEmit(SessionEvent.Finished(session.id, status))
        _events.tryEmit(SessionEvent.NeedsHome)

        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(home) } catch (e: Exception) {}
        } catch (e: Exception) {
            finishGuard.reset()
            throw e
        }
    }

    fun abortWithPin(pin: String): Boolean {
        val hash = pinHash ?: return false
        val salt = pinSalt ?: return false
        if (!pinManager.verify(pin, salt, hash)) return false
        scope.launch { finish(SessionStatus.ABORTED, byPin = true) }
        return true
    }

    fun onBlockDetected(packageName: String, kind: String) {
        val id = currentSessionId
        if (id < 0) return
        scope.launch {
            repository.logBlock(id, packageName, kind)
            val count = repository.blockCount(id)
            _state.value = _state.value.copy(
                blockedAttempts = count,
                lastBlockedPackage = packageName
            )
            _events.tryEmit(SessionEvent.Blocked(packageName, kind))
        }
    }

    fun clearCurrent() {
        currentSessionId = -1
        currentDeadline = 0L
        activeSessionName = ""
        rules = emptyList()
        _state.value = ActiveState()
    }
}
