package tv.quven.glass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuvenGlassTouchLightTest {

    @Test
    fun interactiveGlass_brightensItsOwnColoursUnderTheFinger_andPlainGlassFollowsNoFinger() {
        val interactive = QuvenGlassStyle.Standard.interactive()

        assertFalse(QuvenGlassStyle.Standard.followsFinger)
        assertTrue(interactive.followsFinger)
        assertEquals(0f, interactive.touchLight)
        assertEquals(1.45f, interactive.touchGlow, 0.0001f)
        assertEquals(1.2f, interactive.touchSaturation, 0.0001f)
        assertEquals(90.dp, interactive.touchLightSpread)
    }

    @Test
    fun aSurfaceDrawnByItsContainer_carriesItsFingersLightWhereTheContainerDrawsIt() {
        val light = TouchLight(Offset(10f, 20f), sigma = 95f, amount = 1f)

        assertEquals(Offset(110f, 70f), light.translate(Offset(100f, 50f)).at)
    }
}
