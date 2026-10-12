package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.SensorReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorReadingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reading: SensorReadingEntity): Long

    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings WHERE sensorType = :sensorType ORDER BY timestamp DESC")
    fun observeBySensor(sensorType: String): Flow<List<SensorReadingEntity>>

    @Query(
        """
        SELECT * FROM sensor_readings
        WHERE sensorType = :sensorType
        AND timestamp BETWEEN :fromTimestamp AND :toTimestamp
        ORDER BY timestamp ASC
        """
    )
    fun observeBySensorAndInterval(
        sensorType: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<SensorReadingEntity>>

    @Query("SELECT COUNT(*) FROM sensor_readings WHERE sensorType = :sensorType")
    fun observeCountBySensor(sensorType: String): Flow<Int>

    @Delete
    suspend fun delete(reading: SensorReadingEntity)

    @Query("DELETE FROM sensor_readings WHERE sensorType = :sensorType")
    suspend fun deleteBySensor(sensorType: String)

    @Query("DELETE FROM sensor_readings WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
