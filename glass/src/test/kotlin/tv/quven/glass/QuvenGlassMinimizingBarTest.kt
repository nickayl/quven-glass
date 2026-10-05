package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w500dp-h300dp")
class QuvenGlassMinimizingBarTest {

    @get:Rule
    val compose = createComposeRule()

    private var minimized by mutableStateOf(false)

    @Test
    fun aBarInAWiderRoom_restsCentred_andMinimizesToTheRoomsStart_whileOneAsWideAsTheRoomKeepsItsPlace() {
        assertEquals(200f, minimizedBarStart(0f, 800f, 400f), 0.01f)
        assertEquals(100f, minimizedBarStart(0.5f, 800f, 400f), 0.01f)
        assertEquals(0f, minimizedBarStart(1f, 800f, 400f), 0.01f)
        assertEquals(0f, minimizedBarStart(1.05f, 800f, 400f), 0.01f)
        assertEquals(0f, minimizedBarStart(0f, 400f, 400f), 0.01f)
    }

    @Test
    fun contentScrollingDownPastTheThreshold_minimizesTheBar_andLessDoesNot() {
        val minimizer = QuvenGlassBarMinimizer(threshold = 20f)

        scroll(minimizer, consumed = -15f)
        assertFalse(minimizer.minimized)
        scroll(minimizer, consumed = -6f)
        assertTrue(minimizer.minimized)
    }

    @Test
    fun scrollingBackUpAwayFromTheTop_keepsTheBarMinimized_andTheTopGrowsItBack() {
        val minimizer = QuvenGlassBarMinimizer(threshold = 20f)
        scroll(minimizer, consumed = -40f)

        scroll(minimizer, consumed = 30f)
        assertTrue(minimizer.minimized)
        scroll(minimizer, consumed = 10f, available = 5f)
        assertFalse(minimizer.minimized)
    }

    @Test
    fun scrollingBackUpByTheDistanceScrolledDown_growsTheBarBackWithoutAPull() {
        val minimizer = QuvenGlassBarMinimizer(threshold = 20f)
        scroll(minimizer, consumed = -30f)
        scroll(minimizer, consumed = -30f)

        scroll(minimizer, consumed = 59f)
        assertTrue(minimizer.minimized)
        scroll(minimizer, consumed = 1f)
        assertFalse(minimizer.minimized)
    }

    @Test
    fun aResetForgetsTheDistanceScrolled_soTheNextScrollBackIsNotTheTop() {
        val minimizer = QuvenGlassBarMinimizer(threshold = 20f)
        scroll(minimizer, consumed = -100f)
        minimizer.reset()
        scroll(minimizer, consumed = -30f)

        scroll(minimizer, consumed = 29f)
        assertTrue(minimizer.minimized)
        scroll(minimizer, consumed = 1f)
        assertFalse(minimizer.minimized)
    }

    @Test
    fun aPressThatGrowsTheBarBack_keepsTheDistanceScrolled() {
        val minimizer = QuvenGlassBarMinimizer(threshold = 20f)
        scroll(minimizer, consumed = -100f)
        minimizer.expand()
        scroll(minimizer, consumed = -30f)

        scroll(minimizer, consumed = 100f)
        assertTrue(minimizer.minimized)
    }

    @Test
    fun aFlingThatReachesTheTop_growsTheBarBack() {
        val minimizer = QuvenGlassBarMinimizer(threshold = 20f)
        scroll(minimizer, consumed = -40f)

        runBlocking { minimizer.nestedScrollConnection.onPostFling(Velocity(0f, 900f), Velocity(0f, 300f)) }

        assertFalse(minimizer.minimized)
    }

    @Test
    fun minimized_theTabsSinkIntoACircleAtTheirStart_theirFacesShrunkAndGone() {
        val fold = minimizingBarFold(1f, minimizing = true, tabsWidth = 354f, height = 62f, inset = 7.25f)

        assertEquals(Rect(7.25f, 7.25f, 54.75f, 54.75f), fold.tabs)
        assertEquals(47.5f / 354f, fold.facesScale, 0.001f)
        assertEquals(0f, fold.facesAlpha)
    }

    @Test
    fun aPressOnTheMinimizedBar_asksToGrowItBack() {
        minimized = true
        render()

        compose.onNodeWithTag(HeldTag, useUnmergedTree = true).performClick()

        compose.runOnIdle { assertFalse(minimized) }
    }

    @Test
    fun atRest_theBarDrawsTheCallersTabs_atTheirWidth() {
        render()

        val tabs = compose.onNodeWithTag(TabsTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(TabsWidth, (tabs.right - tabs.left).value, 0.5f)
        compose.onNodeWithTag(HeldTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theAccessory_standsAboveTheRestingBar_andBesideTheMinimizedCircle_asTallAsTheCircle() {
        val above = accessoryRect(0f, TabsWidth, 62f, 7.25f, Gaps)
        val beside = accessoryRect(1f, TabsWidth, 62f, 7.25f, Gaps)

        assertEquals(Rect(0f, 0f, TabsWidth, 47.5f), above)
        assertEquals(Rect(7.25f + 47.5f + 8f, 47.5f + 9.5f + 7.25f, TabsWidth - 7.25f, 47.5f + 9.5f + 7.25f + 47.5f), beside)
    }

    @Test
    fun minimizing_theAccessoryNarrowsBeforeItFalls() {
        val halfway = accessoryRect(0.5f, TabsWidth, 62f, 7.25f, Gaps)

        assertTrue(halfway.left > 0.5f * (7.25f + 47.5f + 8f))
        assertEquals(0f, halfway.top)
    }

    @Test
    fun withAnAccessory_theBarStandsUnderIt_andTheAccessoryIsDrawn() {
        render(accessory = true)

        val tabs = compose.onNodeWithTag(TabsTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val strip = compose.onNodeWithTag(AccessoryTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(47.5f + 9.5f, tabs.top.value, 0.5f)
        assertEquals(0f, strip.top.value, 0.5f)
    }

    private fun scroll(minimizer: QuvenGlassBarMinimizer, consumed: Float, available: Float = 0f) {
        minimizer.nestedScrollConnection.onPostScroll(Offset(0f, consumed), Offset(0f, available), NestedScrollSource.UserInput)
    }

    private fun render(accessory: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.fillMaxSize()) {
                    QuvenGlassMinimizingBar(
                        minimized = minimized,
                        onExpand = { minimized = false },
                        tabsWidth = TabsWidth.dp,
                        height = 62.dp,
                        heldCentre = DpOffset(46.dp, 23.dp),
                        backdrop = null,
                        tabs = { Box(Modifier.fillMaxSize().testTag(TabsTag)) },
                        tabsFace = { BasicText("Tabs") },
                        heldGlyph = { Box(Modifier.size(20.dp).testTag(HeldTag)) },
                        accessory = if (accessory) {
                            { Box(Modifier.fillMaxSize().testTag(AccessoryTag)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val TabsTag = "tabs"
        const val HeldTag = "held"
        const val AccessoryTag = "accessory"
        const val TabsWidth = 354f
        val Gaps = AccessoryGaps(above = 9.5f, inline = 8f)
    }
}
