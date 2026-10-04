package tv.quven.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/** The most surfaces one glass layer joins; a container holding more draws the first of them. */
internal const val MaxGlassSurfaces = 4

/**
 * The uniforms of [LiquidGlassShaderSource], each with the number of floats it takes.
 *
 * @property uniform The uniform's name.
 * @property floats The number of floats the uniform takes; zero for an integer.
 */
internal enum class LiquidGlassUniform(val uniform: String, val floats: Int) {
    ShapeRect("shapeRect", 4 * MaxGlassSurfaces),
    ShapeRadii("shapeRadii", 4 * MaxGlassSurfaces),
    ShapeLift("shapeLift", MaxGlassSurfaces),
    ShapeLight("shapeLight", MaxGlassSurfaces),
    ShapeGlow("shapeGlow", MaxGlassSurfaces),
    PillRect("pillRect", 4 * MaxGlassSurfaces),
    PillLook("pillLook", 4 * MaxGlassSurfaces),
    ShapeCount("shapeCount", 0),
    Blend("blend", 1),
    Optics("optics", 4),
    Lens("lens", 4),
    Detail("detail", 2),
    ThinTone("thinTone", 4),
    ThickTone("thickTone", 4),
    Leans("leans", 4),
    ToneSizes("toneSizes", 2),
    LightTone("lightTone", 4),
    LightLean("lightLean", 2),
    Tint("tint", 4),
    Platter("platter", 4),
    LightPlatter("lightPlatter", 4),
    PressGlow("pressGlow", 1),
    RimGlow("rimGlow", 1),
    Brighten("brighten", 1),
    Zoom("zoom", 1),
    SeeThrough("seeThrough", 1),
    BackdropDim("backdropDim", 1),
    PressTintGlow("pressTintGlow", 1),
    RimLight("rimLight", 1),
    PressLighten("pressLighten", 1),
}

/** The name of the shader uniform the backdrop is bound to. */
internal const val LiquidGlassContent = "content"

/**
 * The AGSL program of Liquid Glass over the blurred backdrop: up to [MaxGlassSurfaces] rounded rectangles joined by a
 * smooth union, each with an optional pill inside, which a press swells out of its surface.
 *
 * Within a band inside the edge, a pixel at depth `δ` shows the backdrop `reach × (1 − δ / band)^lens.x` further in;
 * `reach` and `band` are `optics.z` and `optics.w` times the corner radius, up to `CORNER_SCALE_LIMIT_DP`, no less than `optics.x` and `optics.y`. Red
 * and blue part by `detail.x` of the fold; `detail.y` is the pixels in a density-independent pixel.
 *
 * The thickness of the nearest surface runs from thin to thick as its shorter side runs from `toneSizes.x` to
 * `toneSizes.y`; thin glass leans towards `thinTone.rgb` by `leans.z + leans.w × luminance`, thick glass towards
 * `thickTone.rgb` by `leans.x + leans.y × luminance`, and the saturation scales by the tone's alpha; thin glass turned
 * light by `shapeLight` takes `lightTone`, `lightLean` and `lightPlatter` instead. Last come the rim's light,
 * brighter where it faces the light, the tint over it, and the pill's platter, which clears into a lens while the pill
 * is lifted. Where `pressGlow` is positive, a surface lit by `shapeGlow` turns towards the untoned backdrop lit
 * `pressGlow` times, and where `pressLighten` is, it turns towards white by `pressLighten` times the mean luminance of the
 * backdrop under it, the whole surface alike, in place of the light a lifted surface otherwise gains;
 * where `rimGlow` is, the rim shows the backdrop just outside it lit `rimGlow` times.
 *
 * Inside each surface the backdrop is read `zoom` times further from the surface's centre, so a `zoom` above 1 shows it
 * smaller. Where `seeThrough` is 1 the backdrop's own alpha holds: the glass covers what lies under it only where the
 * backdrop has content, and elsewhere lays the rim's light and a veil of white over it, `brighten` times
 * `SEE_THROUGH_VEIL`, as clear glass lightens a page it does not bend. Every read of the backdrop sees it darkened by
 * `backdropDim`, as glass over a dimmed screen reads it. A lit tinted surface brightens its tint `pressTintGlow`
 * times, and the rim turns `rimLight` of the way to white all the way round.
 */
internal const val LiquidGlassShaderSource: String = """
uniform shader content;
uniform float4 shapeRect[4];
uniform float4 shapeRadii[4];
uniform float shapeLift[4];
uniform float shapeLight[4];
uniform float shapeGlow[4];
uniform float4 pillRect[4];
uniform float4 pillLook[4];
uniform int shapeCount;
uniform float blend;
uniform float4 optics;
uniform float4 lens;
uniform float2 detail;
uniform float4 thinTone;
uniform float4 thickTone;
uniform float4 leans;
uniform float2 toneSizes;
uniform float4 lightTone;
uniform float2 lightLean;
uniform float4 tint;
uniform float4 platter;
uniform float4 lightPlatter;
uniform float pressGlow;
uniform float rimGlow;
uniform float brighten;
uniform float zoom;
uniform float seeThrough;
uniform float backdropDim;
uniform float pressTintGlow;
uniform float rimLight;
uniform float pressLighten;

const float FAR = 100000.0;
const float EPSILON = 0.0001;
const float3 LUMA = float3(0.2126, 0.7152, 0.0722);
const float EDGE_FEATHER = 2.0;
const float PILL_JOIN_DP = 6.0;
const float BAND_SLACK_DP = 2.0;
const float LIFT_REACH_GAIN = 0.3;
const float PILL_MAGNIFY = 0.25;
const float PILL_FOLD = 0.8;
const float MIN_SPREAD_PX = 0.5;
const float PLATTER_CLEARING = 0.7;
const float RIM_WIDTH_DP = 1.5;
const float RIM_AWAY_SHARE = 0.15;
const float RIM_GAIN = 0.4;
const float LIFT_GLOW = 0.05;
const float RIM_REACH_DP = 2.0;
const float SEE_THROUGH_VEIL = 2.5;
const float CORNER_SCALE_LIMIT_DP = 32.0;

float roundBox(float2 p, float4 rect, float4 radii) {
    float2 centre = (rect.xy + rect.zw) * 0.5;
    float2 extent = (rect.zw - rect.xy) * 0.5;
    float2 q = p - centre;
    float r = q.x < 0.0 ? (q.y < 0.0 ? radii.x : radii.w) : (q.y < 0.0 ? radii.y : radii.z);
    float2 d = abs(q) - extent + r;
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - r;
}

float smoothUnion(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / max(k, EPSILON);
    return min(a, b) - h * h * k * 0.25;
}

float cornerRadius(float2 p, float4 rect, float4 radii) {
    float2 q = p - (rect.xy + rect.zw) * 0.5;
    return q.x < 0.0 ? (q.y < 0.0 ? radii.x : radii.w) : (q.y < 0.0 ? radii.y : radii.z);
}

float field(float2 p, out float lift, out float glow, out float radius, out float size, out float light) {
    float d = FAR;
    float closest = FAR;
    lift = 0.0;
    glow = 0.0;
    radius = 0.0;
    size = 0.0;
    light = 0.0;
    float closestShape = FAR;
    for (int i = 0; i < 4; i++) {
        if (i < shapeCount) {
            float4 rect = shapeRect[i];
            float s = roundBox(p, rect, shapeRadii[i]);
            d = smoothUnion(d, s, blend);
            if (s < closest) {
                closest = s;
                radius = cornerRadius(p, rect, shapeRadii[i]);
            }
            if (s < closestShape) {
                closestShape = s;
                size = min(rect.z - rect.x, rect.w - rect.y);
                light = shapeLight[i];
            }
            float within = 1.0 - smoothstep(-EDGE_FEATHER, EDGE_FEATHER, s);
            lift = max(lift, shapeLift[i] * within);
            glow = max(glow, shapeGlow[i] * within);
            float4 look = pillLook[i];
            if (look.x > 0.0 && look.w > 0.0) {
                float4 pr = pillRect[i];
                float pill = roundBox(p, pr, float4(look.z));
                d = smoothUnion(d, pill, PILL_JOIN_DP * detail.y);
                if (pill < closest) {
                    closest = pill;
                    radius = look.z;
                }
                float pillLift = look.w * look.x * (1.0 - smoothstep(-EDGE_FEATHER, EDGE_FEATHER, pill));
                lift = max(lift, pillLift);
                glow = max(glow, pillLift);
            }
        }
    }
    return d;
}

float2 fieldNormal(float2 p) {
    float ignoredLift;
    float ignoredGlow;
    float ignoredRadius;
    float ignoredSize;
    float ignoredLight;
    float dx = field(p + float2(1.0, 0.0), ignoredLift, ignoredGlow, ignoredRadius, ignoredSize, ignoredLight)
        - field(p - float2(1.0, 0.0), ignoredLift, ignoredGlow, ignoredRadius, ignoredSize, ignoredLight);
    float dy = field(p + float2(0.0, 1.0), ignoredLift, ignoredGlow, ignoredRadius, ignoredSize, ignoredLight)
        - field(p - float2(0.0, 1.0), ignoredLift, ignoredGlow, ignoredRadius, ignoredSize, ignoredLight);
    float2 g = float2(dx, dy);
    float len = length(g);
    return len > EPSILON ? g / len : float2(0.0, -1.0);
}

float luma(float3 c) {
    return dot(c, LUMA);
}

float3 backdropAt(float2 at) {
    half4 seen = content.eval(at);
    float3 rgb = seeThrough > 0.5 ? float3(seen.rgb) / max(float(seen.a), EPSILON) : float3(seen.rgb);
    return rgb * (1.0 - backdropDim);
}

float2 boxNormal(float2 p, float4 rect, float4 radii) {
    float dx = roundBox(p + float2(1.0, 0.0), rect, radii) - roundBox(p - float2(1.0, 0.0), rect, radii);
    float dy = roundBox(p + float2(0.0, 1.0), rect, radii) - roundBox(p - float2(0.0, 1.0), rect, radii);
    float2 g = float2(dx, dy);
    float len = length(g);
    return len > EPSILON ? g / len : float2(0.0, -1.0);
}

// The mean luminance of the backdrop under the surface holding p, read on a grid of three by three inside it.
float surfaceLuma(float2 p) {
    for (int i = 0; i < 4; i++) {
        if (i < shapeCount && roundBox(p, shapeRect[i], shapeRadii[i]) < 0.0) {
            float4 rect = shapeRect[i];
            float sum = 0.0;
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    sum += luma(backdropAt(mix(rect.xy, rect.zw, float2(0.25 + 0.25 * float(x), 0.25 + 0.25 * float(y)))));
                }
            }
            return sum / 9.0;
        }
    }
    return 0.0;
}

float bendAt(float depth, float band, float curve) {
    return pow(clamp(1.0 - depth / band, 0.0, 1.0), curve);
}

half4 main(float2 coord) {
    float lift;
    float glow;
    float radius;
    float size;
    float light;
    float d = field(coord, lift, glow, radius, size, light);
    if (d > 1.0) {
        return half4(0.0);
    }
    float pixel = detail.y;
    float depth = max(-d, 0.0);
    float scaled = min(radius, CORNER_SCALE_LIMIT_DP * pixel);
    float band = max(max(optics.y, optics.w * scaled), 1.0);
    float reach = max(optics.x, optics.z * scaled);
    float2 n = float2(0.0, -1.0);
    float bend = 0.0;
    if (depth < band + BAND_SLACK_DP * pixel) {
        n = fieldNormal(coord);
        bend = bendAt(depth, band, lens.x);
    }
    float shift = reach * bend * (1.0 + LIFT_REACH_GAIN * lift);
    float2 at = coord - n * shift;
    if (zoom != 1.0) {
        for (int i = 0; i < 4; i++) {
            if (i < shapeCount && roundBox(coord, shapeRect[i], shapeRadii[i]) < 0.0) {
                float2 centre = (shapeRect[i].xy + shapeRect[i].zw) * 0.5;
                at = centre + (at - centre) * zoom;
            }
        }
    }

    for (int i = 0; i < 4; i++) {
        float4 look = pillLook[i];
        if (i < shapeCount && look.x > 0.0 && look.y > 0.0) {
            float4 pr = pillRect[i];
            float4 radii = float4(look.z);
            float dp = roundBox(coord, pr, radii);
            if (dp < 0.0) {
                float strength = look.x * look.y;
                float2 centre = (pr.xy + pr.zw) * 0.5;
                at = centre + (at - centre) / (1.0 + PILL_MAGNIFY * strength);
                float pillBand = max(0.5 * min(pr.z - pr.x, pr.w - pr.y), 1.0);
                at -= boxNormal(coord, pr, radii) * bendAt(-dp, pillBand, lens.x) * optics.x * PILL_FOLD * strength;
            }
        }
    }

    float spread = detail.x * shift;
    float3 rgb;
    if (spread > MIN_SPREAD_PX) {
        rgb = float3(backdropAt(at + n * spread).r, backdropAt(at).g, backdropAt(at - n * spread).b);
    } else {
        rgb = backdropAt(at);
    }
    float3 seen = rgb;
    float lit = luma(rgb);
    float thickness = smoothstep(toneSizes.x, toneSizes.y, size);
    float lightShare = light * (1.0 - thickness);
    float4 tone = mix(mix(thinTone, thickTone, thickness), lightTone, lightShare);
    float darkLean = mix(leans.z + leans.w * lit, leans.x + leans.y * lit, thickness);
    float lean = clamp(mix(darkLean, lightLean.x + lightLean.y * lit, lightShare), 0.0, 1.0);
    rgb = mix(rgb, tone.rgb, lean);
    float own = luma(rgb);
    rgb = clamp(mix(float3(own), rgb, tone.a) + brighten, 0.0, 1.0);
    // The rim catches its light under the tint, so a strong tint all but hides it.
    float facing = 0.5 + 0.5 * dot(n, lens.zw);
    float rim = 1.0 - smoothstep(0.0, RIM_WIDTH_DP * pixel, depth);
    float shine = lens.y * rim * mix(RIM_AWAY_SHARE, 1.0, facing * facing) * RIM_GAIN + (pressLighten > 0.0 ? 0.0 : LIFT_GLOW * glow);
    rgb += shine;
    rgb = mix(rgb, tint.rgb, tint.a);
    if (pressTintGlow > 1.0) {
        rgb = mix(rgb, clamp(rgb * pressTintGlow, 0.0, 1.0), glow * tint.a);
    }

    for (int i = 0; i < 4; i++) {
        float4 look = pillLook[i];
        if (i < shapeCount && look.x > 0.0) {
            float inside = clamp(0.5 - roundBox(coord, pillRect[i], float4(look.z)), 0.0, 1.0);
            float4 shown = mix(platter, lightPlatter, lightShare);
            rgb = mix(rgb, shown.rgb, shown.a * inside * look.x * (1.0 - PLATTER_CLEARING * look.w));
        }
    }

    if (pressGlow > 0.0) {
        rgb = mix(rgb, clamp(seen * pressGlow, 0.0, 1.0), glow);
    }
    if (pressLighten > 0.0 && glow > 0.0) {
        rgb = mix(rgb, float3(1.0), clamp(pressLighten * surfaceLuma(coord), 0.0, 1.0) * glow);
    }

    if (rimGlow > 0.0) {
        float3 outside = backdropAt(coord + n * RIM_REACH_DP * pixel);
        rgb = mix(rgb, clamp(outside * rimGlow, 0.0, 1.0), rim);
    }
    rgb = mix(rgb, float3(1.0), rim * rimLight);

    float alpha = clamp(0.5 - d, 0.0, 1.0);
    if (seeThrough > 0.5) {
        float body = alpha * float(content.eval(at).a);
        float veil = (alpha - body) * clamp(shine + brighten * SEE_THROUGH_VEIL, 0.0, 1.0);
        return half4(half3(clamp(rgb, 0.0, 1.0) * body + veil), half(body + veil));
    }
    rgb = clamp(rgb, 0.0, 1.0) * alpha;
    return half4(half3(rgb), half(alpha));
}
"""

/**
 * A pill drawn inside a glass surface: the platter under the held option, which turns into a lens while it moves or
 * is pressed.
 *
 * @property rect The pill's rectangle, in the coordinates of the node that draws the surface.
 * @property radius The radius of the pill's corners.
 * @property alpha The pill's opacity, from 0 to 1.
 * @property lens How far the pill acts as a lens, from 0 to 1.
 * @property lift How far the pill is pressed, from 0 to 1; a pressed pill joins its surface's outline and may swell
 * past it.
 */
internal data class GlassPill(val rect: Rect, val radius: Float, val alpha: Float, val lens: Float, val lift: Float) {

    /**
     * Returns this pill moved by [offset].
     *
     * @param offset The distance to move by.
     * @return The moved pill.
     */
    fun translate(offset: Offset): GlassPill = copy(rect = rect.translate(offset))
}

/**
 * One surface a glass layer draws.
 *
 * @property form The surface's form, already swollen by its press.
 * @property lift How far the surface is pressed, from 0 to 1.
 * @property pill The pill inside the surface, or `null` for none.
 * @property light How light the surface has turned, from 0 to 1.
 * @property glow How brightly the surface lights under the finger, from 0 to 1; as far as it is pressed unless a press
 * lights it on a timing of its own.
 */
internal data class GlassSurface(
    val form: GlassForm,
    val lift: Float,
    val pill: GlassPill?,
    val light: Float = 0f,
    val glow: Float = lift,
)

/** Binds a [QuvenGlassStyle] and a set of [GlassSurface]s to the Liquid Glass program and builds its effect. */
@RequiresApi(33)
internal class LiquidGlassShader private constructor() {

    private val shader = RuntimeShader(LiquidGlassShaderSource)
    private val rects = FloatArray(LiquidGlassUniform.ShapeRect.floats)
    private val radii = FloatArray(LiquidGlassUniform.ShapeRadii.floats)
    private val lifts = FloatArray(LiquidGlassUniform.ShapeLift.floats)
    private val lights = FloatArray(LiquidGlassUniform.ShapeLight.floats)
    private val glows = FloatArray(LiquidGlassUniform.ShapeGlow.floats)
    private val pillRects = FloatArray(LiquidGlassUniform.PillRect.floats)
    private val pillLooks = FloatArray(LiquidGlassUniform.PillLook.floats)

    /**
     * Returns the effect that turns the backdrop recorded under [surfaces] into Liquid Glass.
     *
     * @param surfaces The surfaces, in the coordinates of the recorded layer; past [MaxGlassSurfaces] they are ignored.
     * @param style The material.
     * @param blend The distance over which two surfaces join, in pixels.
     * @param density The number of pixels in a density-independent pixel.
     * @param seeThrough Whether the backdrop's own alpha holds, letting what lies under the glass show where the backdrop
     * has no content.
     * @return The glass program, reading the backdrop the renderer has already blurred.
     */
    fun effect(surfaces: List<GlassSurface>, style: QuvenGlassStyle, blend: Float, density: Float, seeThrough: Boolean = false): RenderEffect {
        val count = minOf(surfaces.size, MaxGlassSurfaces)
        rects.fill(0f)
        radii.fill(0f)
        lifts.fill(0f)
        lights.fill(0f)
        glows.fill(0f)
        pillRects.fill(0f)
        pillLooks.fill(0f)
        for (index in 0 until count) {
            val surface = surfaces[index]
            val form = surface.form
            val at = index * 4
            rects[at] = form.rect.left
            rects[at + 1] = form.rect.top
            rects[at + 2] = form.rect.right
            rects[at + 3] = form.rect.bottom
            radii[at] = form.topLeft
            radii[at + 1] = form.topRight
            radii[at + 2] = form.bottomRight
            radii[at + 3] = form.bottomLeft
            lifts[index] = surface.lift
            lights[index] = surface.light
            glows[index] = surface.glow
            surface.pill?.let { pill ->
                pillRects[at] = pill.rect.left
                pillRects[at + 1] = pill.rect.top
                pillRects[at + 2] = pill.rect.right
                pillRects[at + 3] = pill.rect.bottom
                pillLooks[at] = pill.alpha
                pillLooks[at + 1] = pill.lens
                pillLooks[at + 2] = pill.radius
                pillLooks[at + 3] = pill.lift
            }
        }
        val radians = Math.toRadians(style.lightAngle.toDouble())
        with(shader) {
            setFloatUniform(LiquidGlassUniform.ShapeRect.uniform, rects)
            setFloatUniform(LiquidGlassUniform.ShapeRadii.uniform, radii)
            setFloatUniform(LiquidGlassUniform.ShapeLift.uniform, lifts)
            setFloatUniform(LiquidGlassUniform.ShapeLight.uniform, lights)
            setFloatUniform(LiquidGlassUniform.ShapeGlow.uniform, glows)
            setFloatUniform(LiquidGlassUniform.PillRect.uniform, pillRects)
            setFloatUniform(LiquidGlassUniform.PillLook.uniform, pillLooks)
            setIntUniform(LiquidGlassUniform.ShapeCount.uniform, count)
            setFloatUniform(LiquidGlassUniform.Blend.uniform, blend)
            setFloatUniform(
                LiquidGlassUniform.Optics.uniform,
                style.refraction.value * density,
                style.edgeWidth.value * density,
                style.cornerRefraction,
                style.cornerWidth,
            )
            setFloatUniform(LiquidGlassUniform.Lens.uniform, style.lensCurve, style.specular, sin(radians).toFloat(), -cos(radians).toFloat())
            setFloatUniform(
                LiquidGlassUniform.Detail.uniform,
                style.dispersion,
                density,
            )
            setTone(LiquidGlassUniform.ThinTone, style.thinTone)
            setTone(LiquidGlassUniform.ThickTone, style.thickTone)
            setFloatUniform(LiquidGlassUniform.Leans.uniform, style.thickTone.lean, style.thickTone.leanSlope, style.thinTone.lean, style.thinTone.leanSlope)
            setFloatUniform(LiquidGlassUniform.ToneSizes.uniform, style.thinSize.value * density, style.thickSize.value * density)
            setTone(LiquidGlassUniform.LightTone, style.lightTone)
            setFloatUniform(LiquidGlassUniform.LightLean.uniform, style.lightTone.lean, style.lightTone.leanSlope)
            setColor(LiquidGlassUniform.Tint, style.tint)
            setColor(LiquidGlassUniform.Platter, style.platter)
            setColor(LiquidGlassUniform.LightPlatter, style.lightPlatter)
            setFloatUniform(LiquidGlassUniform.PressGlow.uniform, style.pressGlow)
            setFloatUniform(LiquidGlassUniform.RimGlow.uniform, style.rimGlow)
            setFloatUniform(LiquidGlassUniform.Brighten.uniform, style.brighten)
            setFloatUniform(LiquidGlassUniform.Zoom.uniform, style.zoom)
            setFloatUniform(LiquidGlassUniform.SeeThrough.uniform, if (seeThrough) 1f else 0f)
            setFloatUniform(LiquidGlassUniform.BackdropDim.uniform, style.backdropDim)
            setFloatUniform(LiquidGlassUniform.PressTintGlow.uniform, style.pressTintGlow)
            setFloatUniform(LiquidGlassUniform.RimLight.uniform, style.rimLight)
            setFloatUniform(LiquidGlassUniform.PressLighten.uniform, style.pressLighten)
        }
        return RenderEffect.createRuntimeShaderEffect(shader, LiquidGlassContent)
    }

    private fun setColor(uniform: LiquidGlassUniform, color: Color) =
        shader.setFloatUniform(uniform.uniform, color.red, color.green, color.blue, color.alpha)

    private fun setTone(uniform: LiquidGlassUniform, tone: QuvenGlassTone) =
        shader.setFloatUniform(uniform.uniform, tone.shade.red, tone.shade.green, tone.shade.blue, tone.saturation)

    companion object {
        /**
         * Gets the program, built once per process: building it is the costly part, and every surface draws through it
         * from the main thread, one after another. An effect keeps the uniforms it was made with, so no surface reads
         * another's.
         *
         * @throws IllegalArgumentException The runtime cannot build the program.
         */
        val Shared: LiquidGlassShader by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { LiquidGlassShader() }
    }
}
