package tv.quven.glass

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PressSpringsTest {

    private val rise = spring<Float>(dampingRatio = 0.7f, stiffness = 625f)
    private val fall = spring<Float>(dampingRatio = 0.71f, stiffness = 400f)

    @Test
    fun aPress_risesOnItsRise_andFallsOnItsFall() {
        val springs = PressSprings(rise, fall)

        assertSame(rise, springs.spec(held = true, reduceMotion = false))
        assertSame(fall, springs.spec(held = false, reduceMotion = false))
    }

    @Test
    fun whereMotionIsReduced_eitherWayIsAShortFade() {
        val springs = PressSprings(rise, fall)

        listOf(true, false).forEach { held ->
            val spec = springs.spec(held, reduceMotion = true)
            assertTrue("A reduced press moved on $spec", spec is TweenSpec<Float>)
            assertEquals(ReducedMotionFadeMillis, (spec as TweenSpec<Float>).durationMillis)
        }
    }

    @Test
    fun oneSpring_isTheRiseAndTheFallAlike() {
        val springs = PressSprings(rise)

        assertSame(rise, springs.rise)
        assertSame(rise, springs.fall)
    }
}
