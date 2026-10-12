package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.SeccionDocumentoDao
import com.laboratoriofisico.app.backend.local.entity.SeccionDocumentoEntity
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.model.SeccionDocumento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SeccionDocumentoRepository(
    private val seccionDocumentoDao: SeccionDocumentoDao
) {
    suspend fun insert(seccion: SeccionDocumentoEntity): Long = seccionDocumentoDao.insert(seccion)

    suspend fun insertAll(secciones: List<SeccionDocumentoEntity>) =
        seccionDocumentoDao.insertAll(secciones)

    fun observeByDocumento(documentoId: Long): Flow<List<SeccionDocumentoEntity>> =
        seccionDocumentoDao.observeByDocumento(documentoId)

    suspend fun delete(seccion: SeccionDocumentoEntity) = seccionDocumentoDao.delete(seccion)

    suspend fun deleteByDocumento(documentoId: Long) =
        seccionDocumentoDao.deleteByDocumento(documentoId)

    suspend fun getByDocumento(documentoId: Long): List<SeccionDocumento> =
        seccionDocumentoDao.observeByDocumento(documentoId).first().map { it.toDomain() }
}
