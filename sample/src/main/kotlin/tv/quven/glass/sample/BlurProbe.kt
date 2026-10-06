package tv.quven.glass.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tv.quven.glass.LocalQuvenGlassSheetHost
import tv.quven.glass.QuvenGlassSheet
import tv.quven.glass.QuvenGlassSheetDetent
import tv.quven.glass.QuvenGlassSheetHost
import tv.quven.glass.QuvenGlassStyle
import tv.quven.glass.quvenGlassSource
import tv.quven.glass.quvenLiquidGlass
import tv.quven.glass.rememberQuvenGlassBackdrop
import tv.quven.glass.rememberQuvenGlassSheetHostState

/**
 * A piece of the blur probe: its frame in the window and its corner radius, half its height for a capsule.
 *
 * @property x The left edge.
 * @property y The top edge.
 * @property width The width.
 * @property height The height.
 * @property radius The corner radius.
 */
private data class ProbePiece(val x: Dp, val y: Dp, val width: Dp, val height: Dp, val radius: Dp)

// The iOS reference's pieces, each centred on a corner of the cells as there, laid out within the tablet's window.
private val ProbePieces = listOf(
    ProbePiece(58.dp, 98.dp, 44.dp, 44.dp, 22.dp),
    ProbePiece(132.dp, 92.dp, 56.dp, 56.dp, 28.dp),
    ProbePiece(244.dp, 84.dp, 72.dp, 72.dp, 36.dp),
    ProbePiece(350.dp, 70.dp, 100.dp, 100.dp, 50.dp),
    ProbePiece(490.dp, 50.dp, 140.dp, 140.dp, 70.dp),
    ProbePiece(700.dp, 20.dp, 200.dp, 200.dp, 100.dp),
    ProbePiece(30.dp, 248.dp, 340.dp, 224.dp, 28.dp),
    ProbePiece(410.dp, 300.dp, 300.dp, 120.dp, 28.dp),
    ProbePiece(410.dp, 452.dp, 300.dp, 56.dp, 28.dp),
    ProbePiece(755.dp, 240.dp, 250.dp, 400.dp, 28.dp),
    ProbePiece(60.dp, 500.dp, 120.dp, 200.dp, 28.dp),
    ProbePiece(960.dp, 40.dp, 160.dp, 160.dp, 28.dp),
    ProbePiece(1040.dp, 580.dp, 80.dp, 120.dp, 28.dp),
)

// Capsules of thin glass, each along a row of the palette's cells, three to a row over six rows, as the reference's.
private val ThinProbePieces = listOf(80, 200, 320, 440, 560, 680).flatMap { y ->
    listOf(200, 600, 960).map { x -> ProbePiece((x - 150).dp, (y - 28).dp, 300.dp, 56.dp, 28.dp) }
}

// A system menu's panel where the reference's menu probe opens it, so the menu's blur is read over the same cells.
private val MenuProbePiece = ProbePiece(72.dp, 72.dp, 223.5.dp, 252.dp, 34.dp)

/** The side of the probe's checkerboard cells. */
private val ProbeCell = 40.dp

/** The colours of the palette's cells, as the iOS reference's: five greys, then red, green and blue. */
private val ProbePalette = listOf(
    Color(0xFF000000), Color(0xFF404040), Color(0xFF808080), Color(0xFFC0C0C0), Color(0xFFFFFFFF),
    Color(0xFFFF3B30), Color(0xFF34C759), Color(0xFF007AFF),
)

/**
 * Draws glass of several sizes and shapes over a checkerboard of 40 dp cells, as the iOS reference's blur probe does,
 * so the blur under each is read against the checkerboard captured bare.
 *
 * @param bare Whether to leave the glass out.
 * @param palette Whether to draw the cells in [ProbePalette], each piece centred on a cell, so the tone the glass gives
 * every colour is read at the cells' centres.
 * @param scale The factor the palette's colours are scaled by, so the tone is read over a darker backdrop as well.
 * @param thin Whether to draw capsules of thin glass along the palette's rows in place of pieces of every size.
 * @param menu Whether to draw a menu's panel in its material in place of pieces of every size.
 * @param sheet Whether to raise an empty sheet at its medium height in place of pieces of every size.
 */
@Composable
internal fun BlurProbe(bare: Boolean, palette: Boolean, scale: Float, thin: Boolean, menu: Boolean, sheet: Boolean) {
    val backdrop = rememberQuvenGlassBackdrop()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize().quvenGlassSource(backdrop)) {
            val cell = ProbeCell.toPx()
            for (row in 0..(size.height / cell).toInt() + 1) {
                for (column in 0..(size.width / cell).toInt() + 1) {
                    if (palette) {
                        // Along a row the colour steps by one, down a column by three.
                        val base = ProbePalette[(row * 3 + column) % ProbePalette.size]
                        val colour = Color(base.red * scale, base.green * scale, base.blue * scale)
                        drawRect(colour, Offset(column * cell - cell / 2, row * cell - cell / 2), Size(cell, cell))
                    } else if ((row + column) % 2 == 0) {
                        drawRect(Color.White, Offset(column * cell, row * cell), Size(cell, cell))
                    }
                }
            }
        }
        if (sheet && !bare) {
            val sheets = rememberQuvenGlassSheetHostState()
            CompositionLocalProvider(LocalQuvenGlassSheetHost provides sheets) {
                QuvenGlassSheet(onDismissRequest = {}, detents = listOf(QuvenGlassSheetDetent.Medium)) {}
            }
            QuvenGlassSheetHost(sheets, backdrop = backdrop)
        } else if (!bare) {
            val style = if (menu) QuvenGlassStyle.Standard.forMenus() else QuvenGlassStyle.Standard
            val pieces = when {
                menu -> listOf(MenuProbePiece)
                thin -> ThinProbePieces
                else -> ProbePieces
            }
            pieces.forEach { piece ->
                Box(
                    Modifier
                        .offset(piece.x, piece.y)
                        .size(piece.width, piece.height)
                        .quvenLiquidGlass(backdrop, style, RoundedCornerShape(piece.radius)),
                )
            }
        }
    }
}
