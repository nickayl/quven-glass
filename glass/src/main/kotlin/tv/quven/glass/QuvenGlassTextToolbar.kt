package tv.quven.glass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred

/**
 * Gives the text fields and selectable text in [content] the menu of cut, copy and paste on glass, as Apple's edit menu
 * draws it: a capsule of the actions the selection allows, parted by hairlines, standing above the selection, or below it
 * where there is no room, and settling in as it appears.
 *
 * @param style The material of the menu's glass.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param textStyle The typeface of the actions' names; the menu sets their size and colour.
 * @param reduceMotion Whether motion is reduced: the menu then only fades.
 * @param content The content holding the text.
 */
@Composable
public fun ProvideQuvenGlassTextToolbar(
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    textStyle: TextStyle = TextStyle.Default,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val toolbar = remember { GlassTextToolbar() }
    val menus = remember { GlassTextContextMenuProvider() }
    ReadEditLabels(toolbar)
    var placed by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(Modifier.onPlaced { placed = it }) {
        CompositionLocalProvider(LocalTextToolbar provides toolbar, LocalTextContextMenuToolbarProvider provides menus) { content() }
        val box = placed?.takeIf { it.isAttached }
        val asked = menus.shown
        val legacy = toolbar.shown
        when {
            box != null && asked != null -> {
                val session = remember(asked) { object : TextContextMenuSession { override fun close() = menus.close() } }
                val actions = asked.data().components.filterIsInstance<TextContextMenuItem>().map { item ->
                    EditAction(item.label, editKindOf(item.key)) { item.onClick(session) }
                }
                if (actions.isNotEmpty()) {
                    EditMenu(asked, asked.contentBounds(box), box.positionInRoot(), actions, style, backdrop, textStyle, reduceMotion, onDone = {})
                }
            }
            legacy != null -> {
                val origin = box?.positionInRoot() ?: Offset.Zero
                EditMenu(legacy, legacy.selection.translate(-origin), origin, legacy.actions, style, backdrop, textStyle, reduceMotion, toolbar::hide)
            }
        }
    }
}

/**
 * The text context menu provider that draws the edit menu on glass: it holds the menu a text field asks for until the
 * field closes it or an action does.
 */
@Stable
internal class GlassTextContextMenuProvider : TextContextMenuProvider {

    /** Gets the menu shown, or `null` for none. */
    var shown: TextContextMenuDataProvider? by mutableStateOf(null)
        private set

    private var closing: CompletableDeferred<Unit>? = null

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        val done = CompletableDeferred<Unit>()
        closing?.complete(Unit)
        closing = done
        shown = dataProvider
        try {
            done.await()
        } finally {
            if (shown === dataProvider) shown = null
        }
    }

    /** Closes the menu shown. */
    fun close() {
        closing?.complete(Unit)
    }
}

/**
 * A menu of the actions a selection allows, as a text field asks for it.
 *
 * @property selection The selection's bounds, in the root's coordinates.
 * @property actions The actions, in the order they stand.
 */
internal class EditMenuRequest(val selection: Rect, val actions: List<EditAction>)

/**
 * One action of an edit menu.
 *
 * @property label The action's name.
 * @property kind Which of the clipboard's and the selection's own actions this is, if any.
 * @property run Runs the action.
 */
internal class EditAction(val label: String, val kind: EditKind = EditKind.Other, val run: () -> Unit)

/** The actions an edit menu knows by name, which it gives a glyph of its own and heads its expanded form with. */
internal enum class EditKind { Cut, Copy, Paste, SelectAll, Autofill, Other }

/**
 * Returns which known action a text context menu item's [key] names.
 *
 * @param key The item's key.
 * @return The action, or [EditKind.Other] for one the menu does not know.
 */
internal fun editKindOf(key: Any): EditKind = when (key) {
    TextContextMenuKeys.CutKey -> EditKind.Cut
    TextContextMenuKeys.CopyKey -> EditKind.Copy
    TextContextMenuKeys.PasteKey -> EditKind.Paste
    TextContextMenuKeys.SelectAllKey -> EditKind.SelectAll
    TextContextMenuKeys.AutofillKey -> EditKind.Autofill
    else -> EditKind.Other
}

/** The text toolbar that asks for the edit menu on glass, which its provider draws. */
@Stable
internal class GlassTextToolbar : TextToolbar {

    /** Gets the menu shown, or `null` for none. */
    var shown: EditMenuRequest? by mutableStateOf(null)
        private set

    /** Gets or sets the names of the actions, read from the platform's own. */
    var labels: EditLabels = EditLabels.English

    override val status: TextToolbarStatus
        get() = if (shown != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested, null)
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
        onAutofillRequested: (() -> Unit)?,
    ) {
        val actions = listOfNotNull(
            onCutRequested?.let { EditAction(labels.cut, EditKind.Cut, run = it) },
            onCopyRequested?.let { EditAction(labels.copy, EditKind.Copy, run = it) },
            onPasteRequested?.let { EditAction(labels.paste, EditKind.Paste, run = it) },
            onSelectAllRequested?.let { EditAction(labels.selectAll, EditKind.SelectAll, run = it) },
            onAutofillRequested?.let { EditAction(labels.autofill, EditKind.Autofill, run = it) },
        )
        shown = EditMenuRequest(rect, actions).takeIf { actions.isNotEmpty() }
    }

    override fun hide() {
        shown = null
    }
}

/**
 * The names of an edit menu's actions.
 *
 * @property cut The name of cutting.
 * @property copy The name of copying.
 * @property paste The name of pasting.
 * @property selectAll The name of selecting everything.
 * @property autofill The name of filling in automatically.
 */
internal class EditLabels(val cut: String, val copy: String, val paste: String, val selectAll: String, val autofill: String) {
    companion object {
        /** Gets the names used before the platform's are read. */
        val English: EditLabels = EditLabels("Cut", "Copy", "Paste", "Select All", "AutoFill")
    }
}

/**
 * Draws [actions] on a capsule of glass over [selection], centred on it, above it where the room allows and otherwise
 * below. On a phone the actions that do not fit page behind arrows, as iOS pages its edit menu; on a tablet they stand
 * behind a chevron that expands the capsule into a menu of every action, its clipboard's actions heading it side by
 * side, as iPadOS expands its edit menu.
 *
 * @param request The menu, which restarts its appearance when it changes.
 * @param selection The selection's bounds, in the provider's coordinates.
 * @param origin Where the provider stands in the root, which bounds the room above the selection.
 * @param actions The actions, in the order they stand.
 * @param style The material.
 * @param backdrop The backdrop the glass stands over.
 * @param textStyle The typeface of the names.
 * @param reduceMotion Whether motion is reduced.
 * @param onDone Invoked after an action runs.
 */
@Composable
private fun EditMenu(
    request: Any,
    selection: Rect,
    origin: Offset,
    actions: List<EditAction>,
    style: QuvenGlassStyle,
    backdrop: QuvenGlassBackdrop?,
    textStyle: TextStyle,
    reduceMotion: Boolean,
    onDone: () -> Unit,
) {
    var page by remember(request, actions.size) { mutableIntStateOf(0) }
    var expanded by remember(request) { mutableStateOf(false) }
    val appear = remember(request, expanded) { Animatable(0f) }
    LaunchedEffect(request, expanded) { appear.animateTo(1f, tween(AppearMillis)) }
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val metrics = if (with(density) { isTabletWindow(window.width.toFloat(), window.height.toFloat()) }) EditMenuMetrics.Tablet else EditMenuMetrics.Phone
    val gap = with(density) { MenuGap.toPx() }
    val margin = with(density) { MenuMargin.roundToPx() }
    val room = minOf(window.width - margin * 2, with(density) { metrics.maxWidth.roundToPx() })
    val hairlines = remember { LaidOut(emptyList<Float>()) }
    val bar = remember(request) { LaidOut(Rect.Zero) }
    val run: (EditAction) -> Unit = { action ->
        action.run()
        onDone()
    }
    Layout(
        content = {
            Box(
                Modifier.graphicsLayer {
                    val shown = appear.value
                    alpha = shown
                    val grown = if (reduceMotion) 1f else AppearScale + (1f - AppearScale) * shown
                    scaleX = grown
                    scaleY = grown
                    // The expanded menu grows out of the chevron at the capsule's end.
                    transformOrigin = if (expanded) TransformOrigin(1f, 0f) else TransformOrigin.Center
                },
            ) {
                if (expanded) {
                    ExpandedEditMenu(actions, style, backdrop, textStyle, reduceMotion, run)
                } else {
                    EditMenuBar(actions, metrics, room, page, { page = it }, { expanded = true }, hairlines, style, backdrop, textStyle, reduceMotion, run)
                }
            }
        },
    ) { measurables, _ ->
        if (!expanded) {
            // The menu takes the width its actions need, up to its room, wherever the provider stands.
            val menu = measurables.single().measure(Constraints(maxHeight = metrics.height.roundToPx()))
            val above = selection.top - gap - menu.height
            val top = if (origin.y + above >= 0f) above else selection.bottom + gap
            val left = (selection.center.x - menu.width / 2f).coerceAtLeast(margin - origin.x)
            bar.value = Rect(Offset(left, top), Size(menu.width.toFloat(), menu.height.toFloat()))
            layout(0, 0) { menu.place(IntOffset(left.roundToInt(), top.roundToInt())) }
        } else {
            // The expanded menu keeps the capsule's top and end, within the window.
            val menu = measurables.single().measure(Constraints(maxHeight = (window.height - margin * 2).coerceAtLeast(0)))
            val from = bar.value
            val left = (from.right - menu.width).coerceAtLeast(margin - origin.x)
            val top = from.top.coerceIn(margin - origin.y, (window.height - margin - menu.height - origin.y).coerceAtLeast(margin - origin.y))
            layout(0, 0) { menu.place(IntOffset(left.roundToInt(), top.roundToInt())) }
        }
    }
}

/**
 * Draws the edit menu's capsule: the actions that fit in [room], parted by hairlines, with arrows to the other pages on a
 * phone or a chevron that expands the menu on a tablet.
 *
 * @param actions The actions, in the order they stand.
 * @param metrics The capsule's layout.
 * @param room The widest the capsule may stand, in pixels.
 * @param page The page shown, on a phone.
 * @param onPage Invoked with the page to show.
 * @param onExpand Invoked when the chevron is pressed, on a tablet.
 * @param hairlines Where the hairlines stand, as the last layout placed them.
 * @param style The material.
 * @param backdrop The backdrop the glass stands over.
 * @param textStyle The typeface of the names.
 * @param reduceMotion Whether motion is reduced.
 * @param run Runs an action chosen.
 */
@Composable
private fun EditMenuBar(
    actions: List<EditAction>,
    metrics: EditMenuMetrics,
    room: Int,
    page: Int,
    onPage: (Int) -> Unit,
    onExpand: () -> Unit,
    hairlines: LaidOut<List<Float>>,
    style: QuvenGlassStyle,
    backdrop: QuvenGlassBackdrop?,
    textStyle: TextStyle,
    reduceMotion: Boolean,
    run: (EditAction) -> Unit,
) {
    Layout(
        content = {
            actions.forEach { action -> EditMenuAction(action.label, metrics, textStyle) { run(action) } }
            if (metrics.expands) {
                EditMenuMore(metrics, onExpand)
            } else {
                EditMenuAction(PreviousPage, metrics, textStyle) { onPage(page - 1) }
                EditMenuAction(NextPage, metrics, textStyle) { onPage(page + 1) }
            }
        },
        modifier = Modifier
            .height(metrics.height)
            .liquidGlass(backdrop, style, CircleShape, null, reduceMotion, lift = null, pill = null)
            .drawBehind {
                val tall = metrics.hairlineHeight.toPx()
                hairlines.value.forEach { x ->
                    drawRect(metrics.hairlineColor, Offset(x, (size.height - tall) / 2f), Size(HairlineWidth.toPx(), tall))
                }
            },
    ) { measurables, constraints ->
        val tall = constraints.maxHeight
        val placeables = measurables.map { it.measure(Constraints(minHeight = tall, maxHeight = tall)) }
        val hairline = HairlineWidth.roundToPx()
        val row: List<Placeable>
        val parted: Int
        if (metrics.expands) {
            val items = placeables.dropLast(1)
            val more = placeables.last()
            val shown = editMenuFit(items.map { it.width }, room, more.width, hairline)
            row = items.take(shown) + if (shown < items.size) listOf(more) else emptyList()
            // No hairline parts the last action from the chevron.
            parted = shown - 1
        } else {
            val items = placeables.dropLast(2)
            val back = placeables[placeables.size - 2]
            val next = placeables.last()
            val pages = editMenuPages(items.map { it.width }, room, back.width, hairline)
            val shown = pages[page.coerceIn(pages.indices)]
            row = buildList {
                if (shown.first > 0) add(back)
                shown.forEach { add(items[it]) }
                if (shown.last < items.lastIndex) add(next)
            }
            parted = row.size - 1
        }
        val starts = row.runningFold(0) { x, piece -> x + piece.width + hairline }
        hairlines.value = (1..parted).map { (starts[it] - hairline).toFloat() }
        val width = row.sumOf { it.width } + hairline * parted
        layout(width, tall) {
            var x = 0
            row.forEachIndexed { index, piece ->
                piece.place(x, 0)
                x += piece.width + if (index < parted) hairline else 0
            }
        }
    }
}

/**
 * Draws every action of an expanded edit menu on a menu's glass: the clipboard's actions side by side at its head, the
 * others one under another, each with its glyph where it has one.
 *
 * @param actions The actions, in the order they stand.
 * @param style The material.
 * @param backdrop The backdrop the glass stands over.
 * @param textStyle The typeface of the names.
 * @param reduceMotion Whether motion is reduced.
 * @param run Runs an action chosen.
 */
@Composable
private fun ExpandedEditMenu(
    actions: List<EditAction>,
    style: QuvenGlassStyle,
    backdrop: QuvenGlassBackdrop?,
    textStyle: TextStyle,
    reduceMotion: Boolean,
    run: (EditAction) -> Unit,
) {
    val metrics = QuvenGlassMenuMetrics.Tablet
    val clipboard = actions.filter { it.kind in ClipboardKinds }
    val others = actions - clipboard.toSet()
    val palette = clipboard.mapNotNull { action -> editGlyph(action)?.let { glyph -> PaletteAction(action.label, glyph) { run(action) } } }
    Box(Modifier.liquidGlass(backdrop, remember(style) { style.forMenus() }, RoundedCornerShape(metrics.cornerRadius), null, reduceMotion, lift = null, pill = null)) {
        QuvenGlassMenu(metrics = metrics, textStyle = textStyle) {
            if (palette.isNotEmpty()) MenuPalette(palette)
            if (palette.isNotEmpty() && others.isNotEmpty()) QuvenGlassMenuDivider()
            others.forEach { action -> QuvenGlassMenuItem(action.label, onClick = { run(action) }, icon = editGlyph(action)) }
        }
    }
}

/**
 * Returns the glyph an expanded edit menu gives [action], one of its own for an action it knows.
 *
 * @param action The action.
 * @return The glyph, or `null` for an action the menu does not know.
 */
@Composable
private fun editGlyph(action: EditAction): Painter? = when (action.kind) {
    EditKind.Cut -> rememberVectorPainter(EditGlyphs.Cut)
    EditKind.Copy -> rememberVectorPainter(EditGlyphs.Copy)
    EditKind.Paste -> rememberVectorPainter(EditGlyphs.Paste)
    EditKind.SelectAll -> rememberVectorPainter(EditGlyphs.SelectAll)
    EditKind.Autofill -> rememberVectorPainter(EditGlyphs.Autofill)
    EditKind.Other -> null
}

/**
 * Draws the chevron at the end of a tablet's edit menu, in a circle within the capsule's end, that expands the menu.
 *
 * @param metrics The capsule's layout.
 * @param onClick Invoked when the chevron is pressed.
 */
@Composable
private fun EditMenuMore(metrics: EditMenuMetrics, onClick: () -> Unit) {
    val inset = (metrics.height - MoreDiameter) / 2
    Box(
        Modifier
            .fillMaxHeight()
            .width(MoreDiameter + inset)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.align(Alignment.CenterStart).size(MoreDiameter).background(MoreFill, CircleShape), contentAlignment = Alignment.Center) {
            MenuChevron(Color.White, turn = { 0f })
        }
    }
}

/**
 * A value a layout writes and a later pass reads, unobserved, such as where the hairlines stand or where the capsule
 * last stood.
 *
 * @param T The type of the value.
 * @property value The value, as the last layout wrote it.
 */
private class LaidOut<T>(var value: T)

/**
 * The layout of an edit menu's capsule, as the system lays it out on a phone and on a tablet.
 *
 * @property height The capsule's height.
 * @property actionSize The size of an action's name.
 * @property actionPadding The room either side of an action's name.
 * @property hairlineHeight The height of the hairline between two actions.
 * @property hairlineColor The colour of the hairline.
 * @property maxWidth The widest the capsule stands.
 * @property expands Whether the actions that do not fit stand behind a chevron that expands the menu, rather than on
 * further pages.
 */
internal class EditMenuMetrics(
    val height: Dp,
    val actionSize: TextUnit,
    val actionPadding: Dp,
    val hairlineHeight: Dp,
    val hairlineColor: Color,
    val maxWidth: Dp,
    val expands: Boolean,
) {
    companion object {
        /** Gets the layout on a phone, measured on the system's edit menu on an iPhone. */
        val Phone: EditMenuMetrics = EditMenuMetrics(41.dp, 17.sp, 17.dp, 20.dp, Color(0x38FFFFFF), Dp.Infinity, expands = false)

        /** Gets the layout on a tablet, measured on the system's edit menu on an iPad. */
        val Tablet: EditMenuMetrics = EditMenuMetrics(43.dp, 15.sp, 17.dp, 17.dp, Color(0x80545454), 500.dp, expands = true)
    }
}

/**
 * Returns how many of an edit menu's actions stand in its capsule on a tablet: all of them where they fit in [room],
 * otherwise as many as fit beside the chevron that expands the menu, and always the first.
 *
 * @param widths The widths of the actions, in order.
 * @param room The widest the capsule may stand.
 * @param more The width of the chevron.
 * @param hairline The width of the hairline between two actions.
 * @return The number of actions shown.
 */
internal fun editMenuFit(widths: List<Int>, room: Int, more: Int, hairline: Int): Int {
    if (widths.sum() + hairline * (widths.size - 1).coerceAtLeast(0) <= room) return widths.size
    var used = more
    var count = 0
    for (width in widths) {
        val next = used + width + if (count > 0) hairline else 0
        if (next > room && count > 0) break
        used = next
        count++
    }
    return count
}

/**
 * Returns the pages an edit menu's actions part into within [room], as Apple's edit menu pages them: each page holds
 * as many actions as fit beside the arrows to the pages before and after it.
 *
 * @param widths The widths of the actions, in order.
 * @param room The widest the menu may stand.
 * @param arrow The width of an arrow to another page.
 * @param hairline The width of the hairline between two pieces.
 * @return The ranges of the actions each page holds, in order.
 */
internal fun editMenuPages(widths: List<Int>, room: Int, arrow: Int, hairline: Int): List<IntRange> {
    if (widths.isEmpty()) return listOf(IntRange.EMPTY)
    val pages = mutableListOf<IntRange>()
    var start = 0
    while (start < widths.size) {
        var used = if (start > 0) arrow + hairline else 0
        var end = start
        while (end < widths.size) {
            val piece = widths[end] + if (end > start || start > 0) hairline else 0
            val last = end == widths.lastIndex
            val needed = used + piece + if (last) 0 else hairline + arrow
            if (needed > room && end > start) break
            used += piece
            end++
        }
        pages += start until end
        start = end
    }
    return pages
}

/**
 * Draws one action of an edit menu, lit while pressed.
 *
 * @param label The action's name.
 * @param metrics The capsule's layout.
 * @param textStyle The typeface of its name.
 * @param onClick Invoked when the action is chosen.
 */
@Composable
private fun EditMenuAction(label: String, metrics: EditMenuMetrics, textStyle: TextStyle, onClick: () -> Unit) {
    val presses = remember { MutableInteractionSource() }
    val pressed by presses.collectIsPressedAsState()
    Box(
        Modifier
            .fillMaxHeight()
            .background(if (pressed) PressedColor else Color.Transparent)
            .clickable(interactionSource = presses, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = metrics.actionPadding),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = textStyle.merge(TextStyle(color = Color.White, fontSize = metrics.actionSize)), maxLines = 1)
    }
}

/**
 * Reads the platform's names of the edit actions into [toolbar].
 *
 * @param toolbar The toolbar whose names to set.
 */
@Composable
internal fun ReadEditLabels(toolbar: GlassTextToolbar) {
    toolbar.labels = EditLabels(
        stringResource(android.R.string.cut),
        stringResource(android.R.string.copy),
        stringResource(android.R.string.paste),
        stringResource(android.R.string.selectAll),
        // The platform names filling in from Android 8.1 on; before it the menu offers no such action.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) stringResource(android.R.string.autofill) else EditLabels.English.autofill,
    )
}

// Measured on the system's edit menu on an iPhone; a tablet's layout is EditMenuMetrics.Tablet.
private val MenuGap: Dp = 14.dp
private val HairlineWidth: Dp = 1.dp
private val PressedColor = Color(0x1FFFFFFF)
private val MenuMargin: Dp = 8.dp
private const val PreviousPage = "\u2039"
private const val NextPage = "\u203A"
private const val AppearMillis = 200
private const val AppearScale = 0.9f

// Measured on the system's edit menu on an iPad: the chevron's circle within the capsule's end, white at 6%.
private val MoreDiameter: Dp = 36.dp
private val MoreFill = Color(0x0FFFFFFF)
private val ClipboardKinds = setOf(EditKind.Cut, EditKind.Copy, EditKind.Paste)
