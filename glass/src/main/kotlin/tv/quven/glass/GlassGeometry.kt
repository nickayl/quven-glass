package tv.quven.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A rounded rectangle, the form every glass surface's signed distance field is computed on.
 *
 * @property rect The rectangle, in the coordinates of the node that draws the surface.
 * @property topLeft The radius of the top-left corner.
 * @property topRight The radius of the top-right corner.
 * @property bottomRight The radius of the bottom-right corner.
 * @property bottomLeft The radius of the bottom-left corner.
 */
@Immutable
internal data class GlassForm(
    val rect: Rect,
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
) {

    /**
     * Returns this form grown by [amount] on every side, its radii growing with it as far as half the shorter side.
     *
     * @param amount The distance to grow by; a negative value shrinks the form.
     * @return The grown form.
     */
    fun inflate(amount: Float): GlassForm {
        if (amount == 0f) return this
        val grown = rect.inflate(amount)
        val cap = min(grown.width, grown.height) / 2f
        fun radius(r: Float) = if (r <= 0f) 0f else (r + amount).coerceIn(0f, cap)
        return GlassForm(grown, radius(topLeft), radius(topRight), radius(bottomRight), radius(bottomLeft))
    }

    /**
     * Returns this form grown by [factor] about its centre, its radii with it.
     *
     * @param factor The factor to grow by; 1 leaves the form as it is.
     * @return The grown form.
     */
    fun scaled(factor: Float): GlassForm {
        if (factor == 1f) return this
        val centre = rect.center
        val half = Size(rect.width * factor / 2f, rect.height * factor / 2f)
        return GlassForm(
            Rect(centre.x - half.width, centre.y - half.height, centre.x + half.width, centre.y + half.height),
            topLeft * factor,
            topRight * factor,
            bottomRight * factor,
            bottomLeft * factor,
        )
    }

    /**
     * Returns this form moved by [offset].
     *
     * @param offset The distance to move by.
     * @return The moved form.
     */
    fun translate(offset: Offset): GlassForm = copy(rect = rect.translate(offset))

    /**
     * Returns this form as an outline.
     *
     * @return The rounded outline.
     */
    fun toOutline(): Outline = Outline.Rounded(
        RoundRect(
            rect = rect,
            topLeft = CornerRadius(topLeft),
            topRight = CornerRadius(topRight),
            bottomRight = CornerRadius(bottomRight),
            bottomLeft = CornerRadius(bottomLeft),
        ),
    )

    companion object {
        /**
         * Returns the form [shape] takes at [size], or `null` where the shape is no rounded rectangle.
         *
         * @param shape The shape.
         * @param size The size the shape is laid out at.
         * @param layoutDirection The layout direction the shape is laid out in.
         * @param density The density the shape is laid out at.
         * @return The form, its radii no larger than half its shorter side; `null` for a shape outlined by a path.
         */
        fun of(shape: Shape, size: Size, layoutDirection: LayoutDirection, density: Density): GlassForm? =
            when (val outline = shape.createOutline(size, layoutDirection, density)) {
                is Outline.Rectangle -> GlassForm(outline.rect, 0f, 0f, 0f, 0f)
                is Outline.Rounded -> {
                    val round = outline.roundRect
                    val cap = min(round.width, round.height) / 2f
                    GlassForm(
                        rect = Rect(round.left, round.top, round.right, round.bottom),
                        topLeft = min(round.topLeftCornerRadius.x, cap),
                        topRight = min(round.topRightCornerRadius.x, cap),
                        bottomRight = min(round.bottomRightCornerRadius.x, cap),
                        bottomLeft = min(round.bottomLeftCornerRadius.x, cap),
                    )
                }
                is Outline.Generic -> null
            }
    }
}

/**
 * The margin a set of glass surfaces reads beyond their edges: enough for the blur near an edge to average the backdrop
 * that is really there, and for glass that shows the backdrop smaller to read as far out as its zoom reaches. The fold
 * at the rim reads inwards and needs none.
 *
 * @param forms The surfaces' forms.
 * @param blur The radius of the blur, in pixels.
 * @param zoom How much smaller the glass shows the backdrop, 1 for its own size.
 * @return The margin, in pixels.
 */
internal fun glassMargin(forms: List<GlassForm>, blur: Float, zoom: Float = 1f): Float {
    val reach = (zoom - 1f).coerceAtLeast(0f) * (forms.maxOfOrNull { max(it.rect.width, it.rect.height) / 2f } ?: 0f)
    return max(0f, blur) * 2f + 2f + reach
}

/**
 * Returns the whole pixels a set of forms covers once grown by [margin], the region a glass layer is recorded over.
 *
 * @param forms The forms; at least one.
 * @param margin The margin read beyond the forms' edges.
 * @return The region, its edges rounded outwards to whole pixels.
 * @throws IllegalArgumentException [forms] is empty.
 */
internal fun glassRegion(forms: List<GlassForm>, margin: Float): IntRect {
    require(forms.isNotEmpty()) { "A glass region needs at least one form." }
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    for (form in forms) {
        left = min(left, form.rect.left)
        top = min(top, form.rect.top)
        right = max(right, form.rect.right)
        bottom = max(bottom, form.rect.bottom)
    }
    return IntRect(
        floor(left - margin).toInt(),
        floor(top - margin).toInt(),
        ceil(right + margin).toInt(),
        ceil(bottom + margin).toInt(),
    )
}

/**
 * Returns the smooth step of [value] between [from] and [to]: 0 at or below [from], 1 at or above [to], easing in and
 * out between.
 *
 * @param from The value at which the step begins.
 * @param to The value at which the step ends; greater than [from].
 * @param value The value to step.
 * @return The step, from 0 to 1.
 */
internal fun smoothstep(from: Float, to: Float, value: Float): Float {
    val t = ((value - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Returns how much a pressed control grows, as Apple's glass controls grow under the finger: by the factor that lengthens
 * its longer side by [expansion] once fully pressed, its shorter side growing in proportion.
 *
 * @param lift How far the control is pressed, from 0 to 1, past 1 while its spring overshoots.
 * @param expansion How far the longer side grows once fully pressed, in pixels.
 * @param longerSide The control's longer side at rest, in pixels.
 * @return The factor, 1 at rest.
 */
internal fun pressScale(lift: Float, expansion: Float, longerSide: Float): Float =
    if (longerSide <= 0f) 1f else 1f + lift * expansion / longerSide
