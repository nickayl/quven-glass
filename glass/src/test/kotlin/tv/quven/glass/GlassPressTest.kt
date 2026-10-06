package tv.quven.glass

import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class GlassPressTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aTapLiftingBeforeThePressHasRisen_stillRisesAllTheWay_thenFalls() {
        val source = MutableInteractionSource()
        lateinit var press: GlassPress
        compose.mainClock.autoAdvance = false
        compose.setContent {
            press = remember { GlassPress() }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) { press.follow(scope, source) { held -> tween(if (held) Rise else Fall) } }
        }
        compose.mainClock.advanceTimeByFrame()

        val tap = PressInteraction.Press(Offset.Zero)
        compose.runOnIdle {
            source.tryEmit(tap)
            source.tryEmit(PressInteraction.Release(tap))
        }
        compose.mainClock.advanceTimeBy(Rise.toLong() + 16)

        assertTrue("The tap lit the surface only ${press.value} of the way", press.value > 0.95f)

        compose.mainClock.advanceTimeBy(Fall.toLong() + 32)

        assertEquals(0f, press.value, 0.001f)
    }

    @Test
    fun aTapLiftingBeforeThePressHasRisen_turnsBackAtOnce_whereTheRiseNeedNotComplete() {
        val source = MutableInteractionSource()
        lateinit var press: GlassPress
        compose.mainClock.autoAdvance = false
        compose.setContent {
            press = remember { GlassPress() }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) { press.follow(scope, source, completesRise = false) { held -> tween(if (held) Rise else Fall) } }
        }
        compose.mainClock.advanceTimeByFrame()

        val tap = PressInteraction.Press(Offset.Zero)
        compose.runOnIdle {
            source.tryEmit(tap)
            source.tryEmit(PressInteraction.Release(tap))
        }
        compose.mainClock.advanceTimeBy(Rise.toLong() + 16)

        assertTrue("The tap still rose ${press.value} of the way", press.value < 0.5f)
    }

    private companion object {
        const val Rise = 70
        const val Fall = 450
    }
}
