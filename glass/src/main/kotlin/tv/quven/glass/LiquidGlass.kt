package tv.quven.glass

import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    private val touchGlow = Animatable(0f)
    private var touchAt by mutableStateOf(Offset.Zero)
    private var touches: Job? = null
    private var finger: FingerNode? = null
    private var fingerWatch: Job? = null
    private var watchedBackdrop: QuvenGlassBackdrop? = null

    init {
        followFinger()
    }
    private val ownAppearance = QuvenGlassAppearance()
    private val tracker = GlassAppearanceTracker(::backdropReading, ::canTurn, ::isWindowShown)
    private val veil = Animatable(DefaultVeil)
    private var following = false
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
        this.adapts = adapts
        follow()
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            followPresses()
        }
        followFinger()
        invalidateGlass()
    }

    override fun onAttach() {
        register()
        followPresses()
        following = false
        follow()
        followFinger()
    }

    // Glass holding controls takes the pointer, keeping the press for them; interactive glass holding none is no target,
    // as plain glass, and follows the finger its backdrop's source sees, so the press still reaches what lies beneath.
    private fun followFinger() {
        val holds = style.followsFinger && !style.touchPassesThrough
        val held = finger
        if (holds && held == null) {
            finger = delegate(FingerNode(::onFinger, onCancel = { light(on = false) }))
        } else if (!holds && held != null) {
            undelegate(held)
            finger = null
            light(on = false)
        }
        val watches = style.followsFinger && style.touchPassesThrough && isAttached
        if (fingerWatch != null && (!watches || watchedBackdrop !== backdrop)) {
            fingerWatch?.cancel()
            fingerWatch = null
            watchedBackdrop = null
            light(on = false)
        }
        if (watches && fingerWatch == null) {
            watchedBackdrop = backdrop
            fingerWatch = coroutineScope.launch { watchBackdropFinger(backdrop) }
        }
    }

    // Lights where the finger the source sees falls within this surface, and lets the light die once it lifts or leaves.
    private suspend fun watchBackdropFinger(watched: QuvenGlassBackdrop) {
        var lit = false
        snapshotFlow { watched.finger }.collect { at ->
            val own = coordinates?.takeIf { it.isAttached }
            val local = if (at != null && own != null) at - own.positionInRoot() else null
            val inside = local != null && own != null && Rect(Offset.Zero, own.size.toSize()).contains(local)
            if (inside && local != null) {
                touchAt = local
                if (!lit) light(on = true)
            } else if (lit) {
                light(on = false)
            }
            lit = inside
        }
    }

    private fun onFinger(change: PointerInputChange) {
        when {
            change.changedToDownIgnoreConsumed() -> {
                touchAt = change.position
                light(on = true)
            }
            change.changedToUpIgnoreConsumed() -> light(on = false)
            change.pressed -> touchAt = change.position
        }
    }

    // The light gathers at once under the finger and dies away once it lifts, as on Apple's interactive glass; a tap
    // lifting before it has gathered still lights it whole first.
    private fun light(on: Boolean) {
        val gathering = !on && touchGlow.targetValue == 1f && touchGlow.value < 1f
        touches?.cancel()
        touches = coroutineScope.launch {
            if (gathering) touchGlow.animateTo(1f, TouchLightRise)
            touchGlow.animateTo(if (on) 1f else 0f, if (on) TouchLightRise else TouchLightFade)
        }
    }

    override fun onDetach() {
        unregister()
        press.stop()
        touches?.cancel()
        fingerWatch?.cancel()
        fingerWatch = null
        watchedBackdrop = null
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
        // A press springing back passes below rest, so the surface shrinks a little before it settles; its light holds
        // at 0.
        val springing = liftSource?.lift()?.takeIf { abs(it) > abs(press.value) } ?: press.value
        val lift = springing.coerceAtLeast(0f)
        val swell = springing * style.pressGrowth * min(size.width, size.height) / 2f
        val grown = pressScale(springing, with(requireDensity()) { style.pressExpansion.toPx() }, max(size.width, size.height))
        val pill = pillSource?.pill(size)?.translate(offset)
        val glow = glowSource?.lift() ?: lift
        val touch = touchGlow.value.takeIf { it > 0f && style.followsFinger }?.let { amount ->
            TouchLight(touchAt + offset, with(requireDensity()) { style.touchLightSpread.toPx() }, amount)
        } ?: TouchLight.None
        return GlassSurface(form.inflate(swell).scaled(grown).translate(offset), lift, pill, shownAppearance().lightness, veil.value, glow, style.pressWhite, touch)
    }

    private fun shownAppearance(): QuvenGlassAppearance = appearance ?: ownAppearance

    // Glass reads the backdrop while it may turn light or its tone follows the backdrop's luminance.
    private fun follow() {
        val wanted = adapts || style.followsBrightness
        if (wanted == following) return
        following = wanted
        if (wanted) tracker.start(coroutineScope, ::shownAppearance, veil) { style.adaptation } else tracker.stop()
    }

    private fun isWindowShown(): Boolean = currentValueOf(LocalView).windowVisibility == View.VISIBLE

    // Only thin glass turns light.
    private fun canTurn(): Boolean {
        val own = coordinates?.takeIf { it.isAttached } ?: return false
        return adapts && own.size.toSize().minDimension < with(requireDensity()) { style.thickSize.toPx() }
    }

    // The backdrop is read under the surface and AdaptationReach around it, as far as the backdrop goes, and only where
    // the glass turns light or its tone follows the backdrop at its size: a probe per surface is not free.
    private suspend fun backdropReading(): GlassBackdropReading? {
        val own = coordinates?.takeIf { it.isAttached && !painter.isInsideSource } ?: return null
        val density = requireDensity()
        if (!canTurn() && style.adaptationFor(own.size.toSize().minDimension, density) == 0f) return null
        val source = backdrop.layer ?: return null
        val origin = backdrop.origin.takeUnless { it.isUnspecified } ?: return null
        val around = Rect(own.positionInRoot() - origin, own.size.toSize()).inflate(with(density) { AdaptationReach.toPx() })
        val area = around.intersect(Rect(Offset.Zero, source.size.toSize()))
        val probe = probe ?: GlassBrightnessProbe().also { probe = it }
        return probe.sample(source, area, density)
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
        if (isAttached) press.follow(coroutineScope, interactionSource) { held -> PressSprings(style.slideSpring()).spec(held, reduceMotion) }
    }
}

// Measured on an iPad: a tone follows the brightness of the backdrop this far around its surface as well as under it.
private val AdaptationReach = 40.dp

// Measured on Apple's interactive glass on an iPhone.
private val TouchLightRise = tween<Float>(80)
private val TouchLightFade = tween<Float>(450, easing = FastOutSlowInEasing)

/**
 * Follows the first finger on a glass surface without consuming it, for the light that gathers under it.
 *
 * @param onChange Invoked with the finger's change in the initial pass of every pointer event.
 * @param onCancel Invoked when the pointer input is cancelled.
 */
private class FingerNode(
    private val onChange: (PointerInputChange) -> Unit,
    private val onCancel: () -> Unit,
) : Modifier.Node(), PointerInputModifierNode {

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        pointerEvent.changes.firstOrNull()?.let(onChange)
    }

    override fun onCancelPointerInput() = onCancel()

}
