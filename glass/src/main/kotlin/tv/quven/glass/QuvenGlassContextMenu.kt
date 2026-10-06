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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
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
 * @param preview Draws what lifts out of the screen in the card's place, at its own size, or `null` to lift the card a
 * tenth larger.
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
    preview: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberQuvenGlassMorphState()
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val card = rememberGraphicsLayer()
    val own = rememberGraphicsLayer()
    var ownSize by remember { mutableStateOf(IntSize.Zero) }
    val growth = remember { Animatable(1f) }
    var open by remember { mutableStateOf(false) }
    var liftedFrom by remember { mutableFloatStateOf(1f) }
    val system = LocalViewConfiguration.current
    val configuration = remember(system) { ContextMenuViewConfiguration(system) }
    val holdMillis = configuration.longPressTimeoutMillis.toInt()
    LaunchedEffect(pressed, open, reduceMotion) {
        when {
            // Hidden while its preview stands lifted, the card waits at its own size for the menu to close.
            open -> growth.snapTo(1f)
            pressed && !reduceMotion -> growth.animateTo(HeldGrowth, tween(holdMillis, easing = LinearEasing))
            else -> growth.animateTo(1f, spring())
        }
    }
    // The card's long press holds as long as iOS's; what the card holds keeps the system's.
    CompositionLocalProvider(LocalViewConfiguration provides configuration) {
        Box(
            modifier
                .quvenGlassAnchor(state, stretches = false)
                .graphicsLayer {
                    alpha = if (state.isShown) 0f else 1f
                    scaleX = growth.value
                    scaleY = growth.value
                }
                .drawWithContent {
                    card.record { this@drawWithContent.drawContent() }
                    drawLayer(card)
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
        ) {
            CompositionLocalProvider(LocalViewConfiguration provides system) {
                content()
                if (preview != null) PreviewRecording(own, { ownSize = it }, preview)
            }
        }
    }
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
        preview = remember(preview != null) {
            if (preview != null) GlassMenuPreview(own, { liftedFrom }, ContextMenuGap) { ownSize }
            else GlassMenuPreview(card, { liftedFrom }, ContextMenuGap)
        },
        content = menu,
    )
}

/**
 * The control a context menu lifts out of the screen: the layer it records itself into and the size it lifts from.
 *
 * @property layer The layer the control, or the preview drawn in its place, records itself into.
 * @property liftedFrom Reads the size the control grows from as it lifts, a share of its own; 1 once the menu closes.
 * @property gap The room between the lifted control and its menu.
 * @property ownSize Reads the size of the preview drawn in the control's place, or `null` when the control lifts itself.
 */
internal class GlassMenuPreview(
    val layer: GraphicsLayer,
    val liftedFrom: () -> Float,
    val gap: Dp,
    val ownSize: (() -> IntSize)? = null,
) {

    /**
     * Returns the size of what lifts, unscaled: the preview's own, or the control's.
     *
     * @param anchor The control's bounds.
     * @return The size, in pixels.
     */
    fun imageSize(anchor: Rect): Size = ownSize?.invoke()?.takeIf { it != IntSize.Zero }?.toSize() ?: anchor.size

    /**
     * Returns the share of its size what lifts stands at once the menu is open: a preview at its own size, the control
     * at [controlLift].
     *
     * @param controlLift The share a control lifting itself grows to.
     * @return The share.
     */
    fun lift(controlLift: Float): Float = if (ownSize == null) controlLift else 1f

    /**
     * Returns the bounds of what lifts once the menu is open, centred on the control.
     *
     * @param anchor The control's bounds.
     * @param controlLift The share a control lifting itself grows to.
     * @return The bounds, before any shift.
     */
    fun lifted(anchor: Rect, controlLift: Float): Rect {
        val size = imageSize(anchor) * lift(controlLift)
        return Rect(anchor.center - Offset(size.width / 2f, size.height / 2f), size)
    }

    /**
     * Returns how large what lifts draws, as a share of its own size, [progress] of the way to its menu standing open:
     * from the size the control was held at to [lift].
     *
     * @param progress How far the menu has opened, from 0 to 1, past 1 while its spring overshoots.
     * @param lift The share it lifts to once its menu stands open.
     * @param anchor The control's bounds.
     * @return The share.
     */
    fun scaleAt(progress: Float, lift: Float, anchor: Rect): Float {
        val image = imageSize(anchor)
        val from = if (image.width > 0f) liftedFrom() * anchor.width / image.width else liftedFrom()
        return lerp(from, lift, progress)
    }

    /**
     * Returns how far the lifted control moves so its menu, held inside the screen, stands [gap] beyond it: down from a
     * menu above it, up from one below, as the system moves a card whose menu has no room beside it.
     *
     * @param lifted The lifted control's bounds.
     * @param menu The open menu's bounds.
     * @param gap The room between the two, in pixels.
     * @return The distance to move the control down, negative to move it up.
     */
    fun shift(lifted: Rect, menu: Rect, gap: Float): Float =
        if (menu.center.y < lifted.center.y) (menu.bottom + gap - lifted.top).coerceAtLeast(0f)
        else (menu.top - gap - lifted.bottom).coerceAtMost(0f)

    /**
     * Returns where the menu's glass grows out of the lifted control: a capsule half the control's width on the edge the
     * menu stands beyond, as the system's menu flows out of the card it lifts.
     *
     * @param lifted The lifted control's bounds.
     * @param menu The open menu's bounds, or `null` until they are known.
     * @param spaceHeight The height of the space the menu stands in.
     * @param seedHeight The height of the capsule.
     * @return The capsule's bounds.
     */
    fun seed(lifted: Rect, menu: Rect?, spaceHeight: Float, seedHeight: Float): Rect {
        val width = lifted.width * SeedShare
        val left = lifted.center.x - width / 2f
        val above = if (menu != null) menu.center.y < lifted.center.y else opensAbove(lifted.top, lifted.bottom, spaceHeight)
        val top = if (above) lifted.top else lifted.bottom - seedHeight
        return Rect(left, top, left + width, top + seedHeight)
    }
}

/**
 * Composes [preview] without drawing it, measured at its own size, and records it into [layer] for a context menu to
 * lift.
 *
 * @param layer The layer the preview records into.
 * @param onSize Invoked with the preview's size each time it is measured.
 * @param preview Draws the preview.
 */
@Composable
private fun PreviewRecording(layer: GraphicsLayer, onSize: (IntSize) -> Unit, preview: @Composable () -> Unit) {
    var measured by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                measured = IntSize(placeable.width, placeable.height)
                onSize(measured)
                layout(0, 0) { placeable.place(0, 0) }
            }
            .drawWithContent {
                layer.record(size = measured) { this@drawWithContent.drawContent() }
            }
            // A picture of the card, which accessibility services already read.
            .clearAndSetSemantics {},
    ) { Box(Modifier.clip(PreviewShape)) { preview() } }
}

/** The room between a lifted card and its menu, as measured on the system's. */
private val ContextMenuGap = 22.dp

/** The share of the lifted card's width the menu's glass grows out of. */
private const val SeedShare = 0.5f

/** The height of the capsule a context menu's glass grows out of. */
internal val SeedHeight = 44.dp

/**
 * The view configuration of a context menu's card: the system's, its long press held at least as long as iOS holds a
 * card before lifting it.
 *
 * @param system The system's view configuration.
 */
private class ContextMenuViewConfiguration(private val system: ViewConfiguration) : ViewConfiguration by system {
    override val longPressTimeoutMillis: Long
        get() = maxOf(system.longPressTimeoutMillis, ContextMenuHoldMillis)
}

/** The shape a preview of a card's own lifts in, as an iPad rounds it whatever the preview's own corners. */
private val PreviewShape = RoundedCornerShape(22.dp)

/** How long a card is held before its context menu opens, as measured on an iPad. */
private const val ContextMenuHoldMillis = 500L

/** How much a card grows while it is held, before its context menu opens, as measured on the system's. */
private const val HeldGrowth = 1.06f

/** Where a context menu stands: below its lifted card wherever it fits there, as the system's does, and above otherwise. */
private val ContextMenuPlacement = QuvenGlassMorphPlacement.belowWhereItFits(gap = ContextMenuGap, edge = MenuEdge)
