package com.example

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent storage for CR Speed app:
 * - Current playback speed (default: 1.0x)
 * - Pill visibility (default: visible)
 * - Pill coordinates on screen for position restoration across sessions and rotations
 */
class SpeedPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var speed: Float
        get() = prefs.getFloat(KEY_SPEED, DEFAULT_SPEED)
        set(value) = prefs.edit().putFloat(KEY_SPEED, value).apply()

    var isPillVisible: Boolean
        get() = prefs.getBoolean(KEY_PILL_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_PILL_VISIBLE, value).apply()

    var pillX: Float
        get() = prefs.getFloat(KEY_PILL_X, -1f)
        set(value) = prefs.edit().putFloat(KEY_PILL_X, value).apply()

    var pillY: Float
        get() = prefs.getFloat(KEY_PILL_Y, -1f)
        set(value) = prefs.edit().putFloat(KEY_PILL_Y, value).apply()

    fun savePillPosition(x: Float, y: Float) {
        prefs.edit()
            .putFloat(KEY_PILL_X, x)
            .putFloat(KEY_PILL_Y, y)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "cr_speed_prefs"
        private const val KEY_SPEED = "pref_speed"
        private const val KEY_PILL_VISIBLE = "pref_pill_visible"
        private const val KEY_PILL_X = "pref_pill_x"
        private const val KEY_PILL_Y = "pref_pill_y"

        const val DEFAULT_SPEED = 1.0f
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 4.0f
        const val SPEED_STEP = 0.05f
    }
}
