package com.focuslock.app.domain

/** Pure timing helpers, kept free of Android dependencies so they can be unit tested. */
object FocusMath {
    fun remainingMs(deadlineElapsedRealtime: Long, nowElapsedRealtime: Long): Long =
        (deadlineElapsedRealtime - nowElapsedRealtime).coerceAtLeast(0L)

    fun actualDurationMs(plannedDurationMs: Long, deadlineElapsedRealtime: Long, nowElapsedRealtime: Long): Long =
        (plannedDurationMs - remainingMs(deadlineElapsedRealtime, nowElapsedRealtime))
            .coerceIn(0L, plannedDurationMs)

    fun deadlineToWallClock(deadlineElapsedRealtime: Long, nowElapsedRealtime: Long, nowWallClock: Long): Long =
        nowWallClock + (deadlineElapsedRealtime - nowElapsedRealtime)

    fun isDeadlineReached(deadlineElapsedRealtime: Long, nowElapsedRealtime: Long): Boolean =
        nowElapsedRealtime >= deadlineElapsedRealtime
}
