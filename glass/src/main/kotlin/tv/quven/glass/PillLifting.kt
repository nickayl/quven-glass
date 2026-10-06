package tv.quven.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * How the pill of a [QuvenGlassSegmentedTrack] lifts under a press or on its way to another option: within the track's
 * own glass, into a lens of clear glass over the track, as Apple's tab bar lifts its selection, or not at all.
 */
internal sealed interface PillLifting {

    /**
     * Returns what the track's glass reads of the pill.
     *
     * @param motion The pill's motion.
     * @param frame Where the pill stands at this frame.
     * @param rect The pill's rectangle in the track, swollen by its press.
     * @param radius The radius of the pill's corners.
     * @return The pill the glass draws.
     */
    fun pill(motion: GlassPillMotion, frame: PillFrame, rect: Rect, radius: Float): GlassPill

    /**
     * Returns how far the track's own glass is lifted, which grows and lights it.
     *
     * @param motion The pill's motion.
     * @return The lift.
     */
    fun trackLift(motion: GlassPillMotion): Float = motion.press.value

    /**
     * Returns the modifier of the box the track's options stand in.
     *
     * @param motion The pill's motion.
     * @param optionSize The size every option takes.
     * @return The modifier.
     */
    fun faces(motion: GlassPillMotion, optionSize: DpSize): Modifier

    /**
     * Draws what stands over the track while the pill is lifted.
     *
     * @param motion The pill's motion.
     * @param optionSize The size every option takes.
     */
    @Composable
    fun BoxScope.Lens(motion: GlassPillMotion, optionSize: DpSize)

    /** The pill lifts within the track's glass: it bends what lies under it and swells the glass where it stands. */
    data object WithinGlass : PillLifting {

        override fun pill(motion: GlassPillMotion, frame: PillFrame, rect: Rect, radius: Float): GlassPill {
            val lift = motion.lift.coerceAtLeast(0f)
            return GlassPill(rect, radius, motion.alpha.value, pillLens(frame.right - frame.left, frame.restWidth, lift), lift)
        }

        override fun faces(motion: GlassPillMotion, optionSize: DpSize): Modifier = Modifier

        @Composable
        override fun BoxScope.Lens(motion: GlassPillMotion, optionSize: DpSize) = Unit
    }

    /** The pill never lifts: it slides as a platter and the track's glass holds still under a press. */
    data object Flat : PillLifting {

        override fun pill(motion: GlassPillMotion, frame: PillFrame, rect: Rect, radius: Float): GlassPill =
            GlassPill(rect, radius, motion.alpha.value, lens = 0f, lift = 0f)

        override fun trackLift(motion: GlassPillMotion): Float = 0f

        override fun faces(motion: GlassPillMotion, optionSize: DpSize): Modifier = Modifier

        @Composable
        override fun BoxScope.Lens(motion: GlassPillMotion, optionSize: DpSize) = Unit
    }

    /**
     * The pill lifts into a lens of clear glass taller than the track, which shows the options recorded under it a
     * quarter larger and bends them at its rim, the track's own glass seen through it; the track's glass stays as it was,
     * its platter hidden under the lens until the lens has gone.
     */
    class IntoLens : PillLifting {

        private val recorded = QuvenGlassBackdrop(seeThrough = true)

        override fun pill(motion: GlassPillMotion, frame: PillFrame, rect: Rect, radius: Float): GlassPill =
            GlassPill(rect, radius, motion.alpha.value * motion.platter.value, lens = 0f, lift = 0f)

        override fun faces(motion: GlassPillMotion, optionSize: DpSize): Modifier =
            Modifier.lensSource(recorded, opacity = { motion.lensOpacity }) { motion.lensFrame(optionSize, this).takeIf { motion.lift > 0f } }

        @Composable
        override fun BoxScope.Lens(motion: GlassPillMotion, optionSize: DpSize) {
            val shown by remember(motion) { derivedStateOf { motion.lift > 0f } }
            if (!shown) return
            // The lens stands over the track, apart from any container its glass would otherwise join.
            CompositionLocalProvider(LocalGlassContainer provides null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .standingAt { motion.lensFrame(optionSize, this) }
                        .graphicsLayer { alpha = motion.lensOpacity }
                        .lensGlass(recorded, lensMaterial(motion.lift)),
                )
            }
        }
    }
}

/** Gets how much of the lens shows as the pill lifts: all of it once the lift has reached [LensShown]. */
private val GlassPillMotion.lensOpacity: Float
    get() = smoothstep(0f, LensShown, lift)

/**
 * Returns the frame of the lens the pill lifts into, as far as it is lifted: the pill's frame grown by [LensGrowth],
 * keeping the option's full height while the pill thins as it stretches.
 *
 * @param optionSize The size every option takes.
 * @param density The density the frame is measured at.
 * @return The frame, in pixels from the first option's start.
 */
private fun GlassPillMotion.lensFrame(optionSize: DpSize, density: Density): Rect = with(density) {
    val pill = frame(optionSize, density)
    val grown = lift.coerceIn(0f, 1f)
    val wide = LensGrowth.width.toPx() * grown / 2f
    val tall = LensGrowth.height.toPx() * grown / 2f
    val half = pill.restHeight / 2f
    val middle = optionSize.height.toPx() / 2f
    Rect(pill.left - wide, middle - half - tall, pill.right + wide, middle + half + tall)
}

/** How much larger the lens is than the pill it lifts from, as measured on the system's tab bar on an iPad. */
private val LensGrowth = DpSize(31.5.dp, 19.dp)

/** The share of the lift by which the lens has fully appeared. */
private const val LensShown = 0.3f

/** How much smaller the lens shows the options: Apple's tab bar shows them a quarter larger. */
private const val LensZoom = 0.8f

/** The share of white the lens's rim turns all the way round, as the tab bar's lens catches the light. */
private const val LensRimLight = 0.35f

/** How far the lens's rim parts red from blue, as the tab bar's lens shows a faint rainbow. */
private const val LensDispersion = 0.06f

/** How far the lens's rim bends what it shows: less than a control's thumb, so a glyph near the rim never smears. */
private val LensRefraction = 4.dp

/**
 * The clear glass of the lens: a control thumb's lens which leaves what lies beyond the track as dark as it is and casts
 * no shadow.
 */
private val LensMaterial: QuvenGlassStyle = GlassLensThumb.LensMaterial.copy(
    brighten = 0f,
    refraction = LensRefraction,
    dispersion = LensDispersion,
    shadow = Color.Transparent,
    rimLight = LensRimLight,
)

/**
 * Returns the lens's glass as far as the pill is lifted, which shows the options larger the further it is lifted, so the
 * options it magnifies settle onto their own places as it drops.
 *
 * @param lift How far the pill is lifted, from 0 to 1.
 * @return The material.
 */
private fun lensMaterial(lift: Float): QuvenGlassStyle = LensMaterial.copy(zoom = lerp(1f, LensZoom, lift.coerceIn(0f, 1f)))
