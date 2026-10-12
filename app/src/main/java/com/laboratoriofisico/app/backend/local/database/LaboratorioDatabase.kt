package com.laboratoriofisico.app.backend.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.laboratoriofisico.app.backend.local.dao.AnalisisMicroscopioDao
import com.laboratoriofisico.app.backend.local.dao.ConfiguracionDao
import com.laboratoriofisico.app.backend.local.dao.ConversacionIADao
import com.laboratoriofisico.app.backend.local.dao.DocumentoDao
import com.laboratoriofisico.app.backend.local.dao.EjercicioEstudioDao
import com.laboratoriofisico.app.backend.local.dao.EjercicioResueltoDao
import com.laboratoriofisico.app.backend.local.dao.ExperimentoDao
import com.laboratoriofisico.app.backend.local.dao.HistorialEstudioDao
import com.laboratoriofisico.app.backend.local.dao.JuegoEjercicioProgresoDao
import com.laboratoriofisico.app.backend.local.dao.MedicionDao
import com.laboratoriofisico.app.backend.local.dao.PreguntaEstudioDao
import com.laboratoriofisico.app.backend.local.dao.ResultadoDao
import com.laboratoriofisico.app.backend.local.dao.SeccionDocumentoDao
import com.laboratoriofisico.app.backend.local.dao.SensorReadingDao
import com.laboratoriofisico.app.backend.local.dao.TarjetaEstudioDao
import com.laboratoriofisico.app.backend.local.dao.UbicacionDao
import com.laboratoriofisico.app.backend.local.dao.UsuarioDao
import com.laboratoriofisico.app.backend.local.entity.AnalisisMicroscopioEntity
import com.laboratoriofisico.app.backend.local.entity.ConfiguracionEntity
import com.laboratoriofisico.app.backend.local.entity.ConversacionIAEntity
import com.laboratoriofisico.app.backend.local.entity.DocumentoEntity
import com.laboratoriofisico.app.backend.local.entity.EjercicioEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.EjercicioResueltoEntity
import com.laboratoriofisico.app.backend.local.entity.ExperimentoEntity
import com.laboratoriofisico.app.backend.local.entity.HistorialEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.JuegoEjercicioProgresoEntity
import com.laboratoriofisico.app.backend.local.entity.MedicionEntity
import com.laboratoriofisico.app.backend.local.entity.PreguntaEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.ResultadoEntity
import com.laboratoriofisico.app.backend.local.entity.SeccionDocumentoEntity
import com.laboratoriofisico.app.backend.local.entity.SensorReadingEntity
import com.laboratoriofisico.app.backend.local.entity.TarjetaEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.UbicacionEntity
import com.laboratoriofisico.app.backend.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        UbicacionEntity::class,
        ExperimentoEntity::class,
        MedicionEntity::class,
        ResultadoEntity::class,
        ConfiguracionEntity::class,
        SensorReadingEntity::class,
        DocumentoEntity::class,
        SeccionDocumentoEntity::class,
        PreguntaEstudioEntity::class,
        EjercicioEstudioEntity::class,
        TarjetaEstudioEntity::class,
        HistorialEstudioEntity::class,
        ConversacionIAEntity::class,
        AnalisisMicroscopioEntity::class,
        EjercicioResueltoEntity::class,
        JuegoEjercicioProgresoEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class LaboratorioDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun ubicacionDao(): UbicacionDao
    abstract fun experimentoDao(): ExperimentoDao
    abstract fun medicionDao(): MedicionDao
    abstract fun resultadoDao(): ResultadoDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun seccionDocumentoDao(): SeccionDocumentoDao
    abstract fun preguntaEstudioDao(): PreguntaEstudioDao
    abstract fun ejercicioEstudioDao(): EjercicioEstudioDao
    abstract fun tarjetaEstudioDao(): TarjetaEstudioDao
    abstract fun historialEstudioDao(): HistorialEstudioDao
    abstract fun conversacionIADao(): ConversacionIADao
    abstract fun analisisMicroscopioDao(): AnalisisMicroscopioDao
    abstract fun ejercicioResueltoDao(): EjercicioResueltoDao
    abstract fun juegoEjercicioProgresoDao(): JuegoEjercicioProgresoDao

    companion object {
        private const val DATABASE_NAME = "laboratorio.db"

        fun create(context: Context): LaboratorioDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LaboratorioDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14
                )
                .build()
    }
}
