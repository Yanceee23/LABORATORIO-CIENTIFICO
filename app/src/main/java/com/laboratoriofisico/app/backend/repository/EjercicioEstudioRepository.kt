package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.EjercicioEstudioDao
import com.laboratoriofisico.app.backend.local.entity.EjercicioEstudioEntity
import kotlinx.coroutines.flow.Flow

class EjercicioEstudioRepository(
    private val ejercicioEstudioDao: EjercicioEstudioDao
) {
    suspend fun insert(ejercicio: EjercicioEstudioEntity): Long = ejercicioEstudioDao.insert(ejercicio)

    fun observeByDocumento(documentoId: Long): Flow<List<EjercicioEstudioEntity>> =
        ejercicioEstudioDao.observeByDocumento(documentoId)

    suspend fun delete(ejercicio: EjercicioEstudioEntity) = ejercicioEstudioDao.delete(ejercicio)

    suspend fun deleteByDocumento(documentoId: Long) =
        ejercicioEstudioDao.deleteByDocumento(documentoId)
}
