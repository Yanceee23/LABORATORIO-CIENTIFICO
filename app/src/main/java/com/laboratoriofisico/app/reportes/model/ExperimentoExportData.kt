package com.laboratoriofisico.app.reportes.model

import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.model.Ubicacion
import com.laboratoriofisico.app.fisica.FormulaId

data class ExperimentoExportData(
    val experimento: Experimento,
    val usuarioNombre: String,
    val ubicacion: Ubicacion?,
    val mediciones: List<MedicionExportRow>
) {
    val totalMediciones: Int get() = mediciones.size
    val totalResultados: Int get() = mediciones.sumOf { it.resultados.size }
}

data class MedicionExportRow(
    val medicion: Medicion,
    val resultados: List<ResultadoExportRow>
)

data class ResultadoExportRow(
    val resultado: Resultado,
    val formulaId: FormulaId?,
    val sensor: String?
)
