package tv.quven.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

/**
 * Lays out the entries of a glass menu, the content a [QuvenGlassMorph] opens into: [QuvenGlassMenuTitle],
 * [QuvenGlassMenuItem], [QuvenGlassMenuChoice] and [QuvenGlassMenuDivider], one under another. The menu is as wide as
 * its longest row, between the widths [metrics] names. A finger that slides along the menu lights the row under it,
 * with a tick each time it reaches another, and chooses the row it lifts over, as on a system menu.
 *
 * @param modifier Modifier applied to the menu's column.
 * @param metrics The layout of the rows; the morph opening the menu takes its corner radius.
 * @param colors The colours of the rows.
 * @param textStyle The style the rows' names and the titles are drawn in; the menu sets their size and colour.
 * @param onChosen Invoked after any row is chosen, as a system menu closes on a choice.
 * @param rising Whether the menu rises from its control, which lists an untitled menu's entries from the foot up, the
 * first nearest the finger, as a system menu does.
 * @param content The menu's entries.
 */
@Composable
public fun QuvenGlassMenu(
    modifier: Modifier = Modifier,
    metrics: QuvenGlassMenuMetrics = QuvenGlassMenuMetrics.Phone,
    colors: QuvenGlassMenuColors = QuvenGlassMenuColors.Standard,
    textStyle: TextStyle = TextStyle.Default,
    onChosen: () -> Unit = {},
    rising: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val touch = remember { MenuTouch() }
    val chosen by rememberUpdatedState(onChosen)
    val menu = remember(metrics, colors, textStyle, touch) { OpenMenu(metrics, colors, textStyle, touch) { chosen() } }
    val haptics = LocalHapticFeedback.current
    val wash by animateFloatAsState(
        if (touch.isTouched) 1f else 0f,
        tween(if (touch.isTouched) WashInMillis else WashOutMillis),
        label = "wash",
    )
    CompositionLocalProvider(LocalOpenMenu provides menu) {
        Column(
            modifier
                .widthIn(min = metrics.width + menu.choiceShift, max = metrics.maxWidth)
                .width(IntrinsicSize.Max)
                .onPlaced { touch.menu = it }
                .pointerInput(touch, haptics) { scrubRows(touch, haptics) }
                .drawBehind { if (wash > 0f) drawRect(colors.pressWash, alpha = wash) }
                .padding(vertical = metrics.verticalInset),
            verticalArrangement = if (rising && touch.titles == 0) FromTheFoot else Arrangement.Top,
            content = content,
        )
    }
}

/**
 * Draws a section's title in a [QuvenGlassMenu].
 *
 * @param text The title.
 * @param modifier Modifier applied to the title's row.
 */
@Composable
public fun QuvenGlassMenuTitle(text: String, modifier: Modifier = Modifier) {
    val menu = LocalOpenMenu.current
    val metrics = menu.metrics
    menu.counts(menu.touch::titles)
    Layout(
        content = { MenuText(text, menu.colors.title, metrics.titleSize, menu) },
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(horizontal = metrics.sideInset),
    ) { measurables, constraints ->
        val title = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val height = (metrics.titleAbove + metrics.titleBelow).roundToPx()
        layout(maxOf(constraints.minWidth, title.width), height) {
            title.placeRelative(0, (metrics.titleAbove.toPx() - title.height / 2f).roundToInt())
        }
    }
}

/**
 * Draws one row of a [QuvenGlassMenu]: its glyph, its name and an optional trailing glyph, lit while pressed. In a menu
 * holding a choice the row moves past the column of checks.
 *
 * @param label The row's name.
 * @param onClick Invoked when the row is chosen.
 * @param modifier Modifier applied to the row.
 * @param icon The row's glyph, drawn in the row's colour, or `null` for none.
 * @param enabled Whether the row can be chosen; a disabled one is drawn dimmed and lights for no press.
 * @param destructive Whether the row's action cannot be undone, which draws it in the destructive colour.
 * @param color The colour of the row's name and glyphs, or [Color.Unspecified] for the menu's.
 * @param trailingIcon A glyph drawn at the row's end, or `null` for none.
 */
@Composable
public fun QuvenGlassMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    color: Color = Color.Unspecified,
    trailingIcon: Painter? = null,
) {
    val menu = LocalOpenMenu.current
    val metrics = menu.metrics
    val ink = menu.inkOf(enabled, color.takeOrElse { if (destructive) menu.colors.destructive else menu.colors.label })
    if (icon != null) menu.counts(menu.touch::glyphs)
    MenuRowBody(onClick, enabled, Role.Button, modifier) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = metrics.iconCentre + menu.choiceShift - metrics.iconSize / 2)
                    .size(metrics.iconSize),
                colorFilter = ColorFilter.tint(ink),
            )
        }
        val trailingRoom = if (trailingIcon != null) metrics.iconSize + TrailingGap else 0.dp
        MenuLabel(label, ink, start = menu.labelStart, end = metrics.sideInset + trailingRoom)
        if (trailingIcon != null) {
            Image(
                painter = trailingIcon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = metrics.sideInset)
                    .size(metrics.iconSize),
                colorFilter = ColorFilter.tint(ink),
            )
        }
    }
}

/**
 * Draws an option of a [QuvenGlassMenu], checked when chosen, as a picker or a toggle in a system menu draws it. A menu
 * holding one opens a column of checks before every row.
 *
 * @param label The option's name.
 * @param selected Whether the option is the one chosen, which its check marks.
 * @param onClick Invoked when the option is chosen.
 * @param modifier Modifier applied to the row.
 * @param enabled Whether the option can be chosen; a disabled one is drawn dimmed and lights for no press.
 * @param role The kind of option: [Role.RadioButton] for one of several, [Role.Checkbox] for a toggle.
 */
@Composable
public fun QuvenGlassMenuChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: Role = Role.RadioButton,
) {
    val menu = LocalOpenMenu.current
    val metrics = menu.metrics
    val ink = menu.inkOf(enabled, menu.colors.label)
    menu.counts(menu.touch::choices)
    MenuRowBody(onClick, enabled, role, modifier.semantics { this.selected = selected }) {
        if (selected) {
            MenuCheck(
                color = ink,
                modifier = Modifier
                    .padding(start = metrics.checkCentre - metrics.checkSize / 2)
                    .size(metrics.checkSize),
            )
        }
        MenuLabel(label, ink, start = metrics.choiceLabelStart, end = metrics.sideInset)
    }
}

/**
 * Draws one [QuvenGlassMenuChoice] per option, the one in force checked, as a picker in a system menu draws them.
 *
 * @param T The type of an option.
 * @param options The options, in order.
 * @param selected The option in force.
 * @param label Names an option.
 * @param onSelect Invoked with the option chosen.
 */
@Composable
public fun <T> QuvenGlassMenuChoices(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    options.forEach { option ->
        QuvenGlassMenuChoice(label(option), selected = option == selected, onClick = { onSelect(option) })
    }
}

/**
 * Draws the hairline between two sections of a [QuvenGlassMenu].
 *
 * @param modifier Modifier applied to the divider's row.
 */
@Composable
public fun QuvenGlassMenuDivider(modifier: Modifier = Modifier) {
    val menu = LocalOpenMenu.current
    val hairline = with(LocalDensity.current) { 1f.toDp() }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = menu.metrics.sideInset, vertical = menu.metrics.dividerSpace)
            .height(hairline)
            .background(menu.colors.divider),
    )
}

/**
 * Lays out a row of the open menu and answers its presses: the row registers where it stands for a sliding finger,
 * lights while held, after a press has lasted, or at once under a sliding finger, and counts its press into the menu's
 * wash.
 *
 * @param onClick Invoked when the row is chosen.
 * @param enabled Whether the row can be chosen.
 * @param role The row's role.
 * @param modifier Modifier applied to the row.
 * @param content Draws the row's glyphs and name.
 */
@Composable
private fun MenuRowBody(
    onClick: () -> Unit,
    enabled: Boolean,
    role: Role,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val menu = LocalOpenMenu.current
    val metrics = menu.metrics
    val touch = menu.touch
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val currentClick by rememberUpdatedState(onClick)
    val choose = remember(menu) {
        {
            currentClick()
            menu.onChosen()
        }
    }
    val row = remember(touch, choose) { MenuRow(choose) }
    row.enabled = enabled
    val scrubbed = touch.scrubbed === row
    // A press lights its row only once it has lasted, as a touch that goes on to slide never lights the first row.
    val lit by animateFloatAsState(
        if (pressed || scrubbed) 1f else 0f,
        if (pressed && !scrubbed) tween(HighlightFadeMillis, delayMillis = HighlightDelayMillis) else snap(),
        label = "highlight",
    )
    DisposableEffect(touch, row) {
        touch.rows += row
        onDispose { touch.rows -= row }
    }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        touch.pressedRows++
        try {
            awaitCancellation()
        } finally {
            touch.pressedRows--
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.rowHeight)
            .onPlaced { row.coordinates = it }
            .drawBehind {
                if (lit > 0f) {
                    val inset = metrics.highlightInset.toPx()
                    val sliver = HighlightSliver.toPx()
                    val height = size.height - 2f * sliver
                    drawRoundRect(
                        color = menu.colors.highlight,
                        topLeft = Offset(inset, sliver),
                        size = Size(size.width - 2f * inset, height),
                        cornerRadius = CornerRadius(height / 2f),
                        alpha = lit,
                    )
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, role = role, onClick = choose),
        contentAlignment = Alignment.CenterStart,
        content = content,
    )
}

/**
 * Draws a row's name on one line, cut short where the menu is at its widest.
 *
 * @param text The name.
 * @param color The name's colour.
 * @param start The distance from the menu's start to the name.
 * @param end The least room after the name.
 */
@Composable
private fun MenuLabel(text: String, color: Color, start: Dp, end: Dp) {
    val menu = LocalOpenMenu.current
    Box(Modifier.padding(start = start, end = end)) { MenuText(text, color, menu.metrics.labelSize, menu) }
}

/**
 * Draws one line of a menu's text in its typeface.
 *
 * @param text The text.
 * @param color The text's colour.
 * @param size The text's size.
 * @param menu The menu the text stands in.
 */
@Composable
private fun MenuText(text: String, color: Color, size: TextUnit, menu: OpenMenu) {
    BasicText(
        text = text,
        style = menu.textStyle.merge(TextStyle(color = color, fontSize = size)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Draws the check of a chosen option, a stroke as the system draws its checkmark.
 *
 * @param color The check's colour.
 * @param modifier Modifier applied to the check, which sizes it.
 */
@Composable
private fun MenuCheck(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * CheckStart.x, size.height * CheckStart.y)
            lineTo(size.width * CheckTurn.x, size.height * CheckTurn.y)
            lineTo(size.width * CheckEnd.x, size.height * CheckEnd.y)
        }
        drawPath(path, color, style = Stroke(width = size.width * CheckStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * The menu its entries stand in: its layout, colours and typeface, and what a finger does on it.
 *
 * @property metrics The layout of the rows.
 * @property colors The colours of the rows.
 * @property textStyle The typeface of the names and titles.
 * @property touch What a finger does on the menu and what it holds.
 * @property onChosen Runs after any row is chosen.
 */
internal class OpenMenu(
    val metrics: QuvenGlassMenuMetrics,
    val colors: QuvenGlassMenuColors,
    val textStyle: TextStyle,
    val touch: MenuTouch,
    val onChosen: () -> Unit,
) {
    /** Gets the room the column of checks takes before every other row, nothing while the menu holds no choice. */
    val choiceShift: Dp
        get() = if (touch.choices > 0) metrics.choiceShift else 0.dp

    /** Gets where a row's name stands: after the glyphs where any row has one, after the checks, or at the plain inset. */
    val labelStart: Dp
        get() = when {
            touch.glyphs > 0 -> metrics.labelStart + choiceShift
            touch.choices > 0 -> metrics.choiceLabelStart
            else -> metrics.plainLabelStart
        }

    /**
     * Counts the calling entry into [count] while it stays in the menu.
     *
     * @param count The tally the entry joins.
     */
    @Composable
    fun counts(count: KMutableProperty0<Int>) {
        DisposableEffect(this, count) {
            count.set(count.get() + 1)
            onDispose { count.set(count.get() - 1) }
        }
    }

    /**
     * Returns the colour a row's name and glyphs are drawn in.
     *
     * @param enabled Whether the row can be chosen.
     * @param ink The row's own colour.
     * @return The row's colour, or the disabled colour for a row that cannot be chosen.
     */
    fun inkOf(enabled: Boolean, ink: Color): Color = if (enabled) ink else colors.disabled
}

private val LocalOpenMenu = staticCompositionLocalOf {
    OpenMenu(QuvenGlassMenuMetrics.Phone, QuvenGlassMenuColors.Standard, TextStyle.Default, MenuTouch()) {}
}

// Places the entries from the foot up, the first at the foot, as a menu rising from its control lists them.
private object FromTheFoot : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        var top = sizes.sum()
        sizes.forEachIndexed { index, size ->
            top -= size
            outPositions[index] = top
        }
    }
}

private val TrailingGap = 8.dp
private const val HighlightFadeMillis = 180
private const val HighlightDelayMillis = 150
private const val WashInMillis = 50
private const val WashOutMillis = 150
private val HighlightSliver = 1.5.dp
private val CheckStart = Offset(0.06f, 0.53f)
private val CheckTurn = Offset(0.37f, 0.86f)
private val CheckEnd = Offset(0.94f, 0.12f)
private const val CheckStroke = 0.17f
