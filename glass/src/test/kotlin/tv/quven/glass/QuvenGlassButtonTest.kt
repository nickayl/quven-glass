package tv.quven.glass

import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        render { QuvenGlassIconButton(onClick = {}, icon = ColorPainter(Color.White), contentDescription = "Like", modifier = Modifier.testTag(ButtonTag)) }

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
    fun clearGlass_leansTowardsNoShade_lightensWhatItStandsOver_andShowsItLargerAndSharper() {
        val clear = QuvenGlassStyle.Clear

        assertEquals(0f, clear.thinTone.lean, 0f)
        assertEquals(0f, clear.thickTone.lean, 0f)
        assertEquals(0f, clear.lightTone.lean, 0f)
        assertEquals(0.086f, clear.brighten, 0.001f)
        assertTrue(clear.blur < QuvenGlassStyle.Standard.blur)
        assertEquals(0.8f, clear.zoom, 0f)
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

    @Test
    fun aButtonOfItsOwnSize_centresItsContent_inTheInkOfItsGlass() {
        var given = Color.Unspecified
        render {
            QuvenGlassButton(onClick = {}, modifier = Modifier.size(56.dp).testTag(ButtonTag), ink = Color.Yellow) { ink ->
                given = ink
                Box(Modifier.size(20.dp).testTag(FaceTag))
            }
        }

        val button = compose.onNodeWithTag(ButtonTag).getUnclippedBoundsInRoot()
        val face = compose.onNodeWithTag(FaceTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(18f, (face.left - button.left).value, 0.5f)
        assertEquals(18f, (face.top - button.top).value, 0.5f)
        assertEquals(Color.Yellow, given)
    }

    @Test
    fun aPress_growsTheButtonsLongerSideBySixteen_andItsShorterInProportion() {
        assertEquals(1f + 16f / 56f, pressScale(lift = 1f, expansion = 16f, longerSide = 56f), 0.0001f)
        assertEquals(1f, pressScale(lift = 0f, expansion = 16f, longerSide = 56f), 0f)
        assertEquals(1f, pressScale(lift = 1f, expansion = 16f, longerSide = 0f), 0f)
    }

    @Test
    fun aProminentButton_keepsItsInk_whateverTheGlassTurnsTo() {
        var given = Color.Unspecified
        render { QuvenGlassButton(onClick = {}, tint = Color.Blue, ink = Color.Green, lightInk = Color.Red) { ink -> given = ink } }

        assertEquals(Color.Green, given)
    }

    @Test
    fun theButtonAndMenuMaterials_answerTheFingerAsApplesDo_andAMenuFrostsDeeperWithALitRim() {
        val buttons = QuvenGlassStyle.Standard.forButtons()
        val prominent = QuvenGlassStyle.Standard.forButtons(Color.Blue)
        val menus = QuvenGlassStyle.Standard.forMenus()

        assertEquals(0f, buttons.pressGlow, 0f)
        assertEquals(1.05f, buttons.pressLighten, 0f)
        assertEquals(16.dp, buttons.pressExpansion)
        assertEquals(1f, buttons.rimGlow, 0f)
        assertEquals(0f, buttons.rimLight, 0f)
        assertEquals(Color.Blue, prominent.tint)
        assertEquals(1.65f, prominent.pressTintGlow, 0f)
        assertEquals(0f, prominent.pressGlow, 0f)
        assertEquals(0.3f, prominent.rimLight, 0f)
        assertEquals(3.6f, menus.pressGlow, 0f)
        assertEquals(1.7f, menus.rimGlow, 0f)
        assertEquals(9.5.dp, menus.blur)
        assertEquals(menus, menus.forMenus())
    }

    private fun heightOf(size: QuvenGlassButtonSize): Float {
        val bounds = compose.onNodeWithTag("button-$size").getUnclippedBoundsInRoot()
        return (bounds.bottom - bounds.top).value
    }

    private fun render(content: @Composable () -> Unit) {
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { content() } }
        compose.waitForIdle()
    }

    @Test
    fun aPress_risesOnOneAnimation_andFallsOnAnother_soTheLightCanOutlastTheGrowth() {
        val press = GlassPress()
        val interactions = MutableInteractionSource()
        compose.setContent { LaunchedEffect(Unit) { press.follow(this, interactions) { held -> tween(if (held) RiseMillis else FallMillis) } } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false

        val down = PressInteraction.Press(Offset.Zero)
        compose.runOnIdle { interactions.tryEmit(down) }
        compose.mainClock.advanceTimeBy(RiseMillis * 2L)
        assertEquals(1f, press.value, 0f)

        compose.runOnIdle { interactions.tryEmit(PressInteraction.Release(down)) }
        compose.mainClock.advanceTimeBy(FallMillis / 4L)
        assertTrue(press.value > 0.5f)
    }

    private companion object {
        const val ButtonTag = "button"
        const val FaceTag = "face"
        const val RiseMillis = 50
        const val FallMillis = 800
    }
}
