package com.laboratoriofisico.app.frontend.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.EjercicioEstudio
import com.laboratoriofisico.app.backend.model.Examen
import com.laboratoriofisico.app.backend.model.ExamenConfig
import com.laboratoriofisico.app.backend.model.HistorialEstudio
import com.laboratoriofisico.app.backend.model.PreguntaEstudio
import com.laboratoriofisico.app.backend.model.TarjetaEstudio
import com.laboratoriofisico.app.backend.model.TipoResumen
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.repository.EjercicioEstudioRepository
import com.laboratoriofisico.app.backend.repository.HistorialEstudioRepository
import com.laboratoriofisico.app.backend.repository.PreguntaEstudioRepository
import com.laboratoriofisico.app.backend.repository.TarjetaEstudioRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.estudio.EstudioContentService
import com.laboratoriofisico.app.estudio.EstudioTextoLocalRepository
import com.laboratoriofisico.app.ia.gemini.DocumentoSinContenidoException
import com.laboratoriofisico.app.ia.gemini.GeminiConfig
import com.laboratoriofisico.app.reportes.ExportResult
import com.laboratoriofisico.app.reportes.ReporteService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class EstudioUiState(
    val tabIndex: Int = 0,
    val documentos: List<Documento> = emptyList(),
    val documentoSeleccionadoId: Long? = null,
    val tipoResumen: TipoResumen = TipoResumen.AUTOMATICO,
    val resumen: String = "",
    val notas: String = "",
    val dificultad: String = "media",
    val preguntas: List<PreguntaEstudio> = emptyList(),
    val mostrarRespuestas: Boolean = false,
    val ejercicio: EjercicioEstudio? = null,
    val respuestaEjercicio: String = "",
    val verificacionEjercicio: String? = null,
    val examen: Examen? = null,
    val respuestasExamen: Map<Int, Int> = emptyMap(),
    val calificacionExamen: Int? = null,
    val tarjetas: List<TarjetaEstudio> = emptyList(),
    val tarjetaIndex: Int = 0,
    val mostrarReverso: Boolean = false,
    val historial: List<HistorialEstudio> = emptyList(),
    val exportandoHistorial: Boolean = false,
    val shareUri: Uri? = null,
    val shareFileName: String? = null,
    val generando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
    val iaConfigurada: Boolean = GeminiConfig.isApiKeyConfigured(),
    val examenConfig: ExamenConfig = ExamenConfig()
)

@OptIn(ExperimentalCoroutinesApi::class)
class EstudioViewModel(
    private val estudioContentService: EstudioContentService,
    documentoRepository: DocumentoRepository,
    private val preguntaEstudioRepository: PreguntaEstudioRepository,
    private val ejercicioEstudioRepository: EjercicioEstudioRepository,
    private val tarjetaEstudioRepository: TarjetaEstudioRepository,
    private val historialEstudioRepository: HistorialEstudioRepository,
    private val estudioTextoLocalRepository: EstudioTextoLocalRepository,
    private val reporteService: ReporteService,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EstudioUiState())
    val uiState: StateFlow<EstudioUiState> = _uiState.asStateFlow()

    private val documentoSeleccionado = MutableStateFlow<Long?>(null)
    private var inicioActividadMs: Long? = null

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
                _uiState.update { state ->
                    state.copy(documentos = docs, documentoSeleccionadoId = selected)
                }
                if (documentoSeleccionado.value != selected) {
                    documentoSeleccionado.value = selected
                }
                selected?.let { cargarTextosLocales(it) }
            }
        }
        viewModelScope.launch {
            documentoSeleccionado.flatMapLatest { docId ->
                if (docId == null) flowOf(emptyList())
                else historialEstudioRepository.observeByDocumento(docId)
                    .flatMapLatest { list -> flowOf(list.map { it.toDomain() }) }
            }.collect { historial ->
                _uiState.update { it.copy(historial = historial) }
            }
        }
        viewModelScope.launch {
            documentoSeleccionado.flatMapLatest { docId ->
                if (docId == null) flowOf(emptyList())
                else preguntaEstudioRepository.observeByDocumento(docId)
                    .flatMapLatest { list -> flowOf(list.map { it.toDomain() }) }
            }.collect { preguntas ->
                _uiState.update { it.copy(preguntas = preguntas) }
            }
        }
        viewModelScope.launch {
            documentoSeleccionado.flatMapLatest { docId ->
                if (docId == null) flowOf(null)
                else ejercicioEstudioRepository.observeByDocumento(docId)
                    .flatMapLatest { list -> flowOf(list.firstOrNull()?.toDomain()) }
            }.collect { ejercicio ->
                _uiState.update { it.copy(ejercicio = ejercicio) }
            }
        }
        viewModelScope.launch {
            documentoSeleccionado.flatMapLatest { docId ->
                if (docId == null) flowOf(emptyList())
                else tarjetaEstudioRepository.observeByDocumento(docId)
                    .flatMapLatest { list -> flowOf(list.map { it.toDomain() }) }
            }.collect { tarjetas ->
                _uiState.update {
                    it.copy(
                        tarjetas = tarjetas,
                        tarjetaIndex = it.tarjetaIndex.coerceIn(0, (tarjetas.size - 1).coerceAtLeast(0))
                    )
                }
            }
        }
    }

    fun seleccionarTab(index: Int) {
        _uiState.update { it.copy(tabIndex = index) }
    }

    fun seleccionarDocumento(id: Long) {
        _uiState.update {
            it.copy(
                documentoSeleccionadoId = id,
                examen = null,
                calificacionExamen = null,
                respuestasExamen = emptyMap(),
                verificacionEjercicio = null,
                respuestaEjercicio = "",
                error = null
            )
        }
        documentoSeleccionado.value = id
        cargarTextosLocales(id)
    }

    fun seleccionarTipoResumen(tipo: TipoResumen) {
        _uiState.update { it.copy(tipoResumen = tipo) }
        selectedId()?.let { cargarTextosLocales(it) }
    }

    private fun cargarTextosLocales(docId: Long) {
        viewModelScope.launch {
            val tipo = _uiState.value.tipoResumen
            val resumen = estudioTextoLocalRepository.loadResumen(docId, tipo).orEmpty()
            val notas = estudioTextoLocalRepository.loadNotas(docId).orEmpty()
            _uiState.update { it.copy(resumen = resumen, notas = notas) }
        }
    }

    fun generarResumen() {
        val docId = selectedId() ?: return
        viewModelScope.launch {
            inicioActividadMs = System.currentTimeMillis()
            _uiState.update { it.copy(generando = true, error = null) }
            runCatching {
                estudioContentService.generarResumen(docId, _uiState.value.tipoResumen)
            }.onSuccess { texto ->
                estudioTextoLocalRepository.saveResumen(docId, _uiState.value.tipoResumen, texto)
                _uiState.update { it.copy(generando = false, resumen = texto, mensaje = null) }
                registrarHistorial(docId, "resumen", "Completado")
            }.onFailure { error ->
                _uiState.update { it.copy(generando = false, error = mensajeError(error)) }
            }
        }
    }

    fun generarNotas() {
        val docId = selectedId() ?: return
        viewModelScope.launch {
            inicioActividadMs = System.currentTimeMillis()
            _uiState.update { it.copy(generando = true, error = null) }
            runCatching {
                estudioContentService.generarNotas(docId)
            }.onSuccess { notas ->
                estudioTextoLocalRepository.saveNotas(docId, notas)
                _uiState.update { it.copy(generando = false, notas = notas, mensaje = null) }
                registrarHistorial(docId, "notas", "Generadas")
            }.onFailure { error ->
                _uiState.update { it.copy(generando = false, error = mensajeError(error)) }
            }
        }
    }

    fun guardarNotas() {
        val docId = selectedId() ?: return
        viewModelScope.launch {
            estudioTextoLocalRepository.saveNotas(docId, _uiState.value.notas)
            _uiState.update { it.copy(mensaje = "Notas guardadas") }
            registrarHistorial(docId, "notas", "Editadas y guardadas")
        }
    }

    fun onNotasChange(texto: String) {
        _uiState.update { it.copy(notas = texto) }
    }

    fun seleccionarDificultad(dificultad: String) {
        _uiState.update { it.copy(dificultad = dificultad) }
    }

    fun generarPreguntas() {
        val docId = selectedId() ?: return
        viewModelScope.launch {
            inicioActividadMs = System.currentTimeMillis()
            _uiState.update { it.copy(generando = true, mostrarRespuestas = false, error = null) }
            runCatching {
                estudioContentService.generarPreguntas(docId, _uiState.value.dificultad)
            }.onSuccess {
                _uiState.update { it.copy(generando = false, mensaje = null) }
                registrarHistorial(docId, "preguntas", _uiState.value.dificultad)
            }.onFailure { error ->
                _uiState.update { it.copy(generando = false, error = mensajeError(error)) }
            }
        }
    }

    fun toggleMostrarRespuestas() {
        _uiState.update { it.copy(mostrarRespuestas = !it.mostrarRespuestas) }
    }

    fun generarEjercicio() {
        val docId = selectedId() ?: return
        viewModelScope.launch {
            inicioActividadMs = System.currentTimeMillis()
            _uiState.update { it.copy(generando = true, verificacionEjercicio = null, error = null) }
            runCatching {
                estudioContentService.generarEjercicio(docId)
            }.onSuccess {
                _uiState.update { it.copy(generando = false, respuestaEjercicio = "", mensaje = null) }
                registrarHistorial(docId, "ejercicio", "Generado")
            }.onFailure { error ->
                _uiState.update { it.copy(generando = false, error = mensajeError(error)) }
            }
        }
    }

    fun onRespuestaEjercicioChange(valor: String) {
        _uiState.update { it.copy(respuestaEjercicio = valor) }
    }

    fun verificarEjercicio() {
        val ejercicio = _uiState.value.ejercicio ?: return
        val respuestaUsuario = _uiState.value.respuestaEjercicio.trim()
        val solucion = ejercicio.solucion.trim()
        val correcto = respuestasEquivalentes(respuestaUsuario, solucion)
        val docId = selectedId()
        _uiState.update {
            it.copy(
                verificacionEjercicio = if (correcto) {
                    "¡Correcto! ${ejercicio.explicacion}"
                } else {
                    "Incorrecto. Solución: $solucion. ${ejercicio.explicacion}"
                }
            )
        }
        docId?.let {
            registrarHistorial(it, "ejercicio", if (correcto) "Correcto" else "Incorrecto")
        }
    }

    fun onExamenConfigChange(config: ExamenConfig) {
        _uiState.update { it.copy(examenConfig = config) }
    }

    fun crearExamen() {
        val docId = selectedId() ?: return
        viewModelScope.launch {
            inicioActividadMs = System.currentTimeMillis()
            _uiState.update { it.copy(generando = true, calificacionExamen = null, error = null) }
            runCatching {
                estudioContentService.crearExamen(docId, _uiState.value.examenConfig)
            }.onSuccess { examen ->
                _uiState.update {
                    it.copy(generando = false, examen = examen, respuestasExamen = emptyMap(), mensaje = null)
                }
                registrarHistorial(docId, "examen", "Creado (${examen.preguntas.size} preguntas)")
            }.onFailure { error ->
                _uiState.update { it.copy(generando = false, error = mensajeError(error)) }
            }
        }
    }

    fun responderExamen(preguntaId: Int, opcion: Int) {
        _uiState.update { it.copy(respuestasExamen = it.respuestasExamen + (preguntaId to opcion)) }
    }

    fun finalizarExamen() {
        val examen = _uiState.value.examen ?: return
        val total = examen.preguntas.size.coerceAtLeast(1)
        val correctas = examen.preguntas.count { p ->
            _uiState.value.respuestasExamen[p.id] == p.respuestaCorrecta
        }
        val calificacion = (correctas * 100) / total
        _uiState.update { it.copy(calificacionExamen = calificacion) }
        selectedId()?.let { registrarHistorial(it, "examen", "$calificacion%") }
    }

    fun generarTarjetas() {
        val docId = selectedId() ?: return
        viewModelScope.launch {
            inicioActividadMs = System.currentTimeMillis()
            _uiState.update { it.copy(generando = true, tarjetaIndex = 0, mostrarReverso = false, error = null) }
            runCatching {
                estudioContentService.generarTarjetas(docId)
            }.onSuccess { tarjetas ->
                _uiState.update { it.copy(generando = false, mensaje = null) }
                registrarHistorial(docId, "tarjetas", "${tarjetas.size} tarjetas")
            }.onFailure { error ->
                _uiState.update { it.copy(generando = false, error = mensajeError(error)) }
            }
        }
    }

    fun toggleReverso() {
        _uiState.update { it.copy(mostrarReverso = !it.mostrarReverso) }
    }

    fun anteriorTarjeta() {
        val tarjetas = _uiState.value.tarjetas
        if (tarjetas.isEmpty()) return
        val prev = if (_uiState.value.tarjetaIndex == 0) tarjetas.lastIndex else _uiState.value.tarjetaIndex - 1
        _uiState.update { it.copy(tarjetaIndex = prev, mostrarReverso = false) }
    }

    fun siguienteTarjeta() {
        val tarjetas = _uiState.value.tarjetas
        if (tarjetas.isEmpty()) return
        val next = (_uiState.value.tarjetaIndex + 1) % tarjetas.size
        _uiState.update { it.copy(tarjetaIndex = next, mostrarReverso = false) }
        if (next == 0) {
            selectedId()?.let { registrarHistorial(it, "tarjetas", "Ronda completada") }
        }
    }

    fun exportarHistorialPdf() {
        val docId = selectedId() ?: return
        if (_uiState.value.exportandoHistorial) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(exportandoHistorial = true, error = null, shareUri = null, mensaje = null)
            }
            val userId = sessionRepository.getLoggedInUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(exportandoHistorial = false, error = "Inicia sesión para exportar reportes.")
                }
                return@launch
            }
            when (val result = reporteService.exportarHistorialEstudioPdf(docId, userId)) {
                is ExportResult.Success -> _uiState.update {
                    it.copy(
                        exportandoHistorial = false,
                        shareUri = result.uri,
                        shareFileName = result.fileName,
                        error = null
                    )
                }
                is ExportResult.Error -> _uiState.update {
                    it.copy(exportandoHistorial = false, error = result.message)
                }
            }
        }
    }

    fun onShareHandled() {
        _uiState.update { it.copy(shareUri = null, shareFileName = null) }
    }

    fun onShareError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    private fun selectedId(): Long? = _uiState.value.documentoSeleccionadoId

    private fun registrarHistorial(documentoId: Long, tipo: String, resultado: String) {
        val inicio = inicioActividadMs ?: System.currentTimeMillis()
        val tiempo = (System.currentTimeMillis() - inicio).coerceAtLeast(1_000L)
        viewModelScope.launch {
            historialEstudioRepository.insert(
                HistorialEstudio(
                    documentoId = documentoId,
                    tipoActividad = tipo,
                    resultado = resultado,
                    fecha = System.currentTimeMillis(),
                    tiempoEstudio = tiempo
                ).toEntity()
            )
        }
        inicioActividadMs = null
    }

    private fun mensajeError(error: Throwable): String = when (error) {
        is DocumentoSinContenidoException -> error.message.orEmpty()
        else -> error.message ?: "No se pudo completar la operación."
    }

    private fun respuestasEquivalentes(usuario: String, solucion: String): Boolean {
        if (usuario.equals(solucion, ignoreCase = true)) return true
        val usuarioNum = usuario.replace(",", ".").toDoubleOrNull()
        val solucionNum = solucion.replace(",", ".").toDoubleOrNull()
        if (usuarioNum != null && solucionNum != null) {
            return abs(usuarioNum - solucionNum) < 0.05
        }
        return false
    }
}
