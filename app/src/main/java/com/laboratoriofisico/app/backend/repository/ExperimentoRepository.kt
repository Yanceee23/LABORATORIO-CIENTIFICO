package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.ExperimentoDao
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.ResumenDashboard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ExperimentoRepository(
    private val experimentoDao: ExperimentoDao,
    private val medicionRepository: MedicionRepository
) {
    fun observeAll(): Flow<List<Experimento>> =
        experimentoDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Experimento?> =
        experimentoDao.observeById(id).map { it?.toDomain() }

    fun observeResumen(): Flow<ResumenDashboard> =
        combine(
            experimentoDao.observeCount(),
            medicionRepository.observeCount(),
            experimentoDao.observeCountByEstado(EstadoExperimento.EN_PROGRESO.name)
        ) { totalExperimentos, totalMediciones, activos ->
            ResumenDashboard(
                totalExperimentos = totalExperimentos,
                totalMediciones = totalMediciones,
                experimentosActivos = activos
            )
        }

    suspend fun insert(experimento: Experimento): Long =
        experimentoDao.insert(experimento.toEntity())

    suspend fun update(experimento: Experimento) {
        experimentoDao.update(experimento.toEntity())
    }

    suspend fun delete(experimento: Experimento) {
        experimentoDao.delete(experimento.toEntity())
    }
}
