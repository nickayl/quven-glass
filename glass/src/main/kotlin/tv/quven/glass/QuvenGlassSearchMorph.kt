package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
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
 * The morph is as wide as the resting bar, the tabs, [gap] and the Search circle, and [height] tall.
 *
 * @param searching Whether the bar stands as a search field.
 * @param onSearch Invoked when the Search circle, or the field it opens into, is pressed.
 * @param onEndSearch Invoked when the folded tabs are pressed.
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
) {
    val progress = remember { Animatable(if (searching) 1f else 0f) }
    LaunchedEffect(searching, reduceMotion) {
        progress.animateTo(if (searching) 1f else 0f, if (reduceMotion) tween(ReducedMotionFadeMillis) else MorphSpring)
    }
    val density = LocalDensity.current
    val frame = remember(searching, tabsWidth, height, gap, density) {
        val sizes = with(density) { SearchMorphSizes(tabsWidth.toPx(), height.toPx(), gap.toPx(), SearchInset.toPx(), NearGap.toPx()) }
        return@remember { searchMorphFrame(progress.value, opening = searching, sizes) }
    }
    val item = remember(style) { style.forBarItems() }
    QuvenGlassContainer(modifier, style = style, spacing = JoinSpacing, backdrop = backdrop) {
        SearchMorphLayout(frame) {
            if (progress.value == 0f && !searching) {
                tabs()
            } else {
                BarItemButton(onEndSearch, item, backdrop, reduceMotion) {
                    FoldingFace(frame, { progress.value }, heldCentre, tabsFace, heldGlyph)
                }
            }
            BarItemButton(onSearch, item, backdrop, reduceMotion) {
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
 * @property tabs The capsule of tabs, folding or folded.
 * @property field The Search circle, stretching or stretched into the field.
 * @property facesSize The size the tabs' faces are laid out at, the resting capsule's.
 * @property facesScale The scale the tabs' faces are drawn at from their start.
 * @property facesAlpha The opacity of the tabs' faces.
 * @property fieldAlpha The opacity of the field after the Search glyph.
 */
internal class SearchMorphFrame(
    val tabs: Rect,
    val field: Rect,
    val facesSize: Size,
    val facesScale: Float,
    val facesAlpha: Float,
    val fieldAlpha: Float,
)

/**
 * Returns the frame [progress] of the way from the resting bar to the field, as Apple's tab bar draws it: opening, the
 * tabs fold and the field stretches with the room between them growing as both sink into the bar, the tabs' faces fading
 * as they stand; closing, the capsules come within [SearchMorphSizes.nearGap] of each other as the tabs unfold, the
 * field's text gone at once and the tabs' faces growing back with their capsule.
 *
 * @param progress How far the bar has turned into a field, from 0 to 1, past either while its spring overshoots.
 * @param opening Whether the bar is turning into the field, rather than back.
 * @param sizes The sizes the morph is laid out from.
 * @return The frame.
 */
internal fun searchMorphFrame(progress: Float, opening: Boolean, sizes: SearchMorphSizes): SearchMorphFrame {
    val width = sizes.tabsWidth + sizes.gap + sizes.height
    val sunk = sizes.inset * progress.coerceIn(0f, 1f)
    val tall = sizes.height - sunk * 2f
    val tabsEnd = max(sunk + tall, lerp(sizes.tabsWidth, sizes.height, progress) - sunk)
    val searchGap = sizes.inset * 2f
    val back = 1f - progress
    val apart = if (opening) {
        lerp(sizes.gap, searchGap, progress)
    } else {
        lerp(lerp(searchGap, sizes.nearGap, smoothstep(0f, GapCollapseEnd, back)), sizes.gap, smoothstep(GapRestoreStart, 1f, back))
    }
    val fieldStart = min(tabsEnd + apart, width - sunk - tall)
    return SearchMorphFrame(
        tabs = Rect(sunk, sunk, tabsEnd, sunk + tall),
        field = Rect(fieldStart, sunk, width - sunk, sunk + tall),
        facesSize = Size(sizes.tabsWidth, sizes.height),
        facesScale = if (opening) 1f else ((tabsEnd - sunk) / sizes.tabsWidth).coerceIn(0f, 1f),
        facesAlpha = if (opening) 1f - smoothstep(0f, FaceHideEnd, progress) else smoothstep(FaceShowStart, FaceShowEnd, back),
        fieldAlpha = if (opening) smoothstep(FieldShowStart, FieldShowEnd, progress) else 1f - smoothstep(0f, FieldHideEnd, back),
    )
}

/**
 * Draws a pressable item of the bar, a capsule of glass that grows and lights under the finger as the tab bar's items do.
 *
 * @param onClick Invoked when the item is pressed.
 * @param material The item's material.
 * @param backdrop The backdrop the glass stands over.
 * @param reduceMotion Whether motion is reduced.
 * @param content Draws the item's content, filling it.
 */
@Composable
private fun BarItemButton(
    onClick: () -> Unit,
    material: QuvenGlassStyle,
    backdrop: QuvenGlassBackdrop?,
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    GlassButton(
        onClick = onClick,
        modifier = Modifier,
        shape = CircleShape,
        material = material,
        prominent = false,
        ink = Color.White,
        lightInk = Color.Black,
        contentPadding = PaddingValues(0.dp),
        enabled = true,
        backdrop = backdrop,
        reduceMotion = reduceMotion,
        interactionSource = null,
    ) { content() }
}

/**
 * Draws the folding tabs' faces where they stood at rest, fading and, as the tabs unfold, scaled with their capsule, and
 * the held tab's glyph travelling between its tab among them and the circle the tabs fold into at the capsule's start.
 *
 * @param frame Reads the morph's frame.
 * @param progress Reads how far the bar has turned into a field.
 * @param heldCentre The centre of the held tab's glyph in the resting capsule.
 * @param tabsFace Draws the tabs' faces.
 * @param heldGlyph Draws the held tab's glyph.
 */
@Composable
private fun FoldingFace(
    frame: () -> SearchMorphFrame,
    progress: () -> Float,
    heldCentre: DpOffset,
    tabsFace: @Composable RowScope.() -> Unit,
    heldGlyph: @Composable BoxScope.() -> Unit,
) {
    val origin = if (LocalLayoutDirection.current == LayoutDirection.Ltr) FacesOrigin else FacesOriginRtl
    Layout(
        {
            Row(
                Modifier.graphicsLayer {
                    val shown = frame()
                    alpha = shown.facesAlpha
                    scaleX = shown.facesScale
                    scaleY = shown.facesScale
                    transformOrigin = origin
                },
                verticalAlignment = Alignment.CenterVertically,
                content = tabsFace,
            )
            Box(contentAlignment = Alignment.Center, content = heldGlyph)
        },
        Modifier.fillMaxSize().clip(CircleShape),
    ) { measurables, constraints ->
        val shown = frame()
        val width = constraints.maxWidth
        val tall = constraints.maxHeight
        val sunk = shown.tabs.top
        val faces = measurables[0].measure(fixed(shown.facesSize.width, shown.facesSize.height))
        val glyph = measurables[1].measure(Constraints.fixed(tall, tall))
        // The glyph leaves the held tab where the faces stand, scaled with them, for the circle at the capsule's start.
        val p = progress()
        val scale = shown.facesScale
        val facesMiddle = shown.facesSize.height / 2f
        val centreX = lerp(heldCentre.x.toPx() * scale - sunk, tall / 2f, p)
        val centreY = lerp(facesMiddle + (heldCentre.y.toPx() - facesMiddle) * scale - sunk, tall / 2f, p)
        layout(width, tall) {
            faces.placeRelative(-sunk.roundToInt(), -sunk.roundToInt())
            glyph.placeRelative((centreX - tall / 2f).roundToInt(), (centreY - tall / 2f).roundToInt())
        }
    }
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
        val folding = measurables[0].measure(fixed(shown.tabs.width, shown.tabs.height))
        val stretching = measurables[1].measure(fixed(shown.field.width, shown.field.height))
        val width = shown.field.right + shown.tabs.top
        val height = shown.tabs.bottom + shown.tabs.top
        layout(constraints.constrainWidth(width.roundToInt()), constraints.constrainHeight(height.roundToInt())) {
            folding.placeRelative(shown.tabs.left.roundToInt(), shown.tabs.top.roundToInt())
            stretching.placeRelative(shown.field.left.roundToInt(), shown.field.top.roundToInt())
        }
    }
}

private fun fixed(width: Float, height: Float): Constraints = Constraints.fixed(width.roundToInt(), height.roundToInt())

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
private val MorphSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 380f)
private val SearchInset = 7.25.dp
private val NearGap = 4.dp
private val JoinSpacing = 6.dp
private val FacesOrigin = TransformOrigin(0f, 0.5f)
private val FacesOriginRtl = TransformOrigin(1f, 0.5f)
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
