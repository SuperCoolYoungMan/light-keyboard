package com.thelightphone.lp3keyboard

/**
 * Tracks whether the current Space press was consumed by the language-switch long press.
 *
 * After a long press toggles Korean/English, the matching repeat/release callbacks must not
 * insert spaces. A fresh Space press clears the consumed state and behaves normally again.
 */
internal class SpaceLongPressGuard {
    private var consumedByLongPress = false

    fun onPress() {
        consumedByLongPress = false
    }

    fun onLongPress() {
        consumedByLongPress = true
    }

    fun shouldSuppressRepeat(): Boolean = consumedByLongPress

    fun consumeRelease(): Boolean {
        val suppress = consumedByLongPress
        consumedByLongPress = false
        return suppress
    }
}
