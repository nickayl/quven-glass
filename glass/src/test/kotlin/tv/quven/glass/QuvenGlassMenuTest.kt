package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w800dp-h600dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
        assertEquals(62f, drawn("Collections").left.value, 0.5f)
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
    fun aFingerSlidingOntoAnotherRow_ticksOnce() {
        val haptics = CountingHaptics()
        render(QuvenGlassMenuMetrics.Phone, haptics = haptics)

        compose.onNodeWithTag("first").performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 0.3f))
            moveBy(Offset(0f, height * 0.4f))
            moveBy(Offset(0f, height * 0.2f))
            up()
        }

        assertEquals(1, haptics.ticks)
    }

    @Test
    fun aMenuHoldingAChoice_opensAColumnOfChecksBeforeEveryRow_andWidensByIt() {
        show(QuvenGlassMenuMetrics.Tablet) {
            QuvenGlassMenuItem("Download", {}, Modifier.testTag("item"), icon = ColorPainter(Color.White))
            QuvenGlassMenuChoice("Year", selected = true, onClick = {}, modifier = Modifier.testTag("choice"))
        }

        assertEquals(55f + 12f, drawn("Download").left.value, 0.5f)
        assertEquals(32f, drawn("Year").left.value, 0.5f)
        assertEquals(223f + 12f, tagged("item").right.value - tagged("item").left.value, 0.5f)
    }

    @Test
    fun aMenuWithoutAChoice_keepsItsRowsAtTheirPlaces() {
        show(QuvenGlassMenuMetrics.Tablet) {
            QuvenGlassMenuItem("Download", {}, Modifier.testTag("item"), icon = ColorPainter(Color.White))
        }

        assertEquals(55f, drawn("Download").left.value, 0.5f)
        assertEquals(223f, tagged("item").right.value - tagged("item").left.value, 0.5f)
    }

    @Test
    fun aMenuOfPlainRows_startsTheirNamesAtThePlainInset() {
        show(QuvenGlassMenuMetrics.Tablet) {
            QuvenGlassMenuItem("Off", {})
            QuvenGlassMenuItem("End of chapter", {})
        }

        assertEquals(21.5f, drawn("Off").left.value, 0.5f)
    }

    @Test
    fun aLongName_widensTheMenu_upToItsGreatestWidth() {
        show(QuvenGlassMenuMetrics.Tablet) {
            QuvenGlassMenuItem("W".repeat(80), {}, Modifier.testTag("item"))
        }

        assertEquals(320f, tagged("item").right.value - tagged("item").left.value, 0.5f)
    }

    @Test
    fun aChoice_isMarkedSelectedOnlyWhenChosen() {
        show(QuvenGlassMenuMetrics.Tablet) {
            QuvenGlassMenuChoice("Year", selected = true, onClick = {}, modifier = Modifier.testTag("chosen"))
            QuvenGlassMenuChoice("Title", selected = false, onClick = {}, modifier = Modifier.testTag("other"))
        }

        compose.onNodeWithTag("chosen").assertIsSelected()
        compose.onNodeWithTag("other").assertIsNotSelected()
    }

    @Test
    fun aDisabledRow_isChosenNeitherByAPressNorByASlide() {
        var chosen = 0
        show(QuvenGlassMenuMetrics.Phone) {
            QuvenGlassMenuItem("Play", {}, Modifier.testTag("first"))
            QuvenGlassMenuItem("Unavailable", { chosen++ }, Modifier.testTag("disabled"), enabled = false)
        }

        compose.onNodeWithTag("disabled").performClick()
        compose.onNodeWithTag("first").performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 0.5f))
            moveBy(Offset(0f, height * 0.6f))
            up()
        }

        assertEquals(0, chosen)
        compose.onNodeWithTag("disabled").assertIsNotEnabled()
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

    private fun render(
        metrics: QuvenGlassMenuMetrics,
        onFirst: () -> Unit = {},
        onSecond: () -> Unit = {},
        haptics: HapticFeedback = CountingHaptics(),
    ) = show(metrics, haptics) {
        QuvenGlassMenuTitle("Library")
        QuvenGlassMenuItem("Collections", onFirst, Modifier.testTag("first"), icon = ColorPainter(Color.White))
        QuvenGlassMenuItem("Saved", onSecond, Modifier.testTag("second"))
        QuvenGlassMenuDivider(Modifier.testTag("divider"))
        QuvenGlassMenuTitle("Account")
        QuvenGlassMenuItem("Sign out", {}, destructive = true)
    }

    private fun show(
        metrics: QuvenGlassMenuMetrics,
        haptics: HapticFeedback = CountingHaptics(),
        content: @Composable ColumnScope.() -> Unit,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalHapticFeedback provides haptics) {
                QuvenGlassMenu(metrics = metrics, content = content)
            }
        }
    }

    private fun tagged(tag: String): DpRect = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun drawn(text: String): DpRect = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun middleOf(bounds: DpRect): Float = (bounds.top.value + bounds.bottom.value) / 2f

    private fun heightOf(bounds: DpRect): Float = bounds.bottom.value - bounds.top.value

    private class CountingHaptics : HapticFeedback {
        var ticks = 0

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            if (hapticFeedbackType == HapticFeedbackType.SegmentFrequentTick) ticks++
        }
    }
}
