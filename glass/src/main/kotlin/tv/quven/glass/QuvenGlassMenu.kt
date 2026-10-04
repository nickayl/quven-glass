package tv.quven.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.awaitCancellation
import kotlin.math.roundToInt

/**
 * Describes how a glass menu lays out its rows, as Apple lays out its system menus.
 *
 * @property width The menu's width.
 * @property cornerRadius The radius of the open menu's corners.
 * @property verticalInset The room above the menu's first entry and below its last.
 * @property rowHeight The height of a row.
 * @property titleAbove The room above the middle of a section's title.
 * @property titleBelow The room between the middle of a section's title and the row under it.
 * @property sideInset The distance from the menu's sides to a section's title, a divider's ends and a row's trailing
 * glyph.
 * @property iconCentre The distance from the menu's start to the middle of a row's glyph.
 * @property iconSize The size of a row's glyph.
 * @property labelStart The distance from the menu's start to a row's name.
 * @property dividerSpace The room above and below a divider.
 * @property labelSize The size of a row's name.
 * @property titleSize The size of a section's title.
 * @property highlightInset The room between the menu's sides and a held row's highlight, a capsule as tall as the row
 * less a sliver above and below.
 */
@Immutable
public data class QuvenGlassMenuMetrics(
    val width: Dp,
    val cornerRadius: Dp,
    val verticalInset: Dp,
    val rowHeight: Dp,
    val titleAbove: Dp,
    val titleBelow: Dp,
    val sideInset: Dp,
    val iconCentre: Dp,
    val iconSize: Dp,
    val labelStart: Dp,
    val dividerSpace: Dp,
    val labelSize: TextUnit,
    val titleSize: TextUnit,
    val highlightInset: Dp,
) {
    public companion object {
        /** Gets the layout of a menu on a tablet, as iPadOS lays out its system menus. */
        public val Tablet: QuvenGlassMenuMetrics = QuvenGlassMenuMetrics(
            width = 223.dp,
            cornerRadius = 25.dp,
            verticalInset = 6.dp,
            rowHeight = 38.dp,
            titleAbove = 14.5.dp,
            titleBelow = 13.5.dp,
            sideInset = 20.dp,
            iconCentre = 32.5.dp,
            iconSize = 22.dp,
            labelStart = 55.dp,
            dividerSpace = 8.dp,
            labelSize = 15.sp,
            titleSize = 12.sp,
            highlightInset = 13.dp,
        )

        /** Gets the layout of a menu on a phone, as iOS lays out its system menus. */
        public val Phone: QuvenGlassMenuMetrics = QuvenGlassMenuMetrics(
            width = 247.dp,
            cornerRadius = 25.dp,
            verticalInset = 6.dp,
            rowHeight = 42.dp,
            titleAbove = 12.dp,
            titleBelow = 19.dp,
            sideInset = 26.dp,
            iconCentre = 37.dp,
            iconSize = 24.dp,
            labelStart = 62.dp,
            dividerSpace = 9.5.dp,
            labelSize = 17.sp,
            titleSize = 13.sp,
            highlightInset = 14.dp,
        )
    }
}

/**
 * Describes the colours of a glass menu on the thick glass it opens into.
 *
 * @property label The colour of a row's name and glyph.
 * @property title The colour of a section's title.
 * @property divider The colour of the line between sections.
 * @property destructive The colour of a row whose action cannot be undone.
 * @property highlight The colour of a held row's highlight.
 * @property pressWash The colour laid over the whole menu while a row is pressed.
 */
@Immutable
public data class QuvenGlassMenuColors(
    val label: Color = Color.White,
    val title: Color = Color(0x8CFFFFFF),
    val divider: Color = Color(0x2EFFFFFF),
    val destructive: Color = Color(0xFFFF6B6E),
    val highlight: Color = Color(0x29FFFFFF),
    val pressWash: Color = Color(0x26FFFFFF),
) {
    public companion object {
        /** Gets the colours of Apple's system menus on dark glass. */
        public val Standard: QuvenGlassMenuColors = QuvenGlassMenuColors()
    }
}

/**
 * Lays out the entries of a glass menu, the content a [QuvenGlassMorph] opens into: [QuvenGlassMenuTitle],
 * [QuvenGlassMenuItem] and [QuvenGlassMenuDivider], one under another. A finger that slides along the menu lights the
 * row under it, with a tick each time it reaches another, and chooses the row it lifts over, as on a system menu.
 *
 * @param modifier Modifier applied to the menu's column, which fills the width it is given.
 * @param metrics The layout of the rows; the morph opening the menu takes its width and corner radius.
 * @param colors The colours of the rows.
 * @param textStyle The style the rows' names and the titles are drawn in; the menu sets their size and colour.
 * @param content The menu's entries.
 */
@Composable
public fun QuvenGlassMenu(
    modifier: Modifier = Modifier,
    metrics: QuvenGlassMenuMetrics = QuvenGlassMenuMetrics.Phone,
    colors: QuvenGlassMenuColors = QuvenGlassMenuColors.Standard,
    textStyle: TextStyle = TextStyle.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    val look = remember(metrics, colors, textStyle) { MenuLook(metrics, colors, textStyle) }
    val haptics = LocalHapticFeedback.current
    val pressed = look.pressedRows > 0 || look.isScrubbing
    val wash by animateFloatAsState(
        if (pressed) 1f else 0f,
        tween(if (pressed) WashInMillis else WashOutMillis),
        label = "wash",
    )
    CompositionLocalProvider(LocalMenuLook provides look) {
        Column(
            modifier
                .fillMaxWidth()
                .onPlaced { look.menu = it }
                .pointerInput(look, haptics) { scrubRows(look, haptics) }
                .drawBehind { if (wash > 0f) drawRect(colors.pressWash, alpha = wash) }
                .padding(vertical = metrics.verticalInset),
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
    val look = LocalMenuLook.current
    val metrics = look.metrics
    Layout(
        content = {
            BasicText(
                text = text,
                style = look.textStyle.merge(TextStyle(color = look.colors.title, fontSize = metrics.titleSize)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(horizontal = metrics.sideInset),
    ) { measurables, constraints ->
        val title = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val height = (metrics.titleAbove + metrics.titleBelow).roundToPx()
        layout(constraints.maxWidth, height) {
            title.placeRelative(0, (metrics.titleAbove.toPx() - title.height / 2f).roundToInt())
        }
    }
}

/**
 * Draws one row of a [QuvenGlassMenu]: its glyph, its name and an optional trailing glyph, lit while pressed.
 *
 * @param label The row's name.
 * @param onClick Invoked when the row is pressed.
 * @param modifier Modifier applied to the row.
 * @param icon The row's glyph, drawn in the row's colour, or `null` for none.
 * @param destructive Whether the row's action cannot be undone, which draws it in the destructive colour.
 * @param color The colour of the row's name and glyphs, or [Color.Unspecified] for the menu's.
 * @param trailingIcon A glyph drawn at the row's end, such as a check on the chosen option, or `null` for none.
 */
@Composable
public fun QuvenGlassMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    destructive: Boolean = false,
    color: Color = Color.Unspecified,
    trailingIcon: Painter? = null,
) {
    val look = LocalMenuLook.current
    val metrics = look.metrics
    val ink = color.takeOrElse { if (destructive) look.colors.destructive else look.colors.label }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val currentClick by rememberUpdatedState(onClick)
    val row = remember(look) { MenuRow { currentClick() } }
    val scrubbed = look.scrubbed === row
    // A press lights its row only once it has lasted, as a touch that goes on to slide never lights the first row.
    val lit by animateFloatAsState(
        if (pressed || scrubbed) 1f else 0f,
        if (pressed && !scrubbed) tween(HighlightFadeMillis, delayMillis = HighlightDelayMillis) else snap(),
        label = "highlight",
    )
    DisposableEffect(look, row) {
        look.rows += row
        onDispose { look.rows -= row }
    }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        look.pressedRows++
        try {
            awaitCancellation()
        } finally {
            look.pressedRows--
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
                        color = look.colors.highlight,
                        topLeft = Offset(inset, sliver),
                        size = Size(size.width - 2f * inset, height),
                        cornerRadius = CornerRadius(height / 2f),
                        alpha = lit,
                    )
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = metrics.iconCentre - metrics.iconSize / 2)
                    .size(metrics.iconSize),
                colorFilter = ColorFilter.tint(ink),
            )
        }
        val trailingRoom = if (trailingIcon != null) metrics.iconSize + TrailingGap else 0.dp
        BasicText(
            text = label,
            modifier = Modifier.padding(start = metrics.labelStart, end = metrics.sideInset + trailingRoom),
            style = look.textStyle.merge(TextStyle(color = ink, fontSize = metrics.labelSize)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
 * Draws the hairline between two sections of a [QuvenGlassMenu].
 *
 * @param modifier Modifier applied to the divider's row.
 */
@Composable
public fun QuvenGlassMenuDivider(modifier: Modifier = Modifier) {
    val look = LocalMenuLook.current
    val hairline = with(LocalDensity.current) { 1f.toDp() }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = look.metrics.sideInset, vertical = look.metrics.dividerSpace)
            .height(hairline)
            .background(look.colors.divider),
    )
}

/**
 * The layout, colours and text style the entries of a [QuvenGlassMenu] read, with its rows, how many of them are
 * pressed and the row a sliding finger lights.
 */
private class MenuLook(val metrics: QuvenGlassMenuMetrics, val colors: QuvenGlassMenuColors, val textStyle: TextStyle) {
    var pressedRows by mutableIntStateOf(0)
    var scrubbed: MenuRow? by mutableStateOf(null)
    var isScrubbing by mutableStateOf(false)
    var menu: LayoutCoordinates? = null
    val rows = mutableListOf<MenuRow>()

    /**
     * Returns the row under [position].
     *
     * @param position A point in the menu's coordinates.
     * @return The row, or `null` where none stands.
     */
    fun rowAt(position: Offset): MenuRow? {
        val menu = menu?.takeIf { it.isAttached } ?: return null
        return rows.firstOrNull { row ->
            row.coordinates?.takeIf { it.isAttached }?.let { menu.localBoundingBoxOf(it, clipBounds = false).contains(position) } == true
        }
    }
}

/**
 * A row of a [QuvenGlassMenu]: where it stands and what choosing it does.
 *
 * @property action Chooses the row.
 */
private class MenuRow(val action: () -> Unit) {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Follows a finger on the menu: once it slides past the touch slop the menu takes the gesture from its rows, lights
 * the row under the finger, ticks as it reaches another and chooses the row it lifts over.
 *
 * @param look The menu the finger is on.
 * @param haptics The feedback that ticks.
 */
private suspend fun PointerInputScope.scrubRows(look: MenuLook, haptics: HapticFeedback) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    var reached = look.rowAt(down.position)
    try {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
            if (!look.isScrubbing && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                look.isScrubbing = true
            }
            if (!change.pressed) {
                if (look.isScrubbing) {
                    change.consume()
                    look.rowAt(change.position)?.action?.invoke()
                }
                break
            }
            if (look.isScrubbing) {
                change.consume()
                val under = look.rowAt(change.position)
                if (under != null && under !== reached) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    reached = under
                }
                look.scrubbed = under
            }
        }
    } finally {
        look.isScrubbing = false
        look.scrubbed = null
    }
}

private val LocalMenuLook = staticCompositionLocalOf {
    MenuLook(QuvenGlassMenuMetrics.Phone, QuvenGlassMenuColors.Standard, TextStyle.Default)
}

private val TrailingGap = 8.dp
private const val HighlightFadeMillis = 180
private const val HighlightDelayMillis = 150
private const val WashInMillis = 50
private const val WashOutMillis = 150
private val HighlightSliver = 1.5.dp
