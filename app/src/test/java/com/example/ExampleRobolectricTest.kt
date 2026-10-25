package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CR Speed", appName)
  }

  @Test
  fun `speed preferences persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = SpeedPrefs(context)
    prefs.clear()

    assertEquals(SpeedPrefs.DEFAULT_SPEED, prefs.speed, 0.01f)
    assertTrue(prefs.isPillVisible)

    prefs.speed = 1.75f
    assertEquals(1.75f, prefs.speed, 0.01f)

    prefs.isPillVisible = false
    assertFalse(prefs.isPillVisible)

    prefs.savePillPosition(120f, 250f)
    assertEquals(120f, prefs.pillX, 0.01f)
    assertEquals(250f, prefs.pillY, 0.01f)
  }

  @Test
  fun `speed viewmodel validation and clamping`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = SpeedPrefs(context)
    val viewModel = SpeedViewModel(prefs)

    // Valid preset
    assertTrue(viewModel.setSpeed(1.5f))
    assertEquals(1.5f, viewModel.currentSpeed.value, 0.01f)

    // Under minimum limit (< 0.25)
    assertFalse(viewModel.setSpeed(0.1f))

    // Over maximum limit (> 4.0)
    assertFalse(viewModel.setSpeed(5.0f))

    // Valid custom speed with 0.05 step rounding
    assertTrue(viewModel.setSpeed(1.83f))
    assertEquals(1.85f, viewModel.currentSpeed.value, 0.01f)
  }
}
