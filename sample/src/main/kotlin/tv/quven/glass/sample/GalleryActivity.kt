package tv.quven.glass.sample

import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.quven.glass.LocalQuvenGlassAlertHost
import tv.quven.glass.LocalQuvenGlassBackdrop
import tv.quven.glass.LocalQuvenGlassMenuHost
import tv.quven.glass.QuvenGlassAlertHost
import tv.quven.glass.QuvenGlassMenuHost
import tv.quven.glass.QuvenGlassMenuMetrics
import tv.quven.glass.quvenGlassSource
import tv.quven.glass.rememberQuvenGlassAlertHostState
import tv.quven.glass.rememberQuvenGlassBackdrop
import tv.quven.glass.rememberQuvenGlassMenuHostState
import androidx.compose.ui.unit.DpSize

/** Shows every Liquid Glass element Apple draws, the library's own where it draws one, each over hard content. */
class GalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The gallery is dark whatever the system's theme, so its bars carry light glyphs.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        // A capture opens the gallery on the exhibit named and copies the region named on every frame.
        val exhibit = intent.getStringExtra(ExtraExhibit)?.let { title -> Exhibits.indexOfFirst { it.title == title }.takeIf { it >= 0 } }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { GalleryScreen(exhibit) }
        }
        val seconds = intent.getFloatExtra(ExtraWindow, 0f)
        val region = intent.getStringExtra(ExtraRegion)?.split(',')?.mapNotNull(String::toFloatOrNull)
        if (seconds > 0f && region?.size == 4) {
            recordWindowAfterLaunch(RectF(region[0], region[1], region[0] + region[2], region[1] + region[3]), CapturePixelsPerDp, seconds)
        }
    }

    private companion object {
        // Intent extras: the exhibit's title, the seconds to record for, and the region as x,y,width,height in dp.
        const val ExtraExhibit = "exhibit"
        const val ExtraWindow = "window"
        const val ExtraRegion = "region"

        // Two pixels per dp, as the iOS simulator's frames keep two per point.
        const val CapturePixelsPerDp = 2f
    }
}

/**
 * Lists the exhibits beside the one chosen; a narrow window shows the list, then the exhibit chosen from it. An
 * exhibit's alert stands over the whole window, which it dims, as the system's does.
 *
 * @param initial The index of the exhibit to open on, or `null` to open on the first and, in a narrow window, the list.
 */
@Composable
private fun GalleryScreen(initial: Int?) {
    val tuning = remember { SampleTuning() }
    val screen = rememberQuvenGlassBackdrop()
    val alerts = rememberQuvenGlassAlertHostState()
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalQuvenGlassAlertHost provides alerts) {
            GalleryPanes(initial, tuning, Modifier.fillMaxSize().quvenGlassSource(screen))
        }
        QuvenGlassAlertHost(alerts, backdrop = screen.takeIf { tuning.liquid }, reduceMotion = tuning.reduceMotion)
    }
}

/**
 * Draws the list of exhibits beside the one chosen, or one of the two in a narrow window.
 *
 * @param initial The index of the exhibit to open on, or `null` to open on the first and, in a narrow window, the list.
 * @param tuning The live settings.
 * @param modifier Modifier applied to the panes.
 */
@Composable
private fun GalleryPanes(initial: Int?, tuning: SampleTuning, modifier: Modifier) {
    var chosen by rememberSaveable { mutableIntStateOf(initial ?: 0) }
    var opened by rememberSaveable { mutableStateOf(initial != null) }
    BoxWithConstraints(
        modifier
            .background(SampleColors.Ground)
            .safeDrawingPadding(),
    ) {
        when {
            maxWidth >= TwoPaneWidth -> Row(Modifier.fillMaxSize()) {
                ExhibitList(chosen, onChoose = { chosen = it }, Modifier.width(ListWidth).fillMaxHeight())
                ExhibitPage(Exhibits[chosen], tuning, Modifier.weight(1f).fillMaxHeight())
            }
            opened -> {
                BackHandler { opened = false }
                ExhibitPage(Exhibits[chosen], tuning, Modifier.fillMaxSize())
            }
            else -> ExhibitList(chosen, onChoose = { chosen = it; opened = true }, Modifier.fillMaxSize())
        }
    }
}

/**
 * Lists the exhibits grouped by status, the chosen one marked.
 *
 * @param chosen The index of the chosen exhibit.
 * @param onChoose Invoked with the index of a pressed exhibit.
 * @param modifier Modifier applied to the list.
 */
@Composable
private fun ExhibitList(chosen: Int, onChoose: (Int) -> Unit, modifier: Modifier) {
    val ready = Exhibits.count { it.status == ExhibitStatus.Ready }
    LazyColumn(modifier, contentPadding = PaddingValues(ListPadding)) {
        item {
            Column(Modifier.padding(start = RowPadding, bottom = 8.dp)) {
                Text("Liquid Glass", color = SampleColors.TextHigh, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("$ready of ${Exhibits.size} elements ready", color = SampleColors.TextMedium, fontSize = 15.sp)
            }
        }
        ExhibitStatus.entries.forEach { status ->
            val group = Exhibits.withIndex().filter { it.value.status == status }
            if (group.isEmpty()) return@forEach
            item(key = status) {
                Text(
                    status.label.uppercase(),
                    color = SampleColors.TextMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = RowPadding, top = 20.dp, bottom = 6.dp),
                )
            }
            items(group, key = { it.value.title }) { (index, exhibit) ->
                ExhibitRow(exhibit, chosen = index == chosen, onClick = { onChoose(index) })
            }
        }
    }
}

/**
 * Draws one exhibit's row: its status mark and its name.
 *
 * @param exhibit The exhibit.
 * @param chosen Whether the exhibit is the one shown.
 * @param onClick Invoked when the row is pressed.
 */
@Composable
private fun ExhibitRow(exhibit: Exhibit, chosen: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(if (chosen) ChosenRow else Ground)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = RowPadding, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(exhibit.status.color))
        Text(exhibit.title, color = SampleColors.TextHigh, fontSize = 16.sp, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/**
 * Draws an exhibit: its name, status and summary over the stage where it stands.
 *
 * @param exhibit The exhibit.
 * @param tuning The live settings.
 * @param modifier Modifier applied to the page.
 */
@Composable
private fun ExhibitPage(exhibit: Exhibit, tuning: SampleTuning, modifier: Modifier) {
    Column(modifier.padding(PagePadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(exhibit.title, color = SampleColors.TextHigh, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                exhibit.status.label,
                color = exhibit.status.color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(exhibit.status.color.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(exhibit.summary, color = SampleColors.TextMedium, fontSize = 15.sp)
        // The stage is the iOS reference's size, so an exhibit stands over the same content on both.
        key(exhibit) { ExhibitStage(exhibit, tuning, Modifier.padding(top = 6.dp).size(StageSize)) }
    }
}

/**
 * Draws the stage: content that is hard for glass to stand over, which scrolls under the exhibit and its menus.
 *
 * @param exhibit The exhibit.
 * @param tuning The live settings.
 * @param modifier Modifier applied to the stage.
 */
@Composable
private fun ExhibitStage(exhibit: Exhibit, tuning: SampleTuning, modifier: Modifier) {
    val backdrop = rememberQuvenGlassBackdrop()
    val menus = rememberQuvenGlassMenuHostState()
    val metrics = if (LocalConfiguration.current.smallestScreenWidthDp >= TabletWidthDp) QuvenGlassMenuMetrics.Tablet else QuvenGlassMenuMetrics.Phone
    Box(modifier.clip(StageShape)) {
        SampleBackdropContent(Modifier.fillMaxSize().quvenGlassSource(backdrop))
        CompositionLocalProvider(
            LocalQuvenGlassBackdrop provides backdrop.takeIf { tuning.liquid },
            LocalQuvenGlassMenuHost provides menus,
        ) {
            Box(Modifier.fillMaxSize().padding(StageInset)) {
                val stage = exhibit.stage
                if (stage != null) stage(tuning) else PendingExhibitCard(exhibit, tuning, Modifier.align(Alignment.Center))
            }
            QuvenGlassMenuHost(
                state = menus,
                style = tuning.style.forMenus(),
                metrics = metrics,
                reduceMotion = tuning.reduceMotion,
            )
        }
    }
}

private val TwoPaneWidth = 700.dp
private val ListWidth = 300.dp
private val ListPadding = 12.dp
private val RowPadding = 12.dp
private val RowShape = RoundedCornerShape(12.dp)
private val ChosenRow = SampleColors.TextHigh.copy(alpha = 0.12f)
private val Ground = SampleColors.Ground
private val PagePadding = 16.dp
private val StageShape = RoundedCornerShape(24.dp)
private val StageSize = DpSize(400.dp, 600.dp)
private val StageInset = 24.dp
private const val TabletWidthDp = 600
