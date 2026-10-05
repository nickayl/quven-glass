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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
                    EditAction(item.label) { item.onClick(session) }
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
 * @property run Runs the action.
 */
internal class EditAction(val label: String, val run: () -> Unit)

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
            onCutRequested?.let { EditAction(labels.cut, it) },
            onCopyRequested?.let { EditAction(labels.copy, it) },
            onPasteRequested?.let { EditAction(labels.paste, it) },
            onSelectAllRequested?.let { EditAction(labels.selectAll, it) },
            onAutofillRequested?.let { EditAction(labels.autofill, it) },
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
 * below.
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
    val appear = remember(request) { Animatable(0f) }
    LaunchedEffect(request) { appear.animateTo(1f, tween(AppearMillis)) }
    var page by remember(request, actions.size) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val gap = with(density) { MenuGap.toPx() }
    val room = LocalWindowInfo.current.containerSize.width - with(density) { (MenuMargin * 2).roundToPx() }
    val hairlines = remember { HairlineStops() }
    Layout(
        content = {
            Layout(
                content = {
                    actions.forEach { action ->
                        EditMenuAction(action.label, textStyle) {
                            action.run()
                            onDone()
                        }
                    }
                    EditMenuAction(PreviousPage, textStyle) { page-- }
                    EditMenuAction(NextPage, textStyle) { page++ }
                },
                modifier = Modifier
                    .height(MenuHeight)
                    .graphicsLayer {
                        val shown = appear.value
                        alpha = shown
                        val grown = if (reduceMotion) 1f else AppearScale + (1f - AppearScale) * shown
                        scaleX = grown
                        scaleY = grown
                    }
                    .liquidGlass(backdrop, style, CircleShape, null, reduceMotion, lift = null, pill = null)
                    .drawBehind {
                        val tall = HairlineHeight.toPx()
                        hairlines.stops.forEach { x ->
                            drawRect(HairlineColor, Offset(x, (size.height - tall) / 2f), Size(HairlineWidth.toPx(), tall))
                        }
                    },
            ) { measurables, constraints ->
                val tall = constraints.maxHeight
                val placeables = measurables.map { it.measure(Constraints(minHeight = tall, maxHeight = tall)) }
                val items = placeables.dropLast(2)
                val back = placeables[placeables.size - 2]
                val more = placeables.last()
                val hairline = HairlineWidth.roundToPx()
                val pages = editMenuPages(items.map { it.width }, room, back.width, hairline)
                val shown = pages[page.coerceIn(pages.indices)]
                val row = buildList {
                    if (shown.first > 0) add(back)
                    shown.forEach { add(items[it]) }
                    if (shown.last < items.lastIndex) add(more)
                }
                hairlines.stops = row.dropLast(1).runningFold(0) { x, piece -> x + piece.width + hairline }.drop(1).map { (it - hairline).toFloat() }
                val width = row.sumOf { it.width } + hairline * (row.size - 1)
                layout(width, tall) {
                    var x = 0
                    row.forEach { piece ->
                        piece.place(x, 0)
                        x += piece.width + hairline
                    }
                }
            }
        },
    ) { measurables, constraints ->
        // The menu takes the width its actions need, up to the window's, wherever the provider stands.
        val menu = measurables.single().measure(Constraints(maxHeight = MenuHeight.roundToPx()))
        val above = selection.top - gap - menu.height
        val top = if (origin.y + above >= 0f) above else selection.bottom + gap
        val left = (selection.center.x - menu.width / 2f).coerceAtLeast(MenuMargin.toPx() - origin.x)
        layout(0, 0) { menu.place(IntOffset(left.roundToInt(), top.roundToInt())) }
    }
}

/** Where the hairlines between an edit menu's actions stand, from its start, as its last layout placed them. */
private class HairlineStops {
    var stops: List<Float> = emptyList()
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
 * @param textStyle The typeface of its name.
 * @param onClick Invoked when the action is chosen.
 */
@Composable
private fun EditMenuAction(label: String, textStyle: TextStyle, onClick: () -> Unit) {
    val presses = remember { MutableInteractionSource() }
    val pressed by presses.collectIsPressedAsState()
    Box(
        Modifier
            .fillMaxHeight()
            .background(if (pressed) PressedColor else Color.Transparent)
            .clickable(interactionSource = presses, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = ActionPadding),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = textStyle.merge(TextStyle(color = Color.White, fontSize = ActionSize)), maxLines = 1)
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

// Measured on the system's edit menu on an iPhone.
private val MenuHeight: Dp = 41.dp
private val MenuGap: Dp = 14.dp
private val ActionPadding: Dp = 17.dp
private val ActionSize = 17.sp
private val HairlineWidth: Dp = 1.dp
private val HairlineHeight: Dp = 20.dp
private val HairlineColor = Color(0x38FFFFFF)
private val PressedColor = Color(0x1FFFFFFF)
private val MenuMargin: Dp = 8.dp
private const val PreviousPage = "\u2039"
private const val NextPage = "\u203A"
private const val AppearMillis = 200
private const val AppearScale = 0.9f
