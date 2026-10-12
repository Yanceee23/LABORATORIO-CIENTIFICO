package com.laboratoriofisico.app.pdf

import java.io.File

/**
 * Generador de documentos PDF.
 * TODO Fase 4: Implementar exportación de reportes y experimentos.
 */
interface PdfGenerator {
    fun generarReporte(experimentoId: Long, destino: File): Boolean
}
