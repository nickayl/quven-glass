package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w400dp-h800dp")
class QuvenGlassSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private var shown by mutableStateOf(true)
    private var dismissals = 0

    @Test
    fun aSheet_opensAtHalfTheWindow() {
        render()

        val title = compose.onNodeWithText(Title).getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        assertEquals(root.bottom.value * HalfWindowTop, title.top.value - ContentOffset, Tolerance)
    }

    @Test
    fun drawnUp_theSheetRestsAtFullHeight() {
        render()

        compose.onNodeWithText(Title).performTouchInput { swipeUp(startY = top, endY = top - 600f, durationMillis = 300) }
        compose.waitForIdle()

        val title = compose.onNodeWithText(Title).getUnclippedBoundsInRoot()
        assertEquals(ContentOffset, title.top.value, Tolerance)
        assertEquals(0, dismissals)
    }

    @Test
    fun drawnDownPastItsLowestRest_theSheetAsksToClose() {
        render()

        compose.onNodeWithText(Title).performTouchInput { swipeDown(startY = top, endY = top + 500f, durationMillis = 200) }
        compose.waitForIdle()

        assertEquals(1, dismissals)
    }

    @Test
    fun aPressOnTheDimmedScreen_asksToClose() {
        render()

        compose.onRoot().performTouchInput { click(Offset(10f, 10f)) }

        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun aSheetNoLongerDrawn_slidesAway_andTheHostLetsItGo() {
        render()

        compose.runOnIdle { shown = false }
        compose.waitForIdle()

        compose.onNodeWithText(Title).assertDoesNotExist()
    }

    @Test
    fun accessibilityServices_canCloseTheSheet() {
        render()

        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).onFirst().performSemanticsAction(SemanticsActions.Dismiss)

        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test(expected = IllegalStateException::class)
    fun aSheetWithNoHost_fails() {
        compose.setContent { QuvenGlassSheet(onDismissRequest = {}) {} }
        compose.waitForIdle()
    }

    private fun render() {
        val host = QuvenGlassSheetHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassSheetHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    if (shown) {
                        QuvenGlassSheet(onDismissRequest = { dismissals++; shown = false }) {
                            BasicText(Title, Modifier.testTag(Title))
                        }
                    }
                }
                QuvenGlassSheetHost(host, backdrop = null)
            }
        }
        compose.waitForIdle()
        assertTrue(dismissals == 0)
    }

    private companion object {
        const val Title = "Sheet"
        const val HalfWindowTop = 0.473f
        const val ContentOffset = 16f
        const val Tolerance = 2f
    }
}
