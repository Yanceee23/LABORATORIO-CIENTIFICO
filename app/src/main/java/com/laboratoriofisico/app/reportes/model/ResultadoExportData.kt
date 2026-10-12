package com.laboratoriofisico.app.reportes.model

import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.fisica.FormulaId

data class ResultadoExportData(
    val resultado: Resultado,
    val medicion: Medicion,
    val experimento: Experimento,
    val usuarioNombre: String,
    val formulaId: FormulaId?,
    val sensor: String?,
    val valoresNumericos: Map<String, Double>,
    val valoresTexto: Map<String, String>
)
