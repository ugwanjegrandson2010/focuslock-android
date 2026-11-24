package com.focuslock.app

import com.focuslock.app.data.SessionStatus
import com.focuslock.app.domain.ActiveState
import com.focuslock.app.domain.FocusMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Simulates the timing decisions of a focus session using the same pure helpers
 * that the production SessionManager uses. No Android runtime is required.
 */
class SessionTimelineSimulationTest {

    private data class Clock(var elapsed: Long, var wall: Long)

    @Test
    fun one_hour_session_with_midpoint_reboot_finishes_exactly_once() {
        val clock = Clock(elapsed = 10_000L, wall = 1_700_000_000_000L)
        val duration = 60 * 60 * 1000L
        val deadline = clock.elapsed + duration

        // t0: start
        var state = ActiveState(isActive = true, remainingMs = FocusMath.remainingMs(deadline, clock.elapsed))
        assertEquals(duration, state.remainingMs)

        // t0 + 20m: reboot; deadline persisted, session restored
        clock.elapsed += 20 * 60 * 1000L
        clock.wall += 20 * 60 * 1000L
        assertTrue(state.isActive)
        assertEquals(40 * 60 * 1000L, FocusMath.remainingMs(deadline, clock.elapsed))

        // simulated alarm conversion must fire at the original deadline
        val alarmAt = FocusMath.deadlineToWallClock(deadline, clock.elapsed, clock.wall)
        assertEquals(1_700_000_000_000L + duration, alarmAt)

        // deadline reached
        clock.elapsed = deadline
        assertTrue(FocusMath.isDeadlineReached(deadline, clock.elapsed))
        val actual = FocusMath.actualDurationMs(duration, deadline, clock.elapsed)
        assertEquals(duration, actual)

        state = ActiveState()
        assertFalse(state.isActive)
    }

    @Test
    fun early_alarm_does_not_finish_the_session() {
        val deadline = 100_000L
        assertFalse(FocusMath.isDeadlineReached(deadline, 90_000L))
        assertTrue(FocusMath.remainingMs(deadline, 90_000L) > 0)
    }

    @Test
    fun scheduled_session_start_is_converted_with_elapsed_realtime() {
        val nowElapsed = 5_000L
        val startElapsed = 305_000L
        val nowWall = 1_700_000_000_000L
        val startWall = FocusMath.deadlineToWallClock(startElapsed, nowElapsed, nowWall)
        assertEquals(nowWall + 300_000L, startWall)
        assertEquals(SessionStatus.SCHEDULED, SessionStatus.SCHEDULED)
    }
}
