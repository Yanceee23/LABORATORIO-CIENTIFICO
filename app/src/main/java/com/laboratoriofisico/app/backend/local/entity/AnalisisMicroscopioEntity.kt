package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analisis_microscopio",
    foreignKeys = [
        ForeignKey(
            entity = ExperimentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["experimentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("experimentoId"),
        Index("tipoExperimento"),
        Index("timestamp")
    ]
)
data class AnalisisMicroscopioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentoId: Long,
    val tipoExperimento: String,
    val subtipo: String?,
    val imagenPath: String,
    val analisis: String,
    val resumenParaComparacion: String,
    val promptVersion: Int,
    val timestamp: Long
)
