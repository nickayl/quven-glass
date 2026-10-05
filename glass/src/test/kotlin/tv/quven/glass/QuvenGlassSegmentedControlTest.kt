package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class QuvenGlassSegmentedControlTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pressingAnOption_choosesIt_inAControlAsTallAsApples_holdingItsOptionsWithinThePillsInset() {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                QuvenGlassSegmentedControl(
                    Options,
                    selected,
                    onSelect = { selected = Options.indexOf(it) },
                    modifier = Modifier.testTag(Control),
                    optionWidth = 100.dp,
                ) { option, _ ->
                    BasicText(option)
                }
            }
        }

        compose.onNodeWithText("Year").performClick()

        compose.runOnIdle { assertEquals(1, selected) }
        compose.onNodeWithText("Year").assertIsSelected()
        val bounds = compose.onNodeWithTag(Control).getUnclippedBoundsInRoot()
        assertEquals(304f, (bounds.right - bounds.left).value, 0.5f)
        assertEquals(32f, (bounds.bottom - bounds.top).value, 0.5f)
    }

    @Test
    fun thePill_liftsIntoALens18WiderAnd12Taller_showingTheOptionsAtTheirOwnSize() {
        val thumb = segmentThumb(DpSize(98.dp, 28.dp))

        assertEquals(DpSize(98.dp, 28.dp), thumb.thumb)
        assertEquals(DpSize(116.dp, 40.dp), thumb.lens)
        assertEquals(1f, thumb.glass.zoom, 0f)
    }

    private companion object {
        val Options = listOf("Title", "Year", "Added")
        const val Control = "control"
    }
}
