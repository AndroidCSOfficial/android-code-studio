package com.tom.rv2ide.uidesigner

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposePreviewDetectorTest {

  @Test
  fun regexDetectsPreview() {
    val text = """
      import androidx.compose.runtime.Composable
      import androidx.compose.ui.tooling.preview.Preview

      @Preview(showBackground = true)
      @Composable
      fun MyPreview() {
      }

      fun notAPreview() {}
    """

    val found = ComposePreviewDetector.detect(text, null)
    assertEquals(listOf("MyPreview"), found)
  }

  @Test
  fun symbolsDetectsPreviewAboveFunction() {
    val text = """
      @Preview
      @Composable
      fun OtherPreview() {}

      fun somethingElse() {}
    """

    // Simulate a documentSymbol result where the function starts at line 2 (0-based)
    val symbolsJson = "[ { \"name\": \"OtherPreview\", \"kind\": 12, \"range\": { \"start\": { \"line\": 2, \"character\": 0 }, \"end\": { \"line\": 2, \"character\": 20 } } } ]"

    val found = ComposePreviewDetector.detect(text, symbolsJson)
    assertEquals(listOf("OtherPreview"), found)
  }
}
