package com.thelightphone.lp3Keyboard.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyMotionTest {
    @Test
    fun `every motion has a fast attack and controlled release`() {
        KeyMotionStyle.entries.forEach { style ->
            assertTrue("${style.name} attack is too slow", style.attackMs in 8..30)
            assertTrue("${style.name} release is out of bounds", style.releaseMs in 45..100)
            assertTrue("${style.name} should attack faster than it releases", style.attackMs < style.releaseMs)
        }
    }

    @Test
    fun `motion remains subtle for every key style`() {
        KeyMotionStyle.entries.forEach { style ->
            assertTrue("${style.name} scale is too large", style.pressedScale in 0.95f..1.08f)
            assertTrue(
                "${style.name} vertical travel is too large",
                abs(style.translationYDp) <= 3f
            )
        }
    }

    @Test
    fun `characters lift while destructive and commit actions press down`() {
        assertTrue(KeyMotionStyle.Character.pressedScale > 1f)
        assertTrue(KeyMotionStyle.Character.translationYDp < 0f)

        assertTrue(KeyMotionStyle.Backspace.pressedScale < 1f)
        assertTrue(KeyMotionStyle.Backspace.translationYDp > 0f)

        assertTrue(KeyMotionStyle.Enter.pressedScale < 1f)
        assertTrue(KeyMotionStyle.Enter.translationYDp > 0f)
    }

    @Test
    fun `space is quieter than character motion`() {
        assertTrue(abs(KeyMotionStyle.Space.pressedScale - 1f) < abs(KeyMotionStyle.Character.pressedScale - 1f))
        assertTrue(abs(KeyMotionStyle.Space.translationYDp) < abs(KeyMotionStyle.Character.translationYDp))
    }

    @Test
    fun `special keys map to semantic motion profiles`() {
        assertEquals(KeyMotionStyle.Backspace, motionStyleFor(SpecialKey.Backspace))
        assertEquals(KeyMotionStyle.Enter, motionStyleFor(SpecialKey.Return))
        assertEquals(KeyMotionStyle.Enter, motionStyleFor(SpecialKey.Submit))
        assertEquals(KeyMotionStyle.Shift, motionStyleFor(SpecialKey.UpCase))
        assertEquals(KeyMotionStyle.ModeSwitch, motionStyleFor(SpecialKey.Numbers))
        assertEquals(KeyMotionStyle.Dismiss, motionStyleFor(SpecialKey.Close))
        assertEquals(KeyMotionStyle.Voice, motionStyleFor(SpecialKey.Voice))
    }
}
