package tv.quven.glass

import android.os.SystemClock
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps a surface's [QuvenGlassAppearance] in step with the brightness of the backdrop behind it.
 *
 * @param brightness Reads the brightness behind the surface, from 0 to 1, or `null` while the surface cannot turn light.
 * @param isShown Reads whether the surface's window is in view; out of view, nothing is sampled.
 * @param now Reads the time, in milliseconds.
 */
internal class GlassAppearanceTracker(
    private val brightness: suspend () -> Float?,
    private val isShown: () -> Boolean = { true },
    private val now: () -> Long = SystemClock::uptimeMillis,
) {

    private val filter = GlassBrightnessFilter()
    private var sampling: Job? = null

    /**
     * Starts sampling the backdrop.
     *
     * @param scope The scope the samples are taken and the turns animated in.
     * @param appearance Reads the appearance to keep in step.
     * @param adaptation Reads the thresholds and times.
     */
    fun start(scope: CoroutineScope, appearance: () -> QuvenGlassAppearance, adaptation: () -> QuvenGlassAdaptation) {
        sampling?.cancel()
        sampling = scope.launch {
            var last = now()
            while (isActive) {
                delay(SampleMillis)
                val time = now()
                if (!isShown()) {
                    last = time
                    continue
                }
                val sample = brightness()
                val turned = if (sample == null) filter.reset() else filter.take(sample, time - last, adaptation())
                if (turned) {
                    val target = if (filter.isLight) 1f else 0f
                    launch { appearance().turn.animateTo(target, tween(adaptation().turnMillis)) }
                }
                last = time
            }
        }
    }

    /** Stops sampling. */
    fun stop() {
        sampling?.cancel()
        sampling = null
    }

    private companion object {
        const val SampleMillis = 500L
    }
}
