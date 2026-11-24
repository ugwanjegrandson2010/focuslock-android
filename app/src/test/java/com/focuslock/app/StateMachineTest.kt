package com.focuslock.app

import com.focuslock.app.data.SessionStatus
import com.focuslock.app.domain.ActiveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StateMachineTest {

    @Test
    fun initial_state_is_idle() {
        val state = ActiveState()
        assertFalse(state.isActive)
        assertEquals(null, state.session)
        assertEquals(0L, state.remainingMs)
    }

    @Test
    fun active_state_exposes_session_and_countdown() {
        val state = ActiveState(isActive = true, remainingMs = 42_000L)
        assertTrue(state.isActive)
        assertEquals(42_000L, state.remainingMs)
    }

    @Test
    fun statuses_are_distinct() {
        assertEquals("SCHEDULED", SessionStatus.SCHEDULED)
        assertEquals("ACTIVE", SessionStatus.ACTIVE)
        assertEquals("COMPLETED", SessionStatus.COMPLETED)
        assertEquals("ABORTED", SessionStatus.ABORTED)
    }
}
