package com.focuslock.app

import com.focuslock.app.domain.OnceGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class OnceGuardTest {

    @Test
    fun runsTheActionOnlyOnce() {
        val guard = OnceGuard()
        val calls = AtomicInteger()

        assertTrue(guard.runOnce { calls.incrementAndGet() })
        assertFalse(guard.runOnce { calls.incrementAndGet() })
        assertFalse(guard.runOnce { calls.incrementAndGet() })

        assertEquals(1, calls.get())
    }

    @Test
    fun onlyOneConcurrentCallerWins() {
        val guard = OnceGuard()
        val calls = AtomicInteger()
        val winners = AtomicInteger()
        val threads = 8
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val pool = Executors.newFixedThreadPool(threads)

        repeat(threads) {
            pool.submit {
                start.await()
                if (guard.runOnce { calls.incrementAndGet() }) winners.incrementAndGet()
                done.countDown()
            }
        }
        start.countDown()
        done.await()
        pool.shutdown()

        assertEquals(1, winners.get())
        assertEquals(1, calls.get())
    }

    @Test
    fun resetAllowsTheGuardToBeReusedForANewSession() {
        val guard = OnceGuard()
        val calls = AtomicInteger()

        assertTrue(guard.runOnce { calls.incrementAndGet() })
        guard.reset()
        assertTrue(guard.runOnce { calls.incrementAndGet() })

        assertEquals(2, calls.get())
    }
}
