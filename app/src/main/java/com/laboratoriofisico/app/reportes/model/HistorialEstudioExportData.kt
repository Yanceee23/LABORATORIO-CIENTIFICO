package com.laboratoriofisico.app.reportes.model

import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.HistorialEstudio

data class HistorialEstudioExportData(
    val documento: Documento,
    val usuarioNombre: String,
    val sesiones: List<HistorialEstudio>
) {
    val totalSesiones: Int get() = sesiones.size
    val tiempoTotalMs: Long get() = sesiones.sumOf { it.tiempoEstudio }
    val calificacionesExamen: List<CalificacionExamenRow> =
        sesiones
            .filter { it.tipoActividad.equals("examen", ignoreCase = true) && it.resultado.contains('%') }
            .map { CalificacionExamenRow(fecha = it.fecha, calificacion = it.resultado) }
}

data class CalificacionExamenRow(
    val fecha: Long,
    val calificacion: String
)
