package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import kotlinx.coroutines.flow.first

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
    fun overAnchor_hangsFromAnAnchorWithRoomBelow_alignedWithItsNearerSide() {
        val placement = QuvenGlassMorphPlacement.overAnchor(edge = 10.dp)

        assertEquals(IntOffset(546, 40), placement.place(IntSize(200, 300), IntRect(700, 40, 746, 86), Space, Unit))
        assertEquals(IntOffset(40, 40), placement.place(IntSize(200, 300), IntRect(40, 40, 86, 86), Space, Unit))
    }

    @Test
    fun overAnchor_risesFromTheAnchorsFoot_whereThereIsMoreRoomAbove() {
        val placement = QuvenGlassMorphPlacement.overAnchor(edge = 10.dp)

        assertEquals(IntOffset(546, 87), placement.place(IntSize(200, 350), IntRect(700, 391, 746, 437), IntSize(800, 820), Unit))
        assertEquals(IntOffset(40, 146), placement.place(IntSize(200, 200), IntRect(40, 300, 86, 346), Space, Unit))
    }

    @Test
    fun overAnchor_keepsATallGlassInsideTheSpace() {
        val placement = QuvenGlassMorphPlacement.overAnchor(edge = 10.dp)

        assertEquals(IntOffset(546, 10), placement.place(IntSize(200, 580), IntRect(700, 300, 746, 346), Space, Unit))
    }

    @Test
    fun above_centresTheGlassOverTheAnchor_andKeepsItInsideTheSides() {
        val placement = QuvenGlassMorphPlacement.above(gap = 8.dp, edge = 10.dp)

        assertEquals(IntOffset(300, 392), placement.place(IntSize(200, 100), IntRect(380, 500, 420, 540), Space, Unit))
        assertEquals(IntOffset(590, 392), placement.place(IntSize(200, 100), IntRect(760, 500, 800, 540), Space, Unit))
    }

    @Test
    fun morphRadius_keepsTheGlassACapsuleWhileItGrows_andSettlesOnTheOpenRadius() {
        val growing = Size(120f, 80f)

        assertEquals(40f, morphRadius(growing, cornerRadius = 25f, progress = 0.3f), 0.001f)
        assertEquals(40f, morphRadius(growing, cornerRadius = 25f, progress = 0.6f), 0.001f)
        assertEquals(32.5f, morphRadius(growing, cornerRadius = 25f, progress = 0.8f), 0.001f)
        assertEquals(25f, morphRadius(Size(250f, 400f), cornerRadius = 25f, progress = 1f), 0.001f)
        assertEquals(25f, morphRadius(Size(250f, 400f), cornerRadius = 25f, progress = 1.04f), 0.001f)
    }

    @Test
    fun morphRadius_neverRoundsPastACapsule() {
        assertEquals(10f, morphRadius(Size(20f, 20f), cornerRadius = 25f, progress = 1f), 0.001f)
    }

    @Test
    fun morphGeometry_atTheRelease_keepsTheCapAtTheControlsTop_andTheDropInsideTheControl() {
        val geometry = morphGeometry(Control, Hanging, spread = 0f, reach = 0f, cornerRadius = 25f)

        assertEquals(Control.top, geometry.source.top, 0.001f)
        assertTrue(geometry.source.width < Control.width)
        assertTrue(geometry.body.left > Control.left && geometry.body.right < Control.right)
        assertEquals(Control.bottom, geometry.body.bottom, 0.001f)
    }

    @Test
    fun morphGeometry_dropsAlongItsWayAsItSpreads_andLeavesTheControlOnlyAtTheEnd() {
        val falling = morphGeometry(Control, Hanging, spread = 0.6f, reach = 0.75f, cornerRadius = 25f)

        assertEquals(Control.bottom + (Hanging.bottom - Control.bottom) * 0.75f, falling.body.bottom, 0.001f)
        assertEquals(Control.center.y, falling.body.top, 0.001f)

        val open = morphGeometry(Control, Hanging, spread = 1f, reach = 1f, cornerRadius = 25f)
        assertEquals(Hanging, open.body)
        assertEquals(25f, open.bodyRadius, 0.001f)
        assertEquals(0f, open.source.width, 0.001f)
    }

    @Test
    fun morphGeometry_aRisingGlass_dropsUpwards_andKeepsItsCapAtTheControlsFoot() {
        val rising = Rect(Control.left, Control.bottom - 300f, Control.left + 220f, Control.bottom)
        val geometry = morphGeometry(Control, rising, spread = 0.3f, reach = 0.5f, cornerRadius = 25f)

        assertEquals(Control.top + (rising.top - Control.top) * 0.5f, geometry.body.top, 0.001f)
        assertEquals(Control.center.y, geometry.body.bottom, 0.001f)
        assertEquals(Control.bottom, geometry.source.bottom, 0.001f)
    }

    @Test
    fun opening_dropsTheGlassAlongItsWayAheadOfSpreadingIt() {
        var lead = 0f
        render {
            LaunchedEffect(Unit) {
                snapshotFlow { state.progress.value to state.reach.value }.first { (spread, _) -> spread >= HalfSpread }
                    .let { (spread, reach) -> lead = reach - spread }
            }
        }

        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        assertTrue("The glass's drop led its spread by $lead at half its spread", lead > DropLead)
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
    fun closing_returnsIntoTheControlWithinAQuarterOfASecond_withoutSwingingPastIt() {
        render()
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { expanded = false }
        var elapsed = 0L
        while (state.isShown && elapsed < SettleFrames * FrameMillis) {
            compose.mainClock.advanceTimeBy(FrameMillis)
            elapsed += FrameMillis
            assertTrue("The glass swung past its control at $elapsed ms", state.progress.value >= 0f)
        }

        assertFalse(state.isShown)
        assertTrue("The glass took $elapsed ms to close", elapsed <= 250L)
    }

    @Test
    fun closing_stretchesTheControlAsTheGlassLands_andSettlesItBack() {
        var longest = 0f
        render {
            LaunchedEffect(Unit) { snapshotFlow { state.landing.value }.collect { longest = maxOf(longest, it) } }
        }
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()
        assertEquals(0f, longest, 0.001f)

        compose.runOnIdle { expanded = false }
        compose.waitForIdle()

        assertEquals(1f, longest, 0.1f)
        assertEquals(0f, state.landing.value, 0.01f)
    }

    @Test
    fun thePanel_isComposedOnceAndKeptWhileClosed_soAnOpeningComposesNothing() {
        var compositions = 0
        render { remember<Int> { ++compositions } }

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
        const val FrameMillis = 16L
        const val HalfSpread = 0.5f
        const val DropLead = 0.05f
        val Control = Rect(20f, 20f, 89f, 89f)
        val Hanging = Rect(20f, 20f, 243f, 420f)
        val Space = IntSize(800, 600)
        val Unit = Density(1f)
    }
}
