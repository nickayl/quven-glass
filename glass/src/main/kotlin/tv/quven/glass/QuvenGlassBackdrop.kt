package tv.quven.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

/**
 * The content glass surfaces stand over: one source records it into a layer every time it draws, and every surface
 * reading the backdrop draws that layer again under itself, bent, blurred and lit.
 *
 * @property seeThrough Whether glass over this backdrop keeps the source's own alpha, letting what lies under the glass
 * show where the source drew nothing: the backdrop a control records its own track into, which a lens over it bends.
 */
@Stable
public class QuvenGlassBackdrop internal constructor(internal val seeThrough: Boolean = false) {

    private val readers = mutableListOf<BackdropReader>()

    /** Gets the layer the source records its content into, or `null` while no source is attached. */
    internal var layer: GraphicsLayer? = null
        private set

    /** Gets the position of the source in the root, or [Offset.Unspecified] before it is placed. */
    internal var origin: Offset = Offset.Unspecified
        private set

    /**
     * Gets where a finger presses the source, in the root, or `null` while none does, which interactive glass letting
     * presses through lights under.
     */
    internal var finger: Offset? by mutableStateOf(null)
        private set

    /**
     * Follows the finger pressing the source.
     *
     * @param at Where the finger presses, in the root, or `null` once it lifts.
     */
    internal fun touch(at: Offset?) {
        finger = at
    }

    /**
     * Attaches a source, which records its content into [layer].
     *
     * @param layer The layer the source records into.
     */
    internal fun attach(layer: GraphicsLayer) {
        this.layer = layer
        notifyReaders()
    }

    /**
     * Detaches the source that records into [layer]; a later source that replaced it stays attached.
     *
     * @param layer The layer the source recorded into.
     */
    internal fun detach(layer: GraphicsLayer) {
        if (this.layer !== layer) return
        this.layer = null
        origin = Offset.Unspecified
        notifyReaders()
    }

    /**
     * Places the source at [origin] in the root.
     *
     * @param origin The source's position in the root.
     */
    internal fun place(origin: Offset) {
        if (this.origin == origin) return
        this.origin = origin
        notifyReaders()
    }

    /**
     * Adds a surface to tell whenever the source attaches, detaches or moves.
     *
     * @param reader The surface.
     */
    internal fun addReader(reader: BackdropReader) {
        readers += reader
    }

    /**
     * Removes a surface added by [addReader].
     *
     * @param reader The surface.
     */
    internal fun removeReader(reader: BackdropReader) {
        readers -= reader
    }

    private fun notifyReaders() {
        for (index in readers.indices.reversed()) readers.getOrNull(index)?.onBackdropChanged()
    }
}

/** A surface that draws a [QuvenGlassBackdrop] again and must redraw when its source changes. */
internal fun interface BackdropReader {

    /** Called when the source attaches, detaches or moves. */
    fun onBackdropChanged()
}

/**
 * Creates and remembers a [QuvenGlassBackdrop].
 *
 * @return The backdrop.
 */
@Composable
public fun rememberQuvenGlassBackdrop(): QuvenGlassBackdrop = remember { QuvenGlassBackdrop() }

/** Provides the backdrop glass surfaces stand over, or `null` where they stand over none and draw the static material. */
public val LocalQuvenGlassBackdrop: ProvidableCompositionLocal<QuvenGlassBackdrop?> = staticCompositionLocalOf { null }

/**
 * Records this node's content into [backdrop] while drawing it as usual, so glass surfaces drawn after it can stand
 * over it. A glass surface inside the source would draw itself into its own backdrop, so it draws the static material.
 *
 * @param backdrop The backdrop to record into.
 * @return The decorated modifier.
 */
public fun Modifier.quvenGlassSource(backdrop: QuvenGlassBackdrop): Modifier = this then GlassSourceElement(backdrop)

private data class GlassSourceElement(val backdrop: QuvenGlassBackdrop) : ModifierNodeElement<GlassSourceNode>() {
    override fun create(): GlassSourceNode = GlassSourceNode(backdrop)

    override fun update(node: GlassSourceNode) {
        node.update(backdrop)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "quvenGlassSource"
        properties["backdrop"] = backdrop
    }
}

/**
 * Returns a value indicating whether this node lies inside the source of [backdrop].
 *
 * @param backdrop The backdrop.
 * @return `true` if an ancestor records into [backdrop]; otherwise, `false`.
 */
internal fun DelegatableNode.isInsideSourceOf(backdrop: QuvenGlassBackdrop): Boolean {
    var inside = false
    traverseAncestors(GlassSourceNode.Key) { ancestor ->
        inside = (ancestor as GlassSourceNode).records(backdrop)
        !inside
    }
    return inside
}

private class GlassSourceNode(private var backdrop: QuvenGlassBackdrop) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode, TraversableNode, PointerInputModifierNode {

    override val traverseKey: Any = Key

    fun records(backdrop: QuvenGlassBackdrop): Boolean = this.backdrop === backdrop

    private var layer: GraphicsLayer? = null

    fun update(backdrop: QuvenGlassBackdrop) {
        if (this.backdrop === backdrop) return
        layer?.let(this.backdrop::detach)
        this.backdrop = backdrop
        layer?.let(backdrop::attach)
    }

    override fun onAttach() {
        val created = requireGraphicsContext().createGraphicsLayer()
        layer = created
        backdrop.attach(created)
    }

    override fun onDetach() {
        backdrop.touch(null)
        layer?.let {
            backdrop.detach(it)
            requireGraphicsContext().releaseGraphicsLayer(it)
        }
        layer = null
    }

    override fun ContentDrawScope.draw() {
        val recording = layer ?: return drawContent()
        recording.record { this@draw.drawContent() }
        drawLayer(recording)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.place(coordinates.positionInRoot())
    }

    // The source watches the finger without taking it, so what it records still answers the press.
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        val origin = backdrop.origin
        backdrop.touch(if (change.pressed && origin.isSpecified) origin + change.position else null)
    }

    override fun onCancelPointerInput() = backdrop.touch(null)

    companion object Key
}
