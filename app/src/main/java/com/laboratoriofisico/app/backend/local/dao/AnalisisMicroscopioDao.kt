package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.AnalisisMicroscopioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalisisMicroscopioDao {
    @Query(
        """
        SELECT * FROM analisis_microscopio
        WHERE experimentoId = :experimentoId
        ORDER BY timestamp DESC
        """
    )
    fun observeByExperimento(experimentoId: Long): Flow<List<AnalisisMicroscopioEntity>>

    @Query(
        """
        SELECT * FROM analisis_microscopio
        WHERE experimentoId = :experimentoId
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findLatestByExperimento(experimentoId: Long): AnalisisMicroscopioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analisis: AnalisisMicroscopioEntity): Long
}
