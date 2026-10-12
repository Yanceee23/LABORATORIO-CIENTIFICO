package com.laboratoriofisico.app.p08mcp.servidormcp

import com.laboratoriofisico.app.mcp.McpInvocationContext
import com.laboratoriofisico.app.mcp.McpServerConfig

class McpSeguridad(
    private val sessionRepository: com.laboratoriofisico.app.backend.session.SessionRepository
) {
    fun validarEndpoint(endpoint: String) {
        require(
            endpoint == McpServerConfig.LOCAL_ENDPOINT || endpoint.startsWith("http")
        ) { "Endpoint MCP no soportado: $endpoint" }
    }

    suspend fun validarInvocacion(contexto: McpInvocationContext) {
        val userId = sessionRepository.getLoggedInUserId()
        if (contexto.usuarioId != null && userId != null && contexto.usuarioId != userId) {
            throw SecurityException("Contexto de usuario no coincide con la sesión.")
        }
    }
}
