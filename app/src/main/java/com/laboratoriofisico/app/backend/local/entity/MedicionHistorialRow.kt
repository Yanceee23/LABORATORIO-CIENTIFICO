package com.laboratoriofisico.app.backend.local.entity

data class MedicionHistorialRow(
    val medicionId: Long,
    val experimentoId: Long,
    val experimentoTitulo: String,
    val experimentoDescripcion: String,
    val medicionNombre: String,
    val medicionValor: Double,
    val medicionUnidad: String,
    val medicionTimestamp: Long,
    val resultadoId: Long?,
    val resultadoValor: Double?,
    val resultadoUnidad: String?,
    val resultadoNotas: String?,
    val resultadoTimestamp: Long?
)
