package tv.quven.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class QuvenGlassContainerTest {

    @get:Rule
    val compose = createComposeRule()

    private val page = QuvenGlassBackdrop()
    private val search = QuvenGlassBackdrop()
    private var backdrop by mutableStateOf(page)

    @Test
    fun aContainerWhoseBackdropChanges_drawsTheSurfacesOverTheNewOne() {
        var state: GlassContainerState? = null
        var inner: QuvenGlassBackdrop? = null
        compose.setContent {
            QuvenGlassContainer(backdrop = backdrop) {
                state = LocalGlassContainer.current
                inner = LocalQuvenGlassBackdrop.current
                Box(Modifier.size(40.dp).quvenLiquidGlass(LocalQuvenGlassBackdrop.current, shape = CircleShape))
            }
        }

        compose.runOnIdle { backdrop = search }
        compose.waitForIdle()

        assertSame(search, state?.backdrop)
        assertSame("The surfaces inside stand over the container's backdrop", search, inner)
        assertNotNull("No node draws the new backdrop's surfaces", state?.node)
    }
}
