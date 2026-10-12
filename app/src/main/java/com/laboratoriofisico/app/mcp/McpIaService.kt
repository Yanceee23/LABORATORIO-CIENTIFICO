package com.laboratoriofisico.app.mcp

import com.laboratoriofisico.app.backend.model.IaContexto
import com.laboratoriofisico.app.backend.model.IaRespuesta
import com.laboratoriofisico.app.ia.IaService
import com.laboratoriofisico.app.p08mcp.flujos.AnalizarExperimentoFlow
import com.laboratoriofisico.app.p08mcp.flujos.PreguntaSobrePdfFlow
import com.laboratoriofisico.app.p08mcp.flujos.ResolverEjercicioFlow

/**
 * Capa IA que enruta operaciones enriquecidas por MCP y delega el resto a Gemini.
 */
class McpIaService(
    private val delegate: IaService,
    private val preguntaSobrePdf: PreguntaSobrePdfFlow,
    private val resolverEjercicio: ResolverEjercicioFlow,
    private val analizarExperimento: AnalizarExperimentoFlow,
    private val mcpClient: McpClient
) : IaService {

    override suspend fun enviarMensaje(pregunta: String, contexto: IaContexto?): IaRespuesta {
        if (mcpClient.estaConectado() && pregunta.trimStart().startsWith("@tool:")) {
            val texto = mcpClient.enviarConsulta(pregunta)
            return IaRespuesta(
                texto = texto ?: "No se pudo ejecutar la herramienta MCP.",
                esStub = false
            )
        }
        return delegate.enviarMensaje(pregunta, contexto)
    }

    override suspend fun preguntarSobrePdf(documentoId: Long, pregunta: String): IaRespuesta =
        preguntaSobrePdf.ejecutar(documentoId, pregunta)

    override suspend fun explicarFormula(formula: String): IaRespuesta =
        delegate.explicarFormula(formula)

    override suspend fun resolverEjercicio(enunciado: String, datos: String): IaRespuesta =
        resolverEjercicio.ejecutar(enunciado, datos)

    override suspend fun explicarConcepto(concepto: String): IaRespuesta =
        delegate.explicarConcepto(concepto)

    override suspend fun analizarResultado(resultadoId: Long): IaRespuesta =
        analizarExperimento.ejecutar(resultadoId)
}
