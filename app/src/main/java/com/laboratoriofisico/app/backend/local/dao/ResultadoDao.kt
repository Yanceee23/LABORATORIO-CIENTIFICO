package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.ResultadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultadoDao {
    @Query("SELECT * FROM resultados WHERE medicionId = :medicionId ORDER BY timestamp DESC")
    fun observeByMedicion(medicionId: Long): Flow<List<ResultadoEntity>>

    @Query("SELECT * FROM resultados WHERE id = :id")
    fun observeById(id: Long): Flow<ResultadoEntity?>

    @Query("SELECT * FROM resultados ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ResultadoEntity>>

    @Query(
        """
        SELECT r.* FROM resultados r
        INNER JOIN mediciones m ON r.medicionId = m.id
        WHERE m.experimentoId = :experimentoId
        ORDER BY r.timestamp DESC
        LIMIT 1
        """
    )
    fun observeLatestByExperimento(experimentoId: Long): Flow<ResultadoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resultado: ResultadoEntity): Long

    @Query(
        """
        SELECT * FROM resultados
        WHERE notas LIKE '%demo%' OR notas LIKE '%Demo%'
        """
    )
    suspend fun findDemoResultados(): List<ResultadoEntity>

    @Query(
        """
        DELETE FROM resultados
        WHERE notas LIKE '%demo%' OR notas LIKE '%Demo%'
        """
    )
    suspend fun deleteDemoResultados()
}
