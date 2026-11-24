package com.focuslock.app

import com.focuslock.app.domain.MarkerMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerMatcherTest {
    private val markers = listOf("Reels", "Shorts", "ShortsPlayer")

    @Test
    fun detects_case_insensitive_markers() {
        assertTrue(MarkerMatcher.matches(listOf("Home", "Reels"), markers))
        assertTrue(MarkerMatcher.matches(listOf(null, "Watch SHORTS now"), markers))
        assertTrue(MarkerMatcher.matches(listOf("com.google.android.youtube:id/shorts_player"), markers))
    }

    @Test
    fun does_not_match_unrelated_text_or_empty_markers() {
        assertFalse(MarkerMatcher.matches(listOf("Home", "Feed"), markers))
        assertFalse(MarkerMatcher.matches(listOf("Reels"), emptyList()))
        assertFalse(MarkerMatcher.matches(listOf(null, ""), markers))
    }
}
