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

    @Test
    fun aSidebar_blursDeeplyAtEverySize_withItsOwnToneAndALitRim() {
        val sidebar = QuvenGlassStyle.Standard.forSidebars()

        assertEquals(sidebar.blurFor(40f, density), sidebar.blurFor(900f, density), 0.001f)
        assertEquals(500f, sidebar.blurFor(640f, density), 0.001f)
        assertEquals(QuvenGlassTone.Sidebar, sidebar.largeTone)
        assertEquals(0.2f, sidebar.rimLight, 0f)
    }

    @Test
    fun theShade_followsTheBrightnessOnThinAndLargeGlass_andHoldsOnABar() {
        val standard = QuvenGlassStyle.Standard

        assertEquals(QuvenGlassTone.Thin.adaptation, standard.adaptationFor(44f, density), 0.001f)
        assertEquals(0f, standard.adaptationFor(160f, density), 0.001f)
        assertEquals(QuvenGlassTone.Large.adaptation, standard.adaptationFor(260f, density), 0.001f)
        assertEquals(QuvenGlassTone.Large.adaptation / 2f, standard.adaptationFor(224f, density), 0.001f)
    }
}
