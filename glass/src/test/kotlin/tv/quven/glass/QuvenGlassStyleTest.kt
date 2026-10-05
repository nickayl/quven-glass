package tv.quven.glass

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class QuvenGlassStyleTest {

    private val density = Density(2f)
    private val style = QuvenGlassStyle(blur = 4.dp, thickBlur = 8.dp, thinSize = 60.dp, thickBlurSize = 140.dp)

    @Test
    fun theBlur_holdsUpToThinGlass_growsInProportion_andHoldsFromTheThickBlursSize() {
        assertEquals(8f, style.blurFor(80f, density), 0.001f)
        assertEquals(8f, style.blurFor(120f, density), 0.001f)
        assertEquals(12f, style.blurFor(200f, density), 0.001f)
        assertEquals(16f, style.blurFor(280f, density), 0.001f)
        assertEquals(16f, style.blurFor(900f, density), 0.001f)
    }

    @Test
    fun aMaterialOfOneBlur_blursAlikeAtEverySize_asAMenuDoes() {
        val menu = QuvenGlassStyle.Standard.forMenus()

        assertEquals(menu.blurFor(40f, density), menu.blurFor(900f, density), 0.001f)
        assertEquals(6f, style.withBlur(3.dp).blurFor(900f, density), 0.001f)
    }
}
