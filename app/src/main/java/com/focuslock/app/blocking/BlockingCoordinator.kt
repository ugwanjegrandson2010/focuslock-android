package com.focuslock.app.blocking

import android.content.Context
import android.content.Intent
import com.focuslock.app.data.FocusRepository
import com.focuslock.app.data.SessionEntity
import com.focuslock.app.data.SessionStatus

class BlockingCoordinator(
    private val context: Context,
    private val repository: FocusRepository
) {
    private val disabler = AppDisabler(context)

    fun isFullMode(): Boolean = disabler.isOwner()

    suspend fun apply(session: SessionEntity) {
        val rules = repository.getBlockedApps(session.id)
        if (isFullMode()) {
            rules.filter { it.kind == "FULL" }.forEach { rule ->
                val original = disabler.disable(rule.packageName)
                if (original != null) {
                    repository.saveOriginalState(session.id, rule.packageName, original)
                }
            }
            disabler.setupLockTask()
        } else {
            OverlayService.start(context, session.name)
        }
    }

    suspend fun restore(session: SessionEntity) {
        if (isFullMode()) {
            val rules = repository.getBlockedApps(session.id)
            rules.forEach { rule ->
                rule.originalEnabledState?.let { state ->
                    disabler.restore(rule.packageName, state)
                }
            }
        }
        context.stopService(Intent(context, OverlayService::class.java))
    }

    fun isBlocked(packageName: String, rules: List<com.focuslock.app.data.BlockedAppEntity>): Boolean =
        rules.any { it.packageName == packageName && it.kind == "FULL" }
}
