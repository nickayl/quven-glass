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
}
