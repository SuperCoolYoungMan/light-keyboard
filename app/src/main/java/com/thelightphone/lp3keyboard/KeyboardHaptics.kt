package com.thelightphone.lp3keyboard

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.abs


enum class KeyboardHapticEvent {
    Key,
    Space,
    Shift,
    Backspace,
    BackspaceRepeat,
    Enter,
    ModeSwitch,
    LanguageSwitch,
    Dismiss,
    Voice,
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
        perform(event, calibrationScaleOverride = null, strengthOverride = null)
    }

    fun previewCalibration(target: HapticCalibrationTarget, scale: Float) {
        // Device calibration must be independent from the user's global feel preset.
        // Always compare against the Crisp reference so the saved multiplier can later
        // be combined predictably with Light/Crisp/Strong during normal typing.
        perform(
            event = target.event,
            calibrationScaleOverride = scale,
            strengthOverride = HapticStrength.Crisp,
        )
    }

    private fun perform(
        event: KeyboardHapticEvent,
        calibrationScaleOverride: Float?,
        strengthOverride: HapticStrength?,
    ) {
        if (!vibrator.hasVibrator()) return

        val strength = strengthOverride ?: HapticPreferences.getStrength(context)
        if (strength == HapticStrength.Off) return

        val target = HapticCalibrationPreferences.targetFor(event)
        val calibrationScale = calibrationScaleOverride
            ?: target?.let { HapticCalibrationPreferences.getScale(context, it) }
            ?: 1f
        val outputScale = (strength.scale * calibrationScale).coerceIn(0.05f, 1.8f)

        val composition = compositionFor(event, outputScale)
        if (composition != null) {
            vibrator.vibrate(composition)
            return
        }

        // Predefined effects are hardware-tuned but cannot express our event-specific
        // calibration multiplier. Keep them only for the untouched Crisp baseline;
        // calibrated or non-Crisp profiles use the amplitude-aware fallback instead.
        val usePredefined =
            strength == HapticStrength.Crisp && abs(calibrationScale - 1f) < 0.01f
        if (usePredefined) {
            val predefined = predefinedEffect(event)
            val support = vibrator.areEffectsSupported(predefined).firstOrNull()
            if (support != Vibrator.VIBRATION_EFFECT_SUPPORT_NO) {
                vibrator.vibrate(VibrationEffect.createPredefined(predefined))
                return
            }
        }

        vibrator.vibrate(fallback(event, outputScale))
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
            KeyboardHapticEvent.ModeSwitch -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            )
            KeyboardHapticEvent.LanguageSwitch -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
            KeyboardHapticEvent.Dismiss -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            )
            KeyboardHapticEvent.Voice -> vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
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
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scaled(0.24f))
            KeyboardHapticEvent.Space ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scaled(0.30f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, scaled(0.12f), 0)
            KeyboardHapticEvent.Shift ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scaled(0.28f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, scaled(0.14f), 0)
            KeyboardHapticEvent.Backspace ->
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scaled(0.27f))
            KeyboardHapticEvent.BackspaceRepeat ->
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scaled(0.14f))
            KeyboardHapticEvent.Enter ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, scaled(0.24f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scaled(0.22f), 6)
            KeyboardHapticEvent.ModeSwitch ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scaled(0.23f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, scaled(0.10f), 2)
            KeyboardHapticEvent.LanguageSwitch ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scaled(0.25f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scaled(0.34f), 24)
            KeyboardHapticEvent.Dismiss ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scaled(0.18f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, scaled(0.10f), 0)
            KeyboardHapticEvent.Voice ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scaled(0.28f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, scaled(0.12f), 2)
            KeyboardHapticEvent.LongPress ->
                builder
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, scaled(0.14f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scaled(0.22f), 4)
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
        KeyboardHapticEvent.ModeSwitch -> VibrationEffect.EFFECT_CLICK
        KeyboardHapticEvent.LanguageSwitch -> VibrationEffect.EFFECT_DOUBLE_CLICK
        KeyboardHapticEvent.Dismiss -> VibrationEffect.EFFECT_TICK
        KeyboardHapticEvent.Voice -> VibrationEffect.EFFECT_CLICK
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
            KeyboardHapticEvent.ModeSwitch -> 76
            KeyboardHapticEvent.LanguageSwitch -> 88
            KeyboardHapticEvent.Dismiss -> 58
            KeyboardHapticEvent.Voice -> 94
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
                    KeyboardHapticEvent.ModeSwitch -> 5
                    KeyboardHapticEvent.Dismiss -> 4
                    KeyboardHapticEvent.Voice -> 7
                    KeyboardHapticEvent.LongPress -> 7
                    KeyboardHapticEvent.LanguageSwitch -> 6
                },
                amplitude = amplitude
            )
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect {
        val resolvedAmplitude = if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        return VibrationEffect.createOneShot(durationMs, resolvedAmplitude)
    }
}
