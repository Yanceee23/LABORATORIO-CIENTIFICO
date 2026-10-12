package com.laboratoriofisico.app.reportes

import com.laboratoriofisico.app.reportes.model.ResultadoCsvRow
import java.io.File

class ResultadoCsvGenerator {
    fun generar(rows: List<ResultadoCsvRow>, destino: File) {
        val content = buildString {
            appendLine("experimento,tipo,valor,unidad,fecha,hora")
            rows.forEach { row ->
                appendLine(
                    listOf(
                        row.experimento,
                        row.tipo,
                        row.valor,
                        row.unidad,
                        row.fecha,
                        row.hora
                    ).joinToString(",") { escapeCsv(it) }
                )
            }
        }
        destino.writeText(content, Charsets.UTF_8)
    }

    private fun escapeCsv(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
