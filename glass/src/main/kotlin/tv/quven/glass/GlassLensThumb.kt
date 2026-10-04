package tv.quven.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
 */
internal class GlassLensThumb(val thumb: DpSize, val lens: DpSize) {

    /**
     * Returns the thumb's frame, centred on [centreX] and [centreY], [lift] of the way into the lens.
     *
     * @param density The density the frame is measured in.
     * @param centreX The thumb's centre along the track, in pixels.
     * @param centreY The thumb's centre across the track, in pixels.
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The frame, in pixels.
     */
    fun frame(density: Density, centreX: Float, centreY: Float, lift: Float): Rect = with(density) {
        val size = lerp(thumb, lens, lift.coerceIn(0f, 1f))
        val width = size.width.toPx()
        val height = size.height.toPx()
        Rect(centreX - width / 2f, centreY - height / 2f, centreX + width / 2f, centreY + height / 2f)
    }

    /**
     * Returns the material of the lens [lift] of the way into it: frosted while the thumb blurs into it, clear once it
     * has lifted.
     *
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The material.
     */
    fun material(lift: Float): QuvenGlassStyle =
        LensMaterial.copy(blur = lerp(LensFrost, 0.dp, smoothstep(FrostClearStart, 1f, lift)))

    /**
     * Returns how much of the white thumb shows [lift] of the way into the lens: all of it at rest, none past halfway.
     *
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The thumb's opacity, from 0 to 1.
     */
    fun thumbOpacity(lift: Float): Float = 1f - smoothstep(0f, ThumbGone, lift)

    /**
     * Returns how far the white thumb is blurred [lift] of the way into the lens: crisp at rest, blurring as it fades.
     *
     * @param lift How far the thumb has lifted into the lens, from 0 to 1.
     * @return The blur radius.
     */
    fun thumbBlur(lift: Float): Dp = ThumbFrost * smoothstep(0f, ThumbGone, lift)

    companion object {
        /** How far the frosted lens blurs the track while the thumb turns into it. */
        val LensFrost: Dp = 6.dp

        /** How far the white thumb blurs as it fades into the lens. */
        val ThumbFrost: Dp = 5.dp

        /** The share of the lift by which the white thumb has gone. */
        const val ThumbGone: Float = 0.55f

        /** The share of the lift from which the lens begins to clear. */
        const val FrostClearStart: Float = 0.3f

        /** How much smaller the lens shows the track: Apple's shows it a fifth smaller. */
        const val LensZoom: Float = 1.25f

        /** The clear glass of the lens, measured on Apple's switch. */
        val LensMaterial: QuvenGlassStyle = QuvenGlassStyle.Clear.copy(
            blur = 0.dp,
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
    }
}

/**
 * Draws the content of this track with a hole where the lens [frame] reads stands, so the lens alone shows the track
 * there, as it bends it.
 *
 * @param frame Reads the lens's frame in this node's coordinates, or `null` while no lens stands over it.
 * @return The decorated modifier.
 */
internal fun Modifier.lensHole(frame: () -> Rect?): Modifier = drawWithContent {
    val hole = frame() ?: return@drawWithContent drawContent()
    val path = Path().apply { addRoundRect(RoundRect(hole, CornerRadius(hole.height / 2f))) }
    clipPath(path, ClipOp.Difference) { this@drawWithContent.drawContent() }
}

/**
 * Draws a [GlassLensThumb] at the frame [frame] reads, over [backdrop], which records the track alone.
 *
 * @param thumb The thumb's sizes and materials.
 * @param backdrop The backdrop the track records into.
 * @param lift Reads how far the thumb has lifted into the lens, from 0 to 1.
 * @param frame Reads the thumb's frame in the parent's coordinates, given its lift.
 * @param liquid Whether the lens is drawn: without Liquid Glass the thumb stays a white thumb as it grows.
 */
@Composable
internal fun BoxScope.LensThumb(
    thumb: GlassLensThumb,
    backdrop: QuvenGlassBackdrop,
    lift: () -> Float,
    frame: () -> Rect,
    liquid: Boolean,
) {
    val shown = lift()
    val glass = if (liquid && shown > 0f) {
        Modifier.liquidGlass(
            backdrop = backdrop,
            style = remember(shown) { thumb.material(shown) },
            shape = CircleShape,
            interactionSource = null,
            reduceMotion = false,
            lift = null,
            pill = null,
            adapts = false,
        )
    } else {
        Modifier
    }
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
                .background(Color.White, CircleShape),
        )
    }
}

/** The test tag of the thumb a [LensThumb] draws. */
internal const val LensThumbTag = "quven-glass-lens-thumb"
