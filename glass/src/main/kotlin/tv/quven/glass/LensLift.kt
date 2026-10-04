package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable

/**
 * How far a control's thumb has lifted into its lens: up while the thumb is held, dragged or travelling, and back down
 * a moment after it rests, as Apple's switches and sliders lift theirs.
 */
@Stable
internal class LensLift {

    private val lift = Animatable(0f)

    /** Gets how far the thumb has lifted into its lens, from 0 to 1. */
    val value: Float
        get() = lift.value

    /**
     * Lifts the thumb into its lens when [lifted], or lets it settle back after a short hold; cancelled when [lifted]
     * changes, which keeps a thumb that is lifted again from settling.
     *
     * @param lifted Whether the thumb is held, dragged or travelling.
     */
    suspend fun follow(lifted: Boolean) {
        val spec = if (lifted) tween<Float>(LiftMillis, easing = FastOutSlowInEasing) else tween(SettleMillis, SettleHoldMillis, FastOutSlowInEasing)
        lift.animateTo(if (lifted) 1f else 0f, spec)
    }

    private companion object {
        const val LiftMillis = 150
        const val SettleHoldMillis = 60
        const val SettleMillis = 200
    }
}
