package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.TarjetaEstudioDao
import com.laboratoriofisico.app.backend.local.entity.TarjetaEstudioEntity
import kotlinx.coroutines.flow.Flow

class TarjetaEstudioRepository(
    private val tarjetaEstudioDao: TarjetaEstudioDao
) {
    suspend fun insert(tarjeta: TarjetaEstudioEntity): Long = tarjetaEstudioDao.insert(tarjeta)

    fun observeByDocumento(documentoId: Long): Flow<List<TarjetaEstudioEntity>> =
        tarjetaEstudioDao.observeByDocumento(documentoId)

    suspend fun delete(tarjeta: TarjetaEstudioEntity) = tarjetaEstudioDao.delete(tarjeta)

    suspend fun deleteByDocumento(documentoId: Long) =
        tarjetaEstudioDao.deleteByDocumento(documentoId)
}
