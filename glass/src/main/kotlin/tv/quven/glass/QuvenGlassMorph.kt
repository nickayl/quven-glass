package tv.quven.glass

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.util.lerp
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
                    left(size, anchor).coerceIn(inset, maxOf(inset, space.width - inset - size.width)),
                    anchor.top.coerceIn(inset, maxOf(inset, space.height - inset - size.height)),
                )
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
            val x = (anchor.center.x - size.width / 2f).roundToInt().coerceIn(inset, maxOf(inset, space.width - inset - size.width))
            val y = (anchor.top - with(density) { gap.roundToPx() } - size.height).coerceAtLeast(0)
            IntOffset(x, y)
        }
    }
}

/** The opening of a [QuvenGlassMorph]: whether the glass stands open, on its way or closed back into its control. */
@Stable
public class QuvenGlassMorphState {

    /** Gets how far the glass is open, from 0 to 1. */
    internal val progress: Animatable<Float, *> = Animatable(0f)

    /**
     * Gets a value indicating whether the glass is on screen: opening, open or closing. The control it grows from hides
     * meanwhile, as the glass takes its place; the spring's swing past the closed state still counts as closing.
     */
    public val isShown: Boolean by derivedStateOf { progress.isRunning || progress.value != 0f }
}

/**
 * Creates and remembers a [QuvenGlassMorphState].
 *
 * @return The state.
 */
@Composable
public fun rememberQuvenGlassMorphState(): QuvenGlassMorphState = remember { QuvenGlassMorphState() }

/**
 * Opens a control into a panel as one piece of glass: on a spring the glass grows from the control's bounds to the
 * panel's, the control's [face] fading first and the panel's [content] last, and closes the same way back; the control
 * shows again once [QuvenGlassMorphState.isShown] is `false`. Where motion is reduced, the glass and the panel fade in
 * place.
 *
 * The morph fills its parent, in whose coordinates [anchor] and [placement] are measured, and handles no dismissal.
 * The panel stays composed while closed, outside the semantics tree.
 *
 * @param state The opening, which the control reads to hide while the glass is shown.
 * @param expanded Whether the panel is open.
 * @param anchor The bounds of the control the glass grows from, in the morph's coordinates.
 * @param width The open panel's width; its height is its content's.
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
    width: Dp,
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
        val spec = if (reduceMotion) tween<Float>(ReducedMotionFadeMillis) else spring(MorphDamping, MorphStiffness)
        state.progress.animateTo(if (expanded) 1f else 0f, spec)
    }
    val frame = remember { MorphFrame() }
    val shape = remember(frame) { MorphShape(frame) }
    Layout(
        modifier = modifier,
        content = {
            Box(
                Modifier
                    .graphicsLayer { alpha = if (reduceMotion) state.progress.value else 1f }
                    // The glass opens into a panel, thick glass that never turns light.
                    .liquidGlass(
                        backdrop, style, shape, interactionSource = null, reduceMotion = false, lift = null, pill = null,
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
                    .graphicsLayer { alpha = contentAlpha(state.progress.value) }
                    .drawWithContent { frame.clipToGlass(this) { drawContent() } },
            ) { content() }
        },
    ) { measurables, constraints ->
        val space = IntSize(constraints.maxWidth, constraints.maxHeight)
        val panel = measurables[2].measure(Constraints(minWidth = width.roundToPx(), maxWidth = width.roundToPx(), maxHeight = space.height))
        if (!expanded && !state.isShown) return@Layout layout(space.width, space.height) {}
        val target = placement.place(IntSize(panel.width, panel.height), anchor.toIntRectRounded(), space, this)
        val open = Rect(Offset(target.x.toFloat(), target.y.toFloat()), Size(panel.width.toFloat(), panel.height.toFloat()))
        val progress = state.progress.value
        val glass = if (reduceMotion) open else lerp(anchor, open, progress)
        frame.update(glass, open, lerp(anchor.minDimension / 2f, cornerRadius.toPx(), if (reduceMotion) 1f else progress))
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

private fun Rect.toIntRectRounded(): IntRect = IntRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

private fun faceAlpha(progress: Float): Float = (1f - progress * FaceFadeRate).coerceIn(0f, 1f)

private fun contentAlpha(progress: Float): Float = ((progress - ContentFadeStart) / (1f - ContentFadeStart)).coerceIn(0f, 1f)

private const val MorphDamping = 0.78f
private const val MorphStiffness = 360f
private const val FaceFadeRate = 3f
private const val ContentFadeStart = 0.45f
