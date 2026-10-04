package tv.quven.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The sizes of a [QuvenGlassButton], as Apple sizes its glass buttons: the capsule is as tall as its label's line and
 * its padding, and as wide as its content and its padding.
 *
 * @property horizontalPadding The room on either side of the content.
 * @property verticalPadding The room above and below the content.
 * @property labelSize The size of the label.
 * @property lineHeight The height of the label's line.
 * @property iconSize The size of the glyph.
 * @property iconGap The room between the glyph and the label.
 */
public enum class QuvenGlassButtonSize(
    public val horizontalPadding: Dp,
    public val verticalPadding: Dp,
    public val labelSize: TextUnit,
    public val lineHeight: TextUnit,
    public val iconSize: Dp,
    public val iconGap: Dp,
) {
    /** The size of a small control, 28 dp tall. */
    Small(10.dp, 5.dp, 15.sp, 18.sp, 16.dp, 6.dp),

    /** The size of a regular control, 34.5 dp tall. */
    Regular(12.dp, 7.dp, 17.sp, 20.5.sp, 18.dp, 8.dp),

    /** The size of a large control, 50.5 dp tall. */
    Large(20.dp, 15.dp, 17.sp, 20.5.sp, 18.dp, 8.dp),
}

/**
 * Draws a capsule of glass holding a glyph, a label or both, as Apple's glass buttons: clear glass that swells and
 * lights what lies under it while pressed, or, given a [tint], a prominent button of glass tinted nearly opaque. Plain
 * glass over a bright backdrop turns light, and its content turns dark with it.
 *
 * @param onClick Invoked when the button is pressed.
 * @param modifier Modifier applied to the button.
 * @param label The button's label, or `null` for a glyph alone.
 * @param icon The button's glyph, or `null` for a label alone.
 * @param contentDescription The button's name where it carries no label, or `null` where the label names it.
 * @param size The button's size.
 * @param tint The tint of a prominent button, or [Color.Unspecified] for plain glass.
 * @param style The material, which a prominent button tints.
 * @param textStyle The style the label is drawn in; the button sets its size, line height and colour.
 * @param enabled Whether the button can be pressed.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 * @throws IllegalArgumentException Neither [label] nor [icon] is given.
 */
@Composable
public fun QuvenGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: Painter? = null,
    contentDescription: String? = null,
    size: QuvenGlassButtonSize = QuvenGlassButtonSize.Regular,
    tint: Color = Color.Unspecified,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    textStyle: TextStyle = TextStyle.Default,
    enabled: Boolean = true,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
) {
    require(label != null || icon != null) { "A glass button holds a label, a glyph or both." }
    val interactions = remember { MutableInteractionSource() }
    val appearance = rememberQuvenGlassAppearance()
    val prominent = tint.isSpecified
    // A prominent button keeps its tint under the finger; plain glass lights what it stands over.
    val glass = remember(style, tint) { if (prominent) style.tinted(tint) else style.copy(pressGlow = ButtonPressGlow) }
    val ink = if (prominent) Color.White else appearance.contentColor(onDark = Color.White, onLight = Color.Black)
    Row(
        modifier = modifier
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
            .quvenLiquidGlass(
                backdrop = backdrop,
                style = glass,
                shape = CircleShape,
                interactionSource = interactions,
                reduceMotion = reduceMotion,
                appearance = appearance.takeUnless { prominent },
            )
            .clickable(interactionSource = interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding)
            .alpha(if (enabled) 1f else DisabledAlpha),
        horizontalArrangement = Arrangement.spacedBy(size.iconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, contentDescription = null, colorFilter = ColorFilter.tint(ink), modifier = Modifier.size(size.iconSize))
        }
        if (label != null) {
            BasicText(
                label,
                style = textStyle.merge(
                    TextStyle(
                        color = ink,
                        fontSize = size.labelSize,
                        lineHeight = size.lineHeight,
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                    ),
                ),
                maxLines = 1,
            )
        }
    }
}

/** How brightly a pressed glass button lights what lies under it, as measured on Apple's. */
internal const val ButtonPressGlow = 3.6f

private const val DisabledAlpha = 0.4f
