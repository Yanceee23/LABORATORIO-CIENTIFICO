package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.PreguntaEstudioDao
import com.laboratoriofisico.app.backend.local.entity.PreguntaEstudioEntity
import kotlinx.coroutines.flow.Flow

class PreguntaEstudioRepository(
    private val preguntaEstudioDao: PreguntaEstudioDao
) {
    suspend fun insert(pregunta: PreguntaEstudioEntity): Long = preguntaEstudioDao.insert(pregunta)

    fun observeByDocumento(documentoId: Long): Flow<List<PreguntaEstudioEntity>> =
        preguntaEstudioDao.observeByDocumento(documentoId)

    suspend fun delete(pregunta: PreguntaEstudioEntity) = preguntaEstudioDao.delete(pregunta)

    suspend fun deleteByDocumento(documentoId: Long) =
        preguntaEstudioDao.deleteByDocumento(documentoId)
}
