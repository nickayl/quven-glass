package tv.quven.glass

import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

/** Tells whether this device draws Liquid Glass. */
public object QuvenGlass {

    /**
     * Gets a value indicating whether this device draws Liquid Glass: Android 13 or later, where the glass program
     * builds. Elsewhere every surface draws the static material.
     */
    public val isLiquidSupported: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && LiquidGlassProbe.builds()
    }
}

@RequiresApi(33)
private object LiquidGlassProbe {
    // A driver or runtime that cannot build the program draws the static material rather than failing.
    fun builds(): Boolean = try {
        LiquidGlassShader.Shared
        true
    } catch (refused: IllegalArgumentException) {
        Log.w(Tag, "The Liquid Glass program does not build here; drawing the static material.", refused)
        false
    } catch (missing: LinkageError) {
        Log.w(Tag, "The runtime offers no RuntimeShader; drawing the static material.", missing)
        false
    }

    private const val Tag = "QuvenGlass"
}

/**
 * Draws a set of glass surfaces over a backdrop: records the backdrop under them, with a margin, into a layer of its own
 * at [QuvenGlassStyle.backdropScale] and blurs it there, then draws it back at full size through the Liquid Glass
 * program, over a soft shadow; the outline, the fold and the rim keep the screen's full resolution.
 *
 * @param graphics The graphics context the layers are created in and released to.
 */
@RequiresApi(33)
internal class LiquidGlassRenderer(private val graphics: GraphicsContext) {

    private val backdrop: GraphicsLayer = graphics.createGraphicsLayer()
    private val layer: GraphicsLayer = graphics.createGraphicsLayer()
    private val shader = LiquidGlassShader.Shared
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ShadowFill }
    private val shadowPath = Path()
    private val corners = FloatArray(8)

    /**
     * Draws [surfaces] over [source].
     *
     * @param surfaces The surfaces, in the coordinates of this scope; at least one.
     * @param style The material.
     * @param blend The distance over which two surfaces join, in pixels.
     * @param source The layer the backdrop's source records into.
     * @param sourceOffset The position of this scope's origin in the source.
     * @param seeThrough Whether the source's own alpha holds, letting what lies under the glass show where it is empty.
     */
    fun DrawScope.drawGlass(
        surfaces: List<GlassSurface>,
        style: QuvenGlassStyle,
        blend: Float,
        source: GraphicsLayer,
        sourceOffset: Offset,
        seeThrough: Boolean = false,
    ) {
        val forms = surfaces.map(GlassSurface::form)
        val region = glassRegion(forms, glassMargin(forms, style.blur.toPx(), style.zoom))
        val regionOrigin = Offset(region.left.toFloat(), region.top.toFloat())
        val share = style.backdropScale.coerceIn(MinBackdropScale, 1f)
        backdrop.record(IntSize(ceil(region.width * share).toInt(), ceil(region.height * share).toInt())) {
            scale(share, share, pivot = Offset.Zero) {
                translate(-(sourceOffset.x + regionOrigin.x), -(sourceOffset.y + regionOrigin.y)) { drawLayer(source) }
            }
        }
        val blur = style.blur.toPx() * share
        backdrop.renderEffect = if (blur < MinBlur) null else BlurEffect(blur, blur, TileMode.Clamp)
        backdrop.clip = true
        layer.record(IntSize(region.width, region.height)) {
            scale(1f / share, 1f / share, pivot = Offset.Zero) { drawLayer(backdrop) }
        }
        val local = surfaces.map { surface ->
            surface.copy(form = surface.form.translate(-regionOrigin), pill = surface.pill?.translate(-regionOrigin))
        }
        layer.renderEffect = shader.effect(local, style, blend, density, seeThrough).asComposeRenderEffect()
        // The program's output is unbounded; clipped, it runs over the region alone rather than the whole window.
        layer.clip = true
        layer.topLeft = IntOffset(region.left, region.top)
        drawShadows(surfaces, style, outsideOnly = seeThrough)
        drawLayer(layer)
    }

    /** Releases the layers; the renderer draws nothing afterwards. */
    fun release() {
        graphics.releaseGraphicsLayer(layer)
        graphics.releaseGraphicsLayer(backdrop)
    }

    // Glass that lets the page show through it casts its shadow only outside itself, or the page would darken under it.
    private fun DrawScope.drawShadows(surfaces: List<GlassSurface>, style: QuvenGlassStyle, outsideOnly: Boolean) {
        val radius = style.shadowRadius.toPx()
        if (radius <= 0f || style.shadow.alpha <= 0f) return
        shadowPaint.setShadowLayer(radius, 0f, radius * ShadowDrop, style.shadow.toArgb())
        drawIntoCanvas { canvas ->
            for (surface in surfaces) {
                val form = surface.form
                corners[0] = form.topLeft
                corners[1] = form.topLeft
                corners[2] = form.topRight
                corners[3] = form.topRight
                corners[4] = form.bottomRight
                corners[5] = form.bottomRight
                corners[6] = form.bottomLeft
                corners[7] = form.bottomLeft
                shadowPath.rewind()
                shadowPath.addRoundRect(form.rect.left, form.rect.top, form.rect.right, form.rect.bottom, corners, Path.Direction.CW)
                val native = canvas.nativeCanvas
                if (outsideOnly) {
                    native.save()
                    native.clipOutPath(shadowPath)
                }
                native.drawPath(shadowPath, shadowPaint)
                if (outsideOnly) native.restore()
            }
        }
    }

    private companion object {
        // The shape under the glass is all but transparent: only its shadow layer shows.
        const val ShadowFill = 0x01000000
        const val ShadowDrop = 0.3f
        const val MinBlur = 0.5f
        const val MinBackdropScale = 0.25f
    }
}
