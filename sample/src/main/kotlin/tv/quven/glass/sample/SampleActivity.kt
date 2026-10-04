package tv.quven.glass.sample

import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import tv.quven.glass.LocalQuvenGlassBackdrop
import tv.quven.glass.QuvenGlassMenu
import tv.quven.glass.QuvenGlassMenuMetrics
import tv.quven.glass.QuvenGlassMorph
import tv.quven.glass.QuvenGlassMorphPlacement
import tv.quven.glass.QuvenGlassStyle
import tv.quven.glass.quvenGlassSource
import tv.quven.glass.rememberQuvenGlassBackdrop
import tv.quven.glass.rememberQuvenGlassMorphState

/** Shows Liquid Glass surfaces over content that is hard for glass to stand over, with a panel tuning the material. */
class SampleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val launch = SampleLaunch(
            scroll = intent.getFloatExtra(SampleLaunch.Scroll, 0f),
            shift = intent.getFloatExtra(SampleLaunch.Shift, 0f),
            panelOpen = intent.getBooleanExtra(SampleLaunch.Panel, true),
            tab = intent.getIntExtra(SampleLaunch.Tab, 0),
            gap = intent.getFloatExtra(SampleLaunch.Gap, 8f),
            style = Knobs.fold(SampleTuning.BarStyle) { style, knob ->
                val value = intent.getFloatExtra(knob.key, Float.NaN)
                if (value.isNaN()) style else knob.write(style, value)
            },
            liquid = intent.getBooleanExtra(SampleLaunch.Liquid, true),
            menu = intent.getBooleanExtra(SampleLaunch.Menu, false),
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { SampleScreen(launch) }
        }
        val window = intent.getFloatExtra(SampleLaunch.Window, 0f)
        if (window > 0f) recordWindow(window)
    }

    // The top-left corner, where the gear's menu opens, half a pixel per dp.
    private fun recordWindow(seconds: Float) {
        recordWindowAfterLaunch(RectF(0f, 0f, RecordedWidth.value, RecordedHeight.value), RecordedPixelsPerDp, seconds)
    }
}

/**
 * The settings the sample starts with, read from the launching intent so a capture can match the iOS reference's.
 *
 * @property scroll The distance the content starts scrolled by, in density-independent pixels.
 * @property shift The distance the content stands moved to the left by, in density-independent pixels, so the content
 * under the centred bar matches the reference's on a wider screen.
 * @property panelOpen Whether the tuning panel starts open.
 * @property tab The index of the bar's held entry.
 * @property gap The space between the bar's capsule and its Search circle, in density-independent pixels.
 * @property style The material, with any parameter an intent extra named after its [Knob.key] sets.
 * @property liquid Whether the surfaces start as Liquid Glass rather than the static material.
 * @property menu Whether the gear opens the reference's system menu rather than the tuning panel.
 */
private data class SampleLaunch(
    val scroll: Float,
    val shift: Float,
    val panelOpen: Boolean,
    val tab: Int,
    val gap: Float,
    val style: QuvenGlassStyle,
    val liquid: Boolean,
    val menu: Boolean,
) {
    companion object {
        const val Scroll = "scroll"
        const val Shift = "shift"
        const val Panel = "panel"
        const val Tab = "tab"
        const val Gap = "gap"
        const val Liquid = "liquid"
        const val Menu = "menu"
        const val Window = "window"
    }
}

@Composable
private fun SampleScreen(launch: SampleLaunch) {
    val tuning = remember { SampleTuning().apply { barGap = launch.gap; style = launch.style; liquid = launch.liquid } }
    val backdrop = rememberQuvenGlassBackdrop()
    var tab by remember { mutableIntStateOf(launch.tab) }
    var density by remember { mutableIntStateOf(1) }
    var panelOpen by remember { mutableStateOf(launch.panelOpen) }
    Box(Modifier.fillMaxSize().background(SampleColors.Ground)) {
        SampleBackdropContent(
            Modifier
                .fillMaxSize()
                .quvenGlassSource(backdrop)
                .graphicsLayer { translationX = -launch.shift.dp.toPx() },
            scroll = launch.scroll,
        )
        CompositionLocalProvider(LocalQuvenGlassBackdrop provides backdrop.takeIf { tuning.liquid }) {
            val morph = rememberQuvenGlassMorphState()
            var gear by remember { mutableStateOf(Rect.Zero) }
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SampleGlassButton(
                    onClick = { panelOpen = true },
                    tuning = tuning,
                    modifier = Modifier.onGloballyPositioned { gear = it.boundsInRoot() },
                    diameter = ButtonSide,
                    shown = !morph.isShown,
                ) { ink -> GlyphFace(Icons.Filled.Settings, contentDescription = "Tune", tint = ink) }
                SampleDensityTrack(held = density, onHold = { density = it }, tuning = tuning)
            }
            if (panelOpen) {
                Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { panelOpen = false })
            }
            QuvenGlassMorph(
                state = morph,
                expanded = panelOpen,
                anchor = gear,
                width = if (launch.menu) QuvenGlassMenuMetrics.Tablet.width else PanelWidth,
                placement = QuvenGlassMorphPlacement.hangingFromTopLeft(edge = 16.dp),
                modifier = Modifier.fillMaxSize(),
                style = if (launch.menu) tuning.style.forMenus() else tuning.style,
                cornerRadius = if (launch.menu) QuvenGlassMenuMetrics.Tablet.cornerRadius else 28.dp,
                reduceMotion = tuning.reduceMotion,
                face = { GlyphFace(Icons.Filled.Settings, contentDescription = null) },
            ) {
                if (launch.menu) {
                    QuvenGlassMenu(metrics = QuvenGlassMenuMetrics.Tablet, onChosen = { panelOpen = false }) { LibraryMenuEntries() }
                } else {
                    TuningPanel(tuning, Modifier.heightIn(max = PanelMaxHeight))
                }
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
            ) {
                SampleBar(held = tab, onHold = { tab = it }, tuning = tuning)
            }
        }
    }
}

private val ButtonSide = 69.dp
private val RecordedWidth = 330.dp
private val RecordedHeight = 520.dp
private const val RecordedPixelsPerDp = 0.5f
private val PanelWidth = 340.dp
private val PanelMaxHeight = 560.dp
