package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.EjercicioEstudioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EjercicioEstudioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ejercicio: EjercicioEstudioEntity): Long

    @Query("SELECT * FROM ejercicios_estudio WHERE documentoId = :documentoId ORDER BY fechaCreacion DESC")
    fun observeByDocumento(documentoId: Long): Flow<List<EjercicioEstudioEntity>>

    @Delete
    suspend fun delete(ejercicio: EjercicioEstudioEntity)

    @Query("DELETE FROM ejercicios_estudio WHERE documentoId = :documentoId")
    suspend fun deleteByDocumento(documentoId: Long)
}
