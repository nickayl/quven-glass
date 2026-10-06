package tv.quven.glass

import android.app.Application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.CoroutineScope
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

    private fun dynamics(edges: PillEdges): PillDynamics =
        PillDynamics(QuvenGlassStyle.Standard.slideDamping, QuvenGlassStyle.Standard.slideStiffness, edges, PressSprings(QuvenGlassStyle.Standard.slideSpring()))

    private companion object {
        const val Width = 80f
        const val Gap = 2f
    }
}
