package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
@OptIn(ExperimentalTestApi::class)
class QuvenGlassSliderTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aSlider_isAsTallAsApples_andReadsAsARange() {
        render { slider(value = 0.25f) }

        val bounds = compose.onNodeWithTag(SliderTag).getUnclippedBoundsInRoot()
        assertEquals(Width, (bounds.right - bounds.left).value, 0.5f)
        assertEquals(31f, (bounds.bottom - bounds.top).value, 0.5f)
        compose.onNodeWithTag(SliderTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.25f, 0f..1f, 0)))
    }

    @Test
    fun aDragFromTheThumb_movesItByTheShareOfTheTrackTheFingerTravels() {
        var value by mutableFloatStateOf(0.5f)
        render { slider(value = value, onValueChange = { value = it }) }

        drag(from = thumbCentre(0.5f), by = Travel / 4f)

        compose.runOnIdle { assertEquals(0.75f, value, 0.01f) }
    }

    @Test
    fun aDragThatStartsAwayFromTheThumb_changesNothing() {
        var value by mutableFloatStateOf(0.5f)
        render { slider(value = value, onValueChange = { value = it }) }

        drag(from = thumbCentre(0.1f), by = Travel / 4f)

        compose.runOnIdle { assertEquals(0.5f, value, 0f) }
    }

    @Test
    fun aSliderWithSteps_restsOnThem() {
        var value by mutableFloatStateOf(2f)
        render { slider(value = value, onValueChange = { value = it }, range = 0f..5f, steps = 4) }

        drag(from = thumbCentre(0.4f), by = Travel * 0.17f)

        compose.runOnIdle { assertEquals(3f, value, 0f) }
    }

    @Test
    fun aDragInARightToLeftLayout_runsTheOtherWay() {
        var value by mutableFloatStateOf(0.5f)
        render(LayoutDirection.Rtl) { slider(value = value, onValueChange = { value = it }) }

        drag(from = Width - thumbCentre(0.5f), by = -Travel / 4f)

        compose.runOnIdle { assertEquals(0.75f, value, 0.01f) }
    }

    @Test
    fun aDisabledSlider_ignoresDrags() {
        var value by mutableFloatStateOf(0.5f)
        render { slider(value = value, onValueChange = { value = it }, enabled = false) }

        drag(from = thumbCentre(0.5f), by = Travel / 4f)

        compose.runOnIdle { assertEquals(0.5f, value, 0f) }
    }

    @Test
    fun theArrowKeys_stepTheThumb_aTenthOfTheRangeWithoutSteps() {
        var value by mutableFloatStateOf(0.5f)
        var finished = 0
        render { slider(value = value, onValueChange = { value = it }, onFinished = { finished++ }) }

        compose.onNodeWithTag(SliderTag).requestFocus()
        compose.onNodeWithTag(SliderTag).performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(0.6f, value, 0.001f) }
        compose.onNodeWithTag(SliderTag).performKeyInput { pressKey(Key.DirectionLeft) }
        compose.onNodeWithTag(SliderTag).performKeyInput { pressKey(Key.DirectionLeft) }

        compose.runOnIdle {
            assertEquals(0.4f, value, 0.001f)
            assertEquals(3, finished)
        }
    }

    @Test
    fun anAccessibilityService_setsTheValue_onAStep() {
        var value by mutableFloatStateOf(1f)
        render { slider(value = value, onValueChange = { value = it }, range = 0f..5f, steps = 4) }

        compose.onNodeWithTag(SliderTag).performSemanticsAction(SemanticsActions.SetProgress) { it(3.4f) }

        compose.runOnIdle { assertEquals(3f, value, 0f) }
    }

    @Test
    fun theThumb_liftsIntoTheLensWhileHeld_andSettlesBackOnceLetGo() {
        render { slider(value = 0.5f) }

        compose.onNodeWithTag(SliderTag).performTouchInput { down(Offset(thumbCentre(0.5f), height / 2f)) }
        compose.mainClock.advanceTimeBy(HeldMillis)
        compose.assertLensThumbSize(GlassLensThumb.Control.lens)

        compose.onNodeWithTag(SliderTag).performTouchInput { up() }
        compose.waitForIdle()
        compose.assertLensThumbSize(GlassLensThumb.Control.thumb)
    }

    @Test
    fun theScale_snapsToItsSteps_andHoldsToItsRange() {
        val scale = SliderScale(0f..10f, steps = 4)

        assertEquals(4f, scale.snap(3.1f), 0f)
        assertEquals(10f, scale.snap(12f), 0f)
        assertEquals(0.5f, scale.shareOf(5f), 0f)
        assertEquals(2f, scale.keyStep, 0f)
        assertEquals(1f, SliderScale(0f..10f, steps = 0).keyStep, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aScaleOfNegativeSteps_isRefused() {
        SliderScale(0f..1f, steps = -1)
    }

    @Composable
    private fun slider(
        value: Float,
        onValueChange: (Float) -> Unit = {},
        range: ClosedFloatingPointRange<Float> = 0f..1f,
        steps: Int = 0,
        enabled: Boolean = true,
        onFinished: () -> Unit = {},
    ) = GlassSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(Width.dp).testTag(SliderTag),
        enabled = enabled,
        scale = SliderScale(range, steps),
        onValueChangeFinished = onFinished,
        activeColor = SliderActiveColor,
        inactiveColor = SliderInactiveColor,
        reduceMotion = false,
        interactionSource = null,
        liquid = true,
    )

    private fun drag(from: Float, by: Float) {
        compose.onNodeWithTag(SliderTag).performTouchInput {
            down(Offset(from, height / 2f))
            moveTo(Offset(from + by / 2f, height / 2f))
            moveTo(Offset(from + by, height / 2f))
            up()
        }
        compose.waitForIdle()
    }

    private fun thumbCentre(share: Float): Float = ThumbWidth / 2f + Travel * share

    private fun render(direction: LayoutDirection = LayoutDirection.Ltr, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) { content() }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val SliderTag = "slider"
        const val Width = 300f
        const val ThumbWidth = 36f
        const val Travel = Width - ThumbWidth
        const val HeldMillis = 400L
    }
}
