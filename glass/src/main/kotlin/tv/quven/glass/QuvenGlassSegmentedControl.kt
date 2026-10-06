package tv.quven.glass

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Draws Apple's segmented control as it stands in a page's content: a flat track of grey, not glass, holding its options
 * side by side, the chosen one on a lighter pill. Pressed, dragged or sent to another option, the pill lifts into a lens
 * of clear glass, taller than the track, which bends the options under it as it travels and settles back into the pill
 * once it rests, as the system's control does. For a control of glass in a bar, such as a toolbar's, use
 * [QuvenGlassSegmentedTrack].
 *
 * @param T The type of an option.
 * @param options The options, in the order they stand.
 * @param selected The index of the chosen option, or a negative value for none.
 * @param onSelect Invoked with an option pressed, or dragged to and let go over.
 * @param modifier Modifier applied to the control.
 * @param optionWidth The width every option takes.
 * @param style The material; the pill crosses on the system control's own spring whatever its slide.
 * @param reduceMotion Whether motion is reduced: the pill then slides without lifting into a lens.
 * @param option Draws one option, centred in its room and told whether it is chosen; it answers no press of its own.
 */
@Composable
public fun <T> QuvenGlassSegmentedControl(
    options: List<T>,
    selected: Int,
    onSelect: (option: T) -> Unit,
    modifier: Modifier = Modifier,
    optionWidth: Dp = SegmentWidth,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    reduceMotion: Boolean = false,
    option: @Composable (option: T, selected: Boolean) -> Unit,
) {
    val optionSize = DpSize(optionWidth, ControlHeight - PillInset * 2)
    val presses = remember { GlassTrackPresses() }
    presses.lay(options.size, optionWidth, 0.dp, PillInset, LocalDensity.current)
    val motion = rememberTrackedPillMotion(presses, selected, options.size, optionSize, 0.dp, SegmentDynamics, reduceMotion)
    val answer = rememberDragAnswer(options, onSelect)
    val lens = remember { LensLift() }
    LaunchedEffect(presses, motion) {
        // The lens stays lifted all the way across, as the system's does, and settles once the pill rests.
        snapshotFlow { presses.pressed >= 0 || presses.dragging || motion.start.isRunning || motion.end.isRunning }
            .distinctUntilChanged()
            .collectLatest { lens.follow(it) }
    }
    val thumb = remember(optionSize) { segmentThumb(optionSize) }
    Box(
        modifier
            .height(ControlHeight)
            .width(optionWidth * options.size + PillInset * 2)
            .glassTrackPresses(presses, answer),
    ) {
        LensTrack(
            thumb = thumb,
            lift = { lens.value },
            centre = { size ->
                val frame = motion.frame(optionSize, this)
                Offset(PillInset.toPx() + (frame.left + frame.right) / 2f, size.height / 2f)
            },
            liquid = QuvenGlass.isLiquidSupported && !reduceMotion,
            content = {
                Row(Modifier.padding(PillInset), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    options.forEachIndexed { index, item ->
                        Box(
                            Modifier
                                .size(optionSize)
                                .selectable(index == selected, interactionSource = null, indication = null, role = Role.Tab) { onSelect(item) },
                            contentAlignment = Alignment.Center,
                        ) { option(item, index == selected) }
                    }
                }
            },
        ) { drawRoundRect(TrackColour, cornerRadius = CornerRadius(size.height / 2f)) }
    }
}

/**
 * Returns the pill of a segmented control whose options are [optionSize], and the lens it lifts into, as measured on
 * Apple's: the lens 18 wider and 12 taller than the pill, its glass clear and showing the options at their own size.
 *
 * @param optionSize The size every option takes.
 * @return The pill.
 */
internal fun segmentThumb(optionSize: DpSize): GlassLensThumb = GlassLensThumb(
    thumb = optionSize,
    lens = DpSize(optionSize.width + LensGrowth.width, optionSize.height + LensGrowth.height),
    colour = PillColour,
    glass = GlassLensThumb.LensMaterial.copy(zoom = 1f),
)

// Measured on the system's segmented control on an iPad, in the dark appearance: the track is the system's tertiary
// fill, the pill about a quarter of white 2 inside it; its lens is 18 wider and 12 taller.
private val ControlHeight = 32.dp
private val PillInset = 2.dp
private val SegmentWidth = 98.dp
private val LensGrowth = DpSize(18.dp, 12.dp)
private val TrackColour = Color(red = 118, green = 118, blue = 128, alpha = 61)
private val PillColour = Color(red = 240, green = 240, blue = 250, alpha = 70)

/** The damping and stiffness of the spring the pill crosses on, measured on Apple's. */
private const val SegmentDamping = 0.85f
private const val SegmentStiffness = 230f

/** How the system's pill moves: on a calmer spring than a tab bar's, across in about 240 ms, stretching on its way. */
private val SegmentDynamics = PillDynamics(
    SegmentDamping,
    SegmentStiffness,
    PillEdges.Stretching,
    PressSprings(spring(dampingRatio = SegmentDamping, stiffness = SegmentStiffness)),
)
