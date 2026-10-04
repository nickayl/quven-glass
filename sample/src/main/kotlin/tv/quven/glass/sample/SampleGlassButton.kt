package tv.quven.glass.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tv.quven.glass.QuvenGlassButton

/**
 * Draws a round glass button holding [face], in the sample's material and inks.
 *
 * @param onClick Invoked when the button is pressed.
 * @param tuning The live settings.
 * @param modifier Modifier applied to the button.
 * @param diameter The button's diameter.
 * @param shown Whether the button shows; a button hides while the glass it opens into stands in for it.
 * @param face Draws the button's face in the ink it receives.
 */
@Composable
internal fun SampleGlassButton(
    onClick: () -> Unit,
    tuning: SampleTuning,
    modifier: Modifier = Modifier,
    diameter: Dp = ButtonDiameter,
    shown: Boolean = true,
    face: @Composable (ink: Color) -> Unit,
) {
    QuvenGlassButton(
        onClick = onClick,
        modifier = modifier.size(diameter).graphicsLayer { alpha = if (shown) 1f else 0f },
        style = tuning.style,
        ink = SampleColors.TextHigh,
        lightInk = SampleColors.TextHighOnLight,
        reduceMotion = tuning.reduceMotion,
    ) { ink -> face(ink) }
}

/**
 * Draws a glyph in the middle of the space it is given, as the face of a button.
 *
 * @param icon The glyph.
 * @param contentDescription The glyph's name, or `null` where the control is named elsewhere.
 * @param tint The glyph's colour.
 * @param size The glyph's size.
 */
@Composable
internal fun GlyphFace(icon: ImageVector, contentDescription: String?, tint: Color = SampleColors.TextHigh, size: Dp = GlyphSize) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
    }
}

/** The diameter of a glass button, unless the caller names another. */
internal val ButtonDiameter = 56.dp

private val GlyphSize = 24.dp
