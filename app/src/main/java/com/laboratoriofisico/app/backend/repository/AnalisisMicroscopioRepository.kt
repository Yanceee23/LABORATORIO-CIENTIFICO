package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.AnalisisMicroscopioDao
import com.laboratoriofisico.app.backend.local.dao.ExperimentoDao
import com.laboratoriofisico.app.backend.local.entity.ExperimentoEntity
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.AnalisisMicroscopio
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnalisisMicroscopioRepository(
    private val analisisDao: AnalisisMicroscopioDao,
    private val experimentoDao: ExperimentoDao,
    private val sessionRepository: SessionRepository
) {
    suspend fun ensureExperimento(tipo: TipoAnalisisMicroscopio): Long {
        if (sessionRepository.isGuest()) {
            throw IllegalStateException("Esta función requiere una cuenta. Crea una cuenta o inicia sesión.")
        }
        val usuarioId = sessionRepository.getLoggedInUserId()
            ?: throw IllegalStateException("Inicia sesión para guardar análisis de laboratorio.")
        val titulo = "Microscopio - ${tipo.titulo}"
        val existente = experimentoDao.findByUsuarioAndTitulo(usuarioId, titulo)
        if (existente != null) return existente.id

        return experimentoDao.insert(
            ExperimentoEntity(
                titulo = titulo,
                descripcion = "Análisis con microscopio USB y cámara del teléfono.",
                usuarioId = usuarioId,
                ubicacionId = null,
                estado = EstadoExperimento.EN_PROGRESO.name,
                fechaCreacion = System.currentTimeMillis()
            )
        )
    }

    fun observeByExperimento(experimentoId: Long): Flow<List<AnalisisMicroscopio>> =
        analisisDao.observeByExperimento(experimentoId).map { list -> list.map { it.toDomain() } }

    suspend fun latestByExperimento(experimentoId: Long): AnalisisMicroscopio? =
        analisisDao.findLatestByExperimento(experimentoId)?.toDomain()

    suspend fun insert(analisis: AnalisisMicroscopio): Long =
        analisisDao.insert(analisis.toEntity())
}
