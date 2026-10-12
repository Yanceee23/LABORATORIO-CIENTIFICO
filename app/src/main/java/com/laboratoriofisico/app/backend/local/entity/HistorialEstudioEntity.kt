package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historial_estudio",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("documentoId"),
        Index("fecha")
    ]
)
data class HistorialEstudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val tipoActividad: String,
    val resultado: String,
    val fecha: Long,
    val tiempoEstudio: Long
)
