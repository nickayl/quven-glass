package tv.quven.glass

import kotlin.math.exp

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
