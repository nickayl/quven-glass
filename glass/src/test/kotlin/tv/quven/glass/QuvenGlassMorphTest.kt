package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class QuvenGlassMorphTest {

    @get:Rule
    val compose = createComposeRule()

    private var expanded by mutableStateOf(false)
    private val state = QuvenGlassMorphState()

    @Test
    fun hangingFromTopRight_alignsTheRightEdges_andKeepsTheGlassInsideTheSpace() {
        val placement = QuvenGlassMorphPlacement.hangingFromTopRight(edge = 10.dp)

        assertEquals(IntOffset(560, 40), placement.place(IntSize(200, 300), IntRect(720, 40, 760, 80), Space, Unit))
        assertEquals(IntOffset(10, 290), placement.place(IntSize(200, 300), IntRect(0, 700, 40, 740), Space, Unit))
    }

    @Test
    fun hangingFromTopLeft_alignsTheLeftEdges() {
        val placement = QuvenGlassMorphPlacement.hangingFromTopLeft(edge = 10.dp)

        assertEquals(IntOffset(40, 40), placement.place(IntSize(200, 300), IntRect(40, 40, 80, 80), Space, Unit))
    }

    @Test
    fun above_centresTheGlassOverTheAnchor_andKeepsItInsideTheSides() {
        val placement = QuvenGlassMorphPlacement.above(gap = 8.dp, edge = 10.dp)

        assertEquals(IntOffset(300, 392), placement.place(IntSize(200, 100), IntRect(380, 500, 420, 540), Space, Unit))
        assertEquals(IntOffset(590, 392), placement.place(IntSize(200, 100), IntRect(760, 500, 800, 540), Space, Unit))
    }

    @Test
    fun opening_laysThePanelWhereThePlacementStandsIt_andTheStateReadsShown() {
        render()

        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        val panel = compose.onNodeWithTag(PanelTag).getUnclippedBoundsInRoot()
        assertEquals(200f - PanelWidth, panel.left.value, 0.5f)
        assertEquals(20f, panel.top.value, 0.5f)
        assertTrue(state.isShown)
    }

    @Test
    fun closing_returnsTheGlassIntoItsControl_andLeavesNoPanel() {
        render()
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        compose.runOnIdle { expanded = false }
        compose.waitForIdle()

        compose.onNodeWithTag(PanelTag).assertDoesNotExist()
        assertFalse(state.isShown)
    }

    @Test
    fun closing_keepsTheControlHidden_untilTheSpringHasSettled() {
        render()
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { expanded = false }
        var frames = 0
        do {
            compose.mainClock.advanceTimeByFrame()
            assertTrue("The control showed at frame $frames while the glass still moved", state.isShown || !state.progress.isRunning)
            frames++
        } while ((frames < SettleFrames / 10 || state.progress.isRunning) && frames < SettleFrames)

        assertFalse(state.isShown)
    }

    @Test
    fun thePanel_isComposedOnceAndKeptWhileClosed_soAnOpeningComposesNothing() {
        var compositions = 0
        render { remember { compositions++ } }

        repeat(2) {
            compose.runOnIdle { expanded = true }
            compose.waitForIdle()
            compose.runOnIdle { expanded = false }
            compose.waitForIdle()
        }

        assertEquals(1, compositions)
        compose.onNodeWithTag(PanelTag).assertDoesNotExist()
    }

    @Test
    fun underReducedMotion_thePanelStandsOpenAndClosesAway() {
        render(reduceMotion = true)

        compose.runOnIdle { expanded = true }
        compose.waitForIdle()
        assertEquals(20f, compose.onNodeWithTag(PanelTag).getUnclippedBoundsInRoot().top.value, 0.5f)

        compose.runOnIdle { expanded = false }
        compose.waitForIdle()
        compose.onNodeWithTag(PanelTag).assertDoesNotExist()
    }

    private fun render(reduceMotion: Boolean = false, inside: @Composable () -> Unit = {}) {
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                QuvenGlassMorph(
                    state = state,
                    expanded = expanded,
                    anchor = Rect(160f, 20f, 200f, 60f),
                    width = PanelWidth.dp,
                    placement = QuvenGlassMorphPlacement.hangingFromTopRight(),
                    modifier = Modifier.fillMaxSize(),
                    backdrop = null,
                    reduceMotion = reduceMotion,
                ) {
                    Box(Modifier.testTag(PanelTag).height(120.dp)) { inside() }
                }
            }
        }
    }

    private companion object {
        const val PanelTag = "panel"
        const val PanelWidth = 150f
        const val SettleFrames = 200
        val Space = IntSize(800, 600)
        val Unit = Density(1f)
    }
}
