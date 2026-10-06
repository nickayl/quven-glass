package tv.quven.glass

import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.ui.graphics.Color
import kotlin.math.exp
import kotlin.math.pow

/** Smooths the brightness behind a glass surface and turns its appearance with hysteresis. */
internal class GlassBrightnessFilter {

    /** Gets the smoothed brightness, from 0 to 1, or NaN before the first sample. */
    var brightness: Float = Float.NaN
        private set

    /** Gets a value indicating whether the glass is light. */
    var isLight: Boolean = false
        private set

    /**
     * Takes a sample of the brightness behind the glass.
     *
     * @param sample The brightness, from 0 to 1.
     * @param elapsedMillis The time since the previous sample.
     * @param adaptation The thresholds and the time constant.
     * @return `true` if the glass turned; otherwise, `false`.
     */
    fun take(sample: Float, elapsedMillis: Long, adaptation: QuvenGlassAdaptation): Boolean {
        val share = 1f - exp(-elapsedMillis.toFloat() / adaptation.settleMillis.coerceAtLeast(1))
        brightness = if (brightness.isNaN()) sample else brightness + share * (sample - brightness)
        val light = if (isLight) brightness >= adaptation.darkBelow else brightness > adaptation.lightAbove
        val turned = light != isLight
        isLight = light
        return turned
    }

    /**
     * Forgets the brightness and turns the glass dark.
     *
     * @return `true` if the glass turned; otherwise, `false`.
     */
    fun reset(): Boolean {
        val turned = isLight
        brightness = Float.NaN
        isLight = false
        return turned
    }
}

/**
 * What a glass surface reads of the backdrop under and around it.
 *
 * @property brightness The mean of the colour channels, from 0 to 1, which turns thin glass light.
 * @property veil The mean colour, taken in linear light and encoded back as sRGB, which the glass's tone follows.
 */
internal data class GlassBackdropReading(val brightness: Float, val veil: Color)

/** The mean colour glass assumes around it before it reads any: that of a dark page. */
internal val DefaultVeil: Color = Color(0xFF303030)

/** Converts a veil to the three channels it moves on, in their sRGB encoding. */
internal val VeilConverter: TwoWayConverter<Color, AnimationVector3D> = TwoWayConverter(
    convertToVector = { AnimationVector3D(it.red, it.green, it.blue) },
    convertFromVector = { Color(it.v1.coerceIn(0f, 1f), it.v2.coerceIn(0f, 1f), it.v3.coerceIn(0f, 1f)) },
)

/**
 * Returns the mean colour of ARGB pixels taken in linear light and encoded back as sRGB, so bright content weighs as
 * much as the light it gives and its colour carries, as the tone of Apple's glass follows it.
 *
 * @param pixels The pixels, as packed ARGB integers.
 * @return The mean, or black for no pixels.
 */
internal fun meanLight(pixels: IntArray): Color {
    if (pixels.isEmpty()) return Color.Black
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    for (pixel in pixels) {
        red += LinearLight[pixel shr 16 and 0xFF]
        green += LinearLight[pixel shr 8 and 0xFF]
        blue += LinearLight[pixel and 0xFF]
    }
    return Color(encoded(red / pixels.size), encoded(green / pixels.size), encoded(blue / pixels.size))
}

// Encodes linear light as an sRGB channel.
private fun encoded(light: Double): Float =
    (if (light <= 0.0031308) 12.92 * light else 1.055 * light.pow(1.0 / 2.4) - 0.055).toFloat()

/** The light each sRGB channel value gives, from 0 to 1. */
private val LinearLight = DoubleArray(256) { value ->
    val channel = value / 255.0
    if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}

/**
 * Returns the mean of the colour channels of ARGB pixels, from 0 to 1.
 *
 * @param pixels The pixels, as packed ARGB integers.
 * @return The mean, or 0 for no pixels.
 */
internal fun meanChannels(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 0f
    var sum = 0L
    for (pixel in pixels) sum += (pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)
    return sum / (pixels.size * 3f * 255f)
}
