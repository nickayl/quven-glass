package tv.quven.glass

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SheetGeometryTest {

    private val density = Density(1f)

    @Test
    fun onAnIPadSizedWindow_theSheetFloatsAsTheSystemsDoes_atHalfAndAtFullHeight() {
        val geometry = SheetGeometry(820f, 24f, floatingSheet(1180f, 820f, density))

        assertEquals(370f, geometry.topOf(QuvenGlassSheetDetent.Medium), 0.01f)
        assertEquals(84.5f, geometry.topOf(QuvenGlassSheetDetent.Large), 0.01f)
        assertEquals(Rect(300f, 370f, 880f, 727.5f), geometry.boundsAt(370f, 1180f, 9f, 574f))
        assertEquals(Rect(300f, 84.5f, 880f, 727.5f), geometry.boundsAt(84.5f, 1180f, 9f, 574f))
    }

    @Test
    fun aFloatingSheet_slidesDownWhole_onItsWayOut() {
        val geometry = SheetGeometry(820f, 24f, floatingSheet(1180f, 820f, density))

        assertEquals(357.5f, geometry.boundsAt(700f, 1180f, 9f, 574f).height, 0.01f)
    }

    @Test
    fun onAPhone_theSheetRisesFromTheBottomEdge_asBefore() {
        assertNull(floatingSheet(400f, 800f, density))
        assertNotNull(floatingSheet(1138f, 711f, density))
        val geometry = SheetGeometry(800f, 24f)

        assertEquals(Rect(9f, 378.4f, 391f, 791f), geometry.boundsAt(378.4f, 400f, 9f, 574f))
    }
}
