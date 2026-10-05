package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w800dp-h600dp")
class QuvenGlassSidebarTest {

    @get:Rule
    val compose = createComposeRule()

    private var shown by mutableStateOf(false)

    @Test
    fun aHiddenSidebar_drawsNothing() {
        render()

        compose.onNodeWithText(Title).assertDoesNotExist()
    }

    @Test
    fun aSidebarShown_standsInsetFromTheStart_asWideAsApplesIs() {
        render()

        compose.runOnIdle { shown = true }
        compose.waitForIdle()

        val bounds = compose.onNodeWithText(Title).getUnclippedBoundsInRoot()
        assertEquals(7f, bounds.left.value, 0.5f)
        assertEquals(7f, bounds.top.value, 0.5f)
        assertEquals(221f, (bounds.right - bounds.left).value, 0.5f)
    }

    @Test
    fun aSidebarComingIn_startsBeyondTheStartEdge_andEndsInPlace() {
        assertEquals(228, sidebarAway(0f, 228))
        assertEquals(114, sidebarAway(0.5f, 228))
        assertEquals(0, sidebarAway(1f, 228))
    }

    @Test
    fun aSidebarHidden_slidesAway_andLeavesNothing() {
        shown = true
        render()

        compose.runOnIdle { shown = false }
        compose.waitForIdle()

        compose.onNodeWithText(Title).assertDoesNotExist()
    }

    private fun render() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.fillMaxSize()) {
                    QuvenGlassSidebar(shown, backdrop = null) { BasicText(Title, Modifier.fillMaxSize()) }
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val Title = "Library"
    }
}
