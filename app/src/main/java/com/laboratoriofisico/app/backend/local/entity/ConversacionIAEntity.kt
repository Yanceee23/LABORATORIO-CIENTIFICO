package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversaciones_ia",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("usuarioId"),
        Index("documentoId")
    ]
)
data class ConversacionIAEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val documentoId: Long?,
    val pregunta: String,
    val respuesta: String,
    val fechaHora: Long
)
