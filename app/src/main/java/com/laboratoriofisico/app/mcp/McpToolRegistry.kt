package com.laboratoriofisico.app.mcp

interface McpToolHandler {
    val info: McpToolInfo
    suspend fun invoke(args: Map<String, Any?>, context: McpInvocationContext): McpToolResult
}

class McpToolRegistry(
    handlers: List<McpToolHandler>
) {
    private val byName = handlers.associateBy { it.info.name }

    fun listTools(): List<McpToolInfo> = byName.values.map { it.info }

    suspend fun invoke(
        name: String,
        args: Map<String, Any?>,
        context: McpInvocationContext
    ): McpToolResult {
        val handler = byName[name]
            ?: return McpToolResult("Herramienta MCP desconocida: $name", isError = true)
        return runCatching { handler.invoke(args, context) }
            .getOrElse { error ->
                McpToolResult(
                    text = error.message ?: "Error al ejecutar $name",
                    isError = true
                )
            }
    }
}
