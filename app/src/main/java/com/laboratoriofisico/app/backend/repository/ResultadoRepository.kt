package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.ResultadoDao
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Resultado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ResultadoRepository(
    private val resultadoDao: ResultadoDao
) {
    fun observeAll(): Flow<List<Resultado>> =
        resultadoDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Resultado?> =
        resultadoDao.observeById(id).map { it?.toDomain() }

    fun observeByMedicion(medicionId: Long): Flow<List<Resultado>> =
        resultadoDao.observeByMedicion(medicionId).map { list -> list.map { it.toDomain() } }

    fun observeLatestByExperimento(experimentoId: Long): Flow<Resultado?> =
        resultadoDao.observeLatestByExperimento(experimentoId).map { it?.toDomain() }

    suspend fun insert(resultado: Resultado): Long =
        resultadoDao.insert(resultado.toEntity())

    suspend fun getById(id: Long): Resultado? =
        resultadoDao.observeById(id).map { it?.toDomain() }.first()
}
