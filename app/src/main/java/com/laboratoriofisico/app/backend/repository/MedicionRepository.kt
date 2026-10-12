package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.MedicionDao
import com.laboratoriofisico.app.backend.local.entity.MedicionHistorialRow
import com.laboratoriofisico.app.backend.local.entity.MedicionWithResultados
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.MedicionConResultados
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicionRepository(
    private val medicionDao: MedicionDao
) {
    fun observeByExperimento(experimentoId: Long): Flow<List<Medicion>> =
        medicionDao.observeByExperimento(experimentoId).map { list -> list.map { it.toDomain() } }

    fun observeConResultados(experimentoId: Long): Flow<List<MedicionConResultados>> =
        medicionDao.observeWithResultados(experimentoId).map { list ->
            list.map { it.toDomain() }
        }

    fun observeHistorial(): Flow<List<MedicionHistorialRow>> =
        medicionDao.observeHistorial()

    fun observeCount(): Flow<Int> = medicionDao.observeCount()

    suspend fun insert(medicion: Medicion): Long =
        medicionDao.insert(medicion.toEntity())

    suspend fun getById(id: Long): Medicion? =
        medicionDao.findById(id)?.toDomain()

    suspend fun delete(medicion: Medicion) {
        medicionDao.delete(medicion.toEntity())
    }

    suspend fun deleteById(id: Long) {
        medicionDao.deleteById(id)
    }
}
