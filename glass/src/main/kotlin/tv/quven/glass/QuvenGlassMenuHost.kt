package tv.quven.glass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
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
 * over a layer that closes it on a press elsewhere and dims nothing; Back closes it too. Place it last in the screen's
 * root, where it fills the window, and provide [state] through [LocalQuvenGlassMenuHost] to the content.
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
    var origin by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(request) {
        snapshotFlow { request.expanded || request.morph.isShown }.first { !it }
        state.release(request)
    }
    Box(modifier.fillMaxSize().onPlaced { origin = it.positionInWindow() }) {
        if (request.expanded) {
            BackHandler(onBack = request.onDismissRequest)
            Box(
                Modifier
                    .fillMaxSize()
                    .then(request.outsideModifier)
                    .pointerInput(request) { dismissOnPress(request.onDismissRequest) },
            )
        }
        QuvenGlassMorph(
            state = request.morph,
            expanded = request.expanded,
            anchor = (request.morph.anchorInWindow ?: Rect.Zero).translate(-origin),
            placement = request.placement,
            modifier = Modifier.fillMaxSize(),
            style = style,
            cornerRadius = metrics.cornerRadius,
            backdrop = backdrop,
            reduceMotion = reduceMotion,
            face = request.face,
        ) {
            QuvenGlassMenu(
                request.menuModifier,
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
    val host = checkNotNull(LocalQuvenGlassMenuHost.current) { "A QuvenGlassDropdown opens in a QuvenGlassMenuHost above it." }
    val request = remember(state) { DropdownRequest(state) }
    SideEffect {
        request.expanded = expanded
        request.onDismissRequest = onDismissRequest
        request.menuModifier = modifier
        request.outsideModifier = outsideModifier
        request.placement = placement
        request.face = face
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
    var expanded by mutableStateOf(false)
    var onDismissRequest: () -> Unit by mutableStateOf({})
    var menuModifier: Modifier by mutableStateOf(Modifier)
    var outsideModifier: Modifier by mutableStateOf(Modifier)
    var placement: QuvenGlassMorphPlacement by mutableStateOf(DefaultDropdownPlacement)
    var face: @Composable () -> Unit by mutableStateOf({})
    var content: @Composable ColumnScope.() -> Unit by mutableStateOf({})
}

/**
 * Closes the menu on a press anywhere outside it and keeps that press, and whatever it goes on to do, from what lies
 * under the menu.
 *
 * @param onDismiss Closes the menu.
 */
private suspend fun PointerInputScope.dismissOnPress(onDismiss: () -> Unit) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false).consume()
    onDismiss()
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}

private val DefaultDropdownPlacement = QuvenGlassMorphPlacement.overAnchor(edge = 16.dp)
