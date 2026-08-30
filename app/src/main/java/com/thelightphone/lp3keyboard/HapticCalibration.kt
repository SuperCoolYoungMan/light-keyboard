package com.thelightphone.lp3keyboard

import android.content.Context
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val CALIBRATION_MIN_SCALE = 0.55f
private const val CALIBRATION_MAX_SCALE = 1.45f
private const val CALIBRATION_INITIAL_LOW = 0.78f
private const val CALIBRATION_INITIAL_HIGH = 1.22f
private const val CALIBRATION_ROUNDS = 5

enum class HapticCalibrationTarget(
    val label: String,
    val event: KeyboardHapticEvent,
) {
    Character("Character", KeyboardHapticEvent.Key),
    Space("Space", KeyboardHapticEvent.Space),
    Enter("Enter", KeyboardHapticEvent.Enter),
    LanguageSwitch("Language switch", KeyboardHapticEvent.LanguageSwitch),
}

data class HapticCalibrationPair(
    val round: Int,
    val totalRounds: Int,
    val aScale: Float,
    val bScale: Float,
)

/**
 * Small preference-search session for device-specific haptic tuning.
 *
 * It starts with two safely bounded intensity multipliers around the stock profile.
 * Each A/B choice recenters the next pair around the preferred candidate and halves
 * the search step. Candidate ordering alternates every round so A is not always the
 * weaker option and B is not always the stronger option, reducing expectation bias.
 * A neutral choice recenters on the exact midpoint so uncertainty narrows the search
 * without biasing the result toward either the stronger or weaker candidate.
 */
class HapticCalibrationSession(
    private val totalRounds: Int = CALIBRATION_ROUNDS,
) {
    private var round = 1
    private var lowScale = CALIBRATION_INITIAL_LOW
    private var highScale = CALIBRATION_INITIAL_HIGH
    private var lastPreferred = 1f

    val isComplete: Boolean
        get() = round > totalRounds

    fun currentPair(): HapticCalibrationPair {
        val flip = round % 2 == 0
        return HapticCalibrationPair(
            round = min(round, totalRounds),
            totalRounds = totalRounds,
            aScale = if (flip) highScale else lowScale,
            bScale = if (flip) lowScale else highScale,
        )
    }

    fun chooseA(): Float = choose(currentPair().aScale)

    fun chooseB(): Float = choose(currentPair().bScale)

    fun chooseIndifferent(): Float {
        val pair = currentPair()
        return choose((pair.aScale + pair.bScale) / 2f)
    }

    fun preferredScale(): Float = lastPreferred

    fun reset() {
        round = 1
        lowScale = CALIBRATION_INITIAL_LOW
        highScale = CALIBRATION_INITIAL_HIGH
        lastPreferred = 1f
    }

    private fun choose(preferred: Float): Float {
        lastPreferred = preferred
        if (round >= totalRounds) {
            round = totalRounds + 1
            return preferred
        }

        val previousGap = abs(highScale - lowScale).coerceAtLeast(0.02f)
        val nextHalfGap = previousGap / 4f
        lowScale = (preferred - nextHalfGap).coerceIn(CALIBRATION_MIN_SCALE, CALIBRATION_MAX_SCALE)
        highScale = (preferred + nextHalfGap).coerceIn(CALIBRATION_MIN_SCALE, CALIBRATION_MAX_SCALE)

        if (highScale - lowScale < 0.02f) {
            lowScale = max(CALIBRATION_MIN_SCALE, preferred - 0.01f)
            highScale = min(CALIBRATION_MAX_SCALE, preferred + 0.01f)
        }

        round += 1
        return preferred
    }
}

object HapticCalibrationPreferences {
    private const val PREFS_NAME = "keyboard_haptic_calibration"

    fun getScale(context: Context, target: HapticCalibrationTarget): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(target.name, 1f)
            .coerceIn(CALIBRATION_MIN_SCALE, CALIBRATION_MAX_SCALE)
    }

    fun setScale(context: Context, target: HapticCalibrationTarget, scale: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(target.name, scale.coerceIn(CALIBRATION_MIN_SCALE, CALIBRATION_MAX_SCALE))
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun targetFor(event: KeyboardHapticEvent): HapticCalibrationTarget? = when (event) {
        KeyboardHapticEvent.Key -> HapticCalibrationTarget.Character
        KeyboardHapticEvent.Space -> HapticCalibrationTarget.Space
        KeyboardHapticEvent.Enter -> HapticCalibrationTarget.Enter
        KeyboardHapticEvent.LanguageSwitch -> HapticCalibrationTarget.LanguageSwitch
        else -> null
    }
}
