package tv.quven.glass

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w400dp-h300dp")
class QuvenGlassToolbarGroupTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aGroup_isACircleForOneButton_andGrowsFiftyNineForEachMore() {
        assertEquals(44.dp, groupWidth(1))
        assertEquals(103.dp, groupWidth(2))
    }

    @Test
    fun toolbarItems_growAsGlassButtons_andLightAroundTheFinger_evenOverBlack() {
        val material = QuvenGlassStyle.Standard.forToolbarItems()

        assertEquals(16.dp, material.pressExpansion)
        assertEquals(0f, material.pressGlow)
        assertEquals(0f, material.pressLighten)
        assertEquals(0.5f, material.touchLight, 0.001f)
        assertEquals(0.11f, material.pressWhite, 0.001f)
        assertEquals(45.dp, material.touchLightSpread)
    }

    @Test
    fun eachButton_answersItsOwnPress_andSharesTheCapsuleEvenly() {
        var pressed = ""
        val glyph = ColorPainter(Color.White)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                QuvenGlassToolbarGroup(
                    listOf(QuvenGlassToolbarItem(glyph, "Share") { pressed = "Share" }, QuvenGlassToolbarItem(glyph, "Favorite") { pressed = "Favorite" }),
                    backdrop = null,
                )
            }
        }

        compose.onNodeWithContentDescription("Favorite").performClick()

        compose.runOnIdle { assertEquals("Favorite", pressed) }
        val share = compose.onNodeWithContentDescription("Share").getUnclippedBoundsInRoot()
        assertEquals(51.5f, (share.right - share.left).value, 0.5f)
    }
}
