package com.laboratoriofisico.app.reportes

import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.reportes.model.HistorialEstudioExportData
import java.io.File

class HistorialEstudioPdfGenerator {
    fun generar(data: HistorialEstudioExportData, destino: File) {
        val layout = PdfReportLayout()

        layout.drawTitle("Laboratorio Científico")
        layout.drawLine("Reporte de historial de estudio")
        layout.gap()

        layout.drawSection("Información general")
        layout.drawLine("Documento: ${data.documento.nombre}")
        layout.drawLine("Estudiante: ${data.usuarioNombre}")
        layout.drawLine("Importado: ${DateTimeUtils.format(data.documento.fechaImportacion)}")

        layout.drawSection("Resumen de actividad")
        layout.drawLine("Sesiones registradas: ${data.totalSesiones}")
        layout.drawLine("Tiempo total estudiado: ${PdfReportLayout.formatDuration(data.tiempoTotalMs)}")

        val porTipo = data.sesiones.groupBy { it.tipoActividad }
        if (porTipo.isNotEmpty()) {
            layout.drawLine("Actividades por tipo:")
            porTipo.entries.sortedBy { it.key }.forEach { (tipo, lista) ->
                layout.drawLine("• ${tipoActividadLabel(tipo)}: ${lista.size}", indent = 12f)
            }
        }

        if (data.calificacionesExamen.isNotEmpty()) {
            layout.drawSection("Calificaciones de exámenes")
            data.calificacionesExamen.forEach { calificacion ->
                layout.drawLine(
                    "${DateTimeUtils.format(calificacion.fecha)} — ${calificacion.calificacion}"
                )
            }
            val promedio = promedioCalificaciones(data.calificacionesExamen.map { it.calificacion })
            promedio?.let { layout.drawLine("Promedio: $it%") }
        } else {
            layout.drawSection("Calificaciones de exámenes")
            layout.drawLine("No hay exámenes finalizados con calificación registrada.")
        }

        layout.drawSection("Detalle de sesiones")
        data.sesiones.sortedByDescending { it.fecha }.forEach { sesion ->
            layout.drawLine(
                "${DateTimeUtils.format(sesion.fecha)} · ${tipoActividadLabel(sesion.tipoActividad)}"
            )
            layout.drawLine("Resultado: ${sesion.resultado}", indent = 12f)
            layout.drawLine(
                "Tiempo: ${PdfReportLayout.formatDuration(sesion.tiempoEstudio)}",
                indent = 12f
            )
        }

        layout.drawSection("Generado por")
        layout.drawLine("Laboratorio Científico · ${DateTimeUtils.format(System.currentTimeMillis())}")

        layout.writeTo(destino)
    }

    private fun tipoActividadLabel(tipo: String): String = when (tipo.lowercase()) {
        "resumen" -> "Resumen"
        "notas" -> "Notas"
        "preguntas" -> "Preguntas"
        "ejercicio" -> "Ejercicio"
        "examen" -> "Examen"
        "tarjetas" -> "Tarjetas"
        else -> tipo.replaceFirstChar { it.titlecase() }
    }

    private fun promedioCalificaciones(valores: List<String>): Int? {
        val numeros = valores.mapNotNull { texto ->
            Regex("(\\d+)").find(texto)?.groupValues?.get(1)?.toIntOrNull()
        }
        if (numeros.isEmpty()) return null
        return numeros.sum() / numeros.size
    }
}
