package com.thelightphone.lp3keyboard

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticCalibrationTest {
    @Test
    fun `session starts with a clearly distinguishable safe pair`() {
        val session = HapticCalibrationSession()
        val pair = session.currentPair()

        assertEquals(1, pair.round)
        assertEquals(5, pair.totalRounds)
        assertEquals(0.78f, pair.aScale, 0.0001f)
        assertEquals(1.22f, pair.bScale, 0.0001f)
        assertFalse(session.isComplete)
    }

    @Test
    fun `choosing A recenters and narrows the next comparison`() {
        val session = HapticCalibrationSession()
        val initial = session.currentPair()

        val chosen = session.chooseA()
        val next = session.currentPair()
        val nextLow = min(next.aScale, next.bScale)
        val nextHigh = max(next.aScale, next.bScale)

        assertEquals(initial.aScale, chosen, 0.0001f)
        assertTrue(nextLow < chosen)
        assertTrue(nextHigh > chosen)
        assertTrue(abs(next.bScale - next.aScale) < abs(initial.bScale - initial.aScale))
    }

    @Test
    fun `neutral choice keeps exact midpoint and only narrows uncertainty`() {
        val session = HapticCalibrationSession()
        val initial = session.currentPair()
        val initialMidpoint = (initial.aScale + initial.bScale) / 2f
        val initialGap = abs(initial.bScale - initial.aScale)

        val chosen = session.chooseIndifferent()
        val next = session.currentPair()
        val nextMidpoint = (next.aScale + next.bScale) / 2f
        val nextGap = abs(next.bScale - next.aScale)

        assertEquals(initialMidpoint, chosen, 0.0001f)
        assertEquals(initialMidpoint, nextMidpoint, 0.0001f)
        assertEquals(initialMidpoint, session.preferredScale(), 0.0001f)
        assertTrue(nextGap < initialGap)
    }

    @Test
    fun `repeated neutral choices complete without strength bias`() {
        val session = HapticCalibrationSession()

        repeat(5) {
            session.chooseIndifferent()
        }

        assertTrue(session.isComplete)
        assertEquals(1f, session.preferredScale(), 0.0001f)
    }

    @Test
    fun `candidate ordering alternates so one letter is not always stronger`() {
        val session = HapticCalibrationSession()
        val first = session.currentPair()
        assertTrue(first.aScale < first.bScale)

        session.chooseA()
        val second = session.currentPair()
        assertTrue(second.aScale > second.bScale)

        session.chooseA()
        val third = session.currentPair()
        assertTrue(third.aScale < third.bScale)
    }

    @Test
    fun `stronger physical preference converges identically despite alternating labels`() {
        val session = HapticCalibrationSession()
        var lastChosen = 1f

        repeat(5) {
            val pair = session.currentPair()
            lastChosen = if (pair.aScale > pair.bScale) {
                session.chooseA()
            } else {
                session.chooseB()
            }
        }

        assertTrue(session.isComplete)
        assertEquals(lastChosen, session.preferredScale(), 0.0001f)
        assertTrue(session.preferredScale() > 1f)
        assertTrue(session.preferredScale() in 0.55f..1.45f)
    }

    @Test
    fun `weaker physical preference converges identically despite alternating labels`() {
        val session = HapticCalibrationSession()
        var lastChosen = 1f

        repeat(5) {
            val pair = session.currentPair()
            lastChosen = if (pair.aScale < pair.bScale) {
                session.chooseA()
            } else {
                session.chooseB()
            }
        }

        assertTrue(session.isComplete)
        assertEquals(lastChosen, session.preferredScale(), 0.0001f)
        assertTrue(session.preferredScale() < 1f)
        assertTrue(session.preferredScale() in 0.55f..1.45f)
    }

    @Test
    fun `repeated choices converge and complete in five rounds`() {
        val session = HapticCalibrationSession()
        var lastGap = Float.MAX_VALUE

        repeat(5) {
            val pair = session.currentPair()
            val gap = abs(pair.bScale - pair.aScale)
            assertTrue(gap <= lastGap)
            lastGap = gap
            session.chooseB()
        }

        assertTrue(session.isComplete)
        assertTrue(session.preferredScale() in 0.55f..1.45f)
    }

    @Test
    fun `reset restores the factory comparison`() {
        val session = HapticCalibrationSession()
        session.chooseA()
        session.chooseB()

        session.reset()
        val pair = session.currentPair()

        assertEquals(1, pair.round)
        assertEquals(0.78f, pair.aScale, 0.0001f)
        assertEquals(1.22f, pair.bScale, 0.0001f)
        assertEquals(1f, session.preferredScale(), 0.0001f)
        assertFalse(session.isComplete)
    }

    @Test
    fun `semantic targets map only to calibrated keyboard actions`() {
        assertEquals(
            HapticCalibrationTarget.Character,
            HapticCalibrationPreferences.targetFor(KeyboardHapticEvent.Key),
        )
        assertEquals(
            HapticCalibrationTarget.Space,
            HapticCalibrationPreferences.targetFor(KeyboardHapticEvent.Space),
        )
        assertEquals(
            HapticCalibrationTarget.Enter,
            HapticCalibrationPreferences.targetFor(KeyboardHapticEvent.Enter),
        )
        assertEquals(
            HapticCalibrationTarget.LanguageSwitch,
            HapticCalibrationPreferences.targetFor(KeyboardHapticEvent.LanguageSwitch),
        )
        assertEquals(null, HapticCalibrationPreferences.targetFor(KeyboardHapticEvent.Backspace))
    }
}
