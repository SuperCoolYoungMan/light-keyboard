package com.thelightphone.lp3keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceLongPressGuardTest {
    @Test
    fun `short Space press is not suppressed`() {
        val guard = SpaceLongPressGuard()

        guard.onPress()

        assertFalse(guard.shouldSuppressRepeat())
        assertFalse(guard.consumeRelease())
    }

    @Test
    fun `language switch long press suppresses repeat and matching release`() {
        val guard = SpaceLongPressGuard()

        guard.onPress()
        guard.onLongPress()

        assertTrue(guard.shouldSuppressRepeat())
        assertTrue(guard.consumeRelease())
    }

    @Test
    fun `release consumption resets suppression for next interaction`() {
        val guard = SpaceLongPressGuard()

        guard.onPress()
        guard.onLongPress()
        assertTrue(guard.consumeRelease())

        assertFalse(guard.shouldSuppressRepeat())
        assertFalse(guard.consumeRelease())
    }

    @Test
    fun `fresh Space press clears stale long press state`() {
        val guard = SpaceLongPressGuard()

        guard.onPress()
        guard.onLongPress()
        guard.onPress()

        assertFalse(guard.shouldSuppressRepeat())
        assertFalse(guard.consumeRelease())
    }
}
