package com.thelightphone.lp3Keyboard.ui.viewmodel

import com.thelightphone.lp3Keyboard.ui.SpecialKey
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatTimingTest {
    @Test
    fun `backspace repeats accelerate in controlled steps`() {
        assertEquals(260L, repeatIntervalMs(SpecialKey.Backspace, 0))
        assertEquals(260L, repeatIntervalMs(SpecialKey.Backspace, 1))
        assertEquals(180L, repeatIntervalMs(SpecialKey.Backspace, 2))
        assertEquals(120L, repeatIntervalMs(SpecialKey.Backspace, 5))
        assertEquals(85L, repeatIntervalMs(SpecialKey.Backspace, 9))
        assertEquals(65L, repeatIntervalMs(SpecialKey.Backspace, 14))
        assertEquals(65L, repeatIntervalMs(SpecialKey.Backspace, 100))
    }

    @Test
    fun `other held keys retain the original repeat interval`() {
        assertEquals(350L, repeatIntervalMs(SpecialKey.Space, 0))
        assertEquals(350L, repeatIntervalMs(SpecialKey.Return, 20))
    }
}
