package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** The share of the spring's stiffness the edge leading a move slides on. */
internal const val LeadingEdgeShare = 1.6f

/** The share of the spring's stiffness the edge trailing a move slides on. */
internal const val TrailingEdgeShare = 0.55f

private const val SquashPerStretch = 0.35f
private const val MaxSquash = 0.22f
private const val LensPerStretch = 0.5f
private const val PillFadeMillis = 250
private const val TravelRiseMillis = 60
private const val TravelHoldMillis = 140L
private const val TravelSettleDamping = 0.9f
private const val TravelSettleStiffness = 1500f

/**
 * Returns the index of the option under a point of a row of options.
 *
 * @param x The point's distance from the row's start.
 * @param count The number of options.
 * @param optionWidth The width of an option.
 * @param gap The space between two options.
 * @return The option's index, or -1 where the point falls before the row, past its end or in a gap.
 */
internal fun optionAt(x: Float, count: Int, optionWidth: Float, gap: Float): Int {
    if (x < 0f || count <= 0 || optionWidth <= 0f) return -1
    val step = optionWidth + gap
    val index = floor(x / step).toInt()
    return if (index < count && x - index * step < optionWidth) index else -1
}

/**
 * Returns the index of the option nearest a point of a row of options, a point in a gap or past either end included.
 *
 * @param x The point's distance from the row's start.
 * @param count The number of options.
 * @param optionWidth The width of an option.
 * @param gap The space between two options.
 * @return The option's index, or -1 for a row of none.
 */
internal fun optionNearest(x: Float, count: Int, optionWidth: Float, gap: Float): Int {
    if (count <= 0) return -1
    return ((x - optionWidth / 2f) / (optionWidth + gap)).roundToInt().coerceIn(0, count - 1)
}

/**
 * Returns the stiffness an edge of the pill slides on: the edge leading the move runs ahead and the trailing one
 * follows, so the pill stretches on its way and settles back to its width.
 *
 * @param stiffness The stiffness of the material's slide spring.
 * @param leads Whether the edge leads the move.
 * @return The edge's stiffness.
 */
internal fun pillEdgeStiffness(stiffness: Float, leads: Boolean): Float =
    stiffness * if (leads) LeadingEdgeShare else TrailingEdgeShare

/**
 * Returns the height a stretched pill keeps, as a share of its resting height: the further it stretches, the thinner it
 * draws.
 *
 * @param width The pill's width.
 * @param restWidth The pill's resting width.
 * @return The share, from `1 - 0.22` to 1.
 */
internal fun pillSquash(width: Float, restWidth: Float): Float {
    if (restWidth <= 0f) return 1f
    return 1f - ((width / restWidth - 1f) * SquashPerStretch).coerceIn(0f, MaxSquash)
}

/**
 * Returns how far the pill acts as a lens: fully while pressed, and in proportion to its stretch while it moves.
 *
 * @param width The pill's width.
 * @param restWidth The pill's resting width.
 * @param press How far the pill is pressed, from 0 to 1.
 * @return The lens, from 0 to 1.
 */
internal fun pillLens(width: Float, restWidth: Float, press: Float): Float {
    val stretch = if (restWidth <= 0f) 0f else ((width / restWidth - 1f) * LensPerStretch).coerceIn(0f, 1f)
    return max(press.coerceIn(0f, 1f), stretch)
}

/**
 * Where the pill stands at one frame, in pixels from the first option's start.
 *
 * @property left The pill's start edge.
 * @property right The pill's end edge.
 * @property top The top of the pill as drawn, thinned by its stretch.
 * @property bottom The foot of the pill as drawn.
 * @property restWidth The pill's width at rest.
 * @property restHeight The pill's height at rest, the height of an option.
 */
internal data class PillFrame(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val restWidth: Float,
    val restHeight: Float,
)

/**
 * The motion of the pill of a [QuvenGlassSegmentedTrack]: its two edges, in density-independent pixels from the first
 * option's start, its opacity, its press, and the lift it travels on: moving to another option, the pill rises into a
 * magnifying lens, slides there and settles back into a platter, however short the press that moved it.
 */
@Stable
internal class GlassPillMotion {

    /** Gets the pill's start edge. */
    val start = Animatable(0f)

    /** Gets the pill's end edge. */
    val end = Animatable(0f)

    /** Gets the pill's opacity. */
    val alpha = Animatable(0f)

    /** Gets how far the pill is pressed. */
    val press = Animatable(0f)

    /** Gets how far the pill is lifted for its travel to another option. */
    val travel = Animatable(0f)

    private var placed = false
    private var asked = false

    /**
     * Returns where the pill stands at this frame, its height thinned by its stretch.
     *
     * @param optionSize The size every option takes.
     * @param density The density the frame is measured at.
     * @return The frame.
     */
    fun frame(optionSize: DpSize, density: Density): PillFrame = with(density) {
        val left = start.value.dp.toPx()
        val right = end.value.dp.toPx()
        val restWidth = optionSize.width.toPx()
        val restHeight = optionSize.height.toPx()
        val drawn = restHeight * pillSquash(right - left, restWidth)
        val top = (restHeight - drawn) / 2f
        PillFrame(left, right, top, top + drawn, restWidth, restHeight)
    }

    /**
     * Centres the pill under a dragging finger, within the row of options.
     *
     * @param centre The finger's distance from the first option's start.
     * @param width The width of an option.
     * @param span The width of the whole row of options.
     */
    suspend fun follow(centre: Float, width: Float, span: Float) {
        val from = (centre - width / 2f).coerceIn(0f, maxOf(0f, span - width))
        placed = true
        asked = true
        start.snapTo(from)
        end.snapTo(from + width)
        alpha.snapTo(1f)
    }

    /**
     * Moves the pill to option [index], or fades it where it stands for a negative index.
     *
     * @param index The option's index, or a negative value for none.
     * @param width The width of an option.
     * @param step The distance from one option's start to the next one's.
     * @param style The material, whose spring the edges slide on.
     * @param reduceMotion Whether motion is reduced, which fades the pill out and in at its new place.
     */
    suspend fun moveTo(index: Int, width: Float, step: Float, style: QuvenGlassStyle, reduceMotion: Boolean): Unit = coroutineScope {
        // The first answer draws the pill at once; a pill shown later fades in.
        val first = !asked
        asked = true
        if (index < 0) {
            alpha.animateTo(0f, tween(PillFadeMillis))
            return@coroutineScope
        }
        val toStart = index * step
        val toEnd = toStart + width
        if (!placed) {
            placed = true
            start.snapTo(toStart)
            end.snapTo(toEnd)
            if (first) alpha.snapTo(1f) else alpha.animateTo(1f, tween(PillFadeMillis))
            return@coroutineScope
        }
        if (reduceMotion) {
            if (start.value != toStart || end.value != toEnd) {
                if (alpha.value > 0f) alpha.animateTo(0f, tween(ReducedMotionFadeMillis / 2))
                start.snapTo(toStart)
                end.snapTo(toEnd)
            }
            alpha.animateTo(1f, tween(ReducedMotionFadeMillis / 2))
            return@coroutineScope
        }
        val forward = toStart >= start.value
        val moves = start.value != toStart || end.value != toEnd
        launch { alpha.animateTo(1f, tween(PillFadeMillis)) }
        if (moves) {
            launch {
                travel.animateTo(1f, tween(TravelRiseMillis, easing = FastOutSlowInEasing))
                delay(TravelHoldMillis)
                travel.animateTo(0f, spring(TravelSettleDamping, TravelSettleStiffness))
            }
        }
        launch {
            start.animateTo(toStart, spring(style.slideDamping, pillEdgeStiffness(style.slideStiffness, leads = !forward)))
        }
        end.animateTo(toEnd, spring(style.slideDamping, pillEdgeStiffness(style.slideStiffness, leads = forward)))
    }
}
