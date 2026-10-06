package tv.quven.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassPillGeometryTest {

    @Test
    fun aPoint_findsTheOptionUnderIt_andNoneInAGapOrPastTheRow() {
        assertEquals(0, optionAt(x = 0f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(0, optionAt(x = 95.9f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(-1, optionAt(x = 97f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(1, optionAt(x = 98f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(2, optionAt(x = 290f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(-1, optionAt(x = 300f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(-1, optionAt(x = -1f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(-1, optionAt(x = 10f, count = 0, optionWidth = 96f, gap = 2f))
    }

    @Test
    fun aPointLetGo_findsTheNearestOption_inAGapAndPastEitherEndToo() {
        assertEquals(0, optionNearest(x = 10f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(0, optionNearest(x = -40f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(1, optionNearest(x = 97f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(1, optionNearest(x = 194f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(2, optionNearest(x = 196f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(2, optionNearest(x = 900f, count = 3, optionWidth = 96f, gap = 2f))
        assertEquals(-1, optionNearest(x = 10f, count = 0, optionWidth = 96f, gap = 2f))
    }

    @Test
    fun stretchingEdges_runTheLeadingOneAheadOfTheTrailingOne_andEdgesTogetherSlideAlike() {
        val leading = PillEdges.Stretching.stiffness(520f, leads = true)
        val trailing = PillEdges.Stretching.stiffness(520f, leads = false)

        assertTrue(leading > 520f && trailing < 520f)
        assertEquals(520f, PillEdges.Together.stiffness(520f, leads = true))
        assertEquals(520f, PillEdges.Together.stiffness(520f, leads = false))
    }

    @Test
    fun aStretchedPill_thins_asFarAsItsFloor_andAPillAtRestKeepsItsHeight() {
        assertEquals(1f, pillSquash(width = 96f, restWidth = 96f))
        assertEquals(1f, pillSquash(width = 80f, restWidth = 96f))
        assertEquals(1f - 0.35f * 0.5f, pillSquash(width = 144f, restWidth = 96f), 1e-6f)
        assertEquals(0.78f, pillSquash(width = 960f, restWidth = 96f), 1e-6f)
        assertEquals(1f, pillSquash(width = 50f, restWidth = 0f))
    }

    @Test
    fun thePillIsALens_whilePressedOrStretched_andGlassOnlyAtRest() {
        assertEquals(0f, pillLens(width = 96f, restWidth = 96f, press = 0f))
        assertEquals(1f, pillLens(width = 96f, restWidth = 96f, press = 1f))
        assertEquals(0.25f, pillLens(width = 144f, restWidth = 96f, press = 0f), 1e-6f)
        assertEquals(1f, pillLens(width = 400f, restWidth = 96f, press = 0f))
        assertEquals(0.5f, pillLens(width = 96f, restWidth = 96f, press = 0.5f))
    }
}
