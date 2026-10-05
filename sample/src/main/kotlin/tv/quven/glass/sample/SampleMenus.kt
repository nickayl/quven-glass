package tv.quven.glass.sample

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import tv.quven.glass.QuvenGlassMenuDivider
import tv.quven.glass.QuvenGlassMenuItem
import tv.quven.glass.QuvenGlassMenuTitle

/** Draws the entries of the reference's gear menu, entry for entry: two titled sections of glyphed rows. */
@Composable
internal fun ColumnScope.LibraryMenuEntries() {
    QuvenGlassMenuTitle("Library")
    QuvenGlassMenuItem("Collections", {}, icon = rememberVectorPainter(PlayStack))
    QuvenGlassMenuItem("Saved", {}, icon = rememberVectorPainter(BookmarkFill))
    QuvenGlassMenuItem("Bookshelf", {}, icon = rememberVectorPainter(BooksVertical))
    QuvenGlassMenuDivider()
    QuvenGlassMenuTitle("Account")
    QuvenGlassMenuItem("Profile", {}, icon = rememberVectorPainter(Icons.Filled.Person))
    QuvenGlassMenuItem("Sync", {}, icon = rememberVectorPainter(Icons.Filled.Refresh))
    QuvenGlassMenuItem("Switch view", {}, icon = rememberVectorPainter(SwapFrames))
    QuvenGlassMenuItem("Settings", {}, icon = rememberVectorPainter(Icons.Filled.Settings))
    QuvenGlassMenuItem("Sign out", {}, icon = rememberVectorPainter(Icons.AutoMirrored.Filled.ExitToApp), destructive = true)
}
