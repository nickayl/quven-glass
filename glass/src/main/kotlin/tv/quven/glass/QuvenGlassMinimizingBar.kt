package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Draws a phone's tab bar that minimizes while its content scrolls down, as Apple's tab bar does on a phone: the capsule
 * of tabs folds into a circle at its start, carrying the held tab's glyph to its middle, and sinks a little into the bar;
 * the tabs' faces shrink and fade with it. Pressing the circle, or the content returning to its top, unfolds it again.
 * Given more room than the bar's width, as on a tablet, the resting bar stands centred in it and the minimized circle
 * travels to its start, as Apple's tab bar does on an iPad; given the bar's own width it keeps its place, as on a phone,
 * so what stands beside it does not move. An [accessory], such as a player's controls,
 * stands on its own glass above the bar, as wide as it, and comes down beside the circle while the bar is minimized,
 * narrowing before it falls and rising before it widens again.
 *
 * @param minimized Whether the bar stands minimized, such as [QuvenGlassBarMinimizer.minimized] reports.
 * @param onExpand Invoked when the minimized bar is pressed.
 * @param tabsWidth The width of the capsule of tabs.
 * @param height The height of the bar.
 * @param heldCentre The centre of the held tab's glyph in the capsule of tabs, from its top start corner.
 * @param modifier Modifier applied to the bar.
 * @param style The material.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced: the bar then turns on a short ease instead of its spring.
 * @param tabs Draws the capsule of tabs at rest, its own glass, filling the room it is given.
 * @param tabsFace Draws the tabs' faces while they fold or unfold, without the held tab's glyph, which [heldGlyph] draws
 * instead; they answer no press, which belongs to the folded tabs.
 * @param heldGlyph Draws the held tab's glyph, centred in the square it is given.
 * @param accessory Draws the accessory's content in a row filling its capsule, or `null` for none.
 */
@Composable
public fun QuvenGlassMinimizingBar(
    minimized: Boolean,
    onExpand: () -> Unit,
    tabsWidth: Dp,
    height: Dp,
    heldCentre: DpOffset,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
    tabs: @Composable () -> Unit,
    tabsFace: @Composable RowScope.() -> Unit,
    heldGlyph: @Composable BoxScope.() -> Unit,
    accessory: (@Composable RowScope.() -> Unit)? = null,
) {
    val progress = remember { Animatable(if (minimized) 1f else 0f) }
    LaunchedEffect(minimized, reduceMotion) {
        progress.animateTo(if (minimized) 1f else 0f, if (reduceMotion) tween(ReducedMotionFadeMillis) else BarMorphSpring)
    }
    val density = LocalDensity.current
    val fold = remember(minimized, tabsWidth, height, density) {
        val width = with(density) { tabsWidth.toPx() }
        val tall = with(density) { height.toPx() }
        val inset = with(density) { BarSinkInset.toPx() }
        return@remember { minimizingBarFold(progress.value, minimizing = minimized, width, tall, inset) }
    }
    val item = remember(style) { style.forBarItems() }
    val gaps = with(density) { AccessoryGaps(AccessoryGap.toPx(), AccessoryInlineGap.toPx()) }
    Layout(
        {
            QuvenGlassContainer(style = style, backdrop = backdrop) {
                Layout({
                    FoldingTabs(
                        atRest = progress.value == 0f && !minimized,
                        onPress = onExpand,
                        material = item,
                        backdrop = backdrop,
                        reduceMotion = reduceMotion,
                        fold = fold,
                        progress = { progress.value },
                        heldCentre = heldCentre,
                        tabs = tabs,
                        tabsFace = tabsFace,
                        heldGlyph = heldGlyph,
                    )
                }) { measurables, _ ->
                    val shown = fold()
                    val tabsRect = shown.tabs
                    val folding = measurables[0].measure(fixed(tabsRect.width, tabsRect.height))
                    layout(shown.facesSize.width.roundToInt(), shown.facesSize.height.roundToInt()) {
                        folding.placeRelative(tabsRect.left.roundToInt(), tabsRect.top.roundToInt())
                    }
                }
            }
            if (accessory != null) {
                Row(
                    Modifier.liquidGlass(backdrop, style, CircleShape, null, reduceMotion, lift = null, pill = null),
                    verticalAlignment = Alignment.CenterVertically,
                    content = accessory,
                )
            }
        },
        modifier,
    ) { measurables, constraints ->
        val shown = fold()
        val barWidth = shown.facesSize.width
        val barHeight = shown.facesSize.height
        val inset = BarSinkInset.toPx()
        val above = if (measurables.size > 1) barHeight - inset * 2f + gaps.above else 0f
        val bar = measurables[0].measure(fixed(barWidth, barHeight))
        val strip = measurables.getOrNull(1)?.let { child ->
            val rect = accessoryRect(progress.value, barWidth, barHeight, inset, gaps)
            child.measure(fixed(rect.width, rect.height)) to rect
        }
        val width = if (constraints.hasBoundedWidth) max(constraints.maxWidth.toFloat(), barWidth) else barWidth
        val start = minimizedBarStart(progress.value, width, barWidth).roundToInt()
        layout(constraints.constrainWidth(width.roundToInt()), constraints.constrainHeight((above + barHeight).roundToInt())) {
            bar.placeRelative(start, above.roundToInt())
            strip?.let { (placeable, rect) -> placeable.placeRelative(start + rect.left.roundToInt(), rect.top.roundToInt()) }
        }
    }
}

/**
 * Returns the tabs [progress] of the way from rest to minimized, as Apple's tab bar draws them: their faces shrink with
 * the capsule, fading early as it minimizes and late as it grows back.
 *
 * @param progress How far the bar has minimized, from 0 to 1, past either while its spring overshoots.
 * @param minimizing Whether the bar is minimizing, rather than growing back.
 * @param tabsWidth The width of the resting capsule of tabs.
 * @param height The bar's height.
 * @param inset How far the minimized circle sinks into the bar from every side.
 * @return The fold.
 */
internal fun minimizingBarFold(progress: Float, minimizing: Boolean, tabsWidth: Float, height: Float, inset: Float): TabsFold {
    val alpha = if (minimizing) 1f - smoothstep(0f, FaceHideEnd, progress) else smoothstep(FaceShowStart, FaceShowEnd, 1f - progress)
    return tabsFold(progress, tabsWidth, height, inset, scalesFaces = true, facesAlpha = alpha)
}

/**
 * Returns where a bar [progress] of the way to minimized starts in a room [width] wide: centred while it rests, at the
 * room's start once minimized, as Apple's tab bar moves its minimized circle to the start of a tablet's window.
 *
 * @param progress How far the bar has minimized, from 0 to 1, past either while its spring overshoots.
 * @param width The width of the room the bar stands in.
 * @param barWidth The width of the resting bar.
 * @return The distance of the bar's start from the room's, in pixels.
 */
internal fun minimizedBarStart(progress: Float, width: Float, barWidth: Float): Float =
    lerp(max(0f, (width - barWidth) / 2f), 0f, progress.coerceIn(0f, 1f))

/**
 * The room about a [QuvenGlassMinimizingBar]'s accessory, in pixels.
 *
 * @property above The room between the accessory and the resting bar under it.
 * @property inline The room between the minimized circle and the accessory beside it.
 */
internal class AccessoryGaps(val above: Float, val inline: Float)

/**
 * Returns the accessory's capsule [progress] of the way from above the resting bar to beside the minimized circle, as
 * Apple's tab bar moves its bottom accessory: as tall as the minimized circle throughout, its start drawing in to the
 * circle before it falls into the bar's line, so that growing back it rises before it widens. The top of the accessory
 * standing above the bar is 0.
 *
 * @param progress How far the bar has minimized, from 0 to 1, past either while its spring overshoots.
 * @param tabsWidth The width of the resting bar.
 * @param height The bar's height.
 * @param inset How far the minimized circle sinks into the bar from every side.
 * @param gaps The room about the accessory.
 * @param endReserve The room the minimized bar keeps at its end past the accessory, such as a sunken Search circle and
 * the room beside it; none for a bar of tabs alone.
 * @return The accessory's capsule.
 */
internal fun accessoryRect(progress: Float, tabsWidth: Float, height: Float, inset: Float, gaps: AccessoryGaps, endReserve: Float = 0f): Rect {
    val tall = height - inset * 2f
    val down = smoothstep(AccessoryDropStart, 1f, progress)
    val along = smoothstep(AccessoryShiftStart, AccessoryShiftEnd, progress)
    val top = lerp(0f, tall + gaps.above + inset, down)
    val start = lerp(0f, inset + tall + gaps.inline, along)
    val end = lerp(tabsWidth, tabsWidth - inset - endReserve, along)
    return Rect(start, top, end, top + tall)
}

/**
 * Decides when a [QuvenGlassMinimizingBar] minimizes from the scrolling of the content under it, as Apple's tab bar does
 * when it minimizes on scrolling down: once the content has scrolled down [threshold] pixels on end the bar minimizes,
 * and it grows back when the content has scrolled back to where it stood, is drawn or flung against its top, or when
 * [expand] is called. A nested scroll reports no offset, so where the content stands is the distance scrolled since
 * the minimizer was made or last [reset]. Hand [nestedScrollConnection] to the scrolling content's `nestedScroll`
 * modifier.
 *
 * @param threshold How far, in pixels, the content scrolls down on end before the bar minimizes.
 */
@Stable
public class QuvenGlassBarMinimizer(private val threshold: Float) {

    private var travel = 0f
    private var position = 0f

    /** Gets whether the bar stands minimized. */
    public var minimized: Boolean by mutableStateOf(false)
        private set

    /** Grows the bar back, as a press on the minimized bar does. */
    public fun expand() {
        minimized = false
        travel = 0f
    }

    /** Grows the bar back and forgets how far the content has scrolled, for content that starts again at its top. */
    public fun reset() {
        expand()
        position = 0f
    }

    /** Gets the connection that follows the content's scrolling. */
    public val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            position = max(0f, position - consumed.y)
            when {
                consumed.y < 0f -> {
                    travel -= consumed.y
                    if (travel >= threshold) minimized = true
                }
                consumed.y > 0f -> travel = 0f
            }
            if (available.y > 0f || (minimized && position <= 0f)) expand()
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (available.y > 0f) expand()
            return Velocity.Zero
        }
    }
}

/**
 * Returns a [QuvenGlassBarMinimizer] that minimizes the bar once the content has scrolled down a little, as Apple's does.
 *
 * @return The minimizer.
 */
@Composable
public fun rememberQuvenGlassBarMinimizer(): QuvenGlassBarMinimizer {
    val threshold = with(LocalDensity.current) { MinimizeTravel.toPx() }
    return remember(threshold) { QuvenGlassBarMinimizer(threshold) }
}

// Measured on the system's tab bar on an iPhone.
private val MinimizeTravel = 20.dp
internal val AccessoryGap = 9.5.dp
internal val AccessoryInlineGap = 8.dp
private const val AccessoryDropStart = 0.55f
private const val AccessoryShiftStart = 0.1f
private const val AccessoryShiftEnd = 0.7f
private const val FaceHideEnd = 0.6f
private const val FaceShowStart = 0.2f
private const val FaceShowEnd = 0.85f
