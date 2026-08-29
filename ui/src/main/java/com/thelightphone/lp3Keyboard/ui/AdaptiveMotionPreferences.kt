package com.thelightphone.lp3Keyboard.ui

import android.content.Context

/**
 * User-facing visual motion profiles. Haptic intensity is intentionally separate.
 */
enum class AdaptiveMotionMode(
    val label: String,
    val description: String,
    internal val fullIntervalMs: Float,
    internal val fastIntervalMs: Float,
    internal val minFactor: Float,
) {
    Off(
        label = "Off",
        description = "Always use the full tactile motion profile.",
        fullIntervalMs = 260f,
        fastIntervalMs = 125f,
        minFactor = 1f,
    ),
    Balanced(
        label = "Balanced",
        description = "Keep full motion when relaxed and calm it during fast typing.",
        fullIntervalMs = 260f,
        fastIntervalMs = 125f,
        minFactor = 0.32f,
    ),
    Minimal(
        label = "Minimal",
        description = "Reduce visual travel earlier and more aggressively at speed.",
        fullIntervalMs = 300f,
        fastIntervalMs = 145f,
        minFactor = 0.14f,
    ),
}

object AdaptiveMotionPreferences {
    private const val PREFS_NAME = "keyboard_motion"
    private const val KEY_MODE = "adaptive_motion_mode"

    fun getMode(context: Context): AdaptiveMotionMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_MODE, AdaptiveMotionMode.Balanced.name)
        return AdaptiveMotionMode.entries.firstOrNull { it.name == name }
            ?: AdaptiveMotionMode.Balanced
    }

    fun setMode(context: Context, mode: AdaptiveMotionMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}
