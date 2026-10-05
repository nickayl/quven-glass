package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Turns a tab bar into a search field and back, as Apple's tab bar does on a phone when its Search tab is chosen: the
 * capsule of tabs folds into a circle at the start, carrying the held tab's glyph to its centre, while the Search circle
 * stretches into a field across the rest of the bar, and both sink a little into the bar's height; ending the search
 * unfolds them again. The Search circle is a glass button throughout, so the light of the press that opens the field
 * dies away on the field itself, and the folded circle is one too.
 *
 * While [minimized] the bar gets out of the way of the content, as Apple's tab bar does when it minimizes on scrolling
 * down: the tabs fold into the same circle and the Search circle sinks where it stands; a press on the folded tabs then
 * asks the bar to grow back. Drive it from a [QuvenGlassBarMinimizer].
 *
 * The morph is as wide as the resting bar, the tabs, [gap] and the Search circle, and [height] tall.
 *
 * @param searching Whether the bar stands as a search field.
 * @param onSearch Invoked when the Search circle, or the field it opens into, is pressed.
 * @param onEndSearch Invoked when the folded tabs are pressed while the bar stands as a search field.
 * @param tabsWidth The width of the capsule of tabs.
 * @param height The height of the bar, the side of the Search circle.
 * @param gap The room between the tabs and the Search circle at rest.
 * @param heldCentre The centre of the held tab's glyph in the capsule of tabs, from its top start corner.
 * @param modifier Modifier applied to the morph.
 * @param style The material.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced: the bar then turns on a short ease instead of its spring.
 * @param tabs Draws the capsule of tabs at rest, its own glass, filling the room it is given.
 * @param tabsFace Draws the tabs' faces while they fold or unfold, without the held tab's glyph, which [heldGlyph] draws
 * instead; they answer no press, which belongs to the folded tabs.
 * @param heldGlyph Draws the held tab's glyph, centred in the square it is given.
 * @param searchGlyph Draws the Search glyph, centred in the Search circle and then at the field's start.
 * @param field Draws the field after its glyph, such as the text being searched.
 * @param minimized Whether the bar stands minimized; searching takes precedence.
 * @param onExpand Invoked when the folded tabs are pressed while the bar stands minimized.
 * @param searchModifier Modifier applied to the Search circle, the field it opens into.
 */
@Composable
public fun QuvenGlassSearchMorph(
    searching: Boolean,
    onSearch: () -> Unit,
    onEndSearch: () -> Unit,
    tabsWidth: Dp,
    height: Dp,
    gap: Dp,
    heldCentre: DpOffset,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
    tabs: @Composable () -> Unit,
    tabsFace: @Composable RowScope.() -> Unit,
    heldGlyph: @Composable BoxScope.() -> Unit,
    searchGlyph: @Composable BoxScope.() -> Unit,
    field: @Composable RowScope.() -> Unit,
    minimized: Boolean = false,
    onExpand: () -> Unit = {},
    searchModifier: Modifier = Modifier,
) {
    val spec = if (reduceMotion) tween<Float>(ReducedMotionFadeMillis) else BarMorphSpring
    val progress = remember { Animatable(if (searching) 1f else 0f) }
    LaunchedEffect(searching, reduceMotion) { progress.animateTo(if (searching) 1f else 0f, spec) }
    // A search opened from the minimized bar opens from its folded circle, and ends on the whole bar.
    val shrinking = minimized && !searching
    val shrink = remember { Animatable(if (shrinking) 1f else 0f) }
    LaunchedEffect(shrinking, reduceMotion) {
        if (searching) shrink.snapTo(0f) else shrink.animateTo(if (shrinking) 1f else 0f, spec)
    }
    val density = LocalDensity.current
    val foldedAtStart = remember(searching) { searching && shrink.value > FoldedShare }
    val frame = remember(searching, shrinking, foldedAtStart, tabsWidth, height, gap, density) {
        val sizes = with(density) { SearchMorphSizes(tabsWidth.toPx(), height.toPx(), gap.toPx(), BarSinkInset.toPx(), NearGap.toPx()) }
        return@remember {
            if (searching || progress.value > 0f) {
                searchMorphFrame(progress.value, opening = searching, sizes, foldedAtStart)
            } else {
                minimizedBarFrame(shrink.value, minimizing = shrinking, sizes)
            }
        }
    }
    val item = remember(style) { style.forBarItems() }
    QuvenGlassContainer(modifier, style = style, spacing = JoinSpacing, backdrop = backdrop) {
        SearchMorphLayout(frame) {
            FoldingTabs(
                atRest = progress.value == 0f && !searching && shrink.value == 0f && !shrinking,
                onPress = if (searching || progress.value > 0f) onEndSearch else onExpand,
                material = item,
                backdrop = backdrop,
                reduceMotion = reduceMotion,
                fold = { frame().fold },
                progress = { if (foldedAtStart) 1f else max(progress.value, shrink.value) },
                heldCentre = heldCentre,
                tabs = tabs,
                tabsFace = tabsFace,
                heldGlyph = heldGlyph,
            )
            BarItemButton(onSearch, item, backdrop, reduceMotion, searchModifier) {
                Row(Modifier.fillMaxSize().clip(CircleShape).startPaddingFromHeight(GlyphRoom), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(GlyphRoom), contentAlignment = Alignment.Center, content = searchGlyph)
                    Row(
                        Modifier.padding(start = GlyphGap).graphicsLayer { alpha = frame().fieldAlpha },
                        verticalAlignment = Alignment.CenterVertically,
                        content = field,
                    )
                }
            }
        }
    }
}

/**
 * The sizes a [QuvenGlassSearchMorph] is laid out from, in pixels.
 *
 * @property tabsWidth The width of the resting capsule of tabs.
 * @property height The bar's height.
 * @property gap The room between the two capsules at rest.
 * @property inset How far each capsule sinks into the bar once searching.
 * @property nearGap The room the capsules close to while the field gives way to the tabs.
 */
internal class SearchMorphSizes(val tabsWidth: Float, val height: Float, val gap: Float, val inset: Float, val nearGap: Float)

/**
 * One frame of a [QuvenGlassSearchMorph], in the morph's own pixels.
 *
 * @property fold The capsule of tabs, folding or folded.
 * @property field The Search circle, stretching or stretched into the field.
 * @property fieldAlpha The opacity of the field after the Search glyph.
 */
internal class SearchMorphFrame(val fold: TabsFold, val field: Rect, val fieldAlpha: Float)

/**
 * Returns the frame [progress] of the way from the resting bar to the field, as Apple's tab bar draws it: opening, the
 * tabs fold and the field stretches with the room between them growing as both sink into the bar, the tabs' faces fading
 * as they stand; closing, the capsules come within [SearchMorphSizes.nearGap] of each other as the tabs unfold, the
 * field's text gone at once and the tabs' faces growing back with their capsule.
 *
 * @param progress How far the bar has turned into a field, from 0 to 1, past either while its spring overshoots.
 * @param opening Whether the bar is turning into the field, rather than back.
 * @param sizes The sizes the morph is laid out from.
 * @param fromFold Whether the opening starts from the minimized bar, whose tabs are already folded and whose Search circle
 * already sinks, rather than from the resting one.
 * @return The frame.
 */
internal fun searchMorphFrame(progress: Float, opening: Boolean, sizes: SearchMorphSizes, fromFold: Boolean = false): SearchMorphFrame {
    if (fromFold && opening) {
        val folded = minimizedBarFrame(1f, minimizing = true, sizes)
        val stretched = searchMorphFrame(progress, opening = true, sizes)
        return SearchMorphFrame(folded.fold, lerp(folded.field, stretched.field, progress), stretched.fieldAlpha)
    }
    val width = sizes.tabsWidth + sizes.gap + sizes.height
    val back = 1f - progress
    val fold = tabsFold(
        progress,
        sizes.tabsWidth,
        sizes.height,
        sizes.inset,
        scalesFaces = !opening,
        facesAlpha = if (opening) 1f - smoothstep(0f, FaceHideEnd, progress) else smoothstep(FaceShowStart, FaceShowEnd, back),
    )
    val searchGap = sizes.inset * 2f
    val apart = if (opening) {
        lerp(sizes.gap, searchGap, progress)
    } else {
        lerp(lerp(searchGap, sizes.nearGap, smoothstep(0f, GapCollapseEnd, back)), sizes.gap, smoothstep(GapRestoreStart, 1f, back))
    }
    val tabs = fold.tabs
    val fieldStart = min(tabs.right + apart, width - tabs.top - tabs.height)
    return SearchMorphFrame(
        fold = fold,
        field = Rect(fieldStart, tabs.top, width - tabs.top, tabs.bottom),
        fieldAlpha = if (opening) smoothstep(FieldShowStart, FieldShowEnd, progress) else 1f - smoothstep(0f, FieldHideEnd, back),
    )
}

/**
 * Returns the frame [progress] of the way from the resting bar to the minimized one, as Apple's tab bar minimizes with
 * Search beside its tabs: the tabs fold into a circle at the start as a minimizing bar's do, and the Search circle sinks
 * where it stands, as far into the bar.
 *
 * @param progress How far the bar has minimized, from 0 to 1, past either while its spring overshoots.
 * @param minimizing Whether the bar is minimizing, rather than growing back.
 * @param sizes The sizes the morph is laid out from.
 * @return The frame.
 */
internal fun minimizedBarFrame(progress: Float, minimizing: Boolean, sizes: SearchMorphSizes): SearchMorphFrame {
    val width = sizes.tabsWidth + sizes.gap + sizes.height
    val fold = minimizingBarFold(progress, minimizing, sizes.tabsWidth, sizes.height, sizes.inset)
    val sunk = fold.tabs.top
    return SearchMorphFrame(fold, Rect(width - sunk - fold.tabs.height, sunk, width - sunk, fold.tabs.bottom), fieldAlpha = 0f)
}

/**
 * Lays out the folding tabs and the stretching field where [frame] places them, in a box as wide as the resting bar.
 *
 * @param frame Reads the morph's frame.
 * @param content The tabs, then the Search circle.
 */
@Composable
private fun SearchMorphLayout(frame: () -> SearchMorphFrame, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val shown = frame()
        val tabs = shown.fold.tabs
        val folding = measurables[0].measure(fixed(tabs.width, tabs.height))
        val stretching = measurables[1].measure(fixed(shown.field.width, shown.field.height))
        val width = shown.field.right + tabs.top
        val height = tabs.bottom + tabs.top
        layout(constraints.constrainWidth(width.roundToInt()), constraints.constrainHeight(height.roundToInt())) {
            folding.placeRelative(tabs.left.roundToInt(), tabs.top.roundToInt())
            stretching.placeRelative(shown.field.left.roundToInt(), shown.field.top.roundToInt())
        }
    }
}

/**
 * Pads the start of the content by half of what its height leaves around [room], so a glyph that wide stays centred in a
 * circle.
 *
 * @param room The width of the glyph at the content's start.
 * @return The padded modifier.
 */
private fun Modifier.startPaddingFromHeight(room: Dp): Modifier = layout { measurable, constraints ->
    val pad = max(0, (constraints.maxHeight - room.roundToPx()) / 2)
    val placeable = measurable.measure(constraints.offset(horizontal = -pad))
    layout(placeable.width + pad, placeable.height) { placeable.placeRelative(pad, 0) }
}

// Measured on the system's tab bar on an iPhone.
private val NearGap = 4.dp
private val JoinSpacing = 6.dp
private val GlyphRoom = 24.dp
private val GlyphGap = 6.dp
private const val FaceHideEnd = 0.45f
private const val FaceShowStart = 0.1f
private const val FaceShowEnd = 0.8f
private const val FieldShowStart = 0.35f
private const val FieldShowEnd = 0.8f
private const val FieldHideEnd = 0.35f
private const val GapCollapseEnd = 0.15f
private const val GapRestoreStart = 0.6f

// The share of the way to minimized past which a bar counts as folded when the search opens.
private const val FoldedShare = 0.5f
