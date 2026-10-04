package tv.quven.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp

/**
 * Draws the static frosted material of [style]: a track of soft white over its ground, rimmed with a light fading
 * towards its foot. Liquid Glass falls back to it below Android 13.
 *
 * @param style The material.
 * @param shape The track's shape.
 * @return The decorated modifier.
 */
public fun Modifier.quvenGlassTrack(style: QuvenGlassStyle = QuvenGlassStyle.Standard, shape: Shape = CircleShape): Modifier = this
    .clip(shape)
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path()
        onDrawBehind { drawFrost(outline, style, path) }
    }

/**
 * Draws the static pill of [style] that marks the held option of a [quvenGlassTrack].
 *
 * @param style The material.
 * @param shape The pill's shape.
 * @return The decorated modifier.
 */
public fun Modifier.quvenGlassPill(style: QuvenGlassStyle = QuvenGlassStyle.Standard, shape: Shape = CircleShape): Modifier = this
    .shadow(style.pillShadow, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path()
        onDrawBehind { drawPillFrost(outline, style, path) }
    }

/**
 * Draws the mark of an option held or opened apart from a track's sliding pill: the platter of [style] where [backdrop]
 * draws Liquid Glass, as the pill of [QuvenGlassSegmentedTrack] does at rest, and [quvenGlassPill] elsewhere.
 *
 * @param style The material.
 * @param backdrop The backdrop the track stands over, or `null` where it draws the static material.
 * @param shape The mark's shape.
 * @param appearance The appearance of the glass the mark stands on, or `null` for glass that never turns light.
 * @return The decorated modifier.
 */
public fun Modifier.quvenGlassMark(
    style: QuvenGlassStyle,
    backdrop: QuvenGlassBackdrop?,
    shape: Shape = CircleShape,
    appearance: QuvenGlassAppearance? = null,
): Modifier =
    if (backdrop != null && QuvenGlass.isLiquidSupported) {
        clip(shape).drawBehind { drawRect(lerp(style.platter, style.lightPlatter, appearance?.lightness ?: 0f)) }
    } else {
        quvenGlassPill(style, shape)
    }
