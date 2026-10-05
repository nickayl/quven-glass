package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
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
@Config(application = Application::class, sdk = [34], qualifiers = "w500dp-h300dp")
class QuvenGlassSearchMorphTest {

    @get:Rule
    val compose = createComposeRule()

    private var searching by mutableStateOf(false)
    private var minimized by mutableStateOf(false)

    @Test
    fun aPressOnSearch_opensTheField() {
        render()

        compose.onNodeWithTag(SearchTag, useUnmergedTree = true).performClick()

        compose.runOnIdle { assertTrue(searching) }
    }

    @Test
    fun atRest_theFieldIsNotThereToTakeThePress_andComesWithTheSearch() {
        render()

        compose.onNodeWithTag(FieldTag, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(CircleTag, useUnmergedTree = true).assertExists()
        searching = true

        compose.onNodeWithTag(FieldTag, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theSearchModifier_reachesTheSearchCircle() {
        render()

        compose.onNodeWithTag(CircleTag, useUnmergedTree = true).performClick()

        compose.runOnIdle { assertTrue(searching) }
    }

    @Test
    fun aPressOnTheFoldedTabs_endsTheSearch() {
        searching = true
        render()

        compose.onNodeWithTag(HeldTag, useUnmergedTree = true).performClick()

        compose.runOnIdle { assertFalse(searching) }
    }

    @Test
    fun atRest_theBarDrawsTheCallersTabs_andTheSearchCircleBesideThem() {
        render()

        val tabs = compose.onNodeWithTag(TabsTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val glyph = compose.onNodeWithTag(SearchTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(TabsWidth, (tabs.right - tabs.left).value, 0.5f)
        assertEquals(TabsWidth + Gap + Height / 2, ((glyph.left + glyph.right) / 2).value, 0.5f)
        compose.onNodeWithTag(HeldTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun searching_theTabsFoldIntoACircleAtTheStart_holdingTheHeldGlyph_andTheFieldSpansTheRest() {
        searching = true
        render()

        val held = compose.onNodeWithTag(HeldTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val field = compose.onNodeWithTag(FieldTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val sunk = Height - Inset * 2
        assertEquals(Inset + sunk / 2, ((held.left + held.right) / 2).value, 0.5f)
        assertEquals(Height / 2, ((held.top + held.bottom) / 2).value, 0.5f)
        assertTrue(field.left.value > Height)
        compose.onNodeWithTag(TabsTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun atRest_andSearching_theFrameIsTheBarAndThenTheSunkenCircleAndField() {
        val rest = searchMorphFrame(0f, opening = true, Sizes)
        val search = searchMorphFrame(1f, opening = false, Sizes)

        assertEquals(Rect(0f, 0f, TabsWidth, Height), rest.fold.tabs)
        assertEquals(Rect(TabsWidth + Gap, 0f, TabsWidth + Gap + Height, Height), rest.field)
        assertEquals(Rect(Inset, Inset, Height - Inset, Height - Inset), search.fold.tabs)
        assertEquals(TabsWidth + Gap + Height - Inset, search.field.right, 0.01f)
        assertEquals(Inset * 2, search.field.left - search.fold.tabs.right, 0.01f)
    }

    @Test
    fun opening_theRoomBetweenTheCapsulesGrows_andTheFacesKeepTheirSize() {
        val frame = searchMorphFrame(0.5f, opening = true, Sizes)

        assertTrue(frame.field.left - frame.fold.tabs.right > Gap)
        assertEquals(1f, frame.fold.facesScale)
    }

    @Test
    fun closing_theCapsulesComeCloseEnoughToJoin_andTheFacesGrowWithTheirCapsule() {
        val frame = searchMorphFrame(0.6f, opening = false, Sizes)

        assertEquals(NearGap, frame.field.left - frame.fold.tabs.right, 0.01f)
        assertEquals(frame.fold.tabs.width / TabsWidth, frame.fold.facesScale, 0.01f)
        assertTrue(frame.fold.facesScale < 1f)
        assertTrue(frame.fieldAlpha < 0.01f)
    }

    @Test
    fun minimized_theTabsFoldAtTheStart_andTheSearchCircleSinksWhereItStands() {
        val frame = minimizedBarFrame(1f, minimizing = true, Sizes)

        assertEquals(Rect(Inset, Inset, Height - Inset, Height - Inset), frame.fold.tabs)
        assertEquals(Rect(TabsWidth + Gap + Inset, Inset, TabsWidth + Gap + Height - Inset, Height - Inset), frame.field)
        assertEquals(0f, frame.fieldAlpha)
    }

    @Test
    fun aSearchOpenedFromTheMinimizedBar_startsFromTheFoldedFrame_andEndsWhereAnyOtherSearchDoes() {
        val folded = minimizedBarFrame(1f, minimizing = true, Sizes)
        val start = searchMorphFrame(0f, opening = true, Sizes, fromFold = true)
        val end = searchMorphFrame(1f, opening = true, Sizes, fromFold = true)
        val plain = searchMorphFrame(1f, opening = true, Sizes)

        assertEquals(folded.fold.tabs, start.fold.tabs)
        assertEquals(0f, start.fold.facesAlpha)
        assertEquals(folded.field, start.field)
        assertEquals(plain.field, end.field)
        assertEquals(plain.fold.tabs, end.fold.tabs)
    }

    @Test
    fun aSearchOpenedFromTheMinimizedBar_keepsTheHeldGlyphInTheFoldedCircle_fromTheFirstFrame() {
        minimized = true
        render()
        compose.mainClock.autoAdvance = false

        searching = true
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(32)

        val glyph = compose.onNodeWithTag(HeldTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(Height / 2f, (glyph.left.value + glyph.right.value) / 2f, 3f)
    }

    @Test
    fun aPressOnTheMinimizedBar_asksItBack_andEndsNoSearch() {
        minimized = true
        render()

        compose.onNodeWithTag(HeldTag, useUnmergedTree = true).performClick()

        compose.runOnIdle {
            assertFalse(minimized)
            assertFalse(searching)
        }
    }

    private fun render() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.fillMaxSize()) {
                    QuvenGlassSearchMorph(
                        searching = searching,
                        onSearch = { searching = true },
                        onEndSearch = { searching = false },
                        tabsWidth = TabsWidth.dp,
                        height = Height.dp,
                        gap = Gap.dp,
                        heldCentre = DpOffset(50.dp, 20.dp),
                        backdrop = null,
                        tabs = { Box(Modifier.fillMaxSize().testTag(TabsTag)) },
                        tabsFace = { BasicText("Tabs") },
                        heldGlyph = { Box(Modifier.size(20.dp).testTag(HeldTag)) },
                        searchGlyph = { Box(Modifier.size(20.dp).testTag(SearchTag)) },
                        field = { BasicText("Search", Modifier.testTag(FieldTag)) },
                        minimized = minimized,
                        onExpand = { minimized = false },
                        searchModifier = Modifier.testTag(CircleTag),
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val TabsTag = "tabs"
        const val HeldTag = "held"
        const val SearchTag = "search"
        const val CircleTag = "circle"
        const val FieldTag = "field"
        const val TabsWidth = 288f
        const val Height = 62f
        const val Gap = 8f
        const val Inset = 7.25f
        const val NearGap = 4f
        val Sizes = SearchMorphSizes(TabsWidth, Height, Gap, Inset, NearGap)
    }
}
