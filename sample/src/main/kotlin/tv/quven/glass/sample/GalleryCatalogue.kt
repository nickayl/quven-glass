package tv.quven.glass.sample

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * How far along the library is with one of Apple's Liquid Glass elements.
 *
 * @property label The name the gallery shows for the status.
 * @property color The colour the gallery marks the status with.
 */
internal enum class ExhibitStatus(val label: String, val color: Color) {
    /** The library draws the element. */
    Ready("Ready", Color(0xFF34C759)),

    /** The element is being built. */
    InDevelopment("In development", Color(0xFFFF9F0A)),

    /** The element waits on a decision before it is built. */
    Planned("Planned", Color(0xFF8E8E93)),
}

/**
 * One of Apple's Liquid Glass elements, as the gallery shows it. The iOS reference lists the same exhibits, under the
 * same titles and in the same order, and draws Apple's own element for each.
 *
 * @property title The element's name.
 * @property summary What the element is and does.
 * @property status How far along the library is with the element.
 * @property stage Draws the library's element over the stage, given the live material; only a ready exhibit has one.
 * @throws IllegalArgumentException A ready exhibit has no stage, or another one has a stage.
 */
@Immutable
internal class Exhibit(
    val title: String,
    val summary: String,
    val status: ExhibitStatus,
    val stage: (@Composable BoxScope.(SampleTuning) -> Unit)? = null,
) {
    init {
        require((stage != null) == (status == ExhibitStatus.Ready)) { "Exactly the ready exhibits have a stage." }
    }
}

/** The exhibits, in the order the gallery lists them. */
internal val Exhibits: List<Exhibit> = listOf(
    Exhibit(
        "Material",
        "Glass bends the content at its rim, blurs and tones it, and catches the light on its edge. Up to 63 dp it is " +
            "thin glass, which turns light over a bright page; from 66 dp it is thick glass.",
        ExhibitStatus.Ready,
    ) { MaterialExhibit(it) },
    Exhibit(
        "Glass buttons",
        "Round controls of glass that swell and light the content under them while pressed.",
        ExhibitStatus.Ready,
    ) { GlassButtonsExhibit(it) },
    Exhibit(
        "Tab bar",
        "Entries in a capsule beside a Search circle. The held pill slides with a stretch, a press lifts it into a lens " +
            "and a drag carries it to the entry let go over.",
        ExhibitStatus.Ready,
    ) { TabBarExhibit(it) },
    Exhibit(
        "Segmented control",
        "Options on a glass track whose pill slides, stretches and lifts as the tab bar's does.",
        ExhibitStatus.Ready,
    ) { SegmentedExhibit(it) },
    Exhibit(
        "Joining glass",
        "Surfaces closer than their container's spacing flow into one piece of glass, and part again as they move apart.",
        ExhibitStatus.Ready,
    ) { JoiningExhibit(it) },
    Exhibit(
        "Menus",
        "Menus grow out of their control: titles, glyphs, choices, disabled and destructive entries, and plain menus. " +
            "A menu with more room above rises, its entries reversed. Slide a finger along the rows to choose.",
        ExhibitStatus.Ready,
    ) { MenusExhibit(it) },
    Exhibit(
        "Morphing panel",
        "A control opens into a panel of any content as one piece of glass and closes back into it. This panel tunes " +
            "the material of every exhibit.",
        ExhibitStatus.Ready,
    ) { MorphingPanelExhibit(it) },
    Exhibit(
        "Clear and tinted glass",
        "The clear variant, which lets bright media through, and glass tinted with a colour.",
        ExhibitStatus.Ready,
    ) { ClearAndTintedExhibit(it) },
    Exhibit(
        "Capsule buttons",
        "Buttons of clear glass and of prominent, tinted glass, holding a name, a glyph or both.",
        ExhibitStatus.Ready,
    ) { CapsuleButtonsExhibit(it) },
    Exhibit(
        "Switch",
        "A switch whose thumb lifts into a lens of glass while it is held or dragged.",
        ExhibitStatus.Ready,
    ) { SwitchExhibit(it) },
    Exhibit(
        "Slider",
        "A slider whose thumb lifts into a lens of glass while it is dragged.",
        ExhibitStatus.Ready,
    ) { SliderExhibit(it) },
    Exhibit(
        "Context menu",
        "A long press lifts the card out of the page, dims the rest and opens the card's menu beside it.",
        ExhibitStatus.Ready,
    ) { ContextMenuExhibit(it) },
    Exhibit(
        "Submenus",
        "An entry that opens a second menu at the first one's side, and entries that carry a second line.",
        ExhibitStatus.InDevelopment,
    ),
    Exhibit(
        "Toolbar",
        "Glass buttons along the top of a page, grouped into capsules that join and part.",
        ExhibitStatus.InDevelopment,
    ),
    Exhibit(
        "Sheet",
        "A sheet of glass that rises from the bottom edge and turns opaque as it is drawn to full height.",
        ExhibitStatus.Ready,
    ) { SheetExhibit(it) },
    Exhibit(
        "Alert",
        "A dialog on glass over a dimmed page, its buttons capsules.",
        ExhibitStatus.Ready,
    ) { AlertExhibit(it) },
    Exhibit(
        "Popover",
        "A panel of glass that grows out of the control it belongs to and points at it.",
        ExhibitStatus.Ready,
    ) { PopoverExhibit(it) },
    Exhibit(
        "Search",
        "The Search circle opens into a field of glass along the bar.",
        ExhibitStatus.Ready,
    ) { SearchExhibit(it) },
    Exhibit(
        "Minimizing tab bar",
        "On a phone the tab bar shrinks to its held entry while the content scrolls down, and grows back as it scrolls up.",
        ExhibitStatus.InDevelopment,
    ),
    Exhibit(
        "Bottom accessory",
        "A strip of glass above the tab bar, such as a player's controls, that shrinks with the bar.",
        ExhibitStatus.InDevelopment,
    ),
    Exhibit(
        "Scroll edge",
        "Content fades and blurs as it passes under the bars at the top and bottom of a page.",
        ExhibitStatus.InDevelopment,
    ),
    Exhibit(
        "Touch light",
        "Light that gathers under the finger and follows it across interactive glass.",
        ExhibitStatus.InDevelopment,
    ),
    Exhibit(
        "Text menu",
        "The menu of cut, copy and paste on glass, over selected text.",
        ExhibitStatus.InDevelopment,
    ),
    Exhibit(
        "Adaptive sidebar",
        "A sidebar of glass floating over the content, which becomes a tab bar in a narrow window.",
        ExhibitStatus.Planned,
    ),
)
