package com.thelightphone.lp3keyboard

/**
 * Keeps rapid backspace deletion responsive without turning LP3 repeat haptics into a buzz.
 * Deletion cadence is intentionally controlled elsewhere; this gate only limits vibration rate.
 */
internal class BackspaceRepeatHapticGate(
    private val minimumIntervalMs: Long = 110L,
) {
    private var lastEmissionMs: Long? = null

    fun shouldEmit(nowMs: Long): Boolean {
        val previous = lastEmissionMs
        if (previous != null && nowMs - previous < minimumIntervalMs) return false
        lastEmissionMs = nowMs
        return true
    }
}
