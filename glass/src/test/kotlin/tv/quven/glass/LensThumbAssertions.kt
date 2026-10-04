package tv.quven.glass

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import org.junit.Assert.assertEquals

/**
 * Asserts that the lens thumb a control draws is [expected] in size. The frame is laid out on whole pixels, so half a
 * pixel either way is the size asked for.
 *
 * @param expected The size the thumb is expected to be.
 */
internal fun ComposeContentTestRule.assertLensThumbSize(expected: DpSize) {
    val bounds = onNodeWithTag(LensThumbTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
    assertEquals(expected.width.value, (bounds.right - bounds.left).value, 0.5f)
    assertEquals(expected.height.value, (bounds.bottom - bounds.top).value, 0.5f)
}
