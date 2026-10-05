package tv.quven.glass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * The sizes of a [QuvenGlassButton] holding a label or a glyph, as Apple sizes its glass buttons: the capsule is as tall
 * as its label's line and its padding, and as wide as its content and its padding.
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
    /** The size of a mini control, 28 dp tall, its label smaller than a small control's. */
    Mini(10.dp, 5.dp, 13.sp, 18.sp, 14.dp, 5.dp),

    /** The size of a small control, 28 dp tall. */
    Small(10.dp, 5.dp, 15.sp, 18.sp, 16.dp, 6.dp),

    /** The size of a regular control, 34.5 dp tall. */
    Regular(12.dp, 7.dp, 17.sp, 20.5.sp, 18.dp, 8.dp),

    /** The size of a large control, 50.5 dp tall. */
    Large(20.dp, 15.dp, 17.sp, 20.5.sp, 18.dp, 8.dp),
}

/**
 * Draws a button of glass of [shape] holding [content], as Apple's glass buttons: clear glass that swells and lights
 * what lies under it while pressed, or, given a [tint], a prominent button of glass tinted nearly opaque. Plain glass
 * over a bright backdrop turns light, and the ink handed to [content] turns with it.
 *
 * @param onClick Invoked when the button is pressed.
 * @param modifier Modifier applied to the button, which sizes it.
 * @param shape The button's shape.
 * @param tint The tint of a prominent button, or [Color.Unspecified] for plain glass.
 * @param style The material, which a prominent button tints and a plain one lights under the finger.
 * @param ink The colour of the content over dark glass and over a prominent button.
 * @param lightInk The colour of the content over plain glass turned light.
 * @param contentPadding The room between the button's edge and its content.
 * @param enabled Whether the button can be pressed.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 * @param interactionSource The source of the button's presses and focus, or `null` for one of its own.
 * @param content Draws the button's content, in a row centred in the button, given its ink.
 */
@Composable
public fun QuvenGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    tint: Color = Color.Unspecified,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    ink: Color = Color.White,
    lightInk: Color = Color.Black,
    contentPadding: PaddingValues = NoPadding,
    enabled: Boolean = true,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.(ink: Color) -> Unit,
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val appearance = rememberQuvenGlassAppearance()
    val prominent = tint.isSpecified
    // Under the finger the whole button grows, glass and content alike; where motion is reduced it only lights.
    val glass = remember(style, tint, reduceMotion) {
        style.forButtons(tint).let { if (reduceMotion) it.copy(pressExpansion = 0.dp) else it }
    }
    val press = remember { GlassPress() }
    // The light comes up at once and dies away slowly, long after the button has shrunk back, as Apple's does.
    val glow = remember { GlassPress() }
    LaunchedEffect(interactions, reduceMotion) {
        press.follow(this, interactions) { if (reduceMotion) tween(ReducedMotionFadeMillis) else ButtonPressSpring }
        glow.follow(this, interactions) { held -> if (held) ButtonGlowRise else ButtonGlowFade }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val shownInk = if (prominent) ink else appearance.contentColor(onDark = ink, onLight = lightInk)
    Row(
        modifier = modifier
            .onSizeChanged { size = it }
            .liquidGlass(
                backdrop = backdrop,
                style = glass,
                shape = shape,
                interactionSource = null,
                reduceMotion = reduceMotion,
                lift = remember(press) { GlassLiftSource { press.value } },
                pill = null,
                appearance = appearance.takeUnless { prominent },
                glow = remember(glow) { GlassLiftSource { glow.value } },
            )
            .clickable(interactionSource = interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding)
            .graphicsLayer {
                val grown = pressScale(press.value, glass.pressExpansion.toPx(), max(size.width, size.height).toFloat())
                scaleX = grown
                scaleY = grown
            }
            .alpha(if (enabled) 1f else DisabledAlpha),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(shownInk)
    }
}

/**
 * Draws a capsule of glass holding a label, after a glyph where one is given, as Apple's glass buttons: plain glass that
 * lights under the finger, or a prominent button tinted with [tint].
 *
 * @param onClick Invoked when the button is pressed.
 * @param label The button's label.
 * @param modifier Modifier applied to the button.
 * @param icon The glyph before the label, or `null` for none.
 * @param size The button's size.
 * @param tint The tint of a prominent button, or [Color.Unspecified] for plain glass.
 * @param style The material, which a prominent button tints.
 * @param textStyle The style the label is drawn in; the button sets its size, line height and colour.
 * @param enabled Whether the button can be pressed.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 */
@Composable
public fun QuvenGlassButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    size: QuvenGlassButtonSize = QuvenGlassButtonSize.Regular,
    tint: Color = Color.Unspecified,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    textStyle: TextStyle = TextStyle.Default,
    enabled: Boolean = true,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
) {
    QuvenGlassButton(
        onClick = onClick,
        modifier = modifier,
        tint = tint,
        style = style,
        contentPadding = size.padding,
        enabled = enabled,
        backdrop = backdrop,
        reduceMotion = reduceMotion,
    ) { ink ->
        if (icon != null) ButtonGlyph(icon, ink, size, Modifier.padding(end = size.iconGap))
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

/**
 * Draws a capsule of glass holding a glyph alone, as Apple's glass buttons: plain glass that lights under the finger, or
 * a prominent button tinted with [tint].
 *
 * @param onClick Invoked when the button is pressed.
 * @param icon The button's glyph.
 * @param contentDescription The button's name, or `null` where it is named elsewhere.
 * @param modifier Modifier applied to the button.
 * @param size The button's size.
 * @param tint The tint of a prominent button, or [Color.Unspecified] for plain glass.
 * @param style The material, which a prominent button tints.
 * @param enabled Whether the button can be pressed.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 */
@Composable
public fun QuvenGlassIconButton(
    onClick: () -> Unit,
    icon: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: QuvenGlassButtonSize = QuvenGlassButtonSize.Regular,
    tint: Color = Color.Unspecified,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    enabled: Boolean = true,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
) {
    QuvenGlassButton(
        onClick = onClick,
        modifier = modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        tint = tint,
        style = style,
        contentPadding = size.padding,
        enabled = enabled,
        backdrop = backdrop,
        reduceMotion = reduceMotion,
    ) { ink ->
        ButtonGlyph(icon, ink, size)
    }
}

/**
 * Draws a button's glyph at its size, in its ink.
 *
 * @param icon The glyph.
 * @param ink The button's ink.
 * @param size The button's size.
 * @param modifier Modifier applied to the glyph.
 */
@Composable
private fun ButtonGlyph(icon: Painter, ink: Color, size: QuvenGlassButtonSize, modifier: Modifier = Modifier) {
    Image(icon, contentDescription = null, colorFilter = ColorFilter.tint(ink), modifier = modifier.size(size.iconSize))
}

private val QuvenGlassButtonSize.padding: PaddingValues
    get() = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding)

private val NoPadding = PaddingValues(0.dp)

/** The spring a glass button grows and shrinks on under the finger, passing its size a little, as measured on Apple's. */
internal val ButtonPressSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 685f)

/** How a glass button's light comes up under the finger, as measured on Apple's. */
private val ButtonGlowRise = tween<Float>(70, easing = LinearOutSlowInEasing)

/** How a glass button's light dies away once the finger lifts, as measured on Apple's. */
private val ButtonGlowFade = tween<Float>(450, easing = FastOutSlowInEasing)

/** The opacity of a control that cannot be used, as Apple dims one. */
internal const val DisabledAlpha = 0.4f
