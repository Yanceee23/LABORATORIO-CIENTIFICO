package com.laboratoriofisico.app.p08mcp.flujos

import com.laboratoriofisico.app.backend.model.IaRespuesta
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.ia.IaService

class PreguntaSobrePdfFlow(
    private val iaService: IaService
) : PreguntaSobrePDF {
    override suspend fun ejecutar(documentoId: Long, pregunta: String): IaRespuesta =
        iaService.preguntarSobrePdf(documentoId, pregunta)
}

class EstudiarPdfFlow(
    private val estudioContentService: com.laboratoriofisico.app.estudio.EstudioContentService
) : EstudiarPDF {
    override suspend fun ejecutar(documentoId: Long): EstudioPdfResult {
        val resumen = estudioContentService.generarResumen(
            documentoId,
            com.laboratoriofisico.app.backend.model.TipoResumen.CORTO
        )
        val preguntas = estudioContentService.generarPreguntas(documentoId, "media")
        val tarjetas = estudioContentService.generarTarjetas(documentoId)
        return EstudioPdfResult(
            resumen = resumen,
            cantidadPreguntas = preguntas.size,
            cantidadTarjetas = tarjetas.size
        )
    }
}

class ResolverEjercicioFlow(
    private val iaService: IaService,
    private val fisicaEngine: com.laboratoriofisico.app.fisica.FisicaEngine
) : ResolverEjercicio {
    override suspend fun ejecutar(enunciado: String, datos: String): IaRespuesta {
        val calculoHint = extraerCalculo(datos)
        val base = iaService.resolverEjercicio(enunciado, datos)
        return if (calculoHint == null) base else IaRespuesta(
            texto = "${base.texto}\n\nVerificación numérica MCP: $calculoHint",
            esStub = false
        )
    }

    private fun extraerCalculo(datos: String): String? {
        val v = Regex("""v\s*=\s*([\d.]+)""").find(datos)?.groupValues?.get(1)?.toDoubleOrNull()
        val t = Regex("""t\s*=\s*([\d.]+)""").find(datos)?.groupValues?.get(1)?.toDoubleOrNull()
        if (v != null && t != null) {
            val d = fisicaEngine.calcular(
                com.laboratoriofisico.app.fisica.FormulaId.D_IGUAL_V_T.id,
                mapOf("v" to v, "t" to t)
            )
            if (d != null) return "d = v·t = $d"
        }
        return null
    }
}

class AnalizarExperimentoFlow(
    private val iaService: IaService,
    private val resultadoRepository: ResultadoRepository
) : AnalizarExperimento {
    override suspend fun ejecutar(resultadoId: Long): IaRespuesta {
        resultadoRepository.getById(resultadoId)
            ?: return IaRespuesta("Resultado #$resultadoId no encontrado.", esStub = false)
        return iaService.analizarResultado(resultadoId)
    }
}

data class EstudioPdfResult(
    val resumen: String,
    val cantidadPreguntas: Int,
    val cantidadTarjetas: Int
)
