package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

/**
 * Draws a switch as Apple's: a capsule track, green while on, whose white thumb lifts into a lens of glass while it is
 * held, dragged or carried across, and settles back into a thumb once it rests. The lens shows the track through its
 * body, a fifth smaller, and folds it at its rim.
 *
 * A tap turns the switch over; a drag carries the thumb and turns the switch to the side it is let go nearer.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked with the new state when the switch is tapped or its thumb is dragged across, or `null`
 * for a passive switch, whose state a parent such as a toggleable row changes and announces.
 * @param modifier Modifier applied to the switch.
 * @param enabled Whether the switch can be changed.
 * @param onColor The colour of the track while the switch is on.
 * @param offColor The colour of the track while the switch is off, translucent over the page as Apple's is in its dark
 * appearance.
 * @param reduceMotion Whether motion is reduced: the thumb then slides across without lifting into a lens.
 * @param interactionSource The source of the switch's presses and drags, or `null` for one of its own.
 */
@Composable
public fun QuvenGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onColor: Color = SwitchOnColor,
    offColor: Color = SwitchOffColor,
    reduceMotion: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    GlassSwitch(checked, onCheckedChange, modifier, enabled, onColor, offColor, reduceMotion, interactionSource, QuvenGlass.isLiquidSupported)
}

/**
 * Draws a [QuvenGlassSwitch], lifting its thumb into a lens only where [liquid] says the lens is drawn.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked with the new state, or `null` for a passive switch.
 * @param modifier Modifier applied to the switch.
 * @param enabled Whether the switch can be changed.
 * @param onColor The colour of the track while on.
 * @param offColor The colour of the track while off.
 * @param reduceMotion Whether motion is reduced.
 * @param interactionSource The source of the switch's presses and drags, or `null` for one of its own.
 * @param liquid Whether the thumb lifts into a lens; without Liquid Glass it stays a white thumb.
 */
@Composable
internal fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    onColor: Color,
    offColor: Color,
    reduceMotion: Boolean,
    interactionSource: MutableInteractionSource?,
    liquid: Boolean,
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val travel = with(density) { SwitchTravel.toPx() }
    val currentChecked by rememberUpdatedState(checked)
    val currentChange by rememberUpdatedState(onCheckedChange)
    val motion = remember { SwitchThumbMotion(checked) }
    val pressed by interactions.collectIsPressedAsState()
    val active = enabled && onCheckedChange != null

    LaunchedEffect(checked, motion.isDragging, reduceMotion) { motion.settle(checked, reduceMotion) }
    LaunchedEffect(motion) { motion.followTone() }
    val lifted = liquid && !reduceMotion && (pressed || motion.isDragging || motion.isTravelling)
    LaunchedEffect(lifted) { motion.lens.follow(lifted) }
    val dragState = rememberDraggableState { delta -> motion.dragBy((if (rtl) -delta else delta) / travel) }

    Box(
        modifier
            .size(SwitchSize)
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = active,
                interactionSource = interactions,
                onDragStarted = { motion.startDrag() },
                onDragStopped = { motion.release { side -> if (side != currentChecked) currentChange?.invoke(side) } },
            )
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        interactionSource = interactions,
                        indication = null,
                        enabled = enabled,
                        role = Role.Switch,
                        onValueChange = { currentChange?.invoke(it) },
                    )
                } else {
                    // A passive switch offers no action of its own, so its state merges into the parent that changes it.
                    Modifier.semantics {
                        toggleableState = ToggleableState(checked)
                        role = Role.Switch
                    }
                },
            )
            .alpha(if (enabled) 1f else DisabledAlpha),
    ) {
        LensTrack(
            thumb = GlassLensThumb.Control,
            lift = { motion.lens.value },
            centre = { size ->
                val along = (SwitchInset + GlassLensThumb.Control.thumb.width / 2).toPx() + travel * motion.position
                Offset(if (rtl) size.width - along else along, size.height / 2f)
            },
            liquid = liquid,
        ) { drawRoundRect(lerp(offColor, onColor, motion.onShare), cornerRadius = CornerRadius(size.height / 2f)) }
    }
}

/**
 * Where a switch's thumb stands and how it moves: it slides to the side the switch is turned to, follows a drag, and is
 * let go to the side it stands nearer.
 *
 * @param checked Whether the switch is on when the motion begins.
 */
@Stable
internal class SwitchThumbMotion(checked: Boolean) {

    private val settled = Animatable(if (checked) 1f else 0f)
    private val tone = Animatable(if (checked) 1f else 0f)
    private var dragged by mutableFloatStateOf(0f)

    /** Gets how far the thumb lifts into its lens. */
    val lens: LensLift = LensLift()

    /** Gets a value indicating whether a drag carries the thumb. */
    var isDragging: Boolean by mutableStateOf(false)
        private set

    /** Gets where the thumb stands, from 0 at the off side to 1 at the on side. */
    val position: Float
        get() = if (isDragging) dragged else settled.value

    /** Gets a value indicating whether the thumb is sliding to its side and has not yet come within sight of it. */
    val isTravelling: Boolean
        get() = settled.isRunning && abs(settled.targetValue - settled.value) > ArrivedShare

    /** Gets how far the track has turned to the colour it takes while on, from 0 to 1. */
    val onShare: Float
        get() = tone.value

    private val headsForOn: Boolean
        get() = when {
            isDragging -> position >= Halfway
            settled.targetValue >= Halfway -> position >= ToneLead
            else -> position > 1f - ToneLead
        }

    /**
     * Fades the track to the colour of the side the thumb heads for, over a fade of its own, as the system's track does
     * however fast the thumb crosses: as soon as a slide sets off, or as a drag crosses the middle; runs until cancelled.
     */
    suspend fun followTone() {
        snapshotFlow { headsForOn }.collectLatest { on ->
            tone.animateTo(if (on) 1f else 0f, tween(ToneMillis, easing = LinearEasing))
        }
    }

    /** Starts a drag from where the thumb stands. */
    fun startDrag() {
        dragged = settled.value
        isDragging = true
    }

    /**
     * Carries a dragged thumb by [share] of its travel, no further than either side.
     *
     * @param share The distance, as a share of the thumb's travel; positive towards the on side.
     */
    fun dragBy(share: Float) {
        dragged = (dragged + share).coerceIn(0f, 1f)
    }

    /**
     * Lets a dragged thumb go where it stands and tells [onSide] the side it stands nearer, before the thumb slides
     * there.
     *
     * @param onSide Invoked with `true` for the on side, `false` for the off side.
     */
    suspend fun release(onSide: (Boolean) -> Unit) {
        settled.snapTo(dragged)
        onSide(dragged >= Halfway)
        isDragging = false
    }

    /**
     * Slides the thumb to the side [checked] names, unless a drag carries it.
     *
     * @param checked Whether the switch is on.
     * @param reduceMotion Whether motion is reduced, which turns the spring into a short slide.
     */
    suspend fun settle(checked: Boolean, reduceMotion: Boolean) {
        if (isDragging) return
        // The thumb lifts into its lens before it crosses, as the system's does after a tap; one already lifted crosses at once.
        val wait = ((1f - lens.value) * SlideDelayMillis).roundToInt()
        settled.animateTo(
            if (checked) 1f else 0f,
            if (reduceMotion) tween(ReducedMotionFadeMillis) else tween(SlideMillis, wait, FastOutSlowInEasing),
        )
    }

    private companion object {
        const val Halfway = 0.5f
        // The last of the slide moves too little to see: the lens settles from there, as the system's does on arrival.
        const val ArrivedShare = 0.04f
        // The system's thumb crosses in about 140 ms and comes to rest, with no tail for its lens to wait on.
        const val SlideMillis = 140
        const val SlideDelayMillis = 100
        // The system's track turns its colour in about 130 ms, from the moment its thumb sets off.
        const val ToneMillis = 130
        const val ToneLead = 0.1f
    }
}

/** The colour of an Apple switch's track while on, in its dark appearance. */
internal val SwitchOnColor = Color(0xFF30D158)

/** The colour of an Apple switch's track while off, in its dark appearance: a pale fill over the page. */
internal val SwitchOffColor = Color(0x4FDFDFEC)

/** The size of an Apple switch. */
internal val SwitchSize = DpSize(62.dp, 28.dp)

/** The room between the track's edge and the thumb at rest. */
private val SwitchInset: Dp = 2.dp


/** How far the thumb travels from one side to the other. */
private val SwitchTravel: Dp = SwitchSize.width - SwitchInset * 2 - GlassLensThumb.Control.thumb.width

