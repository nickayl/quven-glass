# Quven Glass

Apple's Liquid Glass for Jetpack Compose, made by [Quven Technologies S.R.L.](https://quven.tv) and free for anyone to
use. Every constant comes from measuring the system material on an iPad, not from tuning by eye, and the whole effect is
one AGSL program over `GraphicsLayer` and `RenderEffect`, with no third-party effect library behind it. How close does
it get? Over 111 scrolled scenes the inside of a bar differs from the iPad's by 1.2/255.

What you get:

- glass surfaces that fold, blur, tone and light the content behind them, swelling under a press;
- neighbouring surfaces that flow into one piece of glass as they come close;
- a segmented track whose pill lifts into a lens of clear glass under a press or a drag, as the tab bar's does;
- a control that opens into a panel as one piece of glass, and closes back into it;
- menus that grow out of their button and lay out every kind of row the way Apple does;
- thin and thick glass chosen by size, as iOS does, and thin glass that turns light over a bright backdrop.

## Requirements

| | |
|---|---|
| Liquid Glass | Android 13 (API 33) or later, for `RuntimeShader` |
| Fallback | Android 8 (API 26) to 12 draw a static frosted material with the same API |
| Toolchain | Kotlin 2.4, Jetpack Compose (BOM 2026.06), Java 17 |

## Install

The library isn't on a public Maven repository yet, so you build it once and point Gradle at the result. You can take
the simplest route, Maven Local:

```bash
git clone https://github.com/nickayl/quven-glass.git
cd quven-glass
./gradlew :glass:publishToMavenLocal
```

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

// app/build.gradle.kts
dependencies {
    implementation("tv.quven.glass:glass:2.29.3")
}
```

If your team should build without that step, publish into a folder you commit instead:
`./gradlew :glass:publishReleasePublicationToQuvenRepository -PquvenGlass.repository=/path/to/app/third_party/maven`,
then add `maven { url = uri("third_party/maven") }` to the repositories.

## Quick start

Glass needs something to look through. You mark that content as the backdrop's source, then draw glass after it,
outside it:

```kotlin
@Composable
fun Screen() {
    val backdrop = rememberQuvenGlassBackdrop()
    Box(Modifier.fillMaxSize()) {
        // Everything the glass shows records into the backdrop.
        Catalogue(Modifier.fillMaxSize().quvenGlassSource(backdrop))

        CompositionLocalProvider(LocalQuvenGlassBackdrop provides backdrop) {
            val presses = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .quvenLiquidGlass(
                        backdrop = LocalQuvenGlassBackdrop.current,
                        shape = CircleShape,
                        interactionSource = presses,
                    )
                    .clickable(interactionSource = presses, indication = null, onClick = { }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }
    }
}
```

A surface placed inside its own source can't see past itself, so it draws the static material. Keep the source and the
glass siblings, with the glass drawn second.

## Bars and segmented controls

`QuvenGlassContainer` draws every surface inside it in one pass, so two of them melt together as they swell or slide
close. `QuvenGlassSegmentedTrack` lays out options of one size with a sliding pill; a press moves the pill at once, and
with `onDraggedTo` a drag carries it under the finger and hands you the option it's let go over. Its `feel` decides how it
answers: `QuvenGlassTrackFeel.TabBar`, the default, grows the track under the finger and lifts the pill into a lens as
Apple's tab bar does, while `QuvenGlassTrackFeel.Selector` slides the pill but never lifts it, so the glass can stay
still for a selector standing in a page.

```kotlin
QuvenGlassContainer(spacing = 8.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuvenGlassSegmentedTrack(
            options = tabs,
            held = tabs.indexOf(current),
            optionSize = DpSize(96.dp, 62.dp),
            inset = 4.dp,
            onDraggedTo = { tab -> current = tab },
        ) { tab, held ->
            TabFace(tab, held, onClick = { current = tab })
        }
        QuvenGlassSegmentedTrack(options = listOf(Search), held = -1, optionSize = DpSize(62.dp, 62.dp), inset = 4.dp) { _, _ ->
            SearchFace()
        }
    }
}
```

Each option draws its own face and answers its own press; the track only reads where the finger is.

But inside a page Apple's segmented control isn't glass. It's a flat grey track with a lighter pill, and that's what
`QuvenGlassSegmentedControl` draws. It answers presses and drags on its own. So the options shouldn't carry a click
handler of their own.

```kotlin
QuvenGlassSegmentedControl(options = densities, selected = densities.indexOf(density), onSelect = { density = it }) { option, _ ->
    DensityGlyph(option)
}
```

## Searching from the tab bar

`QuvenGlassSearchMorph` turns a phone's tab bar into a search field and back, the way iOS does when its Search tab is
chosen. Press the Search circle and it swells and lights, then stretches along the bar into a field, while the capsule
of tabs folds into a circle at the start that carries the held tab's glyph to its middle; both sink a little into the
bar as they go. Press that circle and the tabs will unfold. They come close enough to the shrinking field to run into it for
a moment, and their faces grow back with their capsule. What does it need from you? The tabs you draw at rest, their
faces without the held glyph, that glyph on its own, and the field. It's as wide as the resting bar and doesn't centre
itself, so you should place it.

Does a bar with Search beside its tabs minimize too? Pass `minimized` from a `rememberQuvenGlassBarMinimizer()` and the
tabs fold into the same circle at the start while the Search circle sinks where it stands, as Music does on an iPhone.
A press on the folded tabs calls `onExpand`. Opening the search from there starts from the folded circle, and ending it
doesn't leave the bar minimized.

```kotlin
QuvenGlassSearchMorph(
    searching = searching,
    onSearch = { searching = true },
    onEndSearch = { searching = false },
    tabsWidth = 288.dp,
    height = 62.dp,
    gap = 8.dp,
    heldCentre = DpOffset(50.dp, 23.dp),
    modifier = Modifier.align(Alignment.BottomCenter),
    tabs = { Tabs(held, onHold = { held = it }) },
    tabsFace = { TabFaces(held) },
    heldGlyph = { Icon(Icons.Filled.Home, contentDescription = "Home") },
    searchGlyph = { Icon(Icons.Filled.Search, contentDescription = "Search") },
    field = { SearchField(query, onQueryChange = { query = it }) },
)
```

## Minimizing the tab bar

`QuvenGlassMinimizingBar` lets a phone's tab bar get out of the way while you read, the way iOS minimizes its tab bar on
scrolling down, and it hands the screen back to the content without hiding which tab you're on. The capsule of tabs
folds into a circle at its start that keeps the held tab's glyph, and the other faces shrink and fade as it goes.
Scrolling up a little won't bring it back. The content has to scroll back by as far as it scrolled down, or be drawn against its top, or you press the circle. Who decides when
it folds? `rememberQuvenGlassBarMinimizer()` does, once you hand its `nestedScrollConnection` to the scrolling content,
and you can call `expand()` yourself when a screen should open with the whole bar, or `reset()` when it starts again at its top, which also forgets how far the content has scrolled. Given only the bar's width, it stays put. Give it
a whole tablet's width instead and it rests centred, then folds into a circle at the start, as an iPad's tab bar does.

Pass an `accessory`, such as a player's controls or a download that's still running, and it gets a capsule of glass of
its own above the bar, as wide as the bar and as tall as the folded circle. When the bar minimizes, the accessory
narrows first and then drops into the bar's line beside the circle. Growing back, it rises before it widens, as a bottom
accessory does on iOS. Will it merge with the bar where the two pass over each other? No, each keeps its own glass.
`QuvenGlassSearchMorph` takes an `accessory` too, the way Music lays one out over its tabs and Search; there it comes
down between the folded tabs and the sunken Search circle.

```kotlin
val minimizer = rememberQuvenGlassBarMinimizer()
LazyColumn(Modifier.nestedScroll(minimizer.nestedScrollConnection)) { items(titles) { Row(it) } }
QuvenGlassMinimizingBar(
    minimized = minimizer.minimized,
    onExpand = minimizer::expand,
    tabsWidth = 354.dp,
    height = 62.dp,
    heldCentre = DpOffset(46.dp, 23.dp),
    modifier = Modifier.align(Alignment.BottomCenter),
    tabs = { Tabs(held, onHold = { held = it }) },
    tabsFace = { TabFaces(held) },
    heldGlyph = { Icon(Icons.Filled.Home, contentDescription = "Home") },
    accessory = { NowPlaying(Modifier.fillMaxSize().padding(horizontal = 16.dp)) },
)
```

## Scroll edges

`Modifier.quvenGlassScrollEdge` gives content that scrolls under bars the scroll edge effect iOS draws there, and all it
needs to know is how tall the bar over each edge is. The soft style, the default, darkens and blurs the content near the
edge and a little past the bar, so the bar seems to float over whatever passes beneath it, as the navigation bars of
Apple's own apps do. The hard style hides it under an opaque band as tall as the bar. Its edge is sharp. Put it on the
scrolling content, which you should pad by the bars' heights as iOS does, so the content isn't under a bar until you
scroll. Want the bars' glass to see what's drawn under them? Put `quvenGlassSource` first.

```kotlin
LazyColumn(
    Modifier.fillMaxSize().quvenGlassSource(page).quvenGlassScrollEdge(top = 56.dp, bottom = 56.dp),
    contentPadding = PaddingValues(vertical = 56.dp),
) { items(titles) { Row(it) } }
```

## Touch light

Interactive glass lights where you touch it. `QuvenGlassStyle.interactive()` gives a material a faint white light that
gathers under the finger within a few hundredths of a second, follows it as it moves and dies away over about 0.4 s once
it lifts, as iOS's interactive glass does. The light spreads wide, so a small button will light all over while a panel
shows it pooling around the finger. Glass only watches the finger, and whatever stands under it still gets the press.
Does every surface light up? It doesn't.

```kotlin
Box(Modifier.size(360.dp, 260.dp).quvenLiquidGlass(backdrop, QuvenGlassStyle.Standard.interactive(), RoundedCornerShape(32.dp)))
```

## Toolbars

`QuvenGlassToolbarGroup` puts toolbar buttons on glass the way iOS groups its bar items, with one button standing alone
in a circle and two or more sharing a single capsule between them. Press any of them and the whole capsule swells, as a
glass button does, while it lights toward white around your finger even over a black page. Lay groups in a row 13 dp
apart (`QuvenGlassToolbarGap`) inside a `QuvenGlassContainer` spaced at `QuvenGlassToolbarJoin`, and a swollen group
will run into its neighbour for as long as it's held. Why not join them at rest? Apple doesn't.

```kotlin
QuvenGlassContainer(Modifier.fillMaxWidth().padding(horizontal = 16.dp), spacing = QuvenGlassToolbarJoin) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        QuvenGlassToolbarGroup(listOf(QuvenGlassToolbarItem(back, "Back", onBack)))
        Text("Library", Modifier.padding(start = 13.dp).weight(1f))
        QuvenGlassToolbarGroup(listOf(QuvenGlassToolbarItem(share, "Share", onShare), QuvenGlassToolbarItem(heart, "Favorite", onFavorite)))
        Spacer(Modifier.width(QuvenGlassToolbarGap))
        QuvenGlassToolbarGroup(listOf(QuvenGlassToolbarItem(more, "More", onMore)))
    }
}
```

## Text menu

`ProvideQuvenGlassTextToolbar` swaps the system's menu of cut, copy and paste for one on glass, the capsule iOS raises
over selected text. Put it around a screen's content, near the root and over the same backdrop as the rest of its glass,
and every text field or selectable text inside it will use it. The actions stand in a row parted by hairlines, above the
selection where there is room and below it where there is not. A menu too wide for the window pages through its actions
with an arrow at either end, the way iOS does when a selection allows more actions than one row can hold. Do the
platform's own entries go? They stay.

```kotlin
ProvideQuvenGlassTextToolbar(backdrop = screen) {
    Content()
}
```

## Sidebar

`QuvenGlassSplitView` lays a sidebar of glass beside its detail, the way a split view does on an iPad. The sidebar is
320 dp wide by default and floats 10 dp inside the parent's edges, and the detail starts at its end edge. Hide it, and
the sidebar slides out past the start edge while the detail opens up on the same spring, which doesn't overshoot. And
when you show it again it'll slide back in. Although the sidebar floats, it never covers the detail. What goes inside? Whatever you put there, such as a title, the entries and the button that hides it again, which
you can draw however you like.

```kotlin
QuvenGlassSplitView(
    sidebarShown = sidebarShown,
    sidebar = { SidebarEntries(onHide = { sidebarShown = false }) },
) {
    Library(Modifier.fillMaxSize())
}
```

Still, sometimes the sidebar should float over content that stays put, and `QuvenGlassSidebar` is that sidebar alone.

## Text on light glass

Over a bright backdrop thin glass turns light after about two seconds, as the system's does. So hand the surface a
`QuvenGlassAppearance` and colour its content from it, or white text will sit on near-white glass:

```kotlin
val appearance = rememberQuvenGlassAppearance()
Box(Modifier.quvenLiquidGlass(backdrop, shape = CircleShape, appearance = appearance)) {
    Text("A", color = appearance.contentColor(onDark = Color.White, onLight = Color.Black))
}
```

`QuvenGlassSegmentedTrack` takes the same `appearance` parameter. Thick glass, from a shorter side of 66 dp, never turns.

## Buttons and switches

`QuvenGlassButton` draws a glass button of any shape around content you give it, and hands that content the ink that
reads on it. With a label, or a label after a glyph, it is a capsule sized as Apple sizes its buttons, and
`QuvenGlassIconButton` holds a glyph alone. Plain glass lights what's behind it under the finger; give it a `tint` and
it becomes a prominent button, tinted almost opaque, with white ink.

```kotlin
QuvenGlassButton(onClick = ::play, label = "Play", icon = painterResource(R.drawable.play))
QuvenGlassButton(onClick = ::buy, label = "Buy", tint = Color(0xFF0091FF))
QuvenGlassButton(onClick = ::close, modifier = Modifier.size(44.dp)) { ink -> Icon(Icons.Filled.Close, null, tint = ink) }
```

`QuvenGlassStyle.Clear` is the clear variant for glass over bright media. It has no tone. It's a little lighter than what
it covers and shows it sharper and a quarter larger, as a lens would, while `forButtons()` and `forMenus()` turn any
material, this one included, into the one a button or a menu draws.

`QuvenGlassSwitch` is the system's switch. Its white thumb lifts into a lens of clear glass while it's held, dragged or
carried across, shows the track a fifth smaller through it, and settles back into a thumb once it rests. A tap turns it
over, and a drag turns it to the side it's let go nearer. The lens reads only the switch's own track, so it works
anywhere, inside a page that is itself a backdrop too.

```kotlin
QuvenGlassSwitch(checked = downloads, onCheckedChange = { downloads = it })
QuvenGlassSlider(value = volume, onValueChange = { volume = it })
QuvenGlassSlider(value = rating, onValueChange = { rating = it }, valueRange = 0f..5f, steps = 4)
```

`QuvenGlassSlider` lifts the same lens out of its thumb while a finger holds it. As on iOS, only a drag that starts on
the thumb moves it, so tapping the track does nothing. The arrow keys and a TV remote step it along, a tenth of the
range at a time unless it has steps.

## Menus that grow from their control

`QuvenGlassMorph` grows a panel out of the bounds of the control that opened it and folds it back on close. Hide the
control while `isShown` is true, since the morph draws its face in the meantime:

```kotlin
val morph = rememberQuvenGlassMorphState()
var anchor by remember { mutableStateOf(Rect.Zero) }
var open by remember { mutableStateOf(false) }

GearButton(
    modifier = Modifier
        .onGloballyPositioned { anchor = it.boundsInRoot() }
        .graphicsLayer { alpha = if (morph.isShown) 0f else 1f },
    onClick = { open = true },
)
QuvenGlassMorph(
    state = morph,
    expanded = open,
    anchor = anchor,
    width = 320.dp,
    placement = QuvenGlassMorphPlacement.hangingFromTopLeft(edge = 16.dp),
    modifier = Modifier.fillMaxSize(),
    face = { GearFace() },
) {
    SettingsPanel(onClose = { open = false })
}
```

`hangingFromTopRight` and `above` place the panel for a button on the other side or an entry of a bottom bar, and
`aboveOrBelow` beside a larger control, on the side with more room.

## Menus

A menu needs a host: one `QuvenGlassMenuHost`, placed last at the root of the screen so it can draw over everything,
with its state provided below it. Then any control can open a menu with `QuvenGlassMenuBox`:

```kotlin
val menus = rememberQuvenGlassMenuHostState()
CompositionLocalProvider(LocalQuvenGlassBackdrop provides backdrop, LocalQuvenGlassMenuHost provides menus) {
    Box(Modifier.fillMaxSize()) {
        Screen()
        QuvenGlassMenuHost(
            state = menus,
            style = QuvenGlassStyle.Standard.forMenus(),
            metrics = QuvenGlassMenuMetrics.Phone,
        )
    }
}

// Anywhere inside Screen():
QuvenGlassMenuBox(
    menu = {
        QuvenGlassMenuTitle("Sort by")
        QuvenGlassMenuChoices(orders, selected = order, label = { it.title }, onSelect = { order = it })
        QuvenGlassMenuDivider()
        QuvenGlassMenuItem("Share", onClick = ::share, icon = painterResource(R.drawable.share))
        QuvenGlassMenuItem("Remove", onClick = ::remove, destructive = true)
    },
    face = { MoreFace() },
) {
    MoreButton(
        modifier = Modifier.menuAnchor().graphicsLayer { alpha = if (isMenuShown) 0f else 1f },
        onClick = { openMenu() },
    )
}
```

The menu drops out of its button the way the system's does: the button's glass stays as a lit cap while a drop of
clear glass falls from it, lengthens before it spreads and frosts over as it settles, and the button stretches for a
moment when the glass lands back in it. The menu closes itself once a row is chosen, on Back and on a press anywhere
else. It opens over its button and hangs
down from it when the room below is enough; otherwise it rises, and an untitled menu then lists its rows from the foot
up, so the first one stays nearest the finger. A menu holding a choice makes room for the check before every row, as
iOS does. `QuvenGlassMenuChoice` with `role = Role.Checkbox` is a toggle, `enabled = false` greys a row out, and
`QuvenGlassDropdown` takes the place of the box when you'd rather hold the open state yourself. Opened from a keyboard
or a TV remote, the menu moves the focus to its first row, keeps it there until it closes and then hands it back to its
button. A menu too tall for the screen stays 16 dp inside its edges and scrolls, and a swipe over it only scrolls it.

A system menu doesn't dim what's behind it, and neither does the host. Its rim is brighter than a bar's, which is what
`rimGlow` draws. A `QuvenGlassButton` opening it lights up under the finger, and the menu's glass carries that light for
the first part of its growth. A finger can also slide along an open menu: the row under it
lights at once with a light tick, and the row it lifts over is the one chosen.

## Submenus

`QuvenGlassSubmenu` adds an entry that opens a second menu, the way a system menu nests one. Its row carries a chevron,
and when it's pressed the second menu grows out of the row itself, just as wide as the first one, headed by the entry's
own name in bold with the chevron now turned down, while the first menu fades behind it. Want to go back? Press the head
and the second menu folds into its row. A choice in it will close both, and Back closes the second one alone. It won't
open anywhere but in a menu a `QuvenGlassMenuHost` draws.

```kotlin
QuvenGlassSubmenu("Share", icon = sharePainter) {
    QuvenGlassMenuItem("Message", ::sendMessage, icon = messagePainter)
    QuvenGlassMenuItem("Mail", ::sendMail, icon = mailPainter)
}
```

## Context menus

`QuvenGlassContextMenuBox` turns a card into one with a context menu, as a long press on iOS does. The card grows a
little while it is held, then lifts out of the screen as everything behind it darkens, and the menu's glass flows out
of its edge to stand below it, or above it where there is no room below. How far does the card lift? About a tenth, on a
phone and on a tablet alike, and you can change that share in `QuvenGlassMenuMetrics`. Pass a `preview` to lift something
else in the card's place, and it grows from the card's size to its own just as `.contextMenu(menuItems:preview:)` does
on iOS, while a press still runs the card's own action. It opens in the same `QuvenGlassMenuHost` as every other menu.

```kotlin
QuvenGlassContextMenuBox(
    menu = {
        QuvenGlassMenuItem("Play", onClick = ::play, icon = painterResource(R.drawable.play))
        QuvenGlassMenuItem("Remove", onClick = ::remove, destructive = true)
    },
    onClick = ::open,
) {
    Poster(film)
}
```

## Popovers

`QuvenGlassPopover` opens a panel of anything you like out of a control, as an iPad's popovers do. A drop of glass forms
at the control's edge and grows into the panel above or below it, centred on it, then stays joined to the panel as a
small point, and closing it folds the panel back into that drop. The control does not hide, so mark it with
`quvenGlassAnchor(state, stretches = false)`. The popover opens in the same `QuvenGlassMenuHost` as the menus, and a
press elsewhere or Back calls `onDismissRequest`. Why a drop? It can bend and light what lies under it, since it is the
panel's own glass.

```kotlin
val state = rememberQuvenGlassMorphState()
QuvenGlassButton(onClick = { showing = true }, label = "Details", modifier = Modifier.quvenGlassAnchor(state, stretches = false))
QuvenGlassPopover(state, expanded = showing, onDismissRequest = { showing = false }, modifier = Modifier.width(280.dp)) {
    Details(Modifier.padding(20.dp))
}
```

## Sheets

`QuvenGlassSheet` raises a sheet from the bottom edge, the way iOS does on a phone. At about half the window it floats
on glass inside the window's edges, over a dimmed screen, and as you draw it up it reaches the edges and turns opaque,
so by full height nothing behind it shows through. It follows a drag from its grabber or from content scrolled to the
top. Want it closed? Drag or fling it below its lowest detent, press the dimmed screen or go Back, and it calls
`onDismissRequest`. It won't close any other way. Pass `detents` to open it at full height or to keep it there; on a
wide window it stays 574 dp wide, in the middle, and its host, `QuvenGlassSheetHost`, should go last in the window's
root, as the alerts' does.

```kotlin
if (filtering) {
    QuvenGlassSheet(onDismissRequest = { filtering = false }) {
        Filters(Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()))
    }
}
```

## Alerts

`QuvenGlassAlert` raises an alert on glass in the middle of the screen. The screen dims behind it while it settles from
a tenth larger, and once you stop drawing it, it fades at its own size. Title and message stand at its start, over the
actions: two sit side by side, more stack one above another in the order you give them, and a destructive one is named
in red, while Back calls `onDismissRequest` and a press outside does nothing, as on iOS. Words too long for the window
scroll above actions that stay in view. Should Back leave it standing? Pass none. You'll want a `QuvenGlassAlertHost`
last in the window's root, over a backdrop of the whole window, with its state handed down through
`LocalQuvenGlassAlertHost`.

```kotlin
if (removing) {
    QuvenGlassAlert(
        title = "Remove this collection?",
        message = "Its titles stay in the library.",
        actions = listOf(
            QuvenGlassAlertAction("Cancel", { removing = false }, QuvenGlassAlertRole.Cancel),
            QuvenGlassAlertAction("Remove", ::remove, QuvenGlassAlertRole.Destructive),
        ),
        onDismissRequest = { removing = false },
    )
}
```

## Styling

`QuvenGlassStyle.Standard` is the measured material, but you can copy it and change what you need:

| Parameter | What it changes |
|---|---|
| `thinTone`, `thickTone`, `largeTone`, `lightTone` | The colour each kind of glass leans towards, how far, how much lighter it turns over brighter content, and its saturation |
| `thinSize`, `thickSize`, `largeFromSize`, `largeSize` | The shorter sides where glass stops being thin and becomes thick, then large |
| `blur`, `thickBlur`, `thickBlurSize` | The blur under small glass, and how it deepens as the glass grows |
| `refraction`, `edgeWidth` | The fold along the rim |
| `specular`, `lightAngle` | The light caught by the rim and where it comes from |
| `platter`, `lightPlatter` | The pill under a held option, on dark and on light glass |
| `adaptation` | When thin glass turns light and how quickly |
| `pressGrowth`, `slideDamping`, `slideStiffness` | How a press swells the glass and how the pill travels |
| `pressGlow` | How brightly a pressed button lights what's behind it; bars keep it at zero |
| `rimGlow` | How brightly the rim shows what lies just outside it; only menus use it |
| `zoom` | How much smaller the glass shows what's behind its body; only a switch's lens uses it |
| `ground`, `trackTop`, `rimTop` and siblings | The static material drawn without Liquid Glass |

You should pass `reduceMotion` from the system setting to every surface. Springs then become short fades.

## Cost

Although each surface redraws a blurred copy of the backdrop on every frame, it does so at half resolution in one
shader pass. And thin and large glass sample the backdrop under and around them twice a second into 16×16 pixels, on
a renderer of their own, to follow how bright it is; that work stays off the main thread, and it pauses while the window
is out of view.

## Measured model

Reference: iPad A16, iOS 26.5, Liquid Glass set to Glass; `reference/ios` frames through ReplayKit, the screen taken over
the cable, and the blur probe's still screenshots.

| Property | Value |
|---|---|
| Thickness | Thin up to a shorter side of 63 pt, thick from 66 pt, turning large from 100 pt to 124 pt; interactive or still alike |
| Thick tone | Lean 0.77 towards `0x27`, saturation 2.49, sRGB; the same over dark and bright content |
| Thin tone | Lean 0.7 towards `0x1A`, which picks up 0.48 times as much as the luminance of the mean colour of what lies under the surface and 12 pt around it, taken in linear light, passes 0.25, a quarter in that colour and the rest grey, saturation 2; over black it reads 19 levels until the content under it is bright; read over a palette at four brightnesses |
| Large tone | Lean 0.86, less 0.05 per unit of luminance, towards `0x17`, which picks up 0.144 times the mean colour under it and 12 pt around in linear light, saturation 2.6; a panel or a sidebar veils more than a bar |
| Menu tone | Lean 0.88, less 0.05 per unit of luminance, towards `0x0C`, which picks up 0.185 times the mean colour under it and 12 pt around in linear light as the screen shows it, dimmed or not, saturation 2.6; a menu or a sheet veils more than a panel; a context menu leans towards `0x19` |
| Platter | White at 11%; black at 7% on light glass; a tab bar's white at 15% |
| Fold | 1.43 × corner radius over a band of 0.46 × radius, the radius counted up to 32 pt, at least 22 over 12 pt, power 2.5 |
| Blur | Gaussian, σ 2.5 pt up to a shorter side of 63 pt, building up in proportion to σ 5.3 pt at 136 pt and levelling off there |
| Rim | Lit from above, falling off with the square of the facing, 0.15 underneath; the tint lies over its light; still glass shows what lies just outside it there, as it is |
| Tap lens | On an iPad: a press grows the whole bar 17.5 pt along its length on a spring of damping 0.7 and stiffness 625, lights it 35 levels over black under the finger, falling as a Gaussian of about 104 pt, and lifts the platter into a lens of clear glass over the bar, 31.5 pt wider and 19 pt taller than the entry, which keeps its width as it crosses to the pressed entry on a spring of damping 0.96 and stiffness 324, shows the entries under it a quarter larger in the held colour and the bar's own glass through it, its rim lit and faintly parting its colours; a tap lifts it three fifths as far, so it is narrower and magnifies less, and it settles once the pill has nine tenths of its way behind it; the platter is gone within 40 ms and comes back over about 120 ms once the lens has been gone 50 ms; released, the bar shrinks on a spring of damping 0.71 and stiffness 400, passing a little below its size; a finger held on an entry takes the held colour from the entry held until then |
| Drag | The lens follows the finger, only the entry under it in the held colour, and settles on the nearest option |
| Search | On an iPhone: a press grows the Search circle 9 pt and turns it 41% of the way to white whatever lies under it, within about 70 ms; the tabs fold into a circle at the start and the circle stretches into the field on a spring with damping 0.82 and stiffness 380, both sinking 7.25 pt into the bar, the room between them growing from 8 to 14.5 pt; closing, they come within 4 pt and join while the tabs unfold, their faces scaled with their capsule |
| Minimizing tab bar | On an iPhone: once the content has scrolled down a little, the capsule folds into a 47.5 pt circle at its start on the search's spring, the faces shrinking with it; on an iPad the resting bar stands centred and the circle travels to the window's start as it folds; it grows back, passing its size by about 1%, when the content reaches its top or the circle is pressed, and scrolling up anywhere else leaves it minimized |
| Bottom accessory | On an iPhone: a capsule as tall as the minimized circle (47.5 pt), 9.5 pt above the resting bar and as wide; minimizing, it narrows to start 8 pt past the circle and end 7.25 pt in, then falls into the bar's line; growing back, it rises before it widens; it keeps its own glass over the bar's |
| Scroll edge | Under a 56 pt bar on an iPhone: soft, the content blurs about 3.5 pt at the edge, gone by 80% of the reach, and darkens 0.29 toward black over 1.78 times the bar's height, with a lip of about 0.66 along the first 10% that ends by 17%; hard, an opaque band of `#212121` as tall as the bar, 0.45 darker at its very top, ends on a sharp line |
| Touch light | Interactive glass on an iPad: its own colours 1.45 times as bright and 1.2 times as saturated under the finger, falling as a Gaussian with σ about 90 pt, in within about 80 ms, following the finger, gone about 450 ms after it lifts; glass holding no controls lets the press through, so a drag across it scrolls what it stands over |
| Toolbar | On an iPhone: groups 44 pt tall, a circle for one button and 59 pt wider for each further one, 13 pt apart, their symbols about 22 pt across, which a 26 dp stock icon matches; a press swells the whole group 16 pt along its longer side and lights it toward white, 11% all over and 0.5 more at the finger, falling as a Gaussian of about 45 pt, so a swollen group joins its neighbour |
| Light glass | Thin glass only: lean 0.82 towards `0xF5`, saturation 3.27; turns light above a mean channel of 0.74 and dark below 0.64, smoothed over 2 s |
| Press on a glass button | The button grows 16 pt along its longer side on a spring that passes its size by about 12%; once the finger lifts it springs back on a looser one, damping 0.375 and stiffness 250, passing below its size by about a quarter before it settles, and a tap turns it back at once; plain glass shows what lies under it untoned, softened over about 8 pt, its saturation raised 1.3 times and lit 1.5 times, with up to 0.4 added over black that falls steeply as the backdrop brightens, within about 70 ms, and dies away over about 450 ms after release; a tap lights it all the way before it fades; a prominent button's tint lightens 1.15 times |
| Menu | 223 × 38 pt rows on iPad, 247 × 42 pt on iPhone; 25 pt corners; glyph centred at 32.5 or 37 pt, name from 55 or 62 pt |
| Menu choices | Check centred at 21 or 24 pt, name from 32 or 36 pt; a menu holding one widens by 12 or 14 pt and moves its glyphs as far |
| Menu placement | Over its button, hanging when the room below suffices, otherwise rising with untitled rows reversed |
| Menu glass | The tone of its size, the menu tone once its shorter side passes 124 pt, blur σ 4.7 pt at every size, no dimming behind it; a rim about 1 pt wide shows the backdrop just outside it 1.5–1.9 times brighter |
| Menu opening | The button's glass stays as a cap, lit by the press for about 40 ms, while a drop of clear glass falls from its middle and joins it: its length on a spring with damping 0.68 and stiffness 380, its width on a slower one with damping 0.72 and stiffness 300; the near edge leaves the button last, the frost and the corners settle last, the rows come into focus from 40% of the width |
| Menu closing | Back into its button in about 150–165 ms on an almost even ease, with no bounce; the button then stretches about 7% the way the glass came back and settles within about 250 ms |
| Menu press | The whole menu washes about 15% whiter within 50 ms; the held row's capsule, 13 pt in from the sides, follows after 150 ms, fills in over 180 ms and goes the moment the finger lifts |
| Submenu | On an iPhone: the second menu grows out of its entry's row, as wide as the first, its head (the entry in bold, the chevron turned down, then a hairline) centred on the row; the first menu's rows fade to about 0.4 behind it; it folds back into the row |
| Edit menu | On an iPhone: a capsule 41 pt tall of the actions in 17 pt, each 17 pt in from its hairline, the hairlines 20 pt tall; 14 pt above the selection, or below it without room; paged with arrows where wider than the window. On an iPad: 43 pt tall, the actions in 15 pt, hairlines 17 pt tall, at most about 500 pt wide; the actions that do not fit stand behind a chevron in a circle of 36 pt, white at 6%, which expands the capsule into a menu of every action, the clipboard's side by side at its head, each glyph over its name in 12 pt |
| Sidebar | On an iPad: glass 320 pt wide, 10 pt inside its parent's edges, corners of 24, the detail beside it from its end edge, blurred around it by σ about 145 pt so the detail glows in from its end edge, leaning 0.79 towards `0x15`, its rim lit by 0.2 white; it slides in from the start edge on a critically damped spring of stiffness about 480, there in about 250 ms |
| Segmented control | In a page's content on an iPad: a flat track 32 pt tall, (118, 118, 128) at 24%, not glass; the pill 2 pt inside it, white at about 27%; pressed or sent to another option it lifts into a lens of clear glass 18 pt wider and 12 taller, which bends the options under it as it crosses in about 240 ms on a spring with damping 0.85 and stiffness 230, and settles back once it rests |
| Popover | A panel beyond its control on the side with more room, centred on it, 14 pt away; an 18 pt drop of glass 13 pt beyond the control's edge grows into it and stays joined to it over 8 pt as its point; the screen is not dimmed |
| Sheet | Half the window at rest, floating 9 pt inside the window's edges with corners of 39 on glass blurred by σ 5.2 pt over the screen dimmed by 0.48; drawn to full height (the top inset) it reaches the edges and turns opaque (`0x1B1A1D`) between 45% and 85% of the way, the screen above it darkening to 0.85; a drag past full height moves it a third as far. On a tablet it floats as a card 580 pt wide in the middle, its foot 92.5 pt above the window's, 357.5 pt tall at rest and from 84.5 pt below the top at full height, and slides down whole as it closes |
| Alert | 319 pt wide with corners of 33, centred, its title 23 pt below its top and its message 8 pt below the title, on glass letting more of the dimmed screen's colours through than a menu; the screen dims by 0.48 within about 250 ms while the alert settles from 1.1 times its size over about 300 ms and turns opaque in about 180 ms; actions 48 pt capsules 8 apart, inset 15.5, greyed glass under 11% white; a press grows the alert by 1.6%; closing fades it in about 80 ms at its own size |
| Switch | 62 × 28 pt track, `#30D158` while on and a pale fill (`#DFDFEC` at 31%) while off; a 36 × 24 pt white thumb 2 pt in from the ends, casting a shadow of black at 12% blurred 8 pt and dropped 2 pt |
| Switch lens | 57 × 37.5 pt of clear glass about the thumb's centre, showing the track 1.25 times smaller and folding it at the rim; the thumb blurs into it in about 60 ms, it travels about 150 ms while the track's colour fades, and blurs back into a white thumb over about 200 ms once it rests |
| Slider | 31 pt tall; a 6 pt track, `#0091FF` up to the thumb and white at 13% past it; the switch's thumb and lens, moved only by a drag that starts on the thumb |
| Glass button | Capsules 28, 34.5 and 50.5 pt tall; a prominent button's tint covers regular glass at 95% and clear glass at 80% |
| Clear glass | No tone, lightened by about 0.086; blurred about 2 pt and shown 1.25 times larger, its rim lit nearly white |
| Context menu | The card grows about 6% while held; once the long press has held half a second, or the system's long press if longer, the screen darkens to 52% and the card lifts to 110% on an iPhone and an iPad alike, or a preview of its own lifts at its own size in corners of 22 pt, and the menu's glass flows out of the card's edge to stand 22 pt below it wherever it fits there and above it otherwise, aligned with its side nearer the screen's edge, in about 200 ms; it closes back into the card |
| Content on light glass | Resolved in the light colour scheme: primary black, secondary black at 55%, tertiary black at 32%; explicit colours stay; `QuvenGlassAppearance` reports the turn |

Android screenshots may be Display P3 while ReplayKit frames are sRGB, so convert them before you compare anything, or a
whole channel drifts by a few levels and reads as a difference in the glass when it is only a difference in the colour
space.

## Sample

The sample opens on a gallery of every Liquid Glass element Apple draws: the ones the library draws stand over hard
content to try, and the rest say what they will do. The reference app lists the same exhibits in the same order with
the system's own elements, so the two can be held side by side.

`SampleActivity` is the bench the measurements use: `adb shell am start -n tv.quven.glass.sample/.SampleActivity --ef scroll 224 --ef shift 21 --ez panel false --ei tab 2
--ef gap 8 --ez liquid true --ef refraction 22 ...`: every `Knob.key` of the tuning panel is also an extra. `shift`
moves the content left, so what sits under the centred bar matches the iPad's wider screen (21 dp on the tablet it was
tuned on). With `--ez menu true` the gear opens the reference's system menu instead of the tuning panel, and
`--ef window 3.5` copies the top-left corner on every frame for that many seconds, from a second after launch, into
`files/window/` of the app's external storage as raw RGBA frames with their times.

The gallery records too. `--es exhibit Switch --ef window 3 --es region 270,400,110,140` opens straight on an
exhibit and records that region, given in dp, at two pixels per dp, so a capture can be laid frame by frame beside the
reference's. A large region takes `--ef scale 1` so its frames fit in memory. The gallery runs once: a later intent
reaches the running gallery, which shows the exhibit it names, and `--es backdropOffset dx,dy` draws every stage's
content moved by that many dp, so an exhibit stands over what it stands over in the iPad's larger stage. Judge motion on
the release build, never on a debuggable one, which runs Compose interpreted and drops frames the release build
doesn't. `./gradlew :sample:installRelease` builds it.

## Reference launch environment

`reference/ios/build-device.sh <profile> <identity>`, then `xcrun devicectl device install app` and `device process
launch --environment-variables '{...}'`; ReplayKit asks once per install, and the frames land in the app's Documents.
Launched from the Home Screen the reference shows the gallery; any `GLASS_` variable launches it for a measurement.

| Variable | Effect |
|---|---|
| `GLASS_SCROLL`, `GLASS_SCROLLS` | The scroll offset, or a comma list of offsets captured one after the other. |
| `GLASS_TAB`, `GLASS_GAP` | The bar's held entry and the space before its Search circle. |
| `GLASS_CAPTURE` | The name the frames are saved under. |
| `GLASS_RECORD`, `GLASS_TAPS`, `GLASS_REGION` | Keep a clip per press, from a second before it to this many seconds after, for this many presses, of this region (`x,y,w,h` in points). |
| `GLASS_WINDOW` | Keep every frame of the region for this many seconds, a second after launch, whatever is pressed: the way to record the gear's menu opening. |
| `GLASS_PROBE`, `GLASS_PROBE_SIZES` | Glass circles of these sizes over flat colours instead of the screen. |
| `GLASS_PROBE_INK` | Each probe circle carries a glyph in the primary style, one in explicit white and one telling its colour scheme. |
| `GLASS_BLUR_PROBE` | Glass of thirteen sizes and shapes over a checkerboard of 40 pt cells instead of the screen; with `GLASS_PROBE_BARE` the checkerboard alone, with `GLASS_PROBE_PALETTE` cells of five greys and three colours centred on the pieces, scaled by `GLASS_PROBE_SCALE`, with `GLASS_PROBE_SET=thin` eighteen capsules of thin glass along the cells, with `GLASS_PROBE_SET=menu` a system menu's control whose open menu stands over the cells, and with `GLASS_PROBE_SET=sheet` a control presenting an empty sheet at its medium height. The sample's gallery draws the same probe for the `probe` extra (`blur`, `bare`, `palette`, `palette-thin`, and `menu` with a menu's panel where the reference's menu opens, each with `-bare`) and `probeScale`. |
| `GLASS_CONTROLS` | The system's tab bar and segmented control instead of the screen. |
| `GLASS_CONTROLS_WHITE` | Those controls over a white page. |
| `GLASS_MENUS` | A pull-down with every kind of entry, a plain menu and a card's context menu over the screen. |
| `GLASS_OPEN` | An exhibit that presents a dialog, such as the alert, presents it as soon as it opens. |
| `GLASS_REMOTE` | The gallery opens on `GLASS_EXHIBIT` (or the first exhibit), records itself from the first command that asks for a frame, so ReplayKit asks once a launch and never while the Mac records over the cable, and follows the Darwin notifications `tv.quven.glass.remote.show.<exhibit>`, `.save` and `.record.<seconds>` (`devicectl device notification post`); `remote-last.txt` in Documents names the latest frames, and `remote-region.txt` the region a recording keeps. A Mac can also take the screen over the cable as a capture device, with no prompt at all. |

## Licence

MIT No Attribution, copyright Quven Technologies S.R.L. Use it, change it and ship it in anything you like; you don't
need to credit us. The sample and the reference draw their backdrop's text in Inter, which keeps its own licence, the
SIL Open Font License in `fonts/OFL.txt`; the library itself carries no font.
