package com.focuslock.app

import com.focuslock.app.domain.FocusMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusMathTest {

    @Test
    fun remaining_is_clamped_to_zero_after_deadline() {
        assertEquals(5_000L, FocusMath.remainingMs(10_000L, 5_000L))
        assertEquals(0L, FocusMath.remainingMs(10_000L, 12_000L))
    }

    @Test
    fun deadline_detection_uses_elapsed_realtime() {
        assertFalse(FocusMath.isDeadlineReached(10_000L, 9_999L))
        assertTrue(FocusMath.isDeadlineReached(10_000L, 10_000L))
        assertTrue(FocusMath.isDeadlineReached(10_000L, 15_000L))
    }

    @Test
    fun actual_duration_never_exceeds_planned_and_never_negative() {
        val planned = 60_000L
        val deadline = 100_000L
        assertEquals(0L, FocusMath.actualDurationMs(planned, deadline, 40_000L))
        assertEquals(30_000L, FocusMath.actualDurationMs(planned, deadline, 70_000L))
        assertEquals(planned, FocusMath.actualDurationMs(planned, deadline, 110_000L))
        assertEquals(0L, FocusMath.actualDurationMs(planned, deadline, 0L))
    }

    @Test
    fun deadline_conversion_to_wall_clock() {
        assertEquals(1_700_000_100_000L, FocusMath.deadlineToWallClock(110_000L, 10_000L, 1_700_000_000_000L))
    }

    @Test
    fun zero_duration_is_handled() {
        assertEquals(0L, FocusMath.remainingMs(0L, 0L))
        assertEquals(0L, FocusMath.actualDurationMs(0L, 0L, 0L))
    }
}
