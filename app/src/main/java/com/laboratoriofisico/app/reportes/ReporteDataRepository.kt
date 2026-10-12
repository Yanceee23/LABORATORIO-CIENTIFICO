package com.laboratoriofisico.app.reportes



import com.laboratoriofisico.app.backend.mapper.toDomain

import com.laboratoriofisico.app.backend.repository.DocumentoRepository

import com.laboratoriofisico.app.backend.repository.ExperimentoRepository

import com.laboratoriofisico.app.backend.repository.HistorialEstudioRepository

import com.laboratoriofisico.app.backend.repository.MedicionRepository

import com.laboratoriofisico.app.backend.repository.ResultadoRepository

import com.laboratoriofisico.app.backend.repository.UbicacionRepository

import com.laboratoriofisico.app.backend.repository.UsuarioRepository

import com.laboratoriofisico.app.reportes.model.ExperimentoExportData

import com.laboratoriofisico.app.reportes.model.HistorialEstudioExportData

import com.laboratoriofisico.app.reportes.model.MedicionExportRow

import com.laboratoriofisico.app.reportes.model.ResultadoCsvRow

import com.laboratoriofisico.app.reportes.model.ResultadoExportData

import com.laboratoriofisico.app.reportes.model.ResultadoExportRow

import com.laboratoriofisico.app.reportes.model.toCsvRow

import kotlinx.coroutines.flow.first



class ReporteDataRepository(

    private val resultadoRepository: ResultadoRepository,

    private val medicionRepository: MedicionRepository,

    private val experimentoRepository: ExperimentoRepository,

    private val usuarioRepository: UsuarioRepository,

    private val ubicacionRepository: UbicacionRepository,

    private val documentoRepository: DocumentoRepository,

    private val historialEstudioRepository: HistorialEstudioRepository

) {

    suspend fun loadResultadoExport(resultadoId: Long, usuarioId: Long): ResultadoExportData? {

        val resultado = resultadoRepository.getById(resultadoId) ?: return null

        val medicion = medicionRepository.getById(resultado.medicionId) ?: return null

        val experimento = experimentoRepository.getById(medicion.experimentoId) ?: return null

        if (experimento.usuarioId != usuarioId) return null

        val usuario = usuarioRepository.getById(usuarioId) ?: return null

        val parsed = ResultadoNotasParser.parse(resultado.notas)



        return ResultadoExportData(

            resultado = resultado,

            medicion = medicion,

            experimento = experimento,

            usuarioNombre = usuario.nombre,

            formulaId = parsed.formulaId,

            sensor = parsed.sensor,

            valoresNumericos = parsed.valoresNumericos,

            valoresTexto = parsed.valoresTexto

        )

    }



    suspend fun loadExperimentoExport(experimentoId: Long, usuarioId: Long): ExperimentoExportData? {

        val experimento = experimentoRepository.getById(experimentoId) ?: return null

        if (experimento.usuarioId != usuarioId) return null

        val usuario = usuarioRepository.getById(usuarioId) ?: return null

        val ubicacion = experimento.ubicacionId?.let { id ->

            ubicacionRepository.observeAll().first().firstOrNull { it.id == id }

        }

        val mediciones = medicionRepository.observeConResultados(experimentoId).first()

            .map { row ->

                MedicionExportRow(

                    medicion = row.medicion,

                    resultados = row.resultados.map { resultado ->

                        val parsed = ResultadoNotasParser.parse(resultado.notas)

                        ResultadoExportRow(

                            resultado = resultado,

                            formulaId = parsed.formulaId,

                            sensor = parsed.sensor

                        )

                    }

                )

            }



        return ExperimentoExportData(

            experimento = experimento,

            usuarioNombre = usuario.nombre,

            ubicacion = ubicacion,

            mediciones = mediciones

        )

    }



    suspend fun loadHistorialEstudioExport(documentoId: Long, usuarioId: Long): HistorialEstudioExportData? {

        val documento = documentoRepository.getById(documentoId) ?: return null

        if (documento.usuarioId != usuarioId) return null

        val usuario = usuarioRepository.getById(usuarioId) ?: return null

        val sesiones = historialEstudioRepository.observeByDocumento(documentoId).first()

            .map { it.toDomain() }



        return HistorialEstudioExportData(

            documento = documento,

            usuarioNombre = usuario.nombre,

            sesiones = sesiones

        )

    }



    suspend fun loadResultadosCsvRows(resultadoIds: List<Long>, usuarioId: Long): List<ResultadoCsvRow> =

        resultadoIds.mapNotNull { id ->

            loadResultadoExport(id, usuarioId)?.toCsvRow()

        }

}



private suspend fun ExperimentoRepository.getById(id: Long) =

    observeById(id).first()



private suspend fun UsuarioRepository.getById(id: Long) =

    observeById(id).first()


