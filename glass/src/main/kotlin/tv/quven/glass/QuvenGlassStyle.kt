package tv.quven.glass

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 * @property blur The radius of the blur applied to the backdrop.
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
    val blur: Dp = 4.5.dp,
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
) {

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
     * and plain glass lights what lies under it while a prominent button, given a [tint], lightens its tint.
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
            copy(pressGlow = ButtonPressGlow, pressGrowth = 0f, pressExpansion = ButtonPressExpansion, rimGlow = ButtonRimGlow)
        }

    /**
     * Returns this material as a menu draws it: frosted more deeply, its rim lit by what lies just outside it, and still
     * lit by the press of the control it grows from, as Apple's system menus are.
     *
     * @return The menu's material.
     */
    public fun forMenus(): QuvenGlassStyle = copy(blur = MenuBlur, pressGlow = ButtonPressGlow, rimGlow = MenuRimGlow)

    public companion object {
        /** Gets the material of a surface standing over content of its own, Apple's regular glass in its dark appearance. */
        public val Standard: QuvenGlassStyle = QuvenGlassStyle()

        /**
         * Gets Apple's clear glass: the fold of [Standard] without its tone, lightening what it stands over, which it
         * blurs less and shows a fifth larger, as a lens does, its rim lit white, for glass standing over bright media.
         */
        public val Clear: QuvenGlassStyle = QuvenGlassStyle(
            thinTone = QuvenGlassTone.Clear,
            thickTone = QuvenGlassTone.Clear,
            lightTone = QuvenGlassTone.Clear,
            tintStrength = 0.8f,
            brighten = 0.086f,
            blur = 2.dp,
            zoom = 0.8f,
            specular = 0.75f,
        )

        private const val ButtonPressGlow = 3.6f
        private const val ButtonTintGlow = 1.65f
        private const val ButtonRimGlow = 1f
        private const val ButtonRimLight = 0.3f
        private val ButtonPressExpansion = 16.dp
        private const val MenuRimGlow = 1.7f
        private val MenuBlur = 9.5.dp
    }
}
