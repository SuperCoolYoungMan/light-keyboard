package com.thelightphone.lp3keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceRepeatHapticGateTest {
    @Test
    fun `rapid repeat haptics are throttled without delaying later pulses`() {
        val gate = BackspaceRepeatHapticGate(minimumIntervalMs = 110L)

        assertTrue(gate.shouldEmit(1_000L))
        assertFalse(gate.shouldEmit(1_065L))
        assertFalse(gate.shouldEmit(1_109L))
        assertTrue(gate.shouldEmit(1_110L))
        assertFalse(gate.shouldEmit(1_175L))
        assertTrue(gate.shouldEmit(1_220L))
    }

    @Test
    fun `slower repeat cadence is preserved one for one`() {
        val gate = BackspaceRepeatHapticGate(minimumIntervalMs = 110L)

        assertTrue(gate.shouldEmit(0L))
        assertTrue(gate.shouldEmit(260L))
        assertTrue(gate.shouldEmit(440L))
        assertTrue(gate.shouldEmit(560L))
    }
}
