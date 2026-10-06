package tv.quven.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

/**
 * The thumb of a control that lifts into a lens of glass while it is held, as the thumbs of Apple's switches and sliders
 * do: a white capsule that blurs away into clear glass, larger than itself, which shows the control's track a fifth
 * smaller through its body and folds it at its rim.
 *
 * @property thumb The size of the thumb at rest.
 * @property lens The size of the lens it lifts into.
 * @property colour The colour of the thumb at rest.
 * @property glass The clear glass of the lens once lifted.
 */
internal class GlassLensThumb(
    val thumb: DpSize,
    val lens: DpSize,
    val colour: Color = Color.White,
    val glass: QuvenGlassStyle = LensMaterial,
) {

    /**
     * Returns the thumb's frame, centred on [centre], [lift] of the way into the lens: the thumb grows only once it has
     * begun to blur away, and shrinks back before it shows white again, as Apple's does.
     *
     * @param density The density the frame is measured in.
     * @param centre The thumb's centre, in pixels.
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The frame, in pixels.
     */
    fun frame(density: Density, centre: Offset, lift: Float): Rect = with(density) {
        val size = lerp(thumb, lens, smoothstep(GrowthStart, 1f, lift))
        val width = size.width.toPx()
        val height = size.height.toPx()
        Rect(centre.x - width / 2f, centre.y - height / 2f, centre.x + width / 2f, centre.y + height / 2f)
    }

    /**
     * Returns the material of the lens [lift] of the way into it: frosted while the thumb blurs into it, clear once it
     * has lifted; its shadow and the light on its rim come and go with it, so nothing of the lens is left to vanish at
     * once when the thumb settles.
     *
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The material.
     */
    fun material(lift: Float): QuvenGlassStyle {
        val shown = smoothstep(0f, ThumbGone, lift)
        return glass.withBlur(lerp(LensFrost, 0.dp, smoothstep(FrostClearStart, 1f, lift))).copy(
            shadow = glass.shadow.copy(alpha = glass.shadow.alpha * shown),
            specular = glass.specular * shown,
        )
    }

    /**
     * Returns how much of the thumb shows [lift] of the way into the lens: all of it at rest, none past halfway.
     *
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The thumb's opacity, from 0 to 1.
     */
    fun thumbOpacity(lift: Float): Float = 1f - smoothstep(0f, ThumbGone, lift)

    /**
     * Returns how far the thumb is blurred [lift] of the way into the lens: crisp at rest, blurring as it fades.
     *
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The blur radius.
     */
    fun thumbBlur(lift: Float): Dp = ThumbFrost * smoothstep(0f, ThumbGone, lift)

    companion object {
        /** How far the frosted lens blurs the track while the thumb turns into it. */
        val LensFrost: Dp = 6.dp

        /** How far the thumb blurs as it fades into the lens. */
        val ThumbFrost: Dp = 5.dp

        /** The share of the lift by which the thumb has gone. */
        const val ThumbGone: Float = 0.55f

        /** The share of the lift from which the thumb grows into the lens. */
        const val GrowthStart: Float = 0.35f

        /** The share of the lift from which the lens begins to clear. */
        const val FrostClearStart: Float = 0.3f

        /** How much smaller the lens shows the track: Apple's shows it a fifth smaller. */
        const val LensZoom: Float = 1.25f

        /** The clear glass of the lens, measured on Apple's switch. */
        val LensMaterial: QuvenGlassStyle = QuvenGlassStyle.Clear.copy(
            blur = 0.dp,
            thickBlur = 0.dp,
            backdropScale = 1f,
            zoom = LensZoom,
            brighten = 0.03f,
            refraction = 10.dp,
            edgeWidth = 6.dp,
            cornerRefraction = 0.4f,
            cornerWidth = 0.32f,
            specular = 0.8f,
            dispersion = 0.04f,
            shadow = Color(0x1F000000),
            shadowRadius = 8.dp,
        )

        /** Gets the thumb of a switch and of a slider, and the lens it lifts into, as measured on Apple's. */
        val Control: GlassLensThumb = GlassLensThumb(thumb = DpSize(36.dp, 24.dp), lens = DpSize(57.dp, 37.5.dp))
    }
}

/**
 * Records this node's content into [backdrop], which a lens bends, and draws it with a hole where the lens [frame] reads
 * stands, so the lens alone shows the content there; as the lens fades, the content fades back into the hole.
 *
 * @param backdrop The backdrop the content records into, see-through so the lens shows what lies under the content.
 * @param opacity Reads how much of the lens shows, from 0 to 1.
 * @param frame Reads the lens's frame in this node's coordinates, or `null` while no lens stands over it.
 * @return The decorated modifier.
 */
internal fun Modifier.lensSource(backdrop: QuvenGlassBackdrop, opacity: () -> Float = { 1f }, frame: Density.() -> Rect?): Modifier =
    capsuleHole(opacity, frame).quvenGlassSource(backdrop)

/**
 * Draws a lens of [material] over [backdrop], which the content it bends records into through [lensSource].
 *
 * @param backdrop The backdrop the content records into.
 * @param material The lens's glass.
 * @return The decorated modifier.
 */
internal fun Modifier.lensGlass(backdrop: QuvenGlassBackdrop, material: QuvenGlassStyle): Modifier =
    liquidGlass(backdrop, material, CircleShape, interactionSource = null, reduceMotion = false, lift = null, pill = null, adapts = false)

/**
 * Draws a control's track with a [GlassLensThumb] over it: the track records into a backdrop of its own, which the lens
 * bends, and shows through a hole where the lens stands; the thumb blurs away into the lens as it lifts.
 *
 * @param thumb The thumb's sizes and materials.
 * @param lift Reads how far the thumb has lifted into the lens, from 0 to 1.
 * @param centre Reads the thumb's centre, in pixels, in the parent's coordinates, given the parent's size.
 * @param liquid Whether the lens is drawn: without Liquid Glass the thumb stays its thumb.
 * @param content Lays out what stands on the track under the thumb, such as a control's options, which the lens bends
 * with it.
 * @param track Draws the track over the parent's bounds.
 */
@Composable
internal fun BoxScope.LensTrack(
    thumb: GlassLensThumb,
    lift: () -> Float,
    centre: Density.(size: IntSize) -> Offset,
    liquid: Boolean,
    content: @Composable BoxScope.() -> Unit = {},
    track: DrawScope.() -> Unit,
) {
    val density = LocalDensity.current
    val backdrop = remember { QuvenGlassBackdrop(seeThrough = true) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val frame: () -> Rect = { thumb.frame(density, density.centre(size), lift()) }
    Box(
        Modifier
            .matchParentSize()
            .onSizeChanged { size = it }
            .then(if (liquid) Modifier.lensSource(backdrop) { frame().takeIf { lift() > 0f } } else Modifier)
            .drawBehind(track),
        content = content,
    )
    LensThumb(thumb, backdrop, lift, frame, liquid)
}

/**
 * Draws a [GlassLensThumb] at the frame [frame] reads, over [backdrop], which records the track alone.
 *
 * @param thumb The thumb's sizes and materials.
 * @param backdrop The backdrop the track records into.
 * @param lift Reads how far the thumb has lifted into the lens, from 0 to 1.
 * @param frame Reads the thumb's frame in the parent's coordinates, given its lift.
 * @param liquid Whether the lens is drawn: without Liquid Glass the thumb stays its thumb as it grows.
 */
@Composable
private fun BoxScope.LensThumb(
    thumb: GlassLensThumb,
    backdrop: QuvenGlassBackdrop,
    lift: () -> Float,
    frame: () -> Rect,
    liquid: Boolean,
) {
    val shown = lift()
    val glass = if (liquid && shown > 0f) Modifier.lensGlass(backdrop, remember(shown) { thumb.material(shown) }) else Modifier
    Box(
        Modifier
            .matchParentSize()
            .standingAt { frame() }
            .testTag(LensThumbTag)
            .then(glass),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (liquid) thumb.thumbOpacity(lift()) else 1f }
                .blur(if (liquid) thumb.thumbBlur(shown) else 0.dp, BlurredEdgeTreatment.Unbounded)
                .background(thumb.colour, CircleShape),
        )
    }
}

/** The test tag of the thumb a [LensTrack] draws. */
internal const val LensThumbTag = "quven-glass-lens-thumb"
