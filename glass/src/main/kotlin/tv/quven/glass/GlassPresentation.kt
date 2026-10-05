package tv.quven.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A glass surface a host presents while its owner draws it, and keeps while it leaves. */
@Stable
internal abstract class GlassPresentation {

    /** Gets or sets a value indicating whether the owner still draws the surface; `false` while it leaves. */
    var standing: Boolean by mutableStateOf(true)
}

/**
 * The surface a host presents: one at a time, the latest replacing the one before.
 *
 * @param T The kind of surface.
 */
@Stable
internal class PresentationSlot<T : GlassPresentation> {

    /** Gets the surface presented or leaving, or `null` for none. */
    var shown: T? by mutableStateOf(null)
        private set

    /**
     * Presents [presentation], replacing at once any other.
     *
     * @param presentation The surface to present.
     */
    fun show(presentation: T) {
        shown = presentation
    }

    /**
     * Forgets [presentation] if it is the surface presented.
     *
     * @param presentation The surface to forget.
     */
    fun release(presentation: T) {
        if (shown === presentation) shown = null
    }
}

/**
 * Presents [presentation] in [slot] while this is composed, and marks it leaving once it no longer is.
 *
 * @param T The kind of surface.
 * @param slot The host's slot.
 * @param presentation The surface.
 */
@Composable
internal fun <T : GlassPresentation> PresentWhileComposed(slot: PresentationSlot<T>, presentation: T) {
    DisposableEffect(slot, presentation) {
        slot.show(presentation)
        onDispose { presentation.standing = false }
    }
}
