package com.laboratoriofisico.app.frontend.viewmodel

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.sensores.SensorManager
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CameraExperimentUiState(
    val disponible: Boolean = false,
    val permisoRequerido: Boolean = false,
    val hardwareState: CameraHardwareState = CameraHardwareState.CLOSED,
    val lensFacing: CameraLensFacing = CameraLensFacing.BACK,
    val canSwitchToFront: Boolean = false,
    val canSwitchToBack: Boolean = false,
    val resolucion: String = "N/A",
    val brillo: String = "N/A",
    val fps: String = "N/A",
    val formato: String = "N/A",
    val ultimaCaptura: String = "N/A",
    val mensaje: String? = null,
    val activa: Boolean = false,
    val capturando: Boolean = false
)

class CameraExperimentViewModel(
    private val sensorManager: SensorManager,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraExperimentUiState())
    val uiState: StateFlow<CameraExperimentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sensorManager.observeState(SensorType.CAMERA),
                sensorManager.observeLatestReading(SensorType.CAMERA),
                sensorManager.observeCameraHardwareState()
            ) { state, reading, hwState ->
                Triple(state, reading, hwState)
            }.collect { (state, reading, hwState) ->
                updateFromReading(state, reading, hwState)
            }
        }
        refreshAvailability()
    }

    fun refreshAvailability() {
        _uiState.update {
            it.copy(
                disponible = sensorManager.isSensorAvailable(SensorType.CAMERA),
                canSwitchToBack = sensorManager.hasCameraLens(CameraLensFacing.BACK),
                canSwitchToFront = sensorManager.hasCameraLens(CameraLensFacing.FRONT),
                permisoRequerido = sensorManager.observeState(SensorType.CAMERA).value ==
                    SensorState.PERMISSION_DENIED
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        sensorManager.setPermissionGranted(SensorType.CAMERA, granted)
        refreshAvailability()
    }

    fun iniciar(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        sensorManager.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = _uiState.value.lensFacing
        )
        _uiState.update { it.copy(activa = true, mensaje = null) }
    }

    fun detener() {
        sensorManager.unbindCamera()
        _uiState.update { it.copy(activa = false) }
    }

    fun cambiarCamara(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val current = _uiState.value.lensFacing
        val newFacing = if (current == CameraLensFacing.BACK) {
            CameraLensFacing.FRONT
        } else {
            CameraLensFacing.BACK
        }
        if (!sensorManager.hasCameraLens(newFacing)) return
        _uiState.update { it.copy(lensFacing = newFacing) }
        sensorManager.switchCameraLens(lifecycleOwner, previewView, newFacing)
    }

    fun capturar() {
        if (_uiState.value.capturando) return
        _uiState.update { it.copy(capturando = true, mensaje = null) }
        val outputDir = appContext.getExternalFilesDir("capturas") ?: appContext.cacheDir
        outputDir.mkdirs()
        val file = File(outputDir, "captura_${System.currentTimeMillis()}.jpg")
        sensorManager.captureCameraPhoto(file) { result ->
            _uiState.update { state ->
                state.copy(
                    capturando = false,
                    ultimaCaptura = result.getOrNull() ?: state.ultimaCaptura,
                    mensaje = result.fold(
                        onSuccess = { "Fotografía guardada" },
                        onFailure = { it.message ?: "Error al capturar" }
                    )
                )
            }
        }
    }

    fun onResume(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        refreshAvailability()
        if (wasActiveBeforePause) {
            iniciar(lifecycleOwner, previewView)
        }
        wasActiveBeforePause = false
    }

    fun onPause() {
        wasActiveBeforePause = _uiState.value.activa
        detener()
        // No llamar sensorManager.onPause(): afecta sensores globales (p. ej. péndulo).
    }

    override fun onCleared() {
        detener()
        super.onCleared()
    }

    private var wasActiveBeforePause = false

    private fun updateFromReading(
        state: SensorState,
        reading: SensorReading?,
        hwState: CameraHardwareState
    ) {
        val w = reading?.valueX?.toInt()
        val h = reading?.valueY?.toInt()
        val resolucion = if (w != null && h != null) "${w}×${h}" else "N/A"
        _uiState.update {
            it.copy(
                hardwareState = hwState,
                permisoRequerido = state == SensorState.PERMISSION_DENIED,
                brillo = reading?.valueScalar?.let { b -> "%.0f (luma)".format(b) } ?: "N/A",
                fps = reading?.valueZ?.let { f -> "%.1f".format(f) } ?: "N/A",
                formato = reading?.metadata
                    ?.substringAfter("format=", "")
                    ?.substringBefore(";")
                    ?.takeIf { s -> s.isNotBlank() } ?: "N/A",
                resolucion = resolucion,
                ultimaCaptura = reading?.metadata
                    ?.substringAfter("capture=", "")
                    ?.substringBefore(";")
                    ?.takeIf { s -> s.isNotBlank() } ?: it.ultimaCaptura
            )
        }
    }
}
