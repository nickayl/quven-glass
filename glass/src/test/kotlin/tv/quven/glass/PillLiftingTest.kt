package tv.quven.glass

import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PillLiftingTest {

    private val rect = Rect(0f, 0f, 80f, 54f)
    private val frame = PillFrame(left = 0f, right = 80f, top = 0f, bottom = 54f, restWidth = 80f, restHeight = 54f)

    @Test
    fun withinTheGlass_aPressedPillBendsAndSwellsTheTracksGlass() {
        val motion = liftedMotion(platter = 0.4f)

        val pill = PillLifting.WithinGlass.pill(motion, frame, rect, radius = 27f)

        assertEquals(1f, pill.alpha, 0f)
        assertEquals(1f, pill.lens, 0f)
        assertEquals(1f, pill.lift, 0f)
    }

    @Test
    fun intoALens_aPressedPillLeavesTheTracksGlassAsItWas_itsPlatterAsFarAsItShows() {
        val motion = liftedMotion(platter = 0.4f)

        val pill = PillLifting.IntoLens().pill(motion, frame, rect, radius = 27f)

        assertEquals(0.4f, pill.alpha, 0.0001f)
        assertEquals(0f, pill.lens, 0f)
        assertEquals(0f, pill.lift, 0f)
    }

    @Test
    fun aFlatPill_slidesAsAPlatter_andLeavesTheTracksGlassStill() {
        val motion = liftedMotion(platter = 0.4f)

        val pill = PillLifting.Flat.pill(motion, frame, rect, radius = 27f)

        assertEquals(1f, pill.alpha, 0f)
        assertEquals(0f, pill.lens, 0f)
        assertEquals(0f, pill.lift, 0f)
        assertEquals(0f, PillLifting.Flat.trackLift(motion), 0f)
        assertEquals(1f, PillLifting.IntoLens().trackLift(motion), 0f)
    }

    @Test
    fun aSelector_neverLiftsItsPill_whileATabBarLiftsARowsIntoALens() {
        val lens = PillLifting.IntoLens()

        assertEquals(PillLifting.Flat, QuvenGlassTrackFeel.Selector.lifting(lens, liquid = true, reduceMotion = false, count = 3))
        assertEquals(lens, QuvenGlassTrackFeel.TabBar.lifting(lens, liquid = true, reduceMotion = false, count = 3))
        assertEquals(PillLifting.WithinGlass, QuvenGlassTrackFeel.TabBar.lifting(lens, liquid = true, reduceMotion = true, count = 3))
        assertEquals(0f, QuvenGlassTrackFeel.Selector.material(QuvenGlassStyle.Standard).pressGrowth, 0f)
    }

    private fun liftedMotion(platter: Float): GlassPillMotion = GlassPillMotion().apply {
        runBlocking {
            alpha.snapTo(1f)
            press.snapTo(1f)
            this@apply.platter.snapTo(platter)
        }
    }
}
