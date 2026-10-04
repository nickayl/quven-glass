package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w800dp-h600dp")
class QuvenGlassMenuHostTest {

    @get:Rule
    val compose = createComposeRule()

    private var expanded by mutableStateOf(false)
    private var dismissals = 0
    private var chosen = 0
    private val morph = QuvenGlassMorphState()

    @Test
    fun anOpenDropdown_drawsItsMenuInTheHost_overItsControl() {
        render()

        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        val row = compose.onNodeWithTag(RowTag).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals(746f, row.right.value, 1f)
        assertTrue(morph.isShown)
    }

    @Test
    fun aPressOutsideTheMenu_asksItClosed_andChoosesNothing() {
        render()
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        compose.onNodeWithTag(OutsideTag).performClick()

        assertEquals(1, dismissals)
        assertEquals(0, chosen)
    }

    @Test
    fun aClosedDropdown_leavesTheHostEmpty_onceItsGlassHasGone() {
        render()
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        compose.runOnIdle { expanded = false }
        compose.waitForIdle()

        compose.onNodeWithText("Play").assertDoesNotExist()
        assertFalse(morph.isShown)
    }

    @Test
    fun choosingARow_runsItsAction_andClosesTheMenu() {
        render()
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        compose.onNodeWithTag(RowTag).performClick()

        assertEquals(1, chosen)
        assertEquals(1, dismissals)
        compose.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test
    fun aMenuBox_opensFromItsControl_andClosesOnAChoice() {
        var order = "Year"
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    QuvenGlassMenuBox(
                        menu = { QuvenGlassMenuChoices(listOf("Title", "Year"), order, { it }) { order = it } },
                    ) {
                        Box(Modifier.offset(40.dp, 40.dp).size(46.dp).menuAnchor().testTag(ControlTag).clickable { openMenu() })
                    }
                    QuvenGlassMenuHost(host, backdrop = null, metrics = QuvenGlassMenuMetrics.Tablet)
                }
            }
        }

        compose.onNodeWithTag(ControlTag).performClick()
        compose.onNodeWithText("Year", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Title").performClick()
        compose.waitForIdle()

        assertEquals("Title", order)
        compose.onNodeWithText("Year").assertDoesNotExist()
    }

    @Test
    fun aMenuOpenedFromTheKeys_focusesItsFirstRow_keepsTheFocus_andHandsItBackToItsControl() {
        var chosenRow = ""
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    // A control below the menu, first in order, where a focus left loose or let out of the menu lands.
                    Box(Modifier.offset(40.dp, 400.dp).size(46.dp).testTag(DecoyTag).clickable {})
                    QuvenGlassMenuBox(
                        menu = {
                            QuvenGlassMenuItem("First", { chosenRow = "First" }, Modifier.testTag(FirstTag))
                            QuvenGlassMenuItem("Unavailable", {}, enabled = false)
                            QuvenGlassMenuItem("Second", { chosenRow = "Second" }, Modifier.testTag(SecondTag))
                        },
                    ) {
                        Box(Modifier.offset(40.dp, 40.dp).size(46.dp).menuAnchor().testTag(ControlTag).clickable { openMenu() })
                    }
                    QuvenGlassMenuHost(host, backdrop = null, metrics = QuvenGlassMenuMetrics.Tablet)
                }
            }
        }

        compose.onNodeWithTag(ControlTag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(ControlTag).performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        compose.onNodeWithTag(FirstTag).assertIsFocused()

        compose.onNodeWithTag(FirstTag).performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag(SecondTag).assertIsFocused()
        compose.onNodeWithTag(SecondTag).performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag(SecondTag).assertIsFocused()

        // The focus is back on the control two frames on, while the glass still closes over it and the rows remain.
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(SecondTag).performKeyInput { pressKey(Key.Enter) }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals("Second", chosenRow)
        compose.onNodeWithTag(ControlTag).assertIsFocused()
    }

    @Test
    fun aMenuTallerThanTheHost_standsInsideItsEdges_andScrollsWithoutChoosing() {
        var chosenRows = 0
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset(40.dp, 40.dp).size(46.dp).quvenGlassAnchor(morph))
                    QuvenGlassDropdown(morph, expanded, onDismissRequest = { expanded = false }, Modifier.testTag(MenuTag)) {
                        repeat(LongMenuRows) { QuvenGlassMenuItem("Row $it", { chosenRows++ }) }
                    }
                    QuvenGlassMenuHost(host, backdrop = null, metrics = QuvenGlassMenuMetrics.Tablet)
                }
            }
        }
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()

        val menu = compose.onNodeWithTag(MenuTag).getUnclippedBoundsInRoot()
        assertEquals(16f, menu.top.value, 1f)
        assertEquals(584f, menu.bottom.value, 1f)
        assertTrue(drawnTop("Row 0") < drawnTop("Row 1"))
        compose.onNodeWithText("Row 20").assertIsNotDisplayed()

        compose.onNodeWithText("Row 5").performTouchInput { swipeUp(startY = centerY, endY = centerY - 300f) }
        compose.waitForIdle()

        assertEquals(0, chosenRows)
        assertTrue(expanded)
        compose.onNodeWithText("Row 20").assertIsDisplayed()
    }

    @Test
    fun anUntitledMenuRisingFromItsControl_listsItsEntriesFromTheFootUp() {
        renderAt(top = 500f) {
            QuvenGlassMenuItem("First", {})
            QuvenGlassMenuItem("Second", {})
        }

        assertTrue(drawnTop("First") > drawnTop("Second"))
    }

    @Test
    fun aTitledMenu_keepsItsOrder_evenWhenItRises() {
        renderAt(top = 500f) {
            QuvenGlassMenuTitle("Sleep")
            QuvenGlassMenuItem("First", {})
            QuvenGlassMenuItem("Second", {})
        }

        assertTrue(drawnTop("First") < drawnTop("Second"))
    }

    @Test
    fun aMenuHangingFromItsControl_keepsItsOrder() {
        renderAt(top = 40f) {
            QuvenGlassMenuItem("First", {})
            QuvenGlassMenuItem("Second", {})
        }

        assertTrue(drawnTop("First") < drawnTop("Second"))
    }

    private fun renderAt(top: Float, entries: @Composable ColumnScope.() -> Unit) {
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset(700.dp, top.dp).size(46.dp).quvenGlassAnchor(morph))
                    QuvenGlassDropdown(state = morph, expanded = true, onDismissRequest = {}, content = entries)
                    QuvenGlassMenuHost(host, backdrop = null, metrics = QuvenGlassMenuMetrics.Tablet)
                }
            }
        }
        compose.waitForIdle()
    }

    private fun drawnTop(text: String): Float = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().top.value

    @Test(expected = IllegalStateException::class)
    fun aDropdownWithoutAHost_isRefused() {
        compose.setContent {
            QuvenGlassDropdown(morph, expanded = true, onDismissRequest = {}) {}
        }
    }

    private fun render() {
        val host = QuvenGlassMenuHostState()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalQuvenGlassMenuHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset(700.dp, 40.dp).size(46.dp).quvenGlassAnchor(morph))
                    QuvenGlassDropdown(
                        state = morph,
                        expanded = expanded,
                        onDismissRequest = {
                            dismissals++
                            expanded = false
                        },
                        outsideModifier = Modifier.testTag(OutsideTag),
                    ) {
                        QuvenGlassMenuItem("Play", { chosen++ }, Modifier.testTag(RowTag))
                    }
                    QuvenGlassMenuHost(host, backdrop = null, metrics = QuvenGlassMenuMetrics.Tablet)
                }
            }
        }
    }

    private companion object {
        const val RowTag = "row"
        const val OutsideTag = "outside"
        const val ControlTag = "control"
        const val FirstTag = "first"
        const val SecondTag = "second"
        const val DecoyTag = "decoy"
        const val MenuTag = "menu"
        const val LongMenuRows = 40
    }
}
