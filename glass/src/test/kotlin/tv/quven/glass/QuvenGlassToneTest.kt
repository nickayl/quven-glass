package tv.quven.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuvenGlassToneTest {

    @Test
    fun thinGlass_lightensMostlyGrey_whileLargeGlassTakesTheBackdropsColour() {
        assertTrue(QuvenGlassTone.Thin.veilColour < 0.5f)
        assertEquals(1f, QuvenGlassTone.Large.veilColour, 0f)
        assertEquals(1f, QuvenGlassTone.Menu.veilColour, 0f)
    }

    @Test
    fun aToneBetweenTwo_takesEachPropertyInProportion_theVeilsColourIncluded() {
        val between = lerpTone(QuvenGlassTone.Thin, QuvenGlassTone.Large, 0.5f)

        assertEquals((QuvenGlassTone.Thin.veilColour + 1f) / 2f, between.veilColour, 0.0001f)
        assertEquals((QuvenGlassTone.Thin.knee + QuvenGlassTone.Large.knee) / 2f, between.knee, 0.0001f)
    }
}
