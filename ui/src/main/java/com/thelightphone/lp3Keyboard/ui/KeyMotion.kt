package com.thelightphone.lp3Keyboard.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

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
 * Backspace compress downward. The visual direction reinforces the haptic
 * meaning without adding spring overshoot or repeated bounce while a key is held.
 */
@Composable
fun Modifier.premiumKeyMotion(
    pressed: Boolean,
    enabled: Boolean,
    style: KeyMotionStyle,
): Modifier {
    if (!enabled) return this

    val duration = if (pressed) style.attackMs else style.releaseMs
    val easing = if (pressed) FastOutLinearInEasing else LinearOutSlowInEasing

    val scale by animateFloatAsState(
        targetValue = if (pressed) style.pressedScale else 1f,
        animationSpec = tween(durationMillis = duration, easing = easing),
        label = "keyScale",
    )
    val translationYDp by animateFloatAsState(
        targetValue = if (pressed) style.translationYDp else 0f,
        animationSpec = tween(durationMillis = duration, easing = easing),
        label = "keyTranslationY",
    )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = translationYDp.dp.toPx()
    }
}
