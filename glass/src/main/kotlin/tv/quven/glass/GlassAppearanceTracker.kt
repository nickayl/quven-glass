package tv.quven.glass

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Keeps a surface in step with the backdrop under and around it: its veil with the luminance, which the tone follows,
 * and its [QuvenGlassAppearance] with the brightness, which turns thin glass light.
 *
 * @param brightness Reads the backdrop, or `null` while the surface neither turns light nor follows its luminance.
 * @param canTurn Reads whether the surface may turn light.
 * @param isShown Reads whether the surface's window is in view; out of view, nothing is sampled.
 * @param now Reads the time, in milliseconds.
 */
internal class GlassAppearanceTracker(
    private val brightness: suspend () -> GlassBackdropReading?,
    private val canTurn: () -> Boolean = { true },
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
     * @param veil The surface's own mean luminance of the backdrop, which its tone follows.
     * @param adaptation Reads the thresholds and times.
     */
    fun start(
        scope: CoroutineScope,
        appearance: () -> QuvenGlassAppearance,
        veil: Animatable<Float, *>,
        adaptation: () -> QuvenGlassAdaptation,
    ) {
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
                val reading = brightness()
                val turned = if (reading == null || !canTurn()) filter.reset() else filter.take(reading.brightness, time - last, adaptation())
                // The tone moves on to each new reading over the time to the next, so it never steps; a reading it already
                // holds starts no animation, which would redraw the glass for nothing.
                if (reading != null && abs(reading.luminance - veil.targetValue) > VeilStep) {
                    launch { veil.animateTo(reading.luminance, tween(SampleMillis.toInt(), easing = LinearEasing)) }
                }
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

        // About one level of eight bits: a change of luminance the tone could not show.
        const val VeilStep = 0.004f
    }
}
