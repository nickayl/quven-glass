package tv.quven.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlinx.coroutines.awaitCancellation

/**
 * Lays out the entries of a glass menu, the content a [QuvenGlassMorph] opens into: [QuvenGlassMenuTitle],
 * [QuvenGlassMenuItem], [QuvenGlassMenuChoice] and [QuvenGlassMenuDivider], one under another. The menu is as wide as
 * its longest row, between the widths [metrics] names, and scrolls where it is taller than the room it is given. A
 * finger that slides along a menu that does not scroll lights the row under it, with a tick each time it reaches
 * another, and chooses the row it lifts over, as on a system menu.
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
    val scroll = rememberScrollState()
    val touch = remember(scroll) { MenuTouch(scroll) }
    val chosen by rememberUpdatedState(onChosen)
    val contents = remember { MenuContents() }
    val menu = remember(metrics, colors, textStyle, touch, contents) { OpenMenu(metrics, colors, textStyle, touch, contents) { chosen() } }
    val haptics = LocalHapticFeedback.current
    val wash by animateFloatAsState(
        if (touch.isTouched) 1f else 0f,
        tween(if (touch.isTouched) WashInMillis else WashOutMillis),
        label = "wash",
    )
    val submenus = LocalSubmenuSlot.current
    CompositionLocalProvider(LocalOpenMenu provides menu) {
        Column(
            modifier
                .graphicsLayer { alpha = lerp(1f, CoveredAlpha, submenus?.coverage() ?: 0f) }
                .widthIn(min = metrics.width + menu.choiceShift, max = metrics.maxWidth)
                .width(IntrinsicSize.Max)
                .onPlaced { touch.menu = it }
                .pointerInput(touch, haptics) { scrubRows(touch, haptics) }
                .drawBehind { if (wash > 0f) drawRect(colors.pressWash, alpha = wash) }
                .verticalScroll(scroll)
                .padding(vertical = metrics.verticalInset),
            verticalArrangement = if (rising && contents.titles == 0) FromTheFoot else Arrangement.Top,
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
    menu.counts(menu.contents::titles)
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
    val ink = menu.inkOf(enabled, color.takeOrElse { if (destructive) menu.colors.destructive else menu.colors.label })
    MenuItemRow(label, onClick, modifier, icon, enabled, ink, trailing = trailingIcon?.let { { tint -> MenuGlyph(it, tint, Modifier) } })
}

/**
 * Draws an entry of a [QuvenGlassMenu] that opens a second menu over this one, as a system menu's submenu does: its row
 * carries a chevron, and the second menu grows out of it, as wide as this one and headed by the entry's own name, its
 * chevron turned down, while this menu fades behind it. A press on the head closes the second menu; a choice in it closes
 * both. Opens only in a menu a [QuvenGlassMenuHost] draws.
 *
 * @param label The entry's name.
 * @param modifier Modifier applied to the row.
 * @param icon The entry's glyph, or `null` for none.
 * @param enabled Whether the entry can open its menu.
 * @param content The second menu's entries.
 * @throws IllegalStateException The menu does not stand in a [QuvenGlassMenuHost].
 */
@Composable
public fun QuvenGlassSubmenu(
    label: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val slot = checkNotNull(LocalSubmenuSlot.current) { "A submenu opens in a menu a QuvenGlassMenuHost draws." }
    val menu = LocalOpenMenu.current
    val morph = rememberQuvenGlassMorphState()
    val request = remember(morph) { SubmenuRequest(morph) }
    SideEffect {
        request.label = label
        request.icon = icon
        request.content = content
    }
    MenuItemRow(
        label = label,
        onClick = { slot.open(request) },
        modifier = modifier.quvenGlassAnchor(morph, stretches = false),
        icon = icon,
        enabled = enabled,
        ink = menu.inkOf(enabled, menu.colors.label),
        closes = false,
        trailing = { tint -> MenuChevron(tint, turn = { 0f }) },
    )
}

/**
 * Draws the head of an open submenu: its entry's glyph and name, in bold, and the chevron turning down as it opens; a
 * press on it closes the submenu.
 *
 * @param request The submenu.
 * @param onClose Invoked when the head is pressed.
 */
@Composable
internal fun SubmenuHead(request: SubmenuRequest, onClose: () -> Unit) {
    val menu = LocalOpenMenu.current
    MenuItemRow(
        label = request.label,
        onClick = onClose,
        modifier = Modifier,
        icon = request.icon,
        enabled = true,
        ink = menu.colors.label,
        closes = false,
        bold = true,
        trailing = { tint -> MenuChevron(tint, turn = { request.morph.progress.value }) },
    )
}

/**
 * Draws a row of a [QuvenGlassMenu]: its glyph, its name and a trailing mark, lit while pressed.
 *
 * @param label The row's name.
 * @param onClick Invoked when the row is chosen.
 * @param modifier Modifier applied to the row.
 * @param icon The row's glyph, or `null` for none.
 * @param enabled Whether the row can be chosen.
 * @param ink The colour of the row's name and glyphs.
 * @param closes Whether choosing the row closes the menu.
 * @param bold Whether the name is drawn in bold, as a submenu's head is.
 * @param trailing Draws the mark at the row's end in the given colour, or `null` for none.
 */
@Composable
private fun MenuItemRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: Painter?,
    enabled: Boolean,
    ink: Color,
    closes: Boolean = true,
    bold: Boolean = false,
    trailing: (@Composable (Color) -> Unit)? = null,
) {
    val menu = LocalOpenMenu.current
    val metrics = menu.metrics
    if (icon != null) menu.counts(menu.contents::glyphs)
    MenuRowBody(onClick, enabled, Role.Button, modifier, closes) {
        if (icon != null) MenuGlyph(icon, ink, Modifier.padding(start = metrics.iconCentre + menu.choiceShift - metrics.iconSize / 2))
        val trailingRoom = if (trailing != null) metrics.iconSize + TrailingGap else 0.dp
        MenuLabel(label, ink, start = menu.labelStart, end = metrics.sideInset + trailingRoom, bold = bold)
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = metrics.sideInset).size(metrics.iconSize), contentAlignment = Alignment.Center) {
                trailing(ink)
            }
        }
    }
}

/**
 * An action of a [MenuPalette]: its name, its glyph and what choosing it runs.
 *
 * @property label The action's name.
 * @property icon The action's glyph.
 * @property run Runs the action.
 */
internal class PaletteAction(val label: String, val icon: Painter, val run: () -> Unit)

/**
 * Draws actions side by side across a [QuvenGlassMenu], each glyph over its name in small type, as an expanded edit menu
 * heads its entries with the clipboard's actions. A press lights the action's cell; a choice closes the menu.
 *
 * @param actions The actions, in the order they stand.
 * @param modifier Modifier applied to the row.
 */
@Composable
internal fun MenuPalette(actions: List<PaletteAction>, modifier: Modifier = Modifier) {
    val menu = LocalOpenMenu.current
    val metrics = menu.metrics
    Row(modifier.fillMaxWidth().height(PaletteHeight).padding(horizontal = metrics.highlightInset)) {
        actions.forEach { action ->
            val presses = remember { MutableInteractionSource() }
            val pressed by presses.collectIsPressedAsState()
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind { if (pressed) drawRoundRect(menu.colors.highlight, cornerRadius = CornerRadius(PaletteCorner.toPx())) }
                    .clickable(interactionSource = presses, indication = null, role = Role.Button) {
                        action.run()
                        menu.onChosen()
                    },
            ) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = PaletteIconCentre - metrics.iconSize / 2)) {
                    MenuGlyph(action.icon, menu.colors.label, Modifier.testTag(PaletteGlyphTag))
                }
                BasicText(
                    action.label,
                    Modifier.align(Alignment.TopCenter).padding(top = PaletteLabelCentre - PaletteLabelLine / 2),
                    style = menu.textStyle.merge(
                        TextStyle(
                            color = menu.colors.label,
                            fontSize = PaletteLabelSize,
                            lineHeight = PaletteLabelLine.value.sp,
                            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                        ),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Draws a submenu's chevron, pointing to the end and turning down as [turn] goes from 0 to 1.
 *
 * @param color The chevron's colour.
 * @param turn Reads how far the chevron has turned down.
 */
@Composable
private fun MenuChevron(color: Color, turn: () -> Float) {
    Canvas(Modifier.size(ChevronSize).graphicsLayer { rotationZ = QuarterTurn * turn() }) {
        val path = Path().apply {
            moveTo(size.width * ChevronStart.x, size.height * ChevronStart.y)
            lineTo(size.width * ChevronTip.x, size.height * ChevronTip.y)
            lineTo(size.width * ChevronStart.x, size.height * (1f - ChevronStart.y))
        }
        drawPath(path, color, style = Stroke(width = size.width * ChevronStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
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
    menu.counts(menu.contents::choices)
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
    closes: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val menu = LocalOpenMenu.current
    val metrics = menu.metrics
    val touch = menu.touch
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val currentClick by rememberUpdatedState(onClick)
    val choose = remember(menu, closes) {
        {
            currentClick()
            if (closes) menu.onChosen()
        }
    }
    val row = remember(touch, choose) { MenuRow(choose) }
    row.enabled = enabled
    val scrubbed = touch.scrubbed === row
    // A press lights its row only once it has lasted, as a touch that goes on to slide never lights the first row.
    val lit by animateFloatAsState(
        if (pressed || scrubbed || focused) 1f else 0f,
        when {
            scrubbed -> snap()
            pressed -> tween(HighlightFadeMillis, delayMillis = HighlightDelayMillis)
            focused -> tween(HighlightFadeMillis)
            else -> snap()
        },
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
 * Draws a row's glyph at the menu's glyph size, in the row's colour.
 *
 * @param painter The glyph.
 * @param ink The row's colour.
 * @param modifier Modifier applied to the glyph, which places it.
 */
@Composable
private fun MenuGlyph(painter: Painter, ink: Color, modifier: Modifier) {
    Image(painter, contentDescription = null, modifier = modifier.size(LocalOpenMenu.current.metrics.iconSize), colorFilter = ColorFilter.tint(ink))
}

/**
 * Draws a row's name on one line, cut short where the menu is at its widest.
 *
 * @param text The name.
 * @param color The name's colour.
 * @param start The distance from the menu's start to the name.
 * @param end The least room after the name.
 * @param bold Whether the name is drawn in bold.
 */
@Composable
private fun MenuLabel(text: String, color: Color, start: Dp, end: Dp, bold: Boolean = false) {
    val menu = LocalOpenMenu.current
    Box(Modifier.padding(start = start, end = end)) { MenuText(text, color, menu.metrics.labelSize, menu, if (bold) FontWeight.SemiBold else null) }
}

/**
 * Draws one line of a menu's text in its typeface.
 *
 * @param text The text.
 * @param color The text's colour.
 * @param size The text's size.
 * @param menu The menu the text stands in.
 * @param weight The text's weight, or `null` for the typeface's own.
 */
@Composable
private fun MenuText(text: String, color: Color, size: TextUnit, menu: OpenMenu, weight: FontWeight? = null) {
    BasicText(
        text = text,
        style = menu.textStyle.merge(TextStyle(color = color, fontSize = size, fontWeight = weight)),
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
 * @property touch What a finger does on the menu.
 * @property contents How many entries carry a check or a glyph and how many sections a title.
 * @property onChosen Runs after any row is chosen.
 */
internal class OpenMenu(
    val metrics: QuvenGlassMenuMetrics,
    val colors: QuvenGlassMenuColors,
    val textStyle: TextStyle,
    val touch: MenuTouch,
    val contents: MenuContents,
    val onChosen: () -> Unit,
) {
    /** Gets the room the column of checks takes before every other row, nothing while the menu holds no choice. */
    val choiceShift: Dp
        get() = if (contents.choices > 0) metrics.choiceShift else 0.dp

    /** Gets where a row's name stands: after the glyphs where any row has one, after the checks, or at the plain inset. */
    val labelStart: Dp
        get() = when {
            contents.glyphs > 0 -> metrics.labelStart + choiceShift
            contents.choices > 0 -> metrics.choiceLabelStart
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
    OpenMenu(QuvenGlassMenuMetrics.Phone, QuvenGlassMenuColors.Standard, TextStyle.Default, MenuTouch(ScrollState(0)), MenuContents()) {}
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

// Measured on the palette heading an expanded edit menu on an iPad.
private val PaletteHeight = 53.5.dp
private val PaletteIconCentre = 18.dp
private val PaletteLabelCentre = 38.dp
private val PaletteLabelSize = 12.sp
private val PaletteLabelLine = 14.dp
private val PaletteCorner = 12.dp
internal const val PaletteGlyphTag = "quven-glass-palette-glyph"
private const val HighlightFadeMillis = 180
private const val HighlightDelayMillis = 150
private const val WashInMillis = 50
private const val WashOutMillis = 150
private val HighlightSliver = 1.5.dp
private val CheckStart = Offset(0.06f, 0.53f)
private val CheckTurn = Offset(0.37f, 0.86f)
private val CheckEnd = Offset(0.94f, 0.12f)
private const val CheckStroke = 0.17f
private val ChevronSize = 13.dp
private val ChevronStart = Offset(0.32f, 0.12f)
private val ChevronTip = Offset(0.7f, 0.5f)
private const val ChevronStroke = 0.17f
private const val QuarterTurn = 90f

// Measured on a system menu holding a submenu open on an iPhone.
private const val CoveredAlpha = 0.4f

/**
 * A submenu a [QuvenGlassSubmenu] opens over its menu, kept in step with the entry's parameters.
 *
 * @property morph The opening, which grows from the entry's row.
 */
internal class SubmenuRequest(val morph: QuvenGlassMorphState) {

    /** Gets or sets the entry's name, which heads the submenu. */
    var label: String by mutableStateOf("")

    /** Gets or sets the entry's glyph, or `null` for none. */
    var icon: Painter? by mutableStateOf(null)

    /** Gets or sets the submenu's entries. */
    var content: @Composable ColumnScope.() -> Unit by mutableStateOf({})
}

/** The submenu a menu holds open over it, at most one at a time. */
@Stable
internal class SubmenuSlot {

    /** Gets the submenu standing open or closing, or `null` for none. */
    var shown: SubmenuRequest? by mutableStateOf(null)
        private set

    /** Gets or sets a value indicating whether the submenu shown is open rather than closing. */
    var expanded: Boolean by mutableStateOf(false)

    /**
     * Opens [request] over the menu.
     *
     * @param request The submenu to open.
     */
    fun open(request: SubmenuRequest) {
        shown = request
        expanded = true
    }

    /** Closes the submenu standing open, which stays shown until its glass has returned to its row. */
    fun close() {
        expanded = false
    }

    /**
     * Forgets [request] once its glass has returned to its row.
     *
     * @param request The submenu to forget.
     */
    fun release(request: SubmenuRequest) {
        if (shown === request && !expanded) shown = null
    }

    /**
     * Returns how far the menu stands covered by its submenu, from 0 to 1.
     *
     * @return The coverage.
     */
    fun coverage(): Float = shown?.morph?.progress?.value?.coerceIn(0f, 1f) ?: 0f
}

/** Provides the submenu slot of the menu a [QuvenGlassMenuHost] draws, `null` inside a submenu. */
internal val LocalSubmenuSlot = staticCompositionLocalOf<SubmenuSlot?> { null }
