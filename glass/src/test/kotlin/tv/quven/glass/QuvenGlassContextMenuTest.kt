package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w800dp-h600dp")
class QuvenGlassContextMenuTest {

    @get:Rule
    val compose = createComposeRule()

    private var presses = 0
    private var chosen = 0

    @Test
    fun aPress_runsTheCardsAction_andOpensNoMenu() {
        render(top = 300f)

        compose.onNodeWithTag(CardTag).performClick()
        compose.waitForIdle()

        assertEquals(1, presses)
        compose.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test
    fun aLongPress_opensTheMenuBesideTheCard_onTheSideWithMoreRoom() {
        render(top = 340f)

        compose.onNodeWithTag(CardTag).performTouchInput { longClick() }
        compose.waitForIdle()

        val card = compose.onNodeWithTag(CardTag).getUnclippedBoundsInRoot()
        val row = compose.onNodeWithText("Play", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(row.bottom < card.top)
    }

    @Test
    fun aCardHighOnTheScreen_opensItsMenuBelowIt() {
        render(top = 40f)

        compose.onNodeWithTag(CardTag).performTouchInput { longClick() }
        compose.waitForIdle()

        val card = compose.onNodeWithTag(CardTag).getUnclippedBoundsInRoot()
        val row = compose.onNodeWithText("Play", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(row.top > card.bottom)
    }

    @Test
    fun choosingARow_runsIt_andClosesTheMenu() {
        render(top = 300f)
        compose.onNodeWithTag(CardTag).performTouchInput { longClick() }
        compose.waitForIdle()

        compose.onNodeWithText("Play").performClick()
        compose.waitForIdle()

        assertEquals(1, chosen)
        assertEquals(0, presses)
        compose.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test
    fun theMenu_standsBesideTheLiftedCard_alignedWithItsSideNearerTheEdge() {
        val place = QuvenGlassMorphPlacement.aboveOrBelow(gap = 22.dp, edge = 16.dp)
        val space = IntSize(1180, 820)
        val menu = IntSize(223, 166)

        // Lower on the screen and right of its middle: above, its end with the card's.
        assertEquals(IntOffset(732 - 223, 311 - 22 - 166), place.place(menu, IntRect(545, 311, 732, 592), space, Density(1f)))
        // Higher and left of its middle: below, its start with the card's.
        assertEquals(IntOffset(100, 300 + 22), place.place(menu, IntRect(100, 40, 287, 300), space, Density(1f)))
    }

    private fun render(top: Float) {
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    QuvenGlassContextMenuBox(
                        menu = { QuvenGlassMenuItem("Play", { chosen++ }) },
                        onClick = { presses++ },
                        modifier = Modifier.offset(300.dp, top.dp).size(170.dp, 255.dp).testTag(CardTag),
                    ) {}
                    QuvenGlassMenuHost(host, backdrop = null, metrics = QuvenGlassMenuMetrics.Tablet)
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val CardTag = "card"
    }
}
