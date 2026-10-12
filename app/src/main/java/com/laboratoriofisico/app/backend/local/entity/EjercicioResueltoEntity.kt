package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ejercicios_resueltos",
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
        Index("fechaCreacion")
    ]
)
data class EjercicioResueltoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val fotoPath: String,
    val materia: String,
    val tema: String,
    val enunciado: String,
    val datosJson: String,
    val incognita: String,
    val formula: String,
    val formulaLegible: String = "",
    val variablesJson: String = "[]",
    val explicacionFormula: String = "",
    val pasosJson: String,
    val resultadoValor: Double,
    val resultadoUnidad: String,
    val verificacion: String,
    val confianza: String,
    val fechaCreacion: Long
)

@Entity(
    tableName = "juego_ejercicio_progreso",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EjercicioResueltoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ejercicioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("usuarioId"),
        Index("ejercicioId"),
        Index(value = ["usuarioId", "ejercicioId"], unique = true)
    ]
)
data class JuegoEjercicioProgresoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val ejercicioId: Long,
    val intentos: Int,
    val estrellas: Int,
    val pistasUsadas: Int,
    val completo: Boolean,
    val fechaActualizacion: Long
)
