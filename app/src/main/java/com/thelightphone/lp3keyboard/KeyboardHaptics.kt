package com.thelightphone.lp3keyboard

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class KeyboardHapticEvent {
    Key,
    Space,
    Shift,
    Backspace,
    BackspaceRepeat,
    Enter,
    LanguageSwitch,
    LongPress
}

/**
 * Centralized keyboard haptics tuned for short, crisp feedback.
 *
 * Prefer Android's predefined effects so the device can use its own actuator tuning.
 * When an effect is explicitly unsupported, fall back to a very short one-shot/waveform.
 */
class KeyboardHaptics(context: Context) {
    private val vibrator =
        context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun perform(event: KeyboardHapticEvent) {
        if (!vibrator.hasVibrator()) return

        val predefined = when (event) {
            KeyboardHapticEvent.Key -> VibrationEffect.EFFECT_TICK
            KeyboardHapticEvent.Space -> VibrationEffect.EFFECT_CLICK
            KeyboardHapticEvent.Shift -> VibrationEffect.EFFECT_CLICK
            KeyboardHapticEvent.Backspace -> VibrationEffect.EFFECT_TICK
            KeyboardHapticEvent.BackspaceRepeat -> VibrationEffect.EFFECT_TICK
            KeyboardHapticEvent.Enter -> VibrationEffect.EFFECT_HEAVY_CLICK
            KeyboardHapticEvent.LanguageSwitch -> VibrationEffect.EFFECT_DOUBLE_CLICK
            KeyboardHapticEvent.LongPress -> VibrationEffect.EFFECT_CLICK
        }

        val support = vibrator.areEffectsSupported(predefined).firstOrNull()
        if (support != Vibrator.VIBRATION_EFFECT_SUPPORT_NO) {
            vibrator.vibrate(VibrationEffect.createPredefined(predefined))
            return
        }

        vibrator.vibrate(fallback(event))
    }

    private fun fallback(event: KeyboardHapticEvent): VibrationEffect = when (event) {
        KeyboardHapticEvent.Key -> oneShot(6, 72)
        KeyboardHapticEvent.Space -> oneShot(9, 96)
        KeyboardHapticEvent.Shift -> oneShot(10, 108)
        KeyboardHapticEvent.Backspace -> oneShot(7, 80)
        KeyboardHapticEvent.BackspaceRepeat -> oneShot(4, 56)
        KeyboardHapticEvent.Enter -> oneShot(14, 132)
        KeyboardHapticEvent.LanguageSwitch ->
            VibrationEffect.createWaveform(longArrayOf(0, 7, 24, 7), -1)
        KeyboardHapticEvent.LongPress -> oneShot(12, 112)
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect {
        val resolvedAmplitude =
            if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        return VibrationEffect.createOneShot(durationMs, resolvedAmplitude)
    }
}
