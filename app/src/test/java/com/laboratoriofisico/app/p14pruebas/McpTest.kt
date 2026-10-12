package com.laboratoriofisico.app.p14pruebas

import com.laboratoriofisico.app.fisica.DefaultFisicaEngine
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.mcp.McpInvocationContext
import com.laboratoriofisico.app.mcp.McpToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class McpTest {
    private val engine = DefaultFisicaEngine()

    @Test
    fun registry_calcularVelocidad() {
        val registry = McpToolRegistry(
            listOf(
                object : com.laboratoriofisico.app.mcp.McpToolHandler {
                    override val info = com.laboratoriofisico.app.mcp.McpToolInfo(
                        "calcular_velocidad",
                        "test"
                    )
                    override suspend fun invoke(
                        args: Map<String, Any?>,
                        context: McpInvocationContext
                    ) = com.laboratoriofisico.app.mcp.McpToolResult(
                        engine.calcular(
                            FormulaId.V_IGUAL_D_T.id,
                            mapOf("d" to 100.0, "t" to 4.0)
                        ).toString()
                    )
                }
            )
        )
        val result = kotlinx.coroutines.runBlocking {
            registry.invoke("calcular_velocidad", emptyMap(), McpInvocationContext(null))
        }
        assertFalse(result.isError)
        assertEquals("25.0", result.text)
    }
}
