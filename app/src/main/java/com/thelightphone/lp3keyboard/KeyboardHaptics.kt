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

data class HapticCapabilities(
    val lowTick: Boolean,
    val tick: Boolean,
    val click: Boolean,
    val thud: Boolean,
    val quickRise: Boolean,
    val quickFall: Boolean,
) {
    val fullCrispProfile: Boolean
        get() = lowTick && tick && click && thud && quickRise && quickFall
}

class KeyboardHaptics(private val context: Context) {
    private val vibrator =
        context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun capabilities(): HapticCapabilities {
        val supported = vibrator.arePrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_THUD,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
        )
        return HapticCapabilities(
            lowTick = supported[0],
            tick = supported[1],
            click = supported[2],
            thud = supported[3],
            quickRise = supported[4],
            quickFall = supported[5],
        )
    }

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
        val supported = when (event) {
            KeyboardHapticEvent.Key -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            )
            KeyboardHapticEvent.Space -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            )
            KeyboardHapticEvent.Shift -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            )
            KeyboardHapticEvent.Backspace -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            )
            KeyboardHapticEvent.BackspaceRepeat -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            )
            KeyboardHapticEvent.Enter -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
            KeyboardHapticEvent.LanguageSwitch -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
            KeyboardHapticEvent.LongPress -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
        }
        if (!supported) return null

        fun scaled(value: Float): Float = (value * strengthScale).coerceIn(0.05f, 1f)
        val builder = VibrationEffect.startComposition()

        when (event) {
            KeyboardHapticEvent.Key ->
                builder.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    scaled(0.24f)
                )

            KeyboardHapticEvent.Space ->
                builder
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        scaled(0.30f)
                    )
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
                        scaled(0.12f),
                        0
                    )

            KeyboardHapticEvent.Shift ->
                builder
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_TICK,
                        scaled(0.28f)
                    )
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                        scaled(0.14f),
                        0
                    )

            KeyboardHapticEvent.Backspace ->
                builder.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    scaled(0.27f)
                )

            KeyboardHapticEvent.BackspaceRepeat ->
                builder.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    scaled(0.14f)
                )

            KeyboardHapticEvent.Enter ->
                builder
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_THUD,
                        scaled(0.24f)
                    )
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        scaled(0.22f),
                        6
                    )

            KeyboardHapticEvent.LanguageSwitch ->
                builder
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                        scaled(0.25f)
                    )
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        scaled(0.34f),
                        24
                    )

            KeyboardHapticEvent.LongPress ->
                builder
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                        scaled(0.14f)
                    )
                    .addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        scaled(0.22f),
                        4
                    )
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
                val resolved = if (vibrator.hasAmplitudeControl()) {
                    amplitude
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
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
