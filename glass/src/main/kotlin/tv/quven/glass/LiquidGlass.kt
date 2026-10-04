package tv.quven.glass

import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min

/**
 * Draws Liquid Glass of [style] under this node's content: the [backdrop] seen through glass of [shape] that folds it
 * towards the rim, blurs it, parts its colours along the rim, tones it and catches the light on its rim. A press on
 * [interactionSource] swells the glass on a spring; thin glass over a bright backdrop turns light, as [appearance]
 * tells the content.
 *
 * Inside a [QuvenGlassContainer] standing over the same backdrop, the container draws this surface, joined with its
 * neighbours. Without a backdrop, or below Android 13, the node draws [quvenGlassTrack].
 *
 * @param backdrop The backdrop to stand over, or `null` to draw the static material.
 * @param style The material.
 * @param shape The glass's shape; a shape outlined by a path draws the static material.
 * @param interactionSource The source of the presses that swell the glass, or `null` for a glass that never swells.
 * @param reduceMotion Whether motion is reduced, which turns the swelling spring into a short fade.
 * @param appearance The appearance the glass reports to its content, or `null` where the content does not read it.
 * @return The decorated modifier.
 */
public fun Modifier.quvenLiquidGlass(
    backdrop: QuvenGlassBackdrop?,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    shape: Shape = CircleShape,
    interactionSource: InteractionSource? = null,
    reduceMotion: Boolean = false,
    appearance: QuvenGlassAppearance? = null,
): Modifier = liquidGlass(backdrop, style, shape, interactionSource, reduceMotion, lift = null, pill = null, appearance)

/**
 * Draws Liquid Glass as [quvenLiquidGlass] does, swollen by [lift] as well as by its presses and carrying [pill].
 *
 * @param backdrop The backdrop to stand over, or `null` to draw the static material.
 * @param style The material.
 * @param shape The glass's shape.
 * @param interactionSource The source of the presses that swell the glass, or `null` for none.
 * @param reduceMotion Whether motion is reduced.
 * @param lift Reads how far the surface is lifted beyond its own presses, or `null` for never.
 * @param pill Reads the pill inside the surface, or `null` for none.
 * @param appearance The appearance the glass reports to its content, or `null` for none.
 * @param adapts Whether thin glass turns light over a bright backdrop; `false` for glass that is never thin for long.
 * @param glow Reads how brightly the surface lights under the finger, or `null` to light it as far as it is lifted.
 * @return The decorated modifier.
 */
internal fun Modifier.liquidGlass(
    backdrop: QuvenGlassBackdrop?,
    style: QuvenGlassStyle,
    shape: Shape,
    interactionSource: InteractionSource?,
    reduceMotion: Boolean,
    lift: GlassLiftSource?,
    pill: GlassPillSource?,
    appearance: QuvenGlassAppearance? = null,
    adapts: Boolean = true,
    glow: GlassLiftSource? = null,
): Modifier =
    if (backdrop == null || !QuvenGlass.isLiquidSupported) {
        quvenGlassTrack(style, shape)
    } else {
        this then LiquidGlassElement(backdrop, style, shape, interactionSource, reduceMotion, lift, pill, appearance, adapts, glow)
    }

/** Reads how far a surface is lifted, from 0 to 1. */
internal fun interface GlassLiftSource {

    /**
     * Returns how far the surface is lifted.
     *
     * @return The lift, from 0 to 1.
     */
    fun lift(): Float
}

/** Reads the pill inside a surface. */
internal fun interface GlassPillSource {

    /**
     * Returns the pill inside a surface of [size].
     *
     * @param size The surface's size.
     * @return The pill in the surface's coordinates, or `null` for none.
     */
    fun pill(size: Size): GlassPill?
}

private data class LiquidGlassElement(
    val backdrop: QuvenGlassBackdrop,
    val style: QuvenGlassStyle,
    val shape: Shape,
    val interactionSource: InteractionSource?,
    val reduceMotion: Boolean,
    val lift: GlassLiftSource?,
    val pill: GlassPillSource?,
    val appearance: QuvenGlassAppearance?,
    val adapts: Boolean,
    val glow: GlassLiftSource?,
) : ModifierNodeElement<LiquidGlassNode>() {

    @RequiresApi(33)
    override fun create(): LiquidGlassNode =
        LiquidGlassNode(backdrop, style, shape, interactionSource, reduceMotion, lift, pill, appearance, adapts, glow)

    @RequiresApi(33)
    override fun update(node: LiquidGlassNode) {
        node.update(backdrop, style, shape, interactionSource, reduceMotion, lift, pill, appearance, adapts, glow)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "quvenLiquidGlass"
        properties["style"] = style
        properties["shape"] = shape
    }
}

/**
 * Draws one Liquid Glass surface, or hands it to the [QuvenGlassContainer] it stands in.
 *
 * @param backdrop The backdrop to stand over.
 * @param style The material.
 * @param shape The glass's shape.
 * @param interactionSource The source of the presses that swell the glass, or `null` for none.
 * @param reduceMotion Whether motion is reduced.
 * @param liftSource Reads how far the surface is lifted beyond its own presses, or `null` for never.
 * @param pillSource Reads the pill inside the surface, or `null` for none.
 * @param appearance The appearance the glass reports to its content, or `null` for none.
 * @param adapts Whether thin glass turns light over a bright backdrop.
 * @param glowSource Reads how brightly the surface lights under the finger, or `null` to light it as far as it is lifted.
 */
@RequiresApi(33)
internal class LiquidGlassNode(
    private var backdrop: QuvenGlassBackdrop,
    private var style: QuvenGlassStyle,
    private var shape: Shape,
    private var interactionSource: InteractionSource?,
    private var reduceMotion: Boolean,
    private var liftSource: GlassLiftSource?,
    private var pillSource: GlassPillSource?,
    private var appearance: QuvenGlassAppearance?,
    private var adapts: Boolean,
    private var glowSource: GlassLiftSource?,
) : GlassPaintingNode(), CompositionLocalConsumerModifierNode {

    private val press = GlassPress()
    private val ownAppearance = QuvenGlassAppearance()
    private val tracker = GlassAppearanceTracker(::backdropBrightness, ::isWindowShown)
    private var probe: GlassBrightnessProbe? = null
    private var container: GlassContainerState? = null

    /**
     * Applies the arguments of a recomposed modifier.
     *
     * @param backdrop The backdrop to stand over.
     * @param style The material.
     * @param shape The glass's shape.
     * @param interactionSource The source of the presses that swell the glass, or `null` for none.
     * @param reduceMotion Whether motion is reduced.
     * @param liftSource Reads how far the surface is lifted beyond its own presses, or `null` for never.
     * @param pillSource Reads the pill inside the surface, or `null` for none.
     * @param appearance The appearance the glass reports to its content, or `null` for none.
     * @param adapts Whether thin glass turns light over a bright backdrop.
     * @param glowSource Reads how brightly the surface lights under the finger, or `null` to light it as far as it is
     * lifted.
     */
    fun update(
        backdrop: QuvenGlassBackdrop,
        style: QuvenGlassStyle,
        shape: Shape,
        interactionSource: InteractionSource?,
        reduceMotion: Boolean,
        liftSource: GlassLiftSource?,
        pillSource: GlassPillSource?,
        appearance: QuvenGlassAppearance?,
        adapts: Boolean,
        glowSource: GlassLiftSource?,
    ) {
        if (this.backdrop !== backdrop) {
            unregister()
            this.backdrop = backdrop
            register()
        }
        this.style = style
        this.shape = shape
        this.reduceMotion = reduceMotion
        this.liftSource = liftSource
        this.pillSource = pillSource
        this.glowSource = glowSource
        this.appearance = appearance
        if (this.adapts != adapts) {
            this.adapts = adapts
            if (adapts) followBrightness() else tracker.stop()
        }
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            followPresses()
        }
        invalidateGlass()
    }

    override fun onAttach() {
        register()
        followPresses()
        if (adapts) followBrightness()
    }

    override fun onDetach() {
        unregister()
        press.stop()
        tracker.stop()
        probe?.release()
        probe = null
        releasePainter()
    }

    override fun onMoved() {
        invalidateGlass()
    }

    /**
     * Returns this surface in the coordinates of [target], as its container draws it.
     *
     * @param target The container's coordinates.
     * @return The surface, or `null` while it is not placed or its shape is no rounded rectangle.
     */
    fun surfaceIn(target: LayoutCoordinates): GlassSurface? {
        val own = coordinates?.takeIf { it.isAttached } ?: return null
        return surface(own.size.toSize(), target.localPositionOf(own, Offset.Zero))
    }

    override fun ContentDrawScope.draw() {
        if (container == null) {
            surface(size, Offset.Zero)?.let { with(painter) { paint(listOf(it), backdrop, style, blend = 0f) } }
        }
        drawContent()
    }

    private fun surface(size: Size, offset: Offset): GlassSurface? {
        // An empty surface would still join its neighbours at the point it stands on.
        if (size.minDimension <= 0f) return null
        val form = GlassForm.of(shape, size, requireLayoutDirection(), requireDensity()) ?: return null
        val lift = max(press.value, liftSource?.lift() ?: 0f)
        val swell = lift * style.pressGrowth * min(size.width, size.height) / 2f
        val grown = pressScale(lift, with(requireDensity()) { style.pressExpansion.toPx() }, max(size.width, size.height))
        val pill = pillSource?.pill(size)?.translate(offset)
        val glow = glowSource?.lift() ?: lift
        return GlassSurface(form.inflate(swell).scaled(grown).translate(offset), lift, pill, shownAppearance().lightness, glow)
    }

    private fun shownAppearance(): QuvenGlassAppearance = appearance ?: ownAppearance

    private fun followBrightness() = tracker.start(coroutineScope, ::shownAppearance) { style.adaptation }

    private fun isWindowShown(): Boolean = currentValueOf(LocalView).windowVisibility == View.VISIBLE

    // Only thin glass turns light, so a thick surface reads nothing back.
    private suspend fun backdropBrightness(): Float? {
        val own = coordinates?.takeIf { it.isAttached && !painter.isInsideSource } ?: return null
        val size = own.size.toSize()
        if (size.minDimension >= with(requireDensity()) { style.thickSize.toPx() }) return null
        val source = backdrop.layer ?: return null
        val origin = backdrop.origin.takeUnless { it.isUnspecified } ?: return null
        val probe = probe ?: GlassBrightnessProbe().also { probe = it }
        return probe.sample(source, Rect(own.positionInRoot() - origin, size), requireDensity())
    }

    private fun register() {
        painter.attach(backdrop)
        if (painter.isInsideSource) return
        container = currentValueOf(LocalGlassContainer)?.takeIf { it.backdrop === backdrop }
        container?.add(this) ?: backdrop.addReader(this)
    }

    private fun unregister() {
        if (!painter.isInsideSource) container?.remove(this) ?: backdrop.removeReader(this)
        container = null
    }

    private fun invalidateGlass() {
        container?.invalidate() ?: invalidateDraw()
    }

    private fun followPresses() {
        if (isAttached) press.follow(coroutineScope, interactionSource) { GlassPress.spec(style, reduceMotion) }
    }
}
