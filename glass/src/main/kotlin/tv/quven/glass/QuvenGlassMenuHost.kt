package tv.quven.glass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/**
 * The menus a screen opens: at most one stands open at a time, in the [QuvenGlassMenuHost] it is given to, while every
 * [QuvenGlassDropdown] under [LocalQuvenGlassMenuHost] asks to open its own.
 */
@Stable
public class QuvenGlassMenuHostState internal constructor() {

    /** Gets the menu standing open or closing, or `null` for none. */
    internal var shown: DropdownRequest? by mutableStateOf(null)
        private set

    /**
     * Opens [request], closing at once any other menu standing open.
     *
     * @param request The menu to open.
     */
    internal fun open(request: DropdownRequest) {
        shown = request
    }

    /**
     * Forgets [request] if it is the menu standing open.
     *
     * @param request The menu to forget.
     */
    internal fun release(request: DropdownRequest) {
        if (shown === request) shown = null
    }
}

/**
 * Creates and remembers a [QuvenGlassMenuHostState].
 *
 * @return The state.
 */
@Composable
public fun rememberQuvenGlassMenuHostState(): QuvenGlassMenuHostState = remember { QuvenGlassMenuHostState() }

/** Gets the menus the screen's [QuvenGlassDropdown]s open in, or `null` where no host is provided. */
public val LocalQuvenGlassMenuHost: ProvidableCompositionLocal<QuvenGlassMenuHostState?> = staticCompositionLocalOf { null }

/**
 * Draws the menu a screen has open above everything drawn before it, grown out of its control as one piece of glass,
 * over a layer that closes it on a press elsewhere and dims nothing; Back closes it too. A menu too tall for the host
 * stands 16 dp inside its edges and scrolls. Opened from the keys, the menu takes the focus to its first row and keeps
 * it among its rows until it closes, when the focus returns to its control. Place it last in the screen's root, where
 * it fills the window, and provide [state] through [LocalQuvenGlassMenuHost] to the content.
 *
 * @param state The menus the screen opens.
 * @param modifier Modifier applied to the host, which fills its parent.
 * @param style The material of the menus' glass.
 * @param metrics The layout of the menus' rows.
 * @param colors The colours of the menus' rows.
 * @param textStyle The typeface of the menus' names and titles.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced.
 */
@Composable
public fun QuvenGlassMenuHost(
    state: QuvenGlassMenuHostState,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    metrics: QuvenGlassMenuMetrics = QuvenGlassMenuMetrics.Phone,
    colors: QuvenGlassMenuColors = QuvenGlassMenuColors.Standard,
    textStyle: TextStyle = TextStyle.Default,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
) {
    val request = state.shown ?: return
    var origin by remember { mutableStateOf<Offset?>(null) }
    val inputMode = LocalInputModeManager.current
    val menuFocus = remember(request) { FocusRequester() }
    val submenus = remember(request) { SubmenuSlot() }
    var holdsFocus by remember(request) { mutableStateOf(false) }
    LaunchedEffect(request) {
        snapshotFlow { request.expanded || request.morph.isShown }.first { !it }
        state.release(request)
    }
    // A menu opened from the keys takes the focus to its first row, and hands it back to its control as it closes.
    LaunchedEffect(request, request.expanded) {
        if (request.expanded && inputMode.inputMode == InputMode.Keyboard) {
            withFrameNanos {}
            menuFocus.requestFocus()
        } else if (!request.expanded && holdsFocus && request.morph.anchorInWindow != null) {
            request.morph.anchorFocus.requestFocus()
        }
    }
    BoxWithConstraints(modifier.fillMaxSize().onPlaced { origin = it.positionInWindow() }) {
        // Until the host is placed, the control's place in it is unknown and nothing is drawn.
        val placed = origin ?: return@BoxWithConstraints
        val anchor = (request.morph.anchorInWindow ?: Rect.Zero).translate(-placed)
        val preview = request.preview
        // A lifted control places its menu beside the whole of it, and the glass grows from a capsule on its edge.
        val density = LocalDensity.current
        val lifted = anchor.lifted(metrics.previewLift)
        val shift = request.morph.openBounds?.let { menu -> preview?.shift(lifted, menu, with(density) { preview.gap.toPx() }) } ?: 0f
        val seed = preview?.seed(lifted.translate(0f, shift), request.morph.openBounds, constraints.maxHeight.toFloat(), with(density) { SeedHeight.toPx() })
        // A popover grows from a drop of glass at its control's edge, which stays joined to it as its point.
        val point = if (request.popover) popoverPoint(anchor, constraints.maxHeight.toFloat(), density) else null
        val placement = when {
            preview != null -> request.placement.around(lifted)
            point != null -> request.placement.around(anchor)
            else -> request.placement
        }
        if (preview != null) LiftedPreview(preview, anchor, shift, metrics.previewLift) { request.morph.progress.value }
        if (request.expanded) {
            BackHandler(onBack = request.onDismissRequest)
            Box(
                Modifier
                    .fillMaxSize()
                    .then(request.outsideModifier)
                    .pointerInput(request) { keepPresses(request.onDismissRequest) },
            )
        }
        QuvenGlassMorph(
            state = request.morph,
            expanded = request.expanded,
            anchor = seed ?: point ?: anchor,
            placement = placement,
            modifier = Modifier.fillMaxSize(),
            style = when {
                preview != null -> remember(style) { style.dimmed(ScreenDim) }
                point != null -> remember(style) { style.copy(pressGlow = 0f) }
                else -> style
            },
            cornerRadius = metrics.cornerRadius,
            backdrop = backdrop,
            reduceMotion = reduceMotion,
            face = request.face,
            fromControl = preview == null,
            keepsSource = point != null,
        ) {
            val panel = request.menuModifier
                .focusRequester(menuFocus)
                .onFocusChanged { holdsFocus = it.hasFocus }
                .focusProperties { onExit = { if (request.expanded) cancelFocusChange() } }
                .focusGroup()
                .heightIn(max = maxHeight - MenuEdge * 2)
            if (request.popover) {
                Column(panel, content = request.content)
            } else {
                CompositionLocalProvider(LocalSubmenuSlot provides submenus) {
                    QuvenGlassMenu(
                        panel,
                        metrics = metrics,
                        colors = colors,
                        textStyle = textStyle,
                        onChosen = request.onDismissRequest,
                        rising = request.morph.rises,
                        content = request.content,
                    )
                }
            }
        }
        submenus.shown?.let { submenu ->
            OpenSubmenu(submenu, submenus, request, placed, style, metrics, colors, textStyle, backdrop, reduceMotion, maxHeight)
        }
    }
}

/**
 * Draws [submenu] growing out of its entry's row over the menu [parent] holds open, as wide as that menu and headed by
 * the entry, its head centred on the row; Back or a press on the head closes it, and it closes with its menu.
 *
 * @param submenu The submenu.
 * @param slot The slot holding it.
 * @param parent The menu it opens over.
 * @param placed Where the host stands in the window.
 * @param style The material of the glass.
 * @param metrics The layout of the rows.
 * @param colors The colours of the rows.
 * @param textStyle The typeface of the names.
 * @param backdrop The backdrop the glass stands over.
 * @param reduceMotion Whether motion is reduced.
 * @param maxHeight The height of the host.
 */
@Composable
private fun OpenSubmenu(
    submenu: SubmenuRequest,
    slot: SubmenuSlot,
    parent: DropdownRequest,
    placed: Offset,
    style: QuvenGlassStyle,
    metrics: QuvenGlassMenuMetrics,
    colors: QuvenGlassMenuColors,
    textStyle: TextStyle,
    backdrop: QuvenGlassBackdrop?,
    reduceMotion: Boolean,
    maxHeight: Dp,
) {
    val expanded = slot.expanded && parent.expanded
    LaunchedEffect(submenu) {
        snapshotFlow { slot.expanded || submenu.morph.isShown }.first { !it }
        slot.release(submenu)
    }
    if (expanded) BackHandler(onBack = slot::close)
    val row = (submenu.morph.anchorInWindow ?: Rect.Zero).translate(-placed)
    // A morph's glass is born from its anchor's middle down, so an anchor reaching a row above has it born as the row.
    val grown = Rect(row.left, row.top - row.height, row.right, row.bottom)
    val headCentre = metrics.verticalInset + metrics.rowHeight / 2
    QuvenGlassMorph(
        state = submenu.morph,
        expanded = expanded,
        anchor = grown,
        placement = remember(headCentre) { headedOn(headCentre) },
        modifier = Modifier.fillMaxSize(),
        style = style,
        cornerRadius = metrics.cornerRadius,
        backdrop = backdrop,
        reduceMotion = reduceMotion,
        fromControl = false,
    ) {
        val width = with(LocalDensity.current) { row.width.toDp() }
        CompositionLocalProvider(LocalSubmenuSlot provides null) {
            QuvenGlassMenu(
                Modifier.width(width).heightIn(max = maxHeight - MenuEdge * 2),
                metrics = metrics,
                colors = colors,
                textStyle = textStyle,
                onChosen = parent.onDismissRequest,
            ) {
                SubmenuHead(submenu, onClose = slot::close)
                QuvenGlassMenuDivider()
                submenu.content(this)
            }
        }
    }
}

/**
 * Returns the placement that stands a submenu over its entry's row, the lower half of its anchor, aligned with the row's
 * start and its head, [headCentre] below its top, centred on the row, inside the space.
 *
 * @param headCentre The distance from the submenu's top to its head's middle.
 * @return The placement.
 */
private fun headedOn(headCentre: Dp): QuvenGlassMorphPlacement = QuvenGlassMorphPlacement { size, anchor, space, density ->
    val top = anchor.bottom - anchor.height / 4 - with(density) { headCentre.roundToPx() }
    IntOffset(anchor.left, top.coerceIn(0, (space.height - size.height).coerceAtLeast(0)))
}

/**
 * Opens a glass menu out of the control [state] marks with [quvenGlassAnchor], in the [QuvenGlassMenuHost] the screen
 * provides through [LocalQuvenGlassMenuHost]. The dropdown draws nothing where it stands; the control hides while
 * [QuvenGlassMorphState.isShown] is `true`, as the glass takes its place.
 *
 * @param state The opening, whose control the menu grows from.
 * @param expanded Whether the menu is open.
 * @param onDismissRequest Invoked when the viewer closes the menu by a press elsewhere or Back, or chooses a row.
 * @param modifier Modifier applied to the open menu.
 * @param outsideModifier Modifier applied to the layer outside the menu that closes it.
 * @param placement Where the open menu stands.
 * @param face Draws the control's face inside the glass while it starts to grow.
 * @param content The menu's entries: [QuvenGlassMenuTitle], [QuvenGlassMenuItem], [QuvenGlassMenuChoice] and
 * [QuvenGlassMenuDivider].
 * @throws IllegalStateException No [QuvenGlassMenuHost] is provided above the dropdown.
 */
@Composable
public fun QuvenGlassDropdown(
    state: QuvenGlassMorphState,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    outsideModifier: Modifier = Modifier,
    placement: QuvenGlassMorphPlacement = DefaultDropdownPlacement,
    face: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassDropdown(state, expanded, onDismissRequest, modifier, outsideModifier, placement, face, preview = null, content = content)
}

/**
 * Asks the [QuvenGlassMenuHost] above to draw a menu growing out of the control [state] marks, lifting [preview] over a
 * dimmed screen where one is given.
 *
 * @param state The opening, whose control the menu grows from.
 * @param expanded Whether the menu is open.
 * @param onDismissRequest Invoked when the viewer closes the menu or chooses a row.
 * @param modifier Modifier applied to the open menu.
 * @param outsideModifier Modifier applied to the layer outside the menu that closes it.
 * @param placement Where the open menu stands.
 * @param face Draws the control's face inside the glass while it starts to grow.
 * @param preview The control lifted over the dimmed screen while the menu is shown, or `null` for a menu that dims
 * nothing.
 * @param popover Whether [content] is a panel of its own that points at its control, rather than a menu's entries.
 * @param content The menu's entries, or the popover's panel.
 * @throws IllegalStateException No [QuvenGlassMenuHost] is provided above.
 */
@Composable
internal fun GlassDropdown(
    state: QuvenGlassMorphState,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    outsideModifier: Modifier,
    placement: QuvenGlassMorphPlacement,
    face: @Composable () -> Unit,
    preview: GlassMenuPreview?,
    popover: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val host = checkNotNull(LocalQuvenGlassMenuHost.current) { "A glass menu opens in a QuvenGlassMenuHost above it." }
    val request = remember(state) { DropdownRequest(state) }
    SideEffect {
        request.expanded = expanded
        request.onDismissRequest = onDismissRequest
        request.menuModifier = modifier
        request.outsideModifier = outsideModifier
        request.placement = placement
        request.face = face
        request.preview = preview
        request.popover = popover
        request.content = content
    }
    LaunchedEffect(host, request, expanded) {
        if (expanded) host.open(request)
    }
    DisposableEffect(host, request) {
        onDispose { host.release(request) }
    }
}

/** The scope of a [QuvenGlassMenuBox]'s control: whether its menu is open, how to open it and what it grows from. */
@Stable
public interface QuvenGlassMenuBoxScope {

    /**
     * Gets the opening of the menu, which a control drawing glass of its own marks with [quvenGlassAnchor] and hides
     * by while [QuvenGlassMorphState.isShown] is `true`.
     */
    public val menuState: QuvenGlassMorphState

    /** Gets a value indicating whether the menu is open. */
    public val isMenuOpen: Boolean

    /**
     * Gets a value indicating whether the menu's glass is on screen, opening, open or closing; a glass control hides
     * meanwhile, as the menu takes its place.
     */
    public val isMenuShown: Boolean

    /** Opens the menu. */
    public fun openMenu()

    /**
     * Marks the control the menu grows out of.
     *
     * @return The decorated modifier.
     */
    public fun Modifier.menuAnchor(): Modifier
}

/**
 * Pairs a control with the glass menu it opens: the [control] opens the menu through its scope and marks itself with
 * [QuvenGlassMenuBoxScope.menuAnchor]; the menu closes on a choice, a press elsewhere or Back.
 *
 * @param menu The menu's entries.
 * @param placement Where the open menu stands.
 * @param face Draws the control's face inside the glass while it starts to grow.
 * @param control Draws the control.
 * @throws IllegalStateException No [QuvenGlassMenuHost] is provided above the box.
 */
@Composable
public fun QuvenGlassMenuBox(
    menu: @Composable ColumnScope.() -> Unit,
    placement: QuvenGlassMorphPlacement = DefaultDropdownPlacement,
    face: @Composable () -> Unit = {},
    control: @Composable QuvenGlassMenuBoxScope.() -> Unit,
) {
    val scope = remember { MenuBoxScope() }
    scope.control()
    QuvenGlassDropdown(
        state = scope.menuState,
        expanded = scope.isMenuOpen,
        onDismissRequest = scope::closeMenu,
        placement = placement,
        face = face,
        content = menu,
    )
}

/** The [QuvenGlassMenuBoxScope] a [QuvenGlassMenuBox] gives its control. */
private class MenuBoxScope : QuvenGlassMenuBoxScope {
    override val menuState = QuvenGlassMorphState()
    override var isMenuOpen by mutableStateOf(false)
        private set
    override val isMenuShown: Boolean
        get() = menuState.isShown

    override fun openMenu() {
        isMenuOpen = true
    }

    /** Closes the menu. */
    fun closeMenu() {
        isMenuOpen = false
    }

    override fun Modifier.menuAnchor(): Modifier = quvenGlassAnchor(menuState)
}

/**
 * A menu a [QuvenGlassDropdown] asks its host to draw, kept in step with the dropdown's parameters.
 *
 * @property morph The opening, whose control the menu grows from.
 */
internal class DropdownRequest(val morph: QuvenGlassMorphState) {

    /** Gets or sets a value indicating whether the menu is open. */
    var expanded: Boolean by mutableStateOf(false)

    /** Gets or sets what closes the menu. */
    var onDismissRequest: () -> Unit by mutableStateOf({})

    /** Gets or sets the modifier applied to the open menu. */
    var menuModifier: Modifier by mutableStateOf(Modifier)

    /** Gets or sets the modifier applied to the layer outside the menu. */
    var outsideModifier: Modifier by mutableStateOf(Modifier)

    /** Gets or sets where the open menu stands. */
    var placement: QuvenGlassMorphPlacement by mutableStateOf(DefaultDropdownPlacement)

    /** Gets or sets the control's face, drawn inside the glass while it starts to grow. */
    var face: @Composable () -> Unit by mutableStateOf({})

    /** Gets or sets the menu's entries. */
    var content: @Composable ColumnScope.() -> Unit by mutableStateOf({})

    /** Gets or sets the control lifted over the dimmed screen while the menu is shown, or `null` for none. */
    var preview: GlassMenuPreview? by mutableStateOf(null)

    /** Gets or sets a value indicating whether the content is a popover's panel rather than a menu's entries. */
    var popover: Boolean by mutableStateOf(false)
}

/**
 * Returns the drop of glass a popover grows from: a circle beyond [control]'s edge on the side the popover opens to,
 * centred on it, which stays joined to the open panel as its point.
 *
 * @param control The control's bounds.
 * @param spaceHeight The height of the space the popover stands in.
 * @param density The density the point is measured in.
 * @return The drop's bounds.
 */
internal fun popoverPoint(control: Rect, spaceHeight: Float, density: Density): Rect = with(density) {
    val radius = PopoverPointDiameter.toPx() / 2f
    val reach = PopoverPointReach.toPx()
    val centreY = if (opensAbove(control.top, control.bottom, spaceHeight)) control.top - reach else control.bottom + reach
    Rect(Offset(control.center.x, centreY), radius)
}

/**
 * Draws a control's [preview] lifted out of the screen, which dims behind it, as far as [progress] has opened its menu:
 * the control grows to [lift] about its centre and moves by [shift], over a veil of black at [ScreenDim], as a
 * system context menu lifts its preview.
 *
 * @param preview The control.
 * @param anchor The control's bounds, in the host's coordinates.
 * @param shift How far the control moves down once lifted, negative to move up, to keep its menu beside it.
 * @param lift The share of its own size the control lifts to.
 * @param progress Reads how far the menu has opened, from 0 to 1, past 1 while its spring overshoots.
 */
@Composable
private fun LiftedPreview(preview: GlassMenuPreview, anchor: Rect, shift: Float, lift: Float, progress: () -> Float) {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val shown = progress().coerceIn(0f, 1f)
                drawRect(Color.Black, alpha = ScreenDim * shown)
                translate(anchor.left, anchor.top + shift * shown) {
                    scale(preview.liftAt(progress(), lift), pivot = Offset(anchor.width / 2f, anchor.height / 2f)) { drawLayer(preview.layer) }
                }
            },
    )
}

/**
 * Returns this placement measured against [anchor] whatever anchor the morph grows from.
 *
 * @param anchor The bounds to place the glass beside.
 * @return The placement.
 */
private fun QuvenGlassMorphPlacement.around(anchor: Rect): QuvenGlassMorphPlacement {
    val bounds = IntRect(anchor.left.roundToInt(), anchor.top.roundToInt(), anchor.right.roundToInt(), anchor.bottom.roundToInt())
    return QuvenGlassMorphPlacement { size, _, space, density -> place(size, bounds, space, density) }
}

/**
 * Returns this rectangle grown by [scale] about its centre.
 *
 * @param scale The factor to grow by.
 * @return The grown rectangle.
 */
private fun Rect.lifted(scale: Float): Rect {
    val grow = Offset(width * (scale - 1f) / 2f, height * (scale - 1f) / 2f)
    return Rect(topLeft - grow, bottomRight + grow)
}

/**
 * Keeps a press anywhere on this layer, and whatever it goes on to do, from what lies under it, and invokes [onPress]
 * as it lands.
 *
 * @param onPress Invoked when a press lands, such as closing a menu.
 */
internal suspend fun PointerInputScope.keepPresses(onPress: () -> Unit = {}) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false).consume()
    onPress()
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}

/** The diameter of the drop of glass a popover grows from, and keeps as its point. */
private val PopoverPointDiameter = 18.dp

/** How far beyond its control's edge the centre of a popover's point stands. */
private val PopoverPointReach = 13.dp

/** The room between a popover's panel and its control, which its point spans. */
internal val PopoverGap = 14.dp

/** The least room between a menu and the host's edges, which also caps a long menu's height. */
internal val MenuEdge = 16.dp

private val DefaultDropdownPlacement = QuvenGlassMorphPlacement.overAnchor(edge = MenuEdge)
