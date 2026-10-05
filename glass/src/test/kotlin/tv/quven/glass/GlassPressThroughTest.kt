package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// The legacy graphics build the glass program, so these surfaces are drawn by the glass node and take its pointer.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w400dp-h400dp")
class GlassPressThroughTest {

    @get:Rule
    val compose = createComposeRule()

    private var rowPresses = 0
    private var scrimPresses = 0

    @Test
    fun aRowOnPlainGlass_takesItsPress_andTheScrimUnderTheGlassDoesNot() {
        render(QuvenGlassStyle.Standard)

        compose.onNodeWithTag(RowTag).performClick()

        compose.runOnIdle {
            assertEquals(1, rowPresses)
            assertEquals(0, scrimPresses)
        }
    }

    @Test
    fun aRowOnInteractiveGlass_takesItsPress_andTheScrimUnderTheGlassDoesNot() {
        render(QuvenGlassStyle.Standard.interactive())

        compose.onNodeWithTag(RowTag).performClick()

        compose.runOnIdle {
            assertEquals(1, rowPresses)
            assertEquals(0, scrimPresses)
        }
    }

    @Test
    fun aPressOnInteractiveGlassBesideItsRow_staysOnTheGlass() {
        render(QuvenGlassStyle.Standard.interactive())

        compose.onRoot().performTouchInput { click(Offset(150f, 150f)) }

        compose.runOnIdle { assertEquals(0, scrimPresses) }
    }

    private fun render(style: QuvenGlassStyle) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val backdrop = rememberQuvenGlassBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.Red).quvenGlassSource(backdrop))
                    Box(Modifier.fillMaxSize().clickable { scrimPresses++ })
                    Box(Modifier.size(200.dp).quvenLiquidGlass(backdrop, style, RoundedCornerShape(20.dp))) {
                        Box(Modifier.size(100.dp).testTag(RowTag).clickable { rowPresses++ })
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val RowTag = "row"
    }
}
