package tv.quven.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One button of a [QuvenGlassToolbarGroup].
 *
 * @property icon The button's glyph.
 * @property contentDescription The button's name.
 * @property onClick Invoked when the button is pressed.
 */
@Immutable
public class QuvenGlassToolbarItem(
    public val icon: Painter,
    public val contentDescription: String,
    public val onClick: () -> Unit,
)

/**
 * Draws a group of toolbar buttons on one capsule of glass, as Apple's toolbars group their items: a single button
 * stands in a circle, more share a capsule. A press on any of them swells the whole capsule as a glass button swells and
 * lights it towards white around the finger, so that it joins a neighbouring group it comes close to inside the same
 * [QuvenGlassContainer]. Lay groups out in a row, [QuvenGlassToolbarGap] apart, inside a container whose spacing is
 * [QuvenGlassToolbarJoin].
 *
 * @param items The buttons, from the start.
 * @param modifier Modifier applied to the capsule.
 * @param style The material.
 * @param ink The colour of the glyphs.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 */
@Composable
public fun QuvenGlassToolbarGroup(
    items: List<QuvenGlassToolbarItem>,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    ink: Color = Color.White,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
) {
    require(items.isNotEmpty()) { "A toolbar group holds at least one item." }
    val presses = remember { MutableInteractionSource() }
    val material = remember(style) { style.forToolbarItems() }
    GlassButton(
        onClick = null,
        modifier = modifier.height(ToolbarItemHeight).width(groupWidth(items.size)),
        shape = CircleShape,
        material = material,
        prominent = false,
        ink = ink,
        lightInk = Color.Black,
        contentPadding = PaddingValues(0.dp),
        enabled = true,
        backdrop = backdrop,
        reduceMotion = reduceMotion,
        interactionSource = presses,
    ) { shownInk ->
        Row {
            items.forEach { item ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { contentDescription = item.contentDescription }
                        .clickable(interactionSource = presses, indication = null, role = Role.Button, onClick = item.onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(item.icon, contentDescription = null, colorFilter = ColorFilter.tint(shownInk), modifier = Modifier.size(ToolbarGlyph))
                }
            }
        }
    }
}

/**
 * Returns the width of a toolbar capsule holding [count] buttons, as Apple sizes them: a circle for one, and 59 dp more
 * for each further button.
 *
 * @param count The number of buttons.
 * @return The width.
 */
internal fun groupWidth(count: Int): Dp = ToolbarItemHeight + ToolbarItemStep * (count - 1)

/** The room between two toolbar groups standing side by side, as Apple's toolbars part them. */
public val QuvenGlassToolbarGap: Dp = 13.dp

/** The spacing a container of toolbar groups joins them at, so that only a swollen group reaches its neighbour. */
public val QuvenGlassToolbarJoin: Dp = 8.dp

// Measured on the system's toolbar on an iPhone.
private val ToolbarItemHeight = 44.dp
private val ToolbarItemStep = 59.dp
private val ToolbarGlyph = 22.dp
