package com.laboratoriofisico.app.ia

import com.laboratoriofisico.app.backend.model.IaContexto
import com.laboratoriofisico.app.backend.model.IaRespuesta

/** Servicio de inteligencia artificial (implementación: [GeminiIaService]). */
interface IaService {
    suspend fun enviarMensaje(pregunta: String, contexto: IaContexto? = null): IaRespuesta
    suspend fun preguntarSobrePdf(documentoId: Long, pregunta: String): IaRespuesta
    suspend fun explicarFormula(formula: String): IaRespuesta
    suspend fun resolverEjercicio(enunciado: String, datos: String): IaRespuesta
    suspend fun explicarConcepto(concepto: String): IaRespuesta
    suspend fun analizarResultado(resultadoId: Long): IaRespuesta
}
