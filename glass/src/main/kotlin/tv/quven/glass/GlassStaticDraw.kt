package tv.quven.glass

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

// Stroked over the edge and clipped to the outline, the rim keeps its whole width inside the shape, as a border does.
private val StaticRimWidth = 1.dp

/**
 * Draws the static frost of [style] filling [outline]: the ground, the frost fading towards the foot and the rim.
 *
 * @param outline The surface's outline.
 * @param style The material.
 * @param path A path to draw with, rewound first.
 */
internal fun DrawScope.drawFrost(outline: Outline, style: QuvenGlassStyle, path: Path) {
    val bounds = outline.bounds
    path.rewind()
    path.addOutline(outline)
    drawPath(path, style.ground)
    drawPath(path, Brush.verticalGradient(0f to style.trackTop, 1f to style.trackBottom, startY = bounds.top, endY = bounds.bottom))
    drawRim(path, Brush.verticalGradient(0f to style.rimTop, 1f to style.rimBottom, startY = bounds.top, endY = bounds.bottom))
}

/**
 * Draws the static pill of [style] filling [outline].
 *
 * @param outline The pill's outline.
 * @param style The material.
 * @param path A path to draw with, rewound first.
 * @param alpha The pill's opacity.
 */
internal fun DrawScope.drawPillFrost(outline: Outline, style: QuvenGlassStyle, path: Path, alpha: Float = 1f) {
    val bounds = outline.bounds
    path.rewind()
    path.addOutline(outline)
    drawPath(path, Brush.verticalGradient(0f to style.pillTop, 1f to style.pillBottom, startY = bounds.top, endY = bounds.bottom), alpha)
    drawRim(path, Brush.verticalGradient(0f to style.pillRim, 1f to style.pillRim), alpha)
}

/**
 * Draws [surface] in the static material of [style], where a Liquid Glass surface has no backdrop to stand over.
 *
 * @param surface The surface.
 * @param style The material.
 * @param path A path to draw with, rewound first.
 */
internal fun DrawScope.drawStaticSurface(surface: GlassSurface, style: QuvenGlassStyle, path: Path) {
    drawFrost(surface.form.toOutline(), style, path)
    val pill = surface.pill?.takeIf { it.alpha > 0f } ?: return
    drawPillFrost(Outline.Rounded(RoundRect(pill.rect, CornerRadius(pill.radius))), style, path, pill.alpha)
}

private fun DrawScope.drawRim(path: Path, brush: Brush, alpha: Float = 1f) {
    clipPath(path) { drawPath(path, brush, alpha, style = Stroke(StaticRimWidth.toPx() * 2f)) }
}
