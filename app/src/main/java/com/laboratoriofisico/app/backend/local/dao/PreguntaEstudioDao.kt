package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.PreguntaEstudioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreguntaEstudioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pregunta: PreguntaEstudioEntity): Long

    @Query("SELECT * FROM preguntas_estudio WHERE documentoId = :documentoId ORDER BY fechaCreacion DESC")
    fun observeByDocumento(documentoId: Long): Flow<List<PreguntaEstudioEntity>>

    @Delete
    suspend fun delete(pregunta: PreguntaEstudioEntity)

    @Query("DELETE FROM preguntas_estudio WHERE documentoId = :documentoId")
    suspend fun deleteByDocumento(documentoId: Long)
}
