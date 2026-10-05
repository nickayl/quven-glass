package tv.quven.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** The appearance glass takes over its backdrop, which the content drawn on it reads to stay legible. */
@Stable
public class QuvenGlassAppearance {

    internal val turn: Animatable<Float, *> = Animatable(0f)

    // The mean luminance of the backdrop under and around the glass, which its tone follows.
    internal val veil: Animatable<Float, *> = Animatable(DefaultVeil)

    /** Gets how light the glass is, from 0, dark glass, to 1, light glass. */
    public val lightness: Float
        get() = turn.value

    /**
     * Returns the content colour for the glass's appearance.
     *
     * @param onDark The colour over dark glass.
     * @param onLight The colour over light glass.
     * @return The colour, blended while the glass turns.
     */
    public fun contentColor(onDark: Color, onLight: Color): Color = lerp(onDark, onLight, lightness)

    /**
     * Returns the value for the glass's appearance.
     *
     * @param T The type of the value.
     * @param onDark The value over dark glass.
     * @param onLight The value over light glass.
     * @return [onLight] once the glass is more light than dark; otherwise, [onDark].
     */
    public fun <T> pick(onDark: T, onLight: T): T = if (lightness > HalfTurn) onLight else onDark

    internal companion object {
        private const val HalfTurn = 0.5f

        /** The luminance glass assumes around it before it reads any: that of a dark page. */
        const val DefaultVeil = 0.16f
    }
}

/**
 * Creates and remembers a [QuvenGlassAppearance].
 *
 * @return The appearance.
 */
@Composable
public fun rememberQuvenGlassAppearance(): QuvenGlassAppearance = remember { QuvenGlassAppearance() }
