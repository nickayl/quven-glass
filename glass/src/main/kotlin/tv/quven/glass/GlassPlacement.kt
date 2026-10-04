package tv.quven.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * Measures this node at the size of the rectangle [rect] reads and places it at the rectangle's corner. The rectangle
 * is read while laying out, so a rectangle that moves relays out this node alone; the node takes the least room its
 * parent allows, wherever it stands.
 *
 * @param rect Reads the rectangle, in pixels, in the parent's coordinates.
 * @return The decorated modifier.
 */
internal fun Modifier.standingAt(rect: Density.() -> Rect): Modifier = layout { measurable, constraints ->
    val bounds = rect()
    val placeable = measurable.measure(
        Constraints.fixed(bounds.width.roundToInt().coerceAtLeast(0), bounds.height.roundToInt().coerceAtLeast(0)),
    )
    layout(constraints.minWidth, constraints.minHeight) { placeable.place(bounds.left.roundToInt(), bounds.top.roundToInt()) }
}
