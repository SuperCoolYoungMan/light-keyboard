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

    @Test
    fun `adaptive factor stays full at relaxed cadence`() {
        assertEquals(1f, adaptiveMotionFactor(400f), 0.0001f)
        assertEquals(1f, adaptiveMotionFactor(260f), 0.0001f)
    }

    @Test
    fun `adaptive factor shrinks motion at fast cadence but never disappears`() {
        val medium = adaptiveMotionFactor(200f)
        val fast = adaptiveMotionFactor(150f)
        val veryFast = adaptiveMotionFactor(100f)

        assertTrue(medium < 1f)
        assertTrue(fast < medium)
        assertTrue(veryFast < fast)
        assertTrue(veryFast >= 0.30f)
    }

    @Test
    fun `cadence tracker ramps down instead of snapping after one fast interval`() {
        val tracker = TypingCadenceTracker()

        val first = tracker.recordPress(1_000L)
        val second = tracker.recordPress(1_125L)
        val third = tracker.recordPress(1_250L)
        val fourth = tracker.recordPress(1_375L)

        assertEquals(1f, first, 0.0001f)
        assertTrue(second < first)
        assertTrue(second > 0.55f)
        assertTrue(third < second)
        assertTrue(fourth < third)
        assertTrue(fourth >= 0.30f)
    }

    @Test
    fun `cadence tracker returns to full motion after a pause`() {
        val tracker = TypingCadenceTracker()

        tracker.recordPress(1_000L)
        tracker.recordPress(1_125L)
        tracker.recordPress(1_250L)
        val afterPause = tracker.recordPress(2_500L)

        assertEquals(1f, afterPause, 0.0001f)
    }

    @Test
    fun `preview does not mutate cadence until committed`() {
        val tracker = TypingCadenceTracker()
        tracker.recordPress(1_000L)

        val preview = tracker.previewPress(1_125L)
        val repeatedPreview = tracker.previewPress(1_125L)

        assertEquals(preview.factor, repeatedPreview.factor, 0.0001f)
        assertEquals(preview.smoothedIntervalMs, repeatedPreview.smoothedIntervalMs)

        tracker.commit(preview)
        val next = tracker.previewPress(1_250L)
        assertTrue(next.factor < preview.factor)
    }
}
