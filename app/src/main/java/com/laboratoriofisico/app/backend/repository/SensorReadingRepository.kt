package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.local.dao.SensorReadingDao
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.validation.SensorReadingValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SensorReadingRepository(
    private val sensorReadingDao: SensorReadingDao,
    private val sessionRepository: SessionRepository
) {
    suspend fun guardar(reading: SensorReading): Long? {
        if (!SensorReadingValidator.isValid(reading)) return null
        val usuarioId = sessionRepository.getLoggedInUserId() ?: return null
        return sensorReadingDao.insert(reading.toEntity(usuarioId))
    }

    fun observeAll(): Flow<List<SensorReading>> =
        sensorReadingDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeBySensor(sensorType: SensorType): Flow<List<SensorReading>> =
        sensorReadingDao.observeBySensor(sensorType.name).map { list -> list.map { it.toDomain() } }

    fun observeBySensorAndInterval(
        sensorType: SensorType,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<SensorReading>> =
        sensorReadingDao.observeBySensorAndInterval(
            sensorType.name,
            fromTimestamp,
            toTimestamp
        ).map { list -> list.map { it.toDomain() } }

    fun observeCountBySensor(sensorType: SensorType): Flow<Int> =
        sensorReadingDao.observeCountBySensor(sensorType.name)

    suspend fun eliminar(reading: SensorReading) {
        val usuarioId = sessionRepository.getLoggedInUserId() ?: return
        sensorReadingDao.delete(reading.toEntity(usuarioId))
    }

    suspend fun eliminarPorSensor(sensorType: SensorType) {
        sensorReadingDao.deleteBySensor(sensorType.name)
    }

    suspend fun eliminarAnterioresA(beforeTimestamp: Long) {
        sensorReadingDao.deleteOlderThan(beforeTimestamp)
    }
}
