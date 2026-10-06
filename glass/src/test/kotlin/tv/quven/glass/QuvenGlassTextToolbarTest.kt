package tv.quven.glass

import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuvenGlassTextToolbarTest {

    @Test
    fun actionsThatFit_standOnOnePage() {
        assertEquals(listOf(0..2), editMenuPages(listOf(60, 60, 60), room = 300, arrow = 20, hairline = 1))
    }

    @Test
    fun actionsTooWideForTheWindow_partIntoPages_eachLeavingRoomForItsArrows() {
        val pages = editMenuPages(listOf(80, 80, 80, 80), room = 182, arrow = 20, hairline = 1)

        assertEquals(listOf(0..1, 2..2, 3..3), pages)
        assertEquals(listOf(0..0, 1..1, 2..2), editMenuPages(listOf(80, 80, 80), room = 170, arrow = 20, hairline = 1))
    }

    @Test
    fun anActionWiderThanTheWindow_standsAloneOnItsPage() {
        assertEquals(listOf(0..0, 1..1), editMenuPages(listOf(500, 50), room = 200, arrow = 20, hairline = 1))
    }

    @Test
    fun onATablet_actionsThatFit_allStand_withNoChevron() {
        assertEquals(3, editMenuFit(listOf(60, 60, 60), room = 182, more = 40, hairline = 1))
    }

    @Test
    fun onATablet_actionsTooWide_leaveRoomForTheChevron_andTheFirstAlwaysStands() {
        assertEquals(2, editMenuFit(listOf(80, 80, 80, 80), room = 201, more = 40, hairline = 1))
        assertEquals(1, editMenuFit(listOf(500, 50), room = 200, more = 40, hairline = 1))
    }

    @Test
    fun theToolbar_knowsTheClipboardsActions_byTheirKeys() {
        val toolbar = GlassTextToolbar()

        toolbar.showMenu(Rect.Zero, onCopyRequested = {}, onPasteRequested = {}, onCutRequested = {}, onSelectAllRequested = {})

        assertEquals(listOf(EditKind.Cut, EditKind.Copy, EditKind.Paste, EditKind.SelectAll), toolbar.shown?.actions?.map { it.kind })
        assertEquals(EditKind.Paste, editKindOf(TextContextMenuKeys.PasteKey))
        assertEquals(EditKind.Other, editKindOf(Any()))
    }

    @Test
    fun everyGlyph_standsAtTheSizeOfAMenusGlyph() {
        listOf(EditGlyphs.Cut, EditGlyphs.Copy, EditGlyphs.Paste, EditGlyphs.SelectAll, EditGlyphs.Autofill).forEach { glyph ->
            assertEquals(glyph.name, 24.dp, glyph.defaultWidth)
            assertEquals(glyph.name, 24.dp, glyph.defaultHeight)
        }
    }

    @Test
    fun theToolbar_ordersItsActionsAsTheSystemDoes_andHidesWithNone() {
        val toolbar = GlassTextToolbar()

        toolbar.showMenu(Rect.Zero, onCopyRequested = {}, onPasteRequested = {}, onCutRequested = {}, onSelectAllRequested = {})

        assertEquals(listOf("Cut", "Copy", "Paste", "Select All"), toolbar.shown?.actions?.map { it.label })
        assertEquals(TextToolbarStatus.Shown, toolbar.status)
        toolbar.showMenu(Rect.Zero, null, null, null, null)
        assertNull(toolbar.shown)
    }

    @Test
    fun theProvider_holdsTheMenuUntilItIsClosed() = runTest {
        val provider = GlassTextContextMenuProvider()
        val asked = AskedMenu()

        val showing = launch { provider.showTextContextMenu(asked) }
        runCurrent()
        assertSame(asked, provider.shown)

        provider.close()
        runCurrent()
        assertNull(provider.shown)
        assertTrue(showing.isCompleted)
    }

    private class AskedMenu : TextContextMenuDataProvider {
        override fun position(destinationCoordinates: LayoutCoordinates): Offset = Offset.Zero

        override fun contentBounds(destinationCoordinates: LayoutCoordinates): Rect = Rect.Zero

        override fun data(): TextContextMenuData = TextContextMenuData(emptyList())
    }
}
