package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "experimentos",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UbicacionEntity::class,
            parentColumns = ["id"],
            childColumns = ["ubicacionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("usuarioId"),
        Index("ubicacionId")
    ]
)
data class ExperimentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val usuarioId: Long,
    val ubicacionId: Long?,
    val estado: String,
    val fechaCreacion: Long
)
