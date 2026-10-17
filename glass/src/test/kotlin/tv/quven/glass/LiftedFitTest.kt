package tv.quven.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class LiftedFitTest {

    @Test
    fun aCardThatLeavesItsMenuRoom_KeepsItsWholeSize() {
        assertEquals(1f, liftedFit(height = 400f, room = 1000f, gap = 20f, minimumMenu = 300f), 0f)
    }

    @Test
    fun aCardTooTallForItsMenu_ShrinksUntilTheMenuHasItsLeastRoom() {
        val fit = liftedFit(height = 800f, room = 1000f, gap = 20f, minimumMenu = 300f)

        assertEquals(0.85f, fit, 0.0001f)
        assertEquals(1000f, 800f * fit + 20f + 300f, 0.01f)
    }

    @Test
    fun aRoomTooShortForAnyCard_StopsShrinkingAtItsFloor() {
        assertEquals(0.35f, liftedFit(height = 800f, room = 200f, gap = 20f, minimumMenu = 300f), 0f)
    }
}
