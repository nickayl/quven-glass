package tv.quven.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassShaderContractTest {

    @Test
    fun everyUniformTheBinderSetsOnTheGlass_isDeclaredWithTheFloatsItIsGiven() {
        val declared = declaredIn(LiquidGlassShaderSource)

        assertTrue(declared.isNotEmpty())
        assertEquals(LiquidGlassUniform.entries.associate { it.uniform to it.floats } + (LiquidGlassContent to 0), declared)
    }

    @Test
    fun theArraysHoldAsManySurfacesAsAContainerDraws() {
        assertEquals(4 * MaxGlassSurfaces, declaredIn(LiquidGlassShaderSource).getValue("shapeRect"))
        assertTrue(LiquidGlassShaderSource.contains("i < $MaxGlassSurfaces;"))
    }

    private fun declaredIn(source: String): Map<String, Int> = Regex("""uniform\s+(\w+)\s+(\w+)\s*(?:\[(\d+)])?\s*;""")
        .findAll(source)
        .associate { match ->
            val (type, name, count) = match.destructured
            name to floatsOf(type) * (count.toIntOrNull() ?: 1)
        }

    private fun floatsOf(type: String): Int = when (type) {
        "float" -> 1
        "float2" -> 2
        "float4", "half4" -> 4
        "int", "shader" -> 0
        else -> error("The binder cannot set a uniform of type $type.")
    }
}
