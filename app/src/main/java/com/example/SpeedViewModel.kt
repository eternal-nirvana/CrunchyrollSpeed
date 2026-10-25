package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.round

/**
 * ViewModel managing playback speed state and pill visibility.
 */
class SpeedViewModel(private val prefs: SpeedPrefs) : ViewModel() {

    private val _currentSpeed = MutableStateFlow(prefs.speed)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private val _isPillVisible = MutableStateFlow(prefs.isPillVisible)
    val isPillVisible: StateFlow<Boolean> = _isPillVisible.asStateFlow()

    private val _isPanelExpanded = MutableStateFlow(false)
    val isPanelExpanded: StateFlow<Boolean> = _isPanelExpanded.asStateFlow()

    // Preset speeds as specified in requirements
    val presets = listOf(1.0f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f)

    /**
     * Validates and sets a new playback speed.
     * Clamps between 0.25 and 4.0 and rounds to nearest 0.05 step.
     * Returns true if valid and applied, false otherwise.
     */
    fun setSpeed(speed: Float): Boolean {
        if (speed < SpeedPrefs.MIN_SPEED || speed > SpeedPrefs.MAX_SPEED) {
            return false
        }
        // Round to nearest 0.05 step to avoid floating-point drift
        val rounded = (round(speed / SpeedPrefs.SPEED_STEP) * SpeedPrefs.SPEED_STEP)
        val clamped = rounded.coerceIn(SpeedPrefs.MIN_SPEED, SpeedPrefs.MAX_SPEED)

        _currentSpeed.value = clamped
        prefs.speed = clamped
        return true
    }

    /**
     * Adjusts the speed by a delta (e.g. +0.05 or -0.05)
     */
    fun adjustSpeedBy(delta: Float) {
        val next = _currentSpeed.value + delta
        setSpeed(next)
    }

    /**
     * Updates visibility of the floating pill.
     */
    fun setPillVisible(visible: Boolean) {
        _isPillVisible.value = visible
        prefs.isPillVisible = visible
        if (!visible) {
            // Also collapse panel if hiding
            _isPanelExpanded.value = false
        }
    }

    /**
     * Sets panel expanded state (opened or collapsed)
     */
    fun setPanelExpanded(expanded: Boolean) {
        _isPanelExpanded.value = expanded
    }

    /**
     * Saves last pill coordinates (X, Y)
     */
    fun savePillPosition(x: Float, y: Float) {
        prefs.savePillPosition(x, y)
    }

    fun getSavedPillPosition(): Pair<Float, Float> {
        return Pair(prefs.pillX, prefs.pillY)
    }

    class Factory(private val prefs: SpeedPrefs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SpeedViewModel::class.java)) {
                return SpeedViewModel(prefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
