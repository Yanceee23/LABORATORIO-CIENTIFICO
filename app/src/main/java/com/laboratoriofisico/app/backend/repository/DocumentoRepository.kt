package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.DocumentoDao
import com.laboratoriofisico.app.backend.local.entity.DocumentoEntity
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Documento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DocumentoRepository(
    private val documentoDao: DocumentoDao
) {
    suspend fun insert(documento: DocumentoEntity): Long = documentoDao.insert(documento)

    suspend fun insert(documento: Documento): Long = documentoDao.insert(documento.toEntity())

    suspend fun update(documento: Documento) = documentoDao.update(documento.toEntity())

    fun observeById(id: Long): Flow<Documento?> =
        documentoDao.observeById(id).map { it?.toDomain() }

    fun observeByUsuario(usuarioId: Long): Flow<List<Documento>> =
        documentoDao.observeByUsuario(usuarioId).map { list -> list.map { it.toDomain() } }

    suspend fun delete(documento: Documento) = documentoDao.delete(documento.toEntity())

    suspend fun deleteById(id: Long) = documentoDao.deleteById(id)

    suspend fun getById(id: Long): Documento? =
        documentoDao.observeById(id).map { it?.toDomain() }.first()
}
