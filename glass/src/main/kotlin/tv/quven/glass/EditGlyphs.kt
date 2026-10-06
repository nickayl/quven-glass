package tv.quven.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The glyphs an expanded edit menu gives the clipboard's and the selection's own actions, after the system's symbols. */
internal object EditGlyphs {

    // Declared before the glyphs, which read them as the object initialises.
    private val Size = 24.dp
    private const val Viewport = 24f
    private const val Stroke = 1.6f

    /** Gets the glyph of cutting, as the `scissors` symbol draws it. */
    val Cut: ImageVector = glyph(
        "Cut",
        "M6.5 9.5a2.5 2.5 0 1 1 0 -5a2.5 2.5 0 1 1 0 5z M6.5 19.5a2.5 2.5 0 1 1 0 -5a2.5 2.5 0 1 1 0 5z " +
            "M8.6 8.4L20 15.5 M8.6 15.6L20 8.5",
    )

    /** Gets the glyph of copying, as the `doc.on.doc` symbol draws it. */
    val Copy: ImageVector = glyph(
        "Copy",
        "M9 6V5a2 2 0 0 1 2 -2h7a2 2 0 0 1 2 2v9a2 2 0 0 1 -2 2h-1 " +
            "M4 9a2 2 0 0 1 2 -2h7a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-7a2 2 0 0 1 -2 -2z",
    )

    /** Gets the glyph of pasting, as the `doc.on.clipboard` symbol draws it. */
    val Paste: ImageVector = glyph(
        "Paste",
        "M9 4.5H7a2 2 0 0 0 -2 2v12.5a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-12.5a2 2 0 0 0 -2 -2h-2 " +
            "M9.5 3h5a0.5 0.5 0 0 1 0.5 0.5v2a0.5 0.5 0 0 1 -0.5 0.5h-5a0.5 0.5 0 0 1 -0.5 -0.5v-2a0.5 0.5 0 0 1 0.5 -0.5z",
    )

    /** Gets the glyph of selecting everything, as the `selection.pin.in.out` symbol frames a selection. */
    val SelectAll: ImageVector = glyph(
        "SelectAll",
        "M4 8V5a1 1 0 0 1 1 -1h3 M16 4h3a1 1 0 0 1 1 1v3 M20 16v3a1 1 0 0 1 -1 1h-3 M8 20H5a1 1 0 0 1 -1 -1v-3 " +
            "M9 12h6",
    )

    /** Gets the glyph of filling in automatically, as the `rectangle.and.pencil.and.ellipsis` symbol draws it. */
    val Autofill: ImageVector = glyph(
        "Autofill",
        "M13 8H5a2 2 0 0 0 -2 2v6a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2 M6.5 13h0.01 M9.5 13h0.01 M12.5 13h0.01 " +
            "M15 13l6 -6",
    )

    private fun glyph(name: String, path: String): ImageVector = ImageVector.Builder(name, Size, Size, Viewport, Viewport).apply {
        addPath(
            PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = Stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}
