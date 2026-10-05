package tv.quven.glass

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The capsule of a tab bar's tabs folding into a circle at its start, in the bar's own pixels.
 *
 * @property tabs The capsule, folding or folded.
 * @property facesSize The size the tabs' faces are laid out at, the resting capsule's.
 * @property facesScale The scale the tabs' faces are drawn at from their start.
 * @property facesAlpha The opacity of the tabs' faces.
 */
internal class TabsFold(val tabs: Rect, val facesSize: Size, val facesScale: Float, val facesAlpha: Float)

/**
 * Returns the capsule of tabs [progress] of the way from rest to the circle it folds into, as Apple's tab bar folds it:
 * its end draws in to its start while it sinks [inset] into the bar from every side.
 *
 * @param progress How far the tabs have folded, from 0 to 1, past either while a spring overshoots.
 * @param tabsWidth The width of the resting capsule.
 * @param height The bar's height.
 * @param inset How far the folded circle sinks into the bar from every side.
 * @param scalesFaces Whether the faces shrink with the capsule rather than keep their size.
 * @param facesAlpha The opacity of the faces.
 * @return The fold.
 */
internal fun tabsFold(progress: Float, tabsWidth: Float, height: Float, inset: Float, scalesFaces: Boolean, facesAlpha: Float): TabsFold {
    val sunk = inset * progress.coerceIn(0f, 1f)
    val tall = height - sunk * 2f
    val end = max(sunk + tall, lerp(tabsWidth, height, progress) - sunk)
    val scale = if (scalesFaces) ((end - sunk) / tabsWidth).coerceIn(0f, 1f) else 1f
    return TabsFold(Rect(sunk, sunk, end, sunk + tall), Size(tabsWidth, height), scale, facesAlpha)
}

/**
 * Draws the tabs: the caller's own capsule [atRest], and otherwise a pressable capsule of glass holding their faces as
 * [fold] places them, with the held tab's glyph travelling [progress] of the way to the circle they fold into.
 *
 * @param atRest Whether the tabs stand at rest, drawn by [tabs].
 * @param onPress Invoked when the folding or folded tabs are pressed.
 * @param material The material of the folding capsule.
 * @param backdrop The backdrop the glass stands over.
 * @param reduceMotion Whether motion is reduced.
 * @param fold Reads the fold.
 * @param progress Reads how far the tabs have folded.
 * @param heldCentre The centre of the held tab's glyph in the resting capsule.
 * @param tabs Draws the capsule of tabs at rest.
 * @param tabsFace Draws the tabs' faces without the held tab's glyph.
 * @param heldGlyph Draws the held tab's glyph.
 */
@Composable
internal fun FoldingTabs(
    atRest: Boolean,
    onPress: () -> Unit,
    material: QuvenGlassStyle,
    backdrop: QuvenGlassBackdrop?,
    reduceMotion: Boolean,
    fold: () -> TabsFold,
    progress: () -> Float,
    heldCentre: DpOffset,
    tabs: @Composable () -> Unit,
    tabsFace: @Composable RowScope.() -> Unit,
    heldGlyph: @Composable BoxScope.() -> Unit,
) {
    if (atRest) {
        tabs()
    } else {
        BarItemButton(onPress, material, backdrop, reduceMotion) {
            FoldingFace(fold, progress, heldCentre, tabsFace, heldGlyph)
        }
    }
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
internal fun BarItemButton(
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
 * Draws the folding tabs' faces where they stood at rest, fading and scaled as [fold] says, and the held tab's glyph
 * travelling between its tab among them and the circle the tabs fold into at the capsule's start.
 *
 * @param fold Reads the fold.
 * @param progress Reads how far the tabs have folded.
 * @param heldCentre The centre of the held tab's glyph in the resting capsule.
 * @param tabsFace Draws the tabs' faces.
 * @param heldGlyph Draws the held tab's glyph.
 */
@Composable
private fun FoldingFace(
    fold: () -> TabsFold,
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
                    val shown = fold()
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
        val shown = fold()
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
 * Returns constraints fixing a size given in fractional pixels.
 *
 * @param width The width.
 * @param height The height.
 * @return The constraints.
 */
internal fun fixed(width: Float, height: Float): Constraints = Constraints.fixed(width.roundToInt(), height.roundToInt())

// Measured on the system's tab bar on an iPhone.
internal val BarMorphSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 380f)
internal val BarSinkInset = 7.25.dp
private val FacesOrigin = TransformOrigin(0f, 0.5f)
private val FacesOriginRtl = TransformOrigin(1f, 0.5f)
