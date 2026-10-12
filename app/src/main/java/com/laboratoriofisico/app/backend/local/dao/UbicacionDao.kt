package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.UbicacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UbicacionDao {
    @Query("SELECT * FROM ubicaciones ORDER BY nombre ASC")
    fun observeAll(): Flow<List<UbicacionEntity>>

    @Query("SELECT * FROM ubicaciones WHERE id = :id")
    fun observeById(id: Long): Flow<UbicacionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ubicacion: UbicacionEntity): Long
}
