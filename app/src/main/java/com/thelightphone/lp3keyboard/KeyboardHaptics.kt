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

class KeyboardHaptics(private val context: Context) {
    private val vibrator =
        context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun perform(event: KeyboardHapticEvent) {
        if (!vibrator.hasVibrator()) return

        val strength = HapticPreferences.getStrength(context)
        if (strength == HapticStrength.Off) return

        val composition = compositionFor(event, strength.scale)
        if (composition != null) {
            vibrator.vibrate(composition)
            return
        }

        val predefined = predefinedEffect(event)
        val support = vibrator.areEffectsSupported(predefined).firstOrNull()
        if (support != Vibrator.VIBRATION_EFFECT_SUPPORT_NO) {
            vibrator.vibrate(VibrationEffect.createPredefined(predefined))
            return
        }

        vibrator.vibrate(fallback(event, strength.scale))
    }

    private fun compositionFor(event: KeyboardHapticEvent, strengthScale: Float): VibrationEffect? {
        val c = VibrationEffect.Composition
        val primitives = when (event) {
            KeyboardHapticEvent.Key -> intArrayOf(c.PRIMITIVE_LOW_TICK)
            KeyboardHapticEvent.Space -> intArrayOf(c.PRIMITIVE_CLICK, c.PRIMITIVE_QUICK_FALL)
            KeyboardHapticEvent.Shift -> intArrayOf(c.PRIMITIVE_TICK, c.PRIMITIVE_QUICK_RISE)
            KeyboardHapticEvent.Backspace -> intArrayOf(c.PRIMITIVE_LOW_TICK)
            KeyboardHapticEvent.BackspaceRepeat -> intArrayOf(c.PRIMITIVE_LOW_TICK)
            KeyboardHapticEvent.Enter -> intArrayOf(c.PRIMITIVE_THUD, c.PRIMITIVE_CLICK)
            KeyboardHapticEvent.LanguageSwitch -> intArrayOf(c.PRIMITIVE_LOW_TICK, c.PRIMITIVE_CLICK)
            KeyboardHapticEvent.LongPress -> intArrayOf(c.PRIMITIVE_QUICK_RISE, c.PRIMITIVE_CLICK)
        }

        if (!vibrator.areAllPrimitivesSupported(*primitives)) return null

        fun s(value: Float): Float = (value * strengthScale).coerceIn(0.05f, 1f)
        val builder = VibrationEffect.startComposition()

        when (event) {
            KeyboardHapticEvent.Key ->
                builder.addPrimitive(c.PRIMITIVE_LOW_TICK, s(0.24f))

            KeyboardHapticEvent.Space ->
                builder
                    .addPrimitive(c.PRIMITIVE_CLICK, s(0.30f))
                    .addPrimitive(c.PRIMITIVE_QUICK_FALL, s(0.12f), 0)

            KeyboardHapticEvent.Shift ->
                builder
                    .addPrimitive(c.PRIMITIVE_TICK, s(0.28f))
                    .addPrimitive(c.PRIMITIVE_QUICK_RISE, s(0.14f), 0)

            KeyboardHapticEvent.Backspace ->
                builder.addPrimitive(c.PRIMITIVE_LOW_TICK, s(0.27f))

            KeyboardHapticEvent.BackspaceRepeat ->
                builder.addPrimitive(c.PRIMITIVE_LOW_TICK, s(0.14f))

            KeyboardHapticEvent.Enter ->
                builder
                    .addPrimitive(c.PRIMITIVE_THUD, s(0.24f))
                    .addPrimitive(c.PRIMITIVE_CLICK, s(0.22f), 6)

            KeyboardHapticEvent.LanguageSwitch ->
                builder
                    .addPrimitive(c.PRIMITIVE_LOW_TICK, s(0.25f))
                    .addPrimitive(c.PRIMITIVE_CLICK, s(0.34f), 24)

            KeyboardHapticEvent.LongPress ->
                builder
                    .addPrimitive(c.PRIMITIVE_QUICK_RISE, s(0.14f))
                    .addPrimitive(c.PRIMITIVE_CLICK, s(0.22f), 4)
        }

        return builder.compose()
    }

    private fun predefinedEffect(event: KeyboardHapticEvent): Int = when (event) {
        KeyboardHapticEvent.Key -> VibrationEffect.EFFECT_TICK
        KeyboardHapticEvent.Space -> VibrationEffect.EFFECT_CLICK
        KeyboardHapticEvent.Shift -> VibrationEffect.EFFECT_CLICK
        KeyboardHapticEvent.Backspace -> VibrationEffect.EFFECT_TICK
        KeyboardHapticEvent.BackspaceRepeat -> VibrationEffect.EFFECT_TICK
        KeyboardHapticEvent.Enter -> VibrationEffect.EFFECT_HEAVY_CLICK
        KeyboardHapticEvent.LanguageSwitch -> VibrationEffect.EFFECT_DOUBLE_CLICK
        KeyboardHapticEvent.LongPress -> VibrationEffect.EFFECT_CLICK
    }

    private fun fallback(event: KeyboardHapticEvent, strengthScale: Float): VibrationEffect {
        val baseAmplitude = when (event) {
            KeyboardHapticEvent.Key -> 62
            KeyboardHapticEvent.Space -> 86
            KeyboardHapticEvent.Shift -> 94
            KeyboardHapticEvent.Backspace -> 70
            KeyboardHapticEvent.BackspaceRepeat -> 42
            KeyboardHapticEvent.Enter -> 116
            KeyboardHapticEvent.LanguageSwitch -> 88
            KeyboardHapticEvent.LongPress -> 96
        }
        val amplitude = (baseAmplitude * strengthScale).toInt().coerceIn(1, 255)

        return when (event) {
            KeyboardHapticEvent.LanguageSwitch -> {
                val resolved = if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createWaveform(
                    longArrayOf(0, 5, 24, 7),
                    intArrayOf(0, resolved.coerceAtMost(180), 0, resolved),
                    -1
                )
            }
            else -> oneShot(
                durationMs = when (event) {
                    KeyboardHapticEvent.Key -> 4
                    KeyboardHapticEvent.Space -> 6
                    KeyboardHapticEvent.Shift -> 7
                    KeyboardHapticEvent.Backspace -> 5
                    KeyboardHapticEvent.BackspaceRepeat -> 3
                    KeyboardHapticEvent.Enter -> 9
                    KeyboardHapticEvent.LongPress -> 7
                    KeyboardHapticEvent.LanguageSwitch -> 6
                },
                amplitude = amplitude
            )
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect {
        val resolvedAmplitude =
            if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        return VibrationEffect.createOneShot(durationMs, resolvedAmplitude)
    }
}
