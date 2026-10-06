package tv.quven.glass

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
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
class MenuPaletteTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aPalette_drawsEachActionsGlyphOverItsName_andRunsTheOneChosen_closingTheMenu() {
        val chosen = mutableListOf<String>()
        var closed = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                QuvenGlassMenu(metrics = QuvenGlassMenuMetrics.Tablet, onChosen = { closed++ }) {
                    MenuPalette(
                        listOf(
                            PaletteAction("Cut", rememberVectorPainter(EditGlyphs.Cut)) { chosen += "Cut" },
                            PaletteAction("Copy", rememberVectorPainter(EditGlyphs.Copy)) { chosen += "Copy" },
                        ),
                    )
                }
            }
        }

        val glyph = compose.onAllNodesWithTag(PaletteGlyphTag, useUnmergedTree = true)[0].getUnclippedBoundsInRoot()
        // The menu's inset of 6 above the palette, then the glyph centred 18 down.
        assertEquals(6f + 18f - 11f, glyph.top.value, 0.5f)
        assertEquals(22f, glyph.height.value, 0.5f)
        assertEquals(22f, glyph.width.value, 0.5f)
        compose.onNodeWithText("Copy").performClick()

        compose.runOnIdle {
            assertEquals(listOf("Copy"), chosen)
            assertEquals(1, closed)
        }
    }
}
