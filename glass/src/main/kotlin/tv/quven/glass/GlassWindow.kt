package tv.quven.glass

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Returns whether a window [width] by [height] pixels is a tablet's, at least 600 dp each way, where Apple lays its
 * controls out as on an iPad.
 *
 * @param width The window's width, in pixels.
 * @param height The window's height, in pixels.
 * @return `true` for a tablet's window.
 */
internal fun Density.isTabletWindow(width: Float, height: Float): Boolean = minOf(width, height) >= TabletWindowSide.toPx()

private val TabletWindowSide = 600.dp
