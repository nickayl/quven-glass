package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class QuvenGlassSegmentedTrackTest {

    @get:Rule
    val compose = createComposeRule()

    private var held by mutableIntStateOf(0)
    private val draggedTo = mutableListOf<Int>()
    private var reduceMotion by mutableStateOf(false)
    private var backdrop by mutableStateOf<QuvenGlassBackdrop?>(null)

    @Test
    fun thePill_standsUnderTheHeldOption_andSettlesUnderANewlyHeldOne() {
        render()
        assertEquals(Inset, pill().left.value, 0.5f)

        compose.runOnIdle { held = 2 }
        compose.waitForIdle()

        assertEquals(Inset + 2 * Step, pill().left.value, 0.5f)
        assertEquals(OptionWidth, pill().width.value, 0.5f)
    }

    @Test
    fun aPress_takesThePillUnderThePressedOptionBeforeRelease_andACancelReturnsIt() {
        render()

        compose.onNodeWithTag(option(1)).performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals(Inset + Step, pill().left.value, 0.5f)
        assertEquals(0, held)

        compose.onNodeWithTag(option(1)).performTouchInput { cancel() }
        compose.waitForIdle()
        assertEquals(Inset, pill().left.value, 0.5f)
    }

    @Test
    fun aPressReleased_holdsTheOption_andLeavesThePillUnderIt() {
        render()

        compose.onNodeWithTag(option(2)).performClick()
        compose.waitForIdle()

        assertEquals(2, held)
        assertEquals(Inset + 2 * Step, pill().left.value, 0.5f)
    }

    @Test
    fun onItsWay_thePillStretchesAndThins_andSettlesAtItsWidth() {
        render()
        compose.mainClock.autoAdvance = false

        compose.runOnIdle { held = 2 }
        val frames = (1..20).map {
            compose.mainClock.advanceTimeByFrame()
            pill()
        }

        val widest = frames.maxBy { it.width.value }
        assertTrue("widest ${widest.width}", widest.width.value > OptionWidth + 4f)
        assertTrue("height ${widest.height}", widest.height.value < OptionHeight - 1f)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(OptionWidth, pill().width.value, 0.5f)
        assertEquals(OptionHeight, pill().height.value, 0.5f)
    }

    @Test
    fun whereMotionIsReduced_thePillNeverStretches_onItsWayToItsPlace() {
        reduceMotion = true
        render()
        compose.mainClock.autoAdvance = false

        compose.runOnIdle { held = 2 }
        repeat(20) {
            compose.mainClock.advanceTimeByFrame()
            val frame = pill()
            assertEquals(OptionWidth, frame.width.value, 0.5f)
            assertTrue(frame.left.value == Inset || frame.left.value == Inset + 2 * Step)
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(Inset + 2 * Step, pill().left.value, 0.5f)
    }

    @Test
    fun whereNoneIsHeld_thePillStaysWhereItStood() {
        render()
        compose.runOnIdle { held = 1 }
        compose.waitForIdle()

        compose.runOnIdle { held = -1 }
        compose.waitForIdle()

        assertEquals(Inset + Step, pill().left.value, 0.5f)
    }

    @Test
    fun aDrag_carriesThePillUnderTheFinger_andLettingGoOverAnotherOption_holdsIt() {
        render(followsDrags = true)
        val start = centreOf(0)

        compose.onNodeWithTag(TrackTag).performTouchInput {
            down(Offset(start, center.y))
            moveTo(Offset(start + Step * 0.75f, center.y))
            moveTo(Offset(start + Step * 1.5f, center.y))
        }
        compose.waitForIdle()
        val pill = pill()
        assertEquals(start + Step * 1.5f, (pill.left.value + pill.right.value) / 2f, 1f)

        compose.onNodeWithTag(TrackTag).performTouchInput {
            moveTo(Offset(centreOf(2), center.y))
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf(2), draggedTo)
        assertEquals(2, held)
        assertEquals(Inset + 2 * Step, pill().left.value, 0.5f)
    }

    @Test
    fun aDragLetGoOverTheOptionItStartedOn_isLeftToThatOption() {
        render(followsDrags = true)

        compose.onNodeWithTag(TrackTag).performTouchInput {
            down(Offset(centreOf(1), center.y))
            moveTo(Offset(centreOf(1) + 20f, center.y))
            moveTo(Offset(centreOf(1) - 5f, center.y))
            up()
        }
        compose.waitForIdle()

        assertEquals(emptyList<Int>(), draggedTo)
        assertEquals(Inset + held * Step, pill().left.value, 0.5f)
    }

    @Test
    fun withoutADragAnswer_thePillStaysUnderThePressedOption_whileAFingerMoves() {
        render()

        compose.onNodeWithTag(TrackTag).performTouchInput {
            down(Offset(centreOf(0), center.y))
            moveTo(Offset(centreOf(0) + Step * 0.4f, center.y))
        }
        compose.waitForIdle()

        assertEquals(Inset, pill().left.value, 0.5f)
        compose.onNodeWithTag(TrackTag).performTouchInput { up() }
    }

    @Test
    fun overABackdrop_theTrackStillLaysOutAndAnswersItsOptions() {
        backdrop = QuvenGlassBackdrop()
        render()

        compose.onNodeWithTag(option(1)).performClick()
        compose.waitForIdle()

        assertEquals(1, held)
        assertEquals(Inset + Step, pill().left.value, 0.5f)
    }

    private fun render(followsDrags: Boolean = false) {
        compose.setContent {
            Box(Modifier.size(400.dp)) {
                val current = backdrop
                if (current != null) Box(Modifier.fillMaxSize().quvenGlassSource(current))
                QuvenGlassSegmentedTrack(
                    options = listOf(0, 1, 2),
                    held = held,
                    optionSize = DpSize(OptionWidth.dp, OptionHeight.dp),
                    modifier = Modifier.testTag(TrackTag),
                    gap = Gap.dp,
                    inset = Inset.dp,
                    pillTag = PillTag,
                    backdrop = current,
                    reduceMotion = reduceMotion,
                    onDraggedTo = if (followsDrags) { option -> draggedTo += option; held = option } else null,
                ) { index, _ ->
                    Box(Modifier.fillMaxSize().testTag(option(index)).clickable { held = index })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun pill(): DpRect = compose.onNodeWithTag(PillTag).getUnclippedBoundsInRoot()

    private fun centreOf(index: Int) = Inset + index * Step + OptionWidth / 2f

    private fun option(index: Int) = "option-$index"

    private companion object {
        const val PillTag = "pill"
        const val TrackTag = "track"
        const val OptionWidth = 60f
        const val OptionHeight = 40f
        const val Gap = 4f
        const val Inset = 3f
        const val Step = OptionWidth + Gap
    }
}
