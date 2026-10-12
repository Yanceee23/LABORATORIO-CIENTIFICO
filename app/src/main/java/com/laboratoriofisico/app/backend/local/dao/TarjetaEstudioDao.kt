package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.TarjetaEstudioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TarjetaEstudioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tarjeta: TarjetaEstudioEntity): Long

    @Query("SELECT * FROM tarjetas_estudio WHERE documentoId = :documentoId ORDER BY fechaCreacion DESC")
    fun observeByDocumento(documentoId: Long): Flow<List<TarjetaEstudioEntity>>

    @Delete
    suspend fun delete(tarjeta: TarjetaEstudioEntity)

    @Query("DELETE FROM tarjetas_estudio WHERE documentoId = :documentoId")
    suspend fun deleteByDocumento(documentoId: Long)
}
