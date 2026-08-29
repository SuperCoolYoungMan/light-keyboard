package com.thelightphone.lp3keyboard

import android.content.Context

enum class HapticStrength(val label: String, val scale: Float) {
    Off("Off", 0f),
    Light("Light", 0.68f),
    Crisp("Crisp", 1.0f),
    Strong("Strong", 1.25f)
}

object HapticPreferences {
    private const val PREFS_NAME = "keyboard_haptics"
    private const val KEY_STRENGTH = "strength"

    fun getStrength(context: Context): HapticStrength {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_STRENGTH, HapticStrength.Crisp.name)
        return HapticStrength.entries.firstOrNull { it.name == name } ?: HapticStrength.Crisp
    }

    fun setStrength(context: Context, strength: HapticStrength) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STRENGTH, strength.name)
            .apply()
    }
}
