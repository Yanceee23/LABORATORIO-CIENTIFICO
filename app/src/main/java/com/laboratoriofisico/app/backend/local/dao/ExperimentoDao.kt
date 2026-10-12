package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.laboratoriofisico.app.backend.local.entity.ExperimentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperimentoDao {
    @Query("SELECT * FROM experimentos ORDER BY fechaCreacion DESC")
    fun observeAll(): Flow<List<ExperimentoEntity>>

    @Query("SELECT * FROM experimentos WHERE id = :id")
    fun observeById(id: Long): Flow<ExperimentoEntity?>

    @Query("SELECT COUNT(*) FROM experimentos")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM experimentos WHERE estado = :estado")
    fun observeCountByEstado(estado: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(experimento: ExperimentoEntity): Long

    @Update
    suspend fun update(experimento: ExperimentoEntity)

    @Delete
    suspend fun delete(experimento: ExperimentoEntity)

    @Query("SELECT * FROM experimentos WHERE usuarioId = :usuarioId AND titulo = :titulo LIMIT 1")
    suspend fun findByUsuarioAndTitulo(usuarioId: Long, titulo: String): ExperimentoEntity?

    @Query(
        """
        SELECT * FROM experimentos
        WHERE descripcion LIKE '%demo%' OR titulo = 'Caída libre'
        """
    )
    suspend fun findDemoExperimentos(): List<ExperimentoEntity>
}
