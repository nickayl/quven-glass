package tv.quven.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch

/** The part an action of a [QuvenGlassAlert] plays, which sets how its name is drawn. */
public enum class QuvenGlassAlertRole {
    /** An ordinary action. */
    Default,

    /** The action that leaves things as they were. */
    Cancel,

    /** An action that cannot be undone, named in red. */
    Destructive,
}

/**
 * One action of a [QuvenGlassAlert]: a capsule holding its name.
 *
 * @property label The action's name.
 * @property onClick Invoked when the action is chosen; the alert closes once its owner stops drawing it.
 * @property role The part the action plays.
 * @property enabled Whether the action can be chosen.
 * @property modifier Modifier applied to the action's capsule.
 */
@Immutable
public class QuvenGlassAlertAction(
    public val label: String,
    public val onClick: () -> Unit,
    public val role: QuvenGlassAlertRole = QuvenGlassAlertRole.Default,
    public val enabled: Boolean = true,
    public val modifier: Modifier = Modifier,
)

/**
 * The alerts a screen raises: one stands at a time, in the [QuvenGlassAlertHost] it is given to, while every
 * [QuvenGlassAlert] under [LocalQuvenGlassAlertHost] raises its own.
 */
@Stable
public class QuvenGlassAlertHostState internal constructor() {

    /** Gets the alert standing or fading away. */
    internal val slot: PresentationSlot<AlertRequest> = PresentationSlot()
}

/**
 * Creates and remembers a [QuvenGlassAlertHostState].
 *
 * @return The state.
 */
@Composable
public fun rememberQuvenGlassAlertHostState(): QuvenGlassAlertHostState = remember { QuvenGlassAlertHostState() }

/** Gets the alerts the screen's [QuvenGlassAlert]s raise, or `null` where no host is provided. */
public val LocalQuvenGlassAlertHost: ProvidableCompositionLocal<QuvenGlassAlertHostState?> = staticCompositionLocalOf { null }

/**
 * Raises an alert on glass in the middle of the screen, as Apple's alerts stand: the screen dims behind it while it
 * settles from a little larger, and it fades away when it is no longer drawn. The title and the message stand at its
 * start, over its actions: two side by side, more one above another, in the order given. The alert stands while this
 * is drawn; Back invokes [onDismissRequest], and a press outside it does nothing.
 *
 * @param title The alert's question or statement.
 * @param actions The alert's actions, in the order they are drawn.
 * @param modifier Modifier applied to the alert's glass.
 * @param message What the alert explains under its title, or `null` for nothing.
 * @param onDismissRequest Invoked on Back, or `null` for an alert Back does not close.
 * @param content Draws more under the message, such as a box to tick, or `null` for nothing.
 * @throws IllegalStateException No [QuvenGlassAlertHost] is provided above the alert.
 */
@Composable
public fun QuvenGlassAlert(
    title: String,
    actions: List<QuvenGlassAlertAction>,
    modifier: Modifier = Modifier,
    message: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val host = checkNotNull(LocalQuvenGlassAlertHost.current) { "A QuvenGlassAlert needs a QuvenGlassAlertHost above it." }
    val request = remember { AlertRequest() }
    SideEffect { request.update(title, message, actions, modifier, onDismissRequest, content) }
    PresentWhileComposed(host.slot, request)
}

/**
 * Draws the alert a screen has raised above everything drawn before it, over a veil that dims the screen and keeps its
 * presses from what lies under it. Place it last in the screen's root, where it fills the window, and provide [state]
 * through [LocalQuvenGlassAlertHost] to the content.
 *
 * @param state The alerts the screen raises.
 * @param modifier Modifier applied to the host, which fills its parent.
 * @param style The material of the alert's glass.
 * @param textStyle The typeface of the alert's title, message and actions.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param reduceMotion Whether motion is reduced: the alert then fades in at its own size.
 */
@Composable
public fun QuvenGlassAlertHost(
    state: QuvenGlassAlertHostState,
    modifier: Modifier = Modifier,
    style: QuvenGlassStyle = QuvenGlassStyle.Standard,
    textStyle: TextStyle = TextStyle.Default,
    backdrop: QuvenGlassBackdrop? = LocalQuvenGlassBackdrop.current,
    reduceMotion: Boolean = false,
) {
    val request = state.slot.shown ?: return
    val dim = remember(request) { Animatable(0f) }
    val shown = remember(request) { Animatable(0f) }
    val settle = remember(request) { Animatable(if (reduceMotion) 1f else OpeningScale) }
    LaunchedEffect(request, request.standing) {
        if (request.standing) {
            launch { dim.animateTo(1f, tween(SettleMillis, easing = LinearOutSlowInEasing)) }
            launch { settle.animateTo(1f, tween(SettleMillis, easing = LinearOutSlowInEasing)) }
            shown.animateTo(1f, tween(FadeInMillis, easing = LinearOutSlowInEasing))
        } else {
            // Gone, it fades at its own size while the screen brightens a little longer behind it.
            launch { shown.animateTo(0f, tween(FadeOutMillis, easing = LinearOutSlowInEasing)) }
            dim.animateTo(0f, tween(UndimMillis, easing = FastOutSlowInEasing))
            state.slot.release(request)
        }
    }
    if (request.standing) BackHandler(enabled = request.onDismissRequest != null) { request.onDismissRequest?.invoke() }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color.Black, alpha = ScreenDim * dim.value) }
            .pointerInput(request) { keepPresses() },
        contentAlignment = Alignment.Center,
    ) {
        AlertPanel(
            request = request,
            width = min(AlertWidth, maxWidth - AlertEdge * 2),
            maxHeight = maxHeight - AlertEdge * 2,
            style = remember(style) { style.forAlerts() },
            textStyle = textStyle,
            backdrop = backdrop,
            shown = { shown.value },
            settle = { settle.value },
            reduceMotion = reduceMotion,
        )
    }
}

/**
 * An alert raised in a [QuvenGlassAlertHost]: what [QuvenGlassAlert] last drew, kept while the alert fades away.
 */
@Stable
internal class AlertRequest : GlassPresentation() {

    /** Gets the alert's title. */
    var title: String by mutableStateOf("")
        private set

    /** Gets the alert's message, or `null` for none. */
    var message: String? by mutableStateOf(null)
        private set

    /** Gets the alert's actions. */
    var actions: List<QuvenGlassAlertAction> by mutableStateOf(emptyList())
        private set

    /** Gets the modifier applied to the alert's glass. */
    var modifier: Modifier by mutableStateOf(Modifier)
        private set

    /** Gets what Back invokes, or `null` where Back does not close the alert. */
    var onDismissRequest: (() -> Unit)? by mutableStateOf(null)
        private set

    /** Gets what the alert draws under its message, or `null` for nothing. */
    var content: (@Composable ColumnScope.() -> Unit)? by mutableStateOf(null)
        private set

    /**
     * Takes what the alert's owner draws now.
     *
     * @param title The alert's title.
     * @param message The alert's message, or `null` for none.
     * @param actions The alert's actions.
     * @param modifier The modifier applied to the alert's glass.
     * @param onDismissRequest What Back invokes, or `null` for nothing.
     * @param content What the alert draws under its message, or `null` for nothing.
     */
    fun update(
        title: String,
        message: String?,
        actions: List<QuvenGlassAlertAction>,
        modifier: Modifier,
        onDismissRequest: (() -> Unit)?,
        content: (@Composable ColumnScope.() -> Unit)?,
    ) {
        this.title = title
        this.message = message
        this.actions = actions
        this.modifier = modifier
        this.onDismissRequest = onDismissRequest
        this.content = content
    }
}

/**
 * Draws an alert's glass: its title and message at its start over its actions, grown by [settle] and by a press on an
 * action, and as opaque as [shown]. Words too long for the window scroll above actions that stay in view.
 *
 * @param request The alert.
 * @param width The alert's width.
 * @param maxHeight The most the alert may stand tall.
 * @param style The material of the glass.
 * @param textStyle The typeface of the alert's words.
 * @param backdrop The backdrop the glass stands over, or `null` to draw the static material.
 * @param shown Reads the alert's opacity, from 0 to 1.
 * @param settle Reads how large the alert draws, as a share of its own size.
 * @param reduceMotion Whether motion is reduced: a press then does not grow the alert.
 */
@Composable
private fun AlertPanel(
    request: AlertRequest,
    width: Dp,
    maxHeight: Dp,
    style: QuvenGlassStyle,
    textStyle: TextStyle,
    backdrop: QuvenGlassBackdrop?,
    shown: () -> Float,
    settle: () -> Float,
    reduceMotion: Boolean,
) {
    val actions = request.actions
    val presses = remember(actions.size) { List(actions.size) { MutableInteractionSource() } }
    val pressed = presses.map { it.collectIsPressedAsState() }
    // Under the finger the whole alert grows a little, as the system's does.
    val held by animateFloatAsState(
        if (!reduceMotion && pressed.any(State<Boolean>::value)) 1f else 0f,
        ButtonSprings.rise,
        label = "alert press",
    )
    Column(
        request.modifier
            .width(width)
            .heightIn(max = maxHeight)
            .graphicsLayer {
                val grown = settle() * lerp(1f, PressedScale, held)
                scaleX = grown
                scaleY = grown
                alpha = shown()
            }
            .liquidGlass(
                backdrop = backdrop,
                style = style,
                shape = AlertShape,
                interactionSource = null,
                reduceMotion = reduceMotion,
                lift = null,
                pill = null,
                adapts = false,
            )
            .semantics {
                paneTitle = request.title
                request.onDismissRequest?.let { close ->
                    dismiss {
                        close()
                        true
                    }
                }
            }
            .focusGroup(),
    ) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = TextInset, end = TextInset, top = TopInset),
        ) {
            BasicText(request.title, style = textStyle.merge(TitleStyle))
            request.message?.let { message ->
                Spacer(Modifier.height(MessageGap))
                BasicText(message, style = textStyle.merge(MessageStyle))
            }
            request.content?.invoke(this)
        }
        Spacer(Modifier.height(ActionsGap))
        val sideBySide = actions.size == 2
        val arrangement = Arrangement.spacedBy(ActionGap)
        val inset = Modifier.padding(horizontal = ActionInset).fillMaxWidth()
        if (sideBySide) {
            Row(inset, horizontalArrangement = arrangement) {
                actions.forEachIndexed { index, action ->
                    AlertButton(action, presses[index], pressed[index].value, textStyle, Modifier.weight(1f))
                }
            }
        } else {
            Column(inset, verticalArrangement = arrangement) {
                actions.forEachIndexed { index, action ->
                    AlertButton(action, presses[index], pressed[index].value, textStyle, Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.height(BottomInset))
    }
}

/**
 * Draws one action of an alert: a capsule that takes most of the glass's colour out of what it covers and lays a little
 * white over it, a little more while it is pressed, holding the action's name.
 *
 * @param action The action.
 * @param interactions The source of the capsule's presses.
 * @param pressed Whether the capsule is pressed.
 * @param textStyle The typeface of the action's name.
 * @param modifier Modifier applied to the capsule, which sizes it across.
 */
@Composable
private fun AlertButton(
    action: QuvenGlassAlertAction,
    interactions: MutableInteractionSource,
    pressed: Boolean,
    textStyle: TextStyle,
    modifier: Modifier,
) {
    val lit by animateFloatAsState(if (pressed) 1f else 0f, tween(if (pressed) LightUpMillis else LightDownMillis), label = "alert action")
    val ink = if (action.role == QuvenGlassAlertRole.Destructive) DestructiveInk else Color.White
    Box(
        modifier
            .then(action.modifier)
            .height(ActionHeight)
            .clip(CircleShape)
            .drawBehind {
                drawRect(Color.Gray, alpha = ActionGreying, blendMode = BlendMode.Saturation)
                drawRect(Color.White, alpha = lerp(ActionWhite, PressedActionWhite, lit))
            }
            .clickable(interactions, indication = null, enabled = action.enabled, role = Role.Button, onClick = action.onClick)
            .alpha(if (action.enabled) 1f else DisabledAlpha),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(action.label, style = textStyle.merge(ActionStyle.copy(color = ink)), maxLines = 1)
    }
}


// Measured on the system's alert on an iPad.
private val AlertWidth = 319.dp
private val AlertEdge = 24.dp
private val AlertShape = RoundedCornerShape(33.dp)
private val TextInset = 31.dp
private val TopInset = 23.dp
private val MessageGap = 8.dp
private val ActionsGap = 20.dp
private val ActionInset = 15.5.dp
private val ActionGap = 8.dp
private val ActionHeight = 48.dp
private val BottomInset = 16.dp
private const val OpeningScale = 1.1f
private const val PressedScale = 1.016f
private const val SettleMillis = 300
private const val FadeInMillis = 200
private const val FadeOutMillis = 100
private const val UndimMillis = 230
private const val LightUpMillis = 70
private const val LightDownMillis = 250
private const val ActionGreying = 0.8f
private const val ActionWhite = 0.11f
private const val PressedActionWhite = 0.16f
private val DestructiveInk = Color(0xFFFF4245)
private val TitleStyle = TextStyle(color = Color.White, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
private val MessageStyle = TextStyle(color = Color(0x99EBEBF5), fontSize = 15.sp, lineHeight = 20.sp)
private val ActionStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp)
