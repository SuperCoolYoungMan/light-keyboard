package com.thelightphone.lp3keyboard

internal class HangulComposer {
    private val keys = mutableListOf<Char>()

    val text: String
        get() = render(keys)

    val isEmpty: Boolean
        get() = keys.isEmpty()

    fun input(char: Char): String {
        check(isHangulKey(char))
        keys += char
        return text
    }

    fun backspace(): String {
        if (keys.isNotEmpty()) keys.removeAt(keys.lastIndex)
        return text
    }

    fun clear() {
        keys.clear()
    }

    companion object {
        private const val HANGUL_BASE = 0xAC00
        private const val VOWEL_COUNT = 21
        private const val FINAL_COUNT = 28

        private const val INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        private const val VOWELS = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
        private const val FINALS = "\u0000ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

        private val compoundVowels = mapOf(
            ('ㅗ' to 'ㅏ') to 'ㅘ',
            ('ㅗ' to 'ㅐ') to 'ㅙ',
            ('ㅗ' to 'ㅣ') to 'ㅚ',
            ('ㅘ' to 'ㅣ') to 'ㅙ',
            ('ㅜ' to 'ㅓ') to 'ㅝ',
            ('ㅜ' to 'ㅔ') to 'ㅞ',
            ('ㅜ' to 'ㅣ') to 'ㅟ',
            ('ㅝ' to 'ㅣ') to 'ㅞ',
            ('ㅡ' to 'ㅣ') to 'ㅢ'
        )

        private val compoundFinals = mapOf(
            ('ㄱ' to 'ㅅ') to 'ㄳ',
            ('ㄴ' to 'ㅈ') to 'ㄵ',
            ('ㄴ' to 'ㅎ') to 'ㄶ',
            ('ㄹ' to 'ㄱ') to 'ㄺ',
            ('ㄹ' to 'ㅁ') to 'ㄻ',
            ('ㄹ' to 'ㅂ') to 'ㄼ',
            ('ㄹ' to 'ㅅ') to 'ㄽ',
            ('ㄹ' to 'ㅌ') to 'ㄾ',
            ('ㄹ' to 'ㅍ') to 'ㄿ',
            ('ㄹ' to 'ㅎ') to 'ㅀ',
            ('ㅂ' to 'ㅅ') to 'ㅄ'
        )

        private val splitFinals = compoundFinals.entries.associate { (pair, combined) ->
            combined to pair
        }

        fun isHangulKey(char: Char): Boolean =
            INITIALS.indexOf(char) >= 0 || VOWELS.indexOf(char) >= 0

        private fun syllable(initial: Int, vowel: Int, final: Int): Char =
            (HANGUL_BASE + (initial * VOWEL_COUNT + vowel) * FINAL_COUNT + final).toChar()

        private fun render(input: Iterable<Char>): String {
            val output = StringBuilder()
            var initial = -1
            var vowel = -1
            var final = 0

            fun current(): String = when {
                initial >= 0 && vowel >= 0 -> syllable(initial, vowel, final).toString()
                initial >= 0 -> INITIALS[initial].toString()
                vowel >= 0 -> VOWELS[vowel].toString()
                else -> ""
            }

            fun commit() {
                output.append(current())
                initial = -1
                vowel = -1
                final = 0
            }

            for (char in input) {
                val nextInitial = INITIALS.indexOf(char)
                val nextVowel = VOWELS.indexOf(char)

                if (nextVowel >= 0) {
                    when {
                        initial < 0 && vowel < 0 -> vowel = nextVowel
                        initial >= 0 && vowel < 0 -> vowel = nextVowel
                        initial < 0 -> {
                            val combined = compoundVowels[VOWELS[vowel] to char]
                            if (combined != null) {
                                vowel = VOWELS.indexOf(combined)
                            } else {
                                commit()
                                vowel = nextVowel
                            }
                        }
                        final == 0 -> {
                            val combined = compoundVowels[VOWELS[vowel] to char]
                            if (combined != null) {
                                vowel = VOWELS.indexOf(combined)
                            } else {
                                commit()
                                vowel = nextVowel
                            }
                        }
                        else -> {
                            val finalChar = FINALS[final]
                            val split = splitFinals[finalChar]
                            if (split != null) {
                                final = FINALS.indexOf(split.first)
                                output.append(current())
                                initial = INITIALS.indexOf(split.second)
                            } else {
                                final = 0
                                output.append(current())
                                initial = INITIALS.indexOf(finalChar)
                            }
                            vowel = nextVowel
                            final = 0
                        }
                    }
                    continue
                }

                if (nextInitial >= 0) {
                    when {
                        initial < 0 && vowel < 0 -> initial = nextInitial
                        initial >= 0 && vowel < 0 -> {
                            commit()
                            initial = nextInitial
                        }
                        initial < 0 -> {
                            commit()
                            initial = nextInitial
                        }
                        final == 0 -> {
                            val nextFinal = FINALS.indexOf(char)
                            if (nextFinal > 0) {
                                final = nextFinal
                            } else {
                                commit()
                                initial = nextInitial
                            }
                        }
                        else -> {
                            val combined = compoundFinals[FINALS[final] to char]
                            if (combined != null) {
                                final = FINALS.indexOf(combined)
                            } else {
                                commit()
                                initial = nextInitial
                            }
                        }
                    }
                }
            }

            output.append(current())
            return output.toString()
        }
    }
}
