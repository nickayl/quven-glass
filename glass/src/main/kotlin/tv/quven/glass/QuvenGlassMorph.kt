package tv.quven.glass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
            val left = alignedStart(size, anchor, space)
            val hangs = space.height - anchor.top >= anchor.bottom && anchor.top + size.height <= space.height - inset
            val rises = anchor.bottom - size.height >= inset
            val top = if (hangs || !rises) anchor.top else anchor.bottom - size.height
            IntOffset(left.keptInside(size.width, space.width, inset), top.keptInside(size.height, space.height, inset))
        }

        /**
         * Returns the placement that stands the opened glass above or below the anchor, [gap] away from it on the side
         * with more room, aligned with the anchor's side nearer the space's edge and kept at least [edge] inside the
         * space, as a context menu opens beside the card it lifts.
         *
         * @param gap The room between the glass and the anchor.
         * @param edge The least room between the glass and the space's sides.
         * @return The placement.
         */
        public fun aboveOrBelow(gap: Dp, edge: Dp): QuvenGlassMorphPlacement =
            QuvenGlassMorphPlacement { size, anchor, space, density ->
                val inset = with(density) { edge.roundToPx() }
                val apart = with(density) { gap.roundToPx() }
                val left = alignedStart(size, anchor, space)
                val top = if (opensAbove(anchor.top.toFloat(), anchor.bottom.toFloat(), space.height.toFloat())) {
                    anchor.top - apart - size.height
                } else {
                    anchor.bottom + apart
                }
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

    /** Gets how far the glass has spread across, from 0 to 1, past 1 while its spring overshoots. */
    internal val progress: Animatable<Float, *> = Animatable(0f)

    /** Gets how far the glass has dropped along the way it opens, ahead of [progress], from 0 to 1. */
    internal val reach: Animatable<Float, *> = Animatable(0f)

    /** Gets how far the control stretches as the glass lands back in it, from 0, at rest, to 1 at its longest. */
    internal val landing: Animatable<Float, *> = Animatable(0f)

    /** Gets the bounds of the control the glass grows from, in the window, or `null` while none is marked. */
    internal var anchorInWindow: Rect? by mutableStateOf(null)

    /** Gets a value indicating whether the open glass covers its control and rises above it. */
    internal var rises: Boolean by mutableStateOf(false)

    /** Gets the requester of the control's focus, which a menu opened from the keys hands back as it closes. */
    internal val anchorFocus: FocusRequester = FocusRequester()

    /**
     * Gets a value indicating whether the glass is on screen: opening, open or closing. The control it grows from hides
     * meanwhile, as the glass takes its place; the spring's swing past the closed state still counts as closing.
     */
    public val isShown: Boolean by derivedStateOf {
        progress.isRunning || reach.isRunning || progress.value != 0f || reach.value != 0f
    }
}

/**
 * Marks the control a morph grows from, so a glass that opens elsewhere in the window, as a menu in a
 * [QuvenGlassMenuHost] does, grows out of it, the focus a menu opened from the keys took returns to it, and, where
 * [stretches], the control stretches briefly the way the glass came back as it lands in it.
 *
 * @param state The opening the control belongs to.
 * @param stretches Whether the control stretches as the glass lands back in it, as a button does; a card a context
 * menu lifts does not.
 * @return The decorated modifier.
 */
public fun Modifier.quvenGlassAnchor(state: QuvenGlassMorphState, stretches: Boolean = true): Modifier =
    onGloballyPositioned { state.anchorInWindow = it.boundsInWindow() }
        .focusRequester(state.anchorFocus)
        .then(
            if (!stretches) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    scaleY = 1f + LandingStretch * state.landing.value
                    // A glass that hung below its control comes back up, and stretches it upwards; one that rose, downwards.
                    transformOrigin = if (state.rises) TransformOrigin(0.5f, 0f) else TransformOrigin(0.5f, 1f)
                }
            },
        )

/**
 * Creates and remembers a [QuvenGlassMorphState].
 *
 * @return The state.
 */
@Composable
public fun rememberQuvenGlassMorphState(): QuvenGlassMorphState = remember { QuvenGlassMorphState() }

/**
 * Opens a control into a panel as one piece of glass, as a system menu drops out of its button: the control's glass
 * stays where it stood, still lit by its press as [QuvenGlassStyle.pressGlow] lights it, while the panel's glass falls
 * from it as a drop, lengthening on one spring and spreading on a slower one, and joins it; the control's
 * [face] fades at once, the panel's [content] comes into focus as the glass spreads, and the glass takes the panel's
 * corners last. It closes back into the control on a short, even ease that never swings past it, and the control shows
 * again once [QuvenGlassMorphState.isShown] is `false`. Where motion is reduced, the glass and the panel fade in place.
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
 * @param fromControl Whether the control's own glass stays as a lit cap the panel drops from, as a menu drops out of its
 * button; `false` grows the panel's glass alone, as a context menu flows out of the card it lifts.
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
    fromControl: Boolean = true,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(expanded, reduceMotion) {
        val target = if (expanded) 1f else 0f
        val landing = !expanded && !reduceMotion && state.isShown
        coroutineScope {
            launch { state.progress.animateTo(target, morphSpec(expanded, reduceMotion, SpreadDamping, SpreadStiffness)) }
            launch { state.reach.animateTo(target, morphSpec(expanded, reduceMotion, DropDamping, DropStiffness)) }
        }
        // The control takes the glass's momentum as it lands, and settles without swinging past its own shape.
        if (landing) {
            state.landing.animateTo(0f, spring(dampingRatio = 1f, stiffness = LandingStiffness), initialVelocity = LandingKick)
        }
    }
    val frame = remember { MorphFrame() }
    val opening by rememberUpdatedState(expanded && !reduceMotion)
    // The control's glass carries its press as the drop falls, and lets it go as the glass spreads.
    val carriedPress = remember(state) { GlassLiftSource { if (opening) carriedPress(state.progress.value) else 0f } }
    val joined = backdrop != null && QuvenGlass.isLiquidSupported
    // The glass is as clear as its control's while it drops and spreads, and frosts over as the panel settles.
    val frost by remember(state, reduceMotion) {
        derivedStateOf { if (reduceMotion) 1f else smoothstep(FrostStart, 1f, state.progress.value) }
    }
    val glassStyle = remember(style, frost) { style.frosted(frost) }
    Layout(
        modifier = modifier,
        content = {
            QuvenGlassContainer(
                Modifier.graphicsLayer { alpha = if (reduceMotion) state.progress.value else 1f },
                style = glassStyle,
                spacing = MorphJoin,
                backdrop = backdrop,
            ) {
                // The static material draws each surface apart, so only joined glass keeps the control's.
                if (joined && fromControl) {
                    Box(
                        Modifier
                            .standingAt { frame.source }
                            .liquidGlass(
                                backdrop, glassStyle, CircleShape, null, reduceMotion = false, lift = carriedPress, pill = null,
                                adapts = false,
                            ),
                    )
                }
                // The panel's glass never turns light.
                Box(
                    Modifier
                        .standingAt { frame.body }
                        .liquidGlass(
                            backdrop, glassStyle, BodyShape(frame), null, reduceMotion = false, lift = null, pill = null, adapts = false,
                        ),
                )
            }
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
                    .drawWithContent { frame.clipToBody(this) { drawContent() } },
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
        // A glass rises when it ends at its control's foot; one held inside the space past its control does not.
        state.rises = open.top < anchor.top && abs(open.bottom - anchor.bottom) < 1f
        frame.update(
            if (reduceMotion) MorphGeometry.settled(open, cornerRadius.toPx())
            else morphGeometry(anchor, open, state.progress.value, state.reach.value, cornerRadius.toPx()),
            open,
        )
        val glassPlaceable = measurables[0].measure(Constraints.fixed(space.width, space.height))
        val facePlaceable = measurables[1].measure(Constraints.fixed(anchor.width.roundToInt(), anchor.height.roundToInt()))
        layout(space.width, space.height) {
            glassPlaceable.place(0, 0)
            facePlaceable.place(anchor.left.roundToInt(), anchor.top.roundToInt())
            panel.place(target)
        }
    }
}

/** The radius of an open morph's corners, unless the caller names another. */
public val DefaultMorphCornerRadius: Dp = 24.dp

/**
 * Where the morph's two glass surfaces stand at this frame, written by the layout and read, observed, by the surfaces'
 * placement and shape and by the panel's clip, so all of them follow the glass as it grows.
 */
private class MorphFrame {

    /** Gets the control's glass at this frame. */
    var source: Rect by mutableStateOf(Rect.Zero)
        private set

    /** Gets the panel's glass at this frame. */
    var body: Rect by mutableStateOf(Rect.Zero)
        private set

    /** Gets the radius of the panel's glass's corners at this frame, in pixels. */
    var bodyRadius: Float by mutableFloatStateOf(0f)
        private set

    private var open: Rect = Rect.Zero
    private val clip = Path()

    /**
     * Records the glass at this frame.
     *
     * @param geometry The two surfaces.
     * @param open The bounds the panel is laid out at, where it stands open.
     */
    fun update(geometry: MorphGeometry, open: Rect) {
        source = geometry.source
        body = geometry.body
        bodyRadius = geometry.bodyRadius
        this.open = open
    }

    /**
     * Runs [block], which draws the panel laid out where it stands open, clipped to the panel's glass as it grows.
     *
     * @param scope The panel's drawing scope.
     * @param block Draws the panel.
     */
    fun clipToBody(scope: ContentDrawScope, block: () -> Unit) {
        clip.rewind()
        clip.addOutline(Outline.Rounded(RoundRect(body.translate(-open.topLeft), CornerRadius(bodyRadius))))
        with(scope) { clipPath(clip) { block() } }
    }
}

/** The rounded shape the panel's glass takes at this frame. */
private class BodyShape(private val frame: MorphFrame) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(
            RoundRect(Rect(Offset.Zero, size), CornerRadius(frame.bodyRadius.coerceAtMost(size.minDimension / 2f))),
        )
}

/**
 * The two surfaces of a morph's glass at one frame: the control's own glass and the panel's, which the glass draws
 * joined.
 *
 * @property source The control's glass, a circle that shrinks into the panel's glass as it comes to rest.
 * @property body The panel's glass.
 * @property bodyRadius The radius of the panel's glass's corners, in pixels.
 */
internal class MorphGeometry(val source: Rect, val body: Rect, val bodyRadius: Float) {
    companion object {
        /**
         * Returns the geometry of a glass that stands open as [open], with no control's glass left.
         *
         * @param open The open panel's bounds.
         * @param cornerRadius The open panel's corner radius, in pixels.
         * @return The geometry.
         */
        fun settled(open: Rect, cornerRadius: Float): MorphGeometry =
            MorphGeometry(Rect(open.center, Size.Zero), open, cornerRadius.coerceAtMost(open.minDimension / 2f))
    }
}

/**
 * Returns where a morph's glass stands as it opens from [anchor] into [open], as a system menu drops out of its button:
 * the control's glass stays as a cap at its near end, and the panel's glass falls out of the control's middle, its
 * far edge travelling with [reach], its sides with [spread], and its near edge leaving the control only as the glass
 * finishes spreading, when the cap shrinks into it.
 *
 * @param anchor The control's bounds.
 * @param open The open panel's bounds.
 * @param spread How far the glass has spread across, from 0 to 1, past 1 while its spring overshoots.
 * @param reach How far the glass has dropped along the way it opens, from 0 to 1, past 1 while its spring overshoots.
 * @param cornerRadius The open panel's corner radius, in pixels.
 * @return The geometry.
 */
internal fun morphGeometry(anchor: Rect, open: Rect, spread: Float, reach: Float, cornerRadius: Float): MorphGeometry {
    val settle = smoothstep(NearEdgeStart, 1f, spread)
    val hangs = abs(open.top - anchor.top) <= abs(open.bottom - anchor.bottom)
    val top: Float
    val bottom: Float
    if (hangs) {
        bottom = lerp(anchor.bottom, open.bottom, reach)
        top = lerp(anchor.center.y, open.top, settle).coerceAtMost(bottom)
    } else {
        top = lerp(anchor.top, open.top, reach)
        bottom = lerp(anchor.center.y, open.bottom, settle).coerceAtLeast(top)
    }
    // The drop is born narrower than the control, in its middle, so the cap and the drop join inside its outline,
    // and swells to the control's width as soon as it falls.
    val seed = anchor.width * lerp(BodySeedShare, 1f, smoothstep(0f, BodySwellEnd, spread)) / 2f
    val body = Rect(
        lerp(anchor.center.x - seed, open.left, spread),
        top,
        lerp(anchor.center.x + seed, open.right, spread),
        bottom,
    )
    // The cap keeps the control's near edge, then sinks into the panel's glass, away from its edge, as it fades.
    val fade = smoothstep(SourceFadeStart, SourceFadeEnd, spread)
    val kept = lerp(SourceRelease, SourceCap, smoothstep(0f, SourceSqueezeEnd, spread)) * (1f - fade)
    val capSize = Size(anchor.width * kept, anchor.height * kept)
    val pinnedTop = if (hangs) anchor.top else anchor.bottom - capSize.height
    val capTop = lerp(pinnedTop, anchor.center.y - capSize.height / 2f, fade)
    val source = Rect(Offset(anchor.center.x - capSize.width / 2f, capTop), capSize)
    return MorphGeometry(source, body, morphRadius(body.size, cornerRadius, spread))
}

/**
 * Returns this material as the morph's glass draws it: as clear as a control's while [frost] is 0, the thick tone, the
 * blur and the tint of the material once it reaches 1.
 *
 * @param frost How far the glass has frosted over, from 0 to 1.
 * @return The material.
 */
private fun QuvenGlassStyle.frosted(frost: Float): QuvenGlassStyle = if (frost >= 1f) this else copy(
    thickTone = QuvenGlassTone(
        shade = lerpColor(thinTone.shade, thickTone.shade, frost),
        lean = lerp(thinTone.lean, thickTone.lean, frost),
        leanSlope = lerp(thinTone.leanSlope, thickTone.leanSlope, frost),
        saturation = lerp(thinTone.saturation, thickTone.saturation, frost),
    ),
    blur = lerp(ClearBlur.value, blur.value, frost).dp,
    tint = tint.copy(alpha = tint.alpha * frost),
)

private fun morphSpec(expanded: Boolean, reduceMotion: Boolean, damping: Float, stiffness: Float): AnimationSpec<Float> =
    when {
        reduceMotion -> tween(ReducedMotionFadeMillis)
        expanded -> spring(damping, stiffness)
        else -> tween(CloseMillis, easing = CloseEasing)
    }

/**
 * Returns whether glass placed beside an anchor stands above it: where there is more room above the anchor than below.
 *
 * @param top The anchor's top.
 * @param bottom The anchor's bottom.
 * @param height The height of the space the glass stands in.
 * @return `true` if the glass stands above the anchor; otherwise, `false`.
 */
internal fun opensAbove(top: Float, bottom: Float, height: Float): Boolean = top > height - bottom

// Aligns the glass with the anchor's side nearer the space's edge: its start on the left half, its end on the right.
private fun alignedStart(size: IntSize, anchor: IntRect, space: IntSize): Int =
    if (anchor.center.x > space.width / 2) anchor.right - size.width else anchor.left

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
    return lerp(capsule, cornerRadius.coerceAtMost(capsule), smoothstep(CornerSettleStart, 1f, progress))
}

private const val SpreadDamping = 0.72f
private const val SpreadStiffness = 300f
private const val DropDamping = 0.68f
private const val DropStiffness = 380f
private val MorphJoin = 48.dp
private const val NearEdgeStart = 0.7f
private const val BodySeedShare = 0.6f
private const val BodySwellEnd = 0.15f
private const val SourceRelease = 0.85f
private const val SourceCap = 0.8f
private const val SourceSqueezeEnd = 0.5f
private const val SourceFadeStart = 0.6f
private const val SourceFadeEnd = 0.85f
private const val FrostStart = 0.75f
private const val LandingStretch = 0.07f
private const val LandingStiffness = 156f
private const val LandingKick = 34f
private val ClearBlur = QuvenGlassStyle.Standard.blur
private const val CloseMillis = 165
private val CloseEasing = CubicBezierEasing(0.1f, 0f, 1f, 1f)
private const val CornerSettleStart = 0.6f
private val ContentBlur = 10.dp
private const val MinContentBlur = 0.5f
private const val FaceFadeRate = 5f
private const val PressFadeRate = 2f
private const val ContentFadeStart = 0.4f
