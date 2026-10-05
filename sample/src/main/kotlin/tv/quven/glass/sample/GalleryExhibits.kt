package tv.quven.glass.sample

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.PathParser
import tv.quven.glass.QuvenGlassSplitView
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import tv.quven.glass.QuvenGlassSubmenu
import tv.quven.glass.QuvenGlassToolbarJoin
import tv.quven.glass.QuvenGlassToolbarGap
import tv.quven.glass.QuvenGlassToolbarItem
import tv.quven.glass.QuvenGlassToolbarGroup
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.layout.layout
import tv.quven.glass.QuvenGlassSegmentedTrack
import tv.quven.glass.rememberQuvenGlassBackdrop
import tv.quven.glass.quvenGlassSource
import tv.quven.glass.quvenGlassScrollEdge
import tv.quven.glass.QuvenGlassScrollEdgeStyle
import tv.quven.glass.QuvenGlassSegmentedControl
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import tv.quven.glass.rememberQuvenGlassBarMinimizer
import tv.quven.glass.QuvenGlassMinimizingBar
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import tv.quven.glass.LocalQuvenGlassBackdrop
import tv.quven.glass.QuvenGlassAlert
import tv.quven.glass.QuvenGlassAlertAction
import tv.quven.glass.QuvenGlassAlertRole
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
import tv.quven.glass.QuvenGlassPopover
import tv.quven.glass.QuvenGlassSearchMorph
import kotlinx.coroutines.delay
import tv.quven.glass.QuvenGlassSheet
import tv.quven.glass.QuvenGlassStyle
import tv.quven.glass.QuvenGlassSlider
import tv.quven.glass.QuvenGlassSwitch
import tv.quven.glass.quvenGlassAnchor
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
    // The circles over the capsule, so the whole exhibit stands inside the stage, as the reference's does.
    Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(ExhibitGap), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(ExhibitGap), verticalAlignment = Alignment.CenterVertically) {
            MaterialSizes.forEach { side -> Glass(tuning, Modifier.size(side)) }
        }
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
 * Draws a track of glass of three options over the system's segmented control of the same three, as the reference stacks
 * its density track over Apple's picker.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.SegmentedExhibit(tuning: SampleTuning) {
    var held by remember { mutableIntStateOf(1) }
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ExhibitGap)) {
        SampleDensityTrack(held = held, onHold = { held = it }, tuning = tuning)
        QuvenGlassSegmentedControl(
            options = DensityGlyphs,
            selected = held,
            onSelect = { held = DensityGlyphs.indexOf(it) },
            optionWidth = PickerSegment,
            style = tuning.style,
            reduceMotion = tuning.reduceMotion,
        ) { columns, _ -> DensityGlyph(columns, SampleColors.TextHigh, Modifier.size(16.dp)) }
    }
}

/** The columns each option of the density picker lays its squares in. */
private val DensityGlyphs = listOf(3, 2, 1)

/** The width of each option of the density picker, a third of the reference's 300 less the pill's inset. */
private val PickerSegment = 98.6.dp

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
    MenuButton(MoreHorizontal, "Options", tuning, Modifier.align(Alignment.TopEnd)) {
        QuvenGlassMenuTitle("Sort by")
        QuvenGlassMenuChoices(SortOrders, SortOrders[order], label = { it }, onSelect = { order = SortOrders.indexOf(it) })
        QuvenGlassMenuChoice("Ascending", selected = ascending, onClick = { ascending = !ascending }, role = Role.Checkbox)
        QuvenGlassMenuDivider()
        QuvenGlassMenuItem("Share", {}, icon = rememberVectorPainter(ShareUp))
        QuvenGlassMenuItem("Unavailable", {}, icon = rememberVectorPainter(Icons.Filled.Lock), enabled = false)
        QuvenGlassMenuItem("Remove", {}, icon = rememberVectorPainter(Icons.Filled.Delete), destructive = true)
    }
    MenuButton(MoonZzz, "Timer", tuning, Modifier.align(Alignment.CenterStart)) {
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
 * Draws a button whose menu holds entries that open second menus, as the reference's menu holding submenus does.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.SubmenusExhibit(tuning: SampleTuning) {
    MenuButton(MoreHorizontal, "Options", tuning, Modifier.align(Alignment.TopEnd)) {
        QuvenGlassMenuItem("Download", {}, icon = rememberVectorPainter(Icons.Filled.KeyboardArrowDown))
        QuvenGlassSubmenu("Share", icon = rememberVectorPainter(ShareUp)) {
            QuvenGlassMenuItem("Message", {}, icon = rememberVectorPainter(Icons.Filled.Email))
            QuvenGlassMenuItem("Mail", {}, icon = rememberVectorPainter(Icons.Filled.MailOutline))
        }
        QuvenGlassSubmenu("More", icon = rememberVectorPainter(MoreHorizontal)) {
            QuvenGlassMenuItem("First", {})
            QuvenGlassMenuItem("Second", {})
        }
    }
}

/**
 * Draws text on a panel of glass whose selection raises the menu of cut, copy and paste on glass, which the gallery's root
 * provides over the whole window, as the reference's text editor raises the system's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.TextMenuExhibit(tuning: SampleTuning) {
    var text by remember { mutableStateOf("Select a word of this text to raise the menu of cut, copy and paste.") }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        textStyle = TextStyle(color = SampleColors.TextHigh, fontSize = 20.sp),
        cursorBrush = SolidColor(SampleColors.Accent),
        modifier = Modifier
            .align(Alignment.Center)
            .size(TextPanelWidth, 200.dp)
            .quvenLiquidGlass(LocalQuvenGlassBackdrop.current, tuning.style, RoundedCornerShape(28.dp), reduceMotion = tuning.reduceMotion)
            .padding(16.dp),
    )
}

/**
 * Draws a split view over the whole stage: a sidebar of glass beside the stage's content, which slides out past the start
 * edge as the content widens, and back in, as the reference's split view shows and hides its sidebar.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.AdaptiveSidebarExhibit(tuning: SampleTuning) {
    var shown by remember { mutableStateOf(true) }
    val toggle = rememberVectorPainter(SidebarSymbol)
    QuvenGlassSplitView(
        sidebarShown = shown,
        modifier = Modifier.matchParentSize().bleed(StageInset),
        ground = SampleColors.Ground,
        style = tuning.style,
        reduceMotion = tuning.reduceMotion,
        sidebar = {
            Column {
                Box(Modifier.fillMaxWidth().height(SidebarHeadHeight), contentAlignment = Alignment.Center) {
                    Text("Library", color = SampleColors.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Icon(
                        toggle,
                        contentDescription = "Hide sidebar",
                        tint = SampleColors.TextHigh,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(24.dp)
                            .clickable(interactionSource = null, indication = null) { shown = false },
                    )
                }
                Spacer(Modifier.height(SidebarHeadGap))
                SidebarEntries.forEach { (label, icon) ->
                    Row(Modifier.fillMaxWidth().height(SidebarRowHeight), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(SidebarLabelStart), contentAlignment = Alignment.CenterEnd) {
                            Icon(icon, contentDescription = null, tint = SampleColors.TextHigh, modifier = Modifier.padding(end = 13.dp).size(24.dp))
                        }
                        Text(label, color = SampleColors.TextHigh, fontSize = 17.sp)
                    }
                }
            }
        },
    ) {
        // The button over the detail reads the detail itself, as the reference's toolbar button stands over its page.
        val detail = rememberQuvenGlassBackdrop()
        Box(Modifier.fillMaxSize().background(SampleColors.Ground).quvenGlassSource(detail)) {
            SampleBackdropContent(Modifier.fillMaxSize().padding(top = DetailBarHeight))
        }
        if (!shown) {
            // The reference's toolbar holds the button as a circle of 44, 10 in from the detail's corner.
            QuvenGlassToolbarGroup(
                listOf(QuvenGlassToolbarItem(toggle, "Show sidebar") { shown = true }),
                modifier = Modifier.align(Alignment.TopStart).padding(PageBarMargin),
                style = tuning.style,
                backdrop = detail.takeIf { tuning.liquid },
                reduceMotion = tuning.reduceMotion,
            )
        }
    }
}

/** The entries of the sidebar exhibit, as the reference lists them. */
private val SidebarEntries: List<Pair<String, ImageVector>> get() = listOf(
    "Home" to Icons.Filled.Home,
    "Watch" to PlayFill,
    "Favorites" to Icons.Filled.Star,
    "Explore" to Icons.Filled.Info,
    "More" to MoreHorizontal,
)

// The reference's sidebar list on an iPad: a head of 44 over rows of 52, labels 67 from the sidebar's edge.
private val SidebarHeadHeight = 44.dp
private val SidebarHeadGap = 10.dp
private val SidebarRowHeight = 52.dp
private val SidebarLabelStart = 67.dp

/** The height of the bar over the split view's detail, where its content starts. */
private val DetailBarHeight = 64.dp

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
 * Draws a phone's bar whose Search circle opens into a field along it, and whose folded tabs close the field, as the
 * reference's tab bar does.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.SearchExhibit(tuning: SampleTuning) {
    SearchBarStage(tuning, accessory = null)
}

/**
 * Draws a phone's bar with Search beside its tabs, minimizing as the stage scrolls, with [accessory] above it.
 *
 * @param tuning The live settings.
 * @param accessory Draws the accessory's content, or `null` for none.
 */
@Composable
private fun BoxScope.SearchBarStage(tuning: SampleTuning, accessory: (@Composable RowScope.() -> Unit)?) {
    var held by remember { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var searchLit by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    // On a tablet Search is a tab of its own, which shows its page with the field at its top, as the reference's tab
    // bar does on an iPad; on a phone it opens into a field along the bar.
    var searchPage by remember { mutableStateOf(false) }
    // The stage's scrolling minimizes the bar, as Search beside the reference's tabs minimizes with them.
    val minimizer = rememberQuvenGlassBarMinimizer()
    val stage = LocalStageScroll.current
    DisposableEffect(stage, minimizer) {
        stage.follower = minimizer.nestedScrollConnection
        onDispose { stage.follower = null }
    }
    LaunchedEffect(searchLit) {
        if (searchLit) {
            delay(SearchOpenDelayMillis)
            searching = true
            delay(SearchLitMillis)
            searchLit = false
        }
    }
    // On a tablet the tabs stand at the stage's start and Search at its end, as the reference's compact tab bar lays them
    // out on an iPad; on a phone the two fill the bar.
    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= TabletWidthDp
    val page = rememberQuvenGlassBackdrop()
    CompositionLocalProvider(LocalBarLook provides SystemBarLook) {
        BoxWithConstraints(Modifier.matchParentSize().bleed(StageInset)) {
            if (searchPage) SearchPage(Modifier.fillMaxSize().quvenGlassSource(page))
            QuvenGlassSearchMorph(
                searching = searching,
                onSearch = { if (tablet) searchPage = true else if (!searching) searchLit = true },
                onEndSearch = { searching = false },
                minimized = minimizer.minimized,
                onExpand = minimizer::expand,
                tabsWidth = phoneTabsWidth(3, tablet),
                height = PhoneBarHeight,
                gap = if (tablet) maxWidth - PhoneBarMargin * 2 - phoneTabsWidth(3, tablet) - PhoneBarHeight else tuning.barGap.dp,
                heldCentre = phoneGlyphCentre(held, 3, tablet),
                modifier = if (tablet) {
                    Modifier.align(Alignment.BottomStart).padding(start = PhoneBarMargin, bottom = PhoneBarBottom)
                } else {
                    Modifier.align(Alignment.BottomCenter).padding(bottom = StageInset)
                },
                style = tuning.style,
                backdrop = (if (searchPage) page else LocalQuvenGlassBackdrop.current).takeIf { tuning.liquid },
                reduceMotion = tuning.reduceMotion,
                tabs = {
                    SampleTabs(
                        if (searchPage) -1 else held,
                        onHold = {
                            held = it
                            searchPage = false
                        },
                        tuning = tuning,
                        count = 3,
                        entrySize = phoneEntrySize(3, tablet),
                    )
                },
                tabsFace = { SampleTabsFace(count = 3, held = held, selected = !searching, tuning = tuning) },
                heldGlyph = { SampleHeldGlyph(held, selected = !searching) },
                searchGlyph = { SampleSearchGlyph(selected = searchLit || searchPage) },
                field = {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(color = SampleColors.TextHigh, fontSize = 17.sp),
                        cursorBrush = SolidColor(SampleColors.Accent),
                        decorationBox = { inner ->
                            if (text.isEmpty()) Text("Search", color = SampleColors.TextMedium, fontSize = 17.sp)
                            inner()
                        },
                    )
                },
                accessory = accessory,
            )
        }
    }
}

/**
 * Draws the page a tablet's Search tab shows, as the reference's tab bar shows it on an iPad: a large title over a field,
 * the stage's content under them.
 *
 * @param modifier Modifier applied to the page.
 */
@Composable
private fun SearchPage(modifier: Modifier) {
    Box(modifier.background(SampleColors.Ground)) {
        SampleBackdropContent(Modifier.fillMaxSize(), top = SearchPageContentTop)
        Text(
            "Search",
            color = SampleColors.TextHigh,
            fontSize = 34.sp,
            lineHeight = 41.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = SearchPageTitleTop),
        )
        Row(
            Modifier
                .padding(start = SearchFieldInset, end = SearchFieldInset, top = SearchFieldTop)
                .fillMaxWidth()
                .height(SearchFieldHeight)
                .background(SearchFieldColour, CircleShape)
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = SampleColors.TextMedium, modifier = Modifier.size(20.dp))
            Text("Search", color = SampleColors.TextMedium, fontSize = 17.sp)
        }
    }
}

// The reference's Search page on an iPad: the title's line from 68 below the top, a field 44 tall from 117, 8 in from
// the sides, filled with the system's dark grey, and the content from 175.6.
private val SearchPageTitleTop = 68.dp
private val SearchFieldTop = 117.dp
private val SearchFieldHeight = 44.dp
private val SearchFieldInset = 8.dp
private val SearchPageContentTop = 175.6.dp
private val SearchFieldColour = Color(0xFF242325)

/** How long Search stands chosen before the bar opens into the field, as the reference's tab bar waits. */
private const val SearchOpenDelayMillis = 215L

/** How long Search stays lit as chosen once the field opens, before its glyph fades back to the field's colour. */
private const val SearchLitMillis = 50L

/**
 * Draws a phone's bar of four entries that minimizes while the stage scrolls down, and grows back when the stage returns
 * to its top or the minimized bar is pressed, as the reference's tab bar does.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.MinimizingTabBarExhibit(tuning: SampleTuning) {
    MinimizingBarStage(tuning)
}

/**
 * Draws the bar with Search beside its tabs and a player's strip above it, which comes down between the folded tabs and
 * Search while the bar is minimized, as the reference's bottom accessory does.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.BottomAccessoryExhibit(tuning: SampleTuning) {
    SearchBarStage(tuning) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(rememberVectorPainter(MusicNote), contentDescription = null, tint = SampleColors.TextHigh, modifier = Modifier.size(20.dp))
            Text("Now playing", color = SampleColors.TextHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(PlayFill, contentDescription = "Play", tint = SampleColors.TextHigh, modifier = Modifier.size(22.dp))
            Icon(rememberVectorPainter(FastForward), contentDescription = "Next", tint = SampleColors.TextHigh, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Draws a phone's bar of four entries that minimizes as the stage scrolls.
 *
 * @param tuning The live settings.
 */
@Composable
private fun BoxScope.MinimizingBarStage(tuning: SampleTuning) {
    var held by remember { mutableIntStateOf(0) }
    val minimizer = rememberQuvenGlassBarMinimizer()
    val stage = LocalStageScroll.current
    DisposableEffect(stage, minimizer) {
        stage.follower = minimizer.nestedScrollConnection
        onDispose { stage.follower = null }
    }
    // On a tablet the bar hugs its entries in the stage's whole width, as near its foot as the reference's compact tab
    // bar stands, centred at rest and minimized at its start.
    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= TabletWidthDp
    CompositionLocalProvider(LocalBarLook provides SystemBarLook) {
        Box(Modifier.matchParentSize().bleed(StageInset)) {
            QuvenGlassMinimizingBar(
                minimized = minimizer.minimized,
                onExpand = minimizer::expand,
                tabsWidth = phoneTabsWidth(4, tablet),
                height = PhoneBarHeight,
                heldCentre = phoneGlyphCentre(held, 4, tablet),
                modifier = if (tablet) {
                    Modifier.align(Alignment.BottomCenter).padding(start = PhoneBarMargin, end = PhoneBarMargin, bottom = PhoneBarBottom).fillMaxWidth()
                } else {
                    Modifier.align(Alignment.BottomCenter).padding(bottom = StageInset).wrapContentWidth(unbounded = true)
                },
                style = tuning.style,
                reduceMotion = tuning.reduceMotion,
                tabs = { SampleTabs(held, onHold = { held = it }, tuning = tuning, count = 4, entrySize = phoneEntrySize(4, tablet)) },
                tabsFace = { SampleTabsFace(count = 4, held = held, selected = true, tuning = tuning) },
                heldGlyph = { SampleHeldGlyph(held, selected = true) },
            )
        }
    }
}

/** Relays the scrolling of the stage's content to the exhibit that follows it, if any. */
internal class StageScroll : NestedScrollConnection {

    /** Gets or sets the connection the scrolling is handed to. */
    var follower: NestedScrollConnection? = null

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        follower?.onPostScroll(consumed, available, source) ?: Offset.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        follower?.onPostFling(consumed, available) ?: Velocity.Zero
}

/** Provides the stage's scrolling to its exhibit. */
internal val LocalStageScroll: ProvidableCompositionLocal<StageScroll> = staticCompositionLocalOf { StageScroll() }

/**
 * Draws a page of its own whose content scrolls under a bar at its top and a toolbar at its bottom, softly or with a hard
 * edge as the bar's control chooses, as the reference's page does.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.ScrollEdgeExhibit(tuning: SampleTuning) {
    var hard by remember { mutableStateOf(false) }
    PageStage(
        tuning,
        edge = if (hard) QuvenGlassScrollEdgeStyle.Hard else QuvenGlassScrollEdgeStyle.Soft,
        top = { EdgeStyleTrack(hard, onHard = { hard = it }, tuning = tuning) },
        bottom = {
            QuvenGlassIconButton(onClick = {}, icon = rememberVectorPainter(Shuffle), contentDescription = "Shuffle", reduceMotion = tuning.reduceMotion)
            QuvenGlassIconButton(onClick = {}, icon = rememberVectorPainter(PlayFill), contentDescription = "Play", reduceMotion = tuning.reduceMotion)
        },
    )
}

/**
 * Draws a page with a toolbar along its top, a back button, a title, a group of two buttons and one more, and a toolbar
 * along its bottom, as the reference's navigation bar and toolbar group their items.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.ToolbarExhibit(tuning: SampleTuning) {
    val share = rememberVectorPainter(ShareUp)
    val heart = rememberVectorPainter(Icons.Outlined.FavoriteBorder)
    val more = rememberVectorPainter(MoreHorizontal)
    val back = rememberVectorPainter(ChevronBack)
    PageStage(
        tuning,
        edge = QuvenGlassScrollEdgeStyle.Soft,
        top = {
            // The title stands in the middle of the bar, between the groups at its edges, as the reference's does.
            QuvenGlassContainer(Modifier.fillMaxWidth().padding(horizontal = PageBarMargin), style = tuning.style, spacing = QuvenGlassToolbarJoin) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QuvenGlassToolbarGroup(
                        listOf(QuvenGlassToolbarItem(back, "Back") {}),
                        modifier = Modifier.align(Alignment.CenterStart),
                        style = tuning.style,
                        reduceMotion = tuning.reduceMotion,
                    )
                    Text("Library", color = SampleColors.TextHigh, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                        QuvenGlassToolbarGroup(
                            listOf(QuvenGlassToolbarItem(share, "Share") {}, QuvenGlassToolbarItem(heart, "Favorite") {}),
                            style = tuning.style,
                            reduceMotion = tuning.reduceMotion,
                        )
                        Spacer(Modifier.width(QuvenGlassToolbarGap))
                        QuvenGlassToolbarGroup(listOf(QuvenGlassToolbarItem(more, "More") {}), style = tuning.style, reduceMotion = tuning.reduceMotion)
                    }
                }
            }
        },
        bottom = {
            QuvenGlassToolbarGroup(listOf(QuvenGlassToolbarItem(rememberVectorPainter(Shuffle), "Shuffle") {}), style = tuning.style, reduceMotion = tuning.reduceMotion)
            QuvenGlassToolbarGroup(listOf(QuvenGlassToolbarItem(rememberVectorPainter(PlayFill), "Play") {}), style = tuning.style, reduceMotion = tuning.reduceMotion)
        },
    )
}

/**
 * Draws a page of its own over the whole stage: content that scrolls under a bar at its top and a toolbar at its
 * bottom, meeting them with [edge], and the glass of both bars standing over that content.
 *
 * @param tuning The live settings.
 * @param edge How the content meets the bars.
 * @param top Draws the top bar's content, centred in it.
 * @param bottom Draws the bottom toolbar's items, spread along it.
 */
@Composable
private fun BoxScope.PageStage(
    tuning: SampleTuning,
    edge: QuvenGlassScrollEdgeStyle,
    top: @Composable () -> Unit,
    bottom: @Composable RowScope.() -> Unit,
) {
    val page = rememberQuvenGlassBackdrop()
    Box(Modifier.matchParentSize().bleed(StageInset)) {
        SampleBackdropContent(
            Modifier.fillMaxSize().quvenGlassSource(page).quvenGlassScrollEdge(top = PageBarHeight, bottom = PageBarHeight, style = edge),
            top = PageBarHeight,
        )
        CompositionLocalProvider(LocalQuvenGlassBackdrop provides page.takeIf { tuning.liquid }) {
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(PageBarHeight), contentAlignment = Alignment.Center) { top() }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(PageBarHeight).padding(horizontal = PageBarMargin),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = bottom,
            )
        }
    }
}

/**
 * Draws the control choosing a soft or a hard edge.
 *
 * @param hard Whether the hard edge is chosen.
 * @param onHard Invoked with whether the hard edge was pressed.
 * @param tuning The live settings.
 */
@Composable
private fun EdgeStyleTrack(hard: Boolean, onHard: (Boolean) -> Unit, tuning: SampleTuning) {
    val options = listOf(false, true)
    val appearance = rememberQuvenGlassAppearance()
    QuvenGlassSegmentedTrack(
        options = options,
        held = if (hard) 1 else 0,
        optionSize = DpSize(88.dp, 32.dp),
        style = tuning.style,
        gap = 2.dp,
        inset = 4.dp,
        reduceMotion = tuning.reduceMotion,
        onDraggedTo = onHard,
        appearance = appearance,
    ) { option, _ ->
        Box(
            Modifier.fillMaxSize().selectable(selected = option == hard, role = Role.RadioButton, interactionSource = null, indication = null) { onHard(option) },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (option) "Hard" else "Soft", color = appearance.contentColor(SampleColors.TextHigh, SampleColors.TextHighOnLight), fontSize = 15.sp)
        }
    }
}

/**
 * Lays the content out [by] beyond each edge of the room it is given, so it covers the stage's inset too.
 *
 * @param by How far the content reaches past each edge.
 * @return The modifier.
 */
private fun Modifier.bleed(by: Dp): Modifier = layout { measurable, constraints ->
    val reach = by.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + reach, maxWidth = constraints.maxWidth + reach, minHeight = constraints.maxHeight + reach, maxHeight = constraints.maxHeight + reach))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-reach / 2, -reach / 2) }
}

/** The height of the bars over the scroll edge page's edges, as the reference's navigation bar and toolbar stand. */
private val PageBarHeight = 64.dp

/** The room between a page's bars and the stage's start and end edges, as the reference's bars keep it on an iPad. */
private val PageBarMargin = 10.dp

/**
 * Draws a panel of interactive glass that lights under the finger, as the reference's interactive glass does.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.TouchLightExhibit(tuning: SampleTuning) {
    Box(
        Modifier
            .align(Alignment.Center)
            .size(TextPanelWidth, 260.dp)
            .quvenLiquidGlass(LocalQuvenGlassBackdrop.current, tuning.style.interactive(), RoundedCornerShape(32.dp), reduceMotion = tuning.reduceMotion),
        contentAlignment = Alignment.Center,
    ) {
        Text("Press and drag", color = SampleColors.TextHigh, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Draws a button that opens a popover pointing at it, as the reference opens Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.PopoverExhibit(tuning: SampleTuning) {
    val state = rememberQuvenGlassMorphState()
    var shown by remember { mutableStateOf(false) }
    QuvenGlassButton(
        onClick = { shown = true },
        label = "Show popover",
        size = QuvenGlassButtonSize.Large,
        modifier = Modifier.align(Alignment.Center).quvenGlassAnchor(state, stretches = false),
        reduceMotion = tuning.reduceMotion,
    )
    QuvenGlassPopover(state, expanded = shown, onDismissRequest = { shown = false }, modifier = Modifier.width(280.dp)) {
        // The reference's text keeps the system's 22 pt lines and stands centred in the panel, its frame wider than it.
        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Popover", color = SampleColors.TextHigh, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("A panel that points at its control.", color = SampleColors.TextMedium, fontSize = 17.sp, lineHeight = 22.sp)
            }
            CaptureMark(Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * Draws a button that raises a sheet, as the reference raises Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.SheetExhibit(tuning: SampleTuning) {
    var shown by remember { mutableStateOf(false) }
    QuvenGlassButton(
        onClick = { shown = true },
        label = "Show sheet",
        size = QuvenGlassButtonSize.Large,
        modifier = Modifier.align(Alignment.Center),
        reduceMotion = tuning.reduceMotion,
    )
    if (shown) {
        QuvenGlassSheet(onDismissRequest = { shown = false }, modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Text("Sheet", color = SampleColors.TextHigh, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Drag it up to full height to see it turn opaque.", color = SampleColors.TextMedium, fontSize = 17.sp)
                }
                CaptureMark(Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

/**
 * Draws a button that raises an alert asking to remove a collection, as the reference raises Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.AlertExhibit(tuning: SampleTuning) {
    var shown by remember { mutableStateOf(false) }
    QuvenGlassButton(
        onClick = { shown = true },
        label = "Show alert",
        size = QuvenGlassButtonSize.Large,
        modifier = Modifier.align(Alignment.Center),
        reduceMotion = tuning.reduceMotion,
    )
    if (shown) {
        QuvenGlassAlert(
            title = "Remove this collection?",
            message = "Its titles stay in the library.",
            actions = listOf(
                QuvenGlassAlertAction("Cancel", { shown = false }, QuvenGlassAlertRole.Cancel),
                QuvenGlassAlertAction("Remove", { shown = false }, QuvenGlassAlertRole.Destructive),
            ),
            onDismissRequest = { shown = false },
        )
    }
}

/**
 * Draws capsule buttons of plain and prominent glass in three sizes, as the reference lays out Apple's.
 *
 * @param tuning The live settings.
 */
@Composable
internal fun BoxScope.CapsuleButtonsExhibit(tuning: SampleTuning) {
    val play = rememberVectorPainter(PlayFill)
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
    val play = rememberVectorPainter(PlayFill)
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
                QuvenGlassMenuItem("Share", {}, icon = rememberVectorPainter(ShareUp))
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
private val ButtonFaces: List<Pair<Dp, ImageVector>> get() = listOf(
    46.dp to PlayFill,
    56.dp to Icons.Filled.Favorite,
    69.dp to ShareUp,
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
/** A note, as the system's `music.note` symbol draws it. */
private val MusicNote: ImageVector = materialIcon(name = "Filled.MusicNote") {
    materialPath {
        moveTo(12f, 3f)
        verticalLineToRelative(10.55f)
        curveToRelative(-0.59f, -0.34f, -1.27f, -0.55f, -2f, -0.55f)
        curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
        reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
        reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
        verticalLineTo(7f)
        horizontalLineToRelative(4f)
        verticalLineTo(3f)
        horizontalLineToRelative(-6f)
        close()
    }
}

/** Two arrows forward, as the system's `forward.fill` symbol draws them. */
private val FastForward: ImageVector = materialIcon(name = "Filled.FastForward") {
    materialPath {
        moveTo(4f, 18f)
        lineToRelative(8.5f, -6f)
        lineTo(4f, 6f)
        verticalLineToRelative(12f)
        close()
        moveTo(13f, 6f)
        verticalLineToRelative(12f)
        lineToRelative(8.5f, -6f)
        lineTo(13f, 6f)
        close()
    }
}

/** Three dots in a row, as the system's `ellipsis` symbol draws them. */
internal val MoreHorizontal: ImageVector = materialIcon(name = "Filled.MoreHoriz") {
    for (x in listOf(6f, 12f, 18f)) {
        materialPath {
            moveTo(x, 10f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
            reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
            reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
            close()
        }
    }
    this
}

/** A filled bookmark, as the system's `bookmark.fill` symbol draws it. */
internal val BookmarkFill: ImageVector = ImageVector.Builder("BookmarkFill", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 3f)
        horizontalLineTo(17f)
        quadTo(18.5f, 3f, 18.5f, 4.5f)
        verticalLineTo(21f)
        lineTo(12f, 16.6f)
        lineTo(5.5f, 21f)
        verticalLineTo(4.5f)
        quadTo(5.5f, 3f, 7f, 3f)
        close()
    }
}.build()

/** Three books standing side by side, the last leaning, as the system's `books.vertical.fill` symbol draws them. */
internal val BooksVertical: ImageVector = ImageVector.Builder("BooksVertical", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 4f); horizontalLineTo(7f); verticalLineTo(20f); horizontalLineTo(3f); close()
        moveTo(8.5f, 6f); horizontalLineTo(12.5f); verticalLineTo(20f); horizontalLineTo(8.5f); close()
        moveTo(14.2f, 5.6f); lineTo(17.9f, 4.6f); lineTo(21.6f, 18.4f); lineTo(17.9f, 19.4f); close()
    }
}.build()

/** A filled frame with a play mark over the outline of a second behind it, as the system's
 * `play.rectangle.on.rectangle.fill` symbol draws them. */
internal val PlayStack: ImageVector = ImageVector.Builder("PlayStack", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(7f, 4f); horizontalLineTo(19f); quadTo(21f, 4f, 21f, 6f); verticalLineTo(14f)
    }
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        moveTo(5f, 7.5f); horizontalLineTo(16f); quadTo(18f, 7.5f, 18f, 9.5f); verticalLineTo(18f); quadTo(18f, 20f, 16f, 20f)
        horizontalLineTo(5f); quadTo(3f, 20f, 3f, 18f); verticalLineTo(9.5f); quadTo(3f, 7.5f, 5f, 7.5f); close()
        moveTo(8.8f, 10.6f); lineTo(13.6f, 13.75f); lineTo(8.8f, 16.9f); close()
    }
}.build()

/** Two frames with arrows turning between them, as the system's `rectangle.2.swap` symbol draws them. */
internal val SwapFrames: ImageVector = ImageVector.Builder("SwapFrames", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(3.5f, 4f); horizontalLineTo(11f); verticalLineTo(10f); horizontalLineTo(3.5f); close()
        moveTo(13f, 14f); horizontalLineTo(20.5f); verticalLineTo(20f); horizontalLineTo(13f); close()
        moveTo(14.5f, 4.5f); quadTo(19.5f, 4.5f, 19.5f, 10f); moveTo(17.5f, 8.5f); lineTo(19.5f, 10.5f); lineTo(21.5f, 8.5f)
        moveTo(9.5f, 19.5f); quadTo(4.5f, 19.5f, 4.5f, 14f); moveTo(6.5f, 15.5f); lineTo(4.5f, 13.5f); lineTo(2.5f, 15.5f)
    }
}.build()

/** A crescent moon with small letters beside it, as the system's `moon.zzz` symbol draws it. */
internal val MoonZzz: ImageVector = ImageVector.Builder("MoonZzz", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(11.5f, 6.5f)
        arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 19.5f, y1 = 16f)
        arcTo(6.6f, 6.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 11.5f, y1 = 6.5f)
        close()
        moveTo(15f, 3f)
        horizontalLineTo(18f)
        lineTo(15f, 6.4f)
        horizontalLineTo(18f)
        moveTo(19.4f, 7.6f)
        horizontalLineTo(21.6f)
        lineTo(19.4f, 10.2f)
        horizontalLineTo(21.6f)
    }
}.build()

/** A triangle pointing forward, as the system's `play.fill` symbol draws it. */
internal val PlayFill: ImageVector = ImageVector.Builder("PlayFill", 24.dp, 24.dp, 24f, 24f, autoMirror = true).apply {
    path(fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineJoin = StrokeJoin.Round) {
        moveTo(6.6f, 4.4f)
        lineTo(19.2f, 12f)
        lineTo(6.6f, 19.6f)
        close()
    }
}.build()

/** A chevron pointing back, as the system's `chevron.left` symbol draws it. */
internal val ChevronBack: ImageVector = ImageVector.Builder("ChevronBack", 24.dp, 24.dp, 24f, 24f, autoMirror = true).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(15.5f, 3.2f)
        lineTo(7f, 12f)
        lineTo(15.5f, 20.8f)
    }
}.build()

/** A tray with an arrow rising out of it, as the system's `square.and.arrow.up` symbol draws it. */
internal val ShareUp: ImageVector = ImageVector.Builder("ShareUp", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 2.8f)
        verticalLineTo(14.5f)
        moveTo(8.2f, 6.6f)
        lineTo(12f, 2.8f)
        lineTo(15.8f, 6.6f)
        moveTo(8.6f, 9.8f)
        horizontalLineTo(7f)
        quadTo(5.2f, 9.8f, 5.2f, 11.6f)
        verticalLineTo(19.4f)
        quadTo(5.2f, 21.2f, 7f, 21.2f)
        horizontalLineTo(17f)
        quadTo(18.8f, 21.2f, 18.8f, 19.4f)
        verticalLineTo(11.6f)
        quadTo(18.8f, 9.8f, 17f, 9.8f)
        horizontalLineTo(15.4f)
    }
}.build()

/** Two crossing arrows, as the system's `shuffle` symbol draws them. */
private val Shuffle: ImageVector = materialIcon(name = "Filled.Shuffle") {
    materialPath {
        moveTo(10.59f, 9.17f)
        lineTo(5.41f, 4f)
        lineTo(4f, 5.41f)
        lineToRelative(5.17f, 5.17f)
        lineToRelative(1.42f, -1.41f)
        close()
        moveTo(14.5f, 4f)
        lineToRelative(2.04f, 2.04f)
        lineTo(4f, 18.59f)
        lineTo(5.41f, 20f)
        lineTo(17.96f, 7.46f)
        lineTo(20f, 9.5f)
        verticalLineTo(4f)
        horizontalLineToRelative(-5.5f)
        close()
        moveTo(14.83f, 13.41f)
        lineToRelative(-1.41f, 1.41f)
        lineToRelative(3.13f, 3.13f)
        lineTo(14.5f, 20f)
        horizontalLineTo(20f)
        verticalLineToRelative(-5.5f)
        lineToRelative(-2.04f, 2.04f)
        lineToRelative(-3.13f, -3.13f)
        close()
    }
}

/** A window with a column at its start, as the system's `sidebar.left` symbol draws it. */
private val SidebarSymbol: ImageVector = ImageVector.Builder("SidebarLeft", 24.dp, 24.dp, 24f, 24f).apply {
    addPath(
        PathParser().parsePathString("M5 4.5h14a2.5 2.5 0 0 1 2.5 2.5v10a2.5 2.5 0 0 1 -2.5 2.5h-14a2.5 2.5 0 0 1 -2.5 -2.5v-10a2.5 2.5 0 0 1 2.5 -2.5z M9.5 4.5v15").toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
    )
}.build()

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

/** Gets whether the gallery is being recorded, which draws the marks a capture follows. */
internal val LocalCaptureMarks: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/**
 * Draws, while the gallery is recorded, a mark of pure green a capture follows, as the reference marks the system's
 * elements.
 *
 * @param modifier Modifier applied to the mark.
 */
@Composable
private fun CaptureMark(modifier: Modifier) {
    if (LocalCaptureMarks.current) Box(modifier.padding(8.dp).size(8.dp).background(Color(0xFF00FF00)))
}

/** The width of the panels of the text menu and the touch light, as the reference sizes them. */
private val TextPanelWidth = 440.dp
