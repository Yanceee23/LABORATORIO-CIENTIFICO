package com.laboratoriofisico.app.fisica

import com.laboratoriofisico.app.sensores.model.SensorType

/**
 * Serializa metadatos de [Resultado] en el campo `notas`.
 *
 * TODO Fase 4+: considerar columna `formulaId` en [ResultadoEntity] para consultas/filtros.
 */
object ResultadoMetadata {
    fun build(
        formulaId: FormulaId,
        sensor: SensorType? = null,
        extras: Map<String, Double> = emptyMap()
    ): String = buildExtended(formulaId = formulaId, sensor = sensor, numericExtras = extras)

    fun buildExtended(
        formulaId: FormulaId? = null,
        sensor: SensorType? = null,
        numericExtras: Map<String, Double> = emptyMap(),
        textExtras: Map<String, String> = emptyMap()
    ): String {
        val parts = mutableListOf<String>()
        formulaId?.let { parts.add("formula=${it.id}") }
        sensor?.let { parts.add("sensor=${it.name}") }
        numericExtras.forEach { (key, value) -> parts.add("$key=$value") }
        textExtras.forEach { (key, value) -> if (value.isNotBlank()) parts.add("$key=$value") }
        return parts.joinToString(";").ifBlank { "medicion_sensor" }
    }
}
