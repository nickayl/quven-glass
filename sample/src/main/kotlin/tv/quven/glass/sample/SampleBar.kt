package tv.quven.glass.sample

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.quven.glass.QuvenGlassContainer
import tv.quven.glass.QuvenGlassSegmentedTrack
import tv.quven.glass.QuvenGlassTrackFeel
import tv.quven.glass.rememberQuvenGlassAppearance

private class BarEntry(val label: String, val icon: ImageVector)

/**
 * How a bar draws its entries.
 *
 * @property accent The colour of the chosen entry.
 * @property glyph The size a glyph is drawn at, so a stock icon stands as large as the reference's symbol.
 * @property label The size of a label.
 * @property labelDrop How far a label stands below its place under the glyph.
 */
internal class BarLook(val accent: Color, val glyph: Dp, val label: TextUnit, val labelDrop: Dp)

/** The reference's own bar, its stock icons drawn large enough to stand as its symbols, marked in the sample's accent. */
internal val ReferenceBarLook = BarLook(SampleColors.Accent, 34.dp, 12.sp, 2.5.dp)

/**
 * Returns the look of the system's tab bar, its symbols and labels smaller than the reference's own bar's, marked in the
 * blue the system draws a chosen tab in on glass; on an iPad its symbols stand about 24 pt tall, larger than on an iPhone.
 *
 * @param tablet Whether the bar stands on a tablet.
 * @return The look.
 */
internal fun systemBarLook(tablet: Boolean): BarLook = if (tablet) TabletSystemBarLook else PhoneSystemBarLook

// Measured on the system's tab bar on an iPad: a chosen tab's glyph reads (33, 179, 255).
private val SystemBarAccent = Color(0xFF21B3FF)
private val PhoneSystemBarLook = BarLook(SystemBarAccent, 25.dp, 10.sp, 1.dp)
private val TabletSystemBarLook = BarLook(SystemBarAccent, 31.dp, 10.sp, 1.dp)

/** The look the bars below draw their entries in. */
internal val LocalBarLook = staticCompositionLocalOf { ReferenceBarLook }

private val Entries = listOf(
    BarEntry("Home", Icons.Filled.Home),
    BarEntry("Watch", PlayFill),
    BarEntry("Favorites", Icons.Filled.Star),
    BarEntry("Explore", Icons.Filled.Info),
    BarEntry("More", MoreHorizontal),
)
private val SearchEntry = BarEntry("Search", Icons.Filled.Search)
private val EntrySize = DpSize(96.dp, 62.dp)
private val TrackGap = 2.dp
private val TrackInset = 4.dp

/**
 * Draws a bar: [count] named entries in a glass capsule, five on a tablet and three on a phone, and Search in a glass
 * circle beside it, joined into one piece of glass as they come close.
 *
 * @param held The index of the held entry.
 * @param onHold Invoked with the index of a pressed entry.
 * @param tuning The live settings.
 */
@Composable
internal fun SampleBar(held: Int, onHold: (Int) -> Unit, tuning: SampleTuning) {
    QuvenGlassContainer(style = tuning.style, spacing = tuning.spacing.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(tuning.barGap.dp), verticalAlignment = Alignment.CenterVertically) {
            SampleTabs(held, onHold, tuning, Entries.size, EntrySize)
            QuvenGlassSegmentedTrack(
                options = listOf(SearchEntry),
                held = -1,
                optionSize = DpSize(EntrySize.height, EntrySize.height),
                style = tuning.style,
                inset = TrackInset,
                reduceMotion = tuning.reduceMotion,
            ) { entry, isHeld ->
                EntryFace(entry, isHeld, showsLabel = false)
            }
        }
    }
}

/**
 * Draws the first [count] named entries in a glass capsule.
 *
 * @param held The index of the held entry.
 * @param onHold Invoked with the index of a pressed entry.
 * @param tuning The live settings.
 * @param count The number of entries.
 * @param entrySize The size of an entry.
 */
@Composable
internal fun SampleTabs(held: Int, onHold: (Int) -> Unit, tuning: SampleTuning, count: Int, entrySize: DpSize) {
    val entries = Entries.take(count)
    QuvenGlassSegmentedTrack(
        options = entries,
        held = held,
        optionSize = entrySize,
        style = tuning.style,
        gap = TrackGap,
        inset = TrackInset,
        reduceMotion = tuning.reduceMotion,
        onDraggedTo = { entry -> onHold(entries.indexOf(entry)) },
    ) { entry, isHeld ->
        EntryFace(entry, isHeld, showsLabel = true) { onHold(entries.indexOf(entry)) }
    }
}

@Composable
private fun EntryFace(entry: BarEntry, held: Boolean, showsLabel: Boolean, showsIcon: Boolean = true, onClick: (() -> Unit)? = null) {
    val colour by animateColorAsState(if (held) LocalBarLook.current.accent else SampleColors.TextHigh, label = "entryColour")
    Column(
        Modifier
            .fillMaxSize()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.selectable(selected = held, role = Role.Tab, interactionSource = null, indication = null, onClick = onClick)
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(GlyphSize), contentAlignment = Alignment.Center) {
            Icon(entry.icon, contentDescription = entry.label, tint = colour, modifier = Modifier.requiredSize(LocalBarLook.current.glyph).alpha(if (showsIcon) 1f else 0f))
        }
        if (showsLabel) {
            Text(
                entry.label,
                color = colour,
                fontSize = LocalBarLook.current.label,
                lineHeight = LabelLine,
                fontWeight = if (held) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.offset(y = LocalBarLook.current.labelDrop),
            )
        }
    }
}

/**
 * Draws a group of three options as a catalogue's density selector.
 *
 * @param held The index of the held option.
 * @param onHold Invoked with the index of a pressed option.
 * @param tuning The live settings.
 */
@Composable
internal fun SampleDensityTrack(held: Int, onHold: (Int) -> Unit, tuning: SampleTuning) {
    val appearance = rememberQuvenGlassAppearance()
    QuvenGlassSegmentedTrack(
        options = DensityColumns,
        held = held,
        optionSize = DpSize(52.dp, 44.dp),
        style = tuning.style.copy(ground = Color.Transparent),
        gap = 3.dp,
        inset = 4.dp,
        reduceMotion = tuning.reduceMotion,
        feel = QuvenGlassTrackFeel.Selector,
        onDraggedTo = { columns -> onHold(DensityColumns.indexOf(columns)) },
        appearance = appearance,
    ) { columns, isHeld ->
        val onDark by animateColorAsState(if (isHeld) SampleColors.TextHigh else SampleColors.TextMedium, label = "densityColour")
        val onLight by animateColorAsState(
            if (isHeld) SampleColors.TextHighOnLight else SampleColors.TextMediumOnLight,
            label = "densityColourOnLight",
        )
        val colour = appearance.contentColor(onDark, onLight)
        Box(
            Modifier
                .fillMaxSize()
                .selectable(selected = isHeld, role = Role.RadioButton, interactionSource = null, indication = null) {
                    onHold(DensityColumns.indexOf(columns))
                },
            contentAlignment = Alignment.Center,
        ) {
            DensityGlyph(columns, colour, Modifier.size(20.dp))
        }
    }
}

/**
 * Draws a density option's glyph: [columns] by [columns] rounded squares.
 *
 * @param columns The squares in a row and in a column.
 * @param colour The squares' colour.
 * @param modifier Modifier applied to the glyph.
 */
@Composable
internal fun DensityGlyph(columns: Int, colour: Color, modifier: Modifier) {
    Canvas(modifier) {
        val cell = size.width / columns
        for (row in 0 until columns) {
            for (column in 0 until columns) {
                drawRoundRect(
                    colour,
                    topLeft = Offset(column * cell + cell * 0.1f, row * cell + cell * 0.1f),
                    size = Size(cell * 0.8f, cell * 0.8f),
                    cornerRadius = CornerRadius(cell * 0.18f),
                )
            }
        }
    }
}

/** The columns each density option lays its covers in. */
private val DensityColumns = listOf(3, 2, 1)

private val GlyphSize = 24.dp

/** How long Search's glyph takes to fade from chosen to plain as the field opens. */
private const val SearchFadeMillis = 100
private val LabelLine = 16.sp

/**
 * The layout of a phone's bar as the reference's system tab bar lays it out: [count] entries in a capsule, with Search
 * beside them or alone. On an iPhone the capsule fills the bar, 288 dp beside Search and 354 alone; on an iPad it hugs
 * its entries, 257.5 dp for three and 336 for four.
 *
 * @property count The number of entries, three or four.
 * @param tablet Whether the bar stands on a tablet.
 * @param search Whether Search stands beside the entries.
 */
internal class PhoneBarLayout(val count: Int, tablet: Boolean, search: Boolean) {

    /** Gets the width of the capsule. */
    val tabsWidth: Dp = when {
        tablet -> if (count == 3) 257.5.dp else 336.dp
        search -> 288.dp
        else -> 354.dp
    }

    /** Gets the size of an entry. */
    val entrySize: DpSize = DpSize((tabsWidth - TrackInset * 2 - TrackGap * (count - 1)) / count, PhoneBarHeight - TrackInset * 2)

    /**
     * Returns the centre of an entry's glyph, a glyph and a label stacked in the middle of the entry.
     *
     * @param index The index of the entry.
     * @return The centre, from the capsule's top start corner.
     */
    fun glyphCentre(index: Int): DpOffset = DpOffset(
        TrackInset + (entrySize.width + TrackGap) * index + entrySize.width / 2,
        TrackInset + (entrySize.height - GlyphSize - LabelLine.value.dp) / 2 + GlyphSize / 2,
    )
}

/** The height of a phone's bar, the side of its Search circle. */
internal val PhoneBarHeight: Dp = 62.dp

/** The room between a phone's bar and the start and end edges of the reference's stage on an iPad. */
internal val PhoneBarMargin: Dp = 10.dp

/** The room between a phone's bar and the bottom edge of the reference's stage on an iPad. */
internal val PhoneBarBottom: Dp = 11.dp

/**
 * Draws the faces of the first [count] entries where the capsule lays them out, without the held entry's glyph and
 * without answering presses, the held entry on its platter while it stands chosen.
 *
 * @param count The number of entries.
 * @param held The index of the held entry.
 * @param selected Whether the held entry stands chosen.
 * @param tuning The live settings.
 */
@Composable
internal fun RowScope.SampleTabsFace(count: Int, held: Int, selected: Boolean, tuning: SampleTuning) {
    Row(Modifier.weight(1f).fillMaxHeight().padding(TrackInset), horizontalArrangement = Arrangement.spacedBy(TrackGap)) {
        Entries.take(count).forEachIndexed { index, entry ->
            val chosen = selected && index == held
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (chosen) tuning.style.platter else Color.Transparent, CircleShape),
            ) {
                EntryFace(entry, held = chosen, showsLabel = true, showsIcon = index != held)
            }
        }
    }
}

/**
 * Draws the glyph of the held entry, as the bar folded into a circle shows it.
 *
 * @param held The index of the held entry.
 * @param selected Whether the entry is drawn as chosen.
 */
@Composable
internal fun SampleHeldGlyph(held: Int, selected: Boolean) {
    val entry = Entries[held]
    val colour by animateColorAsState(if (selected) LocalBarLook.current.accent else SampleColors.TextHigh, label = "heldColour")
    Icon(entry.icon, contentDescription = entry.label, tint = colour, modifier = Modifier.requiredSize(LocalBarLook.current.glyph))
}

/**
 * Draws the Search glyph.
 *
 * @param selected Whether Search is drawn as chosen.
 */
@Composable
internal fun SampleSearchGlyph(selected: Boolean) {
    val colour by animateColorAsState(
        if (selected) LocalBarLook.current.accent else SampleColors.TextHigh,
        animationSpec = tween(SearchFadeMillis),
        label = "searchColour",
    )
    Icon(SearchEntry.icon, contentDescription = SearchEntry.label, tint = colour, modifier = Modifier.size(GlyphSize))
}
