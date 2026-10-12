package com.laboratoriofisico.app.p08mcp.servidormcp

import com.laboratoriofisico.app.mcp.McpServerConfig

/** Configuración del servidor MCP in-app. */
data class ConfiguracionMcp(
    val endpoint: String = McpServerConfig.LOCAL_ENDPOINT,
    val habilitado: Boolean = true
) {
    fun toServerConfig(): McpServerConfig = McpServerConfig(endpoint, habilitado)
}
