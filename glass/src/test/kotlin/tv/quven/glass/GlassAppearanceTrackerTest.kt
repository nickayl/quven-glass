package tv.quven.glass

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

    @Test
    fun aBackdropTurningBright_turnsTheAppearanceLight_onceTheBrightnessSettles() = runTest {
        val appearance = QuvenGlassAppearance()
        val samples = ArrayDeque(listOf(0.2f))
        val tracker = GlassAppearanceTracker(brightness = { samples.removeFirstOrNull() ?: 1f }, now = { currentTime })

        withContext(TestMonotonicFrameClock(this)) {
            tracker.start(this, { appearance }, { adaptation })
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
        val tracker = GlassAppearanceTracker(brightness = { if (thick) null else 1f }, now = { currentTime })

        withContext(TestMonotonicFrameClock(this)) {
            tracker.start(this, { appearance }, { adaptation })
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
        val tracker = GlassAppearanceTracker(brightness = { samples++; 1f }, isShown = { false }, now = { currentTime })

        withContext(TestMonotonicFrameClock(this)) {
            tracker.start(this, { appearance }, { adaptation })
            advanceTimeBy(5_000)
            tracker.stop()
        }

        assertEquals(0, samples)
        assertEquals(0f, appearance.lightness)
    }
}
