package com.thelightphone.lp3keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class HangulComposerTest {
    private fun compose(keys: String): String {
        val composer = HangulComposer()
        keys.forEach(composer::input)
        return composer.text
    }

    @Test
    fun `composes common Korean input`() {
        assertEquals("안녕하세요", compose("ㅇㅏㄴㄴㅕㅇㅎㅏㅅㅔㅇㅛ"))
        assertEquals("괜찮아요", compose("ㄱㅗㅐㄴㅊㅏㄴㅎㅇㅏㅇㅛ"))
        assertEquals("읽고", compose("ㅇㅣㄹㄱㄱㅗ"))
        assertEquals("값이", compose("ㄱㅏㅂㅅㅇㅣ"))
    }

    @Test
    fun `moves a final consonant before a following vowel`() {
        assertEquals("가나", compose("ㄱㅏㄴㅏ"))
        assertEquals("달가", compose("ㄷㅏㄹㄱㅏ"))
    }

    @Test
    fun `supports shifted jamo`() {
        assertEquals("까", compose("ㄲㅏ"))
        assertEquals("있어", compose("ㅇㅣㅆㅇㅓ"))
    }

    @Test
    fun `backspace removes one key at a time`() {
        val composer = HangulComposer()
        "ㄱㅗㅏ".forEach(composer::input)

        assertEquals("과", composer.text)
        assertEquals("고", composer.backspace())
        assertEquals("ㄱ", composer.backspace())
        assertEquals("", composer.backspace())
    }
}
