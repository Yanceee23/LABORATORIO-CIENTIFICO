package com.laboratoriofisico.app.frontend.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.AnalisisMicroscopio
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.repository.AnalisisMicroscopioRepository
import com.laboratoriofisico.app.backend.repository.MedicionRepository
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.backend.sangre.NeubauerConteo
import com.laboratoriofisico.app.backend.sangre.TipoConteoNeubauer
import com.laboratoriofisico.app.backend.session.SessionRepository
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
    val incluirHipotesisEducativaSangre: Boolean = false,
    val edad: String = "",
    val peso: String = "",
    val comio: String = "",
    val horaComida: String = "",
    val glucosa: String = "",
    val celulasContadas: String = "",
    val tipoConteo: TipoConteoNeubauer = TipoConteoNeubauer.ROJOS,
    val dilucion: String = "",
    val volumenMicrolitros: String = "",
    val lecturasSangre: List<Medicion> = emptyList(),
    val guardandoLectura: Boolean = false
)

class MicroscopioAnalisisViewModel(
    private val tipo: TipoAnalisisMicroscopio,
    private val appContext: Context,
    private val analisisRepository: AnalisisMicroscopioRepository,
    private val geminiClient: GeminiClient,
    val uvcMicroscopeManager: UvcMicroscopeManager,
    private val medicionRepository: MedicionRepository,
    private val resultadoRepository: ResultadoRepository,
    private val sessionRepository: SessionRepository
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
                if (tipo == TipoAnalisisMicroscopio.SANGRE) {
                    viewModelScope.launch {
                        medicionRepository.observeByExperimento(experimentoId).collect { lecturas ->
                            _uiState.update {
                                it.copy(
                                    lecturasSangre = lecturas.filter { medicion ->
                                        medicion.unidad == NeubauerConteo.UNIDAD_GLUCOSA ||
                                            medicion.unidad == NeubauerConteo.UNIDAD
                                    }
                                )
                            }
                        }
                    }
                }
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
                val respuesta = geminiClient.generarConImagen(prompt, imagenPath)
                validarRespuestaGemini(respuesta)
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

    fun setIncluirHipotesisEducativaSangre(value: Boolean) {
        _uiState.update { it.copy(incluirHipotesisEducativaSangre = value) }
    }

    fun onEdadSangre(value: String) = _uiState.update { it.copy(edad = value) }

    fun onPesoSangre(value: String) = _uiState.update { it.copy(peso = value) }

    fun onComioSangre(value: String) = _uiState.update { it.copy(comio = value) }

    fun onHoraComidaSangre(value: String) = _uiState.update { it.copy(horaComida = value) }

    fun onGlucosaSangre(value: String) = _uiState.update { it.copy(glucosa = value) }

    fun onCelulasSangre(value: String) = _uiState.update { it.copy(celulasContadas = value) }

    fun onTipoConteoSangre(value: TipoConteoNeubauer) = _uiState.update { it.copy(tipoConteo = value) }

    fun onDilucionSangre(value: String) = _uiState.update { it.copy(dilucion = value) }

    fun onVolumenSangre(value: String) = _uiState.update { it.copy(volumenMicrolitros = value) }

    fun conteoNeubauer(state: MicroscopioAnalisisUiState): Double? {
        val n = state.celulasContadas.trim().toIntOrNull() ?: return null
        return NeubauerConteo.calcular(
            tipo = state.tipoConteo,
            celulasContadas = n,
            dilucion = state.dilucion.aNumeroPositivo(),
            volumenMicrolitros = state.volumenMicrolitros.aNumeroPositivo()
        )
    }

    fun guardarLecturaSangre() {
        if (tipo != TipoAnalisisMicroscopio.SANGRE || _uiState.value.guardandoLectura) return
        val state = _uiState.value
        val glucosa = state.glucosa.aNumeroPositivo()
        val glucosaEscrita = state.glucosa.isNotBlank()
        val glucosaValida = glucosa != null && glucosa <= 600.0
        val conteo = conteoNeubauer(state)
        if (glucosaEscrita && !glucosaValida) {
            _uiState.update {
                it.copy(error = "La glucosa debe ser mayor que 0 y no pasar de 600 mg/dL.", mensaje = null)
            }
            return
        }
        if (!glucosaValida && conteo == null) {
            val detalle = if (state.celulasContadas.isNotBlank() && state.tipoConteo == TipoConteoNeubauer.OTRA) {
                "Para otra dilución escribe la dilución y el volumen en µL."
            } else {
                "Escribe la glucosa o las células contadas."
            }
            _uiState.update { it.copy(error = detalle, mensaje = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardandoLectura = true, error = null, mensaje = null) }
            runCatching {
                val experimentoId = state.experimentoId ?: analisisRepository.ensureExperimento(tipo)
                val usuarioId = sessionRepository.getLoggedInUserId()
                    ?: throw IllegalStateException("Se requiere una cuenta registrada para guardar.")
                val notas = notasPersonales(state)
                val ahora = System.currentTimeMillis()
                if (glucosaValida && glucosa != null) {
                    val medicionId = medicionRepository.insert(
                        Medicion(
                            experimentoId = experimentoId,
                            usuarioId = usuarioId,
                            nombre = "Glucosa",
                            valor = glucosa,
                            unidad = NeubauerConteo.UNIDAD_GLUCOSA,
                            timestamp = ahora
                        )
                    )
                    resultadoRepository.insert(
                        Resultado(
                            medicionId = medicionId,
                            usuarioId = usuarioId,
                            valorCalculado = glucosa,
                            unidad = NeubauerConteo.UNIDAD_GLUCOSA,
                            notas = notas,
                            timestamp = ahora
                        )
                    )
                }
                if (conteo != null) {
                    val medicionId = medicionRepository.insert(
                        Medicion(
                            experimentoId = experimentoId,
                            usuarioId = usuarioId,
                            nombre = state.tipoConteo.titulo,
                            valor = conteo,
                            unidad = NeubauerConteo.UNIDAD,
                            timestamp = ahora
                        )
                    )
                    resultadoRepository.insert(
                        Resultado(
                            medicionId = medicionId,
                            usuarioId = usuarioId,
                            valorCalculado = conteo,
                            unidad = NeubauerConteo.UNIDAD,
                            notas = notas,
                            timestamp = ahora
                        )
                    )
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(guardandoLectura = false, mensaje = "Lectura guardada.", error = null)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        guardandoLectura = false,
                        error = error.message ?: "No se pudo guardar la lectura."
                    )
                }
            }
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
        TipoAnalisisMicroscopio.SANGRE -> GeminiPrompts.analizarSangreMicroscopio(
            solicitaHipotesisEducativa = _uiState.value.incluirHipotesisEducativaSangre,
            datosAnotados = datosAnotadosSangre()
        )
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
        val conteo = conteoNeubauer(state)
        val glucosa = state.glucosa.aNumeroPositivo()
        return buildString {
            appendLine("Edad: ${state.edad.ifBlank { "no indicada" }}")
            appendLine("Peso: ${state.peso.ifBlank { "no indicado" }}")
            appendLine("Qué comió: ${state.comio.ifBlank { "no indicado" }}")
            appendLine("Hora de la comida: ${state.horaComida.ifBlank { "no indicada" }}")
            if (glucosa != null && glucosa <= 600.0) {
                appendLine("Glucosa anotada: ${NeubauerConteo.texto(glucosa)} mg/dL")
            } else {
                appendLine("Glucosa: no indicada")
            }
            if (conteo != null) {
                appendLine(
                    "Conteo de cámara de Neubauer (${state.tipoConteo.titulo}): " +
                        "${NeubauerConteo.texto(conteo)} células por µL. " +
                        "Es un cálculo de la cámara, no un conteo de la foto."
                )
            } else {
                appendLine("Conteo de cámara de Neubauer: no indicado.")
            }
        }.trim()
    }

    private fun notasPersonales(state: MicroscopioAnalisisUiState): String =
        "Edad: ${state.edad.ifBlank { "no indicada" }}. " +
            "Peso: ${state.peso.ifBlank { "no indicado" }}. " +
            "Comió: ${state.comio.ifBlank { "no indicado" }}. " +
            "Hora: ${state.horaComida.ifBlank { "no indicada" }}."

    private fun String.aNumeroPositivo(): Double? {
        val valor = trim().replace(',', '.').toDoubleOrNull() ?: return null
        return valor.takeIf { it > 0.0 }
    }
}
