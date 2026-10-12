package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.laboratoriofisico.app.backend.local.entity.DocumentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(documento: DocumentoEntity): Long

    @Update
    suspend fun update(documento: DocumentoEntity)

    @Query("SELECT * FROM documentos WHERE id = :id")
    fun observeById(id: Long): Flow<DocumentoEntity?>

    @Query("SELECT * FROM documentos WHERE usuarioId = :usuarioId ORDER BY fechaImportacion DESC")
    fun observeByUsuario(usuarioId: Long): Flow<List<DocumentoEntity>>

    @Delete
    suspend fun delete(documento: DocumentoEntity)

    @Query("DELETE FROM documentos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT * FROM documentos
        WHERE estadoProcesamiento = 'demo' OR nombreArchivo LIKE '%demo%'
        """
    )
    suspend fun findDemoDocumentos(): List<DocumentoEntity>
}
