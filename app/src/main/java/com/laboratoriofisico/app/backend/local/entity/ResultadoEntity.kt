package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Metadatos de fórmula/sensor se guardan en [notas].
 */
@Entity(
    tableName = "resultados",
    foreignKeys = [
        ForeignKey(
            entity = MedicionEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("medicionId"),
        Index("usuarioId")
    ]
)
data class ResultadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicionId: Long,
    val usuarioId: Long,
    val valorCalculado: Double,
    val unidad: String,
    val notas: String,
    val timestamp: Long
)
