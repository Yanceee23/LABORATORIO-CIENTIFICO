package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.repository.CapturaMagnetometroRepository
import com.laboratoriofisico.app.sensores.SensorManager
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CapturaMagnetometroUiState(
    val available: Boolean = false,
    val state: SensorState = SensorState.IDLE,
    val latestReading: SensorReading? = null,
    val capturedCount: Int = 0,
    val isSaving: Boolean = false,
    val saveMessage: String? = null
)

class CapturaMagnetometroViewModel(
    private val experimentoId: Long,
    private val sensorManager: SensorManager,
    private val capturaMagnetometroRepository: CapturaMagnetometroRepository
) : ViewModel() {

    private val sensorType = SensorType.MAGNETOMETER
    private val capturedReadings = mutableListOf<SensorReading>()

    private val _uiState = MutableStateFlow(CapturaMagnetometroUiState())
    val uiState: StateFlow<CapturaMagnetometroUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sensorManager.observeReadings(sensorType).collect { reading ->
                if (_uiState.value.state == SensorState.ACTIVE) {
                    capturedReadings.add(reading)
                    _uiState.value = _uiState.value.copy(
                        latestReading = reading,
                        capturedCount = capturedReadings.size
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                sensorManager.observeState(sensorType),
                sensorManager.observeLatestReading(sensorType)
            ) { state, reading ->
                state to reading
            }.collect { (state, reading) ->
                _uiState.value = _uiState.value.copy(
                    available = sensorManager.isSensorAvailable(sensorType),
                    state = state,
                    latestReading = reading ?: _uiState.value.latestReading
                )
            }
        }
    }

    fun startSensor() {
        capturedReadings.clear()
        _uiState.value = _uiState.value.copy(
            capturedCount = 0,
            saveMessage = null
        )
        sensorManager.startReading(sensorType)
    }

    fun stopSensor() {
        sensorManager.stopReading(sensorType)
    }

    fun guardarCaptura() {
        if (_uiState.value.isSaving) return
        val promedios = promediarComponentes(capturedReadings)
            ?: leerComponentes(_uiState.value.latestReading)
            ?: run {
                _uiState.value = _uiState.value.copy(saveMessage = "No hay lecturas para guardar")
                return
            }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveMessage = null)
            val resultado = capturaMagnetometroRepository.procesarCaptura(
                experimentoId = experimentoId,
                bx = promedios.first,
                by = promedios.second,
                bz = promedios.third
            )
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveMessage = if (resultado != null) {
                    "Resultado guardado: ${resultado.valorCalculado} ${resultado.unidad}"
                } else {
                    "Error al calcular o guardar el resultado"
                }
            )
        }
    }

    fun onResume() {
        sensorManager.onResume()
    }

    fun onPause() {
        sensorManager.onPause()
    }

    override fun onCleared() {
        sensorManager.stopReading(sensorType)
        super.onCleared()
    }

    private fun promediarComponentes(readings: List<SensorReading>): Triple<Double, Double, Double>? {
        val valid = readings.mapNotNull { leerComponentes(it) }
        if (valid.isEmpty()) return null
        val bx = valid.map { it.first }.average()
        val by = valid.map { it.second }.average()
        val bz = valid.map { it.third }.average()
        return Triple(bx, by, bz)
    }

    private fun leerComponentes(reading: SensorReading?): Triple<Double, Double, Double>? {
        val bx = reading?.valueX
        val by = reading?.valueY
        val bz = reading?.valueZ
        if (bx == null || by == null || bz == null) return null
        return Triple(bx, by, bz)
    }
}
