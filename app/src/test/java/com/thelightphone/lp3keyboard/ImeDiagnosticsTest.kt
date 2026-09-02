package com.thelightphone.lp3keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeDiagnosticsTest {
    @Test
    fun `normalizes short and fully qualified component ids`() {
        assertEquals(
            "com.thelightphone.lp3keyboard/com.thelightphone.lp3keyboard.IMEService",
            ImeDiagnostics.normalizeImeId("com.thelightphone.lp3keyboard/.IMEService"),
        )
        assertEquals(
            "com.example/com.example.Keyboard",
            ImeDiagnostics.normalizeImeId("com.example/com.example.Keyboard"),
        )
    }

    @Test
    fun `enabled ime parser ignores subtype suffixes`() {
        val ids = ImeDiagnostics.parseEnabledImeIds(
            "com.example/.Keyboard;123:com.thelightphone.lp3keyboard/.IMEService;456",
        )

        assertEquals(2, ids.size)
        assertTrue("com.example/com.example.Keyboard" in ids)
        assertTrue(
            "com.thelightphone.lp3keyboard/com.thelightphone.lp3keyboard.IMEService" in ids,
        )
    }

    @Test
    fun `empty enabled ime setting is safe`() {
        assertTrue(ImeDiagnostics.parseEnabledImeIds(null).isEmpty())
        assertTrue(ImeDiagnostics.parseEnabledImeIds("").isEmpty())
    }
}
