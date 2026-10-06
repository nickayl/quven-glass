package tv.quven.glass

import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * How a [QuvenGlassSegmentedTrack] answers a press and a move to another option: as Apple's tab bar does, or as a
 * selector standing in a page does.
 */
public abstract class QuvenGlassTrackFeel internal constructor() {

    /** Gets how the pill moves. */
    internal abstract val dynamics: PillDynamics

    /**
     * Returns the material the track draws its glass in.
     *
     * @param style The material the track was given.
     * @return The track's material.
     */
    internal abstract fun material(style: QuvenGlassStyle): QuvenGlassStyle

    /**
     * Returns how the pill lifts.
     *
     * @param lens The lens a track holds, should the pill lift into one.
     * @param liquid Whether the track draws Liquid Glass.
     * @param reduceMotion Whether motion is reduced.
     * @param count The number of options.
     * @return How the pill lifts.
     */
    internal abstract fun lifting(lens: PillLifting.IntoLens, liquid: Boolean, reduceMotion: Boolean, count: Int): PillLifting

    public companion object {
        /**
         * Gets the feel of Apple's tab bar: the whole track grows and lights under the finger, and the held pill lifts
         * into a lens of clear glass that crosses to the option pressed.
         */
        public val TabBar: QuvenGlassTrackFeel = TabBarFeel

        /**
         * Gets the feel of a selector standing in a page, such as a catalogue's density: the held pill slides to the
         * option pressed and the glass holds still.
         */
        public val Selector: QuvenGlassTrackFeel = SelectorFeel
    }
}

private object TabBarFeel : QuvenGlassTrackFeel() {

    // Measured on the system's tab bar on an iPad: the lens crosses on a spring with damping 0.96 and stiffness 324; the
    // track grows on one with damping 0.7 and stiffness 625 and shrinks back on one with 0.71 and 400.
    override val dynamics = PillDynamics(
        damping = 0.96f,
        stiffness = 324f,
        edges = PillEdges.Together,
        press = PressSprings(rise = spring(dampingRatio = 0.7f, stiffness = 625f), fall = spring(dampingRatio = 0.71f, stiffness = 400f)),
    )

    override fun material(style: QuvenGlassStyle): QuvenGlassStyle = style.forTracks()

    // A row's selection lifts into a lens of its own; a lone option, or a row where motion is reduced, within the glass.
    override fun lifting(lens: PillLifting.IntoLens, liquid: Boolean, reduceMotion: Boolean, count: Int): PillLifting =
        if (liquid && !reduceMotion && count > 1) lens else PillLifting.WithinGlass
}

private object SelectorFeel : QuvenGlassTrackFeel() {

    // Measured on the reference's density selector, which Quven's iOS client draws alike: a pill on a spring with response
    // 0.28 s and damping 0.62.
    override val dynamics = PillDynamics(
        damping = 0.62f,
        stiffness = 502f,
        edges = PillEdges.Together,
        press = PressSprings(spring(dampingRatio = 0.62f, stiffness = 502f)),
    )

    override fun material(style: QuvenGlassStyle): QuvenGlassStyle = style.copy(pressGrowth = 0f, pressExpansion = 0.dp)

    override fun lifting(lens: PillLifting.IntoLens, liquid: Boolean, reduceMotion: Boolean, count: Int): PillLifting = PillLifting.Flat
}
