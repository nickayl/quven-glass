package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w800dp-h600dp")
class QuvenGlassAlertTest {

    @get:Rule
    val compose = createComposeRule()

    private var shown by mutableStateOf(true)
    private val chosen = mutableListOf<String>()
    private var dismissals = 0
    private var pressesBehind = 0
    private var actionNames by mutableStateOf(emptyList<String>())

    @Test
    fun anAlert_drawsItsTitleMessageAndActions_andAChoiceRunsIt() {
        render(actions = listOf("Cancel", "Remove"))

        compose.onNodeWithText("Remove this collection?").assertExists()
        compose.onNodeWithText("Its titles stay in the library.").assertExists()
        compose.onNodeWithText("Remove").performClick()

        compose.runOnIdle { assertEquals(listOf("Remove"), chosen) }
    }

    @Test
    fun twoActions_standSideBySide_andThreeOneAboveAnother() {
        render(actions = listOf("Cancel", "Remove"))
        val cancel = compose.onNodeWithTag("action-Cancel").getUnclippedBoundsInRoot()
        val remove = compose.onNodeWithTag("action-Remove").getUnclippedBoundsInRoot()
        assertEquals(cancel.top, remove.top)
        assertTrue(cancel.right < remove.left)

        compose.runOnIdle { actionNames = listOf("Save", "Discard", "Cancel") }
        compose.waitForIdle()
        val save = compose.onNodeWithTag("action-Save").getUnclippedBoundsInRoot()
        val discard = compose.onNodeWithTag("action-Discard").getUnclippedBoundsInRoot()
        assertEquals(save.left, discard.left)
        assertTrue(save.bottom < discard.top)
    }

    @Test
    fun aPressOutsideTheAlert_neitherClosesItNorReachesWhatLiesUnder() {
        render(actions = listOf("Cancel", "Remove"))

        compose.onRoot().performTouchInput { click(Offset(10f, 10f)) }

        compose.runOnIdle {
            assertEquals(0, dismissals)
            assertEquals(0, pressesBehind)
        }
        compose.onNodeWithText("Remove this collection?").assertExists()
    }

    @Test
    fun anAlertNoLongerDrawn_fadesAway_andTheHostLetsItGo() {
        render(actions = listOf("Cancel", "Remove"))

        compose.runOnIdle { shown = false }
        compose.waitForIdle()

        compose.onNodeWithText("Remove this collection?").assertDoesNotExist()
        compose.onRoot().performTouchInput { click(Offset(10f, 10f)) }
        compose.runOnIdle { assertEquals(1, pressesBehind) }
    }

    @Test(expected = IllegalStateException::class)
    fun anAlertWithNoHost_fails() {
        compose.setContent { QuvenGlassAlert(title = "Alone", actions = emptyList()) }
        compose.waitForIdle()
    }

    private fun render(actions: List<String>) {
        actionNames = actions
        val host = QuvenGlassAlertHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassAlertHost provides host) {
                Box(Modifier.fillMaxSize().clickable { pressesBehind++ }) {
                    if (shown) {
                        QuvenGlassAlert(
                            title = "Remove this collection?",
                            message = "Its titles stay in the library.",
                            actions = actionNames.map { name ->
                                QuvenGlassAlertAction(name, { chosen += name }, modifier = Modifier.testTag("action-$name"))
                            },
                            onDismissRequest = { dismissals++ },
                        )
                    }
                }
                QuvenGlassAlertHost(host, backdrop = null)
            }
        }
        compose.waitForIdle()
    }
}
