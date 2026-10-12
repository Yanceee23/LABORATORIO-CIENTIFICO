package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preguntas_estudio",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentoId")]
)
data class PreguntaEstudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val pregunta: String,
    val respuesta: String,
    val explicacion: String,
    val dificultad: String,
    val fechaCreacion: Long
)
