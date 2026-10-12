package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.laboratoriofisico.app.backend.local.entity.MedicionHistorialRow
import com.laboratoriofisico.app.backend.local.entity.MedicionEntity
import com.laboratoriofisico.app.backend.local.entity.MedicionWithResultados
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicionDao {
    @Transaction
    @Query("SELECT * FROM mediciones WHERE experimentoId = :experimentoId ORDER BY timestamp DESC")
    fun observeWithResultados(experimentoId: Long): Flow<List<MedicionWithResultados>>

    @Query("SELECT * FROM mediciones WHERE experimentoId = :experimentoId ORDER BY timestamp DESC")
    fun observeByExperimento(experimentoId: Long): Flow<List<MedicionEntity>>

    @Query(
        """
        SELECT
            m.id AS medicionId,
            m.experimentoId AS experimentoId,
            e.titulo AS experimentoTitulo,
            e.descripcion AS experimentoDescripcion,
            m.nombre AS medicionNombre,
            m.valor AS medicionValor,
            m.unidad AS medicionUnidad,
            m.timestamp AS medicionTimestamp,
            r.id AS resultadoId,
            r.valorCalculado AS resultadoValor,
            r.unidad AS resultadoUnidad,
            r.notas AS resultadoNotas,
            r.timestamp AS resultadoTimestamp
        FROM mediciones m
        INNER JOIN experimentos e ON e.id = m.experimentoId
        LEFT JOIN resultados r ON r.medicionId = m.id
        ORDER BY m.timestamp DESC, r.timestamp DESC
        """
    )
    fun observeHistorial(): Flow<List<MedicionHistorialRow>>

    @Query("SELECT COUNT(*) FROM mediciones")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM mediciones WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MedicionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicion: MedicionEntity): Long

    @Delete
    suspend fun delete(medicion: MedicionEntity)

    @Query("DELETE FROM mediciones WHERE id = :id")
    suspend fun deleteById(id: Long)
}
