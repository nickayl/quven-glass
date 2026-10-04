package tv.quven.glass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt

/**
 * Where an opened [QuvenGlassMorph] comes to rest, given the size its content asks for, the anchor it grows from and
 * the space it stands in, all in pixels.
 */
public fun interface QuvenGlassMorphPlacement {

    /**
     * Returns the top-start corner of the opened glass.
     *
     * @param size The opened glass's size.
     * @param anchor The bounds of the control the glass grows from.
     * @param space The size of the space the glass stands in.
     * @param density The density the placement is measured at.
     * @return The opened glass's top-start corner.
     */
    public fun place(size: IntSize, anchor: IntRect, space: IntSize, density: Density): IntOffset

    public companion object {
        /**
         * Returns the placement that hangs the opened glass from the anchor's top-right corner, as a menu opens from a
         * button in the window's top-right corner, kept at least [edge] inside the space.
         *
         * @param edge The least room between the glass and the space's sides.
         * @return The placement.
         */
        public fun hangingFromTopRight(edge: Dp = 0.dp): QuvenGlassMorphPlacement = hangingFromTop(edge) { size, anchor -> anchor.right - size.width }

        /**
         * Returns the placement that hangs the opened glass from the anchor's top-left corner, as a menu opens from a
         * button in the window's top-left corner, kept at least [edge] inside the space.
         *
         * @param edge The least room between the glass and the space's sides.
         * @return The placement.
         */
        public fun hangingFromTopLeft(edge: Dp = 0.dp): QuvenGlassMorphPlacement = hangingFromTop(edge) { _, anchor -> anchor.left }

        private fun hangingFromTop(edge: Dp, left: (IntSize, IntRect) -> Int): QuvenGlassMorphPlacement =
            QuvenGlassMorphPlacement { size, anchor, space, density ->
                val inset = with(density) { edge.roundToPx() }
                IntOffset(
                    left(size, anchor).keptInside(size.width, space.width, inset),
                    anchor.top.keptInside(size.height, space.height, inset),
                )
            }

        /**
         * Returns the placement that stands the opened glass over its anchor, as a system menu opens from its button:
         * aligned with the anchor's side nearer the space's edge, hanging from the anchor's top where there is at
         * least as much room below the anchor as above it and the glass fits, and otherwise rising from the anchor's
         * foot, kept at least [edge] inside the space.
         *
         * @param edge The least room between the glass and the space's sides.
         * @return The placement.
         */
        public fun overAnchor(edge: Dp = 0.dp): QuvenGlassMorphPlacement = QuvenGlassMorphPlacement { size, anchor, space, density ->
            val inset = with(density) { edge.roundToPx() }
            val left = if (anchor.center.x > space.width / 2) anchor.right - size.width else anchor.left
            val hangs = space.height - anchor.top >= anchor.bottom && anchor.top + size.height <= space.height - inset
            val rises = anchor.bottom - size.height >= inset
            val top = if (hangs || !rises) anchor.top else anchor.bottom - size.height
            IntOffset(left.keptInside(size.width, space.width, inset), top.keptInside(size.height, space.height, inset))
        }

        /**
         * Returns the placement that stands the opened glass above the anchor, centred on it, [gap] above it and at least
         * [edge] inside the space's sides, as a menu opens from an entry of a bar at the foot of the window.
         *
         * @param gap The room between the glass and the anchor.
         * @param edge The least room between the glass and the space's sides.
         * @return The placement.
         */
        public fun above(gap: Dp, edge: Dp): QuvenGlassMorphPlacement = QuvenGlassMorphPlacement { size, anchor, space, density ->
            val inset = with(density) { edge.roundToPx() }
            val x = (anchor.center.x - size.width / 2f).roundToInt().keptInside(size.width, space.width, inset)
            val y = (anchor.top - with(density) { gap.roundToPx() } - size.height).coerceAtLeast(0)
            IntOffset(x, y)
        }
    }
}

/**
 * The opening of a [QuvenGlassMorph]: whether the glass stands open, on its way or closed back into its control, and
 * where that control stands once [quvenGlassAnchor] marks it.
 */
@Stable
public class QuvenGlassMorphState {

    /** Gets how far the glass is open, from 0 to 1. */
    internal val progress: Animatable<Float, *> = Animatable(0f)

    /** Gets the bounds of the control the glass grows from, in the window, or `null` while none is marked. */
    internal var anchorInWindow: Rect? by mutableStateOf(null)

    /** Gets a value indicating whether the open glass covers its control and rises above it. */
    internal var rises: Boolean by mutableStateOf(false)

    /**
     * Gets a value indicating whether the glass is on screen: opening, open or closing. The control it grows from hides
     * meanwhile, as the glass takes its place; the spring's swing past the closed state still counts as closing.
     */
    public val isShown: Boolean by derivedStateOf { progress.isRunning || progress.value != 0f }
}

/**
 * Marks the control a morph grows from, so a glass that opens elsewhere in the window, as a menu in a
 * [QuvenGlassMenuHost] does, grows out of it.
 *
 * @param state The opening the control belongs to.
 * @return The decorated modifier.
 */
public fun Modifier.quvenGlassAnchor(state: QuvenGlassMorphState): Modifier =
    onGloballyPositioned { state.anchorInWindow = it.boundsInWindow() }

/**
 * Creates and remembers a [QuvenGlassMorphState].
 *
 * @return The state.
 */
@Composable
public fun rememberQuvenGlassMorphState(): QuvenGlassMorphState = remember { QuvenGlassMorphState() }

/**
 * Opens a control into a panel as one piece of glass: on a spring the glass grows from the control's bounds to the
 * panel's, a capsule until it comes to rest, still lit by the control's press as [QuvenGlassStyle.pressGlow] lights
 * it; the control's [face] fades first and the panel's [content] comes into focus last. It closes back into the
 * control on a short, even ease that never swings past it, and the control shows again once
 * [QuvenGlassMorphState.isShown] is `false`. Where motion is reduced, the glass and the panel fade in place.
 *
 * The morph fills its parent, in whose coordinates [anchor] and [placement] are measured, and handles no dismissal.
 * The panel stays composed while closed, outside the semantics tree.
 *
 * @param state The opening, which the control reads to hide while the glass is shown.
 * @param expanded Whether the panel is open.
 * @param anchor The bounds of the control the glass grows from, in the morph's coordinates.
 * @param width The open panel's width, or [Dp.Unspecified] for its content's own; its height is its content's.
 * @param placement Where the open panel stands.
 * @param modifier Modifier applied to the morph, which fills its parent.
 * @param style The material.
 * @param cornerRadius The radius of the open panel's corners; the closed glass takes the control's, half its shorter
 * side.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 * @param face Draws the control's own face inside the glass while it starts to grow.
 * @param content Draws the open panel.
 */
@Composable
public fun QuvenGlassMorph(
    state: QuvenGlassMorphState,
    expanded: Boolean,
    anchor: Rect,
    width: Dp = Dp.Unspecified,
    placement: QuvenGlassMorphPlacement,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    cornerRadius: Dp = DefaultMorphCornerRadius,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
    face: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    LaunchedEffect(expanded, reduceMotion) {
        val spec: AnimationSpec<Float> = when {
            reduceMotion -> tween(ReducedMotionFadeMillis)
            expanded -> spring(MorphDamping, MorphStiffness)
            else -> tween(CloseMillis, easing = CloseEasing)
        }
        state.progress.animateTo(if (expanded) 1f else 0f, spec)
    }
    val frame = remember { MorphFrame() }
    val shape = remember(frame) { MorphShape(frame) }
    val opening by rememberUpdatedState(expanded && !reduceMotion)
    // The glass carries the control's press as it starts to grow, and lets it go as it takes the panel's shape.
    val carriedPress = remember(state) { GlassLiftSource { if (opening) carriedPress(state.progress.value) else 0f } }
    Layout(
        modifier = modifier,
        content = {
            Box(
                Modifier
                    .graphicsLayer { alpha = if (reduceMotion) state.progress.value else 1f }
                    // The glass opens into a panel, thick glass that never turns light.
                    .liquidGlass(
                        backdrop, style, shape, interactionSource = null, reduceMotion = false, lift = carriedPress, pill = null,
                        adapts = false,
                    ),
            )
            // The face doubles the control, which keeps the semantics; it exists only while the glass stands in for it.
            Box(Modifier.clearAndSetSemantics {}.graphicsLayer { alpha = faceAlpha(state.progress.value) }) {
                if (expanded || state.isShown) face()
            }
            Box(
                Modifier
                    .then(if (expanded) Modifier else Modifier.clearAndSetSemantics {})
                    .graphicsLayer {
                        val shown = contentAlpha(state.progress.value)
                        alpha = shown
                        renderEffect = contentBlur((1f - shown) * ContentBlur.toPx())
                    }
                    .drawWithContent { frame.clipToGlass(this) { drawContent() } },
            ) { content() }
        },
    ) { measurables, constraints ->
        val space = IntSize(constraints.maxWidth, constraints.maxHeight)
        val panelConstraints = if (width.isSpecified) {
            Constraints(minWidth = width.roundToPx(), maxWidth = width.roundToPx(), maxHeight = space.height)
        } else {
            Constraints(maxWidth = space.width, maxHeight = space.height)
        }
        val panel = measurables[2].measure(panelConstraints)
        if (!expanded && !state.isShown) return@Layout layout(space.width, space.height) {}
        val target = placement.place(IntSize(panel.width, panel.height), anchor.toIntRectRounded(), space, this)
        val open = Rect(Offset(target.x.toFloat(), target.y.toFloat()), Size(panel.width.toFloat(), panel.height.toFloat()))
        state.rises = open.top < anchor.top && open.bottom >= anchor.bottom
        val progress = state.progress.value
        val glass = if (reduceMotion) open else lerp(anchor, open, progress)
        frame.update(glass, open, if (reduceMotion) cornerRadius.toPx() else morphRadius(glass.size, cornerRadius.toPx(), progress))
        val glassPlaceable = measurables[0].measure(Constraints.fixed(glass.width.roundToInt().coerceAtLeast(0), glass.height.roundToInt().coerceAtLeast(0)))
        val facePlaceable = measurables[1].measure(Constraints.fixed(anchor.width.roundToInt(), anchor.height.roundToInt()))
        layout(space.width, space.height) {
            glassPlaceable.place(glass.left.roundToInt(), glass.top.roundToInt())
            facePlaceable.place(anchor.left.roundToInt(), anchor.top.roundToInt())
            panel.place(target)
        }
    }
}

/** The radius of an open morph's corners, unless the caller names another. */
public val DefaultMorphCornerRadius: Dp = 24.dp

/**
 * Where the morph's glass stands at this frame, written by the layout and read, observed, by the glass's shape and the
 * panel's clip, so both redraw as the glass grows.
 */
private class MorphFrame {
    var glass: Rect by mutableStateOf(Rect.Zero)
        private set
    var open: Rect by mutableStateOf(Rect.Zero)
        private set
    var radius: Float by mutableFloatStateOf(0f)
        private set
    private val clip = Path()

    fun update(glass: Rect, open: Rect, radius: Float) {
        this.glass = glass
        this.open = open
        this.radius = radius
    }

    // The panel is laid out where it stands open; it shows only inside the glass as it grows.
    fun clipToGlass(scope: ContentDrawScope, block: () -> Unit) {
        clip.rewind()
        clip.addOutline(Outline.Rounded(RoundRect(glass.translate(-open.topLeft), CornerRadius(radius))))
        with(scope) { clipPath(clip) { block() } }
    }
}

/** The rounded shape the morph's glass takes at this frame. */
private class MorphShape(private val frame: MorphFrame) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(frame.radius.coerceAtMost(size.minDimension / 2f))))
}

// Moves a start along one axis so the extent it begins stands inside the space, or at the inset where it cannot.
private fun Int.keptInside(extent: Int, space: Int, inset: Int): Int = coerceIn(inset, maxOf(inset, space - inset - extent))

private fun Rect.toIntRectRounded(): IntRect = IntRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

private fun faceAlpha(progress: Float): Float = (1f - progress * FaceFadeRate).coerceIn(0f, 1f)

private fun carriedPress(progress: Float): Float = (1f - progress * PressFadeRate).coerceIn(0f, 1f)

private fun contentAlpha(progress: Float): Float = ((progress - ContentFadeStart) / (1f - ContentFadeStart)).coerceIn(0f, 1f)

// The panel's content comes into focus as it fades in; blurring needs Android 12.
private fun contentBlur(radius: Float): RenderEffect? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > MinContentBlur) BlurEffect(radius, radius, TileMode.Decal) else null

/**
 * Returns the corner radius of the morph's glass at [progress]: a capsule while it grows, as liquid does, settling to
 * [cornerRadius] only as it comes to rest.
 *
 * @param glass The glass's size at this frame.
 * @param cornerRadius The open panel's corner radius, in pixels.
 * @param progress How far the morph has opened, from 0 to 1, past 1 while the spring overshoots.
 * @return The radius, in pixels.
 */
internal fun morphRadius(glass: Size, cornerRadius: Float, progress: Float): Float {
    val capsule = glass.minDimension / 2f
    val settled = ((progress - CornerSettleStart) / (1f - CornerSettleStart)).coerceIn(0f, 1f)
    val eased = settled * settled * (3f - 2f * settled)
    return capsule + (cornerRadius.coerceAtMost(capsule) - capsule) * eased
}

private const val MorphDamping = 0.72f
private const val MorphStiffness = 580f
private const val CloseMillis = 165
private val CloseEasing = CubicBezierEasing(0.1f, 0f, 1f, 1f)
private const val CornerSettleStart = 0.6f
private val ContentBlur = 10.dp
private const val MinContentBlur = 0.5f
private const val FaceFadeRate = 3f
private const val PressFadeRate = 2f
private const val ContentFadeStart = 0.25f
