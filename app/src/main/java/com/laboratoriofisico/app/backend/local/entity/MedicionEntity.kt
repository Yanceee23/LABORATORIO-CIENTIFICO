package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mediciones",
    foreignKeys = [
        ForeignKey(
            entity = ExperimentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["experimentoId"],
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
        Index("experimentoId"),
        Index("usuarioId")
    ]
)
data class MedicionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentoId: Long,
    val usuarioId: Long,
    val nombre: String,
    val valor: Double,
    val unidad: String,
    val timestamp: Long
)
