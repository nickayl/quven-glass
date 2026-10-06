package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TestMonotonicFrameClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class GlassAppearanceTrackerTest {

    private val adaptation = QuvenGlassAdaptation()
    private val veil = Animatable(DefaultVeil, VeilConverter)

    @Test
    fun aBackdropTurningBright_turnsTheAppearanceLight_onceTheBrightnessSettles() = runTest {
        val appearance = QuvenGlassAppearance()
        val samples = ArrayDeque(listOf(0.2f))
        val tracker = GlassAppearanceTracker(brightness = { reading(samples.removeFirstOrNull() ?: 1f) }, now = { currentTime })

        withContext(TestMonotonicFrameClock(this)) {
            tracker.start(this, { appearance }, veil, { adaptation })
            advanceTimeBy(1_000)
            assertEquals(0f, appearance.lightness)
            advanceTimeBy(5_000)
            tracker.stop()
        }

        assertEquals(1f, appearance.lightness)
    }

    @Test
    fun aSurfaceThatCannotTurnLight_turnsTheAppearanceBackDark() = runTest {
        val appearance = QuvenGlassAppearance()
        var thick = false
        val tracker = GlassAppearanceTracker(brightness = { reading(1f) }, canTurn = { !thick }, now = { currentTime })

        withContext(TestMonotonicFrameClock(this)) {
            tracker.start(this, { appearance }, veil, { adaptation })
            advanceTimeBy(1_000)
            assertEquals(1f, appearance.lightness)
            thick = true
            advanceTimeBy(1_000)
            tracker.stop()
        }

        assertEquals(0f, appearance.lightness)
    }

    @Test
    fun aWindowOutOfView_takesNoSample_andTheAppearanceStays() = runTest {
        val appearance = QuvenGlassAppearance()
        var samples = 0
        val tracker = GlassAppearanceTracker(brightness = { samples++; reading(1f) }, isShown = { false }, now = { currentTime })

        withContext(TestMonotonicFrameClock(this)) {
            tracker.start(this, { appearance }, veil, { adaptation })
            advanceTimeBy(5_000)
            tracker.stop()
        }

        assertEquals(0, samples)
        assertEquals(0f, appearance.lightness)
    }

    @Test
    fun theVeil_movesOnToEachNewColour_overTheTimeToTheNext_whetherOrNotTheGlassMayTurn() = runTest {
        val appearance = QuvenGlassAppearance()
        val red = Color(0.6f, 0.2f, 0.1f)
        val readings = ArrayDeque(listOf(red, red, Color(0.601f, 0.2f, 0.1f)))
        val tracker = GlassAppearanceTracker(
            brightness = { GlassBackdropReading(0.9f, readings.removeFirstOrNull() ?: Color(0.601f, 0.2f, 0.1f)) },
            canTurn = { false },
            now = { currentTime },
        )

        withContext(TestMonotonicFrameClock(this)) {
            tracker.start(this, { appearance }, veil, { adaptation })
            advanceTimeBy(750)
            assertEquals((DefaultVeil.red + 0.6f) / 2f, veil.value.red, 0.05f)
            assertEquals((DefaultVeil.blue + 0.1f) / 2f, veil.value.blue, 0.05f)
            advanceTimeBy(1_000)
            tracker.stop()
        }

        // A change of less than a level starts no animation: the tone holds what it shows.
        assertEquals(0.6f, veil.value.red, 1f / 255f)
        assertEquals(0.1f, veil.value.blue, 1f / 255f)
        assertEquals(0f, appearance.lightness)
    }

    private fun reading(brightness: Float) = GlassBackdropReading(brightness, Color(brightness, brightness, brightness))
}
