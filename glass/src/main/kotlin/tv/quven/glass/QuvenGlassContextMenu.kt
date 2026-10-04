package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Lifts [content], a card, out of the screen on a long press and opens its glass menu beside it, as a system context
 * menu does: the card grows while it is held, the screen dims behind it as it lifts, and the menu grows out of the
 * card's glass on the side with more room. A press runs [onClick]; the menu closes on a choice, a press elsewhere or
 * Back, and the card settles back into its place.
 *
 * @param menu The menu's entries: [QuvenGlassMenuTitle], [QuvenGlassMenuItem], [QuvenGlassMenuChoice] and
 * [QuvenGlassMenuDivider].
 * @param onClick Invoked when the card is pressed.
 * @param modifier Modifier applied to the card.
 * @param enabled Whether the card answers presses and long presses.
 * @param onClickLabel The name accessibility services give the press, or `null` for theirs.
 * @param menuLabel The name accessibility services give the long press that opens the menu, or `null` for theirs.
 * @param reduceMotion Whether motion is reduced: the card then does not grow while it is held.
 * @param interactionSource The source of the card's presses and focus, or `null` for one of its own.
 * @param content Draws the card.
 * @throws IllegalStateException No [QuvenGlassMenuHost] is provided above the card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun QuvenGlassContextMenuBox(
    menu: @Composable ColumnScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    menuLabel: String? = null,
    reduceMotion: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberQuvenGlassMorphState()
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val preview = rememberGraphicsLayer()
    val growth = remember { Animatable(1f) }
    var open by remember { mutableStateOf(false) }
    var liftedFrom by remember { mutableFloatStateOf(1f) }
    val holdMillis = LocalViewConfiguration.current.longPressTimeoutMillis.toInt()
    LaunchedEffect(pressed, open, reduceMotion) {
        when {
            // Hidden while its preview stands lifted, the card waits at its own size for the menu to close.
            open -> growth.snapTo(1f)
            pressed && !reduceMotion -> growth.animateTo(HeldGrowth, tween(holdMillis, easing = LinearEasing))
            else -> growth.animateTo(1f, spring())
        }
    }
    Box(
        modifier
            .quvenGlassAnchor(state, stretches = false)
            .graphicsLayer {
                alpha = if (state.isShown) 0f else 1f
                scaleX = growth.value
                scaleY = growth.value
            }
            .drawWithContent {
                preview.record { this@drawWithContent.drawContent() }
                drawLayer(preview)
            }
            .combinedClickable(
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = onClickLabel,
                onLongClickLabel = menuLabel,
                onLongClick = {
                    liftedFrom = growth.value
                    open = true
                },
                onClick = onClick,
            ),
        content = content,
    )
    GlassDropdown(
        state = state,
        expanded = open,
        onDismissRequest = {
            // The preview lifts from the size the card was held at, and settles back to the card's own.
            liftedFrom = 1f
            open = false
        },
        modifier = Modifier,
        outsideModifier = Modifier,
        placement = ContextMenuPlacement,
        face = {},
        preview = remember(preview) { GlassMenuPreview(preview) { liftedFrom } },
        content = menu,
    )
}

/**
 * The control a context menu lifts out of the screen: the layer it records itself into and the size it lifts from.
 *
 * @property layer The layer the control records itself into.
 * @property liftedFrom Reads the size the control grows from as it lifts, a share of its own; 1 once the menu closes.
 */
internal class GlassMenuPreview(val layer: GraphicsLayer, val liftedFrom: () -> Float) {

    /**
     * Returns how large the control draws, as a share of its own size, [progress] of the way to its menu standing open.
     *
     * @param progress How far the menu has opened, from 0 to 1, past 1 while its spring overshoots.
     * @return The share.
     */
    fun liftAt(progress: Float): Float = lerp(liftedFrom(), PreviewLift, progress)

    /**
     * Returns where the menu's glass grows out of the lifted control: a capsule half the control's width on the edge the
     * menu stands beyond, as the system's menu flows out of the card it lifts.
     *
     * @param lifted The lifted control's bounds.
     * @param spaceHeight The height of the space the menu stands in.
     * @param seedHeight The height of the capsule.
     * @return The capsule's bounds.
     */
    fun seed(lifted: Rect, spaceHeight: Float, seedHeight: Float): Rect {
        val width = lifted.width * SeedShare
        val left = lifted.center.x - width / 2f
        val top = if (opensAbove(lifted.top, lifted.bottom, spaceHeight)) lifted.top else lifted.bottom - seedHeight
        return Rect(left, top, left + width, top + seedHeight)
    }
}

/** The share of the lifted card's width the menu's glass grows out of. */
private const val SeedShare = 0.5f

/** The height of the capsule a context menu's glass grows out of. */
internal val SeedHeight = 44.dp

/** How much a card grows while it is held, before its context menu opens, as measured on the system's. */
private const val HeldGrowth = 1.06f

/** Where a context menu stands: beside its lifted card, on the side with more room, as the system's does. */
private val ContextMenuPlacement = QuvenGlassMorphPlacement.aboveOrBelow(gap = 22.dp, edge = MenuEdge)
