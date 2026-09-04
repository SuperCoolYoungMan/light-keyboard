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

    @Test
    fun `diagnostic report is stable and preserves raw default ime id`() {
        val report = ImeDiagnostics.formatReport(
            ImeDiagnosticSnapshot(
                enabled = true,
                isDefault = false,
                enabledImeCount = 3,
                defaultImeId = "com.light/.EmbeddedKeyboard;subtype=7",
            ),
        )

        assertEquals(
            """Light Keyboard · IME diagnostics
This keyboard enabled: YES
This keyboard is default: NO
Enabled IME count: 3
Default IME: com.light/.EmbeddedKeyboard;subtype=7""",
            report,
        )
    }

    @Test
    fun `diagnostic report renders missing default ime safely`() {
        val report = ImeDiagnostics.formatReport(
            ImeDiagnosticSnapshot(
                enabled = false,
                isDefault = false,
                enabledImeCount = 0,
                defaultImeId = null,
            ),
        )

        assertTrue(report.contains("This keyboard enabled: NO"))
        assertTrue(report.endsWith("Default IME: none"))
    }
}
