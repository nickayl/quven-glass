# Quven Glass

Apple's Liquid Glass for Jetpack Compose, made by [Quven Technologies S.R.L.](https://quven.tv) and free for anyone to
use. Every constant comes from measuring the system material on an iPad, not from tuning by eye, and the whole effect is
one AGSL program over `GraphicsLayer` and `RenderEffect`, with no third-party effect library behind it. How close does
it get? Over 111 scrolled scenes the inside of a bar differs from the iPad's by 1.2/255.

What you get:

- glass surfaces that fold, blur, tone and light the content behind them, swelling under a press;
- neighbouring surfaces that flow into one piece of glass as they come close;
- a segmented track whose pill turns into a lens under a press or a drag;
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
    implementation("tv.quven.glass:glass:2.4.0")
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
with `onDraggedTo` a drag carries it under the finger and hands you the option it's let go over.

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

`hangingFromTopRight` and `above` place the panel for a button on the other side or an entry of a bottom bar.

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
            style = QuvenGlassStyle.Standard.copy(blur = 9.5.dp, pressGlow = 3.6f, rimGlow = 1.7f),
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

The menu closes itself once a row is chosen, on Back and on a press anywhere else. It opens over its button and hangs
down from it when the room below is enough; otherwise it rises, and an untitled menu then lists its rows from the foot
up, so the first one stays nearest the finger. A menu holding a choice makes room for the check before every row, as
iOS does. `QuvenGlassMenuChoice` with `role = Role.Checkbox` is a toggle, `enabled = false` greys a row out, and
`QuvenGlassDropdown` takes the place of the box when you'd rather hold the open state yourself.

A system menu doesn't dim what's behind it, and neither does the host. Its rim is brighter than a bar's, which is what
`rimGlow` draws. Give the button that opens it the same `pressGlow`: it lights up under the finger, and the menu's glass
carries that light for the first part of its growth. A finger can also slide along an open menu: the row under it
lights at once with a light tick, and the row it lifts over is the one chosen.

## Styling

`QuvenGlassStyle.Standard` is the measured material, but you can copy it and change what you need:

| Parameter | What it changes |
|---|---|
| `thinTone`, `thickTone`, `lightTone` | The colour each kind of glass leans towards, how far, and its saturation |
| `thinSize`, `thickSize` | The shorter sides where glass stops being thin and becomes thick |
| `blur`, `refraction`, `edgeWidth` | The blur and the fold along the rim |
| `specular`, `lightAngle` | The light caught by the rim and where it comes from |
| `platter`, `lightPlatter` | The pill under a held option, on dark and on light glass |
| `adaptation` | When thin glass turns light and how quickly |
| `pressGrowth`, `slideDamping`, `slideStiffness` | How a press swells the glass and how the pill travels |
| `pressGlow` | How brightly a pressed button lights what's behind it; bars keep it at zero |
| `rimGlow` | How brightly the rim shows what lies just outside it; only menus use it |
| `ground`, `trackTop`, `rimTop` and siblings | The static material drawn without Liquid Glass |

You should pass `reduceMotion` from the system setting to every surface. Springs then become short fades.

## Cost

Although each surface redraws a blurred copy of the backdrop on every frame, it does so at half resolution in one
shader pass. And thin glass samples its
backdrop twice a second into 8×8 pixels on a renderer of its own; it doesn't touch the main thread, and it pauses while
the window is out of view.

## Measured model

Reference: iPad A16, iOS 26.5, Liquid Glass set to Glass; `reference/ios` frames through ReplayKit.

| Property | Value |
|---|---|
| Thickness | Thin up to a shorter side of 63 pt, thick from 66 pt; interactive or still alike |
| Thick tone | Lean 0.77 towards `0x27`, saturation 2.49, sRGB |
| Thin tone | Lean 0.42 + 0.23 × luminance towards `0x2C`, saturation 1.22 |
| Platter | White at 11%; black at 7% on light glass |
| Fold | 1.43 × corner radius over a band of 0.46 × radius, at least 22 over 12 pt, power 2.5 |
| Blur | Gaussian, σ 3.5 pt |
| Rim | Lit from above, falling off with the square of the facing, 0.15 underneath |
| Tap lens | Forms in 50–65 ms, travels about 200 ms, settles about 100 ms after arriving |
| Drag | The lens follows the finger and settles on the nearest option |
| Light glass | Thin glass only: lean 0.82 towards `0xF5`, saturation 3.27; turns light above a mean channel of 0.74 and dark below 0.64, smoothed over 2 s |
| Press on a glass button | The backdrop lit about 3.6 times and untoned, within 80–100 ms |
| Menu | 223 × 38 pt rows on iPad, 247 × 42 pt on iPhone; 25 pt corners; glyph centred at 32.5 or 37 pt, name from 55 or 62 pt |
| Menu choices | Check centred at 21 or 24 pt, name from 32 or 36 pt; a menu holding one widens by 12 or 14 pt and moves its glyphs as far |
| Menu placement | Over its button, hanging when the room below suffices, otherwise rising with untitled rows reversed |
| Menu glass | Thick tone, blur σ 7.4 pt, no dimming behind it; a rim about 1 pt wide shows the backdrop just outside it 1.5–1.9 times brighter |
| Menu opening | Spring with damping 0.72 and stiffness 580; a capsule until 60% of the way, content fading in and coming into focus from a quarter of the way |
| Menu closing | Back into its button in about 165 ms on an almost even ease, with no bounce |
| Menu press | The whole menu washes about 15% whiter within 50 ms; the held row's capsule, 13 pt in from the sides, follows after 150 ms, fills in over 180 ms and goes the moment the finger lifts |
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
| `GLASS_CONTROLS` | The system's tab bar and segmented control instead of the screen. |
| `GLASS_CONTROLS_WHITE` | Those controls over a white page. |
| `GLASS_MENUS` | A pull-down with every kind of entry, a plain menu and a card's context menu over the screen. |

## Licence

MIT No Attribution, copyright Quven Technologies S.R.L. Use it, change it and ship it in anything you like; you don't
need to credit us.
