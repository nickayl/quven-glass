package tv.quven.glass

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class LiftedInsideTest {

    @Test
    fun aCardLiftedUnderTheStatusBar_ComesDownIntoView() {
        assertEquals(30f, liftedInside(Rect(0f, 20f, 100f, 200f), top = 50f, bottom = 1000f), 0f)
    }

    @Test
    fun aCardLiftedUnderTheNavigationBar_GoesUpIntoView() {
        assertEquals(-40f, liftedInside(Rect(0f, 800f, 100f, 1040f), top = 50f, bottom = 1000f), 0f)
    }

    @Test
    fun aCardInsideTheRoom_StaysWhereItIs() {
        assertEquals(0f, liftedInside(Rect(0f, 100f, 100f, 300f), top = 50f, bottom = 1000f), 0f)
    }

    @Test
    fun aCardTallerThanTheRoom_KeepsItsTopInView() {
        assertEquals(30f, liftedInside(Rect(0f, 20f, 100f, 1200f), top = 50f, bottom = 1000f), 0f)
    }
}
