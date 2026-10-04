package tv.quven.glass.sample

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import tv.quven.glass.LocalQuvenGlassBackdrop
import tv.quven.glass.QuvenGlassButton
import tv.quven.glass.QuvenGlassButtonSize
import tv.quven.glass.QuvenGlassIconButton
import tv.quven.glass.QuvenGlassContainer
import tv.quven.glass.QuvenGlassContextMenuBox
import tv.quven.glass.QuvenGlassMenuBox
import tv.quven.glass.QuvenGlassMenuChoice
import tv.quven.glass.QuvenGlassMenuChoices
import tv.quven.glass.QuvenGlassMenuDivider
import tv.quven.glass.QuvenGlassMenuItem
import tv.quven.glass.QuvenGlassMenuTitle
import tv.quven.glass.QuvenGlassMorph
import tv.quven.glass.QuvenGlassMorphPlacement
import tv.quven.glass.QuvenGlassStyle
import tv.quven.glass.QuvenGlassSlider
import tv.quven.glass.QuvenGlassSwitch
import tv.quven.glass.quvenLiquidGlass
import tv.quven.glass.rememberQuvenGlassAppearance
import tv.quven.glass.rememberQuvenGlassMorphState

/**
 * Draws still glass of several sizes, thin and thick, round and a capsule.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.MaterialExhibit(tuning: SampleTuning) {
    Row(
        Modifier.align(Alignment.Center),
        horizontalArrangement = Arrangement.spacedBy(ExhibitGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaterialSizes.forEach { side -> Glass(tuning, Modifier.size(side)) }
        Glass(tuning, Modifier.size(width = CapsuleWidth, height = CapsuleHeight))
    }
}

/**
 * Draws round glass buttons of three sizes.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.GlassButtonsExhibit(tuning: SampleTuning) {
    Row(
        Modifier.align(Alignment.Center),
        horizontalArrangement = Arrangement.spacedBy(ExhibitGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonFaces.forEach { (diameter, icon) ->
            SampleGlassButton(onClick = {}, tuning = tuning, diameter = diameter) { ink ->
                GlyphFace(icon, contentDescription = null, tint = ink, size = diameter * ButtonGlyphShare)
            }
        }
    }
}

/**
 * Draws a tablet's tab bar at the foot of the stage.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.TabBarExhibit(tuning: SampleTuning) {
    var held by remember { mutableIntStateOf(0) }
    Box(Modifier.align(Alignment.BottomCenter)) { SampleBar(held = held, onHold = { held = it }, tuning = tuning) }
}

/**
 * Draws a segmented control of three options.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.SegmentedExhibit(tuning: SampleTuning) {
    var held by remember { mutableIntStateOf(1) }
    Box(Modifier.align(Alignment.Center)) { SampleDensityTrack(held = held, onHold = { held = it }, tuning = tuning) }
}

/**
 * Draws two circles of glass that move together until they join and apart again, over and over.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.JoiningExhibit(tuning: SampleTuning) {
    val closeness by rememberInfiniteTransition(label = "joining").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(JoiningMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "closeness",
    )
    val reach = lerp(JoiningApart, JoiningTogether, closeness)
    QuvenGlassContainer(Modifier.align(Alignment.Center), style = tuning.style, spacing = JoiningSpacing) {
        Box(Modifier.size(width = JoiningApart * 2 + JoiningDiameter, height = JoiningDiameter)) {
            listOf(-1, 1).forEach { side ->
                Glass(
                    tuning,
                    Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset((side * reach.toPx()).toInt(), 0) }
                        .size(JoiningDiameter),
                )
            }
        }
    }
}

/**
 * Draws the four kinds of menu: titled sections of glyphed rows, choices among other rows, a plain menu and one that
 * rises from the foot of the stage.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.MenusExhibit(tuning: SampleTuning) {
    var order by remember { mutableIntStateOf(1) }
    var ascending by remember { mutableStateOf(true) }
    MenuButton(Icons.Filled.Settings, "Library", tuning, Modifier.align(Alignment.TopStart)) { LibraryMenuEntries() }
    MenuButton(Icons.Filled.MoreVert, "Options", tuning, Modifier.align(Alignment.TopEnd)) {
        QuvenGlassMenuTitle("Sort by")
        QuvenGlassMenuChoices(SortOrders, SortOrders[order], label = { it }, onSelect = { order = SortOrders.indexOf(it) })
        QuvenGlassMenuChoice("Ascending", selected = ascending, onClick = { ascending = !ascending }, role = Role.Checkbox)
        QuvenGlassMenuDivider()
        QuvenGlassMenuItem("Share", {}, icon = rememberVectorPainter(Icons.Filled.Share))
        QuvenGlassMenuItem("Unavailable", {}, icon = rememberVectorPainter(Icons.Filled.Lock), enabled = false)
        QuvenGlassMenuItem("Remove", {}, icon = rememberVectorPainter(Icons.Filled.Delete), destructive = true)
    }
    MenuButton(Icons.Filled.DateRange, "Timer", tuning, Modifier.align(Alignment.CenterStart)) {
        QuvenGlassMenuItem("15 minutes", {})
        QuvenGlassMenuItem("30 minutes", {})
        QuvenGlassMenuItem("End of chapter", {})
        QuvenGlassMenuItem("Off", {}, destructive = true)
    }
    MenuButton(Icons.Filled.Add, "New", tuning, Modifier.align(Alignment.BottomStart)) {
        QuvenGlassMenuItem("Collection", {}, icon = rememberVectorPainter(Icons.AutoMirrored.Filled.List))
        QuvenGlassMenuItem("Message", {}, icon = rememberVectorPainter(Icons.Filled.Email))
        QuvenGlassMenuItem("Order", {}, icon = rememberVectorPainter(Icons.Filled.ShoppingCart))
    }
}

/**
 * Draws a gear that opens into the panel tuning the material, as one piece of glass.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.MorphingPanelExhibit(tuning: SampleTuning) {
    val morph = rememberQuvenGlassMorphState()
    var open by remember { mutableStateOf(false) }
    var gear by remember { mutableStateOf(Rect.Zero) }
    SampleGlassButton(
        onClick = { open = true },
        tuning = tuning,
        modifier = Modifier.align(Alignment.TopStart).onPlaced { gear = it.boundsInParent() },
        shown = !morph.isShown,
    ) { ink -> GlyphFace(Icons.Filled.Settings, contentDescription = "Tune", tint = ink) }
    if (open) {
        Box(Modifier.matchParentSize().clickable(interactionSource = null, indication = null) { open = false })
    }
    QuvenGlassMorph(
        state = morph,
        expanded = open,
        anchor = gear,
        width = PanelWidth,
        placement = QuvenGlassMorphPlacement.hangingFromTopLeft(),
        modifier = Modifier.matchParentSize(),
        style = tuning.style,
        cornerRadius = PanelCornerRadius,
        reduceMotion = tuning.reduceMotion,
        face = { GlyphFace(Icons.Filled.Settings, contentDescription = null) },
    ) {
        TuningPanel(tuning, Modifier.heightIn(max = PanelMaxHeight))
    }
}

/**
 * Draws regular, clear and tinted glass two by two, each named under it.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.ClearAndTintedExhibit(tuning: SampleTuning) {
    // Two by two, so the four stand inside the stage.
    Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(ExhibitGap)) {
        GlassVariants.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(ExhibitGap), verticalAlignment = Alignment.CenterVertically) {
                pair.forEach { (name, style) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(VariantDiameter)
                                .quvenLiquidGlass(LocalQuvenGlassBackdrop.current, style, CircleShape, reduceMotion = tuning.reduceMotion),
                        )
                        GlassCaption(name, tuning)
                    }
                }
            }
        }
    }
}

/**
 * Draws capsule buttons of plain and prominent glass in three sizes, as the reference lays out Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.CapsuleButtonsExhibit(tuning: SampleTuning) {
    val play = rememberVectorPainter(Icons.Filled.PlayArrow)
    val heart = rememberVectorPainter(Icons.Filled.Favorite)
    val down = rememberVectorPainter(ArrowDownward)
    val star = rememberVectorPainter(Icons.Filled.Star)
    // Rows short enough for the stage, as the reference lays them out.
    Column(
        Modifier.align(Alignment.Center),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ButtonRow {
            QuvenGlassButton(onClick = {}, label = "Play", reduceMotion = tuning.reduceMotion)
            QuvenGlassButton(onClick = {}, label = "Play", icon = play, reduceMotion = tuning.reduceMotion)
            QuvenGlassIconButton(onClick = {}, icon = heart, contentDescription = "Favorite", reduceMotion = tuning.reduceMotion)
        }
        ButtonRow {
            QuvenGlassButton(onClick = {}, label = "Small", size = QuvenGlassButtonSize.Small, reduceMotion = tuning.reduceMotion)
            QuvenGlassButton(onClick = {}, label = "Mini", size = QuvenGlassButtonSize.Mini, reduceMotion = tuning.reduceMotion)
        }
        ButtonRow {
            QuvenGlassButton(onClick = {}, label = "Buy", tint = SystemBlue, reduceMotion = tuning.reduceMotion)
            QuvenGlassButton(onClick = {}, label = "Download", icon = down, tint = SampleColors.Accent, reduceMotion = tuning.reduceMotion)
            QuvenGlassButton(onClick = {}, label = "Delete", tint = SystemRed, reduceMotion = tuning.reduceMotion)
        }
        ButtonRow {
            QuvenGlassButton(onClick = {}, label = "Large", size = QuvenGlassButtonSize.Large, reduceMotion = tuning.reduceMotion)
            QuvenGlassButton(onClick = {}, label = "Extra large", size = QuvenGlassButtonSize.Large, reduceMotion = tuning.reduceMotion)
        }
        QuvenGlassButton(
            onClick = {},
            label = "Prominent",
            icon = star,
            size = QuvenGlassButtonSize.Large,
            tint = SystemBlue,
            reduceMotion = tuning.reduceMotion,
        )
    }
}

/**
 * Draws a switch that is on and one that is off, each beside its name, as the reference lays out Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.SwitchExhibit(tuning: SampleTuning) {
    var downloads by remember { mutableStateOf(true) }
    var subtitles by remember { mutableStateOf(false) }
    Column(Modifier.align(Alignment.Center).width(SwitchColumnWidth), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SwitchRow("Downloads", downloads, { downloads = it }, tuning)
        SwitchRow("Subtitles", subtitles, { subtitles = it }, tuning)
    }
}

/**
 * Draws a continuous slider and one with steps, as the reference lays out Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.SliderExhibit(tuning: SampleTuning) {
    var volume by remember { mutableFloatStateOf(0.4f) }
    var rating by remember { mutableFloatStateOf(3f) }
    Column(Modifier.align(Alignment.Center).width(SliderColumnWidth), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        QuvenGlassSlider(value = volume, onValueChange = { volume = it }, reduceMotion = tuning.reduceMotion)
        QuvenGlassSlider(value = rating, onValueChange = { rating = it }, valueRange = 0f..5f, steps = 4, reduceMotion = tuning.reduceMotion)
    }
}

/**
 * Draws two cards that lift out of the stage on a long press and open their menus beside them, as the reference lays
 * out Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.ContextMenuExhibit(tuning: SampleTuning) {
    val play = rememberVectorPainter(Icons.Filled.PlayArrow)
    Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        QuvenGlassContextMenuBox(
            menu = {
                QuvenGlassMenuItem("Play", {}, icon = play)
                QuvenGlassMenuItem("Details", {}, icon = rememberVectorPainter(Icons.Filled.Info))
                QuvenGlassMenuItem("Save", {}, icon = rememberVectorPainter(Icons.Filled.Favorite))
                QuvenGlassMenuItem("Remove", {}, icon = rememberVectorPainter(Icons.Filled.Delete), destructive = true)
            },
            onClick = {},
            reduceMotion = tuning.reduceMotion,
        ) { SamplePosterCard(2) }
        QuvenGlassContextMenuBox(
            menu = {
                QuvenGlassMenuItem("Play", {}, icon = play)
                QuvenGlassMenuItem("Share", {}, icon = rememberVectorPainter(Icons.Filled.Share))
            },
            onClick = {},
            reduceMotion = tuning.reduceMotion,
        ) { SamplePosterCard(6) }
    }
}

/**
 * Lays out a switch at the end of a row holding its name.
 *
 * @param name The switch's name.
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked with the switch's new state.
 * @param tuning The live settings.
 */
@Composable
private fun SwitchRow(name: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, tuning: SampleTuning) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        GlassCaption(name, tuning)
        QuvenGlassSwitch(checked = checked, onCheckedChange = onCheckedChange, reduceMotion = tuning.reduceMotion)
    }
}

/**
 * Lays out a row of buttons, centred on one another.
 *
 * @param content The buttons.
 */
@Composable
private fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

/**
 * Draws a short name on a capsule of glass, readable over any content.
 *
 * @param text The name.
 * @param tuning The live settings.
 */
@Composable
private fun GlassCaption(text: String, tuning: SampleTuning) {
    val appearance = rememberQuvenGlassAppearance()
    Text(
        text,
        color = appearance.contentColor(SampleColors.TextHigh, SampleColors.TextHighOnLight),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .quvenLiquidGlass(
                LocalQuvenGlassBackdrop.current,
                QuvenGlassStyle.Standard,
                CircleShape,
                reduceMotion = tuning.reduceMotion,
                appearance = appearance,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * Draws the card of an exhibit the library does not draw yet, pointing to the reference that shows Apple's element.
 *
 * @param exhibit The exhibit.
 * @param tuning The live settings.
 * @param modifier Modifier applied to the card.
 */
@Composable
internal fun PendingExhibitCard(exhibit: Exhibit, tuning: SampleTuning, modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(max = PendingCardWidth)
            .quvenLiquidGlass(
                backdrop = LocalQuvenGlassBackdrop.current,
                style = tuning.style,
                shape = RoundedCornerShape(PanelCornerRadius),
                reduceMotion = tuning.reduceMotion,
            )
            .padding(PendingCardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(exhibit.status.label, color = exhibit.status.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(exhibit.title, color = SampleColors.TextHigh, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "The iOS reference shows Apple's element under the same name.",
            color = SampleColors.TextHigh.copy(alpha = 0.8f),
            fontSize = 15.sp,
        )
    }
}

/**
 * Draws still glass of the live material.
 *
 * @param tuning The live settings.
 * @param modifier Modifier applied to the glass, which sizes it.
 */
@Composable
private fun Glass(tuning: SampleTuning, modifier: Modifier) {
    Box(modifier.quvenLiquidGlass(LocalQuvenGlassBackdrop.current, tuning.style, CircleShape, reduceMotion = tuning.reduceMotion))
}

/**
 * Draws a round glass button that opens a glass menu, hiding while the menu's glass takes its place.
 *
 * @param icon The button's glyph.
 * @param name The button's name.
 * @param tuning The live settings.
 * @param modifier Modifier applied to the button.
 * @param menu The menu's entries.
 */
@Composable
private fun MenuButton(icon: ImageVector, name: String, tuning: SampleTuning, modifier: Modifier, menu: @Composable ColumnScope.() -> Unit) {
    QuvenGlassMenuBox(menu = menu, face = { GlyphFace(icon, contentDescription = null) }) {
        SampleGlassButton(
            onClick = { openMenu() },
            tuning = tuning,
            modifier = modifier.menuAnchor(),
            shown = !isMenuShown,
        ) { ink -> GlyphFace(icon, contentDescription = name, tint = ink) }
    }
}

private val ExhibitGap = 28.dp
private val MaterialSizes = listOf(36.dp, 51.dp, 70.dp, 100.dp)
private val CapsuleWidth = 240.dp
private val CapsuleHeight = 62.dp
private val SwitchColumnWidth = 280.dp
private val SliderColumnWidth = 352.dp
private val ButtonFaces: List<Pair<Dp, ImageVector>> = listOf(
    46.dp to Icons.Filled.PlayArrow,
    56.dp to Icons.Filled.Favorite,
    69.dp to Icons.Filled.Share,
)
private const val ButtonGlyphShare = 0.42f
private const val JoiningMillis = 1800
private val JoiningDiameter = 90.dp
private val JoiningApart = 80.dp
private val JoiningTogether = 38.dp
private val JoiningSpacing = 24.dp
private val SortOrders = listOf("Title", "Year", "Added")
private val PanelWidth = 340.dp
private val PanelMaxHeight = 560.dp
private val PanelCornerRadius = 28.dp
private val PendingCardWidth = 440.dp
private val PendingCardPadding = 28.dp
private val VariantDiameter = 100.dp
private val SystemBlue = Color(0xFF0091FF)
private val SystemRed = Color(0xFFFF453A)
/** A downward arrow, as the system's `arrow.down` symbol draws it. */
private val ArrowDownward: ImageVector = materialIcon(name = "Filled.ArrowDownward") {
    materialPath {
        moveTo(20f, 12f)
        lineToRelative(-1.41f, -1.41f)
        lineTo(13f, 16.17f)
        verticalLineTo(4f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(12.17f)
        lineToRelative(-5.58f, -5.59f)
        lineTo(4f, 12f)
        lineToRelative(8f, 8f)
        lineToRelative(8f, -8f)
        close()
    }
}

private val GlassVariants: List<Pair<String, QuvenGlassStyle>> = listOf(
    "Regular" to QuvenGlassStyle.Standard,
    "Clear" to QuvenGlassStyle.Clear,
    "Tinted" to QuvenGlassStyle.Standard.tinted(SampleColors.Accent),
    "Clear, tinted" to QuvenGlassStyle.Clear.copy(tint = Color(0x662979FF)),
)
