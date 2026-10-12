package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.JuegoEjercicioProgresoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JuegoEjercicioProgresoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: JuegoEjercicioProgresoEntity): Long

    @Query(
        "SELECT * FROM juego_ejercicio_progreso WHERE usuarioId = :usuarioId AND ejercicioId = :ejercicioId LIMIT 1"
    )
    suspend fun getByUsuarioAndEjercicio(usuarioId: Long, ejercicioId: Long): JuegoEjercicioProgresoEntity?

    @Query("SELECT * FROM juego_ejercicio_progreso WHERE usuarioId = :usuarioId")
    fun observeByUsuario(usuarioId: Long): Flow<List<JuegoEjercicioProgresoEntity>>

    @Query("SELECT * FROM juego_ejercicio_progreso WHERE usuarioId = :usuarioId")
    suspend fun getAllByUsuario(usuarioId: Long): List<JuegoEjercicioProgresoEntity>
}
