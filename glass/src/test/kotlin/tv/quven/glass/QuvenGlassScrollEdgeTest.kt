package tv.quven.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuvenGlassScrollEdgeTest {

    @Test
    fun theSoftEdge_holdsADarkLipAlongItsStart_thenFadesOverItsReach() {
        assertEquals(0.76f, softEdgeDim(0f), 0.02f)
        assertTrue(softEdgeDim(0.09f) > 0.6f)
        assertEquals(0.29f, softEdgeDim(0.19f), 0.03f)
        assertEquals(0.13f, softEdgeDim(0.57f), 0.03f)
        assertEquals(0f, softEdgeDim(1f), 0.001f)
    }

    @Test
    fun theSoftEdge_blursMostAtTheEdge_andNotAtAllPastMostOfItsReach() {
        assertEquals(1f, softEdgeBlur(0f))
        assertTrue(softEdgeBlur(0.4f) in 0.2f..0.8f)
        assertEquals(0f, softEdgeBlur(0.8f))
    }

    @Test
    fun aHardEdge_darkensTheTopOfItsBand_andNotTheRest() {
        assertEquals(0.45f, hardEdgeDim(0f), 0.001f)
        assertEquals(0f, hardEdgeDim(1f), 0.001f)
    }
}
