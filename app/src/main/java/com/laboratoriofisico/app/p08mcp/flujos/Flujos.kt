package com.laboratoriofisico.app.p08mcp.flujos

import com.laboratoriofisico.app.backend.model.IaRespuesta

interface PreguntaSobrePDF {
    suspend fun ejecutar(documentoId: Long, pregunta: String): IaRespuesta
}

interface EstudiarPDF {
    suspend fun ejecutar(documentoId: Long): EstudioPdfResult
}

interface ResolverEjercicio {
    suspend fun ejecutar(enunciado: String, datos: String): IaRespuesta
}

interface AnalizarExperimento {
    suspend fun ejecutar(resultadoId: Long): IaRespuesta
}
