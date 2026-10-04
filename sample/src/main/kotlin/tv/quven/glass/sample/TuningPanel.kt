package tv.quven.glass.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import tv.quven.glass.QuvenGlassStyle
import tv.quven.glass.QuvenGlassSwitch
import java.util.Locale

/**
 * One tunable parameter of the material.
 *
 * @property key The name of the intent extra that sets it at launch.
 * @property name The name the panel shows.
 * @property range The values the slider spans.
 * @property read Reads the parameter from a material.
 * @property write Returns a material with the parameter set.
 */
internal class Knob(
    val key: String,
    val name: String,
    val range: ClosedFloatingPointRange<Float>,
    val read: (QuvenGlassStyle) -> Float,
    val write: QuvenGlassStyle.(Float) -> QuvenGlassStyle,
)

/** Gets the material's tunable parameters. */
internal val Knobs = listOf(
    Knob("thickLean", "Thick glass lean", 0f..1f, { it.thickTone.lean }, { copy(thickTone = thickTone.copy(lean = it)) }),
    Knob("thickSaturation", "Thick glass saturation", 0f..4f, { it.thickTone.saturation }, { copy(thickTone = thickTone.copy(saturation = it)) }),
    Knob("thinLean", "Thin glass lean", 0f..1f, { it.thinTone.lean }, { copy(thinTone = thinTone.copy(lean = it)) }),
    Knob("thinLeanSlope", "Thin lean per luminance", 0f..1f, { it.thinTone.leanSlope }, { copy(thinTone = thinTone.copy(leanSlope = it)) }),
    Knob("thinSaturation", "Thin glass saturation", 0f..4f, { it.thinTone.saturation }, { copy(thinTone = thinTone.copy(saturation = it)) }),
    Knob("thinSize", "Thin glass up to (dp)", 16f..120f, { it.thinSize.value }, { copy(thinSize = it.dp) }),
    Knob("thickSize", "Thick glass from (dp)", 16f..160f, { it.thickSize.value }, { copy(thickSize = it.dp) }),
    Knob("tintAlpha", "Tint alpha", 0f..0.8f, { it.tint.alpha }, { copy(tint = Color.Black.copy(alpha = it)) }),
    Knob("blur", "Blur (dp)", 0f..24f, { it.blur.value }, { copy(blur = it.dp) }),
    Knob("refraction", "Edge fold (dp)", 0f..60f, { it.refraction.value }, { copy(refraction = it.dp) }),
    Knob("edgeWidth", "Edge band (dp)", 1f..40f, { it.edgeWidth.value }, { copy(edgeWidth = it.dp) }),
    Knob("cornerRefraction", "Corner fold (× radius)", 0f..3f, { it.cornerRefraction }, { copy(cornerRefraction = it) }),
    Knob("cornerWidth", "Corner band (× radius)", 0f..1f, { it.cornerWidth }, { copy(cornerWidth = it) }),
    Knob("lensCurve", "Fold falloff", 1f..5f, { it.lensCurve }, { copy(lensCurve = it) }),
    Knob("dispersion", "Dispersion", 0f..1f, { it.dispersion }, { copy(dispersion = it) }),
    Knob("specular", "Rim light", 0f..2f, { it.specular }, { copy(specular = it) }),
    Knob("lightAngle", "Light angle", -180f..180f, { it.lightAngle }, { copy(lightAngle = it) }),
    Knob("platterAlpha", "Platter alpha", 0f..0.6f, { it.platter.alpha }, { copy(platter = platter.copy(alpha = it)) }),
    Knob("pressGrowth", "Press growth", 0f..0.4f, { it.pressGrowth }, { copy(pressGrowth = it) }),
    Knob("shadowAlpha", "Shadow alpha", 0f..0.6f, { it.shadow.alpha }, { copy(shadow = shadow.copy(alpha = it)) }),
    Knob("shadowRadius", "Shadow radius (dp)", 0f..40f, { it.shadowRadius.value }, { copy(shadowRadius = it.dp) }),
    Knob("slideDamping", "Spring damping", 0.2f..1f, { it.slideDamping }, { copy(slideDamping = it) }),
    Knob("slideStiffness", "Spring stiffness", 50f..1500f, { it.slideStiffness }, { copy(slideStiffness = it) }),
    Knob("groundAlpha", "Ground alpha (static)", 0f..1f, { it.ground.alpha }, { copy(ground = ground.copy(alpha = it)) }),
)

/**
 * Draws the content of the panel that tunes the material live: a slider per parameter of [QuvenGlassStyle], the bar's
 * join, and switches between Liquid Glass and the static material and for reduced motion.
 *
 * @param tuning The live settings.
 * @param modifier Modifier applied to the panel's content.
 */
@Composable
internal fun TuningPanel(tuning: SampleTuning, modifier: Modifier = Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Toggle("Liquid Glass", tuning.liquid) { tuning.liquid = it }
        Toggle("Reduce motion", tuning.reduceMotion) { tuning.reduceMotion = it }
        Setting("Bar join spacing (dp)", tuning.spacing, 0f..48f) { tuning.spacing = it }
        Setting("Bar gap (dp)", tuning.barGap, 0f..48f) { tuning.barGap = it }
        Knobs.forEach { knob ->
            Setting(knob.name, knob.read(tuning.style), knob.range) { tuning.style = knob.write(tuning.style, it) }
        }
        TextButton(onClick = { tuning.style = SampleTuning.BarStyle }) { Text("Reset", color = SampleColors.Accent) }
    }
}

/**
 * Draws a setting that is on or off: its name beside a glass switch.
 *
 * @param name The setting's name.
 * @param on Whether the setting is on.
 * @param onChange Invoked with the setting's new state.
 */
@Composable
private fun Toggle(name: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = SampleColors.TextHigh, fontSize = 15.sp, modifier = Modifier.weight(1f))
        QuvenGlassSwitch(checked = on, onCheckedChange = onChange, onColor = SampleColors.Accent)
    }
}

/**
 * Draws a setting of a value in a range: its name and value over a slider.
 *
 * @param name The setting's name.
 * @param value The setting's value.
 * @param range The values the slider spans.
 * @param onChange Invoked with the setting's new value.
 */
@Composable
private fun Setting(name: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(name, color = SampleColors.TextHigh, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(String.format(Locale.ROOT, "%.2f", value), color = SampleColors.TextMedium, fontSize = 13.sp)
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = SampleColors.Accent, activeTrackColor = SampleColors.Accent),
        )
    }
}
