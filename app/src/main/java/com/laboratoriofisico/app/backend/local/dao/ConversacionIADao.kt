package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.ConversacionIAEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversacionIADao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(conversacion: ConversacionIAEntity): Long

    @Query("SELECT * FROM conversaciones_ia WHERE usuarioId = :usuarioId ORDER BY fechaHora DESC")
    fun observeByUsuario(usuarioId: Long): Flow<List<ConversacionIAEntity>>

    @Query(
        """
        SELECT * FROM conversaciones_ia
        WHERE usuarioId = :usuarioId AND documentoId = :documentoId
        ORDER BY fechaHora DESC
        """
    )
    fun observeByUsuarioAndDocumento(usuarioId: Long, documentoId: Long): Flow<List<ConversacionIAEntity>>

    @Query(
        """
        SELECT * FROM conversaciones_ia
        WHERE usuarioId = :usuarioId AND documentoId IS NULL
        ORDER BY fechaHora ASC
        """
    )
    fun observeChatByUsuario(usuarioId: Long): Flow<List<ConversacionIAEntity>>

    @Delete
    suspend fun delete(conversacion: ConversacionIAEntity)

    @Query("DELETE FROM conversaciones_ia WHERE usuarioId = :usuarioId")
    suspend fun deleteByUsuario(usuarioId: Long)
}
