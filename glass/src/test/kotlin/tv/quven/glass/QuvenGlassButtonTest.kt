package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class QuvenGlassButtonTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aLabelledButton_isAsTallAsApplesForEachSize() {
        render {
            Column {
                QuvenGlassButtonSize.entries.forEach { size ->
                    QuvenGlassButton(onClick = {}, label = "Play", size = size, modifier = Modifier.testTag("button-$size"))
                }
            }
        }

        assertEquals(28f, heightOf(QuvenGlassButtonSize.Small), 0.5f)
        assertEquals(34.5f, heightOf(QuvenGlassButtonSize.Regular), 0.5f)
        assertEquals(50.5f, heightOf(QuvenGlassButtonSize.Large), 0.5f)
    }

    @Test
    fun aGlyphAlone_isPaddedAsApplesIs() {
        render { QuvenGlassButton(onClick = {}, icon = ColorPainter(Color.White), contentDescription = "Like", modifier = Modifier.testTag(ButtonTag)) }

        val bounds = compose.onNodeWithTag(ButtonTag).getUnclippedBoundsInRoot()
        assertEquals(42f, (bounds.right - bounds.left).value, 0.5f)
        assertEquals(32f, (bounds.bottom - bounds.top).value, 0.5f)
    }

    @Test
    fun aTint_coversRegularGlassNearlyWhole_andClearGlassLessSo() {
        assertEquals(0.95f, QuvenGlassStyle.Standard.tinted(Color.Red).tint.alpha, 0.001f)
        assertEquals(0.8f, QuvenGlassStyle.Clear.tinted(Color.Red).tint.alpha, 0.001f)
        assertEquals(Color.Red.copy(alpha = 0.8f), QuvenGlassStyle.Clear.tinted(Color.Red).tint)
    }

    @Test
    fun clearGlass_leansTowardsNoShade_andLightensWhatItStandsOver() {
        val clear = QuvenGlassStyle.Clear

        assertEquals(0f, clear.thinTone.lean, 0f)
        assertEquals(0f, clear.thickTone.lean, 0f)
        assertEquals(0f, clear.lightTone.lean, 0f)
        assertEquals(0.086f, clear.brighten, 0.001f)
        assertEquals(QuvenGlassStyle.Standard.blur, clear.blur)
    }

    @Test
    fun aPress_runsTheAction_unlessTheButtonIsDisabled() {
        var presses = 0
        var enabled by mutableStateOf(true)
        render { QuvenGlassButton(onClick = { presses++ }, label = "Play", enabled = enabled, modifier = Modifier.testTag(ButtonTag)) }

        compose.onNodeWithTag(ButtonTag).performClick()
        compose.runOnIdle { enabled = false }
        compose.onNodeWithTag(ButtonTag).performClick()

        assertEquals(1, presses)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aButtonWithNeitherLabelNorGlyph_isRefused() {
        render { QuvenGlassButton(onClick = {}) }
    }

    private fun heightOf(size: QuvenGlassButtonSize): Float {
        val bounds = compose.onNodeWithTag("button-$size").getUnclippedBoundsInRoot()
        return (bounds.bottom - bounds.top).value
    }

    private fun render(content: @Composable () -> Unit) {
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { content() } }
        compose.waitForIdle()
    }

    private companion object {
        const val ButtonTag = "button"
    }
}
