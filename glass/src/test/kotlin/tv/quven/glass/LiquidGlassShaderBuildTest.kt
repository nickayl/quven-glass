package tv.quven.glass

import android.app.Application
import android.graphics.RuntimeShader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class LiquidGlassShaderBuildTest {

    // A program that does not build leaves every surface on the static material, with nothing else failing.
    @Test
    fun theGlassProgram_builds() {
        RuntimeShader(inTestRuntime(LiquidGlassShaderSource))
    }

    // The test runtime's SkSL predates `shader.eval`, which it still spells `sample(shader, ...)`.
    private fun inTestRuntime(source: String): String = Regex("""(\w+)\.eval\(""").replace(source) { "sample(${it.groupValues[1]}, " }
}
