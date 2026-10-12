package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.local.entity.ConversacionIAEntity
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.MensajeChat
import com.laboratoriofisico.app.backend.repository.ConversacionIARepository
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.ia.IaService
import com.laboratoriofisico.app.ia.gemini.GeminiConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TutorUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val historialPdf: List<MensajeChat> = emptyList(),
    val pregunta: String = "",
    val escribiendo: Boolean = false,
    val documentos: List<Documento> = emptyList(),
    val documentoSeleccionadoId: Long? = null,
    val preguntaPdf: String = "",
    val accionRapida: String? = null,
    val iaConfigurada: Boolean = GeminiConfig.isApiKeyConfigured(),
    val error: String? = null,
    val ultimoResultadoId: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TutorViewModel(
    private val iaService: IaService,
    private val conversacionIARepository: ConversacionIARepository,
    documentoRepository: DocumentoRepository,
    private val resultadoRepository: ResultadoRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TutorUiState())
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    private val documentoPdfSeleccionado = MutableStateFlow<Long?>(null)
    private var requestInFlight = false
    private val resultadoInicialAnalizado = mutableSetOf<Long>()

    val documentos: StateFlow<List<Documento>> = sessionRepository.sessionUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList())
            else documentoRepository.observeByUsuario(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            documentos.collect { docs ->
                val selected = _uiState.value.documentoSeleccionadoId ?: docs.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        documentos = docs,
                        documentoSeleccionadoId = selected
                    )
                }
                if (documentoPdfSeleccionado.value != selected) {
                    documentoPdfSeleccionado.value = selected
                }
            }
        }
        viewModelScope.launch {
            sessionRepository.sessionUserId.flatMapLatest { userId ->
                if (userId == null) flowOf(emptyList())
                else conversacionIARepository.observeChatByUsuario(userId)
            }.collect { conversaciones ->
                _uiState.update { it.copy(mensajes = conversaciones.toMensajesChat()) }
            }
        }
        viewModelScope.launch {
            combine(sessionRepository.sessionUserId, documentoPdfSeleccionado) { userId, docId ->
                userId to docId
            }.flatMapLatest { (userId, docId) ->
                if (userId == null || docId == null) flowOf(emptyList())
                else conversacionIARepository.observeByUsuarioAndDocumento(userId, docId)
            }.collect { conversaciones ->
                _uiState.update { it.copy(historialPdf = conversaciones.toMensajesChat()) }
            }
        }
        viewModelScope.launch {
            resultadoRepository.observeAll().collect { resultados ->
                _uiState.update { it.copy(ultimoResultadoId = resultados.firstOrNull()?.id) }
            }
        }
    }

    fun onPreguntaChange(texto: String) {
        _uiState.update { it.copy(pregunta = texto, error = null) }
    }

    fun enviarMensaje() {
        val pregunta = _uiState.value.pregunta.trim()
        if (pregunta.isBlank() || _uiState.value.escribiendo) return
        ejecutarAccion(
            preguntaUsuario = pregunta,
            persistirPregunta = pregunta,
            documentoId = null
        ) {
            iaService.enviarMensaje(pregunta)
        }
        _uiState.update { it.copy(pregunta = "") }
    }

    fun explicarFormula(formula: String) {
        if (_uiState.value.escribiendo) return
        ejecutarAccion(
            preguntaUsuario = "Explicá la fórmula: $formula",
            persistirPregunta = "Fórmula: $formula",
            documentoId = null
        ) {
            iaService.explicarFormula(formula)
        }
    }

    fun explicarConcepto(concepto: String) {
        if (_uiState.value.escribiendo) return
        ejecutarAccion(
            preguntaUsuario = "Explicá el concepto: $concepto",
            persistirPregunta = "Concepto: $concepto",
            documentoId = null
        ) {
            iaService.explicarConcepto(concepto)
        }
    }

    fun resolverEjercicio(enunciado: String, datos: String) {
        if (_uiState.value.escribiendo) return
        ejecutarAccion(
            preguntaUsuario = "Resolvé: $enunciado\nDatos: $datos",
            persistirPregunta = enunciado,
            documentoId = null
        ) {
            iaService.resolverEjercicio(enunciado, datos)
        }
    }

    fun analizarResultado(resultadoId: Long) {
        if (_uiState.value.escribiendo) return
        ejecutarAccion(
            preguntaUsuario = "Analizá el resultado #$resultadoId",
            persistirPregunta = "Resultado $resultadoId",
            documentoId = null
        ) {
            iaService.analizarResultado(resultadoId)
        }
    }

    fun analizarResultadoInicial(resultadoId: Long) {
        if (!resultadoInicialAnalizado.add(resultadoId)) return
        analizarResultado(resultadoId)
    }

    fun analizarUltimoResultado() {
        val id = _uiState.value.ultimoResultadoId
        if (id == null) {
            _uiState.update { it.copy(error = "No hay resultados guardados para analizar.") }
            return
        }
        analizarResultado(id)
    }

    fun seleccionarDocumento(id: Long) {
        _uiState.update { it.copy(documentoSeleccionadoId = id, error = null) }
        documentoPdfSeleccionado.value = id
    }

    fun onPreguntaPdfChange(texto: String) {
        _uiState.update { it.copy(preguntaPdf = texto, error = null) }
    }

    fun preguntarSobrePdf() {
        val docId = _uiState.value.documentoSeleccionadoId ?: return
        val pregunta = _uiState.value.preguntaPdf.trim()
        if (pregunta.isBlank() || _uiState.value.escribiendo || requestInFlight) return
        requestInFlight = true
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(escribiendo = true, error = null) }
                val respuesta = iaService.preguntarSobrePdf(docId, pregunta)
                if (esRespuestaDeError(respuesta.texto)) {
                    _uiState.update { it.copy(escribiendo = false, error = respuesta.texto) }
                } else {
                    persistir(pregunta, respuesta.texto, docId)
                    _uiState.update { it.copy(escribiendo = false, preguntaPdf = "", error = null) }
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(escribiendo = false, error = error.message ?: "No se pudo consultar a Gemini.")
                }
            } finally {
                requestInFlight = false
            }
        }
    }

    private fun ejecutarAccion(
        preguntaUsuario: String,
        persistirPregunta: String,
        documentoId: Long?,
        solicitud: suspend () -> com.laboratoriofisico.app.backend.model.IaRespuesta
    ) {
        if (requestInFlight) return
        requestInFlight = true
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(escribiendo = true, error = null) }
                val respuesta = solicitud()
                if (esRespuestaDeError(respuesta.texto)) {
                    _uiState.update { it.copy(escribiendo = false, error = respuesta.texto) }
                } else {
                    persistir(persistirPregunta, respuesta.texto, documentoId)
                    _uiState.update { it.copy(escribiendo = false, error = null) }
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(escribiendo = false, error = error.message ?: "No se pudo consultar a Gemini.")
                }
            } finally {
                requestInFlight = false
            }
        }
    }

    private fun esRespuestaDeError(texto: String): Boolean {
        val lower = texto.lowercase()
        return lower.startsWith("sin conexión") ||
            lower.startsWith("falta configurar") ||
            lower.startsWith("esperá un momento") ||
            lower.startsWith("no se pudo") ||
            lower.startsWith("se alcanzó") ||
            lower.contains("api key") ||
            lower.contains("no encontré el resultado") ||
            lower.contains("no tiene suficiente contenido") ||
            lower.contains("no se encontró el documento") ||
            lower.contains("aún se está procesando") ||
            lower.contains("error al procesarse")
    }

    private suspend fun persistir(pregunta: String, respuesta: String, documentoId: Long?) {
        val userId = sessionRepository.getLoggedInUserId() ?: return
        conversacionIARepository.insert(
            ConversacionIAEntity(
                usuarioId = userId,
                documentoId = documentoId,
                pregunta = pregunta,
                respuesta = respuesta,
                fechaHora = System.currentTimeMillis()
            )
        )
    }

    private fun List<ConversacionIAEntity>.toMensajesChat(): List<MensajeChat> =
        flatMap { conv ->
            listOf(
                MensajeChat(
                    id = conv.id * 2,
                    texto = conv.pregunta,
                    esUsuario = true,
                    fechaHora = conv.fechaHora
                ),
                MensajeChat(
                    id = conv.id * 2 + 1,
                    texto = conv.respuesta,
                    esUsuario = false,
                    fechaHora = conv.fechaHora + 1
                )
            )
        }
}
