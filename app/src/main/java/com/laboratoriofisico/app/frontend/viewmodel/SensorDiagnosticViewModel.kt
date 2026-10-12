package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.repository.SensorReadingRepository
import com.laboratoriofisico.app.sensores.SensorManager
import com.laboratoriofisico.app.sensores.model.SamplingRate
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.permissions.SensorPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SensorDiagnosticItem(
    val type: SensorType,
    val available: Boolean,
    val state: SensorState,
    val unit: String,
    val latestReading: SensorReading?,
    val storedCount: Int,
    val requiresPermission: Boolean
)

data class SensorDiagnosticUiState(
    val sensors: List<SensorDiagnosticItem> = emptyList(),
    val samplingRate: SamplingRate = SamplingRate.NORMAL
)

class SensorDiagnosticViewModel(
    private val sensorManager: SensorManager,
    private val sensorReadingRepository: SensorReadingRepository
) : ViewModel() {

    private val sensorItems = SensorType.entries.associateWith {
        MutableStateFlow(createInitialItem(it))
    }.toMutableMap()

    private val _uiState = MutableStateFlow(SensorDiagnosticUiState())
    val uiState: StateFlow<SensorDiagnosticUiState> = _uiState.asStateFlow()

    init {
        SensorType.entries.forEach { type ->
            viewModelScope.launch {
                sensorManager.observeReadings(type).collect { reading ->
                    sensorReadingRepository.guardar(reading)
                }
            }
            viewModelScope.launch {
                combine(
                    sensorManager.observeState(type),
                    sensorManager.observeLatestReading(type),
                    sensorReadingRepository.observeCountBySensor(type)
                ) { state, reading, count ->
                    createItem(type, state, reading, count)
                }.collect { item ->
                    sensorItems[type]?.value = item
                    publishUiState()
                }
            }
        }

        viewModelScope.launch {
            sensorManager.samplingRate.collect {
                _uiState.value = _uiState.value.copy(samplingRate = it)
            }
        }

        publishUiState()
    }

    fun setSamplingRate(rate: SamplingRate) {
        sensorManager.setSamplingRate(rate)
    }

    fun startSensor(type: SensorType) {
        sensorManager.startReading(type)
    }

    fun stopSensor(type: SensorType) {
        sensorManager.stopReading(type)
    }

    fun onPermissionResult(type: SensorType, granted: Boolean) {
        sensorManager.setPermissionGranted(type, granted)
    }

    fun onResume() {
        sensorManager.onResume()
    }

    fun onPause() {
        sensorManager.onPause()
    }

    fun onClearStored(type: SensorType) {
        viewModelScope.launch {
            sensorReadingRepository.eliminarPorSensor(type)
        }
    }

    override fun onCleared() {
        sensorManager.stopAll()
        super.onCleared()
    }

    private fun publishUiState() {
        _uiState.value = _uiState.value.copy(
            sensors = SensorType.entries.map { sensorItems[it]?.value ?: createInitialItem(it) }
        )
    }

    private fun createInitialItem(type: SensorType): SensorDiagnosticItem =
        createItem(type, sensorManager.observeState(type).value, null, 0)

    private fun createItem(
        type: SensorType,
        state: SensorState,
        reading: SensorReading?,
        count: Int
    ): SensorDiagnosticItem = SensorDiagnosticItem(
        type = type,
        available = sensorManager.isSensorAvailable(type),
        state = state,
        unit = type.unit,
        latestReading = reading,
        storedCount = count,
        requiresPermission = SensorPermissions.requiresRuntimePermission(type)
    )
}
