package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LifecycleOwner
import com.laboratoriofisico.app.backend.repository.GuardarMedicionResult
import com.laboratoriofisico.app.backend.repository.MedicionSensorRepository
import com.laboratoriofisico.app.backend.repository.SensorMedicionSnapshot
import com.laboratoriofisico.app.core.debug.DebugSessionLog
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.fisica.FisicaEngine
import com.laboratoriofisico.app.frontend.sensor.MedicionEstado
import com.laboratoriofisico.app.sensores.SensorManager
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.util.CameraSceneLux
import com.laboratoriofisico.app.sensores.util.CompassCalculator
import com.laboratoriofisico.app.sensores.util.MagneticAnomalyDetector
import com.laboratoriofisico.app.sensores.util.MagneticAnomalyPhase
import com.laboratoriofisico.app.sensores.util.MagneticAnomalyResult
import com.laboratoriofisico.app.sensores.util.MagneticCardinals
import com.laboratoriofisico.app.sensores.util.NearbyMagnetism
import com.laboratoriofisico.app.sensores.util.LightClassifier
import com.laboratoriofisico.app.sensores.util.LiveDisplayFilter
import com.laboratoriofisico.app.sensores.util.LightLevel
import com.laboratoriofisico.app.sensores.util.PendulumAnalyzer
import com.laboratoriofisico.app.sensores.util.PendulumState
import com.laboratoriofisico.app.sensores.util.RelativeLightCalibrator
import com.laboratoriofisico.app.sensores.util.SensorValueExtractors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

data class SensorMedicionUiState(
    val kind: ExperimentoSensorKind,
    val medicionEstado: MedicionEstado = MedicionEstado.DETENIDO,
    val sensorState: SensorState = SensorState.IDLE,
    val disponible: Boolean = false,
    val permisoRequerido: Boolean = false,
    val lecturaActual: SensorReading? = null,
    val valorPrincipal: Double? = null,
    val historial: List<Double> = emptyList(),
    val timestamp: Long? = null,
    val mensaje: String? = null,
    val extras: Map<String, String> = emptyMap(),
    val penduloLongitudM: Double? = null,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val luzExperimental: Boolean = false,
    val lightLevel: LightLevel? = null,
    val lightProgress: Float? = null,
    val lightHistoryEntries: List<LightHistoryEntry> = emptyList(),
    val pendulum: PendulumState = PendulumState(),
    val pendulumSignal: List<Double> = emptyList()
)

class SensorMedicionViewModel(
    private val sensorManager: SensorManager,
    private val fisicaEngine: FisicaEngine,
    private val medicionSensorRepository: MedicionSensorRepository,
    val kind: ExperimentoSensorKind
) : ViewModel() {

    private val sensorType = kind.sensorType
    private val _uiState = MutableStateFlow(SensorMedicionUiState(kind = kind))
    val uiState: StateFlow<SensorMedicionUiState> = _uiState.asStateFlow()

    private var readingsJob: Job? = null
    private var extraReadingsJob: Job? = null
    private var lastLocation: Pair<Double, Double>? = null
    private var accumulatedDistance = 0.0
    private var measurementStartMs: Long? = null
    private val pendulumAnalyzer = PendulumAnalyzer()
    private var pendulumState = PendulumState()
    private val compassCalculator = CompassCalculator()
    private val magneticAnomalyDetector = MagneticAnomalyDetector()
    private var magnetismShownUt: Double? = null
    private val liveFilter = LiveDisplayFilter()
    private var lightClassificationLogCount = 0
    private var pendulumSampleCount = 0
    private var pendulumFirstSampleMs: Long? = null
    private var magnetDiagUiCount = 0
    private var filteredPitchDeg: Double? = null
    private var filteredRollDeg: Double? = null
    private val relativeLightCalibrator = RelativeLightCalibrator()
    private var relativeLightLevel: LightLevel? = null
    private var cameraLifecycleOwner: LifecycleOwner? = null
    private var cameraExposureLocked = false

    init {
        _uiState.update {
            it.copy(
                disponible = sensorManager.isSensorAvailable(sensorType),
                sensorState = sensorManager.observeState(sensorType).value,
                luzExperimental = isLightExperimentalMode()
            )
        }
        if (kind == ExperimentoSensorKind.LUZ && sensorManager.isSensorAvailable(SensorType.LIGHT)) {
            sensorManager.startReading(SensorType.LIGHT)
        }
        viewModelScope.launch {
            if (usesCameraForLight()) {
                sensorManager.observeState(SensorType.CAMERA).collect { state ->
                    refreshCameraLightAvailability(state)
                }
            } else {
                combine(
                    sensorManager.observeState(sensorType),
                    sensorManager.observeLatestReading(sensorType)
                ) { state, reading -> state to reading }
                    .collect { (state, reading) ->
                        refreshAvailabilityInternal(state, reading)
                    }
            }
        }
    }

    /** El S20 FE no expone TYPE_LIGHT a apps de terceros; la cámara sí cambia con la luz real. */
    private fun usesCameraForLight(): Boolean =
        kind == ExperimentoSensorKind.LUZ &&
            !sensorManager.isSensorAvailable(SensorType.LIGHT) &&
            sensorManager.isSensorAvailable(SensorType.CAMERA)

    fun permisosRequeridos(): Array<String> =
        if (usesCameraForLight()) {
            com.laboratoriofisico.app.sensores.permissions.SensorPermissions
                .requiredPermissions(SensorType.CAMERA)
        } else {
            com.laboratoriofisico.app.sensores.permissions.SensorPermissions
                .requiredPermissions(sensorType)
        }

    private fun readingSource(): SensorType =
        if (usesCameraForLight()) SensorType.CAMERA else sensorType

    private fun refreshAvailabilityInternal(state: SensorState, reading: SensorReading?) {
        val disponible = sensorManager.isSensorAvailable(sensorType)
        val permiso = state == SensorState.PERMISSION_DENIED
        val resolvedState = if (
            disponible &&
            (state == SensorState.UNAVAILABLE || state == SensorState.DISABLED)
        ) {
            SensorState.IDLE
        } else {
            state
        }
        // #region agent log
        if (kind == ExperimentoSensorKind.LUZ) {
            DebugSessionLog.log(
                hypothesisId = "H3",
                location = "SensorMedicionViewModel.refreshAvailability",
                message = "Estado UI sensor luz",
                data = mapOf(
                    "disponible" to disponible,
                    "rawState" to state.name,
                    "resolvedState" to resolvedState.name,
                    "permisoRequerido" to permiso
                )
            )
        }
        // #endregion
        // REFLEXION adapta cada frame de CAMERA a una lectura LIGHT relativa en processReading;
        // no publicar aquí la luma sin adaptar porque competiría con el valor RAW equivalente.
        val valor = if (kind == ExperimentoSensorKind.REFLEXION) null else reading?.let { extractPrimary(it) }
        val lightSnapshot = if (
            (kind == ExperimentoSensorKind.LUZ || kind == ExperimentoSensorKind.REFLEXION) &&
            reading != null &&
            valor != null
        ) {
            buildLightSnapshot(reading, valor)
        } else {
            null
        }
        _uiState.update { current ->
            current.copy(
                sensorState = resolvedState,
                disponible = disponible,
                permisoRequerido = permiso,
                luzExperimental = isLightExperimentalMode(),
                lecturaActual = if (kind == ExperimentoSensorKind.REFLEXION) {
                    current.lecturaActual
                } else {
                    reading ?: current.lecturaActual
                },
                valorPrincipal = if (kind == ExperimentoSensorKind.MAGNETISMO) {
                    current.valorPrincipal
                } else {
                    valor ?: current.valorPrincipal
                },
                timestamp = if (kind == ExperimentoSensorKind.REFLEXION) {
                    current.timestamp
                } else {
                    reading?.timestamp ?: current.timestamp
                },
                extras = when {
                    kind == ExperimentoSensorKind.REFLEXION -> current.extras
                    kind == ExperimentoSensorKind.MAGNETISMO &&
                        current.medicionEstado == MedicionEstado.ACTIVO -> current.extras
                    else -> reading?.let { buildExtras(it, current.medicionEstado, lightSnapshot?.level) }
                        ?: current.extras
                },
                lightLevel = lightSnapshot?.level ?: current.lightLevel,
                lightProgress = lightSnapshot?.progress ?: current.lightProgress
            )
        }
    }

    private data class LightSnapshot(
        val level: LightLevel,
        val progress: Float,
        val unit: String
    )

    /** Número, barra y nivel a partir de la misma lectura y la unidad real del sensor. */
    private fun buildLightSnapshot(reading: SensorReading, value: Double): LightSnapshot {
        val unit = reading.unit
        val measurementUnit = LightClassifier.unitFromReading(reading)
        val maxRange = SensorValueExtractors.lightSensorMaxRange(reading)
        val fromCamera = reading.metadata?.contains("source=camera_luma") == true
        val level = if (kind == ExperimentoSensorKind.REFLEXION || fromCamera) {
            relativeLightLevel ?: LightLevel.MEDIUM
        } else {
            LightClassifier.classifyLight(value, measurementUnit)
        }
        val progress = if (kind == ExperimentoSensorKind.REFLEXION || fromCamera) {
            reading.metadata
                ?.substringAfter("relative_change=")
                ?.substringBefore(';')
                ?.toDoubleOrNull()
                ?.let { change ->
                    val luma = reading.metadata.substringAfter("luma=").substringBefore(';').toDoubleOrNull() ?: 0.0
                    val calibrated = reading.metadata.contains("auto_calibrated=true")
                    relativeLightCalibrator.progressFor(change, luma, calibrated)
                }
                ?: LightClassifier.lightProgress(value, measurementUnit, maxRange)
        } else {
            LightClassifier.lightProgress(value, measurementUnit, maxRange)
        }
        logLightUiPipeline(reading, value, unit, level, progress)
        return LightSnapshot(level = level, progress = progress, unit = unit)
    }

    private fun logLightUiPipeline(
        reading: SensorReading,
        value: Double,
        unit: String,
        level: LightLevel,
        progress: Float
    ) {
        if (lightClassificationLogCount >= 8) return
        lightClassificationLogCount++
        val meta = reading.metadata.orEmpty()
        fun metaValue(key: String): String =
            meta.split(';').firstOrNull { it.startsWith("$key=") }?.substringAfter('=') ?: "n/a"
        android.util.Log.i(
            "LuzDiag",
            "UI #$lightClassificationLogCount: valor=$value unit=$unit progress=$progress " +
                "nivel=${LightClassifier.levelDisplayName(level)} " +
                "sensor=${metaValue("sensor_name")} type=${metaValue("sensor_type")} " +
                "maxRange=${metaValue("sensor_max_range")}"
        )
    }

    private fun refreshAvailability() {
        if (usesCameraForLight()) {
            refreshCameraLightAvailability(sensorManager.observeState(SensorType.CAMERA).value)
            return
        }
        refreshAvailabilityInternal(
            state = sensorManager.observeState(sensorType).value,
            reading = sensorManager.observeLatestReading(sensorType).value
        )
    }

    private fun refreshCameraLightAvailability(cameraState: SensorState) {
        val denied = cameraState == SensorState.PERMISSION_DENIED
        val missing = cameraState == SensorState.UNAVAILABLE
        _uiState.update { current ->
            current.copy(
                sensorState = when {
                    denied -> SensorState.PERMISSION_DENIED
                    cameraState == SensorState.ERROR -> SensorState.ERROR
                    missing -> SensorState.UNAVAILABLE
                    current.medicionEstado == MedicionEstado.ACTIVO -> SensorState.ACTIVE
                    else -> SensorState.IDLE
                },
                disponible = !missing,
                permisoRequerido = denied,
                luzExperimental = true
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        val permissionType = if (usesCameraForLight()) SensorType.CAMERA else sensorType
        sensorManager.setPermissionGranted(permissionType, granted)
        if (!granted) return
        if (kind == ExperimentoSensorKind.REFLEXION &&
            _uiState.value.medicionEstado == MedicionEstado.ACTIVO
        ) {
            cameraLifecycleOwner?.let(::bindReflectionCamera)
        }
        if (usesCameraForLight()) {
            ensureCameraLightRunning()
        }
    }

    fun setPenduloLongitud(metros: Double?) {
        _uiState.update { it.copy(penduloLongitudM = metros?.takeIf { m -> m > 0 }) }
    }

    fun guardarResultado() {
        if (_uiState.value.isSaving) return
        val state = _uiState.value
        val valor = state.valorPrincipal
        if (valor == null && state.historial.isEmpty()) {
            _uiState.update { it.copy(saveMessage = "No hay lecturas para guardar") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null) }
            when (
                val result = medicionSensorRepository.guardar(
                    kind = kind,
                    snapshot = SensorMedicionSnapshot(
                        valorPrincipal = valor ?: state.historial.last(),
                        unidad = when {
                            state.lecturaActual?.unit == "lux" -> "lux"
                            state.luzExperimental -> "raw"
                            else -> kind.unidadDisplay
                        },
                        lectura = state.lecturaActual,
                        historial = state.historial,
                        extras = state.extras,
                        penduloLongitudM = state.penduloLongitudM,
                        luzExperimental = state.luzExperimental,
                        lightHistoryEntries = state.lightHistoryEntries
                    )
                )
            ) {
                is GuardarMedicionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveMessage = "Resultado guardado: ${result.resultado.valorCalculado} ${result.resultado.unidad}"
                        )
                    }
                }
                is GuardarMedicionResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, saveMessage = result.message) }
                }
            }
        }
    }

    private var wasMeasuringBeforePause = false

    fun iniciar(lifecycleOwner: LifecycleOwner? = null) {
        if (_uiState.value.medicionEstado == MedicionEstado.ACTIVO) return
        if (kind == ExperimentoSensorKind.REFLEXION || usesCameraForLight()) {
            if (lifecycleOwner != null) cameraLifecycleOwner = lifecycleOwner
        }
        resetMeasurementState()
        if (kind == ExperimentoSensorKind.MAGNETISMO) {
            compassCalculator.reset()
            magneticAnomalyDetector.reset()
            magnetDiagUiCount = 0
            sensorManager.setSamplingRate(com.laboratoriofisico.app.sensores.model.SamplingRate.NORMAL)
        }
        if (kind == ExperimentoSensorKind.PENDULO) {
            sensorManager.setSamplingRate(com.laboratoriofisico.app.sensores.model.SamplingRate.HIGH)
            // #region agent log
            android.util.Log.i(
                "PenduloDiag",
                "iniciar PENDULO sampling=${com.laboratoriofisico.app.sensores.model.SamplingRate.HIGH} " +
                    "state=${sensorManager.observeState(SensorType.ACCELEROMETER).value}"
            )
            // #endregion
        }
        _uiState.update { it.copy(medicionEstado = MedicionEstado.ACTIVO, mensaje = null) }
        startReadingCollection()
        if (kind == ExperimentoSensorKind.REFLEXION) {
            // No heredar SamplingRate.HIGH del péndulo: la inclinación del acelerómetro basta con NORMAL.
            sensorManager.setSamplingRate(com.laboratoriofisico.app.sensores.model.SamplingRate.NORMAL)
            // #region agent log
            DebugSessionLog.log(
                hypothesisId = "H-LCAM-BIND",
                location = "SensorMedicionViewModel.iniciar",
                message = "Iniciar medición REFLEXION",
                data = mapOf(
                    "hasLifecycleOwner" to (lifecycleOwner != null),
                    "permisoRequerido" to _uiState.value.permisoRequerido,
                    "samplingRate" to "NORMAL"
                ),
                runId = "luz-isolation"
            )
            // #endregion
            lifecycleOwner?.let(::bindReflectionCamera)
                ?: _uiState.update { it.copy(mensaje = "No se pudo vincular la cámara al ciclo de vida") }
        } else if (kind == ExperimentoSensorKind.LUZ) {
            if (usesCameraForLight()) {
                val owner = cameraLifecycleOwner
                if (owner != null) {
                    bindReflectionCamera(owner)
                } else {
                    _uiState.update { it.copy(mensaje = "No se pudo vincular la cámara al ciclo de vida") }
                }
            } else {
                sensorManager.startReading(SensorType.LIGHT, forceRestart = true)
            }
        } else {
            sensorManager.startReading(sensorType, forceRestart = true)
        }
        kind.extraSensorType?.let { sensorManager.startReading(it, forceRestart = true) }
        // Sembrar UI con la última lectura conocida (si el sensor ya emitió).
        sensorManager.observeLatestReading(readingSource()).value?.let { processReading(it) }
    }

    private fun bindReflectionCamera(lifecycleOwner: LifecycleOwner) {
        cameraLifecycleOwner = lifecycleOwner
        // #region agent log
        android.util.Log.i("LuzCamaraDiag", "bindReflectionCamera lifecycle=${lifecycleOwner.javaClass.simpleName}")
        DebugSessionLog.log(
            hypothesisId = "H-LCAM-BIND",
            location = "SensorMedicionViewModel.bindReflectionCamera",
            message = "Vinculando cámara trasera para medición relativa",
            data = mapOf(
                "medicionEstado" to _uiState.value.medicionEstado.name,
                "disponible" to _uiState.value.disponible,
                "permisoRequerido" to _uiState.value.permisoRequerido,
                "sensorState" to _uiState.value.sensorState.name
            ),
            runId = "relative-light"
        )
        // #endregion
        sensorManager.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = null,
            lensFacing = CameraLensFacing.BACK
        )
    }

    private fun startReadingCollection() {
        readingsJob?.cancel()
        readingsJob = viewModelScope.launch {
            sensorManager.observeReadings(readingSource()).collect { reading ->
                processReading(reading)
            }
        }
        kind.extraSensorType?.let { extraType ->
            extraReadingsJob?.cancel()
            extraReadingsJob = viewModelScope.launch {
                sensorManager.observeReadings(extraType).collect { reading ->
                    if (kind == ExperimentoSensorKind.MAGNETISMO) {
                        updateCompassAccelerometer(reading)
                    }
                    if (kind == ExperimentoSensorKind.REFLEXION) {
                        updateReflexionTilt(reading)
                    }
                }
            }
        }
    }

    fun detener() {
        readingsJob?.cancel()
        extraReadingsJob?.cancel()
        if (kind == ExperimentoSensorKind.REFLEXION || usesCameraForLight()) {
            if (cameraExposureLocked) sensorManager.setCameraAutoExposureLock(false)
            sensorManager.unbindCamera()
            cameraExposureLocked = false
        } else if (kind != ExperimentoSensorKind.LUZ) {
            sensorManager.stopReading(sensorType)
        }
        kind.extraSensorType?.let { sensorManager.stopReading(it) }
        _uiState.update { it.copy(medicionEstado = MedicionEstado.DETENIDO) }
    }

    fun onResume(lifecycleOwner: LifecycleOwner? = null) {
        if ((kind == ExperimentoSensorKind.REFLEXION || kind == ExperimentoSensorKind.LUZ) &&
            lifecycleOwner != null
        ) {
            cameraLifecycleOwner = lifecycleOwner
        }
        sensorManager.onResume()
        refreshAvailability()
        val resumeMeasurement = wasMeasuringBeforePause &&
            _uiState.value.medicionEstado == MedicionEstado.ACTIVO
        if (kind == ExperimentoSensorKind.LUZ && usesCameraForLight()) {
            if (resumeMeasurement) {
                relativeLightCalibrator.reset()
                relativeLightLevel = null
                cameraExposureLocked = false
                startReadingCollection()
                cameraLifecycleOwner?.let(::bindReflectionCamera)
            } else if (_uiState.value.medicionEstado != MedicionEstado.ACTIVO) {
                ensureCameraLightRunning()
            }
        } else if (kind == ExperimentoSensorKind.LUZ) {
            sensorManager.startReading(SensorType.LIGHT, forceRestart = true)
            sensorManager.observeLatestReading(SensorType.LIGHT).value?.let { reading ->
                refreshAvailabilityInternal(
                    sensorManager.observeState(SensorType.LIGHT).value,
                    reading
                )
            }
            if (resumeMeasurement) {
                startReadingCollection()
                sensorManager.startReading(SensorType.LIGHT, forceRestart = true)
            }
        } else if (resumeMeasurement) {
            startReadingCollection()
            if (kind == ExperimentoSensorKind.REFLEXION) {
                cameraLifecycleOwner?.let(::bindReflectionCamera)
            } else {
                sensorManager.startReading(sensorType)
            }
            kind.extraSensorType?.let { sensorManager.startReading(it, forceRestart = true) }
        }
        wasMeasuringBeforePause = false
    }

    private fun ensureCameraLightRunning() {
        if (!usesCameraForLight()) return
        val cameraState = sensorManager.observeState(SensorType.CAMERA).value
        refreshCameraLightAvailability(cameraState)
        if (cameraState == SensorState.PERMISSION_DENIED) return
        val owner = cameraLifecycleOwner ?: return
        relativeLightCalibrator.reset()
        relativeLightLevel = null
        cameraExposureLocked = false
        if (_uiState.value.medicionEstado != MedicionEstado.ACTIVO) {
            resetMeasurementState()
        }
        _uiState.update { it.copy(medicionEstado = MedicionEstado.ACTIVO, mensaje = null) }
        startReadingCollection()
        bindReflectionCamera(owner)
    }

    fun onPause() {
        wasMeasuringBeforePause = _uiState.value.medicionEstado == MedicionEstado.ACTIVO
        if (kind == ExperimentoSensorKind.REFLEXION || usesCameraForLight()) {
            cameraExposureLocked = false
        }
        if (wasMeasuringBeforePause) {
            readingsJob?.cancel()
            extraReadingsJob?.cancel()
            if (usesCameraForLight()) sensorManager.unbindCamera()
        } else {
            detener()
        }
        sensorManager.onPause()
    }

    override fun onCleared() {
        detener()
        if (kind == ExperimentoSensorKind.LUZ) {
            sensorManager.stopReading(SensorType.LIGHT)
        }
        cameraLifecycleOwner = null
        super.onCleared()
    }

    private fun resetMeasurementState() {
        magnetismShownUt = null
        liveFilter.reset()
        lastLocation = null
        accumulatedDistance = 0.0
        measurementStartMs = System.currentTimeMillis()
        pendulumAnalyzer.reset()
        pendulumState = PendulumState()
        pendulumSampleCount = 0
        pendulumFirstSampleMs = null
        _uiState.update {
            it.copy(
                historial = emptyList(),
                extras = emptyMap(),
                lightHistoryEntries = emptyList(),
                pendulum = PendulumState(),
                pendulumSignal = emptyList()
            )
        }
        if (kind == ExperimentoSensorKind.LUZ || kind == ExperimentoSensorKind.REFLEXION) {
            lightClassificationLogCount = 0
            filteredPitchDeg = null
            filteredRollDeg = null
        }
        if (kind == ExperimentoSensorKind.REFLEXION || usesCameraForLight()) {
            relativeLightCalibrator.reset()
            relativeLightLevel = null
            cameraExposureLocked = false
        }
    }

    private fun processReading(incomingReading: SensorReading) {
        val reading = adaptReflectionCameraReading(incomingReading)
        if (kind == ExperimentoSensorKind.DISTANCIA) {
            updateDistance(reading)
        }
        if (kind == ExperimentoSensorKind.PENDULO) {
            updatePendulumPeriod(reading)
        }
        if (kind == ExperimentoSensorKind.MAGNETISMO) {
            updateCompassMagnetometer(reading)
        }
        val value = extractPrimary(reading)
        if (value == null) {
            // El péndulo debe seguir mostrando tiempo, oscilaciones y señal mientras
            // todavía no hay un periodo fiable, en lugar de reportar 0.0 s.
            if (kind == ExperimentoSensorKind.PENDULO) {
                _uiState.update { state ->
                    state.copy(
                        lecturaActual = reading,
                        timestamp = reading.timestamp,
                        extras = buildExtras(reading, MedicionEstado.ACTIVO)
                    )
                }
            }
            return
        }
        // #region agent log
        val viaEngine = usesFisicaEngine(kind)
        DebugSessionLog.log(
            hypothesisId = "H-FLOW",
            location = "SensorMedicionViewModel.processReading",
            message = "Lectura procesada → valor pantalla",
            data = mapOf(
                "kind" to kind.name,
                "sensorType" to reading.sensorType.name,
                "valorPrincipal" to value,
                "rawScalar" to reading.valueScalar,
                "rawX" to reading.valueX,
                "rawY" to reading.valueY,
                "rawZ" to reading.valueZ,
                "viaFisicaEngine" to viaEngine
            ),
            runId = "post-fix"
        )
        if (viaEngine) {
            android.util.Log.i("H-ENGINE", "kind=${kind.name} valor=$value")
        }
        // #endregion
        val lightUpdate = if (
            kind == ExperimentoSensorKind.LUZ || kind == ExperimentoSensorKind.REFLEXION
        ) {
            buildLightUpdate(reading, value)
        } else {
            null
        }
        // #region agent log
        if (kind == ExperimentoSensorKind.LUZ || kind == ExperimentoSensorKind.REFLEXION) {
            DebugSessionLog.log(
                hypothesisId = "H-LUZ-C",
                location = "SensorMedicionViewModel.processReading",
                message = "Luz llegó al ViewModel",
                data = mapOf(
                    "kind" to kind.name,
                    "value" to value,
                    "unit" to reading.unit,
                    "progress" to lightUpdate?.progress,
                    "level" to lightUpdate?.level?.name,
                    "medicionEstado" to _uiState.value.medicionEstado.name
                ),
                runId = "post-fix"
            )
        }
        if (kind == ExperimentoSensorKind.MOVIMIENTO) {
            val dynamic = SensorValueExtractors.accelDynamicMagnitude(reading)
            DebugSessionLog.log(
                hypothesisId = "H-MOV-F",
                location = "SensorMedicionViewModel.processReading",
                message = "Movimiento lectura",
                data = mapOf(
                    "x" to reading.valueX,
                    "y" to reading.valueY,
                    "z" to reading.valueZ,
                    "dynamic" to dynamic,
                    "threshold" to 0.5,
                    "isMoving" to (dynamic?.let { SensorValueExtractors.isMoving(it) }),
                    "valorPrincipal" to value
                )
            )
        }
        // #endregion
        val extras = buildExtras(reading, MedicionEstado.ACTIVO, lightUpdate?.level)
        val shown = displayValue(value)
        _uiState.update { state ->
            state.copy(
                lecturaActual = reading,
                valorPrincipal = shown,
                timestamp = reading.timestamp,
                historial = when {
                    shown == null -> state.historial
                    state.historial.lastOrNull() == shown -> state.historial
                    else -> (state.historial + shown).takeLast(120)
                },
                extras = extras,
                lightLevel = lightUpdate?.level ?: state.lightLevel,
                lightProgress = lightUpdate?.progress ?: state.lightProgress,
                lightHistoryEntries = lightUpdate?.historyEntry?.let { entry ->
                    (state.lightHistoryEntries + entry).takeLast(120)
                } ?: state.lightHistoryEntries
            )
        }
    }

    private fun adaptCameraLuxReading(reading: SensorReading): SensorReading {
        val luma = reading.valueScalar
        val exposureNs = metadataLong(reading.metadata, "exposure_ns")
        val iso = metadataLong(reading.metadata, "iso")?.toInt()
        val aperture = metadataDouble(reading.metadata, "aperture")
        val lux = if (luma != null && exposureNs != null && iso != null) {
            CameraSceneLux.estimateLux(luma, exposureNs, iso, aperture)
        } else {
            null
        }
        if (lux == null) {
            android.util.Log.w(
                "LuzCamaraDiag",
                "sin datos para estimar lux luma=$luma exposureNs=$exposureNs iso=$iso"
            )
            return reading.copy(valueScalar = null)
        }
        android.util.Log.i(
            "LuzCamaraDiag",
            "lux=%.1f luma=%.2f exposureNs=%d iso=%d aperture=%s".format(
                lux,
                luma,
                exposureNs,
                iso,
                aperture?.toString() ?: "fallback"
            )
        )
        return reading.copy(
            sensorType = SensorType.LIGHT,
            valueScalar = lux,
            unit = "lux",
            metadata = buildString {
                append("source=camera_lux")
                append(";sensor_name=Rear Camera")
                append(";luma=$luma")
                append(";exposure_ns=$exposureNs")
                append(";iso=$iso")
                aperture?.let { append(";aperture=$it") }
            }
        )
    }

    private fun metadataLong(metadata: String?, key: String): Long? =
        metadata?.split(';')
            ?.firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            ?.toLongOrNull()

    private fun metadataDouble(metadata: String?, key: String): Double? =
        metadata?.split(';')
            ?.firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            ?.toDoubleOrNull()

    private fun adaptReflectionCameraReading(reading: SensorReading): SensorReading {
        if (reading.sensorType != SensorType.CAMERA) return reading
        if (usesCameraForLight()) return adaptCameraLuxReading(reading)
        if (kind != ExperimentoSensorKind.REFLEXION) return reading
        val luma = reading.valueScalar ?: return reading
        val result = relativeLightCalibrator.update(luma, reading.timestamp)
        relativeLightLevel = result.level
        if (result.calibrated && !cameraExposureLocked) {
            sensorManager.setCameraAutoExposureLock(true)
            cameraExposureLocked = true
        }
        val classification = LightClassifier.levelDisplayName(result.level)
        val baselineText = result.baseline?.let { "%.3f".format(it) } ?: "CALIBRANDO"
        // #region agent log
        android.util.Log.i(
            "LuzCamaraDiag",
            "lumaPromedio=%.3f rawEquivalente=%.1f baseline=%s cambioRelativo=%.4f clasificacion=%s calibrada=%s"
                .format(
                    result.luma,
                    result.rawEquivalent,
                    baselineText,
                    result.relativeChange,
                    classification,
                    result.calibrated
                )
        )
        DebugSessionLog.log(
            hypothesisId = "H-LCAM-REL",
            location = "SensorMedicionViewModel.adaptReflectionCameraReading",
            message = "Luma clasificada contra línea base automática",
            data = mapOf(
                "lumaAverage" to result.luma,
                "rawEquivalent" to result.rawEquivalent,
                "baseline" to result.baseline,
                "relativeChange" to result.relativeChange,
                "classification" to classification,
                "calibrated" to result.calibrated,
                "exposureLocked" to cameraExposureLocked
            ),
            runId = "relative-light"
        )
        // #endregion
        return reading.copy(
            sensorType = SensorType.LIGHT,
            valueScalar = result.rawEquivalent,
            unit = "raw",
            metadata = buildString {
                append("source=camera_luma")
                append(";sensor_name=Rear Camera")
                append(";sensor_type=camera")
                append(";sensor_max_range=60000")
                append(";luma=${result.luma}")
                result.baseline?.let { append(";baseline_luma=$it") }
                append(";relative_change=${result.relativeChange}")
                append(";auto_calibrated=${result.calibrated}")
            }
        )
    }

    private data class LightUpdate(
        val level: LightLevel,
        val progress: Float,
        val historyEntry: LightHistoryEntry
    )

    private fun buildLightUpdate(reading: SensorReading, value: Double): LightUpdate {
        val snapshot = buildLightSnapshot(reading, value)
        return LightUpdate(
            level = snapshot.level,
            progress = snapshot.progress,
            historyEntry = LightHistoryEntry(
                timestamp = reading.timestamp,
                value = value,
                unit = snapshot.unit,
                level = snapshot.level
            )
        )
    }

    private fun updateCompassAccelerometer(reading: SensorReading) {
        val x = reading.valueX ?: return
        val y = reading.valueY ?: return
        val z = reading.valueZ ?: return
        compassCalculator.updateAccelerometer(x, y, z)
    }

    private fun updateCompassMagnetometer(reading: SensorReading) {
        val x = reading.valueX ?: return
        val y = reading.valueY ?: return
        val z = reading.valueZ ?: return
        compassCalculator.updateMagnetometer(x, y, z)
    }

    private fun updateReflexionTilt(reading: SensorReading) {
        // Solo actualizar ángulos mientras la medición está activa.
        if (_uiState.value.medicionEstado != MedicionEstado.ACTIVO) return
        val tilt = SensorValueExtractors.tiltDegrees(reading) ?: return
        val pitch = smoothTilt(filteredPitchDeg, tilt.first).also { filteredPitchDeg = it }
        val roll = smoothTilt(filteredRollDeg, tilt.second).also { filteredRollDeg = it }
        _uiState.update { state ->
            state.copy(
                extras = state.extras + mapOf(
                    "angulo_inclinacion" to "%.1f°".format(pitch),
                    "angulo_lateral" to "%.1f°".format(roll)
                )
            )
        }
    }

    /** Filtro paso bajo para que los ángulos no vibren sin control. */
    private fun smoothTilt(previous: Double?, raw: Double): Double =
        if (previous == null) raw else previous + TILT_SMOOTH_ALPHA * (raw - previous)

    private fun displayValue(raw: Double?): Double? = when (kind) {
        ExperimentoSensorKind.MAGNETISMO ->
            magnetismShownUt?.let { liveFilter.stable("b", it, 1.0) }
        ExperimentoSensorKind.SONIDO,
        ExperimentoSensorKind.ONDAS ->
            raw?.let { liveFilter.stable("nivel", it, 1.0) }
        ExperimentoSensorKind.MOVIMIENTO ->
            raw?.let { liveFilter.stable("mag", it, 0.1) }
        ExperimentoSensorKind.LUZ ->
            if (usesCameraForLight()) raw?.let { liveFilter.stable("luz", it, 1.0) } else raw
        else -> raw
    }

    private fun extractPrimary(reading: SensorReading): Double? = when (kind) {
        ExperimentoSensorKind.LUZ,
        ExperimentoSensorKind.REFLEXION -> SensorValueExtractors.lightPrimaryValue(reading)
        ExperimentoSensorKind.SONIDO,
        ExperimentoSensorKind.ONDAS -> reading.valueScalar?.let(SensorValueExtractors::microphoneLevelPercent)
        ExperimentoSensorKind.FRECUENCIA -> reading.valueY?.takeIf { it > 0 }
            ?: reading.metadata?.substringAfter("freq=")?.toDoubleOrNull()
        ExperimentoSensorKind.MAGNETISMO -> magnetIntensityViaEngine(reading)
        ExperimentoSensorKind.DISTANCIA -> accumulatedDistance
        ExperimentoSensorKind.MOVIMIENTO -> SensorValueExtractors.accelMagnitude(reading)
        ExperimentoSensorKind.PENDULO -> pendulumState.periodSeconds
    }

    private fun buildExtras(reading: SensorReading, estado: MedicionEstado, lightLevel: LightLevel? = null): Map<String, String> {
        val map = mutableMapOf<String, String>()
        when (kind) {
            ExperimentoSensorKind.LUZ -> {
                val level = lightLevel
                    ?: _uiState.value.lightLevel
                    ?: LightClassifier.classifyFromReading(reading)
                level?.let {
                    map["nivel_iluminacion"] = LightClassifier.levelDisplayName(it)
                    map["descripcion_iluminacion"] = LightClassifier.levelDescription(it)
                }
            }
            ExperimentoSensorKind.REFLEXION -> {
                val level = lightLevel
                    ?: _uiState.value.lightLevel
                    ?: LightClassifier.classifyFromReading(reading)
                level?.let {
                    map["nivel_iluminacion"] = LightClassifier.levelDisplayName(it)
                    map["clasificacion"] = when (it) {
                        LightLevel.LOW -> "Baja"
                        LightLevel.MEDIUM -> "Moderada"
                        LightLevel.HIGH -> "Alta"
                    }
                }
                _uiState.value.extras["angulo_inclinacion"]?.let { map["angulo_inclinacion"] = it }
                _uiState.value.extras["angulo_lateral"]?.let { map["angulo_lateral"] = it }
            }
            ExperimentoSensorKind.SONIDO,
            ExperimentoSensorKind.ONDAS -> {
                SensorValueExtractors.dbRelativeFromReading(reading)?.let { db ->
                    SensorValueExtractors.clasificarSonidoRelativeDb(db)?.let { map["clasificacion"] = it }
                    map["nivel_db"] = "%.1f dB rel".format(db)
                }
                reading.valueY?.let { freq ->
                    map["frecuencia_hz"] = "%.0f".format(freq)
                    map["frecuencia_nota"] = "Estimación aproximada (zero-crossing)"
                    if (kind == ExperimentoSensorKind.ONDAS && freq > 0) {
                        val lambda = SPEED_OF_SOUND_MS / freq
                        map["longitud_onda_m"] = "%.3f".format(lambda)
                    }
                }
                reading.valueScalar?.let { map["amplitud_rms"] = "%.4f".format(it) }
                measurementStartMs?.let { start ->
                    val elapsed = ((reading.timestamp - start).coerceAtLeast(0)) / 1000
                    map["tiempo"] = formatDuration(elapsed)
                }
            }
            ExperimentoSensorKind.FRECUENCIA -> {
                reading.valueY?.let { map["frecuencia_hz"] = "%.0f".format(it) }
            }
            ExperimentoSensorKind.MAGNETISMO -> {
                val azimuth = compassCalculator.azimuthDegrees()
                azimuth?.let {
                    map["azimuth"] = "%.0f".format(it)
                    map["direccion"] = MagneticCardinals.fromAzimuth(it)
                }
                map["brujula_valida"] = if (compassCalculator.rotationMatrixValid()) "sí" else "interferencia"
                reading.valueX?.let { map["bx"] = "%.0f".format(liveFilter.stable("bx", it, 1.0)) }
                reading.valueY?.let { map["by"] = "%.0f".format(liveFilter.stable("by", it, 1.0)) }
                reading.valueZ?.let { map["bz"] = "%.0f".format(liveFilter.stable("bz", it, 1.0)) }
                magnetIntensityViaEngine(reading)?.let { intensity ->
                    val anomaly = if (estado == MedicionEstado.ACTIVO) {
                        magneticAnomalyDetector.observe(intensity, azimuth)
                    } else {
                        null
                    }
                    magnetismShownUt = if (
                        anomaly == null || anomaly.phase == MagneticAnomalyPhase.CALIBRATING
                    ) {
                        null
                    } else {
                        NearbyMagnetism.excessUt(anomaly.deltaUt)
                    }
                    anomaly?.baselineUt?.let { map["campo_base"] = "%.1f µT".format(it) }
                    if (anomaly == null) return@let
                    map["campo_actual"] = "%.0f µT".format(liveFilter.stable("btot", anomaly.currentUt, 1.0))
                    anomaly.deltaUt?.let { map["delta_campo"] = "%.1f µT".format(it) }
                    map["anomalia_estado"] = when (anomaly.phase) {
                        MagneticAnomalyPhase.CALIBRATING ->
                            "Calibrando (${anomaly.samplesCollected}/${anomaly.samplesRequired})"
                        MagneticAnomalyPhase.NORMAL -> "NORMAL"
                        MagneticAnomalyPhase.ANOMALY -> {
                            anomaly.peakDirectionCardinal?.let { dir ->
                                "IMÁN CERCA → $dir"
                            } ?: "ANOMALÍA DETECTADA"
                        }
                    }
                    anomaly.peakDirectionCardinal?.let { map["iman_direccion"] = it }
                    anomaly.hintMessage?.let { map["iman_hint"] = it }
                    logMagnetUi(reading, intensity, azimuth, anomaly)
                }
            }
            ExperimentoSensorKind.DISTANCIA -> {
                map["velocidad"] = formatAverageSpeed(reading)
                measurementStartMs?.let { start ->
                    val elapsed = ((reading.timestamp - start).coerceAtLeast(0)) / 1000
                    map["tiempo"] = formatDuration(elapsed)
                }
                reading.accuracy?.let { map["precision"] = "±%.0f m".format(it) }
            }
            ExperimentoSensorKind.MOVIMIENTO -> {
                val dynamic = liveFilter.stable(
                    "dyn",
                    SensorValueExtractors.accelDynamicMagnitude(reading) ?: 0.0,
                    0.2
                )
                map["estado_movimiento"] = if (SensorValueExtractors.isMoving(dynamic)) "En movimiento" else "En reposo"
                reading.valueX?.let { map["ax"] = "%.1f".format(liveFilter.stable("ax", it, 0.1)) }
                reading.valueY?.let { map["ay"] = "%.1f".format(liveFilter.stable("ay", it, 0.1)) }
                reading.valueZ?.let { map["az"] = "%.1f".format(liveFilter.stable("az", it, 0.1)) }
                map["cambio_ms2"] = "%.1f".format(dynamic)
                sensorManager.observeLatestReading(SensorType.LOCATION).value?.speed?.let {
                    map["velocidad"] = "%.2f m/s".format(it)
                }
                measurementStartMs?.let { start ->
                    val elapsed = ((reading.timestamp - start).coerceAtLeast(0)) / 1000
                    map["tiempo"] = formatDuration(elapsed)
                }
            }
            ExperimentoSensorKind.PENDULO -> {
                reading.valueX?.let { map["ax"] = "%.1f".format(liveFilter.stable("pax", it, 0.1)) }
                reading.valueY?.let { map["ay"] = "%.1f".format(liveFilter.stable("pay", it, 0.1)) }
                reading.valueZ?.let { map["az"] = "%.1f".format(liveFilter.stable("paz", it, 0.1)) }
                val estado = pendulumState
                map["oscilaciones"] = estado.oscillations.toString()
                map["tiempo_medicion"] = "%.1f s".format(estado.elapsedSeconds)
                map["eje_dominante"] = estado.dominantAxis?.name ?: "—"
                estado.amplitudeMs2?.let { map["amplitud"] = "%.2f m/s²".format(it) }
                map["estado_senal"] = when {
                    !estado.hasEnoughSignal -> "Señal insuficiente"
                    estado.periodSeconds != null -> "Midiendo…"
                    else -> "Detectando oscilaciones…"
                }
                estado.frequencyHz?.let { map["frecuencia_hz"] = "%.2f Hz".format(it) }
                estado.periodSeconds?.let { periodoMedido ->
                    _uiState.value.penduloLongitudM?.let { longitud ->
                        val periodoTeorico = 2.0 * PI * sqrt(longitud / GRAVITY_MS2)
                        map["periodo_teorico"] = "%.2f s".format(periodoTeorico)
                        fisicaEngine.calcular(
                            FormulaId.G_PENDULO.id,
                            mapOf("L" to longitud, "T" to periodoMedido)
                        )?.let { gravedad ->
                            map["gravedad_calculada"] = "%.2f m/s²".format(gravedad)
                        }
                    }
                }
            }
        }
        if (estado == MedicionEstado.ACTIVO) {
            map["midiendo"] = "true"
        }
        return map
    }

    private fun updateDistance(reading: SensorReading) {
        val lat = reading.valueX ?: return
        val lon = reading.valueY ?: return
        val prev = lastLocation
        if (prev != null) {
            accumulatedDistance += SensorValueExtractors.haversineMeters(prev.first, prev.second, lat, lon)
        }
        lastLocation = lat to lon
    }

    private fun updatePendulumPeriod(reading: SensorReading) {
        val x = reading.valueX ?: return
        val y = reading.valueY ?: return
        val z = reading.valueZ ?: return
        // Timestamp monótono del hardware: la hora de entrega en el hilo principal
        // introduce jitter suficiente para falsear el periodo.
        val timestampMs = SensorValueExtractors.motionEventTimestampMs(reading) ?: reading.timestamp
        pendulumState = pendulumAnalyzer.update(timestampMs, x, y, z)
        logPendulumDiagnostics(timestampMs, reading)
        _uiState.update { state ->
            state.copy(
                pendulum = pendulumState,
                pendulumSignal = (state.pendulumSignal + pendulumState.signal).takeLast(240)
            )
        }
    }

    /** Diagnóstico acotado del péndulo: cadencia real, eje, amplitud y ciclos detectados. */
    private fun logPendulumDiagnostics(timestampMs: Long, reading: SensorReading) {
        pendulumSampleCount++
        if (pendulumFirstSampleMs == null) pendulumFirstSampleMs = timestampMs
        if (pendulumSampleCount % PENDULUM_LOG_EVERY != 0) return
        if (pendulumSampleCount / PENDULUM_LOG_EVERY > PENDULUM_LOG_LIMIT) return
        val start = pendulumFirstSampleMs ?: return
        val elapsedSec = (timestampMs - start) / 1000.0
        val rateHz = if (elapsedSec > 0.0) pendulumSampleCount / elapsedSec else 0.0
        val samplePeriodUs = reading.metadata
            ?.split(';')
            ?.firstOrNull { it.startsWith("sample_period_us=") }
            ?.substringAfter('=')
        android.util.Log.i(
            "PenduloDiag",
            "muestras=$pendulumSampleCount cadencia=%.1fHz periodoReg=${samplePeriodUs ?: "?"}us eje=%s amp=%s ultimoCiclo=%s promedio=%s f=%s cruces=%d"
                .format(
                    rateHz,
                    pendulumState.dominantAxis?.name ?: "n/a",
                    pendulumState.amplitudeMs2?.let { "%.2f m/s²".format(it) } ?: "n/a",
                    pendulumState.lastCyclePeriodSec?.let { "%.3fs".format(it) } ?: "n/a",
                    pendulumState.averagePeriodSec?.let { "%.3fs".format(it) } ?: "n/a",
                    pendulumState.frequencyHz?.let { "%.3fHz".format(it) } ?: "n/a",
                    pendulumState.oscillations
                )
        )
    }

    /** Diagnóstico acotado del magnetómetro: raw, intensidad, azimut y dirección del imán. */
    private fun logMagnetUi(
        reading: SensorReading,
        intensity: Double,
        azimuth: Double?,
        anomaly: MagneticAnomalyResult
    ) {
        magnetDiagUiCount++
        if (magnetDiagUiCount > 12) return
        if (magnetDiagUiCount > 4 && magnetDiagUiCount % 3 != 0) return
        android.util.Log.i(
            "MagnetometroDiag",
            "UI #$magnetDiagUiCount Bx=${reading.valueX} By=${reading.valueY} Bz=${reading.valueZ} " +
                "B=${"%.2f".format(intensity)}µT azimut=${azimuth?.let { "%.1f°".format(it) } ?: "n/a"} " +
                "cardinal=${azimuth?.let { MagneticCardinals.fromAzimuth(it) } ?: "n/a"} " +
                "fase=${anomaly.phase} delta=${anomaly.deltaUt?.let { "%.1f".format(it) } ?: "n/a"} " +
                "imanDir=${anomaly.peakDirectionCardinal ?: "n/a"} " +
                "rotOk=${compassCalculator.rotationMatrixValid()} accuracy=${
                    reading.metadata?.split(';')?.firstOrNull { it.startsWith("accuracy=") }?.substringAfter('=')
                        ?: "?"
                }"
        )
    }

    private fun formatAverageSpeed(reading: SensorReading): String {
        val start = measurementStartMs ?: return reading.speed?.let { "%.2f m/s".format(it) } ?: "—"
        val elapsedSec = ((reading.timestamp - start).coerceAtLeast(0)) / 1000.0
        val avg = if (elapsedSec > 0.0 && accumulatedDistance > 0.0) {
            accumulatedDistance / elapsedSec
        } else {
            reading.speed
        }
        // #region agent log
        if (kind == ExperimentoSensorKind.DISTANCIA) {
            DebugSessionLog.log(
                hypothesisId = "H2",
                location = "SensorMedicionViewModel.formatAverageSpeed",
                message = "Cálculo velocidad",
                data = mapOf(
                    "distanciaM" to accumulatedDistance,
                    "elapsedSec" to elapsedSec,
                    "gpsInstantMs" to reading.speed,
                    "avgMs" to avg
                )
            )
        }
        // #endregion
        return avg?.let { "%.2f m/s".format(it) } ?: "—"
    }

    private fun formatDuration(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun magnetIntensityViaEngine(reading: SensorReading): Double? {
        val x = reading.valueX ?: return null
        val y = reading.valueY ?: return null
        val z = reading.valueZ ?: return null
        return fisicaEngine.calcular(
            FormulaId.B_INTENSIDAD.id,
            mapOf("Bx" to x, "By" to y, "Bz" to z)
        )
    }

    private fun usesFisicaEngine(kind: ExperimentoSensorKind): Boolean =
        kind == ExperimentoSensorKind.MAGNETISMO ||
            (kind == ExperimentoSensorKind.PENDULO && _uiState.value.penduloLongitudM != null)

    private fun isLightExperimentalMode(): Boolean =
        kind == ExperimentoSensorKind.REFLEXION ||
            usesCameraForLight() ||
            (sensorType == SensorType.LIGHT && sensorManager.isLightSensorExperimental())

    private companion object {
        const val SPEED_OF_SOUND_MS = 343.0
        const val GRAVITY_MS2 = 9.80665
        const val PENDULUM_LOG_EVERY = 100
        const val PENDULUM_LOG_LIMIT = 5
        const val TILT_SMOOTH_ALPHA = 0.12
    }
}
