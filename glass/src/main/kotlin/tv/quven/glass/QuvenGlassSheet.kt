package tv.quven.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The heights a [QuvenGlassSheet] rests at. */
public enum class QuvenGlassSheetDetent {
    /** About half the window, the sheet floating on clear glass inside the window's edges. */
    Medium,

    /** The window's height below its top inset, the sheet opaque and edge to edge. */
    Large,
}

/**
 * The sheets a screen raises: one stands at a time, in the [QuvenGlassSheetHost] it is given to, while every
 * [QuvenGlassSheet] under [LocalQuvenGlassSheetHost] raises its own.
 */
@Stable
public class QuvenGlassSheetHostState internal constructor() {

    /** Gets the sheet standing or leaving. */
    internal val slot: PresentationSlot<SheetRequest> = PresentationSlot()
}

/**
 * Creates and remembers a [QuvenGlassSheetHostState].
 *
 * @return The state.
 */
@Composable
public fun rememberQuvenGlassSheetHostState(): QuvenGlassSheetHostState = remember { QuvenGlassSheetHostState() }

/** Gets the sheets the screen's [QuvenGlassSheet]s raise, or `null` where no host is provided. */
public val LocalQuvenGlassSheetHost: ProvidableCompositionLocal<QuvenGlassSheetHostState?> = staticCompositionLocalOf { null }

/**
 * Raises a sheet from the bottom edge of the window, as Apple's sheets rise: at about half the window it floats on
 * glass inside the window's edges over the dimmed screen, and drawn up to full height it turns opaque and reaches the
 * edges. It follows a drag, from its grabber or from content scrolled to its top, rests at the nearest of [detents],
 * and a drag or a fling below the lowest closes it, as do a press on the dimmed screen and Back, all by invoking
 * [onDismissRequest]. It stands while this is drawn and slides away once it is not.
 *
 * @param onDismissRequest Invoked when the viewer closes the sheet.
 * @param modifier Modifier applied to the sheet's content.
 * @param detents The heights the sheet rests at; it opens at the first.
 * @param content The sheet's content, laid out below its grabber.
 * @throws IllegalStateException No [QuvenGlassSheetHost] is provided above the sheet, or [detents] is empty.
 */
@Composable
public fun QuvenGlassSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    detents: List<QuvenGlassSheetDetent> = DefaultDetents,
    content: @Composable ColumnScope.() -> Unit,
) {
    check(detents.isNotEmpty()) { "A QuvenGlassSheet needs at least one detent." }
    val host = checkNotNull(LocalQuvenGlassSheetHost.current) { "A QuvenGlassSheet needs a QuvenGlassSheetHost above it." }
    val request = remember { SheetRequest() }
    SideEffect { request.update(onDismissRequest, modifier, detents, content) }
    PresentWhileComposed(host.slot, request)
}

/**
 * Draws the sheet a screen has raised above everything drawn before it, over a veil that dims the screen. Place it last
 * in the screen's root, where it fills the window, and provide [state] through [LocalQuvenGlassSheetHost] to the
 * content.
 *
 * @param state The sheets the screen raises.
 * @param modifier Modifier applied to the host, which fills its parent.
 * @param style The material of the sheet's glass.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced: the sheet then moves on short, even slides.
 */
@Composable
public fun QuvenGlassSheetHost(
    state: QuvenGlassSheetHostState,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
) {
    val request = state.slot.shown ?: return
    val density = LocalDensity.current
    val topInset = WindowInsets.safeDrawing.getTop(density).toFloat()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val height = constraints.maxHeight.toFloat()
        val geometry = SheetGeometry(height, topInset)
        val scope = rememberCoroutineScope()
        val motion = remember(request) { SheetMotion(height, scope) }
        val rests = request.detents.map(geometry::topOf)
        LaunchedEffect(request, request.standing) {
            if (request.standing) {
                motion.moveTo(rests.first(), 0f, reduceMotion)
            } else {
                motion.moveTo(height, 0f, reduceMotion).join()
                state.slot.release(request)
            }
        }
        if (request.standing) BackHandler(onBack = request.onDismissRequest)
        val release: (Float) -> Unit = { velocity ->
            val target = motion.restFor(velocity, rests, height)
            // A sheet flung closed asks its owner to close it, and returns to rest should the owner keep it.
            if (target == null) request.onDismissRequest()
            motion.moveTo(target ?: rests.first(), velocity, reduceMotion)
        }
        val drag = rememberDraggableState { delta -> motion.dragBy(delta, rests.min()) }
        val nested = remember(motion, rests) { SheetScrollConnection(motion, rests.min(), release) }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = geometry.dimAt(motion.top)) }
                .pointerInput(request) { keepPresses(request.onDismissRequest) },
        )
        SheetLayout(geometry = geometry, top = { motion.top }, maxWidth = constraints.maxWidth) {
            Box(
                Modifier
                    .liquidGlass(
                        backdrop = backdrop,
                        style = remember(style) { style.forMenus().dimmed(ScreenDim) },
                        shape = SheetShape,
                        interactionSource = null,
                        reduceMotion = reduceMotion,
                        lift = null,
                        pill = null,
                        adapts = false,
                    )
                    .drawWithContent {
                        drawOutline(SheetShape.createOutline(size, layoutDirection, this), OpaqueSheet, alpha = geometry.opacityAt(motion.top))
                        drawContent()
                    }
                    .clip(SheetShape)
                    .nestedScroll(nested)
                    .draggable(drag, Orientation.Vertical, onDragStopped = { velocity -> release(velocity) }),
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = GrabberTop)
                        .size(GrabberSize.first, GrabberSize.second)
                        .background(GrabberColor, CircleShape),
                )
                Column(
                    request.modifier
                        .fillMaxWidth()
                        .padding(top = ContentTop)
                        .navigationBarsPadding(),
                    content = request.content,
                )
            }
        }
    }
}

/** A sheet raised in a [QuvenGlassSheetHost]: what [QuvenGlassSheet] last drew, kept while it slides away. */
@Stable
internal class SheetRequest : GlassPresentation() {

    /** Gets what closing the sheet invokes. */
    var onDismissRequest: () -> Unit by mutableStateOf({})
        private set

    /** Gets the modifier applied to the sheet's content. */
    var modifier: Modifier by mutableStateOf(Modifier)
        private set

    /** Gets the heights the sheet rests at, the first the one it opens at. */
    var detents: List<QuvenGlassSheetDetent> by mutableStateOf(DefaultDetents)
        private set

    /** Gets the sheet's content. */
    var content: @Composable ColumnScope.() -> Unit by mutableStateOf({})
        private set

    /**
     * Takes what the sheet's owner draws now.
     *
     * @param onDismissRequest What closing the sheet invokes.
     * @param modifier The modifier applied to the sheet's content.
     * @param detents The heights the sheet rests at.
     * @param content The sheet's content.
     */
    fun update(
        onDismissRequest: () -> Unit,
        modifier: Modifier,
        detents: List<QuvenGlassSheetDetent>,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        this.onDismissRequest = onDismissRequest
        this.modifier = modifier
        this.detents = detents
        this.content = content
    }
}

/**
 * Where a sheet stands in a window [height] tall below a top inset of [topInset], and how it looks there: the higher it
 * stands between half the window and full height, the nearer the edges it reaches and the more opaque it turns.
 *
 * @property height The window's height, in pixels.
 * @property topInset The window's top inset, in pixels.
 */
internal class SheetGeometry(val height: Float, val topInset: Float) {

    private val mediumTop = height * (1f - MediumShare)

    /**
     * Returns the top of a sheet resting at [detent].
     *
     * @param detent The detent.
     * @return The top, in pixels from the window's top.
     */
    fun topOf(detent: QuvenGlassSheetDetent): Float = when (detent) {
        QuvenGlassSheetDetent.Medium -> mediumTop
        QuvenGlassSheetDetent.Large -> topInset
    }

    /**
     * Returns how far a sheet whose top stands at [top] has been drawn from half the window to full height.
     *
     * @param top The sheet's top, in pixels.
     * @return The share, from 0 at half the window or lower to 1 at full height.
     */
    fun expansionAt(top: Float): Float = ((mediumTop - top) / (mediumTop - topInset)).coerceIn(0f, 1f)

    /**
     * Returns the share of the opaque surface laid over the sheet's glass where its top stands at [top].
     *
     * @param top The sheet's top, in pixels.
     * @return The share, from 0 to 1.
     */
    fun opacityAt(top: Float): Float = smoothstep(OpaqueStart, OpaqueEnd, expansionAt(top))

    /**
     * Returns the share of black laid over the screen behind a sheet whose top stands at [top]: it dims as the sheet
     * rises to half the window and darkens further on the way to full height.
     *
     * @param top The sheet's top, in pixels.
     * @return The share, from 0 to 1.
     */
    fun dimAt(top: Float): Float {
        val risen = ((height - top) / (height - mediumTop)).coerceIn(0f, 1f)
        return risen * lerp(ScreenDim, LargeDim, expansionAt(top))
    }

    /**
     * Returns how far inside the window's sides and bottom a sheet whose top stands at [top] floats, as a share of
     * [inset], the gap it keeps at half the window.
     *
     * @param top The sheet's top, in pixels.
     * @param inset The gap at half the window, in pixels.
     * @return The gap, in pixels.
     */
    fun insetAt(top: Float, inset: Float): Float = inset * (1f - expansionAt(top))
}

/**
 * Where a sheet's top stands and how it moves: it follows a drag, resisting past its highest rest, and comes to rest
 * on a spring that a new drag stops at once.
 *
 * @param height The window's height, in pixels, where a closed sheet stands.
 * @param scope The scope the sheet moves in.
 */
@Stable
internal class SheetMotion(height: Float, private val scope: CoroutineScope) {

    private var moving: Job? = null

    /** Gets the sheet's top, in pixels from the window's top. */
    var top: Float by mutableFloatStateOf(height)
        private set

    /**
     * Moves the sheet's top by [delta], a third as far past [highest], its highest rest, as a sheet resists being drawn
     * further than it rests; a sheet still moving stops where it stands.
     *
     * @param delta The distance, in pixels; negative upwards.
     * @param highest The top of the sheet's highest rest, in pixels.
     */
    fun dragBy(delta: Float, highest: Float) {
        moving?.cancel()
        moving = null
        top = if (delta < 0f && top + delta < highest) top + delta * BeyondResistance else top + delta
    }

    /**
     * Moves the sheet's top to [target] from [velocity], stopping any move under way.
     *
     * @param target The top to rest at, in pixels.
     * @param velocity The velocity it starts at, in pixels a second.
     * @param reduceMotion Whether motion is reduced, which turns the spring into a short, even slide.
     * @return The move, which completes once the sheet rests.
     */
    fun moveTo(target: Float, velocity: Float, reduceMotion: Boolean): Job {
        moving?.cancel()
        val spec = if (reduceMotion) tween<Float>(ReducedMotionFadeMillis) else SettleSpring
        return scope.launch { animate(top, target, velocity, spec) { value, _ -> top = value } }.also { moving = it }
    }

    /**
     * Returns where a sheet let go at [velocity] comes to rest: the rest nearest where it is flung, or `null` where it
     * is flung below its lowest rest and closes.
     *
     * @param velocity The velocity it is let go at, in pixels a second; positive downwards.
     * @param rests The tops of the sheet's rests, in pixels.
     * @param height The window's height, in pixels.
     * @return The top to rest at, or `null` to close.
     */
    fun restFor(velocity: Float, rests: List<Float>, height: Float): Float? {
        val projected = top + velocity * FlingProjectionSeconds
        val lowest = rests.max()
        if (projected > lowest + (height - lowest) * CloseShare) return null
        return rests.minBy { abs(it - projected) }
    }
}

/**
 * Hands a sheet the scrolling its content does not use: a drag up raises the sheet before its content scrolls, and a
 * drag down past the content's top lowers it.
 *
 * @param motion The sheet's motion.
 * @param highest The top of the sheet's highest rest, in pixels.
 * @param release Settles the sheet from a velocity.
 */
private class SheetScrollConnection(
    private val motion: SheetMotion,
    private val highest: Float,
    private val release: (Float) -> Unit,
) : NestedScrollConnection {

    private var moved = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y >= 0f || motion.top <= highest) return Offset.Zero
        moved = true
        motion.dragBy(available.y, highest)
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        moved = true
        motion.dragBy(available.y, highest)
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!moved) return Velocity.Zero
        moved = false
        release(available.y)
        return available
    }
}

/**
 * Lays out the sheet: its top where [top] reads, as wide as the window less the gap it keeps from the sides, and as
 * tall as the room left above the window's bottom less the same gap; a wide window keeps it [SheetMaxWidth] wide, in
 * the middle.
 *
 * @param geometry The sheet's geometry.
 * @param top Reads the sheet's top, in pixels.
 * @param maxWidth The window's width, in pixels.
 * @param content The sheet.
 */
@Composable
private fun SheetLayout(geometry: SheetGeometry, top: () -> Float, maxWidth: Int, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    Layout(content) { measurables, constraints ->
        val at = top()
        val inset = geometry.insetAt(at, with(density) { SheetInset.toPx() })
        val width = minOf(maxWidth - 2f * inset, with(density) { SheetMaxWidth.toPx() }).coerceAtLeast(0f)
        val sheetHeight = (geometry.height - at - inset).coerceAtLeast(0f)
        val placeable = measurables.single().measure(Constraints.fixed(width.roundToInt(), sheetHeight.roundToInt()))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(IntOffset(((maxWidth - width) / 2f).roundToInt(), at.roundToInt()))
        }
    }
}

private val DefaultDetents = listOf(QuvenGlassSheetDetent.Medium, QuvenGlassSheetDetent.Large)

// Measured on the system's sheet on an iPhone.
private const val MediumShare = 0.527f
private val SheetInset = 9.dp
private val SheetShape = RoundedCornerShape(39.dp)
private val SheetMaxWidth = 574.dp
private const val OpaqueStart = 0.45f
private const val OpaqueEnd = 0.85f
private const val LargeDim = 0.85f
private val OpaqueSheet = Color(0xFF1B1A1D)
private val GrabberTop = 5.dp
private val GrabberSize = 36.dp to 5.dp
private val GrabberColor = Color(0x59FFFFFF)
private val ContentTop = 16.dp
private const val BeyondResistance = 0.3f
private const val FlingProjectionSeconds = 0.15f
private const val CloseShare = 0.5f
private const val SettleStiffness = 450f
private val SettleSpring = spring<Float>(dampingRatio = 0.9f, stiffness = SettleStiffness)
