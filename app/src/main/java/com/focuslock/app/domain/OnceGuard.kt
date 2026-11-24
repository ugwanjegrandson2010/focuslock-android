package com.focuslock.app.domain

import java.util.concurrent.atomic.AtomicBoolean

/** Ensures an idempotent operation runs at most once until explicitly reset. */
class OnceGuard {
    private val claimed = AtomicBoolean(false)

    fun runOnce(action: () -> Unit): Boolean {
        if (!claimed.compareAndSet(false, true)) return false
        action()
        return true
    }

    suspend fun runOnceSuspend(action: suspend () -> Unit): Boolean {
        if (!claimed.compareAndSet(false, true)) return false
        action()
        return true
    }

    fun reset() {
        claimed.set(false)
    }
}
