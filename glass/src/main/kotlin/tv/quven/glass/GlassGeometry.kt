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
 * The margin a glass surface reads beyond its edge, so the blur near the edge averages the backdrop that is really
 * there; the lens folds the inside of the glass and reads nothing beyond it.
 *
 * @param blur The radius of the blur, in pixels.
 * @return The margin, in pixels.
 */
internal fun glassMargin(blur: Float): Float = max(0f, blur) * 2f + 2f

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
