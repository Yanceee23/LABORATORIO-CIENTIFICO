package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.HistorialEstudioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialEstudioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(historial: HistorialEstudioEntity): Long

    @Query("SELECT * FROM historial_estudio WHERE documentoId = :documentoId ORDER BY fecha DESC")
    fun observeByDocumento(documentoId: Long): Flow<List<HistorialEstudioEntity>>

    @Delete
    suspend fun delete(historial: HistorialEstudioEntity)

    @Query("DELETE FROM historial_estudio WHERE documentoId = :documentoId")
    suspend fun deleteByDocumento(documentoId: Long)
}
