package com.laboratoriofisico.app.mcp

class McpNaturalLanguageRouter(
    private val registry: McpToolRegistry
) {
    suspend fun route(text: String, context: McpInvocationContext): McpToolResult? {
        val lower = text.lowercase()
        return when {
            lower.contains("listar") && lower.contains("documento") ->
                registry.invoke("listar_documentos", emptyMap(), context)

            lower.contains("buscar") && lower.contains("pdf") -> {
                val query = extractAfter(lower, "buscar") ?: return null
                registry.invoke(
                    "buscar_texto",
                    mapOf("documentoId" to extractLong(text, "documentoId"), "query" to query),
                    context
                )
            }

            lower.contains("calcular") && lower.contains("velocidad") ->
                registry.invoke("calcular_velocidad", extractNumericArgs(text), context)

            lower.contains("calcular") && lower.contains("magnet") ->
                registry.invoke("calcular_magnetismo", extractNumericArgs(text), context)

            lower.contains("resumen") && lower.contains("documento") ->
                registry.invoke(
                    "crear_resumen",
                    mapOf(
                        "documentoId" to extractLong(text, "documentoId"),
                        "tipo" to "CORTO"
                    ),
                    context
                )

            lower.contains("comparar") && lower.contains("resultado") ->
                registry.invoke(
                    "comparar_resultados",
                    mapOf(
                        "resultadoIdA" to extractLong(text, "resultadoIdA"),
                        "resultadoIdB" to extractLong(text, "resultadoIdB")
                    ),
                    context
                )

            else -> null
        }
    }

    fun helpText(): String = buildString {
        appendLine("Consulta MCP. Herramientas disponibles:")
        registry.listTools().forEach { tool ->
            appendLine("- ${tool.name}: ${tool.description}")
        }
        appendLine()
        append("Formato directo: @tool:nombre {\"clave\": valor}")
    }

    private fun extractAfter(text: String, keyword: String): String? {
        val idx = text.indexOf(keyword, ignoreCase = true)
        if (idx < 0) return null
        return text.substring(idx + keyword.length).trim().trim('"', ' ', ':')
            .takeIf { it.isNotBlank() }
    }

    private fun extractLong(text: String, key: String): Long? {
        val regex = Regex("""$key\s*[=:]\s*(\d+)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractNumericArgs(text: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        Regex("""(\w+)\s*[=:]\s*(-?\d+(?:\.\d+)?)""").findAll(text).forEach { match ->
            map[match.groupValues[1]] = match.groupValues[2].toDoubleOrNull()
        }
        return map
    }
}
