package tv.quven.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density

/**
 * Draws this node's content with a capsule-shaped hole where [frame] reads, the content fading back into the hole as
 * [opacity], how much of what covers the hole shows, falls.
 *
 * @param opacity Reads how much of what covers the hole shows, from 0 to 1.
 * @param frame Reads the hole's frame in this node's coordinates, or `null` for none.
 * @return The decorated modifier.
 */
internal fun Modifier.capsuleHole(opacity: () -> Float = { 1f }, frame: Density.() -> Rect?): Modifier = drawWithContent {
    val hole = frame() ?: return@drawWithContent drawContent()
    val path = capsule(hole)
    clipPath(path, ClipOp.Difference) { this@drawWithContent.drawContent() }
    drawFaded(path, hole, 1f - opacity().coerceIn(0f, 1f))
}

/**
 * Draws this node's content only inside the capsule [frame] reads, at [opacity].
 *
 * @param opacity Reads how much of the content shows, from 0 to 1.
 * @param frame Reads the capsule's frame in this node's coordinates.
 * @return The decorated modifier.
 */
internal fun Modifier.insideCapsule(opacity: () -> Float = { 1f }, frame: Density.() -> Rect): Modifier = drawWithContent {
    val area = frame()
    drawFaded(capsule(area), area, opacity().coerceIn(0f, 1f))
}

// Draws the content inside the path at the given opacity, nothing at all at 0.
private fun ContentDrawScope.drawFaded(path: Path, bounds: Rect, opacity: Float) {
    if (opacity <= 0f) return
    clipPath(path) {
        if (opacity >= 1f) {
            this@drawFaded.drawContent()
            return@clipPath
        }
        drawContext.canvas.saveLayer(bounds, Paint().apply { alpha = opacity })
        this@drawFaded.drawContent()
        drawContext.canvas.restore()
    }
}

// A capsule filling the rectangle.
private fun capsule(rect: Rect): Path = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(rect.height / 2f))) }
