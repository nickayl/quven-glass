package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
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
class QuvenGlassPopoverTest {

    @get:Rule
    val compose = createComposeRule()

    private var open by mutableStateOf(true)

    @Test
    fun aControlHighOnTheScreen_opensItsPopoverBelowIt_centredOnIt() {
        render(top = 60f)

        val control = compose.onNodeWithTag(ControlTag).getUnclippedBoundsInRoot()
        val panel = compose.onNodeWithTag(PanelTag).getUnclippedBoundsInRoot()
        assertTrue(panel.top > control.bottom)
        assertEquals(((control.left + control.right) / 2f).value, ((panel.left + panel.right) / 2f).value, 1f)
    }

    @Test
    fun aControlLowOnTheScreen_opensItsPopoverAboveIt() {
        render(top = 480f)

        val control = compose.onNodeWithTag(ControlTag).getUnclippedBoundsInRoot()
        val panel = compose.onNodeWithTag(PanelTag).getUnclippedBoundsInRoot()
        assertTrue(panel.bottom < control.top)
    }

    @Test
    fun aPressElsewhere_closesThePopover() {
        render(top = 60f)

        compose.onRoot().performTouchInput { click(Offset(10f, 590f)) }

        compose.runOnIdle { assertTrue(!open) }
    }

    @Test
    fun thePoint_standsBeyondTheControlsEdge_onTheSideThePopoverOpensTo_andStaysWhole() {
        val control = Rect(100f, 100f, 200f, 140f)
        val below = popoverPoint(control, spaceHeight = 600f, density = Density(1f))
        val above = popoverPoint(control.translate(0f, 400f), spaceHeight = 600f, density = Density(1f))

        assertEquals(Rect(Offset(150f, 149f), 9f), below)
        assertEquals(Rect(Offset(150f, 491f), 9f), above)
    }

    @Test
    fun theDrop_turnsIntoAWedge_fromInsideThePanelToTheControlsEdge_pointingAtTheControl() {
        val control = Rect(100f, 100f, 200f, 140f)
        val below = popoverPoint(control, spaceHeight = 600f, density = Density(1f))
        val opening = morphGeometry(below, Rect(0f, 154f, 300f, 254f), 0.2f, 0.2f, 25f, keepsSource = true, pointBase = 30f)
        val open = morphGeometry(below, Rect(0f, 154f, 300f, 254f), 1f, 1f, 25f, keepsSource = true, pointBase = 30f)

        assertEquals(below, opening.source)
        assertEquals(0f, opening.sourceWedge, 0f)
        // Below its control the panel's point turns up, its apex on the control's edge.
        assertEquals(Rect(135f, 140f, 165f, 163f), open.source)
        assertEquals(-1f, open.sourceWedge, 0f)
    }

    private fun render(top: Float) {
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                val state = rememberQuvenGlassMorphState()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset(300.dp, top.dp).size(150.dp, 50.dp).quvenGlassAnchor(state, stretches = false).testTag(ControlTag))
                    QuvenGlassPopover(state, expanded = open, onDismissRequest = { open = false }) {
                        BasicText("A panel that points at its control.", Modifier.size(280.dp, 80.dp).testTag(PanelTag))
                    }
                }
                QuvenGlassMenuHost(host, backdrop = null)
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val ControlTag = "control"
        const val PanelTag = "panel"
    }
}
