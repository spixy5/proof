package com.proofapp.android.ui.cooking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTimerTest {

    @Test
    fun `a fresh timer has its full duration left`() {
        val timer = StepTimer(totalSeconds = 90)
        assertFalse(timer.running)
        assertEquals(90_000, timer.remainingMillis(now = 0))
        assertEquals(0f, timer.progress(now = 0), 1e-6f)
    }

    @Test
    fun `a running timer counts down from its end time`() {
        val timer = StepTimer(totalSeconds = 60, endsAtMillis = 100_000)
        assertTrue(timer.running)
        assertEquals(30_000, timer.remainingMillis(now = 70_000))
        assertEquals(0.5f, timer.progress(now = 70_000), 1e-6f)
    }

    @Test
    fun `a paused timer keeps its remaining time no matter the clock`() {
        val timer = StepTimer(totalSeconds = 60, endsAtMillis = null, pausedRemainingMillis = 12_000)
        assertEquals(12_000, timer.remainingMillis(now = 0))
        assertEquals(12_000, timer.remainingMillis(now = 999_999))
    }

    @Test
    fun `remaining time never goes negative and finishes`() {
        val timer = StepTimer(totalSeconds = 10, endsAtMillis = 5_000)
        assertEquals(0, timer.remainingMillis(now = 60_000))
        assertTrue(timer.finished(now = 60_000))
        assertEquals(1f, timer.progress(now = 60_000), 1e-6f)
    }
}
