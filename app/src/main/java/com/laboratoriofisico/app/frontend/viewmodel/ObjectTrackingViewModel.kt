package com.laboratoriofisico.app.frontend.viewmodel

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.repository.ObjectTrackingMeasurementRepository
import com.laboratoriofisico.app.ia.gemini.GeminiClient
import com.laboratoriofisico.app.ia.gemini.GeminiPrompts
import com.laboratoriofisico.app.backend.repository.SavedObjectTrackingMeasurement
import com.laboratoriofisico.app.sensores.SensorManager
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.util.DistanceCalibration
import com.laboratoriofisico.app.sensores.util.LumaFrame
import com.laboratoriofisico.app.sensores.util.ObjectMotionAnalyzer
import com.laboratoriofisico.app.sensores.util.ObjectTracker
import com.laboratoriofisico.app.sensores.util.PixelCalibration
import com.laboratoriofisico.app.sensores.util.TrackingOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/** Punto de la trayectoria en píxeles reales del frame, con el tiempo transcurrido. */
data class TrackPoint(
    val timeSeconds: Double,
    val x: Double,
    val y: Double
)

data class ObjectTrackingResult(
    val detectado: Boolean,
    val tiempoSegundos: Double,
    val initialX: Int,
    val initialY: Int,
    val finalX: Int,
    val finalY: Int,
    val deltaX: Int,
    val deltaY: Int,
    val distanciaPx: Double,
    val velocidadMediaPxPorSegundo: Double?
)

data class ObjectTrackingUiState(
    val disponible: Boolean = false,
    val permisoRequerido: Boolean = false,
    val hardwareState: CameraHardwareState = CameraHardwareState.CLOSED,
    val camaraActiva: Boolean = false,
    val objetivoSeleccionado: Boolean = false,
    val estadoSeguimiento: TrackingOutcome = TrackingOutcome.IDLE,
    val initialX: Int? = null,
    val initialY: Int? = null,
    val currentX: Int? = null,
    val currentY: Int? = null,
    val deltaX: Int? = null,
    val deltaY: Int? = null,
    val distanciaPx: Double? = null,
    val tiempoSegundos: Double = 0.0,
    /** Distancia en línea recta desde la posición inicial hasta la posición actual. */
    val desplazamientoPx: Double? = null,
    /** Distancia total acumulada tramo a tramo mientras el objeto se mueve. */
    val distanciaTotalPx: Double? = null,
    /** Velocidad instantánea sobre una ventana corta; tiende a 0 cuando el objeto se detiene. */
    val velocidadPxPorSegundo: Double? = null,
    /** Desplazamiento total dividido por el tiempo total de seguimiento. */
    val velocidadMediaPxPorSegundo: Double? = null,
    val velocidadMaximaPxPorSegundo: Double? = null,
    val vecesMovimiento: Int = 0,
    val enMovimiento: Boolean = false,
    val confianza: Double? = null,
    val marcadorX: Float? = null,
    val marcadorY: Float? = null,
    val frameSourceWidth: Int? = null,
    val frameSourceHeight: Int? = null,
    val trayectoria: List<TrackPoint> = emptyList(),
    val resultado: ObjectTrackingResult? = null,
    val calibracion: PixelCalibration? = null,
    val distanciaCm: Double? = null,
    val distanciaTotalCm: Double? = null,
    val puedeGuardar: Boolean = false,
    val guardando: Boolean = false,
    val guardado: SavedObjectTrackingMeasurement? = null,
    val mensaje: String? = null,
    val identificando: Boolean = false,
    val descripcionObjeto: String? = null
)

/**
 * Seguimiento de un objeto sobre los frames reales de CameraX.
 *
 * Todas las cifras (posición, desplazamiento, distancia, tiempo y velocidad) se derivan de
 * los frames de ImageAnalysis y de sus timestamps. Cuando el rastreador pierde el objeto el
 * estado pasa a [TrackingOutcome.LOST] y no se conserva la última posición como si siguiera ahí.
 */
class ObjectTrackingViewModel(
    private val sensorManager: SensorManager,
    private val measurementRepository: ObjectTrackingMeasurementRepository,
    private val appContext: Context,
    private val geminiClient: GeminiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectTrackingUiState())
    val uiState: StateFlow<ObjectTrackingUiState> = _uiState.asStateFlow()

    private val tracker = ObjectTracker()
    private val motionAnalyzer = ObjectMotionAnalyzer()
    private val analyzing = AtomicBoolean(false)
    private val identificando = AtomicBoolean(false)
    private val identificacionGeneration = AtomicInteger(0)

    @Volatile
    private var pendingSelection: Pair<Float, Float>? = null

    @Volatile
    private var startTimestampMs: Long? = null

    @Volatile
    private var initialSourceX: Int? = null

    @Volatile
    private var initialSourceY: Int? = null

    private var wasActiveBeforePause = false
    private val frameCount = AtomicInteger(0)
    private val firstFrameMs = AtomicLong(0L)
    private val lastAnalyzedFrameMs = AtomicLong(0L)
    private var objetoDiagLogs = 0

    init {
        refreshAvailability()
        // Mantener hardwareState al día: el overlay del preview depende de RUNNING.
        viewModelScope.launch {
            sensorManager.observeCameraHardwareState().collect { hwState ->
                _uiState.update { it.copy(hardwareState = hwState) }
            }
        }
        viewModelScope.launch {
            sensorManager.observeState(SensorType.CAMERA).collect { state ->
                _uiState.update {
                    it.copy(
                        permisoRequerido = state == SensorState.PERMISSION_DENIED,
                        disponible = sensorManager.isSensorAvailable(SensorType.CAMERA)
                    )
                }
            }
        }
    }

    fun refreshAvailability() {
        _uiState.update {
            it.copy(
                disponible = sensorManager.isSensorAvailable(SensorType.CAMERA),
                permisoRequerido = sensorManager.observeState(SensorType.CAMERA).value ==
                    SensorState.PERMISSION_DENIED,
                hardwareState = sensorManager.observeCameraHardwareState().value
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        // #region agent log
        com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
            hypothesisId = "H-CAM-H",
            location = "ObjectTrackingViewModel.onPermissionResult",
            message = "resultado permiso cámara",
            data = mapOf("granted" to granted)
        )
        // #endregion
        sensorManager.setPermissionGranted(SensorType.CAMERA, granted)
        refreshAvailability()
    }

    fun iniciar(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        // #region agent log
        com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
            hypothesisId = "H-CAM-J",
            location = "ObjectTrackingViewModel.iniciar",
            message = "iniciar cámara",
            data = mapOf(
                "camaraActiva" to _uiState.value.camaraActiva,
                "disponible" to _uiState.value.disponible,
                "permisoRequerido" to _uiState.value.permisoRequerido,
                "hardwareState" to _uiState.value.hardwareState.name,
                "sensorState" to sensorManager.observeState(SensorType.CAMERA).value.name
            )
        )
        // #endregion
        if (_uiState.value.camaraActiva) return
        resetTracking()
        analyzing.set(true)
        // Un solo flujo: preview + análisis + tracking con el mismo bind.
        sensorManager.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = CameraLensFacing.BACK
        )
        sensorManager.setCameraFrameAnalyzer(::onFrame)
        val hw = sensorManager.observeCameraHardwareState().value
        val sensorState = sensorManager.observeState(SensorType.CAMERA).value
        val opened = hw == CameraHardwareState.RUNNING ||
            hw == CameraHardwareState.INITIALIZING ||
            hw == CameraHardwareState.READY
        _uiState.update {
            it.copy(
                camaraActiva = opened,
                hardwareState = hw,
                permisoRequerido = sensorState == SensorState.PERMISSION_DENIED,
                mensaje = if (opened) {
                    null
                } else {
                    "No se pudo abrir la cámara (${hw.name})"
                }
            )
        }
        if (!opened) {
            analyzing.set(false)
            sensorManager.setCameraFrameAnalyzer(null)
        }
        // #region agent log
        android.util.Log.i(
            "ObjetoDiag",
            "iniciar.afterBind hw=$hw sensor=$sensorState camaraActiva=$opened analyzing=$opened"
        )
        com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
            hypothesisId = "H-CAM-I",
            location = "ObjectTrackingViewModel.iniciar.afterBind",
            message = "estado tras bindCamera",
            data = mapOf(
                "hardwareState" to hw.name,
                "sensorState" to sensorState.name,
                "camaraActiva" to opened
            )
        )
        // #endregion
    }

    fun detener() {
        if (!_uiState.value.camaraActiva) return
        analyzing.set(false)
        sensorManager.setCameraFrameAnalyzer(null)
        sensorManager.unbindCamera()
        _uiState.update { state ->
            val result = buildResult(state)
            state.copy(
                camaraActiva = false,
                hardwareState = CameraHardwareState.CLOSED,
                resultado = result,
                puedeGuardar = result != null,
                marcadorX = null,
                marcadorY = null
            )
        }
    }

    /** Coordenadas normalizadas (0..1) del toque dentro del área del preview. */
    fun onSeleccionarObjeto(normalizedX: Float, normalizedY: Float) {
        if (!_uiState.value.camaraActiva) return
        pendingSelection = normalizedX.coerceIn(0f, 1f) to normalizedY.coerceIn(0f, 1f)
    }

    fun setCalibracion(referenceSizeCm: Double?, observedPixels: Double?) {
        val calibration = if (referenceSizeCm != null && observedPixels != null) {
            PixelCalibration(referenceSizeCm, observedPixels).takeIf { it.isValid }
        } else {
            null
        }
        _uiState.update { state ->
            state.copy(
                calibracion = calibration,
                distanciaCm = DistanceCalibration.toCentimeters(
                    state.desplazamientoPx ?: state.distanciaPx ?: 0.0,
                    calibration
                ),
                distanciaTotalCm = DistanceCalibration.toCentimeters(
                    state.distanciaTotalPx ?: 0.0,
                    calibration
                )
            )
        }
    }

    fun guardarResultado() {
        val state = _uiState.value
        if (!state.puedeGuardar || state.guardando) return
        _uiState.update { it.copy(guardando = true, mensaje = null) }
        viewModelScope.launch {
            measurementRepository.guardar(state)
                .onSuccess { saved ->
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            puedeGuardar = false,
                            guardado = saved,
                            mensaje = "Medición guardada en Medir."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            mensaje = error.message ?: "No se pudo guardar la medición."
                        )
                    }
                }
        }
    }

    fun descartarResultado() {
        resetTracking()
    }

    fun onPause() {
        wasActiveBeforePause = _uiState.value.camaraActiva
        analyzing.set(false)
        sensorManager.setCameraFrameAnalyzer(null)
        if (_uiState.value.camaraActiva) {
            sensorManager.unbindCamera()
            _uiState.update {
                it.copy(camaraActiva = false, hardwareState = CameraHardwareState.CLOSED)
            }
        }
        // No llamar sensorManager.onPause(): detiene TODOS los sensores y deja paused=true,
        // lo que rompe péndulo/movimiento al volver a otras pantallas.
    }

    fun onResume(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        refreshAvailability()
        if (wasActiveBeforePause) {
            wasActiveBeforePause = false
            iniciar(lifecycleOwner, previewView)
        }
    }

    override fun onCleared() {
        analyzing.set(false)
        sensorManager.setCameraFrameAnalyzer(null)
        sensorManager.unbindCamera()
        super.onCleared()
    }

    /** Invocado desde el hilo de ImageAnalysis por cada frame real de la cámara. */
    private fun onFrame(frame: LumaFrame) {
        if (!analyzing.get()) return
        logFrameReceipt(frame)

        val pending = pendingSelection
        if (pending == null && shouldSkipFrame(frame.timestampMs)) return
        if (pending != null) {
            pendingSelection = null
            lastAnalyzedFrameMs.set(frame.timestampMs)
            val (nx, ny) = pending
            val targetX = (nx * (frame.width - 1)).roundToInt()
            val targetY = (ny * (frame.height - 1)).roundToInt()
            val selection = tracker.selectTarget(frame, targetX, targetY)
            publishUi {
                if (selection.outcome != TrackingOutcome.TRACKED) {
                    it.copy(
                        estadoSeguimiento = selection.outcome,
                        objetivoSeleccionado = false,
                        mensaje = when (selection.outcome) {
                            TrackingOutcome.INSUFFICIENT_CONTRAST ->
                                "Poco contraste en ese punto; toca un objeto con bordes claros"
                            else -> "No se pudo fijar el objeto; vuelve a tocar"
                        }
                    )
                } else {
                    val position = selection.position!!
                    val sourceX = (position.x * frame.scaleToSourceX).roundToInt()
                    val sourceY = (position.y * frame.scaleToSourceY).roundToInt()
                    val motion = motionAnalyzer.select(
                        timestampMs = frame.timestampMs,
                        x = sourceX.toDouble(),
                        y = sourceY.toDouble()
                    )
                    startTimestampMs = frame.timestampMs
                    initialSourceX = sourceX
                    initialSourceY = sourceY
                    it.copy(
                        objetivoSeleccionado = true,
                        estadoSeguimiento = TrackingOutcome.TRACKED,
                        initialX = sourceX,
                        initialY = sourceY,
                        currentX = sourceX,
                        currentY = sourceY,
                        deltaX = 0,
                        deltaY = 0,
                        distanciaPx = 0.0,
                        desplazamientoPx = motion.displacementPx,
                        distanciaTotalPx = motion.totalDistancePx,
                        tiempoSegundos = 0.0,
                        velocidadPxPorSegundo = 0.0,
                        velocidadMediaPxPorSegundo = 0.0,
                        velocidadMaximaPxPorSegundo = 0.0,
                        vecesMovimiento = 0,
                        enMovimiento = false,
                        confianza = position.confidence,
                        marcadorX = position.x.toFloat() / frame.width,
                        marcadorY = position.y.toFloat() / frame.height,
                        frameSourceWidth = frame.sourceWidth,
                        frameSourceHeight = frame.sourceHeight,
                        trayectoria = motion.path.map { point ->
                            TrackPoint(point.timeSeconds, point.x, point.y)
                        },
                        resultado = null,
                        puedeGuardar = false,
                        guardado = null,
                        mensaje = null
                    )
                }
            }
            // #region agent log
            android.util.Log.i(
                "ObjetoDiag",
                "select.outcome=${selection.outcome} nx=${"%.3f".format(nx)} ny=${"%.3f".format(ny)} " +
                    "target=$targetX,$targetY grid=${frame.width}x${frame.height}"
            )
            // #endregion
            logTracking("select", frame, selection.matchScore, targetX, targetY)
            if (selection.outcome == TrackingOutcome.TRACKED) {
                solicitarIdentificacion()
            }
            return
        }

        if (!tracker.hasTarget) return

        lastAnalyzedFrameMs.set(frame.timestampMs)
        val result = tracker.track(frame)
        if (result.outcome != TrackingOutcome.TRACKED || result.position == null) {
            publishUi {
                it.copy(
                    estadoSeguimiento = result.outcome,
                    confianza = null,
                    marcadorX = null,
                    marcadorY = null
                )
            }
            logTracking("lost", frame, result.matchScore, null, null)
            return
        }

        val position = result.position
        val sourceX = (position.x * frame.scaleToSourceX).roundToInt()
        val sourceY = (position.y * frame.scaleToSourceY).roundToInt()
        val motion = motionAnalyzer.update(frame.timestampMs, sourceX.toDouble(), sourceY.toDouble())
        val currentX = motion.currentX.roundToInt()
        val currentY = motion.currentY.roundToInt()
        val originX = motion.initialX.roundToInt()
        val originY = motion.initialY.roundToInt()
        val deltaX = currentX - originX
        val deltaY = currentY - originY

        publishUi { state ->
            val trayectoria = motion.path.map { point ->
                TrackPoint(point.timeSeconds, point.x, point.y)
            }
            state.copy(
                estadoSeguimiento = TrackingOutcome.TRACKED,
                currentX = currentX,
                currentY = currentY,
                deltaX = deltaX,
                deltaY = deltaY,
                distanciaPx = motion.displacementPx,
                desplazamientoPx = motion.displacementPx,
                distanciaTotalPx = motion.totalDistancePx,
                tiempoSegundos = motion.elapsedSeconds,
                velocidadPxPorSegundo = motion.currentSpeedPxPerSecond,
                velocidadMediaPxPorSegundo = motion.averageSpeedPxPerSecond,
                velocidadMaximaPxPorSegundo = motion.maxSpeedPxPerSecond,
                vecesMovimiento = motion.movementCount,
                enMovimiento = motion.isMoving,
                confianza = position.confidence,
                marcadorX = position.x.toFloat() / frame.width,
                marcadorY = position.y.toFloat() / frame.height,
                frameSourceWidth = frame.sourceWidth,
                frameSourceHeight = frame.sourceHeight,
                trayectoria = trayectoria,
                distanciaCm = DistanceCalibration.toCentimeters(motion.displacementPx, state.calibracion),
                distanciaTotalCm = DistanceCalibration.toCentimeters(motion.totalDistancePx, state.calibracion)
            )
        }
        logTracking(
            "track",
            frame,
            result.matchScore,
            currentX,
            currentY,
            motion.displacementPx,
            motion.currentSpeedPxPerSecond
        )
    }

    private fun publishUi(block: (ObjectTrackingUiState) -> ObjectTrackingUiState) {
        viewModelScope.launch {
            _uiState.update(block)
        }
    }

    private fun logFrameReceipt(frame: LumaFrame) {
        val count = frameCount.incrementAndGet()
        if (count == 1) firstFrameMs.set(frame.timestampMs)
        if (count > 8) return
        val elapsed = (frame.timestampMs - firstFrameMs.get()).coerceAtLeast(1L) / 1000.0
        val fps = count / elapsed
        android.util.Log.i(
            "ObjetoDiag",
            "frame #$count ${frame.sourceWidth}x${frame.sourceHeight} grid=${frame.width}x${frame.height} fps~%.1f"
                .format(fps)
        )
    }

    private fun logTracking(
        phase: String,
        frame: LumaFrame,
        score: Double?,
        x: Int?,
        y: Int?,
        distance: Double? = null,
        speed: Double? = null
    ) {
        if (objetoDiagLogs >= 12) return
        objetoDiagLogs++
        android.util.Log.i(
            "ObjetoDiag",
            "$phase score=${score?.let { "%.1f".format(it) } ?: "n/a"} " +
                "X=$x Y=$y dist=${distance?.let { "%.1f".format(it) } ?: "n/a"} " +
                "vel=${speed?.let { "%.1f".format(it) } ?: "n/a"}px/s"
        )
    }

    private fun shouldSkipFrame(timestampMs: Long): Boolean {
        val previous = lastAnalyzedFrameMs.get()
        if (previous == 0L) return false
        return timestampMs - previous < MIN_ANALYSIS_INTERVAL_MS
    }

    private fun buildResult(state: ObjectTrackingUiState): ObjectTrackingResult? {
        val initialX = state.initialX ?: return null
        val initialY = state.initialY ?: return null
        val currentX = state.currentX ?: return null
        val currentY = state.currentY ?: return null
        return ObjectTrackingResult(
            detectado = state.estadoSeguimiento == TrackingOutcome.TRACKED,
            tiempoSegundos = state.tiempoSegundos,
            initialX = initialX,
            initialY = initialY,
            finalX = currentX,
            finalY = currentY,
            deltaX = state.deltaX ?: (currentX - initialX),
            deltaY = state.deltaY ?: (currentY - initialY),
            distanciaPx = state.distanciaPx ?: 0.0,
            velocidadMediaPxPorSegundo = state.velocidadMediaPxPorSegundo
        )
    }

    /** Una sola consulta a Gemini por objeto. Si ya hay una en curso, no se lanza otra. */
    private fun solicitarIdentificacion() {
        if (!identificando.compareAndSet(false, true)) return
        val generation = identificacionGeneration.incrementAndGet()
        _uiState.update { it.copy(identificando = true, descripcionObjeto = null) }
        val dir = File(appContext.cacheDir, "objetos").also { it.mkdirs() }
        val file = File(dir, "objeto_${System.currentTimeMillis()}.jpg")
        sensorManager.captureCameraPhoto(file) { result ->
            viewModelScope.launch {
                val path = result.getOrNull()
                if (generation != identificacionGeneration.get()) {
                    identificando.set(false)
                    return@launch
                }
                if (path == null) {
                    identificando.set(false)
                    _uiState.update {
                        it.copy(
                            identificando = false,
                            descripcionObjeto = "No se pudo ver el objeto. Tócalo otra vez."
                        )
                    }
                    return@launch
                }
                val texto = geminiClient.generarConImagen(GeminiPrompts.identificarObjeto(), path)
                identificando.set(false)
                if (generation != identificacionGeneration.get()) return@launch
                _uiState.update {
                    it.copy(identificando = false, descripcionObjeto = texto)
                }
            }
        }
    }

    private fun resetTracking() {
        tracker.reset()
        motionAnalyzer.reset()
        pendingSelection = null
        startTimestampMs = null
        initialSourceX = null
        initialSourceY = null
        frameCount.set(0)
        firstFrameMs.set(0L)
        lastAnalyzedFrameMs.set(0L)
        objetoDiagLogs = 0
        identificacionGeneration.incrementAndGet()
        identificando.set(false)
        _uiState.update { state ->
            ObjectTrackingUiState(
                disponible = state.disponible,
                permisoRequerido = state.permisoRequerido,
                calibracion = state.calibracion
            )
        }
    }

    private companion object {
        const val MIN_ANALYSIS_INTERVAL_MS = 33L
    }
}
