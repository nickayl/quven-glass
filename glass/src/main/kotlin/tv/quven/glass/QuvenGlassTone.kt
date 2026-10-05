package tv.quven.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Describes how glass of one thickness tones the backdrop: it leans it towards [shade] by [lean], plus [leanSlope] per
 * unit of luminance, then scales its saturation by [saturation].
 *
 * @property shade The colour the glass leans towards.
 * @property lean How far the glass leans towards [shade] over a black backdrop, from 0 to 1.
 * @property leanSlope How much further it leans per unit of the backdrop's luminance, 0 for a constant lean.
 * @property saturation The saturation of the backdrop seen through the glass, 1 leaving it unchanged.
 */
@Immutable
public data class QuvenGlassTone(
    val shade: Color,
    val lean: Float,
    val leanSlope: Float = 0f,
    val saturation: Float = 1f,
) {

    public companion object {
        /** Gets the tone of thin glass, as on a small button or control. */
        public val Thin: QuvenGlassTone = QuvenGlassTone(shade = Color(0xFF171717), lean = 0.68f, saturation = 2.2f)

        /** Gets the tone of thick glass, as on a bar, a large button or a panel. */
        public val Thick: QuvenGlassTone = QuvenGlassTone(shade = Color(0xFF272727), lean = 0.77f, saturation = 2.49f)

        /** Gets the tone of thin glass turned light over a bright backdrop. */
        public val Light: QuvenGlassTone = QuvenGlassTone(shade = Color(0xFFF5F5F5), lean = 0.82f, saturation = 3.27f)

        /** Gets the tone of clear glass, which leans towards no shade and leaves the backdrop's colours as they are. */
        public val Clear: QuvenGlassTone = QuvenGlassTone(shade = Color(0xFF808080), lean = 0f, saturation = 1f)
    }
}
