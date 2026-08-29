package com.thelightphone.lp3Keyboard.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class KeyMotionTest {
    @Test
    fun `press attack is faster than release`() {
        assertTrue(KEY_MOTION_ATTACK_MS < KEY_MOTION_RELEASE_MS)
        assertTrue(KEY_MOTION_ATTACK_MS in 1..30)
        assertTrue(KEY_MOTION_RELEASE_MS in 45..100)
    }

    @Test
    fun `motion remains subtle for every key style`() {
        KeyMotionStyle.entries.forEach { style ->
            assertTrue("${style.name} scale is too large", style.pressedScale in 1.0f..1.08f)
            assertTrue("${style.name} lift is too large", style.liftDp in 0f..3f)
        }
    }

    @Test
    fun `space moves less than character keys`() {
        assertTrue(KeyMotionStyle.Space.pressedScale < KeyMotionStyle.Character.pressedScale)
        assertTrue(KeyMotionStyle.Space.liftDp < KeyMotionStyle.Character.liftDp)
    }
}
