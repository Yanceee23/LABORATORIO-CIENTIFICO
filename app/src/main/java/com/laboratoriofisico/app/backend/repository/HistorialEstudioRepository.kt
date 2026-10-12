package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.HistorialEstudioDao
import com.laboratoriofisico.app.backend.local.entity.HistorialEstudioEntity
import kotlinx.coroutines.flow.Flow

class HistorialEstudioRepository(
    private val historialEstudioDao: HistorialEstudioDao
) {
    suspend fun insert(historial: HistorialEstudioEntity): Long = historialEstudioDao.insert(historial)

    fun observeByDocumento(documentoId: Long): Flow<List<HistorialEstudioEntity>> =
        historialEstudioDao.observeByDocumento(documentoId)

    suspend fun delete(historial: HistorialEstudioEntity) = historialEstudioDao.delete(historial)

    suspend fun deleteByDocumento(documentoId: Long) =
        historialEstudioDao.deleteByDocumento(documentoId)
}
