package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w800dp-h600dp")
class QuvenGlassSubmenuTest {

    @get:Rule
    val compose = createComposeRule()

    private var expanded by mutableStateOf(true)
    private var chosen = ""
    private val morph = QuvenGlassMorphState()

    @Test
    fun aSubmenuEntry_opensItsMenuOverTheRow_headedByItsName_andKeepsTheMenuOpen() {
        render()

        compose.onNodeWithText("Share").performClick()
        compose.waitForIdle()

        val row = compose.onAllNodesWithText("Share").onFirst().getUnclippedBoundsInRoot()
        val head = compose.onAllNodesWithText("Share").onLast().getUnclippedBoundsInRoot()
        assertEquals((row.top + row.bottom).value / 2f, (head.top + head.bottom).value / 2f, 1f)
        assertTrue(compose.onNodeWithText("Message").fetchSemanticsNode().layoutInfo.isPlaced)
        assertTrue(expanded)
    }

    @Test
    fun aPressOnTheHead_closesTheSubmenuAlone() {
        render()
        compose.onNodeWithText("Share").performClick()
        compose.waitForIdle()

        compose.onAllNodesWithText("Share").onLast().performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Message").assertDoesNotExist()
        assertTrue(expanded)
    }

    @Test
    fun aChoiceInTheSubmenu_runsIt_andClosesBothMenus() {
        render()
        compose.onNodeWithText("Share").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Message").performClick()
        compose.waitForIdle()

        assertEquals("Message", chosen)
        assertTrue(!expanded)
    }

    private fun render() {
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset(700.dp, 40.dp).size(46.dp).quvenGlassAnchor(morph))
                    QuvenGlassDropdown(state = morph, expanded = expanded, onDismissRequest = { expanded = false }) {
                        QuvenGlassMenuItem("Download", {})
                        QuvenGlassSubmenu("Share") {
                            QuvenGlassMenuItem("Message", { chosen = "Message" })
                            QuvenGlassMenuItem("Mail", { chosen = "Mail" })
                        }
                    }
                    QuvenGlassMenuHost(host, backdrop = null, metrics = QuvenGlassMenuMetrics.Phone)
                }
            }
        }
        compose.waitForIdle()
    }
}
