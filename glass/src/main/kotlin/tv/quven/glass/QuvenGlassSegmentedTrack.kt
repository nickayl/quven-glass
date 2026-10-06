package tv.quven.glass

import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * Draws options in a row on a glass track, with a pill that marks the held one, as Apple's tab bar does. Pressed, the
 * whole track grows and lights around the finger, and the pill lifts into a lens of clear glass over it that crosses to
 * the pressed option, showing the options under it a quarter larger; the platter under the lens comes back once it has
 * settled. Sent to another option, the pill slides there on a spring, lifted into the lens. Where none is held it fades where it last stood; where motion is reduced
 * it fades out and in at its new place and the track does not grow. Each option draws itself and answers its own press;
 * given [onDraggedTo], a finger dragging along the track carries the lens under it, and the option it lets go over is
 * handed to it.
 *
 * The track is Liquid Glass over [backdrop] from Android 13, joined with its neighbours inside a
 * [QuvenGlassContainer], and the static material elsewhere.
 *
 * @param T The type of an option.
 * @param options The options, in the order they stand.
 * @param held The index of the held option, or a negative value for none.
 * @param optionSize The size every option takes.
 * @param modifier Modifier applied to the track.
 * @param style The material.
 * @param shape The shape of the track and of the pill.
 * @param gap The space between two options.
 * @param inset The space between the track's edge and its options.
 * @param pillTag The test tag of the pill, or `null` for none.
 * @param backdrop The backdrop the track stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 * @param onDraggedTo Invoked with the option a drag ends over, when that is not the option it started on, which answers
 * its own press; `null` keeps the pill from following a drag.
 * @param appearance The appearance the track reports to its options, or `null` where they do not read it.
 * @param option Draws one option, told whether it is held.
 */
@Composable
public fun <T> QuvenGlassSegmentedTrack(
    options: List<T>,
    held: Int,
    optionSize: DpSize,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    shape: Shape = CircleShape,
    gap: Dp = 0.dp,
    inset: Dp = 0.dp,
    pillTag: String? = null,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
    onDraggedTo: ((option: T) -> Unit)? = null,
    appearance: QuvenGlassAppearance? = null,
    option: @Composable (option: T, held: Boolean) -> Unit,
) {
    val presses = remember { GlassTrackPresses() }
    presses.lay(options.size, optionSize.width, gap, inset, LocalDensity.current)
    val track = remember(style, reduceMotion) { style.forTracks().growingUnless(reduceMotion) }
    val dynamics = remember(track) { PillDynamics(track.slideDamping, track.slideStiffness, PillEdges.Together, TrackPressSprings) }
    val motion = rememberTrackedPillMotion(presses, held, options.size, optionSize, gap, dynamics, reduceMotion)
    val dragAnswer = rememberDragAnswer(options, onDraggedTo)
    val liquid = backdrop != null && QuvenGlass.isLiquidSupported
    // A row's selection lifts into a lens of its own over the track; a lone option, or a row where motion is reduced,
    // lifts it within the track's glass.
    val intoLens = remember { PillLifting.IntoLens() }
    val lifting = if (liquid && !reduceMotion && options.size > 1) intoLens else PillLifting.WithinGlass
    val pillSource = rememberGlassPillSource(motion, optionSize, inset, shape, track, lifting)
    val trackLift = remember(motion) { GlassLiftSource { motion.press.value } }
    Box(
        modifier
            .liquidGlass(backdrop, track, shape, interactionSource = null, reduceMotion, trackLift, pillSource.takeIf { liquid }, appearance)
            .glassTrackPresses(presses, dragAnswer)
            .padding(inset),
    ) {
        Box(lifting.faces(motion, optionSize)) {
            GlassTrackPill(motion, optionSize, pillTag, drawsStaticPill = !liquid, track, shape)
            // The held look travels with the pill, as the selection's colour travels with the lens of Apple's tab bar:
            // the options stand as they are outside the pill, and as held inside it.
            // A finger held on an option takes the held look from the option held until then, as Apple's tab bar hands
            // it over; while a finger drags, the held look stays inside the pill alone.
            val shownHeld = when {
                presses.dragging -> -1
                presses.pressed in options.indices -> presses.pressed
                else -> held
            }
            // The held look shows inside the pill as far as the pill itself shows.
            val pillShown = { motion.alpha.value }
            val pill: Density.() -> Rect = { motion.frame(optionSize, this).rect }
            OptionRow(options, optionSize, gap, Modifier.capsuleHole(pillShown, pill), { it == shownHeld }, option)
            OptionRow(options, optionSize, gap, Modifier.insideCapsule(pillShown, pill).clearAndSetSemantics {}, { true }, option)
        }
        with(lifting) { Lens(motion, optionSize) }
    }
}

/**
 * Draws the options in a row, each in its room.
 *
 * @param T The type of an option.
 * @param options The options, in the order they stand.
 * @param optionSize The size every option takes.
 * @param gap The space between two options.
 * @param modifier Modifier applied to the row.
 * @param held Returns whether the option at an index is drawn as held.
 * @param option Draws one option, told whether it is held.
 */
@Composable
private fun <T> OptionRow(
    options: List<T>,
    optionSize: DpSize,
    gap: Dp,
    modifier: Modifier,
    held: (index: Int) -> Boolean,
    option: @Composable (option: T, held: Boolean) -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        options.forEachIndexed { index, item ->
            Box(Modifier.size(optionSize)) { option(item, held(index)) }
        }
    }
}

/**
 * Remembers a drag answer that maps an option's index to [onDraggedTo]; it stays the same across recompositions, so a
 * gesture in progress is never restarted.
 *
 * @param T The type of an option.
 * @param options The options, in the order they stand.
 * @param onDraggedTo Invoked with the option a drag ends over, or `null` to follow no drag.
 * @return The answer, or `null` where the track follows no drag.
 */
@Composable
internal fun <T> rememberDragAnswer(options: List<T>, onDraggedTo: ((option: T) -> Unit)?): ((index: Int) -> Unit)? {
    val currentOptions by rememberUpdatedState(options)
    val currentAnswer by rememberUpdatedState(onDraggedTo)
    val follows = onDraggedTo != null
    return remember(follows) { if (follows) { index -> currentAnswer?.invoke(currentOptions[index]) } else null }
}

/**
 * Remembers the motion of a track's pill and keeps it under the held option, the pressed option or a dragging finger.
 *
 * @param presses The track's presses.
 * @param held The index of the held option, or a negative value for none.
 * @param count The number of options.
 * @param optionSize The size every option takes.
 * @param gap The space between two options.
 * @param dynamics How the pill moves.
 * @param reduceMotion Whether motion is reduced.
 * @return The pill's motion.
 */
@Composable
internal fun rememberTrackedPillMotion(
    presses: GlassTrackPresses,
    held: Int,
    count: Int,
    optionSize: DpSize,
    gap: Dp,
    dynamics: PillDynamics,
    reduceMotion: Boolean,
): GlassPillMotion {
    val motion = remember { GlassPillMotion() }
    val density = LocalDensity.current
    val pressed = presses.pressed
    val target = if (pressed in 0 until count) pressed else held
    val dragging = presses.dragging
    LaunchedEffect(target, dragging, optionSize, gap, dynamics, reduceMotion) {
        if (!dragging) motion.moveTo(target, optionSize.width.value, (optionSize.width + gap).value, dynamics, reduceMotion)
    }
    LaunchedEffect(presses, count, optionSize, gap, density) {
        val span = optionSize.width.value * count + gap.value * (count - 1).coerceAtLeast(0)
        snapshotFlow { presses.dragX }
            .filter { !it.isNaN() }
            .collect { x -> motion.follow(with(density) { x.toDp() }.value, optionSize.width.value, span) }
    }
    LaunchedEffect(pressed >= 0, reduceMotion, dynamics) {
        motion.press.animateTo(if (pressed >= 0) 1f else 0f, dynamics.press.spec(held = pressed >= 0, reduceMotion))
    }
    LaunchedEffect(motion) {
        snapshotFlow { motion.lift > 0f }.collectLatest { lifted -> motion.showPlatter(!lifted) }
    }
    return motion
}

/**
 * The option a finger holds down on a track, read before the option's own press answers, so the pill can move under it
 * at once; and the track's layout the point is read against.
 */
@Stable
internal class GlassTrackPresses {

    /** Gets the index of the option held down, or -1 for none. */
    var pressed by mutableIntStateOf(-1)
        private set

    /** Gets the dragging finger's distance from the first option's start, or NaN while no finger drags. */
    var dragX by mutableFloatStateOf(Float.NaN)
        private set

    /** Gets a value indicating whether a finger drags along the track. */
    val dragging: Boolean
        get() = !dragX.isNaN()

    private var count = 0
    private var optionWidth = 0f
    private var optionGap = 0f
    private var trackInset = 0f

    /**
     * Records the track's layout.
     *
     * @param count The number of options.
     * @param width The width of an option.
     * @param gap The space between two options.
     * @param inset The space between the track's edge and its options.
     * @param density The density the layout is measured at.
     */
    fun lay(count: Int, width: Dp, gap: Dp, inset: Dp, density: Density) {
        this.count = count
        optionWidth = with(density) { width.toPx() }
        optionGap = with(density) { gap.toPx() }
        trackInset = with(density) { inset.toPx() }
    }

    /**
     * Holds the option under [x], measured from the track's edge.
     *
     * @param x The point's distance from the track's start edge.
     */
    fun press(x: Float) {
        pressed = optionAt(x - trackInset, count, optionWidth, optionGap)
    }

    /**
     * Carries the pill under a dragging finger at [x], measured from the track's edge.
     *
     * @param x The finger's distance from the track's start edge.
     */
    fun drag(x: Float) {
        dragX = x - trackInset
    }

    /**
     * Releases the held option and any drag.
     *
     * @return The index of the option the drag ended over, or -1 where none was dragged or it ended between options.
     */
    fun release(): Int {
        val over = if (dragging) optionNearest(dragX, count, optionWidth, optionGap) else -1
        pressed = -1
        dragX = Float.NaN
        return over
    }
}

/**
 * Reports presses on the track into [presses] without consuming them; given [onDraggedTo], a finger moving past the
 * touch slop drags the pill.
 *
 * @param presses The track's presses.
 * @param onDraggedTo Invoked with the index of the option a drag ends over, or `null` to follow no drag.
 * @return The decorated modifier.
 */
internal fun Modifier.glassTrackPresses(presses: GlassTrackPresses, onDraggedTo: ((Int) -> Unit)?): Modifier =
    pointerInput(presses, onDraggedTo) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            presses.press(down.position.x)
            if (onDraggedTo == null) {
                waitForUpOrCancellation(PointerEventPass.Initial)
                presses.release()
            } else {
                val started = presses.pressed
                followDrag(down, presses)
                val over = presses.release()
                if (over >= 0 && over != started) onDraggedTo(over)
            }
        }
    }

/**
 * Drags the pill under the finger that went [down] once it moves past the touch slop, until it lifts.
 *
 * @param down The change the finger went down with.
 * @param presses The track's presses.
 */
private suspend fun AwaitPointerEventScope.followDrag(down: PointerInputChange, presses: GlassTrackPresses) {
    var dragging = false
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
        if (change == null || !change.pressed) return
        dragging = dragging || abs(change.position.x - down.position.x) > viewConfiguration.touchSlop
        if (dragging) presses.drag(change.position.x)
    }
}

/**
 * Remembers what the glass reads of the pill: its rectangle as it slides, stretches and swells, its radius, opacity,
 * lens and lift.
 *
 * @param motion The pill's motion.
 * @param optionSize The size every option takes.
 * @param inset The space between the track's edge and its options.
 * @param shape The pill's shape.
 * @param style The material.
 * @param lifting How the pill lifts: within the track's glass or into a lens of its own.
 * @return The source the glass reads the pill from.
 */
@Composable
internal fun rememberGlassPillSource(
    motion: GlassPillMotion,
    optionSize: DpSize,
    inset: Dp,
    shape: Shape,
    style: QuvenGlassStyle,
    lifting: PillLifting,
): GlassPillSource {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(motion, density, layoutDirection, optionSize, inset, shape, style, lifting) {
        GlassPillSource { _ ->
            val frame = motion.frame(optionSize, density)
            val swell = motion.lift.coerceAtLeast(0f) * style.pressGrowth * PillPressShare * frame.restHeight / 2f
            val offset = with(density) { inset.toPx() }
            val rect = Rect(frame.left + offset - swell, frame.top + offset - swell, frame.right + offset + swell, frame.bottom + offset + swell)
            val radius = GlassForm.of(shape, rect.size, layoutDirection, density)?.topLeft ?: (rect.height / 2f)
            lifting.pill(motion, frame, rect, radius)
        }
    }
}

/**
 * Lays out the pill where its motion stands: a box that carries the pill's test tag in both materials and draws the
 * static pill where the track draws no Liquid Glass.
 *
 * @param motion The pill's motion.
 * @param optionSize The size every option takes.
 * @param tag The pill's test tag, or `null` for none.
 * @param drawsStaticPill Whether the box draws the static pill.
 * @param style The material.
 * @param shape The pill's shape.
 */
@Composable
internal fun GlassTrackPill(motion: GlassPillMotion, optionSize: DpSize, tag: String?, drawsStaticPill: Boolean, style: QuvenGlassStyle, shape: Shape) {
    Box(
        Modifier
            .standingAt { motion.frame(optionSize, this).rect }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .then(if (drawsStaticPill) Modifier.graphicsLayer { alpha = motion.alpha.value }.quvenGlassPill(style, shape) else Modifier),
    )
}

/** The share of the material's press growth a pressed pill swells by, past the track it lies in. */
private const val PillPressShare = 2.5f

/**
 * The springs a track grows on under the finger and shrinks back on once it lifts, passing a little below its size, as
 * measured on the system's tab bar on an iPad.
 */
private val TrackPressSprings = PressSprings(
    rise = spring(dampingRatio = 0.7f, stiffness = 625f),
    fall = spring(dampingRatio = 0.71f, stiffness = 400f),
)
