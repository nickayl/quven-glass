package tv.quven.glass

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * What a finger does on a glass menu: the rows it can reach, how many of them are pressed, the row a sliding finger
 * lights, and whether the menu is taller than its room and scrolls.
 *
 * @param scroll The menu's scrolling.
 */
internal class MenuTouch(private val scroll: ScrollState) {

    /** Gets or sets the number of rows pressed now. */
    var pressedRows: Int by mutableIntStateOf(0)

    /** Gets or sets the row a sliding finger lights, or `null` for none. */
    var scrubbed: MenuRow? by mutableStateOf(null)

    /** Gets or sets a value indicating whether a finger slides along the menu. */
    var isScrubbing: Boolean by mutableStateOf(false)

    /** Gets or sets where the menu stands, or `null` before it is placed. */
    var menu: LayoutCoordinates? = null

    /** Gets the rows the menu holds, in the order they joined it. */
    val rows: MutableList<MenuRow> = mutableListOf()

    /** Gets a value indicating whether the menu is taller than its room and scrolls. */
    val scrolls: Boolean
        get() = scroll.maxValue > 0

    /** Gets a value indicating whether a row is pressed or a finger slides along the menu. */
    val isTouched: Boolean
        get() = pressedRows > 0 || isScrubbing

    /**
     * Returns the row under [position] that can be chosen.
     *
     * @param position A point in the menu's coordinates.
     * @return The row, or `null` where none stands or the one there is disabled.
     */
    fun rowAt(position: Offset): MenuRow? {
        val menu = menu?.takeIf { it.isAttached } ?: return null
        return rows.firstOrNull { row ->
            row.enabled && row.coordinates?.takeIf { it.isAttached }
                ?.let { menu.localBoundingBoxOf(it, clipBounds = false).contains(position) } == true
        }
    }
}

/**
 * A row of a glass menu: where it stands, whether it can be chosen and what choosing it does.
 *
 * @property action Chooses the row.
 */
internal class MenuRow(val action: () -> Unit) {

    /** Gets or sets where the row stands, or `null` before it is placed. */
    var coordinates: LayoutCoordinates? = null

    /** Gets or sets a value indicating whether the row can be chosen. */
    var enabled: Boolean = true
}

/**
 * How many of a menu's entries carry a check or a glyph and how many sections a title, which set where the names stand
 * and the order the entries are listed in.
 */
internal class MenuContents {

    /** Gets or sets the number of choices the menu holds. */
    var choices: Int by mutableIntStateOf(0)

    /** Gets or sets the number of rows that carry a glyph. */
    var glyphs: Int by mutableIntStateOf(0)

    /** Gets or sets the number of sections' titles. */
    var titles: Int by mutableIntStateOf(0)
}

/**
 * Follows a finger on the menu: once it slides past the touch slop the menu takes the gesture from its rows, lights
 * the row under the finger, ticks as it reaches another and chooses the row it lifts over. A menu that scrolls leaves
 * the gesture to its scrolling, as a system menu does.
 *
 * @param touch The menu the finger is on.
 * @param haptics The feedback that ticks.
 */
internal suspend fun PointerInputScope.scrubRows(touch: MenuTouch, haptics: HapticFeedback) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    if (touch.scrolls) return@awaitEachGesture
    var reached = touch.rowAt(down.position)
    try {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
            if (!touch.isScrubbing && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                touch.isScrubbing = true
            }
            if (!change.pressed) {
                if (touch.isScrubbing) {
                    change.consume()
                    touch.rowAt(change.position)?.action?.invoke()
                }
                break
            }
            if (touch.isScrubbing) {
                change.consume()
                val under = touch.rowAt(change.position)
                if (under != null && under !== reached) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    reached = under
                }
                touch.scrubbed = under
            }
        }
    } finally {
        touch.isScrubbing = false
        touch.scrubbed = null
    }
}
