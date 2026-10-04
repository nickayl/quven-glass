package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

/** The length of the fade that stands in for a spring where motion is reduced. */
internal const val ReducedMotionFadeMillis = 160

/**
 * How far a glass surface is pressed, from 0 to 1: it rises on the material's spring while any press holds and falls
 * once the last one ends, with a short fade instead where motion is reduced.
 */
internal class GlassPress {

    private val press = Animatable(0f)
    private var presses: Job? = null

    /** Gets how far the surface is pressed, from 0 to 1. */
    val value: Float
        get() = press.value

    /**
     * Follows the presses of [source], replacing any source followed before.
     *
     * @param scope The scope the presses are collected in.
     * @param source The source of the presses, or `null` to follow none.
     * @param spec Gets the animation the press rises on, given `true`, and falls on, given `false`.
     */
    fun follow(scope: CoroutineScope, source: InteractionSource?, spec: (held: Boolean) -> AnimationSpec<Float>) {
        presses?.cancel()
        presses = source?.let {
            scope.launch {
                var held = 0
                it.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> held++
                        is PressInteraction.Release, is PressInteraction.Cancel -> held = max(0, held - 1)
                    }
                    launch { press.animateTo(if (held > 0) 1f else 0f, spec(held > 0)) }
                }
            }
        }
    }

    /** Stops following presses. */
    fun stop() {
        presses?.cancel()
        presses = null
    }

    companion object {
        /**
         * Returns the animation a press of [style] rises and falls on.
         *
         * @param style The material, whose spring a press follows.
         * @param reduceMotion Whether motion is reduced, which turns the spring into a short fade.
         * @return The animation.
         */
        fun spec(style: QuvenGlassStyle, reduceMotion: Boolean): AnimationSpec<Float> =
            if (reduceMotion) tween(ReducedMotionFadeMillis) else style.slideSpring()
    }
}
