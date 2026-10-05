package tv.quven.glass

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class QuvenGlassTouchLightTest {

    @Test
    fun interactiveGlass_gathersAFaintWhiteLightUnderTheFinger_andPlainGlassNone() {
        assertEquals(0f, QuvenGlassStyle.Standard.touchLight)
        assertEquals(0.057f, QuvenGlassStyle.Standard.interactive().touchLight, 0.0001f)
    }

    @Test
    fun aSurfaceDrawnByItsContainer_carriesItsFingersLightWhereTheContainerDrawsIt() {
        val light = TouchLight(Offset(10f, 20f), sigma = 95f, amount = 0.057f)

        assertEquals(Offset(110f, 70f), light.translate(Offset(100f, 50f)).at)
    }
}
