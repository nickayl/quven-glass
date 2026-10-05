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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.quven.glass.QuvenGlassContainer
import tv.quven.glass.QuvenGlassSegmentedTrack
import tv.quven.glass.rememberQuvenGlassAppearance

private class BarEntry(val label: String, val icon: ImageVector)

private val Entries = listOf(
    BarEntry("Home", Icons.Filled.Home),
    BarEntry("Watch", Icons.Filled.PlayArrow),
    BarEntry("Favorites", Icons.Filled.Star),
    BarEntry("Explore", Icons.Filled.Info),
    BarEntry("More", Icons.Filled.MoreVert),
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
    val colour by animateColorAsState(if (held) SampleColors.Accent else SampleColors.TextHigh, label = "entryColour")
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
        Icon(entry.icon, contentDescription = entry.label, tint = colour, modifier = Modifier.size(GlyphSize).alpha(if (showsIcon) 1f else 0f))
        if (showsLabel) {
            Text(
                entry.label,
                color = colour,
                fontSize = 12.sp,
                lineHeight = LabelLine,
                fontWeight = if (held) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
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
            Canvas(Modifier.size(20.dp)) {
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
    }
}

/** The columns each density option lays its covers in. */
private val DensityColumns = listOf(3, 2, 1)

private val GlyphSize = 24.dp

/** How long Search's glyph takes to fade from chosen to plain as the field opens. */
private const val SearchFadeMillis = 100
private val LabelLine = 16.sp

/** The size of a named entry on a phone's bar, as the reference's tab bar sizes it on an iPhone. */
internal val PhoneEntrySize: DpSize = DpSize(92.dp, 54.dp)

/** The width of a phone bar's capsule of three entries, as its track lays them out. */
internal val PhoneTabsWidth: Dp = PhoneEntrySize.width * 3 + TrackGap * 2 + TrackInset * 2

/** The height of a phone's bar, the side of its Search circle. */
internal val PhoneBarHeight: Dp = PhoneEntrySize.height + TrackInset * 2

/**
 * Returns the centre of an entry's glyph in a phone bar's capsule, a glyph and a label stacked in the middle of the entry.
 *
 * @param index The index of the entry.
 * @return The centre, from the capsule's top start corner.
 */
internal fun phoneGlyphCentre(index: Int): DpOffset = DpOffset(
    TrackInset + (PhoneEntrySize.width + TrackGap) * index + PhoneEntrySize.width / 2,
    TrackInset + (PhoneEntrySize.height - GlyphSize - LabelLine.value.dp) / 2 + GlyphSize / 2,
)

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
    val colour by animateColorAsState(if (selected) SampleColors.Accent else SampleColors.TextHigh, label = "heldColour")
    Icon(entry.icon, contentDescription = entry.label, tint = colour, modifier = Modifier.size(GlyphSize))
}

/**
 * Draws the Search glyph.
 *
 * @param selected Whether Search is drawn as chosen.
 */
@Composable
internal fun SampleSearchGlyph(selected: Boolean) {
    val colour by animateColorAsState(
        if (selected) SampleColors.Accent else SampleColors.TextHigh,
        animationSpec = tween(SearchFadeMillis),
        label = "searchColour",
    )
    Icon(SearchEntry.icon, contentDescription = SearchEntry.label, tint = colour, modifier = Modifier.size(GlyphSize))
}
