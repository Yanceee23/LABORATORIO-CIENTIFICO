package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.SeccionDocumentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeccionDocumentoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(seccion: SeccionDocumentoEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(secciones: List<SeccionDocumentoEntity>)

    @Query("SELECT * FROM secciones_documento WHERE documentoId = :documentoId ORDER BY orden ASC")
    fun observeByDocumento(documentoId: Long): Flow<List<SeccionDocumentoEntity>>

    @Delete
    suspend fun delete(seccion: SeccionDocumentoEntity)

    @Query("DELETE FROM secciones_documento WHERE documentoId = :documentoId")
    suspend fun deleteByDocumento(documentoId: Long)
}
