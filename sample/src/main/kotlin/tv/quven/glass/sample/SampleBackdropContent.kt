package tv.quven.glass.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class Poster(val title: String, val top: Color, val bottom: Color, val mark: Color, val text: Color)

private val Posters = listOf(
    Poster("Solstice", Color(0xFFFFF3B0), Color(0xFFFFC300), Color(0xFFFFFFFF), Color(0xFF1A1A1A)),
    Poster("Abyss", Color(0xFF0B1A3A), Color(0xFF000000), Color(0xFF2E6BFF), Color.White),
    Poster("Crimson", Color(0xFFFF2D55), Color(0xFF6A0018), Color(0xFFFFB3C1), Color.White),
    Poster("Snowfield", Color(0xFFFFFFFF), Color(0xFFE6F0FF), Color(0xFF9EC5FF), Color(0xFF101010)),
    Poster("Verdant", Color(0xFF00C853), Color(0xFF003D1A), Color(0xFFB9FFD1), Color.White),
    Poster("Nightfall", Color(0xFF111111), Color(0xFF000000), Color(0xFFE8540E), Color.White),
    Poster("Lagoon", Color(0xFF00E5FF), Color(0xFF0040FF), Color(0xFFFFFFFF), Color.White),
    Poster("Ember", Color(0xFFFFAB00), Color(0xFFD50000), Color(0xFFFFF59D), Color.White),
    Poster("Lilac", Color(0xFFE1BEE7), Color(0xFF7B1FA2), Color(0xFFFFFFFF), Color.White),
)

/**
 * Draws the content the glass stands over: posters very bright and very dark, white text, bands of white and black
 * and fine stripes, in a list that scrolls under the surfaces; the same content as the iOS reference.
 *
 * @param modifier Modifier applied to the list.
 * @param scroll The distance the list starts scrolled by, in density-independent pixels.
 */
@Composable
internal fun SampleBackdropContent(modifier: Modifier = Modifier, scroll: Float = 0f) {
    val state = rememberLazyListState()
    val density = LocalDensity.current
    LaunchedEffect(scroll) { if (scroll > 0f) state.scrollBy(with(density) { scroll.dp.toPx() }) }
    LazyColumn(modifier.background(SampleColors.Ground), state = state, contentPadding = PaddingValues(bottom = 140.dp)) {
        item { Header() }
        items(12) { block ->
            when (block % 4) {
                0 -> PosterRow(offset = block)
                1 -> TextBlock()
                2 -> Bands()
                else -> PosterRow(offset = block + 3)
            }
        }
        item { CalibrationGrid() }
        item { CalibrationRamp(horizontal = true, period = 48.dp, height = 160.dp) }
        item { CalibrationRamp(horizontal = false, period = 96.dp, height = 240.dp) }
        item { CalibrationProbe(horizontal = true, height = 160.dp) }
        item { CalibrationProbe(horizontal = false, height = 200.dp) }
    }
}

// White lines every 12 dp and red ones every 48 on black, to measure how the glass moves what lies under it.
@Composable
private fun CalibrationGrid() {
    Canvas(Modifier.fillMaxWidth().height(240.dp)) {
        drawRect(Color.Black)
        val step = 12.dp.toPx()
        val line = 2.dp.toPx()
        var index = 0
        var x = 0f
        while (x < size.width) {
            drawRect(if (index % 4 == 0) Color.Red else Color.White, topLeft = Offset(x, 0f), size = Size(line, size.height))
            x += step
            index++
        }
        index = 0
        var y = 0f
        while (y < size.height) {
            drawRect(if (index % 4 == 0) Color.Red else Color.White, topLeft = Offset(0f, y), size = Size(size.width, line))
            y += step
            index++
        }
    }
}

@Composable
private fun Header() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF3A0CA3), Color(0xFFF72585), Color(0xFFFFD60A)))),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(Modifier.statusBarsPadding().padding(24.dp)) {
            Text("Quven Glass", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
            Text("Liquid Glass for Compose", color = Color.White.copy(alpha = 0.85f), fontSize = 22.sp)
        }
    }
}

@Composable
private fun PosterRow(offset: Int) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(Posters.size) { index -> PosterCard(Posters[(index + offset) % Posters.size]) }
    }
}

@Composable
private fun PosterCard(poster: Poster) {
    Box(
        Modifier
            .width(170.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(poster.top, poster.bottom))),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(poster.mark, radius = size.width * 0.32f, center = Offset(size.width * 0.62f, size.height * 0.36f))
            drawCircle(poster.mark.copy(alpha = 0.4f), radius = size.width * 0.18f, center = Offset(size.width * 0.25f, size.height * 0.62f))
        }
        Text(
            poster.title.uppercase(),
            color = poster.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}

@Composable
private fun TextBlock() {
    Text(
        "The glass bends what lies under it towards its rim, blurs it a little, parts its colours along the edge and " +
            "lifts its saturation. Over bright posters it darkens so the labels stay legible; over dark ones it stays " +
            "clear. Scroll this text under the bar to see it bend.",
        color = SampleColors.TextHigh,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        modifier = Modifier.fillMaxWidth().height(150.dp).padding(start = 24.dp, end = 24.dp, top = 20.dp),
    )
}

@Composable
private fun Bands() {
    Column {
        Box(Modifier.fillMaxWidth().height(90.dp).background(Color.White), contentAlignment = Alignment.CenterStart) {
            Text("A WHITE BAND", color = Color.Black, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp))
        }
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val stripe = 6.dp.toPx()
            var x = 0f
            var dark = false
            while (x < size.width) {
                drawRect(if (dark) Color.Black else Color.White, topLeft = Offset(x, 0f), size = Size(stripe, size.height))
                x += stripe
                dark = !dark
            }
        }
        Row(Modifier.fillMaxWidth().height(90.dp)) {
            listOf(Color(0xFFFF1744), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFFFEA00), Color.Black).forEach {
                Box(Modifier.weight(1f).height(90.dp).background(it))
            }
        }
    }
}

// A grey sawtooth from black to white every period, across or down, whose shade tells where a pixel seen through the
// glass was taken from.
@Composable
private fun CalibrationRamp(horizontal: Boolean, period: Dp, height: Dp) {
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val step = period.toPx()
        val extent = if (horizontal) size.width else size.height
        var start = 0f
        while (start < extent) {
            val brush = if (horizontal) {
                Brush.horizontalGradient(listOf(Color.Black, Color.White), startX = start, endX = start + step)
            } else {
                Brush.verticalGradient(listOf(Color.Black, Color.White), startY = start, endY = start + step)
            }
            val topLeft = if (horizontal) Offset(start, 0f) else Offset(0f, start)
            val area = if (horizontal) Size(step, size.height) else Size(size.width, step)
            drawRect(brush, topLeft = topLeft, size = area)
            start += step
        }
    }
}

// One grey ramp, black to white over 120 dp: across, centred on the left rim of the bar's capsule (287 dp left of the
// window's centre); down, from 40 to 160 dp below the band's top.
@Composable
private fun CalibrationProbe(horizontal: Boolean, height: Dp) {
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val rim = size.width / 2f - 287.dp.toPx()
        val brush = if (horizontal) {
            Brush.horizontalGradient(listOf(Color.Black, Color.White), startX = rim - 60.dp.toPx(), endX = rim + 60.dp.toPx())
        } else {
            Brush.verticalGradient(listOf(Color.Black, Color.White), startY = 40.dp.toPx(), endY = 160.dp.toPx())
        }
        drawRect(brush)
    }
}
