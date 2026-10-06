package tv.quven.glass

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Describes the glass material: the frosted look every API level draws, and the Liquid Glass optics drawn over a live
 * backdrop from Android 13.
 *
 * Liquid Glass folds the backdrop towards the rim by the outline's corner radius, blurs it and tones it by the
 * surface's thickness: [thinTone] up to [thinSize], [thickTone] from [thickSize], blended between; thin glass over a
 * bright backdrop turns to [lightTone] as [adaptation] decides. A thin rim catches the light.
 *
 * @property ground The colour under the frost of the static material, transparent where the surface stands on one of
 * its own.
 * @property trackTop The static material's frost at the surface's top.
 * @property trackBottom The static material's frost at the surface's foot.
 * @property rimTop The static material's rim at its top.
 * @property rimBottom The static material's rim at its foot.
 * @property pillTop The static pill's frost at its top.
 * @property pillBottom The static pill's frost at its foot.
 * @property pillRim The static pill's rim.
 * @property pillShadow The elevation of the static pill's shadow.
 * @property slideDamping The damping ratio of the spring the pill slides and the surface swells on.
 * @property slideStiffness The stiffness of the spring the pill slides and the surface swells on.
 * @property thinTone The tone of a surface whose shorter side is no longer than [thinSize].
 * @property thickTone The tone of a surface whose shorter side is no shorter than [thickSize].
 * @property thinSize The shorter side at and below which a surface is thin glass.
 * @property thickSize The shorter side at and above which a surface is thick glass.
 * @property lightTone The tone of thin glass turned light over a bright backdrop.
 * @property adaptation When thin glass turns light and back.
 * @property tint The colour laid over the toned backdrop, by its alpha; [tinted] sets it at the material's own strength.
 * @property tintStrength How much of a tint [tinted] lays over the glass, from 0 to 1.
 * @property brighten The share of white added to everything the glass shows, as clear glass lightens what it stands over.
 * @property blur The radius of the blur applied to the backdrop under a surface whose shorter side is no longer than
 * [thinSize].
 * @property backdropScale The resolution the backdrop is recorded and blurred at, as a share of the screen's, from 0.25
 * to 1; the outline, the fold and the rim are always drawn at full resolution.
 * @property refraction The least distance in from the rim from which the lens takes what the rim shows: the bevel folds
 * the inside of the glass back towards the edge.
 * @property edgeWidth The least width of the band inside the rim where the lens folds the backdrop.
 * @property cornerRefraction How far in the lens reaches at the rim, as a share of the outline's corner radius, where that
 * is more than [refraction].
 * @property cornerWidth The width of the band where the lens folds the backdrop, as a share of the outline's corner
 * radius, where that is more than [edgeWidth].
 * @property lensCurve The power the fold falls off with across the band: the higher, the more it gathers at the rim.
 * @property dispersion The share of the bend by which red and blue part along the edge, from 0 to 1.
 * @property specular The strength of the light caught by the rim, from 0 to 1.
 * @property lightAngle The direction the rim's light comes from, in degrees clockwise from the top.
 * @property platter The colour of the pill marking the held option on Liquid Glass.
 * @property lightPlatter The colour of that pill on glass turned light.
 * @property pressGrowth The share of its shorter side by which a pressed surface swells.
 * @property pressGlow How brightly a pressed surface lights its backdrop: fully pressed, the glass shows the backdrop
 * this many times brighter and untoned, as a glass button lights under the finger; 0 keeps the toned glass.
 * @property rimGlow How brightly the rim shows what lies just outside the glass: the backdrop there, this many times
 * brighter and untoned, as the rim of a system menu shows it; 0 keeps the toned rim.
 * @property shadow The colour of the soft shadow under a Liquid Glass surface.
 * @property shadowRadius The blur radius of the shadow under a Liquid Glass surface.
 * @property zoom How much smaller Liquid Glass shows what lies under its body: 1 shows it at its own size, and 1.25 a
 * fifth smaller, as the lens a control's thumb lifts into shows its track.
 * @property backdropDim The share of black Liquid Glass sees laid over its backdrop before it tones it, from 0 to 1, as
 * glass standing over a dimmed screen reads the screen dimmed; its rim reads it dimmed too.
 * @property pressExpansion How far a pressed surface's longer side grows, the shorter growing in proportion, as Apple's
 * glass buttons grow under the finger; 0 leaves [pressGrowth] alone to swell it.
 * @property pressTintGlow How much brighter a tinted surface turns while pressed, as a prominent button lightens under the
 * finger; 1 keeps its tint.
 * @property rimLight The share of white the rim turns all the way round, from 0 to 1, as a glass button's rim catches the
 * light; 0 leaves the rim to [specular].
 * @property pressLighten How far a pressed surface turns towards white, the whole surface alike, as a multiple of the
 * mean luminance of what lies under it, as Apple's glass buttons lighten under the finger: barely over dark content, to
 * white over bright content; 0 leaves it to [pressGlow].
 * @property pressWhite The share of white a pressed surface turns towards at least, whatever lies under it, from 0 to 1, as
 * the items of Apple's tab bar light under the finger even over black; 0 leaves it to [pressLighten].
 * @property pressSaturation How saturated a pressed surface shows its backdrop as [pressGlow] lights it: 1 keeps its
 * colours, more deepens them, as Apple's glass buttons deepen what lies under them while they light.
 * @property pressZoom How much further from its centre a pressed surface reads its backdrop, as Apple's glass buttons
 * show more of what lies around them while they swell; 1 reads it where it lies.
 * @property pressBlur How much further a pressed surface blurs its backdrop as [pressGlow] lights it, as the radius it
 * softens it over, as Apple's glass buttons soften what lies under them while they light.
 * @property pressBrighten How much light a pressed surface adds to its backdrop as [pressGlow] lights it, from 0 to 1, all
 * of it over black and less the brighter the backdrop, so it lights even over black, as Apple's glass buttons do.
 * @property touchLight How much white light gathers under the finger on interactive glass and follows it, at its middle,
 * from 0 to 1; 0 for glass that adds no white where it is touched.
 * @property touchLightSpread How far the light under the finger spreads, as a Gaussian's standard deviation.
 * @property touchGlow How much brighter interactive glass shows its own colours under the finger, at its middle, as a
 * factor; 0 for glass whose colours do not brighten where it is touched.
 * @property touchSaturation The saturation of the colours [touchGlow] brightens, 1 leaving it unchanged.
 * @property touchPassesThrough Whether a press on glass that lights under the finger also reaches what lies under it, as
 * on Apple's interactive glass holding no controls of its own; `false` keeps the press for what stands on the glass.
 * @property thickBlur The radius of the blur under a surface whose shorter side is at least [thickBlurSize]; between
 * [thinSize] and [thickBlurSize] the blur grows from [blur] in proportion, as Apple's glass frosts more deeply the larger
 * it stands.
 * @property thickBlurSize The shorter side from which a surface takes [thickBlur].
 * @property largeTone The tone of a surface whose shorter side is no shorter than [largeSize], as a panel, a sheet or a
 * sidebar, which Apple's glass veils more deeply than a bar.
 * @property largeFromSize The shorter side from which thick glass begins to turn into large glass.
 * @property largeSize The shorter side at and above which a surface is large glass.
 */
@Immutable
public data class QuvenGlassStyle(
    val ground: Color = Color.Transparent,
    val trackTop: Color = Color(0x29FFFFFF),
    val trackBottom: Color = Color(0x0DFFFFFF),
    val rimTop: Color = Color(0x4DFFFFFF),
    val rimBottom: Color = Color(0x12FFFFFF),
    val pillTop: Color = Color(0x52FFFFFF),
    val pillBottom: Color = Color(0x24FFFFFF),
    val pillRim: Color = Color(0x66FFFFFF),
    val pillShadow: Dp = 8.dp,
    val slideDamping: Float = 0.62f,
    val slideStiffness: Float = 520f,
    val thinTone: QuvenGlassTone = QuvenGlassTone.Thin,
    val thickTone: QuvenGlassTone = QuvenGlassTone.Thick,
    val thinSize: Dp = 63.dp,
    val thickSize: Dp = 66.dp,
    val lightTone: QuvenGlassTone = QuvenGlassTone.Light,
    val adaptation: QuvenGlassAdaptation = QuvenGlassAdaptation(),
    val tint: Color = Color.Transparent,
    val tintStrength: Float = 0.95f,
    val brighten: Float = 0f,
    val blur: Dp = 3.6.dp,
    val backdropScale: Float = 0.5f,
    val refraction: Dp = 22.dp,
    val edgeWidth: Dp = 12.dp,
    val cornerRefraction: Float = 1.43f,
    val cornerWidth: Float = 0.46f,
    val lensCurve: Float = 2.5f,
    val dispersion: Float = 0.015f,
    val specular: Float = 0.2f,
    val lightAngle: Float = 0f,
    val platter: Color = Color(0x1CFFFFFF),
    val lightPlatter: Color = Color(0x12000000),
    val pressGrowth: Float = 0.12f,
    val pressGlow: Float = 0f,
    val rimGlow: Float = 0f,
    val shadow: Color = Color(0x14000000),
    val shadowRadius: Dp = 14.dp,
    val zoom: Float = 1f,
    val backdropDim: Float = 0f,
    val pressExpansion: Dp = 0.dp,
    val pressTintGlow: Float = 1f,
    val rimLight: Float = 0f,
    val pressLighten: Float = 0f,
    val pressWhite: Float = 0f,
    val pressSaturation: Float = 1f,
    val pressZoom: Float = 1f,
    val pressBlur: Dp = 0.dp,
    val pressBrighten: Float = 0f,
    val touchLight: Float = 0f,
    val touchLightSpread: Dp = 95.dp,
    val touchGlow: Float = 0f,
    val touchSaturation: Float = 1f,
    val touchPassesThrough: Boolean = false,
    val thickBlur: Dp = 8.4.dp,
    val thickBlurSize: Dp = 136.dp,
    val largeTone: QuvenGlassTone = QuvenGlassTone.Large,
    val largeFromSize: Dp = 100.dp,
    val largeSize: Dp = 124.dp,
) {

    /**
     * Returns the radius of the blur under a surface whose shorter side is [shorterSide]: [blur] up to [thinSize],
     * [thickBlur] from [thickBlurSize], in proportion between.
     *
     * @param shorterSide The surface's shorter side, in pixels.
     * @param density The density the sizes are read at.
     * @return The radius, in pixels.
     */
    internal fun blurFor(shorterSide: Float, density: Density): Float = with(density) {
        val start = thinSize.toPx()
        val end = thickBlurSize.toPx()
        val share = if (end > start) ((shorterSide - start) / (end - start)).coerceIn(0f, 1f) else if (shorterSide >= end) 1f else 0f
        blur.toPx() + (thickBlur.toPx() - blur.toPx()) * share
    }

    /**
     * Returns how far the shade of a surface whose shorter side is [shorterSide] lightens with the mean luminance around
     * it, its tones blended by size as the program blends them.
     *
     * @param shorterSide The surface's shorter side, in pixels.
     * @param density The density the sizes are read at.
     * @return The adaptation; 0 for a surface whose tone holds.
     */
    internal fun adaptationFor(shorterSide: Float, density: Density): Float = with(density) {
        val thickness = smoothstep(thinSize.toPx(), thickSize.toPx(), shorterSide)
        val largeness = smoothstep(largeFromSize.toPx(), largeSize.toPx(), shorterSide)
        lerp(lerp(thinTone.adaptation, thickTone.adaptation, thickness), largeTone.adaptation, largeness)
    }

    /** Gets whether any of the tones lightens with the brightness of the backdrop around its surface. */
    internal val followsBrightness: Boolean
        get() = thinTone.adaptation != 0f || thickTone.adaptation != 0f || largeTone.adaptation != 0f || lightTone.adaptation != 0f

    /**
     * Returns this material blurring the backdrop by [radius] whatever its size.
     *
     * @param radius The radius of the blur.
     * @return The material.
     */
    internal fun withBlur(radius: Dp): QuvenGlassStyle = copy(blur = radius, thickBlur = radius)

    /**
     * Returns the spring the pill slides on between options: quick, passing its place a little before it settles.
     *
     * @param T The type of the animated value.
     * @return The spring.
     */
    public fun <T> slideSpring(): SpringSpec<T> = spring(dampingRatio = slideDamping, stiffness = slideStiffness)

    /**
     * Returns this material tinted with [color], as Apple's glass takes a tint: nearly opaque on regular glass, letting
     * a fifth of the backdrop through on clear glass.
     *
     * @param color The tint; its own alpha is replaced by the material's [tintStrength].
     * @return The tinted material.
     */
    public fun tinted(color: Color): QuvenGlassStyle = copy(tint = color.copy(alpha = tintStrength))

    /**
     * Returns this material as a glass button draws it, as Apple's glass buttons answer the finger: the button grows,
     * plain glass lights what lies under and around it, deepening its colours, and a prominent button, given a [tint],
     * lightens its tint.
     *
     * @param tint The tint of a prominent button, or [Color.Unspecified] for plain glass.
     * @return The button's material.
     */
    public fun forButtons(tint: Color = Color.Unspecified): QuvenGlassStyle =
        if (tint.isSpecified) {
            // A prominent button covers its glass whole with the tint, which its rim lightens.
            copy(
                tint = tint.copy(alpha = 1f),
                pressGrowth = 0f,
                pressExpansion = ButtonPressExpansion,
                pressTintGlow = ButtonTintGlow,
                rimLight = ButtonRimLight,
            )
        } else {
            // Plain glass shows at its rim what lies just outside it, as it is.
            copy(
                pressGlow = ButtonPressGlow,
                pressSaturation = ButtonPressSaturation,
                pressZoom = ButtonPressZoom,
                pressBlur = ButtonPressBlur,
                pressBrighten = ButtonPressBrighten,
                pressGrowth = 0f,
                pressExpansion = ButtonPressExpansion,
                rimGlow = ButtonRimGlow,
            )
        }

    /**
     * Returns this material as a menu draws it: frosted alike at every size, veiled more deeply, its rim lit by what lies
     * just outside it, and still lit by the press of the control it grows from, as Apple's system menus are.
     *
     * @return The menu's material.
     */
    public fun forMenus(): QuvenGlassStyle =
        copy(blur = MenuBlur, thickBlur = MenuBlur, largeTone = QuvenGlassTone.Menu, pressGlow = MenuPressGlow, rimGlow = MenuRimGlow)

    /**
     * Returns this material as a split view's sidebar draws it, as iPadOS draws its own: blurring what lies around it so
     * deeply that the detail beside it glows in from its edge, veiled less than a panel, its rim lit.
     *
     * @return The sidebar's material.
     */
    internal fun forSidebars(): QuvenGlassStyle =
        copy(blur = SidebarBlur, thickBlur = SidebarBlur, largeTone = QuvenGlassTone.Sidebar, rimLight = SidebarRimLight)

    /**
     * Returns this material as an alert draws it over the screen it dims, as Apple's alerts: a menu's glass, letting more
     * of the screen's colours through, its rim faintly lit.
     *
     * @return The alert's material.
     */
    internal fun forAlerts(): QuvenGlassStyle =
        forMenus().dimmed(ScreenDim).copy(largeTone = QuvenGlassTone.Alert, rimLight = AlertRimLight)

    /** Gets whether this material lights under the finger, which makes its glass follow the finger. */
    internal val followsFinger: Boolean
        get() = touchLight > 0f || touchGlow > 0f

    /**
     * Returns this material as interactive glass draws it, for glass holding no controls of its own: its colours brighten
     * under the finger and the light follows it, while the press still reaches what lies under the glass, so a drag across
     * it scrolls what it stands over, as on Apple's interactive glass.
     *
     * @return The interactive material.
     */
    public fun interactive(): QuvenGlassStyle = copy(
        touchGlow = InteractiveTouchGlow,
        touchSaturation = InteractiveTouchSaturation,
        touchLightSpread = InteractiveTouchSpread,
        touchPassesThrough = true,
    )

    /**
     * Returns this material as a group of toolbar buttons draws it: it grows under the finger as a glass button does and
     * lights towards white around the finger even over black, as Apple's toolbar items do.
     *
     * @return The group's material.
     */
    internal fun forToolbarItems(): QuvenGlassStyle =
        forButtons().copy(pressGlow = 0f, pressWhite = ToolbarPressWhite, touchLight = ToolbarTouchLight, touchLightSpread = ToolbarTouchSpread)

    /**
     * Returns this material as a pressable item of a tab bar draws it, such as its Search circle: it grows under the finger
     * and lights towards white even over black, as Apple's tab bar items do.
     *
     * @return The item's material.
     */
    internal fun forBarItems(): QuvenGlassStyle = copy(pressWhite = BarItemPressWhite, pressGrowth = 0f, pressExpansion = BarItemPressExpansion)

    /**
     * Returns this material as a tab bar's track of options draws it: the whole track grows under the finger and lights
     * around it, and the chosen option's platter is brighter, as Apple's tab bar does.
     *
     * @return The track's material.
     */
    internal fun forTracks(): QuvenGlassStyle = copy(
        pressGrowth = 0f,
        pressExpansion = TrackPressExpansion,
        touchLight = TrackTouchLight,
        touchLightSpread = TrackTouchSpread,
        platter = TrackPlatter,
    )

    /**
     * Returns this material with its growth under the finger left out where motion is reduced, so a press only lights it.
     *
     * @param reduceMotion Whether motion is reduced.
     * @return This material, or this material without its growth.
     */
    internal fun growingUnless(reduceMotion: Boolean): QuvenGlassStyle =
        if (reduceMotion && (pressExpansion != 0.dp || pressGrowth != 0f)) copy(pressExpansion = 0.dp, pressGrowth = 0f) else this

    public companion object {
        /** Gets the material of a surface standing over content of its own, Apple's regular glass in its dark appearance. */
        public val Standard: QuvenGlassStyle = QuvenGlassStyle(rimGlow = StandardRimGlow)

        /**
         * Gets Apple's clear glass: the fold of [Standard] without its tone, lightening what it stands over, which it
         * blurs less and shows a fifth larger, as a lens does, its rim lit white, for glass standing over bright media.
         */
        public val Clear: QuvenGlassStyle = QuvenGlassStyle(
            thinTone = QuvenGlassTone.Clear,
            thickTone = QuvenGlassTone.Clear,
            largeTone = QuvenGlassTone.Clear,
            lightTone = QuvenGlassTone.Clear,
            tintStrength = 0.8f,
            brighten = 0.086f,
            blur = 2.dp,
            thickBlur = 2.dp,
            zoom = 0.8f,
            specular = 0.75f,
        )

        private const val BarItemPressWhite = 0.41f
        // Measured on an iPad's interactive glass: its colours 1.45 times as bright and 1.2 times as saturated under the
        // finger, falling as a Gaussian of 90 pt.
        private const val InteractiveTouchGlow = 1.45f
        private const val InteractiveTouchSaturation = 1.2f
        private val InteractiveTouchSpread = 90.dp
        private const val ToolbarTouchLight = 0.5f
        private const val ToolbarPressWhite = 0.11f
        private val ToolbarTouchSpread = 45.dp
        private val BarItemPressExpansion = 9.dp
        // Measured on the system's tab bar on an iPad: the track grows 17.5 pt along its length under the finger, which 16
        // asked of ours draw with the rim; it lights 35 levels over black there, falling as a Gaussian of about 104 pt.
        private val TrackPressExpansion = 16.dp
        private const val TrackTouchLight = 0.137f
        private val TrackTouchSpread = 104.dp
        // Measured on the system's tab bar on an iPad: the chosen tab's platter is white at 15%.
        private val TrackPlatter = Color(0x26FFFFFF)
        // Measured on a physical iPad: the rim of still glass shows what lies just outside it, as it is.
        private const val StandardRimGlow = 1f
        private const val MenuPressGlow = 3.6f
        private const val ButtonTintGlow = 1.15f
        private const val ButtonPressGlow = 1.5f
        private const val ButtonPressSaturation = 1.3f
        private const val ButtonPressZoom = 1f
        private val ButtonPressBlur = 8.dp
        private const val ButtonPressBrighten = 0.4f
        private const val ButtonRimGlow = 1f
        private const val ButtonRimLight = 0.3f
        private val ButtonPressExpansion = 16.dp
        private const val MenuRimGlow = 1.7f
        private val MenuBlur = 7.2.dp

        // Measured on a split view's sidebar on an iPad: what lies around it blurs by σ about 145 pt.
        private val SidebarBlur = 250.dp
        private const val SidebarRimLight = 0.2f
        private const val AlertRimLight = 0.06f
    }
}

/**
 * Returns this material as glass over a screen dimmed by [dim] draws it: it reads the screen dimmed, and its rim shows
 * the dimmed screen beyond it [DimmedRimGlow] times brighter, as the system's menus and alerts do over the screen they
 * dim.
 *
 * @param dim The share of black laid over the screen, from 0 to 1.
 * @return The material.
 */
internal fun QuvenGlassStyle.dimmed(dim: Float): QuvenGlassStyle = copy(backdropDim = dim, rimGlow = DimmedRimGlow)

/** How dark the screen turns behind a context menu's lifted control or an alert, as measured on the system's. */
internal const val ScreenDim = 0.48f

/** How much brighter the rim of glass over a dimmed screen shows the screen beyond it, as measured on the system's. */
private const val DimmedRimGlow = 1.2f
