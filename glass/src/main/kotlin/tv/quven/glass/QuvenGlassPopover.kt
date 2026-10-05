package tv.quven.glass

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Opens a popover out of the control [state] marks with [quvenGlassAnchor], in the [QuvenGlassMenuHost] the screen
 * provides through [LocalQuvenGlassMenuHost], as Apple's popovers open: a drop of glass at the control's edge grows into
 * a panel of [content] above or below it, centred on it and pointing at it, and shrinks back into the drop on close.
 * The control stays where it stands; a press elsewhere or Back invokes [onDismissRequest].
 *
 * @param state The opening, whose control the popover points at.
 * @param expanded Whether the popover is open.
 * @param onDismissRequest Invoked when the viewer closes the popover.
 * @param modifier Modifier applied to the popover's panel, such as its padding.
 * @param content The popover's panel.
 * @throws IllegalStateException No [QuvenGlassMenuHost] is provided above the popover.
 */
@Composable
public fun QuvenGlassPopover(
    state: QuvenGlassMorphState,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassDropdown(
        state = state,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        outsideModifier = Modifier,
        placement = PopoverPlacement,
        face = {},
        preview = null,
        popover = true,
        content = content,
    )
}

/** Where a popover stands: beyond its control on the side with more room, centred on it. */
private val PopoverPlacement = QuvenGlassMorphPlacement.aboveOrBelowCentred(gap = PopoverGap, edge = MenuEdge)
