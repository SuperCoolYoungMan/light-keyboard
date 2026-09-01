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
    fun `composes common compound finals`() {
        assertEquals("닭이", compose("ㄷㅏㄹㄱㅇㅣ"))
        assertEquals("삶아", compose("ㅅㅏㄹㅁㅇㅏ"))
        assertEquals("앉아", compose("ㅇㅏㄴㅈㅇㅏ"))
        assertEquals("않아", compose("ㅇㅏㄴㅎㅇㅏ"))
        assertEquals("없어", compose("ㅇㅓㅂㅅㅇㅓ"))
    }

    @Test
    fun `composes compound vowels`() {
        assertEquals("과", compose("ㄱㅗㅏ"))
        assertEquals("괘", compose("ㄱㅗㅐ"))
        assertEquals("워", compose("ㅇㅜㅓ"))
        assertEquals("웨", compose("ㅇㅜㅔ"))
        assertEquals("위", compose("ㅇㅜㅣ"))
        assertEquals("의", compose("ㅇㅡㅣ"))
    }

    @Test
    fun `composes chained compound vowels from sequential Dubeolsik keys`() {
        assertEquals("괘", compose("ㄱㅗㅏㅣ"))
        assertEquals("궤", compose("ㄱㅜㅓㅣ"))
    }

    @Test
    fun `moves a final consonant before a following vowel`() {
        assertEquals("가나", compose("ㄱㅏㄴㅏ"))
        assertEquals("달가", compose("ㄷㅏㄹㄱㅏ"))
        assertEquals("갑시", compose("ㄱㅏㅂㅅㅣ"))
    }

    @Test
    fun `splits every supported compound final before a following vowel`() {
        val cases = listOf(
            "ㄱㅏㄱㅅㅏ" to "각사",
            "ㄱㅏㄴㅈㅏ" to "간자",
            "ㄱㅏㄴㅎㅏ" to "간하",
            "ㄱㅏㄹㄱㅏ" to "갈가",
            "ㄱㅏㄹㅁㅏ" to "갈마",
            "ㄱㅏㄹㅂㅏ" to "갈바",
            "ㄱㅏㄹㅅㅏ" to "갈사",
            "ㄱㅏㄹㅌㅏ" to "갈타",
            "ㄱㅏㄹㅍㅏ" to "갈파",
            "ㄱㅏㄹㅎㅏ" to "갈하",
            "ㄱㅏㅂㅅㅏ" to "갑사",
        )

        cases.forEach { (keys, expected) ->
            assertEquals("failed for $keys", expected, compose(keys))
        }
    }

    @Test
    fun `keeps a compound final when the next syllable starts explicitly`() {
        assertEquals("읽어", compose("ㅇㅣㄹㄱㅇㅓ"))
        assertEquals("값이", compose("ㄱㅏㅂㅅㅇㅣ"))
    }

    @Test
    fun `supports shifted jamo`() {
        assertEquals("까", compose("ㄲㅏ"))
        assertEquals("또", compose("ㄸㅗ"))
        assertEquals("뼈", compose("ㅃㅕ"))
        assertEquals("쪼", compose("ㅉㅗ"))
        assertEquals("있어", compose("ㅇㅣㅆㅇㅓ"))
    }

    @Test
    fun `keeps incompatible standalone jamo separate`() {
        assertEquals("ㄱㄴ", compose("ㄱㄴ"))
        assertEquals("ㅏㅓ", compose("ㅏㅓ"))
    }

    @Test
    fun `backspace removes one raw input at a time for compound vowel`() {
        val composer = HangulComposer()
        "ㄱㅗㅏ".forEach(composer::input)

        assertEquals("과", composer.text)
        assertEquals("고", composer.backspace())
        assertEquals("ㄱ", composer.backspace())
        assertEquals("", composer.backspace())
    }

    @Test
    fun `backspace unwinds compound final naturally`() {
        val composer = HangulComposer()
        "ㄱㅏㅂㅅ".forEach(composer::input)

        assertEquals("값", composer.text)
        assertEquals("갑", composer.backspace())
        assertEquals("가", composer.backspace())
        assertEquals("ㄱ", composer.backspace())
        assertEquals("", composer.backspace())
    }

    @Test
    fun `backspace after resyllabification restores the previous compound final`() {
        val composer = HangulComposer()
        "ㄱㅏㅂㅅㅏ".forEach(composer::input)

        assertEquals("갑사", composer.text)
        assertEquals("값", composer.backspace())
        assertEquals("갑", composer.backspace())
        assertEquals("가", composer.backspace())
    }
}
