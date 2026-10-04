package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class QuvenGlassMenuTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aTabletMenu_standsItsTitlesRowsAndDividerWhereIPadOSStandsThem() {
        render(QuvenGlassMenuMetrics.Tablet)

        assertEquals(20.5f, middleOf(drawn("Library")), 1f)
        assertEquals(34f, tagged("first").top.value, 0.5f)
        assertEquals(72f, tagged("second").top.value, 0.5f)
        assertEquals(110f, tagged("divider").top.value, 0.5f)
        assertEquals(17f, heightOf(tagged("divider")), 0.5f)
        assertEquals(141.5f, middleOf(drawn("Account")), 1f)
    }

    @Test
    fun aPhoneMenu_standsItsRowsAndNamesWhereIOSStandsThem() {
        render(QuvenGlassMenuMetrics.Phone)

        assertEquals(18f, middleOf(drawn("Library")), 1f)
        assertEquals(37f, tagged("first").top.value, 0.5f)
        assertEquals(42f, heightOf(tagged("first")), 0.5f)
        assertEquals(62f, drawn("Playlists").left.value, 0.5f)
    }

    @Test
    fun pressingARow_runsItsAction() {
        var pressed = 0
        render(QuvenGlassMenuMetrics.Phone, onFirst = { pressed++ })

        compose.onNodeWithTag("first").performClick()

        assertEquals(1, pressed)
    }

    @Test
    fun aFingerSlidingAlongTheMenu_choosesTheRowItLiftsOver_andNotTheOneItPressed() {
        var first = 0
        var second = 0
        render(QuvenGlassMenuMetrics.Phone, onFirst = { first++ }, onSecond = { second++ })

        compose.onNodeWithTag("first").performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 0.5f))
            moveBy(Offset(0f, height * 0.6f))
            up()
        }

        assertEquals(0, first)
        assertEquals(1, second)
    }

    @Test
    fun aFingerLiftingOverNoRow_choosesNothing() {
        var chosen = 0
        render(QuvenGlassMenuMetrics.Phone, onFirst = { chosen++ }, onSecond = { chosen++ })

        compose.onNodeWithTag("first").performTouchInput {
            down(center)
            moveBy(Offset(0f, -height * 0.6f))
            moveBy(Offset(0f, -height * 0.6f))
            up()
        }

        assertEquals(0, chosen)
    }

    private fun render(metrics: QuvenGlassMenuMetrics, onFirst: () -> Unit = {}, onSecond: () -> Unit = {}) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                QuvenGlassMenu(Modifier.width(metrics.width), metrics = metrics) {
                    QuvenGlassMenuTitle("Library")
                    QuvenGlassMenuItem("Playlists", onFirst, Modifier.testTag("first"), icon = ColorPainter(Color.White))
                    QuvenGlassMenuItem("Watchlist", onSecond, Modifier.testTag("second"))
                    QuvenGlassMenuDivider(Modifier.testTag("divider"))
                    QuvenGlassMenuTitle("Account")
                    QuvenGlassMenuItem("Sign out", {}, destructive = true)
                }
            }
        }
    }

    private fun tagged(tag: String): DpRect = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun drawn(text: String): DpRect = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun middleOf(bounds: DpRect): Float = (bounds.top.value + bounds.bottom.value) / 2f

    private fun heightOf(bounds: DpRect): Float = bounds.bottom.value - bounds.top.value
}
