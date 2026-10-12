package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.local.dao.ExperimentoDao
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.experimentos.sensores.ProcessedSensorExperiment
import com.laboratoriofisico.app.fisica.ResultadoMetadata
import com.laboratoriofisico.app.sensores.model.SensorType

data class SavedSensorExperimentResult(
    val experimentoId: Long,
    val medicionId: Long,
    val resultadoId: Long,
    val rawReadingsSaved: Int
)

class SensorPhysicsExperimentRepository(
    private val experimentoDao: ExperimentoDao,
    private val experimentoRepository: ExperimentoRepository,
    private val medicionRepository: MedicionRepository,
    private val resultadoRepository: ResultadoRepository,
    private val sensorReadingRepository: SensorReadingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun guardar(processed: ProcessedSensorExperiment): SavedSensorExperimentResult {
        val usuarioId = sessionRepository.getLoggedInUserId()
            ?: error(AuthMessages.INVITADO_BLOQUEADO)
        val experimentoId = getOrCreateExperiment(processed, usuarioId)
        var rawSaved = 0
        processed.samples.forEach { sample ->
            if (sensorReadingRepository.guardar(sample.reading) != null) {
                rawSaved++
            }
        }

        val timestamp = processed.samples.lastOrNull()?.timestampMs ?: System.currentTimeMillis()
        val medicionId = medicionRepository.insert(
            Medicion(
                experimentoId = experimentoId,
                usuarioId = usuarioId,
                nombre = processed.primaryName,
                valor = processed.primaryValue,
                unidad = processed.primaryUnit,
                timestamp = timestamp
            )
        )
        val resultadoId = resultadoRepository.insert(
            Resultado(
                medicionId = medicionId,
                usuarioId = usuarioId,
                valorCalculado = processed.primaryValue,
                unidad = processed.primaryUnit,
                notas = ResultadoMetadata.buildExtended(
                    sensor = SensorType.ACCELEROMETER,
                    numericExtras = processed.numericResults,
                    textExtras = processed.textResults + mapOf(
                        "experimento_sensor" to processed.type.name,
                        "fuente_datos" to "event.values reales"
                    )
                ),
                timestamp = timestamp
            )
        )
        return SavedSensorExperimentResult(
            experimentoId = experimentoId,
            medicionId = medicionId,
            resultadoId = resultadoId,
            rawReadingsSaved = rawSaved
        )
    }

    private suspend fun getOrCreateExperiment(
        processed: ProcessedSensorExperiment,
        usuarioId: Long
    ): Long {
        val existing = experimentoDao.findByUsuarioAndTitulo(usuarioId, processed.type.title)
        if (existing != null) return existing.toDomain().id

        return experimentoRepository.insert(
            Experimento(
                titulo = processed.type.title,
                descripcion = processed.type.description,
                usuarioId = usuarioId,
                ubicacionId = null,
                estado = EstadoExperimento.EN_PROGRESO,
                fechaCreacion = System.currentTimeMillis()
            )
        )
    }
}
