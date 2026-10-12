package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.ConfiguracionDao
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Configuracion
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.configuracion.ConfiguracionDefaults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ConfiguracionRepository(
    private val configuracionDao: ConfiguracionDao,
    private val sessionRepository: SessionRepository
) {
    fun observe(): Flow<Configuracion> =
        sessionRepository.sessionUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(ConfiguracionDefaults.crear())
            } else {
                configuracionDao.observeByUsuario(userId).map { entity ->
                    entity?.toDomain() ?: ConfiguracionDefaults.crear(userId)
                }
            }
        }

    suspend fun guardar(configuracion: Configuracion) {
        val userId = sessionRepository.getLoggedInUserId() ?: return
        val existing = configuracionDao.findByUsuario(userId)
        val toSave = configuracion.copy(
            id = existing?.id ?: 0L,
            usuarioId = userId
        )
        if (existing == null) {
            configuracionDao.insert(toSave.toEntity())
        } else {
            configuracionDao.update(toSave.toEntity())
        }
    }

    suspend fun ensureForUsuario(userId: Long) {
        if (configuracionDao.findByUsuario(userId) != null) return
        configuracionDao.insert(ConfiguracionDefaults.crear(userId).toEntity())
    }
}
