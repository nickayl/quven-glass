package tv.quven.glass.sample

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import tv.quven.glass.QuvenGlassStyle

/** The colours of the sample, a dark palette. */
internal object SampleColors {
    /** Gets the window's ground. */
    val Ground = Color(0xFF0A0A0A)

    /** Gets the colour of text and glyphs. */
    val TextHigh = Color(0xFFF5F5F5)

    /** Gets the colour of secondary text. */
    val TextMedium = Color(0xFFA8A8A8)

    /** Gets the colour of text and glyphs on light glass. */
    val TextHighOnLight = Color(0xFF000000)

    /** Gets the colour of secondary text on light glass. */
    val TextMediumOnLight = Color(0x8C000000)

    /** Gets the colour of a held entry. */
    val Accent = Color(0xFFE8540E)
}

/** The settings the sample's panel tunes while the glass is on screen. */
@Stable
internal class SampleTuning {

    /** Gets or sets the material every surface draws. */
    var style: QuvenGlassStyle by mutableStateOf(QuvenBarStyle)

    /** Gets or sets a value indicating whether the surfaces draw Liquid Glass; otherwise they draw the static material. */
    var liquid: Boolean by mutableStateOf(true)

    /** Gets or sets a value indicating whether motion is reduced. */
    var reduceMotion: Boolean by mutableStateOf(false)

    /** Gets or sets the distance, in density-independent pixels, below which the bar's two surfaces start to join. */
    var spacing: Float by mutableFloatStateOf(8f)

    /** Gets or sets the space, in density-independent pixels, between the bar's capsule and its Search circle. */
    var barGap: Float by mutableFloatStateOf(8f)

    companion object {
        /** Gets the material of a handheld bar: the standard glass, its static form on a dark ground with quieter rims. */
        val QuvenBarStyle = QuvenGlassStyle.Standard.copy(
            ground = SampleColors.Ground.copy(alpha = 0.55f),
            rimTop = Color(0x26FFFFFF),
            rimBottom = Color(0x0AFFFFFF),
            pillRim = Color(0x2EFFFFFF),
        )
    }
}
