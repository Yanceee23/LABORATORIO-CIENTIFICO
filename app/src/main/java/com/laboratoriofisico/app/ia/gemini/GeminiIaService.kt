package com.laboratoriofisico.app.ia.gemini

import com.laboratoriofisico.app.backend.model.IaContexto
import com.laboratoriofisico.app.backend.model.IaRespuesta
import com.laboratoriofisico.app.backend.repository.MedicionRepository
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.ia.IaService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiIaService(
    private val geminiClient: GeminiClient,
    private val resultadoRepository: ResultadoRepository,
    private val medicionRepository: MedicionRepository,
    private val documentoContextoHelper: DocumentoContextoHelper
) : IaService {

    override suspend fun enviarMensaje(pregunta: String, contexto: IaContexto?): IaRespuesta =
        IaRespuesta(texto = geminiClient.generar(pregunta), esStub = false)

    override suspend fun preguntarSobrePdf(documentoId: Long, pregunta: String): IaRespuesta =
        withContext(Dispatchers.IO) {
            runCatching {
                val context = documentoContextoHelper.cargarContexto(documentoId, pregunta)
                val referencias = context.seccionesUsadas.joinToString(", ") {
                    "${it.titulo} (pág. ${it.numeroPagina})"
                }
                val prompt = GeminiPrompts.preguntarSobrePdf(
                    contexto = context.texto,
                    pregunta = pregunta,
                    referencias = referencias
                )
                IaRespuesta(texto = geminiClient.generar(prompt), esStub = false)
            }.getOrElse { error ->
                IaRespuesta(
                    texto = when (error) {
                        is DocumentoSinContenidoException -> error.message.orEmpty()
                        else -> GeminiErrorMapper.mensajeParaUsuario(error)
                    },
                    esStub = false
                )
            }
        }

    override suspend fun explicarFormula(formula: String): IaRespuesta =
        IaRespuesta(texto = geminiClient.generar(GeminiPrompts.explicarFormula(formula)), esStub = false)

    override suspend fun resolverEjercicio(enunciado: String, datos: String): IaRespuesta =
        IaRespuesta(texto = geminiClient.generar(GeminiPrompts.resolverEjercicio(enunciado, datos)), esStub = false)

    override suspend fun explicarConcepto(concepto: String): IaRespuesta =
        IaRespuesta(texto = geminiClient.generar(GeminiPrompts.explicarConcepto(concepto)), esStub = false)

    override suspend fun analizarResultado(resultadoId: Long): IaRespuesta = withContext(Dispatchers.IO) {
        runCatching {
            val resultado = resultadoRepository.getById(resultadoId)
                ?: return@withContext IaRespuesta(
                    texto = "No encontré el resultado #$resultadoId. Guardá un experimento primero en Resultados.",
                    esStub = false
                )
            val medicion = medicionRepository.getById(resultado.medicionId)
            IaRespuesta(
                texto = geminiClient.generar(
                    GeminiPrompts.analizarResultado(
                        resultadoId = resultadoId,
                        valor = resultado.valorCalculado,
                        unidad = resultado.unidad,
                        notas = resultado.notas,
                        medicionNombre = medicion?.nombre,
                        medicionValor = medicion?.valor,
                        medicionUnidad = medicion?.unidad
                    )
                ),
                esStub = false
            )
        }.getOrElse { error ->
            IaRespuesta(texto = GeminiErrorMapper.mensajeParaUsuario(error), esStub = false)
        }
    }
}
