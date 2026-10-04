package tv.quven.glass

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
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
 * @property tint The colour laid over the toned backdrop, by its alpha.
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
) {

    /**
     * Returns the spring the pill slides on between options: quick, passing its place a little before it settles.
     *
     * @param T The type of the animated value.
     * @return The spring.
     */
    public fun <T> slideSpring(): SpringSpec<T> = spring(dampingRatio = slideDamping, stiffness = slideStiffness)

    public companion object {
        /** Gets the material of a surface standing over content of its own, Apple's regular glass in its dark appearance. */
        public val Standard: QuvenGlassStyle = QuvenGlassStyle()
    }
}
