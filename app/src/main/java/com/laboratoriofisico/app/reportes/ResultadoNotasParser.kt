package com.laboratoriofisico.app.reportes

import com.laboratoriofisico.app.fisica.FormulaId

object ResultadoNotasParser {
    fun parse(notas: String): ParsedNotas {
        if (notas.isBlank()) return ParsedNotas()

        val numericos = mutableMapOf<String, Double>()
        val textos = mutableMapOf<String, String>()
        var formulaId: FormulaId? = null
        var sensor: String? = null

        notas.split(";").forEach { part ->
            val trimmed = part.trim()
            if (trimmed.isBlank()) return@forEach
            val idx = trimmed.indexOf('=')
            if (idx <= 0) return@forEach
            val key = trimmed.substring(0, idx).trim()
            val value = trimmed.substring(idx + 1).trim()
            when (key) {
                "formula" -> formulaId = FormulaId.from(value)
                "sensor" -> sensor = value
                else -> {
                    value.toDoubleOrNull()?.let { numericos[key] = it }
                        ?: run { if (value.isNotBlank()) textos[key] = value }
                }
            }
        }

        return ParsedNotas(
            formulaId = formulaId,
            sensor = sensor,
            valoresNumericos = numericos,
            valoresTexto = textos
        )
    }
}

data class ParsedNotas(
    val formulaId: FormulaId? = null,
    val sensor: String? = null,
    val valoresNumericos: Map<String, Double> = emptyMap(),
    val valoresTexto: Map<String, String> = emptyMap()
)
