package tv.quven.glass

import androidx.compose.runtime.Immutable

/**
 * Describes when thin glass turns light: the brightness behind it, the mean of its channels, is smoothed over
 * [settleMillis]; the glass turns light above [lightAbove] and dark again below [darkBelow].
 *
 * @property lightAbove The smoothed brightness above which thin glass turns light, from 0 to 1.
 * @property darkBelow The smoothed brightness below which light glass turns dark again, from 0 to 1.
 * @property settleMillis The time constant the brightness is smoothed over.
 * @property turnMillis The length of the turn from one appearance to the other.
 */
@Immutable
public data class QuvenGlassAdaptation(
    val lightAbove: Float = 0.74f,
    val darkBelow: Float = 0.64f,
    val settleMillis: Int = 2000,
    val turnMillis: Int = 300,
)
