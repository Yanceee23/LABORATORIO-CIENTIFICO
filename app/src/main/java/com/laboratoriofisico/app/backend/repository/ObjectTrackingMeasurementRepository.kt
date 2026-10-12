package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.local.dao.ExperimentoDao
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.fisica.ResultadoMetadata
import com.laboratoriofisico.app.frontend.viewmodel.ObjectTrackingUiState
import com.laboratoriofisico.app.sensores.model.SensorType

data class SavedObjectTrackingMeasurement(
    val experimentoId: Long,
    val medicionId: Long,
    val resultadoId: Long
)

class ObjectTrackingMeasurementRepository(
    private val sessionRepository: SessionRepository,
    private val experimentoDao: ExperimentoDao,
    private val experimentoRepository: ExperimentoRepository,
    private val medicionRepository: MedicionRepository,
    private val resultadoRepository: ResultadoRepository
) {
    suspend fun guardar(state: ObjectTrackingUiState): Result<SavedObjectTrackingMeasurement> = runCatching {
        val resultado = state.resultado ?: error("Detén la medición antes de guardar.")
        val usuarioId = sessionRepository.getLoggedInUserId()
            ?: error(AuthMessages.INVITADO_BLOQUEADO)
        val experimentoId = findOrCreateExperiment(usuarioId)
        val timestamp = System.currentTimeMillis()
        val primaryDistance = state.distanciaCm ?: resultado.distanciaPx
        val primaryUnit = if (state.distanciaCm != null) "cm" else "px"
        val medicionId = medicionRepository.insert(
            Medicion(
                experimentoId = experimentoId,
                usuarioId = usuarioId,
                nombre = "Movimiento de objeto",
                valor = primaryDistance,
                unidad = primaryUnit,
                timestamp = timestamp
            )
        )
        val resultadoId = resultadoRepository.insert(
            Resultado(
                medicionId = medicionId,
                usuarioId = usuarioId,
                valorCalculado = state.velocidadMediaPxPorSegundo ?: resultado.velocidadMediaPxPorSegundo ?: 0.0,
                unidad = if (state.calibracion != null) "cm/s" else "px/s",
                notas = ResultadoMetadata.buildExtended(
                    sensor = SensorType.CAMERA,
                    numericExtras = buildMap {
                        put("duracion_s", resultado.tiempoSegundos)
                        put("x_inicial_px", resultado.initialX.toDouble())
                        put("y_inicial_px", resultado.initialY.toDouble())
                        put("x_final_px", resultado.finalX.toDouble())
                        put("y_final_px", resultado.finalY.toDouble())
                        put("delta_x_px", resultado.deltaX.toDouble())
                        put("delta_y_px", resultado.deltaY.toDouble())
                        put("desplazamiento_px", state.desplazamientoPx ?: resultado.distanciaPx)
                        put("distancia_total_px", state.distanciaTotalPx ?: resultado.distanciaPx)
                        put("velocidad_actual_px_s", state.velocidadPxPorSegundo ?: 0.0)
                        put("velocidad_media_px_s", state.velocidadMediaPxPorSegundo ?: resultado.velocidadMediaPxPorSegundo ?: 0.0)
                        put("velocidad_maxima_px_s", state.velocidadMaximaPxPorSegundo ?: 0.0)
                        put("veces_movimiento", state.vecesMovimiento.toDouble())
                        state.distanciaCm?.let { put("desplazamiento_cm", it) }
                        state.distanciaTotalCm?.let { put("distancia_total_cm", it) }
                    },
                    textExtras = mapOf(
                        "estado" to if (resultado.detectado) "Objeto detectado" else "Objeto perdido",
                        "calibracion" to if (state.calibracion != null) "cm disponible" else "sin calibracion"
                    )
                ),
                timestamp = timestamp + 1
            )
        )
        SavedObjectTrackingMeasurement(
            experimentoId = experimentoId,
            medicionId = medicionId,
            resultadoId = resultadoId
        )
    }

    private suspend fun findOrCreateExperiment(usuarioId: Long): Long {
        experimentoDao.findByUsuarioAndTitulo(usuarioId, TITLE)?.let { return it.id }
        return experimentoRepository.insert(
            Experimento(
                titulo = TITLE,
                descripcion = "Seguimiento visual con cámara trasera",
                usuarioId = usuarioId,
                ubicacionId = null,
                estado = EstadoExperimento.EN_PROGRESO,
                fechaCreacion = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val TITLE = "Análisis de movimiento de objetos"
    }
}
