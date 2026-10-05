package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Draws a sidebar of glass floating over the content at the start of its parent, as Apple's split views float their
 * sidebar on an iPad: thick glass inset a little from the parent's edges, which slides in from the start edge while
 * [shown] and out to it otherwise. The content under it stays where it is, seen blurred through the glass.
 *
 * @param shown Whether the sidebar stands in view.
 * @param modifier Modifier applied to the sidebar.
 * @param width The sidebar's width.
 * @param style The material.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced: the sidebar then fades in and out where it stands.
 * @param content The sidebar's content, such as its title and its entries.
 */
@Composable
public fun QuvenGlassSidebar(
    shown: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = SidebarWidth,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val presence = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(shown, reduceMotion) {
        presence.animateTo(if (shown) 1f else 0f, if (reduceMotion) tween(ReducedMotionFadeMillis) else SidebarSpring)
    }
    if (presence.value <= 0f && !shown) return
    Box(
        modifier
            .padding(SidebarInset)
            .width(width)
            .fillMaxHeight()
            // The glass reads what lies under where it stands, so it travels by its layout, which mirrors for the
            // layout direction, not by its layer.
            .offset { IntOffset(if (reduceMotion) 0 else -sidebarAway(presence.value, (width + SidebarInset).roundToPx()), 0) }
            .graphicsLayer { alpha = if (reduceMotion) presence.value else 1f }
            .liquidGlass(backdrop, style, RoundedCornerShape(SidebarCorner), null, reduceMotion, lift = null, pill = null),
        content = content,
    )
}

/**
 * Returns how far a sidebar [presence] of the way in stands from its place, travelling its own width and its inset so
 * it leaves from beyond the edge.
 *
 * @param presence How far the sidebar has come in, from 0 to 1.
 * @param travel The sidebar's width and its inset, in pixels.
 * @return The distance towards the start edge, in pixels.
 */
internal fun sidebarAway(presence: Float, travel: Int): Int = (travel * (1f - presence)).roundToInt()

// Measured on the system's split view sidebar on an iPad.
private val SidebarWidth = 221.dp
private val SidebarInset = 7.dp
private val SidebarCorner = 24.dp
private val SidebarSpring = spring<Float>(dampingRatio = 1f, stiffness = 480f)
