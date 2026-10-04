package tv.quven.glass.sample

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.quven.glass.QuvenGlassContainer
import tv.quven.glass.QuvenGlassSegmentedTrack
import tv.quven.glass.rememberQuvenGlassAppearance

private class BarEntry(val label: String, val icon: ImageVector)

private val Entries = listOf(
    BarEntry("Home", Icons.Filled.Home),
    BarEntry("Film", Icons.Filled.PlayArrow),
    BarEntry("Serie", Icons.Filled.Star),
    BarEntry("Documentari", Icons.Filled.Info),
    BarEntry("Altro", Icons.Filled.MoreVert),
)
private val SearchEntry = BarEntry("Cerca", Icons.Filled.Search)
private val EntrySize = DpSize(96.dp, 62.dp)

/**
 * Draws a tablet bar: five named entries in a glass capsule and Search in a glass circle beside it,
 * joined into one piece of glass as they come close.
 *
 * @param held The index of the held entry.
 * @param onHold Invoked with the index of a pressed entry.
 * @param tuning The live settings.
 */
@Composable
internal fun SampleBar(held: Int, onHold: (Int) -> Unit, tuning: SampleTuning) {
    QuvenGlassContainer(style = tuning.style, spacing = tuning.spacing.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(tuning.barGap.dp), verticalAlignment = Alignment.CenterVertically) {
            QuvenGlassSegmentedTrack(
                options = Entries,
                held = held,
                optionSize = EntrySize,
                style = tuning.style,
                gap = 2.dp,
                inset = 4.dp,
                reduceMotion = tuning.reduceMotion,
                onDraggedTo = { entry -> onHold(Entries.indexOf(entry)) },
            ) { entry, isHeld ->
                EntryFace(entry, isHeld, showsLabel = true) { onHold(Entries.indexOf(entry)) }
            }
            QuvenGlassSegmentedTrack(
                options = listOf(SearchEntry),
                held = -1,
                optionSize = DpSize(EntrySize.height, EntrySize.height),
                style = tuning.style,
                inset = 4.dp,
                reduceMotion = tuning.reduceMotion,
            ) { entry, isHeld ->
                EntryFace(entry, isHeld, showsLabel = false) {}
            }
        }
    }
}

@Composable
private fun EntryFace(entry: BarEntry, held: Boolean, showsLabel: Boolean, onClick: () -> Unit) {
    val colour by animateColorAsState(if (held) SampleColors.Accent else SampleColors.TextHigh, label = "entryColour")
    Column(
        Modifier
            .fillMaxSize()
            .selectable(selected = held, role = Role.Tab, interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(entry.icon, contentDescription = entry.label, tint = colour, modifier = Modifier.size(24.dp))
        if (showsLabel) {
            Text(entry.label, color = colour, fontSize = 12.sp, fontWeight = if (held) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
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
