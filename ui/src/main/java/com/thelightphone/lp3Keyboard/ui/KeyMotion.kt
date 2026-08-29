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
    val liftDp: Float,
) {
    Character(pressedScale = 1.055f, liftDp = 2.0f),
    Icon(pressedScale = 1.060f, liftDp = 2.2f),
    Space(pressedScale = 1.018f, liftDp = 1.0f),
    MultiLabel(pressedScale = 1.045f, liftDp = 1.6f),
}

internal const val KEY_MOTION_ATTACK_MS = 18
internal const val KEY_MOTION_RELEASE_MS = 76

/**
 * Short, non-bouncy key motion intended to complement touch-down haptics.
 *
 * Haptics are fired synchronously from the press callback. Visual motion then
 * lands on the next rendered frame: a tiny lift/scale on press and a slower,
 * controlled return on release. This intentionally avoids spring overshoot.
 */
@Composable
fun Modifier.premiumKeyMotion(
    pressed: Boolean,
    enabled: Boolean,
    style: KeyMotionStyle,
): Modifier {
    if (!enabled) return this

    val duration = if (pressed) KEY_MOTION_ATTACK_MS else KEY_MOTION_RELEASE_MS
    val easing = if (pressed) FastOutLinearInEasing else LinearOutSlowInEasing

    val scale by animateFloatAsState(
        targetValue = if (pressed) style.pressedScale else 1f,
        animationSpec = tween(durationMillis = duration, easing = easing),
        label = "keyScale",
    )
    val liftDp by animateFloatAsState(
        targetValue = if (pressed) -style.liftDp else 0f,
        animationSpec = tween(durationMillis = duration, easing = easing),
        label = "keyLift",
    )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = liftDp.dp.toPx()
    }
}
