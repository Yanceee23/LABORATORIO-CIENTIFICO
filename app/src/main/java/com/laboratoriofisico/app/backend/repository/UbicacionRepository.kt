package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.UbicacionDao
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Ubicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UbicacionRepository(
    private val ubicacionDao: UbicacionDao
) {
    fun observeAll(): Flow<List<Ubicacion>> =
        ubicacionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Ubicacion?> =
        ubicacionDao.observeById(id).map { it?.toDomain() }

    suspend fun insert(ubicacion: Ubicacion): Long =
        ubicacionDao.insert(ubicacion.toEntity())
}
