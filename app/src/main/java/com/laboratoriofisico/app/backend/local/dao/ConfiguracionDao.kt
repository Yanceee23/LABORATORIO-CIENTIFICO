package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.laboratoriofisico.app.backend.local.entity.ConfiguracionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracionDao {
    @Query("SELECT * FROM configuracion WHERE usuarioId = :usuarioId LIMIT 1")
    fun observeByUsuario(usuarioId: Long): Flow<ConfiguracionEntity?>

    @Query("SELECT COUNT(*) FROM configuracion")
    suspend fun count(): Int

    @Query("SELECT * FROM configuracion WHERE usuarioId = :usuarioId LIMIT 1")
    suspend fun findByUsuario(usuarioId: Long): ConfiguracionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(configuracion: ConfiguracionEntity): Long

    @Update
    suspend fun update(configuracion: ConfiguracionEntity)
}
