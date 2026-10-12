package com.laboratoriofisico.app.estudio

import com.laboratoriofisico.app.backend.model.EjercicioEstudio
import com.laboratoriofisico.app.backend.model.Examen
import com.laboratoriofisico.app.backend.model.ExamenConfig
import com.laboratoriofisico.app.backend.model.PreguntaEstudio
import com.laboratoriofisico.app.backend.model.TarjetaEstudio
import com.laboratoriofisico.app.backend.model.TipoResumen

interface EstudioContentService {
    suspend fun generarResumen(documentoId: Long, tipo: TipoResumen): String
    suspend fun generarNotas(documentoId: Long): String
    suspend fun generarPreguntas(documentoId: Long, dificultad: String): List<PreguntaEstudio>
    suspend fun generarEjercicio(documentoId: Long): EjercicioEstudio
    suspend fun generarTarjetas(documentoId: Long): List<TarjetaEstudio>
    suspend fun crearExamen(documentoId: Long, config: ExamenConfig): Examen
}
