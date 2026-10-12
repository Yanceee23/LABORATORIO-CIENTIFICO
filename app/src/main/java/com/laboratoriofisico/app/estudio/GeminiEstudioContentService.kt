package com.laboratoriofisico.app.estudio

import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.EjercicioEstudio
import com.laboratoriofisico.app.backend.model.Examen
import com.laboratoriofisico.app.backend.model.ExamenConfig
import com.laboratoriofisico.app.backend.model.PreguntaEstudio
import com.laboratoriofisico.app.backend.model.TarjetaEstudio
import com.laboratoriofisico.app.backend.model.TipoResumen
import com.laboratoriofisico.app.backend.repository.EjercicioEstudioRepository
import com.laboratoriofisico.app.backend.repository.PreguntaEstudioRepository
import com.laboratoriofisico.app.backend.repository.TarjetaEstudioRepository
import com.laboratoriofisico.app.ia.gemini.DocumentoContextoHelper
import com.laboratoriofisico.app.ia.gemini.DocumentoSinContenidoException
import com.laboratoriofisico.app.ia.gemini.GeminiClient
import com.laboratoriofisico.app.ia.gemini.GeminiErrorMapper
import com.laboratoriofisico.app.ia.gemini.GeminiPrompts
import com.laboratoriofisico.app.ia.gemini.GeminiResponseParser

class GeminiEstudioContentService(
    private val geminiClient: GeminiClient,
    private val documentoContextoHelper: DocumentoContextoHelper,
    private val preguntaEstudioRepository: PreguntaEstudioRepository,
    private val ejercicioEstudioRepository: EjercicioEstudioRepository,
    private val tarjetaEstudioRepository: TarjetaEstudioRepository
) : EstudioContentService {

    override suspend fun generarResumen(documentoId: Long, tipo: TipoResumen): String =
        generarTexto(documentoId) { contexto ->
            val etiqueta = when (tipo) {
                TipoResumen.AUTOMATICO -> "automático equilibrado"
                TipoResumen.CORTO -> "corto (máximo 5 oraciones)"
                TipoResumen.DETALLADO -> "detallado con secciones numeradas"
            }
            GeminiPrompts.resumen(contexto, etiqueta)
        }

    override suspend fun generarNotas(documentoId: Long): String =
        generarTexto(documentoId) { contexto ->
            GeminiPrompts.notas(contexto)
        }

    override suspend fun generarPreguntas(documentoId: Long, dificultad: String): List<PreguntaEstudio> =
        generarEstructurado(documentoId) { contexto ->
            val respuesta = geminiClient.generar(
                GeminiPrompts.preguntasEstudio(contexto, dificultad, cantidad = 4)
            )
            val preguntas = GeminiResponseParser.parsePreguntas(respuesta, documentoId, dificultad)
            if (preguntas.isEmpty()) error("La IA no generó preguntas válidas para este documento.")
            preguntas.forEach { preguntaEstudioRepository.insert(it.toEntity()) }
            preguntas
        }

    override suspend fun generarEjercicio(documentoId: Long): EjercicioEstudio =
        generarEstructurado(documentoId) { contexto ->
            val respuesta = geminiClient.generar(GeminiPrompts.ejercicioEstudio(contexto))
            val ejercicio = GeminiResponseParser.parseEjercicio(respuesta, documentoId)
            ejercicioEstudioRepository.insert(ejercicio.toEntity())
            ejercicio
        }

    override suspend fun generarTarjetas(documentoId: Long): List<TarjetaEstudio> =
        generarEstructurado(documentoId) { contexto ->
            val respuesta = geminiClient.generar(GeminiPrompts.tarjetasEstudio(contexto, cantidad = 6))
            val tarjetas = GeminiResponseParser.parseTarjetas(respuesta, documentoId)
            if (tarjetas.isEmpty()) error("La IA no generó tarjetas válidas para este documento.")
            tarjetas.forEach { tarjetaEstudioRepository.insert(it.toEntity()) }
            tarjetas
        }

    override suspend fun crearExamen(documentoId: Long, config: ExamenConfig): Examen =
        generarEstructurado(documentoId) { contexto ->
            val respuesta = geminiClient.generar(
                GeminiPrompts.examenEstudio(contexto, config.numeroPreguntas)
            )
            GeminiResponseParser.parseExamen(respuesta, config)
        }

    private suspend fun generarTexto(
        documentoId: Long,
        promptBuilder: (String) -> String
    ): String = runCatching {
        val contexto = documentoContextoHelper.cargarContexto(documentoId).texto
        val respuesta = geminiClient.generar(promptBuilder(contexto))
        if (esErrorDeGemini(respuesta)) error(respuesta)
        respuesta
    }.getOrElse { error ->
        throw mapError(error)
    }

    private suspend fun <T> generarEstructurado(
        documentoId: Long,
        block: suspend (String) -> T
    ): T = runCatching {
        val contexto = documentoContextoHelper.cargarContexto(documentoId).texto
        block(contexto)
    }.getOrElse { error ->
        throw mapError(error)
    }

    private fun mapError(error: Throwable): Throwable = when (error) {
        is DocumentoSinContenidoException -> error
        is IllegalArgumentException -> error
        else -> RuntimeException(GeminiErrorMapper.mensajeParaUsuario(error), error)
    }

    private fun esErrorDeGemini(texto: String): Boolean {
        val lower = texto.lowercase()
        return lower.startsWith("sin conexión") ||
            lower.startsWith("falta configurar") ||
            lower.startsWith("esperá un momento") ||
            lower.startsWith("no se pudo") ||
            lower.startsWith("se alcanzó") ||
            lower.contains("api key")
    }
}
