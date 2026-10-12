package com.laboratoriofisico.app.reportes

import android.content.Context
import com.laboratoriofisico.app.core.util.DateTimeUtils
import java.io.File

interface ReporteService {
    suspend fun exportarResultadoPdf(resultadoId: Long, usuarioId: Long): ExportResult
    suspend fun exportarExperimentoPdf(experimentoId: Long, usuarioId: Long): ExportResult
    suspend fun exportarResultadosCsv(usuarioId: Long, resultadoIds: List<Long>): ExportResult
    suspend fun exportarResultadosPeriodoPdf(
        usuarioId: Long,
        resultadoIds: List<Long>,
        filtro: String? = null
    ): ExportResult
    suspend fun exportarHistorialEstudioPdf(documentoId: Long, usuarioId: Long): ExportResult
}

class RealReporteService(
    private val context: Context,
    private val reporteDataRepository: ReporteDataRepository,
    private val resultadoPdfGenerator: ResultadoPdfGenerator = ResultadoPdfGenerator(),
    private val experimentoPdfGenerator: ExperimentoPdfGenerator = ExperimentoPdfGenerator(),
    private val historialEstudioPdfGenerator: HistorialEstudioPdfGenerator = HistorialEstudioPdfGenerator(),
    private val resultadoCsvGenerator: ResultadoCsvGenerator = ResultadoCsvGenerator(),
    private val resultadosPeriodoPdfGenerator: ResultadosPeriodoPdfGenerator = ResultadosPeriodoPdfGenerator()
) : ReporteService {

    override suspend fun exportarResultadoPdf(resultadoId: Long, usuarioId: Long): ExportResult {
        val data = reporteDataRepository.loadResultadoExport(resultadoId, usuarioId)
            ?: return ExportResult.Error("No se encontró el resultado para exportar.")

        return runCatching {
            val fileNameBase = "resultado_${data.experimento.titulo}_${resultadoId}"
            val file = ExportFileHelper.createExportFile(context, fileNameBase, "pdf")
            resultadoPdfGenerator.generar(data, file)
            ExportResult.Success(
                uri = ExportFileHelper.contentUri(context, file),
                fileName = file.name
            )
        }.getOrElse { error ->
            ExportResult.Error("No se pudo generar el PDF: ${error.message ?: "error desconocido"}")
        }
    }

    override suspend fun exportarExperimentoPdf(experimentoId: Long, usuarioId: Long): ExportResult {
        val data = reporteDataRepository.loadExperimentoExport(experimentoId, usuarioId)
            ?: return ExportResult.Error("No se encontró el experimento para exportar.")

        if (data.totalMediciones == 0 && data.totalResultados == 0) {
            return ExportResult.Error("Este experimento no tiene mediciones ni resultados para exportar.")
        }

        return runCatching {
            val fileNameBase = "experimento_${data.experimento.titulo}_${experimentoId}"
            val file = ExportFileHelper.createExportFile(context, fileNameBase, "pdf")
            experimentoPdfGenerator.generar(data, file)
            ExportResult.Success(
                uri = ExportFileHelper.contentUri(context, file),
                fileName = file.name
            )
        }.getOrElse { error ->
            ExportResult.Error("No se pudo generar el PDF: ${error.message ?: "error desconocido"}")
        }
    }

    override suspend fun exportarResultadosCsv(usuarioId: Long, resultadoIds: List<Long>): ExportResult {
        if (resultadoIds.isEmpty()) {
            return ExportResult.Error("No tenés resultados para exportar todavía.")
        }

        val rows = reporteDataRepository.loadResultadosCsvRows(resultadoIds, usuarioId)
        if (rows.isEmpty()) {
            return ExportResult.Error("No tenés resultados para exportar todavía.")
        }

        return runCatching {
            val file = ExportFileHelper.createExportFile(context, "resultados_laboratorio", "csv")
            resultadoCsvGenerator.generar(rows, file)
            ExportResult.Success(
                uri = ExportFileHelper.contentUri(context, file),
                fileName = file.name
            )
        }.getOrElse { error ->
            ExportResult.Error("No se pudo generar el CSV: ${error.message ?: "error desconocido"}")
        }
    }

    override suspend fun exportarResultadosPeriodoPdf(
        usuarioId: Long,
        resultadoIds: List<Long>,
        filtro: String?
    ): ExportResult {
        if (resultadoIds.isEmpty()) {
            return ExportResult.Error("Todavía no hay resultados para armar el PDF.")
        }
        val datos = resultadoIds.mapNotNull { reporteDataRepository.loadResultadoExport(it, usuarioId) }
        if (datos.isEmpty()) {
            return ExportResult.Error("Todavía no hay resultados para armar el PDF.")
        }
        val items = datos.map(ResultadosPeriodo::desde).sortedBy { it.timestamp }
        val estudiante = datos.first().usuarioNombre
        return runCatching {
            val file = ExportFileHelper.createExportFile(context, "resultados_por_dia", "pdf")
            resultadosPeriodoPdfGenerator.generar(
                items = items,
                destino = file,
                estudiante = estudiante,
                filtro = filtro?.takeIf { it.isNotBlank() },
                generadoEn = DateTimeUtils.format(System.currentTimeMillis())
            )
            ExportResult.Success(
                uri = ExportFileHelper.contentUri(context, file),
                fileName = file.name
            )
        }.getOrElse { error ->
            ExportResult.Error("No se pudo crear el PDF: ${error.message ?: "error desconocido"}")
        }
    }

    override suspend fun exportarHistorialEstudioPdf(documentoId: Long, usuarioId: Long): ExportResult {
        val data = reporteDataRepository.loadHistorialEstudioExport(documentoId, usuarioId)
            ?: return ExportResult.Error("No se encontró el documento para exportar.")

        if (data.sesiones.isEmpty()) {
            return ExportResult.Error("No hay sesiones de estudio registradas para exportar.")
        }

        return runCatching {
            val fileNameBase = "historial_estudio_${data.documento.nombre}_${documentoId}"
            val file = ExportFileHelper.createExportFile(context, fileNameBase, "pdf")
            historialEstudioPdfGenerator.generar(data, file)
            ExportResult.Success(
                uri = ExportFileHelper.contentUri(context, file),
                fileName = file.name
            )
        }.getOrElse { error ->
            ExportResult.Error("No se pudo generar el PDF: ${error.message ?: "error desconocido"}")
        }
    }
}

class StubReporteService : ReporteService {
    override suspend fun exportarResultadoPdf(resultadoId: Long, usuarioId: Long): ExportResult =
        ExportResult.Error("Servicio de reportes no configurado.")

    override suspend fun exportarExperimentoPdf(experimentoId: Long, usuarioId: Long): ExportResult =
        ExportResult.Error("Servicio de reportes no configurado.")

    override suspend fun exportarResultadosCsv(usuarioId: Long, resultadoIds: List<Long>): ExportResult =
        ExportResult.Error("Servicio de reportes no configurado.")

    override suspend fun exportarResultadosPeriodoPdf(
        usuarioId: Long,
        resultadoIds: List<Long>,
        filtro: String?
    ): ExportResult = ExportResult.Error("Servicio de reportes no configurado.")

    override suspend fun exportarHistorialEstudioPdf(documentoId: Long, usuarioId: Long): ExportResult =
        ExportResult.Error("Servicio de reportes no configurado.")
}
