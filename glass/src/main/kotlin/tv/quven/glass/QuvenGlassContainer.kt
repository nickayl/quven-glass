package tv.quven.glass

import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lays out [content] in a box whose Liquid Glass surfaces are drawn together in the container's [style]: surfaces
 * closer than [spacing] flow into one another along their facing edges, and touch where they swell into each other,
 * as one piece of glass would. Below Android 13, or without a backdrop, every surface draws the static material on
 * its own.
 *
 * @param modifier Modifier applied to the container.
 * @param style The material every surface in the container is drawn in.
 * @param spacing The distance below which two surfaces start to join.
 * @param backdrop The backdrop the surfaces stand over, or `null` to draw the static material.
 * @param content The content, whose [quvenLiquidGlass] surfaces over [backdrop] the container draws.
 */
@Composable
public fun QuvenGlassContainer(
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    spacing: Dp = DefaultGlassSpacing,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    content: @Composable BoxScope.() -> Unit,
) {
    if (backdrop == null || !QuvenGlass.isLiquidSupported) {
        Box(modifier, content = content)
        return
    }
    val state = remember(backdrop) { GlassContainerState(backdrop) }
    CompositionLocalProvider(LocalGlassContainer provides state) {
        Box(modifier.then(GlassContainerElement(state, style, spacing)), content = content)
    }
}

/** The distance below which two surfaces of a [QuvenGlassContainer] start to join, unless the caller names another. */
public val DefaultGlassSpacing: Dp = 16.dp

/**
 * The surfaces a [QuvenGlassContainer] draws, and the node that draws them.
 *
 * @property backdrop The backdrop the surfaces stand over.
 */
internal class GlassContainerState(val backdrop: QuvenGlassBackdrop) {

    private val surfaces = mutableListOf<LiquidGlassNode>()

    /** Gets or sets the node that draws the surfaces. */
    var node: GlassContainerNode? = null

    /**
     * Adds a surface the container draws.
     *
     * @param surface The surface.
     */
    fun add(surface: LiquidGlassNode) {
        surfaces += surface
        invalidate()
    }

    /**
     * Removes a surface added by [add].
     *
     * @param surface The surface.
     */
    fun remove(surface: LiquidGlassNode) {
        surfaces -= surface
        invalidate()
    }

    /** Redraws the surfaces. */
    fun invalidate() {
        node?.takeIf { it.isAttached }?.invalidateDraw()
    }

    /**
     * Returns the surfaces in the coordinates of [target].
     *
     * @param target The container's coordinates.
     * @return The placed surfaces, at most [MaxGlassSurfaces], in the order they were added.
     */
    @RequiresApi(33)
    fun surfacesIn(target: LayoutCoordinates): List<GlassSurface> =
        surfaces.asSequence().mapNotNull { it.surfaceIn(target) }.take(MaxGlassSurfaces).toList()
}

/** Provides the container whose surfaces a [quvenLiquidGlass] node hands its drawing to. */
internal val LocalGlassContainer = staticCompositionLocalOf<GlassContainerState?> { null }

private data class GlassContainerElement(
    val state: GlassContainerState,
    val style: QuvenGlassStyle,
    val spacing: Dp,
) : ModifierNodeElement<GlassContainerNode>() {

    @RequiresApi(33)
    override fun create(): GlassContainerNode = GlassContainerNode(state, style, spacing)

    @RequiresApi(33)
    override fun update(node: GlassContainerNode) {
        node.update(style, spacing)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "quvenGlassContainer"
        properties["style"] = style
        properties["spacing"] = spacing
    }
}

/**
 * Draws the surfaces of a [QuvenGlassContainer] in one Liquid Glass layer, under the container's content.
 *
 * @param state The container's surfaces.
 * @param style The material.
 * @param spacing The distance below which two surfaces start to join.
 */
@RequiresApi(33)
internal class GlassContainerNode(
    private val state: GlassContainerState,
    private var style: QuvenGlassStyle,
    private var spacing: Dp,
) : GlassPaintingNode() {

    /**
     * Applies the arguments of a recomposed container.
     *
     * @param style The material.
     * @param spacing The distance below which two surfaces start to join.
     */
    fun update(style: QuvenGlassStyle, spacing: Dp) {
        this.style = style
        this.spacing = spacing
        invalidateDraw()
    }

    override fun onAttach() {
        state.node = this
        painter.attach(state.backdrop)
        state.backdrop.addReader(this)
    }

    override fun onDetach() {
        if (state.node === this) state.node = null
        state.backdrop.removeReader(this)
        releasePainter()
    }

    override fun ContentDrawScope.draw() {
        coordinates?.takeIf { it.isAttached }?.let { placed ->
            with(painter) { paint(state.surfacesIn(placed), state.backdrop, style, spacing.toPx()) }
        }
        drawContent()
    }
}
