package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Draws a sidebar of glass floating over the content at the start of its parent, as Apple's split views float their
 * sidebar on an iPad: thick glass inset a little from the parent's edges, which slides in from the start edge while
 * [shown] and out to it otherwise. The content under it stays where it is, seen blurred through the glass; lay the
 * content beside the sidebar instead with [QuvenGlassSplitView].
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
    val presence = rememberSidebarPresence(shown, reduceMotion)
    SidebarPane(presence, shown, modifier, width, style, backdrop, reduceMotion, content)
}

/**
 * Draws a sidebar of glass beside its detail, as Apple's split view lays them out on an iPad: the sidebar floats inset
 * from the parent's start, top and bottom edges, and the detail stands beside it, from the sidebar's end edge to the
 * parent's. Hiding the sidebar slides it out past the start edge while the detail widens to the whole parent, on the same
 * spring; showing it again does the reverse. The sidebar's glass reads what lies under it in the split view, the parent's
 * [ground] where the detail does not reach.
 *
 * @param sidebarShown Whether the sidebar stands in view.
 * @param modifier Modifier applied to the split view.
 * @param sidebarWidth The sidebar's width.
 * @param ground The colour under the sidebar, which its glass shows.
 * @param style The sidebar's material.
 * @param reduceMotion Whether motion is reduced: the sidebar then fades where it stands and the detail moves at once.
 * @param sidebar The sidebar's content, such as its title and its entries.
 * @param detail The detail's content.
 */
@Composable
public fun QuvenGlassSplitView(
    sidebarShown: Boolean,
    modifier: Modifier = Modifier,
    sidebarWidth: Dp = SidebarWidth,
    ground: Color = Color.Black,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    reduceMotion: Boolean = false,
    sidebar: @Composable BoxScope.() -> Unit,
    detail: @Composable BoxScope.() -> Unit,
) {
    val presence = rememberSidebarPresence(sidebarShown, reduceMotion)
    val split = rememberQuvenGlassBackdrop()
    Box(modifier) {
        Box(Modifier.fillMaxSize().quvenGlassSource(split).background(ground)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        val start = detailStart(if (reduceMotion) (if (sidebarShown) 1f else 0f) else presence.value, (SidebarInset + sidebarWidth).roundToPx())
                        val width = (constraints.maxWidth - start).coerceAtLeast(0)
                        val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(start, 0) }
                    },
                content = detail,
            )
        }
        CompositionLocalProvider(LocalQuvenGlassBackdrop provides split) {
            SidebarPane(presence, sidebarShown, Modifier, sidebarWidth, style, split, reduceMotion, sidebar)
        }
    }
}

/**
 * Remembers how far a sidebar has come in, from 0 hidden to 1 in place, moving towards [shown].
 *
 * @param shown Whether the sidebar stands in view.
 * @param reduceMotion Whether motion is reduced.
 * @return The presence.
 */
@Composable
private fun rememberSidebarPresence(shown: Boolean, reduceMotion: Boolean): Animatable<Float, AnimationVector1D> {
    val presence = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(shown, reduceMotion) {
        presence.animateTo(if (shown) 1f else 0f, if (reduceMotion) tween(ReducedMotionFadeMillis) else SidebarSpring)
    }
    return presence
}

/**
 * Draws the sidebar's glass [presence] of the way in.
 *
 * @param presence How far the sidebar has come in.
 * @param shown Whether the sidebar stands in view.
 * @param modifier Modifier applied to the sidebar.
 * @param width The sidebar's width.
 * @param style The material.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 * @param content The sidebar's content.
 */
@Composable
private fun SidebarPane(
    presence: Animatable<Float, AnimationVector1D>,
    shown: Boolean,
    modifier: Modifier,
    width: Dp,
    style: QuvenGlassStyle,
    backdrop: QuvenGlassBackdrop?,
    reduceMotion: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
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
            .liquidGlass(backdrop, remember(style) { style.forSidebars() }, RoundedCornerShape(SidebarCorner), null, reduceMotion, lift = null, pill = null),
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

/**
 * Returns where a split view's detail starts when its sidebar is [presence] of the way in: at the sidebar's end edge,
 * which stands [extent] from the start edge in place.
 *
 * @param presence How far the sidebar has come in, from 0 to 1.
 * @param extent The sidebar's inset and width, in pixels.
 * @return The detail's start, in pixels.
 */
internal fun detailStart(presence: Float, extent: Int): Int = (extent * presence.coerceIn(0f, 1f)).roundToInt()

// Measured on the system's split view sidebar on an iPad.
private val SidebarWidth = 320.dp
private val SidebarInset = 10.dp
private val SidebarCorner = 24.dp
private val SidebarSpring = spring<Float>(dampingRatio = 1f, stiffness = 480f)
