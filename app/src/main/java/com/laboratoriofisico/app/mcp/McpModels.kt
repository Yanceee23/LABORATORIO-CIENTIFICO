package com.laboratoriofisico.app.mcp

data class McpInvocationContext(
    val usuarioId: Long?
)

data class McpToolInfo(
    val name: String,
    val description: String
)

data class McpToolResult(
    val text: String,
    val isError: Boolean = false
)

data class McpServerConfig(
    val endpoint: String = LOCAL_ENDPOINT,
    val habilitado: Boolean = true
) {
    companion object {
        const val LOCAL_ENDPOINT = "local://mcp"
    }
}
