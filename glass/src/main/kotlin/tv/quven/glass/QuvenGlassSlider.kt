package tv.quven.glass

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Draws a slider as Apple's: a thin track filled up to the thumb, whose white thumb lifts into a lens of glass while it
 * is held or dragged and settles back once it is let go. The lens shows the track through its body, a fifth smaller,
 * and folds it at its rim. The thumb moves only under a drag that starts on it, as Apple's does, so a press elsewhere on
 * the track changes nothing; the arrow keys and a remote step it along.
 *
 * @param value The value the thumb stands at, within [valueRange].
 * @param onValueChange Invoked with each value the thumb is moved to.
 * @param modifier Modifier applied to the slider, which fills the width it is given.
 * @param enabled Whether the slider can be moved.
 * @param valueRange The values the slider spans.
 * @param steps The number of values between the two ends the thumb rests on, or 0 for any value.
 * @param onValueChangeFinished Invoked once a drag or a key press has moved the thumb, or `null` for nothing.
 * @param activeColor The colour of the track up to the thumb.
 * @param inactiveColor The colour of the track past the thumb, translucent over the page as Apple's is in its dark
 * appearance.
 * @param reduceMotion Whether motion is reduced: the thumb then stays a thumb while it is held.
 * @param interactionSource The source of the slider's focus, or `null` for one of its own.
 * @throws IllegalArgumentException [steps] is negative.
 */
@Composable
public fun QuvenGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = SliderActiveColor,
    inactiveColor: Color = SliderInactiveColor,
    reduceMotion: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    GlassSlider(
        value, onValueChange, modifier, enabled, SliderScale(valueRange, steps), onValueChangeFinished, activeColor,
        inactiveColor, reduceMotion, interactionSource, QuvenGlass.isLiquidSupported,
    )
}

/**
 * Draws a [QuvenGlassSlider], lifting its thumb into a lens only where [liquid] says the lens is drawn.
 *
 * @param value The value the thumb stands at.
 * @param onValueChange Invoked with each value the thumb is moved to.
 * @param modifier Modifier applied to the slider.
 * @param enabled Whether the slider can be moved.
 * @param scale The values the slider spans and the ones its thumb rests on.
 * @param onValueChangeFinished Invoked once a drag or a key press has moved the thumb, or `null` for nothing.
 * @param activeColor The colour of the track up to the thumb.
 * @param inactiveColor The colour of the track past the thumb.
 * @param reduceMotion Whether motion is reduced.
 * @param interactionSource The source of the slider's focus, or `null` for one of its own.
 * @param liquid Whether the thumb lifts into a lens; without Liquid Glass it stays a white thumb.
 */
@Composable
internal fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    scale: SliderScale,
    onValueChangeFinished: (() -> Unit)?,
    activeColor: Color,
    inactiveColor: Color,
    reduceMotion: Boolean,
    interactionSource: MutableInteractionSource?,
    liquid: Boolean,
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentValue by rememberUpdatedState(value)
    val currentChange by rememberUpdatedState(onValueChange)
    val currentFinished by rememberUpdatedState(onValueChangeFinished)
    val motion = remember { SliderThumbMotion() }
    val lifted = liquid && !reduceMotion && motion.isHeld
    LaunchedEffect(lifted) { motion.lens.follow(lifted) }
    // A key press or an accessibility action moves the thumb at once, as a finished change.
    val moveTo: (Float) -> Unit = { target ->
        val next = scale.snap(target)
        if (next != currentValue) {
            currentChange(next)
            currentFinished?.invoke()
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(SliderHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, scale.range, scale.steps)
                if (!enabled) disabled()
                setProgress { target ->
                    moveTo(target)
                    true
                }
            }
            .focusable(enabled, interactions)
            .onKeyEvent { event ->
                val direction = when (event.key) {
                    Key.DirectionRight -> if (rtl) -1 else 1
                    Key.DirectionLeft -> if (rtl) 1 else -1
                    else -> 0
                }
                if (!enabled || direction == 0) return@onKeyEvent false
                if (event.type == KeyEventType.KeyDown) moveTo(currentValue + direction * scale.keyStep)
                true
            }
            .pointerInput(enabled, rtl, scale) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val travel = size.width - GlassLensThumb.Control.thumb.width.toPx()
                    val centre = GlassLensThumb.Control.thumb.width.toPx() / 2f + travel * scale.shareOf(currentValue)
                    val along = if (rtl) size.width - down.position.x else down.position.x
                    if (abs(along - centre) > ThumbReach.toPx() / 2f) return@awaitEachGesture
                    motion.hold(scale.shareOf(currentValue))
                    try {
                        // Past the slop the thumb catches up with the finger, which it follows from then on.
                        val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                            val moved = change.position.x - down.position.x
                            currentChange(scale.valueAt(motion.dragBy((if (rtl) -moved else moved) / travel)))
                        } ?: return@awaitEachGesture
                        horizontalDrag(start.id) { change ->
                            val dx = change.positionChange().x
                            change.consume()
                            currentChange(scale.valueAt(motion.dragBy((if (rtl) -dx else dx) / travel)))
                        }
                        currentFinished?.invoke()
                    } finally {
                        motion.release()
                    }
                }
            }
            .alpha(if (enabled) 1f else DisabledAlpha),
    ) {
        LensTrack(
            thumb = GlassLensThumb.Control,
            lift = { motion.lens.value },
            centre = { size ->
                val width = GlassLensThumb.Control.thumb.width.toPx()
                val along = width / 2f + (size.width - width) * scale.shareOf(value)
                Offset(if (rtl) size.width - along else along, size.height / 2f)
            },
            liquid = liquid,
        ) {
            val height = SliderTrackHeight.toPx()
            val top = (size.height - height) / 2f
            val thumbWidth = GlassLensThumb.Control.thumb.width.toPx()
            val filled = thumbWidth / 2f + (size.width - thumbWidth) * scale.shareOf(value)
            val radius = CornerRadius(height / 2f)
            drawRoundRect(inactiveColor, Offset(0f, top), Size(size.width, height), radius)
            drawRoundRect(activeColor, Offset(if (rtl) size.width - filled else 0f, top), Size(filled, height), radius)
        }
    }
}

/**
 * The values a slider spans and the ones its thumb rests on.
 *
 * @property range The values the slider spans.
 * @property steps The number of values between the two ends the thumb rests on, or 0 for any value.
 * @throws IllegalArgumentException [steps] is negative.
 */
@Stable
internal data class SliderScale(val range: ClosedFloatingPointRange<Float>, val steps: Int) {

    init {
        require(steps >= 0) { "A slider rests on no fewer than zero steps." }
    }

    /** Gets how far a key press moves the thumb: one step, or a tenth of the range for a slider without steps. */
    val keyStep: Float
        get() = span / if (steps > 0) steps + 1 else KeyStepsWithoutSteps

    private val span: Float
        get() = range.endInclusive - range.start

    /**
     * Returns where [value] stands along the track.
     *
     * @param value The value.
     * @return The share of the track, from 0 to 1.
     */
    fun shareOf(value: Float): Float = if (span <= 0f) 0f else ((value - range.start) / span).coerceIn(0f, 1f)

    /**
     * Returns the value the thumb rests on at [share] of the track.
     *
     * @param share The share of the track, from 0 to 1.
     * @return The value, on a step where the slider has steps.
     */
    fun valueAt(share: Float): Float = snap(range.start + span * share.coerceIn(0f, 1f))

    /**
     * Returns [value] held to the range and, where the slider has steps, moved to the nearest one.
     *
     * @param value The value.
     * @return The value the thumb rests on.
     */
    fun snap(value: Float): Float {
        val held = value.coerceIn(range)
        if (steps == 0 || span <= 0f) return held
        val step = span / (steps + 1)
        return (range.start + ((held - range.start) / step).roundToInt() * step).coerceIn(range)
    }

    private companion object {
        const val KeyStepsWithoutSteps = 10
    }
}

/**
 * How a slider's thumb is held: whether a finger holds it, and where a drag has carried it, before the value it stands
 * at is snapped to a step.
 */
@Stable
internal class SliderThumbMotion {

    private var dragged by mutableFloatStateOf(0f)

    /** Gets how far the thumb lifts into its lens. */
    val lens: LensLift = LensLift()

    /** Gets a value indicating whether a finger holds the thumb. */
    var isHeld: Boolean by mutableStateOf(false)
        private set

    /**
     * Holds the thumb where it stands.
     *
     * @param share Where the thumb stands, as a share of the track.
     */
    fun hold(share: Float) {
        dragged = share
        isHeld = true
    }

    /**
     * Carries the held thumb by [share] of the track, no further than either end.
     *
     * @param share The distance, as a share of the track; positive towards the end.
     * @return Where the thumb stands now, as a share of the track.
     */
    fun dragBy(share: Float): Float {
        dragged = (dragged + share).coerceIn(0f, 1f)
        return dragged
    }

    /** Lets the thumb go. */
    fun release() {
        isHeld = false
    }
}

/** The colour of an Apple slider's track up to the thumb, in its dark appearance. */
internal val SliderActiveColor = Color(0xFF0091FF)

/** The colour of an Apple slider's track past the thumb, in its dark appearance: a pale fill over the page. */
internal val SliderInactiveColor = Color(0x21FFFFFF)

/** The height of an Apple slider. */
internal val SliderHeight: Dp = 31.dp

/** The height of an Apple slider's track. */
private val SliderTrackHeight: Dp = 6.dp

/** The width around the thumb's centre a finger takes the thumb from. */
private val ThumbReach: Dp = 44.dp
