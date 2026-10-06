package tv.quven.glass

import android.app.Application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class GlassPillMotionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun thePlatter_goesAtOnceAsThePillLifts_andComesBackOnlyAMomentAfterItSettles() {
        val motion = GlassPillMotion()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LaunchedEffect(Unit) {
                motion.showPlatter(shown = false)
                motion.showPlatter(shown = true)
            }
        }

        compose.mainClock.advanceTimeBy(80)
        assertEquals(0f, motion.platter.value, 0.001f)

        compose.mainClock.advanceTimeBy(16)
        assertEquals("The platter came back before the lens had gone", 0f, motion.platter.value, 0.001f)

        compose.mainClock.advanceTimeBy(250)
        assertEquals(1f, motion.platter.value, 0.001f)
    }

    @Test
    fun stretchingEdges_widenThePillOnItsWay_whileEdgesTogetherKeepItsWidth() {
        val stretching = GlassPillMotion()
        val together = GlassPillMotion()
        lateinit var scope: CoroutineScope
        compose.mainClock.autoAdvance = false
        compose.setContent { scope = rememberCoroutineScope() }
        compose.mainClock.advanceTimeByFrame()
        fun moveBoth(index: Int) = compose.runOnIdle {
            scope.launch { stretching.moveTo(index, Width, Width + Gap, dynamics(PillEdges.Stretching), reduceMotion = false) }
            scope.launch { together.moveTo(index, Width, Width + Gap, dynamics(PillEdges.Together), reduceMotion = false) }
        }
        moveBoth(0)
        compose.mainClock.advanceTimeByFrame()

        moveBoth(2)
        val widths = (1..30).map {
            compose.mainClock.advanceTimeByFrame()
            stretching.end.value - stretching.start.value to together.end.value - together.start.value
        }

        assertTrue("The stretching pill grew only to ${widths.maxOf { it.first }}", widths.maxOf { it.first } > Width + 4f)
        widths.forEach { (_, width) -> assertEquals(Width, width, 0.01f) }
        assertEquals(2 * (Width + Gap), together.start.value, 0.5f)
    }

    @Test
    fun aMoveCutShort_thenAMoveThatNeedNotTravel_stillSettlesTheLens() {
        val motion = GlassPillMotion()
        lateinit var scope: CoroutineScope
        compose.mainClock.autoAdvance = false
        compose.setContent { scope = rememberCoroutineScope() }
        compose.mainClock.advanceTimeByFrame()
        val moves = dynamics(PillEdges.Together)
        compose.runOnIdle { scope.launch { motion.moveTo(0, Width, Width + Gap, moves, reduceMotion = false) } }
        compose.mainClock.advanceTimeByFrame()
        lateinit var cutShort: Job
        compose.runOnIdle { cutShort = scope.launch { motion.moveTo(2, Width, Width + Gap, moves, reduceMotion = false) } }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { cutShort.cancel() }
        // A drag lets go right over the option, so the pill stands where the next move sends it.
        compose.runOnIdle {
            scope.launch {
                motion.follow(2 * (Width + Gap) + Width / 2f, Width, 3 * Width + 2 * Gap)
                motion.moveTo(2, Width, Width + Gap, moves, reduceMotion = false)
            }
        }
        compose.mainClock.advanceTimeBy(400)

        assertEquals(0f, motion.travel.value, 0.001f)
    }

    @Test
    fun theLens_staysLiftedUntilThePillHasNearlyArrived_thenSettles() {
        val motion = GlassPillMotion()
        lateinit var scope: CoroutineScope
        compose.mainClock.autoAdvance = false
        compose.setContent { scope = rememberCoroutineScope() }
        compose.mainClock.advanceTimeByFrame()
        // The tab bar's own spring, slow enough for a lens held a fixed while to settle before the pill arrives.
        val moves = PillDynamics(0.96f, 324f, PillEdges.Together, PressSprings(QuvenGlassStyle.Standard.slideSpring()))
        compose.runOnIdle { scope.launch { motion.moveTo(0, Width, Width + Gap, moves, reduceMotion = false) } }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { scope.launch { motion.moveTo(2, Width, Width + Gap, moves, reduceMotion = false) } }
        val target = 2 * (Width + Gap)

        val lifts = (1..60).map {
            compose.mainClock.advanceTimeByFrame()
            (motion.start.value / target) to motion.travel.value
        }

        // A tap lifts the lens to three fifths, and it holds all of that until the pill has nearly arrived.
        val onTheWay = lifts.filter { it.first in 0.15f..0.88f }
        assertTrue("The lens dropped before the pill had nearly arrived: $onTheWay", onTheWay.all { it.second >= 0.55f })
        assertEquals(0f, lifts.last().second, 0.001f)
    }

    private fun dynamics(edges: PillEdges): PillDynamics =
        PillDynamics(QuvenGlassStyle.Standard.slideDamping, QuvenGlassStyle.Standard.slideStiffness, edges, PressSprings(QuvenGlassStyle.Standard.slideSpring()))

    private companion object {
        const val Width = 80f
        const val Gap = 2f
    }
}
