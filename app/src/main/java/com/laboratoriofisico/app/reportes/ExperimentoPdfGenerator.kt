package com.laboratoriofisico.app.reportes

import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.reportes.model.ExperimentoExportData
import java.io.File

class ExperimentoPdfGenerator {
    fun generar(data: ExperimentoExportData, destino: File) {
        val layout = PdfReportLayout()

        layout.drawTitle("Laboratorio Científico")
        layout.drawLine("Reporte completo de experimento")
        layout.gap()

        layout.drawSection("Información general")
        layout.drawLine("Experimento: ${data.experimento.titulo}")
        if (data.experimento.descripcion.isNotBlank()) {
            layout.drawLine("Descripción: ${data.experimento.descripcion}")
        }
        layout.drawLine("Estudiante: ${data.usuarioNombre}")
        layout.drawLine("Estado: ${data.experimento.estado.name}")
        layout.drawLine("Fecha de creación: ${DateTimeUtils.format(data.experimento.fechaCreacion)}")
        data.ubicacion?.let { layout.drawLine("Ubicación: ${it.nombre}") }

        layout.drawSection("Resumen")
        layout.drawLine("Mediciones registradas: ${data.totalMediciones}")
        layout.drawLine("Resultados calculados: ${data.totalResultados}")

        data.mediciones.forEachIndexed { index, row ->
            layout.drawSection("Medición ${index + 1}: ${row.medicion.nombre}")
            layout.drawLine("Valor medido: ${PdfReportLayout.formatNumber(row.medicion.valor)} ${row.medicion.unidad}")
            layout.drawLine("Fecha: ${DateTimeUtils.format(row.medicion.timestamp)}")

            if (row.resultados.isEmpty()) {
                layout.drawLine("Sin resultados calculados para esta medición.", indent = 12f)
            } else {
                row.resultados.forEachIndexed { resultadoIndex, resultadoRow ->
                    layout.drawLine(
                        "Resultado ${resultadoIndex + 1}: " +
                            "${PdfReportLayout.formatNumber(resultadoRow.resultado.valorCalculado)} " +
                            "${resultadoRow.resultado.unidad}",
                        indent = 12f
                    )
                    layout.drawLine(
                        "Fecha: ${DateTimeUtils.format(resultadoRow.resultado.timestamp)}",
                        indent = 12f
                    )
                    resultadoRow.formulaId?.let {
                        layout.drawLine("Fórmula: ${formulaLabel(it)}", indent = 12f)
                    }
                    resultadoRow.sensor?.let {
                        layout.drawLine("Sensor: ${it.replace('_', ' ')}", indent = 12f)
                    }
                }
            }
        }

        layout.drawSection("Generado por")
        layout.drawLine("Laboratorio Científico · ${DateTimeUtils.format(System.currentTimeMillis())}")

        layout.writeTo(destino)
    }

    private fun formulaLabel(formulaId: FormulaId): String = when (formulaId) {
        FormulaId.V_IGUAL_D_T -> "v = Δd / Δt"
        FormulaId.A_IGUAL_DELTA_V_DELTA_T -> "a = Δv / Δt"
        FormulaId.D_IGUAL_V_T -> "d = v · t"
        FormulaId.F_IGUAL_1_SOBRE_T -> "f = 1 / T"
        FormulaId.T_IGUAL_1_SOBRE_F -> "T = 1 / f"
        FormulaId.V_IGUAL_LAMBDA_F -> "v = λ · f"
        FormulaId.G_PENDULO -> "g = 4π²L / T²"
        FormulaId.B_INTENSIDAD -> "B = √(Bx² + By² + Bz²)"
        FormulaId.THETA_INCIDENCIA_REFLEXION -> "θ incidencia = θ reflexión"
    }
}
