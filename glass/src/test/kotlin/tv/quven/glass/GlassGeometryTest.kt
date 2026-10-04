package tv.quven.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlassGeometryTest {

    private val density = Density(2f)

    @Test
    fun aCapsule_isARoundedRectangleWhoseRadiiAreHalfItsHeight() {
        val form = GlassForm.of(CircleShape, Size(300f, 80f), LayoutDirection.Ltr, density)!!

        assertEquals(Rect(0f, 0f, 300f, 80f), form.rect)
        assertEquals(listOf(40f, 40f, 40f, 40f), form.radii())
    }

    @Test
    fun aRoundedRectangle_keepsEachCornerItsOwnRadius_andARightToLeftLayoutMirrorsThem() {
        val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomEnd = 12.dp, bottomStart = 16.dp)

        assertEquals(listOf(8f, 16f, 24f, 32f), GlassForm.of(shape, Size(200f, 100f), LayoutDirection.Ltr, density)!!.radii())
        assertEquals(listOf(16f, 8f, 32f, 24f), GlassForm.of(shape, Size(200f, 100f), LayoutDirection.Rtl, density)!!.radii())
    }

    @Test
    fun aRadiusLargerThanTheForm_isHeldToHalfItsShorterSide() {
        val form = GlassForm.of(RoundedCornerShape(100.dp), Size(60f, 40f), LayoutDirection.Ltr, density)!!

        assertEquals(listOf(20f, 20f, 20f, 20f), form.radii())
    }

    @Test
    fun aRectangle_hasSquareCorners_andAShapeOutlinedByAPath_hasNoForm() {
        assertEquals(listOf(0f, 0f, 0f, 0f), GlassForm.of(RectangleShape, Size(10f, 10f), LayoutDirection.Ltr, density)!!.radii())
        val path = GenericShape { size, _ -> lineTo(size.width, 0f); lineTo(0f, size.height); close() }
        assertNull(GlassForm.of(path, Size(10f, 10f), LayoutDirection.Ltr, density))
    }

    @Test
    fun swelling_growsTheRectAndTheRoundedCorners_andLeavesSquareCornersSquare() {
        val form = GlassForm(Rect(10f, 10f, 110f, 50f), topLeft = 20f, topRight = 0f, bottomRight = 20f, bottomLeft = 0f)

        val swollen = form.inflate(4f)

        assertEquals(Rect(6f, 6f, 114f, 54f), swollen.rect)
        assertEquals(listOf(24f, 0f, 24f, 0f), swollen.radii())
        assertEquals(form, form.inflate(0f))
    }

    @Test
    fun aCircle_staysACircleAsItSwells() {
        val circle = GlassForm.of(CircleShape, Size(60f, 60f), LayoutDirection.Ltr, density)!!

        val swollen = circle.inflate(6f)

        assertEquals(listOf(36f, 36f, 36f, 36f), swollen.radii())
        assertEquals(72f, swollen.rect.width)
    }

    @Test
    fun translating_movesTheRectAlone() {
        val form = GlassForm(Rect(0f, 0f, 10f, 10f), 5f, 5f, 5f, 5f)

        assertEquals(GlassForm(Rect(3f, -2f, 13f, 8f), 5f, 5f, 5f, 5f), form.translate(Offset(3f, -2f)))
    }

    @Test
    fun theMargin_coversTwiceTheBlur_andNeverFallsBelowTwoPixels() {
        assertEquals(6f * 2f + 2f, glassMargin(blur = 6f))
        assertEquals(2f, glassMargin(blur = -3f))
    }

    @Test
    fun theRegion_spansEveryFormGrownByTheMargin_roundedOutToWholePixels() {
        val capsule = GlassForm(Rect(10.4f, 20.6f, 300.2f, 90.5f), 35f, 35f, 35f, 35f)
        val circle = GlassForm(Rect(310f, 18f, 380.7f, 88f), 35f, 35f, 35f, 35f)

        assertEquals(IntRect(0, 8, 391, 101), glassRegion(listOf(capsule, circle), margin = 10f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun aRegionOfNoForm_isRefused() {
        glassRegion(emptyList(), margin = 0f)
    }

    private fun GlassForm.radii() = listOf(topLeft, topRight, bottomRight, bottomLeft)
}
