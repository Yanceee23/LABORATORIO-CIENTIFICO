package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.EjercicioResueltoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EjercicioResueltoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EjercicioResueltoEntity): Long

    @Query("SELECT * FROM ejercicios_resueltos WHERE usuarioId = :usuarioId ORDER BY fechaCreacion DESC")
    fun observeByUsuario(usuarioId: Long): Flow<List<EjercicioResueltoEntity>>

    @Query(
        """
        SELECT * FROM ejercicios_resueltos
        WHERE usuarioId = :usuarioId AND (:materia IS NULL OR materia = :materia)
        ORDER BY fechaCreacion DESC
        """
    )
    fun observeByUsuarioAndMateria(usuarioId: Long, materia: String?): Flow<List<EjercicioResueltoEntity>>

    @Query("SELECT * FROM ejercicios_resueltos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EjercicioResueltoEntity?

    @Query("SELECT * FROM ejercicios_resueltos WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    suspend fun getByIdAndUsuario(id: Long, usuarioId: Long): EjercicioResueltoEntity?

    @Delete
    suspend fun delete(entity: EjercicioResueltoEntity)

    @Query("DELETE FROM ejercicios_resueltos WHERE id = :id AND usuarioId = :usuarioId")
    suspend fun deleteByIdAndUsuario(id: Long, usuarioId: Long)
}
