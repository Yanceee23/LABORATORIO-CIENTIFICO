package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.UsuarioDao
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Usuario
import com.laboratoriofisico.app.backend.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class UsuarioRepository(
    private val usuarioDao: UsuarioDao,
    private val sessionRepository: SessionRepository
) {
    fun observeSesion(): Flow<Usuario?> =
        sessionRepository.sessionUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(null)
            } else {
                usuarioDao.observeById(userId).map { it?.toDomain() }
            }
        }

    fun observeById(id: Long): Flow<Usuario?> =
        usuarioDao.observeById(id).map { it?.toDomain() }

    suspend fun insert(usuario: Usuario): Long =
        usuarioDao.insert(usuario.toEntity())

    suspend fun update(usuario: Usuario) {
        usuarioDao.updateProfile(
            id = usuario.id,
            nombre = usuario.nombre.trim(),
            email = usuario.email.trim().lowercase(),
            rol = usuario.rol.trim()
        )
    }

    suspend fun guardar(usuario: Usuario) {
        if (usuario.id == 0L) {
            insert(usuario)
        } else {
            update(usuario)
        }
    }

    suspend fun count(): Int = usuarioDao.count()
}
