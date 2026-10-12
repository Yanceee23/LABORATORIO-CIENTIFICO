package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_readings",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("usuarioId"),
        Index("sensorType"),
        Index("timestamp")
    ]
)
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val sensorType: String,
    val timestamp: Long,
    val valueX: Double?,
    val valueY: Double?,
    val valueZ: Double?,
    val valueScalar: Double?,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
    val unit: String,
    val metadata: String?
)
