package tv.quven.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Describes how a glass menu lays out its rows, as Apple lays out its system menus. A menu holding a choice gains a
 * column for its check before every row, which widens it by [choiceShift].
 *
 * @property width The least width of a menu; a longer row widens it up to [maxWidth].
 * @property maxWidth The greatest width of a menu, past which a row's name is cut short.
 * @property cornerRadius The radius of the open menu's corners.
 * @property verticalInset The room above the menu's first entry and below its last.
 * @property rowHeight The height of a row.
 * @property titleAbove The room above the middle of a section's title.
 * @property titleBelow The room between the middle of a section's title and the row under it.
 * @property sideInset The distance from the menu's sides to a section's title, a divider's ends and a row's trailing
 * glyph.
 * @property iconCentre The distance from the menu's start to the middle of a row's glyph.
 * @property iconSize The size of a row's glyph.
 * @property labelStart The distance from the menu's start to a row's name in a menu with glyphs.
 * @property plainLabelStart The distance from the menu's start to a row's name in a menu with neither glyphs nor
 * choices.
 * @property checkCentre The distance from the menu's start to the middle of a chosen row's check.
 * @property checkSize The width of a chosen row's check.
 * @property choiceLabelStart The distance from the menu's start to the name of a choice.
 * @property choiceShift The room the column of checks takes before every other row of a menu holding a choice.
 * @property dividerSpace The room above and below a divider.
 * @property labelSize The size of a row's name.
 * @property titleSize The size of a section's title.
 * @property highlightInset The room between the menu's sides and a held row's highlight, a capsule as tall as the row
 * less a sliver above and below.
 * @property previewLift How much larger a context menu lifts its control, as a share of its own size.
 */
@Immutable
public data class QuvenGlassMenuMetrics(
    val width: Dp,
    val maxWidth: Dp,
    val cornerRadius: Dp,
    val verticalInset: Dp,
    val rowHeight: Dp,
    val titleAbove: Dp,
    val titleBelow: Dp,
    val sideInset: Dp,
    val iconCentre: Dp,
    val iconSize: Dp,
    val labelStart: Dp,
    val plainLabelStart: Dp,
    val checkCentre: Dp,
    val checkSize: Dp,
    val choiceLabelStart: Dp,
    val choiceShift: Dp,
    val dividerSpace: Dp,
    val labelSize: TextUnit,
    val titleSize: TextUnit,
    val highlightInset: Dp,
    val previewLift: Float = PhonePreviewLift,
) {
    public companion object {
        /** Gets the layout of a menu on a tablet, as iPadOS lays out its system menus. */
        public val Tablet: QuvenGlassMenuMetrics = QuvenGlassMenuMetrics(
            width = 223.dp,
            maxWidth = 320.dp,
            cornerRadius = 25.dp,
            verticalInset = 6.dp,
            rowHeight = 38.dp,
            titleAbove = 14.5.dp,
            titleBelow = 13.5.dp,
            sideInset = 20.dp,
            iconCentre = 32.5.dp,
            iconSize = 22.dp,
            labelStart = 55.dp,
            plainLabelStart = 21.5.dp,
            checkCentre = 21.dp,
            checkSize = 9.dp,
            choiceLabelStart = 32.dp,
            choiceShift = 12.dp,
            dividerSpace = 8.dp,
            labelSize = 15.sp,
            titleSize = 12.sp,
            highlightInset = 13.dp,
            previewLift = TabletPreviewLift,
        )

        /** Gets the layout of a menu on a phone, as iOS lays out its system menus. */
        public val Phone: QuvenGlassMenuMetrics = QuvenGlassMenuMetrics(
            width = 247.dp,
            maxWidth = 340.dp,
            cornerRadius = 25.dp,
            verticalInset = 6.dp,
            rowHeight = 42.dp,
            titleAbove = 12.dp,
            titleBelow = 19.dp,
            sideInset = 26.dp,
            iconCentre = 37.dp,
            iconSize = 24.dp,
            labelStart = 62.dp,
            plainLabelStart = 27.5.dp,
            checkCentre = 24.dp,
            checkSize = 10.dp,
            choiceLabelStart = 36.dp,
            choiceShift = 14.dp,
            dividerSpace = 9.5.dp,
            labelSize = 17.sp,
            titleSize = 13.sp,
            highlightInset = 14.dp,
        )

        // Measured on the system's context menus: an iPhone lifts the card a tenth larger, an iPad more than half again.
        private const val PhonePreviewLift = 1.1f
        private const val TabletPreviewLift = 1.6f
    }
}

/**
 * Describes the colours of a glass menu on the thick glass it opens into.
 *
 * @property label The colour of a row's name and glyph.
 * @property title The colour of a section's title.
 * @property divider The colour of the line between sections.
 * @property destructive The colour of a row whose action cannot be undone.
 * @property disabled The colour of a row that cannot be chosen now.
 * @property highlight The colour of a held row's highlight.
 * @property pressWash The colour laid over the whole menu while a row is pressed.
 */
@Immutable
public data class QuvenGlassMenuColors(
    val label: Color = Color.White,
    val title: Color = Color(0x8CFFFFFF),
    val divider: Color = Color(0x2EFFFFFF),
    val destructive: Color = Color(0xFFFF6B6E),
    val disabled: Color = Color(0x99FFFFFF),
    val highlight: Color = Color(0x29FFFFFF),
    val pressWash: Color = Color(0x26FFFFFF),
) {
    public companion object {
        /** Gets the colours of Apple's system menus on dark glass. */
        public val Standard: QuvenGlassMenuColors = QuvenGlassMenuColors()
    }
}
