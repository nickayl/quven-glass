package tv.quven.glass

import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext

/**
 * Paints glass surfaces for the node that owns it: Liquid Glass over the backdrop once both stand in place, and the
 * static material while they do not or where the node lies inside the backdrop's own source.
 *
 * @param node The node the painter draws for, whose graphics context holds its layer.
 */
@RequiresApi(33)
internal class GlassPainter(private val node: DelegatableNode) {

    private var renderer: LiquidGlassRenderer? = null
    private val staticPath = Path()

    /** Gets the node's position in the root, or [Offset.Unspecified] before it is placed. */
    var origin: Offset = Offset.Unspecified
        private set

    /** Gets a value indicating whether the node lies inside the source of the backdrop it was attached to. */
    var isInsideSource: Boolean = false
        private set

    /**
     * Attaches the painter to [backdrop].
     *
     * @param backdrop The backdrop the node stands over.
     */
    fun attach(backdrop: QuvenGlassBackdrop) {
        isInsideSource = node.isInsideSourceOf(backdrop)
    }

    /** Releases the layer and forgets the node's place. */
    fun detach() {
        renderer?.release()
        renderer = null
        origin = Offset.Unspecified
        isInsideSource = false
    }

    /**
     * Records where the node now stands.
     *
     * @param coordinates The node's coordinates.
     * @return `true` if the node moved; otherwise, `false`.
     */
    fun place(coordinates: LayoutCoordinates): Boolean {
        val position = coordinates.positionInRoot()
        if (position == origin) return false
        origin = position
        return true
    }

    /**
     * Paints [surfaces] over [backdrop].
     *
     * @param surfaces The surfaces, in the coordinates of this scope.
     * @param backdrop The backdrop the surfaces stand over.
     * @param style The material.
     * @param blend The distance over which two surfaces join, in pixels.
     */
    fun DrawScope.paint(surfaces: List<GlassSurface>, backdrop: QuvenGlassBackdrop, style: QuvenGlassStyle, blend: Float) {
        if (surfaces.isEmpty()) return
        val source = backdrop.layer
        val sourceOrigin = backdrop.origin
        if (isInsideSource || source == null || sourceOrigin.isUnspecified || origin.isUnspecified) {
            surfaces.forEach { drawStaticSurface(it, style, staticPath) }
            return
        }
        val drawer = renderer ?: LiquidGlassRenderer(node.requireGraphicsContext()).also { renderer = it }
        with(drawer) { drawGlass(surfaces, style, blend, source, origin - sourceOrigin, backdrop.seeThrough) }
    }
}

/**
 * A node that paints glass over a backdrop: it keeps the painter, follows where it stands, and redraws when the backdrop
 * changes or the node moves.
 */
@RequiresApi(33)
internal abstract class GlassPaintingNode : DelegatingNode(), DrawModifierNode, GlobalPositionAwareModifierNode, BackdropReader {

    /** Gets the painter that draws the node's glass. */
    protected val painter: GlassPainter = GlassPainter(this)

    /** Gets where the node stands, or `null` while it is not placed. */
    protected var coordinates: LayoutCoordinates? = null
        private set

    override fun onBackdropChanged() {
        invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        if (painter.place(coordinates)) onMoved()
    }

    /** Called when the node has moved; redraws it. */
    protected open fun onMoved() {
        invalidateDraw()
    }

    /** Releases the painter's layer and forgets where the node stood. */
    protected fun releasePainter() {
        painter.detach()
        coordinates = null
    }
}
