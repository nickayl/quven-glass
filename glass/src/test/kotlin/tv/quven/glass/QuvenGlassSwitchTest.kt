package tv.quven.glass

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
@Config(application = Application::class, sdk = [34])
class QuvenGlassSwitchTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aSwitch_isAsLargeAsApples_andReadsAsASwitch() {
        render { QuvenGlassSwitch(checked = true, onCheckedChange = {}, modifier = Modifier.testTag(SwitchTag)) }

        val bounds = compose.onNodeWithTag(SwitchTag).getUnclippedBoundsInRoot()
        assertEquals(62f, (bounds.right - bounds.left).value, 0.5f)
        assertEquals(28f, (bounds.bottom - bounds.top).value, 0.5f)
        compose.onNodeWithTag(SwitchTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun aTap_turnsTheSwitchOver() {
        var checked by mutableStateOf(false)
        render { QuvenGlassSwitch(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag(SwitchTag)) }

        compose.onNodeWithTag(SwitchTag).performClick()
        compose.runOnIdle { assertTrue(checked) }
        compose.onNodeWithTag(SwitchTag).performClick()
        compose.runOnIdle { assertEquals(false, checked) }
    }

    @Test
    fun aDragAcross_turnsTheSwitchToTheSideItIsLetGoNearer_once() {
        val changes = mutableListOf<Boolean>()
        var checked by mutableStateOf(false)
        render {
            QuvenGlassSwitch(checked = checked, onCheckedChange = { changes += it; checked = it }, modifier = Modifier.testTag(SwitchTag))
        }

        dragThumb(from = 0.25f, to = 0.9f)

        compose.runOnIdle { assertEquals(listOf(true), changes) }
    }

    @Test
    fun aDragThatComesBack_leavesTheSwitchAsItWas() {
        val changes = mutableListOf<Boolean>()
        render { QuvenGlassSwitch(checked = false, onCheckedChange = { changes += it }, modifier = Modifier.testTag(SwitchTag)) }

        dragThumb(from = 0.25f, to = 0.9f, back = 0.2f)

        compose.runOnIdle { assertEquals(emptyList<Boolean>(), changes) }
    }

    @Test
    fun aDragAcross_inARightToLeftLayout_runsTheOtherWay() {
        val changes = mutableListOf<Boolean>()
        render(LayoutDirection.Rtl) {
            QuvenGlassSwitch(checked = false, onCheckedChange = { changes += it }, modifier = Modifier.testTag(SwitchTag))
        }

        dragThumb(from = 0.75f, to = 0.1f)

        compose.runOnIdle { assertEquals(listOf(true), changes) }
    }

    @Test
    fun aDisabledSwitch_ignoresTapsAndDrags() {
        val changes = mutableListOf<Boolean>()
        render {
            QuvenGlassSwitch(checked = false, onCheckedChange = { changes += it }, enabled = false, modifier = Modifier.testTag(SwitchTag))
        }

        compose.onNodeWithTag(SwitchTag).performClick()
        dragThumb(from = 0.25f, to = 0.9f)

        compose.runOnIdle { assertEquals(emptyList<Boolean>(), changes) }
    }

    @Test
    fun theThumb_liftsIntoTheLensWhileHeld_andSettlesBackOnceItRests() {
        render { switchOf(checked = false, liquid = true) }
        assertThumbSize(Thumb)

        compose.onNodeWithTag(SwitchTag).performTouchInput { down(Offset(width * 0.25f, height / 2f)) }
        compose.mainClock.advanceTimeBy(HeldMillis)
        assertThumbSize(Lens)

        compose.onNodeWithTag(SwitchTag).performTouchInput { cancel() }
        compose.waitForIdle()
        assertThumbSize(Thumb)
    }

    @Test
    fun theLens_staysLiftedWhileTheThumbTravels_afterATap() {
        var checked by mutableStateOf(false)
        render { switchOf(checked = checked, liquid = true) { checked = it } }
        compose.mainClock.autoAdvance = false

        compose.onNodeWithTag(SwitchTag).performTouchInput { down(Offset(width * 0.25f, height / 2f)) }
        compose.mainClock.advanceTimeBy(HeldMillis)
        compose.onNodeWithTag(SwitchTag).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(TravellingMillis)

        assertTrue(checked)
        assertThumbSize(Lens)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertThumbSize(Thumb)
    }

    @Test
    fun withoutLiquidGlass_theThumbStaysAThumb() {
        render { switchOf(checked = false, liquid = false) }

        compose.onNodeWithTag(SwitchTag).performTouchInput { down(Offset(width * 0.25f, height / 2f)) }
        compose.mainClock.advanceTimeBy(HeldMillis)

        assertThumbSize(Thumb)
    }

    @Test
    fun theLensThumb_growsAboutItsCentre_andTheWhiteThumbIsGoneBeforeTheLensClears() {
        val thumb = GlassLensThumb(Thumb, Lens)
        val density = Density(1f)

        val rest = thumb.frame(density, 40f, 14f, 0f)
        val lifted = thumb.frame(density, 40f, 14f, 1f)

        assertEquals(Offset(40f, 14f), rest.center)
        assertEquals(Offset(40f, 14f), lifted.center)
        assertEquals(57f, lifted.width, 0.001f)
        assertEquals(37.5f, lifted.height, 0.001f)
        assertEquals(1f, thumb.thumbOpacity(0f), 0f)
        assertEquals(0f, thumb.thumbOpacity(GlassLensThumb.ThumbGone), 0f)
        assertEquals(GlassLensThumb.LensFrost, thumb.material(GlassLensThumb.FrostClearStart).blur)
        assertEquals(0.dp, thumb.material(1f).blur)
        assertEquals(GlassLensThumb.LensZoom, thumb.material(1f).zoom, 0f)
    }

    @Composable
    private fun switchOf(checked: Boolean, liquid: Boolean, onCheckedChange: (Boolean) -> Unit = {}) = GlassSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(SwitchTag),
        enabled = true,
        onColor = SwitchOnColor,
        offColor = SwitchOffColor,
        reduceMotion = false,
        interactionSource = null,
        liquid = liquid,
    )

    private fun dragThumb(from: Float, to: Float, back: Float? = null) {
        compose.onNodeWithTag(SwitchTag).performTouchInput {
            down(Offset(width * from, height / 2f))
            moveTo(Offset(width * to, height / 2f))
            if (back != null) moveTo(Offset(width * back, height / 2f))
            up()
        }
        compose.waitForIdle()
    }

    // The frame is laid out on whole pixels, so half a pixel either way is the size asked for.
    private fun assertThumbSize(expected: DpSize) {
        val bounds = compose.onNodeWithTag(LensThumbTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(expected.width.value, (bounds.right - bounds.left).value, 0.5f)
        assertEquals(expected.height.value, (bounds.bottom - bounds.top).value, 0.5f)
    }

    private fun render(direction: LayoutDirection = LayoutDirection.Ltr, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) { content() }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val SwitchTag = "switch"
        const val HeldMillis = 400L
        const val TravellingMillis = 160L
        val Thumb = DpSize(36.dp, 24.dp)
        val Lens = DpSize(57.dp, 37.5.dp)
    }
}
