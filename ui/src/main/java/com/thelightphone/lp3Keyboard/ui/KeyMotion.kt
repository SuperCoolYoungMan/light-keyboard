package com.thelightphone.lp3Keyboard.ui

import android.os.SystemClock
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val ADAPTIVE_MOTION_FULL_INTERVAL_MS = 260f
private const val ADAPTIVE_MOTION_FAST_INTERVAL_MS = 125f
private const val ADAPTIVE_MOTION_MIN_FACTOR = 0.32f
private const val ADAPTIVE_MOTION_RESET_MS = 900L
private const val ADAPTIVE_MOTION_SMOOTHING_ALPHA = 0.45f

internal data class MotionCadenceSample(
    val timestampMs: Long,
    val smoothedIntervalMs: Float?,
    val factor: Float,
)

/**
 * Tracks keyboard-wide press cadence without owning Compose state.
 *
 * A preview/commit split lets the pressed key choose its motion factor during
 * composition, while the cadence history is only mutated after that composition
 * is successfully applied. This avoids counting speculative/restarted composition.
 */
internal class TypingCadenceTracker {
    private var lastPressMs: Long? = null
    private var smoothedIntervalMs: Float? = null

    fun previewPress(nowMs: Long): MotionCadenceSample {
        val previousPress = lastPressMs
        if (
            previousPress == null ||
            nowMs <= previousPress ||
            nowMs - previousPress > ADAPTIVE_MOTION_RESET_MS
        ) {
            return MotionCadenceSample(
                timestampMs = nowMs,
                smoothedIntervalMs = null,
                factor = 1f,
            )
        }

        val intervalMs = (nowMs - previousPress).toFloat()
        val smoothed = smoothedIntervalMs?.let { previous ->
            previous * (1f - ADAPTIVE_MOTION_SMOOTHING_ALPHA) +
                intervalMs * ADAPTIVE_MOTION_SMOOTHING_ALPHA
        } ?: intervalMs

        return MotionCadenceSample(
            timestampMs = nowMs,
            smoothedIntervalMs = smoothed,
            factor = adaptiveMotionFactor(smoothed),
        )
    }

    fun commit(sample: MotionCadenceSample) {
        lastPressMs = sample.timestampMs
        smoothedIntervalMs = sample.smoothedIntervalMs
    }

    fun recordPress(nowMs: Long): Float {
        val sample = previewPress(nowMs)
        commit(sample)
        return sample.factor
    }

    fun reset() {
        lastPressMs = null
        smoothedIntervalMs = null
    }
}

internal fun adaptiveMotionFactor(intervalMs: Float): Float {
    if (intervalMs >= ADAPTIVE_MOTION_FULL_INTERVAL_MS) return 1f
    if (intervalMs <= ADAPTIVE_MOTION_FAST_INTERVAL_MS) return ADAPTIVE_MOTION_MIN_FACTOR

    val progress =
        (intervalMs - ADAPTIVE_MOTION_FAST_INTERVAL_MS) /
            (ADAPTIVE_MOTION_FULL_INTERVAL_MS - ADAPTIVE_MOTION_FAST_INTERVAL_MS)
    return ADAPTIVE_MOTION_MIN_FACTOR + progress * (1f - ADAPTIVE_MOTION_MIN_FACTOR)
}

private val adaptiveMotionCadence = TypingCadenceTracker()

enum class KeyMotionStyle(
    val pressedScale: Float,
    /** Negative lifts the glyph; positive presses it down. */
    val translationYDp: Float,
    val attackMs: Int,
    val releaseMs: Int,
) {
    Character(
        pressedScale = 1.055f,
        translationYDp = -2.0f,
        attackMs = 18,
        releaseMs = 76,
    ),
    GenericIcon(
        pressedScale = 1.035f,
        translationYDp = -1.2f,
        attackMs = 18,
        releaseMs = 72,
    ),
    Space(
        pressedScale = 0.992f,
        translationYDp = 0.8f,
        attackMs = 16,
        releaseMs = 70,
    ),
    Backspace(
        pressedScale = 0.968f,
        translationYDp = 1.4f,
        attackMs = 14,
        releaseMs = 62,
    ),
    Enter(
        pressedScale = 0.965f,
        translationYDp = 2.0f,
        attackMs = 14,
        releaseMs = 70,
    ),
    Shift(
        pressedScale = 1.028f,
        translationYDp = -1.0f,
        attackMs = 16,
        releaseMs = 68,
    ),
    ModeSwitch(
        pressedScale = 1.020f,
        translationYDp = -0.7f,
        attackMs = 16,
        releaseMs = 66,
    ),
    Dismiss(
        pressedScale = 0.985f,
        translationYDp = 0.8f,
        attackMs = 16,
        releaseMs = 64,
    ),
    Voice(
        pressedScale = 1.024f,
        translationYDp = -0.8f,
        attackMs = 16,
        releaseMs = 68,
    ),
}

internal fun motionStyleFor(key: SpecialKey): KeyMotionStyle = when (key) {
    SpecialKey.Backspace -> KeyMotionStyle.Backspace
    SpecialKey.Return, SpecialKey.Submit -> KeyMotionStyle.Enter
    SpecialKey.UpCase, SpecialKey.DownCase -> KeyMotionStyle.Shift
    SpecialKey.Letters, SpecialKey.Numbers, SpecialKey.Symbols, SpecialKey.Emojis ->
        KeyMotionStyle.ModeSwitch
    SpecialKey.Space -> KeyMotionStyle.Space
    SpecialKey.Close -> KeyMotionStyle.Dismiss
    SpecialKey.Voice -> KeyMotionStyle.Voice
}

/**
 * Short, non-bouncy key motion intended to complement touch-down haptics.
 *
 * Character keys lift very slightly, while action keys such as Enter and
 * Backspace compress downward. At fast typing cadence the visual travel is
 * automatically reduced while haptics remain unchanged, preventing the keyboard
 * from looking busy during rapid input.
 */
@Composable
fun Modifier.premiumKeyMotion(
    pressed: Boolean,
    enabled: Boolean,
    style: KeyMotionStyle,
): Modifier {
    if (!enabled) return this

    val cadenceSample = remember(pressed) {
        if (pressed) {
            adaptiveMotionCadence.previewPress(SystemClock.uptimeMillis())
        } else {
            MotionCadenceSample(0L, null, 1f)
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) adaptiveMotionCadence.commit(cadenceSample)
    }

    val motionFactor = if (pressed) cadenceSample.factor else 1f
    val targetScale = if (pressed) {
        1f + (style.pressedScale - 1f) * motionFactor
    } else {
        1f
    }
    val targetTranslationYDp = if (pressed) {
        style.translationYDp * motionFactor
    } else {
        0f
    }

    val duration = if (pressed) style.attackMs else style.releaseMs
    val easing = if (pressed) FastOutLinearInEasing else LinearOutSlowInEasing

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = duration, easing = easing),
        label = "keyScale",
    )
    val translationYDp by animateFloatAsState(
        targetValue = targetTranslationYDp,
        animationSpec = tween(durationMillis = duration, easing = easing),
        label = "keyTranslationY",
    )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = translationYDp.dp.toPx()
    }
}
