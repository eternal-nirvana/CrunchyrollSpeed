package com.example

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit tests for PlayerJs script generation and playback rate formatting.
 */
class ExampleUnitTest {

  @Test
  fun testPlayerJsScriptContent() {
    val script = PlayerJs.getInjectionScript(1.75f)
    assertTrue(script.contains("1.75"))
    assertTrue(script.contains("playbackRate"))
    assertTrue(script.contains("MutationObserver"))
    assertTrue(script.contains("setCrSpeed"))
    assertTrue(script.contains("ratechange"))
    assertTrue(script.contains("loadedmetadata"))
  }

  @Test
  fun testSetSpeedScriptContent() {
    val script = PlayerJs.getSetSpeedScript(2.25f)
    assertTrue(script.contains("2.25"))
    assertTrue(script.contains("setCrSpeed"))
  }
}
