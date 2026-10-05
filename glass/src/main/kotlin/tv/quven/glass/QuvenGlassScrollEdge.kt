package tv.quven.glass

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** How content meets the bars standing over its edges, as Apple's scroll edge effects draw it. */
public enum class QuvenGlassScrollEdgeStyle {
    /** The content blurs and darkens progressively under the bar and a little past it, so the bar floats over it. */
    Soft,

    /** An opaque band as tall as the bar hides the content under it, ending on a sharp edge. */
    Hard,
}

/**
 * Draws the scroll edge effect of Apple's bars over this content: under a bar of [top] at its top edge and one of
 * [bottom] at its bottom edge, the content blurs and darkens as it nears the edge, or, [QuvenGlassScrollEdgeStyle.Hard],
 * disappears under an opaque band of [color]. Apply it to the content that scrolls under the bars, not to the bars.
 *
 * @param top The height of the bar standing over the top edge, or 0 for none.
 * @param bottom The height of the bar standing over the bottom edge, or 0 for none.
 * @param style How the content meets the bars.
 * @param color The colour of a hard edge's band.
 * @return The modifier.
 */
public fun Modifier.quvenGlassScrollEdge(
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    style: QuvenGlassScrollEdgeStyle = QuvenGlassScrollEdgeStyle.Soft,
    color: Color = HardEdgeColor,
): Modifier = this then ScrollEdgeElement(top, bottom, style, color)

/**
 * Returns how far toward black soft content stands at [share] of the way across the soft edge from the edge itself:
 * a long, faint tail over the whole edge and a darker lip along its start that ends abruptly, as measured on Apple's.
 *
 * @param share The distance from the edge as a share of the soft edge's reach, from 0 to 1.
 * @return The opacity of black over the content, from 0 to 1.
 */
internal fun softEdgeDim(share: Float): Float {
    val tail = SoftTail * (1f - smoothstep(SoftTailStart, 1f, share))
    val lip = (SoftLip - LipSlope * share) * (1f - smoothstep(LipEndStart, LipEnd, share))
    return 1f - (1f - tail) * (1f - lip)
}

/**
 * Returns how much of its blur soft content keeps at [share] of the way across the soft edge from the edge itself.
 *
 * @param share The distance from the edge as a share of the soft edge's reach, from 0 to 1.
 * @return The share of the blurred content shown, from 0 to 1.
 */
internal fun softEdgeBlur(share: Float): Float = 1f - smoothstep(0f, BlurReach, share)

/**
 * Returns how far toward black a hard edge's band stands at [share] of the way down its lip, as measured on Apple's.
 *
 * @param share The distance from the edge as a share of the band's lip, from 0 to 1.
 * @return The opacity of black over the band, from 0 to 1.
 */
internal fun hardEdgeDim(share: Float): Float = HardLip * (1f - smoothstep(0f, 1f, share))

private data class ScrollEdgeElement(
    val top: Dp,
    val bottom: Dp,
    val style: QuvenGlassScrollEdgeStyle,
    val color: Color,
) : ModifierNodeElement<ScrollEdgeNode>() {

    override fun create(): ScrollEdgeNode = ScrollEdgeNode(top, bottom, style, color)

    override fun update(node: ScrollEdgeNode) {
        node.top = top
        node.bottom = bottom
        node.style = style
        node.color = color
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "quvenGlassScrollEdge"
        properties["top"] = top
        properties["bottom"] = bottom
        properties["style"] = style
        properties["color"] = color
    }
}

private class ScrollEdgeNode(
    var top: Dp,
    var bottom: Dp,
    var style: QuvenGlassScrollEdgeStyle,
    var color: Color,
) : Modifier.Node(), DrawModifierNode {

    private var content: GraphicsLayer? = null
    private var blurred: GraphicsLayer? = null

    override fun onAttach() {
        content = requireGraphicsContext().createGraphicsLayer()
        blurred = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        listOfNotNull(content, blurred).forEach(requireGraphicsContext()::releaseGraphicsLayer)
        content = null
        blurred = null
    }

    override fun ContentDrawScope.draw() {
        val recording = content
        val blur = blurred
        if (recording == null || blur == null || (top <= 0.dp && bottom <= 0.dp)) return drawContent()
        recording.record { this@draw.drawContent() }
        drawLayer(recording)
        when (style) {
            QuvenGlassScrollEdgeStyle.Soft -> {
                val reaches = listOf(top.toPx() * SoftReach to true, bottom.toPx() * SoftReach to false).filter { it.first > 0f }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val sigma = EdgeBlur.toPx()
                    blur.renderEffect = BlurEffect(sigma, sigma, TileMode.Clamp)
                    blur.record(IntSize(size.width.toInt(), size.height.toInt())) { drawLayer(recording) }
                    reaches.forEach { (reach, atTop) -> softBlur(blur, reach, atTop) }
                }
                reaches.forEach { (reach, atTop) -> drawRect(edgeBrush(reach, atTop, ::softEdgeDim), band(reach, atTop).topLeft, band(reach, atTop).size) }
            }
            QuvenGlassScrollEdgeStyle.Hard -> {
                listOf(top.toPx() to true, bottom.toPx() to false).filter { it.first > 0f }.forEach { (height, atTop) ->
                    val rect = band(height, atTop)
                    drawRect(color, rect.topLeft, rect.size)
                    val lip = minOf(height, HardLipReach.toPx())
                    val lipRect = band(lip, atTop)
                    drawRect(edgeBrush(lip, atTop, ::hardEdgeDim), lipRect.topLeft, lipRect.size)
                }
            }
        }
    }

    /** Draws [blur] over the band [reach] tall at the top or bottom edge, faded out away from the edge. */
    private fun DrawScope.softBlur(blur: GraphicsLayer, reach: Float, atTop: Boolean) {
        val rect = band(reach, atTop)
        clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(rect, Paint())
                drawLayer(blur)
                drawRect(edgeBrush(reach, atTop, ::softEdgeBlur), rect.topLeft, rect.size, blendMode = BlendMode.DstIn)
                canvas.restore()
            }
        }
    }

    private fun DrawScope.band(height: Float, atTop: Boolean): Rect =
        if (atTop) Rect(Offset.Zero, Size(size.width, height)) else Rect(Offset(0f, size.height - height), Size(size.width, height))

    /** Returns a vertical brush of black whose opacity follows [opacity] from the edge across [reach]. */
    private fun DrawScope.edgeBrush(reach: Float, atTop: Boolean, opacity: (Float) -> Float): Brush {
        val stops = Array(GradientStops) { index ->
            val share = index / (GradientStops - 1f)
            val at = if (atTop) share else 1f - share
            at to Color.Black.copy(alpha = opacity(share))
        }.sortedBy { it.first }.toTypedArray()
        val rect = band(reach, atTop)
        return Brush.verticalGradient(*stops, startY = rect.top, endY = rect.bottom)
    }
}

// Measured on Apple's soft and hard scroll edges under a bar 56 pt tall on an iPhone.
private val HardEdgeColor = Color(0xFF212121)
private val EdgeBlur = 3.5.dp
private val HardLipReach = 18.dp
private const val SoftReach = 1.78f
private const val SoftTail = 0.29f
private const val SoftTailStart = 0.15f
private const val SoftLip = 0.66f
private const val LipSlope = 1f
private const val LipEndStart = 0.1f
private const val LipEnd = 0.17f
private const val BlurReach = 0.8f
private const val HardLip = 0.45f
private const val GradientStops = 12
