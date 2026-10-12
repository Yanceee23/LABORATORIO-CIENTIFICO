package com.laboratoriofisico.app.backend.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MedicionWithResultados(
    @Embedded val medicion: MedicionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicionId"
    )
    val resultados: List<ResultadoEntity>
)
