package tv.quven.glass

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassBrightnessFilterTest {

    private val adaptation = QuvenGlassAdaptation(lightAbove = 0.74f, darkBelow = 0.64f, settleMillis = 2000)

    @Test
    fun theFirstSample_isTheBrightness_andABrightOneTurnsTheGlassLightAtOnce() {
        val filter = GlassBrightnessFilter()

        val turned = filter.take(sample = 1f, elapsedMillis = 200, adaptation)

        assertEquals(1f, filter.brightness)
        assertTrue(turned)
        assertTrue(filter.isLight)
    }

    @Test
    fun aBriefBrightFlash_overADarkBackdrop_leavesTheGlassDark() {
        val filter = GlassBrightnessFilter()
        filter.take(sample = 0.2f, elapsedMillis = 200, adaptation)

        repeat(3) { filter.take(sample = 1f, elapsedMillis = 200, adaptation) }

        assertFalse(filter.isLight)
    }

    @Test
    fun aBackdropStayingBright_turnsTheGlassLight_onceTheSmoothedBrightnessPassesTheThreshold() {
        val filter = GlassBrightnessFilter()
        filter.take(sample = 0.2f, elapsedMillis = 200, adaptation)

        val turns = (1..20).map { filter.take(sample = 1f, elapsedMillis = 200, adaptation) }

        assertEquals(1, turns.count { it })
        assertTrue(filter.isLight)
        assertTrue(filter.brightness > 0.74f)
    }

    @Test
    fun lightGlass_staysLight_untilTheBrightnessFallsBelowTheLowerThreshold() {
        val filter = GlassBrightnessFilter()
        filter.take(sample = 1f, elapsedMillis = 200, adaptation)

        repeat(40) { filter.take(sample = 0.68f, elapsedMillis = 200, adaptation) }
        assertTrue(filter.isLight)

        repeat(40) { filter.take(sample = 0.4f, elapsedMillis = 200, adaptation) }
        assertFalse(filter.isLight)
    }

    @Test
    fun theMeanOfTheChannels_readsWhiteAsOne_blackAsZero_andYellowBelowTheThreshold() {
        assertEquals(1f, meanChannels(IntArray(4) { 0xFFFFFFFF.toInt() }), 1e-6f)
        assertEquals(0f, meanChannels(IntArray(4) { 0xFF000000.toInt() }), 1e-6f)
        assertEquals((255 + 235) / 765f, meanChannels(intArrayOf(0xFFFFEB00.toInt())), 1e-6f)
        assertEquals(0f, meanChannels(IntArray(0)), 1e-6f)
    }

    @Test
    fun theAppearance_answersTheContentOfDarkGlass_untilItTurns() {
        val appearance = QuvenGlassAppearance()

        assertEquals(Color.White, appearance.contentColor(onDark = Color.White, onLight = Color.Black))
        assertEquals("dark", appearance.pick(onDark = "dark", onLight = "light"))
    }

    @Test
    fun theVeil_isTheMeanColourInLinearLight_encodedBackAsSrgb() {
        assertColour(1f, 1f, 1f, meanLight(IntArray(4) { 0xFFFFFFFF.toInt() }))
        assertColour(0f, 0f, 0f, meanLight(IntArray(4) { 0xFF000000.toInt() }))
        assertColour(0f, 0f, 1f, meanLight(intArrayOf(0xFF0000FF.toInt())))
        assertColour(srgb(0.5), srgb(0.5), 0f, meanLight(intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt())))
        // Half white over black gives half the light, which reads brighter than the middle grey.
        assertColour(srgb(0.5), srgb(0.5), srgb(0.5), meanLight(intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt())))
    }

    @Test
    fun theVeil_ofAPartOfAGrid_readsThatPartAlone() {
        // A grid of three columns and two rows: red, green, blue over black, white, blue.
        val red = 0xFFFF0000.toInt()
        val green = 0xFF00FF00.toInt()
        val blue = 0xFF0000FF.toInt()
        val grid = intArrayOf(red, green, blue, 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), blue)

        assertColour(0f, 0f, 1f, meanLight(grid, stride = 3, columns = 2..2, rows = 0..1))
        assertColour(srgb(0.5), srgb(0.5), 0f, meanLight(grid, stride = 3, columns = 0..1, rows = 0..0))
        assertColour(0f, 0f, 0f, meanLight(grid, stride = 3, columns = 0..0, rows = 1..1))
        assertColour(0f, 0f, 0f, meanLight(grid, stride = 3, columns = IntRange.EMPTY, rows = 0..1))
    }

    // A colour holds each channel in eight bits.
    private fun assertColour(red: Float, green: Float, blue: Float, actual: Color) {
        assertEquals(red, actual.red, ChannelStep)
        assertEquals(green, actual.green, ChannelStep)
        assertEquals(blue, actual.blue, ChannelStep)
    }

    private fun srgb(light: Double): Float = (1.055 * Math.pow(light, 1.0 / 2.4) - 0.055).toFloat()

    private companion object {
        const val ChannelStep = 1f / 255f
    }
}
