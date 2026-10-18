package com.laboratoriofisico.app.frontend.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.hoja.HojaCalculoNotas
import com.laboratoriofisico.app.backend.model.AnalisisMicroscopio
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.piel.PielCalculoNotas
import com.laboratoriofisico.app.backend.repository.AnalisisMicroscopioRepository
import com.laboratoriofisico.app.backend.sangre.SangreAnalisis
import com.laboratoriofisico.app.backend.sangre.SangreFormulario
import com.laboratoriofisico.app.ia.gemini.GeminiClient
import com.laboratoriofisico.app.ia.gemini.GeminiPrompts
import com.laboratoriofisico.app.usb.UvcMicroscopeManager
import com.laboratoriofisico.app.usb.UvcMicroscopeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

data class MicroscopioAnalisisUiState(
    val tipo: TipoAnalisisMicroscopio,
    val experimentoId: Long? = null,
    val capturando: Boolean = false,
    val analizando: Boolean = false,
    val geminiCallsEnAnalisisActual: Int = 0,
    val ultimoAnalisisId: Long? = null,
    val ultimaCapturaPath: String? = null,
    val mensaje: String? = null,
    val error: String? = null,
    val historial: List<AnalisisMicroscopio> = emptyList(),
    val uvcState: UvcMicroscopeState = UvcMicroscopeState(),
    val comio: String = "",
    val horaComida: String = "",
    val minutoComida: String = "",
    val periodoComida: String = "AM",
    val glucosa: String = "",
    val unidadGlucosa: String = SangreFormulario.MG_DL,
    val haceActividad: Boolean = false,
    val minutosActividad: String = "",
    val tipoActividad: String = "",
    val nRojos: String = "",
    val nBlancos: String = "",
    val nGeneral: String = "",
    val dilucionGeneral: String = "",
    val areaGeneral: String = ""
)

class MicroscopioAnalisisViewModel(
    private val tipo: TipoAnalisisMicroscopio,
    private val appContext: Context,
    private val analisisRepository: AnalisisMicroscopioRepository,
    private val geminiClient: GeminiClient,
    val uvcMicroscopeManager: UvcMicroscopeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MicroscopioAnalisisUiState(tipo = tipo))
    val uiState: StateFlow<MicroscopioAnalisisUiState> = _uiState.asStateFlow()
    private var analysisInFlight = false
    private val analyzedImageKeys = mutableSetOf<String>()

    init {
        uvcMicroscopeManager.startMonitoring()
        viewModelScope.launch {
            uvcMicroscopeManager.state.collect { uvcState ->
                _uiState.update {
                    val mergedError = mergeUvcError(uvcState.errorMessage, it.error)
                    // #region agent log
                    com.laboratoriofisico.app.usb.UsbDebugNdjson.log(
                        hypothesisId = "H4",
                        location = "MicroscopioAnalisisViewModel.state.collect",
                        message = "ViewModel merged USB error",
                        data = mapOf(
                            "uvcError" to uvcState.errorMessage,
                            "previousError" to it.error,
                            "mergedError" to mergedError,
                            "permissionGranted" to uvcState.permissionGranted,
                            "permissionDenied" to uvcState.permissionDenied,
                            "lastGranted" to uvcState.debug.lastGranted,
                            "liveHasPermission" to uvcState.debug.liveHasPermission
                        )
                    )
                    // #endregion
                    it.copy(
                        uvcState = uvcState,
                        capturando = uvcState.capturing,
                        error = mergedError
                    )
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                val experimentoId = analisisRepository.ensureExperimento(tipo)
                _uiState.update { it.copy(experimentoId = experimentoId) }
                analisisRepository.observeByExperimento(experimentoId).collect { historial ->
                    _uiState.update { it.copy(historial = historial) }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "No se pudo cargar el historial.") }
            }
        }
    }

    fun solicitarPermisoUsb() {
        _uiState.update { it.copy(mensaje = null, error = null) }
        uvcMicroscopeManager.requestUsbPermission()
    }

    fun capturar() {
        if (_uiState.value.capturando || _uiState.value.analizando) return
        viewModelScope.launch {
            _uiState.update { it.copy(capturando = true, mensaje = null, error = null) }
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    val outputDir = appContext.getExternalFilesDir("capturas_microscopio") ?: appContext.cacheDir
                    outputDir.mkdirs()
                    File(outputDir, "microscopio_${tipo.key}_${System.currentTimeMillis()}.jpg")
                }
                com.laboratoriofisico.app.usb.UsbDebugNdjson.log(
                    hypothesisId = "CAP",
                    location = "MicroscopioAnalisisViewModel.capturar",
                    message = "Solicitando captura al manager",
                    data = mapOf("file" to file.absolutePath, "tipo" to tipo.key)
                )
                suspendCancellableCoroutine { continuation ->
                    uvcMicroscopeManager.capturePhoto(file) { result ->
                        if (continuation.isActive) continuation.resume(result)
                    }
                }.getOrThrow()
            }.onSuccess { path ->
                com.laboratoriofisico.app.usb.UsbDebugNdjson.log(
                    hypothesisId = "CAP",
                    location = "MicroscopioAnalisisViewModel.capturar.success",
                    message = "Captura lista para UI",
                    data = mapOf("path" to path)
                )
                _uiState.update {
                    it.copy(
                        capturando = false,
                        ultimaCapturaPath = path,
                        mensaje = "Fotografía capturada. Pulsa \"Analizar foto\" para usar Gemini.",
                        error = null
                    )
                }
            }.onFailure { error ->
                com.laboratoriofisico.app.usb.UsbDebugNdjson.log(
                    hypothesisId = "CAP",
                    location = "MicroscopioAnalisisViewModel.capturar.failure",
                    message = "Captura falló sin crash",
                    data = mapOf("error" to (error.message ?: error.javaClass.simpleName))
                )
                _uiState.update {
                    it.copy(
                        capturando = false,
                        error = error.message ?: "Error al capturar la fotografía."
                    )
                }
            }
        }
    }

    fun seleccionarImagenFallback(uri: Uri) {
        if (_uiState.value.analizando) return
        viewModelScope.launch {
            runCatching {
                val outputDir = appContext.getExternalFilesDir("capturas_microscopio") ?: appContext.cacheDir
                outputDir.mkdirs()
                val file = File(outputDir, "microscopio_${tipo.key}_galeria_${System.currentTimeMillis()}.jpg")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalArgumentException("No se pudo leer la imagen seleccionada.")
                file.absolutePath
            }.onSuccess { path ->
                _uiState.update {
                    it.copy(
                        ultimaCapturaPath = path,
                        mensaje = "Imagen cargada como fallback. Pulsa \"Analizar foto\" para usar Gemini.",
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "No se pudo cargar la imagen seleccionada.")
                }
            }
        }
    }

    fun analizarCaptura() {
        val state = _uiState.value
        val imagenPath = state.ultimaCapturaPath ?: return
        if (state.analizando || analysisInFlight) return
        val imageKey = analysisCacheKey(imagenPath)
        val repetidaEnHistorial = tipo != TipoAnalisisMicroscopio.SANGRE &&
            state.historial.any { it.imagenPath == imagenPath && it.promptVersion == promptVersionActual() }
        if (imageKey in analyzedImageKeys || repetidaEnHistorial) {
            com.laboratoriofisico.app.usb.UsbDebugNdjson.log(
                hypothesisId = "GEMINI-ONE-CALL",
                location = "MicroscopioAnalisisViewModel.analizarCaptura.cache",
                message = "Análisis de imagen omitido: captura ya analizada",
                data = mapOf("tipo" to tipo.key, "imagenPath" to imagenPath)
            )
            _uiState.update {
                it.copy(
                    mensaje = "Esta captura ya fue analizada; se reutiliza el resultado del historial.",
                    error = null
                )
            }
            return
        }

        analysisInFlight = true
        val analysisId = System.currentTimeMillis()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    analizando = true,
                    geminiCallsEnAnalisisActual = 0,
                    ultimoAnalisisId = analysisId,
                    mensaje = null,
                    error = null
                )
            }
            runCatching {
                val experimentoId = state.experimentoId ?: analisisRepository.ensureExperimento(tipo)
                val resumenAnterior = if (tipo == TipoAnalisisMicroscopio.YOGUR) {
                    state.historial.firstOrNull { it.imagenPath != imagenPath }?.resumenParaComparacion
                } else {
                    null
                }
                val prompt = promptForTipo(resumenAnterior)
                registrarLlamadaGemini(analysisId, imagenPath)
                val cruda = if (tipo == TipoAnalisisMicroscopio.SANGRE) {
                    geminiClient.generarConImagenJson(prompt, imagenPath)
                } else {
                    geminiClient.generarConImagen(prompt, imagenPath)
                }
                validarRespuestaGemini(cruda)
                val respuesta = if (tipo == TipoAnalisisMicroscopio.SANGRE) {
                    val visible = SangreAnalisis.presentar(cruda, datosAnotadosSangre())
                    val (rojos, blancos) = SangreAnalisis.conteos(visible)
                    _uiState.update { actual ->
                        actual.copy(
                            nRojos = rojos?.toString() ?: actual.nRojos,
                            nBlancos = blancos?.toString() ?: actual.nBlancos
                        )
                    }
                    visible
                } else {
                    cruda
                }
                analisisRepository.insert(
                    AnalisisMicroscopio(
                        experimentoId = experimentoId,
                        tipoExperimento = tipo,
                        subtipo = tipo.subtipo(),
                        imagenPath = imagenPath,
                        analisis = respuesta,
                        resumenParaComparacion = resumenDesdeAnalisis(respuesta),
                        promptVersion = promptVersionActual(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                analyzedImageKeys.add(imageKey)
                _uiState.update {
                    it.copy(
                        analizando = false,
                        mensaje = "Análisis guardado en el historial de Laboratorio."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        analizando = false,
                        error = error.message ?: "No se pudo analizar la fotografía."
                    )
                }
            }.also {
                analysisInFlight = false
            }
        }
    }

    fun onComioSangre(value: String) = _uiState.update { it.copy(comio = value) }

    fun onHoraComidaSangre(value: String) = _uiState.update { it.copy(horaComida = value.filter { char -> char.isDigit() }.take(2)) }

    fun onMinutoComidaSangre(value: String) = _uiState.update { it.copy(minutoComida = value.filter { char -> char.isDigit() }.take(2)) }

    fun onPeriodoComidaSangre(value: String) = _uiState.update { it.copy(periodoComida = value) }

    fun onGlucosaSangre(value: String) = _uiState.update { it.copy(glucosa = value) }

    fun onUnidadGlucosaSangre(value: String) = _uiState.update { it.copy(unidadGlucosa = value) }

    fun onHaceActividadSangre(value: Boolean) = _uiState.update { it.copy(haceActividad = value) }

    fun onMinutosActividadSangre(value: String) = _uiState.update { it.copy(minutosActividad = value) }

    fun onTipoActividadSangre(value: String) = _uiState.update { it.copy(tipoActividad = value) }

    fun onNRojosSangre(value: String) = _uiState.update { it.copy(nRojos = value.filter { char -> char.isDigit() }) }

    fun onNBlancosSangre(value: String) = _uiState.update { it.copy(nBlancos = value.filter { char -> char.isDigit() }) }

    fun onNGeneralSangre(value: String) = _uiState.update { it.copy(nGeneral = value.filter { char -> char.isDigit() }) }

    fun onDilucionGeneralSangre(value: String) = _uiState.update { it.copy(dilucionGeneral = value) }

    fun onAreaGeneralSangre(value: String) = _uiState.update { it.copy(areaGeneral = value) }

    fun guardarMedidasPiel(imagenPath: String, bloque: String) {
        if (tipo != TipoAnalisisMicroscopio.PIEL) return
        val texto = bloque.trim()
        if (texto.isEmpty()) return
        viewModelScope.launch {
            val item = _uiState.value.historial.firstOrNull { it.imagenPath == imagenPath } ?: return@launch
            val unido = PielCalculoNotas.unir(item.analisis, texto)
            if (unido == item.analisis) return@launch
            runCatching { analisisRepository.insert(item.copy(analisis = unido)) }
        }
    }

    fun guardarMedidasHoja(imagenPath: String, bloque: String) {
        if (tipo != TipoAnalisisMicroscopio.HOJA) return
        val texto = bloque.trim()
        if (texto.isEmpty()) return
        viewModelScope.launch {
            val item = _uiState.value.historial.firstOrNull { it.imagenPath == imagenPath } ?: return@launch
            val unido = HojaCalculoNotas.unir(item.analisis, texto)
            if (unido == item.analisis) return@launch
            runCatching { analisisRepository.insert(item.copy(analisis = unido)) }
        }
    }

    private fun promptVersionActual(): Int = when (tipo) {
        TipoAnalisisMicroscopio.SANGRE -> GeminiPrompts.SANGRE_PROMPT_VERSION
        else -> GeminiPrompts.MICROSCOPIO_PROMPT_VERSION
    }

    private fun promptForTipo(resumenAnterior: String?): String = when (tipo) {
        TipoAnalisisMicroscopio.YOGUR -> GeminiPrompts.analizarYogurMicroscopio(resumenAnterior)
        TipoAnalisisMicroscopio.PIEL -> GeminiPrompts.analizarPielMicroscopio()
        TipoAnalisisMicroscopio.HOJA -> GeminiPrompts.analizarHojaMicroscopio()
        TipoAnalisisMicroscopio.SANGRE -> GeminiPrompts.analizarSangreMicroscopio(datosAnotadosSangre())
    }

    private fun TipoAnalisisMicroscopio.subtipo(): String? = when (this) {
        TipoAnalisisMicroscopio.PIEL -> "piel_escamas"
        TipoAnalisisMicroscopio.HOJA -> "celulas_hoja"
        else -> null
    }

    private fun resumenDesdeAnalisis(analisis: String): String =
        analisis.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(8)
            .joinToString(" ")
            .take(900)

    private fun mergeUvcError(uvcError: String?, currentError: String?): String? {
        if (uvcError != null) return uvcError
        if (currentError != null && isUvcRelatedError(currentError)) return null
        return currentError
    }

    private fun isUvcRelatedError(message: String): Boolean {
        val normalized = message.lowercase()
        return normalized.contains("permiso usb") ||
            normalized.contains("microscopio desconectado") ||
            normalized.contains("conecta el microscopio") ||
            normalized.contains("preview uvc") ||
            normalized.contains("microscopio usb")
    }

    private fun validarRespuestaGemini(respuesta: String) {
        val normalized = respuesta.lowercase()
        val esErrorTransitorio = normalized.startsWith("sin conexión") ||
            normalized.startsWith("falta configurar") ||
            normalized.startsWith("esperá un momento") ||
            normalized.startsWith("no se pudo") ||
            normalized.startsWith("se alcanzó") ||
            normalized.contains("api key") ||
            normalized.contains("currently experiencing high demand") ||
            normalized.contains("try again later") ||
            normalized.contains("model is overloaded") ||
            normalized.contains("overloaded")

        if (esErrorTransitorio) {
            throw IllegalStateException(
                "Gemini no pudo analizar la imagen en este momento. Intenta de nuevo en unos minutos."
            )
        }
    }

    private fun registrarLlamadaGemini(analysisId: Long, imagenPath: String) {
        val nextCount = _uiState.value.geminiCallsEnAnalisisActual + 1
        _uiState.update { it.copy(geminiCallsEnAnalisisActual = nextCount) }
        com.laboratoriofisico.app.usb.UsbDebugNdjson.log(
            hypothesisId = "GEMINI-ONE-CALL",
            location = "MicroscopioAnalisisViewModel.registrarLlamadaGemini",
            message = "Llamada real a Gemini para análisis de microscopio",
            data = mapOf(
                "analysisId" to analysisId,
                "callCountForAnalysis" to nextCount,
                "tipo" to tipo.key,
                "imagenPath" to imagenPath
            )
        )
    }

    private fun analysisCacheKey(imagenPath: String): String {
        val file = File(imagenPath)
        val base = "${file.absolutePath}|${file.lastModified()}|${file.length()}|${promptVersionActual()}"
        return if (tipo == TipoAnalisisMicroscopio.SANGRE) {
            "$base|${datosAnotadosSangre()}"
        } else {
            base
        }
    }

    private fun datosAnotadosSangre(): String {
        val state = _uiState.value
        return SangreFormulario.contexto(
            comio = state.comio,
            hora = state.horaComida,
            minuto = state.minutoComida,
            periodo = state.periodoComida,
            glucosa = state.glucosa,
            unidadGlucosa = state.unidadGlucosa,
            haceActividad = state.haceActividad,
            minutosActividad = state.minutosActividad,
            tipoActividad = state.tipoActividad
        )
    }
}
