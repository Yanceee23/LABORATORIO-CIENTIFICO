package com.laboratoriofisico.app.mcp

import org.json.JSONObject

object McpQueryParser {
    private val toolPrefix = Regex("""^@tool:(\w+)\s*(\{.*\})?\s*$""", RegexOption.DOT_MATCHES_ALL)

    fun parse(consulta: String): ParsedQuery {
        val trimmed = consulta.trim()
        toolPrefix.matchEntire(trimmed)?.let { match ->
            val name = match.groupValues[1]
            val json = match.groupValues.getOrNull(2)?.trim().orEmpty()
            val args = if (json.isBlank()) emptyMap() else jsonToMap(JSONObject(json))
            return ParsedQuery.ToolCall(name, args)
        }
        return ParsedQuery.NaturalLanguage(trimmed)
    }

    private fun jsonToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        json.keys().forEach { key ->
            map[key] = json.get(key)
        }
        return map
    }
}

sealed class ParsedQuery {
    data class ToolCall(val name: String, val args: Map<String, Any?>) : ParsedQuery()
    data class NaturalLanguage(val text: String) : ParsedQuery()
}
