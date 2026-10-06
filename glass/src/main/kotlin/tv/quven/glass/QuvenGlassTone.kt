package tv.quven.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp

/**
 * Describes how glass of one thickness tones the backdrop: it leans it towards [shade], lightened by [adaptation] times
 * how far the luminance of the mean colour of the backdrop under and just about the surface passes [knee], in that
 * colour, by [lean], plus [leanSlope] per unit of luminance, then scales its saturation by [saturation].
 *
 * @property shade The colour the glass leans towards over a black backdrop.
 * @property lean How far the glass leans towards [shade] over a black backdrop, from 0 to 1.
 * @property leanSlope How much further it leans per unit of the backdrop's luminance, 0 for a constant lean.
 * @property saturation The saturation of the backdrop seen through the glass, 1 leaving it unchanged.
 * @property adaptation How far [shade] lightens per unit of the luminance of the mean colour of the backdrop under and
 * just about the surface past [knee], taken in linear light, as Apple's glass grows lighter over brighter content; 0 for
 * a shade that holds.
 * @property knee The mean luminance up to which [shade] holds, from 0 to 1.
 */
@Immutable
public data class QuvenGlassTone(
    val shade: Color,
    val lean: Float,
    val leanSlope: Float = 0f,
    val saturation: Float = 1f,
    val adaptation: Float = 0f,
    val knee: Float = 0f,
) {

    public companion object {
        /** Gets the tone of thin glass, as on a small button or control. */
        public val Thin: QuvenGlassTone = QuvenGlassTone(
            shade = Color(0xFF1A1A1A),
            lean = 0.7f,
            saturation = 2f,
            adaptation = 0.48f,
            knee = 0.25f,
        )

        /** Gets the tone of thick glass, as on a bar, a large button or a panel. */
        public val Thick: QuvenGlassTone = QuvenGlassTone(shade = Color(0xFF272727), lean = 0.77f, saturation = 2.49f)

        /** Gets the tone of large glass, as on a panel, a sheet or a sidebar, veiled more deeply than a bar. */
        public val Large: QuvenGlassTone = QuvenGlassTone(
            shade = Color(0xFF171717),
            lean = 0.86f,
            leanSlope = -0.05f,
            saturation = 2.6f,
            adaptation = 0.144f,
        )

        /**
         * Gets the tone of a menu's or a sheet's large glass, veiled more deeply than a panel and lighter over a brighter
         * screen.
         */
        internal val Menu: QuvenGlassTone = QuvenGlassTone(
            shade = Color(0xFF0C0C0C),
            lean = 0.88f,
            leanSlope = -0.05f,
            saturation = 2.6f,
            adaptation = 0.185f,
        )

        /** Gets the tone of a context menu's glass over the screen it dims, a shade lighter than a pull-down menu's. */
        internal val ContextMenu: QuvenGlassTone = Menu.copy(shade = Color(0xFF191919))

        /** Gets the tone of an alert, which lets more of the dimmed screen's colours through than a menu. */
        internal val Alert: QuvenGlassTone = QuvenGlassTone(
            shade = Color(0xFF1A1A1A),
            lean = 0.76f,
            leanSlope = 0.45f,
            saturation = 2.6f,
            adaptation = 0.16f,
        )

        /** Gets the tone of a split view's sidebar, which lets more of the glow beside it through than a panel. */
        internal val Sidebar: QuvenGlassTone = QuvenGlassTone(
            shade = Color(0xFF151515),
            lean = 0.79f,
            leanSlope = -0.05f,
            saturation = 2.6f,
            adaptation = 0.144f,
        )

        /** Gets the tone of thin glass turned light over a bright backdrop. */
        public val Light: QuvenGlassTone = QuvenGlassTone(shade = Color(0xFFF5F5F5), lean = 0.82f, saturation = 3.27f)

        /** Gets the tone of clear glass, which leans towards no shade and leaves the backdrop's colours as they are. */
        public val Clear: QuvenGlassTone = QuvenGlassTone(shade = Color(0xFF808080), lean = 0f, saturation = 1f)
    }
}

/**
 * Returns the tone [fraction] of the way from [start] to [stop], each property in proportion.
 *
 * @param start The tone at 0.
 * @param stop The tone at 1.
 * @param fraction How far along, from 0 to 1.
 * @return The tone between.
 */
internal fun lerpTone(start: QuvenGlassTone, stop: QuvenGlassTone, fraction: Float): QuvenGlassTone = QuvenGlassTone(
    shade = lerp(start.shade, stop.shade, fraction),
    lean = lerp(start.lean, stop.lean, fraction),
    leanSlope = lerp(start.leanSlope, stop.leanSlope, fraction),
    saturation = lerp(start.saturation, stop.saturation, fraction),
    adaptation = lerp(start.adaptation, stop.adaptation, fraction),
    knee = lerp(start.knee, stop.knee, fraction),
)
