package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.ConversacionIADao
import com.laboratoriofisico.app.backend.local.entity.ConversacionIAEntity
import kotlinx.coroutines.flow.Flow

class ConversacionIARepository(
    private val conversacionIADao: ConversacionIADao
) {
    suspend fun insert(conversacion: ConversacionIAEntity): Long = conversacionIADao.insert(conversacion)

    fun observeByUsuario(usuarioId: Long): Flow<List<ConversacionIAEntity>> =
        conversacionIADao.observeByUsuario(usuarioId)

    fun observeChatByUsuario(usuarioId: Long): Flow<List<ConversacionIAEntity>> =
        conversacionIADao.observeChatByUsuario(usuarioId)

    fun observeByUsuarioAndDocumento(usuarioId: Long, documentoId: Long): Flow<List<ConversacionIAEntity>> =
        conversacionIADao.observeByUsuarioAndDocumento(usuarioId, documentoId)

    suspend fun delete(conversacion: ConversacionIAEntity) = conversacionIADao.delete(conversacion)

    suspend fun deleteByUsuario(usuarioId: Long) = conversacionIADao.deleteByUsuario(usuarioId)
}
