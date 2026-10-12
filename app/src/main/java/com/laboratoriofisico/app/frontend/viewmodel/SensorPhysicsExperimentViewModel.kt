package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.repository.SavedSensorExperimentResult
import com.laboratoriofisico.app.backend.repository.SensorPhysicsExperimentRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.experimentos.sensores.MotionSensorSample
import com.laboratoriofisico.app.experimentos.sensores.ProcessedSensorExperiment
import com.laboratoriofisico.app.experimentos.sensores.SensorPhysicsExperimentProcessor
import com.laboratoriofisico.app.experimentos.sensores.SensorPhysicsExperimentType
import com.laboratoriofisico.app.sensores.SensorManager
import com.laboratoriofisico.app.sensores.model.SamplingRate
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.util.LiveDisplayFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SensorPhysicsExperimentUiState(
    val isCapturing: Boolean = false,
    val isSaving: Boolean = false,
    val type: SensorPhysicsExperimentType? = null,
    val sampleCount: Int = 0,
    val elapsedSeconds: Double = 0.0,
    val latestAccelerationMs2: Double? = null,
    val latestAx: Double? = null,
    val latestAy: Double? = null,
    val latestAz: Double? = null,
    val latestSummary: ProcessedSensorExperiment? = null,
    val canSave: Boolean = false,
    val savedResult: SavedSensorExperimentResult? = null,
    val error: String? = null
)

class SensorPhysicsExperimentViewModel(
    private val sensorManager: SensorManager,
    private val repository: SensorPhysicsExperimentRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SensorPhysicsExperimentUiState())
    val uiState: StateFlow<SensorPhysicsExperimentUiState> = _uiState.asStateFlow()

    private val readings = mutableListOf<SensorReading>()
    private var captureJob: Job? = null
    private var startedAtMs: Long? = null
    private val liveFilter = LiveDisplayFilter()
    private var lastUiPublishMs = 0L

    fun iniciar(type: SensorPhysicsExperimentType) {
        if (_uiState.value.isCapturing) return
        if (!sensorManager.isSensorAvailable(SensorType.ACCELEROMETER)) {
            _uiState.update {
                it.copy(error = "El acelerómetro no está disponible en este dispositivo.")
            }
            return
        }

        readings.clear()
        startedAtMs = null
        liveFilter.reset()
        lastUiPublishMs = 0L
        val captureWallStart = System.currentTimeMillis()
        _uiState.value = SensorPhysicsExperimentUiState(isCapturing = true, type = type)
        // Otra pantalla pudo dejar el gestor en pausa. Sin esto, startReading no arranca
        // y la prueba se queda en la última lectura vieja.
        sensorManager.onResume()
        sensorManager.setSamplingRate(SamplingRate.HIGH)
        captureJob = viewModelScope.launch {
            sensorManager.observeReadings(SensorType.ACCELEROMETER).collect { reading ->
                if (reading.timestamp + 1_000L < captureWallStart) return@collect
                val sample = MotionSensorSample.fromReading(reading) ?: return@collect
                readings.add(reading)
                if (startedAtMs == null) startedAtMs = sample.timestampMs
                val nowMs = android.os.SystemClock.elapsedRealtime()
                if (readings.size > 1 && nowMs - lastUiPublishMs < 120L) return@collect
                lastUiPublishMs = nowMs
                val elapsed = ((sample.timestampMs - (startedAtMs ?: sample.timestampMs)).coerceAtLeast(0L)) / 1000.0
                _uiState.update {
                    it.copy(
                        sampleCount = readings.size,
                        elapsedSeconds = elapsed,
                        latestAccelerationMs2 = liveFilter.stable("mag", sample.magnitude, 0.1),
                        latestAx = liveFilter.stable("ax", sample.ax, 0.1),
                        latestAy = liveFilter.stable("ay", sample.ay, 0.1),
                        latestAz = liveFilter.stable("az", sample.az, 0.1),
                        latestSummary = SensorPhysicsExperimentProcessor.process(
                            type = type,
                            samples = readings.mapNotNull(MotionSensorSample::fromReading)
                        ),
                        error = null
                    )
                }
            }
        }
        sensorManager.startReading(SensorType.ACCELEROMETER, forceRestart = true)
    }

    fun detener(pendulumLengthM: Double? = null) {
        val type = _uiState.value.type ?: return
        if (!_uiState.value.isCapturing && readings.isEmpty()) return

        captureJob?.cancel()
        captureJob = null
        sensorManager.stopReading(SensorType.ACCELEROMETER)
        val samples = readings.mapNotNull(MotionSensorSample::fromReading)
        val processed = SensorPhysicsExperimentProcessor.process(type, samples, pendulumLengthM)
        viewModelScope.launch {
            val loggedIn = sessionRepository.getLoggedInUserId() != null
            val canSave = processed != null && loggedIn
            _uiState.update {
                it.copy(
                    isCapturing = false,
                    latestSummary = processed ?: it.latestSummary,
                    canSave = canSave,
                    error = when {
                        processed == null -> "No hay lecturas reales suficientes para guardar el experimento."
                        !loggedIn -> "Inicia sesión con una cuenta para guardar. El invitado no guarda datos."
                        else -> null
                    }
                )
            }
        }
    }

    fun guardarResultado() {
        val processed = _uiState.value.latestSummary ?: return
        if (!_uiState.value.canSave || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            runCatching {
                repository.guardar(processed)
            }.onSuccess { saved ->
                _uiState.update {
                    it.copy(isSaving = false, savedResult = saved, canSave = false, error = null)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSaving = false, error = throwable.message ?: "No se pudo guardar el experimento.")
                }
            }
        }
    }

    fun cancelar() {
        captureJob?.cancel()
        captureJob = null
        sensorManager.stopReading(SensorType.ACCELEROMETER)
        readings.clear()
        _uiState.value = SensorPhysicsExperimentUiState()
    }

    override fun onCleared() {
        captureJob?.cancel()
        sensorManager.stopReading(SensorType.ACCELEROMETER)
        super.onCleared()
    }
}
