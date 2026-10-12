package com.laboratoriofisico.app.sensores

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.model.SamplingRate
import com.laboratoriofisico.app.sensores.model.SensorInfo
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.permissions.SensorPermissions
import com.laboratoriofisico.app.sensores.util.LumaFrame
import com.laboratoriofisico.app.sensores.provider.AudioSensorProvider
import com.laboratoriofisico.app.sensores.provider.CameraSensorProvider
import com.laboratoriofisico.app.sensores.provider.LocationSensorProvider
import com.laboratoriofisico.app.sensores.provider.MotionSensorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class LaboratorioSensorManager(
    context: Context,
    private val scope: CoroutineScope
) : SensorManager {

    private val appContext = context.applicationContext
    private val motionProvider = MotionSensorProvider(appContext)
    private val locationProvider = LocationSensorProvider(appContext)
    private val audioProvider = AudioSensorProvider()
    private val cameraProvider = CameraSensorProvider(appContext)

    private val _samplingRate = MutableStateFlow(SamplingRate.NORMAL)
    override val samplingRate: StateFlow<SamplingRate> = _samplingRate.asStateFlow()

    private val _cameraHardwareState = MutableStateFlow(CameraHardwareState.CLOSED)
    override fun observeCameraHardwareState(): StateFlow<CameraHardwareState> =
        _cameraHardwareState.asStateFlow()

    private val permissionGranted = SensorType.entries.associateWith { type ->
        if (type == SensorType.CAMERA) {
            cameraProvider.checkCameraPermission()
        } else {
            !SensorPermissions.requiresRuntimePermission(type)
        }
    }.toMutableMap()

    private val states = SensorType.entries.associateWith {
        MutableStateFlow(initialState(it))
    }
    private val latestReadings = SensorType.entries.associateWith {
        MutableStateFlow<SensorReading?>(null)
    }
    private val readingFlows = SensorType.entries.associateWith {
        MutableSharedFlow<SensorReading>(replay = 1, extraBufferCapacity = 64)
    }

    private val activeSensors = mutableSetOf<SensorType>()
    private var pausedSensorSnapshot: Set<SensorType> = emptySet()
    private var paused = false
    private var cameraPrepared = false

    init {
        cameraProvider.setPermissionGranted(permissionGranted[SensorType.CAMERA] == true)
        scope.launch {
            cameraProvider.prepare { ok -> cameraPrepared = ok }
        }
        refreshHardwareAvailability()
    }

    override fun getAvailableSensors(): List<SensorInfo> =
        SensorType.entries.map { type ->
            SensorInfo(
                type = type,
                available = isSensorAvailable(type),
                state = states[type]?.value ?: SensorState.UNAVAILABLE,
                unit = type.unit
            )
        }

    override fun isSensorAvailable(type: SensorType): Boolean = when (type) {
        SensorType.ACCELEROMETER,
        SensorType.GYROSCOPE,
        SensorType.MAGNETOMETER,
        SensorType.LIGHT -> motionProvider.isAvailable(type)
        SensorType.LOCATION -> locationProvider.isAvailable()
        SensorType.MICROPHONE -> audioProvider.isAvailable()
        SensorType.CAMERA -> cameraProvider.isAvailable()
    }

    override fun isLightSensorExperimental(): Boolean = motionProvider.isLightExperimental()

    override fun refreshHardwareAvailability() {
        SensorType.entries.forEach { type ->
            val available = isSensorAvailable(type)
            val current = states[type]?.value ?: return@forEach
            when {
                available && current == SensorState.UNAVAILABLE -> updateState(
                    type,
                    if (SensorPermissions.requiresRuntimePermission(type) && permissionGranted[type] != true) {
                        SensorState.PERMISSION_DENIED
                    } else {
                        SensorState.IDLE
                    }
                )
                !available && current != SensorState.UNAVAILABLE && !activeSensors.contains(type) ->
                    updateState(type, SensorState.UNAVAILABLE)
            }
        }
    }

    override fun observeState(type: SensorType): StateFlow<SensorState> =
        states.getValue(type).asStateFlow()

    override fun observeLatestReading(type: SensorType): StateFlow<SensorReading?> =
        latestReadings.getValue(type).asStateFlow()

    override fun observeReadings(type: SensorType): Flow<SensorReading> =
        readingFlows.getValue(type).asSharedFlow()

    override fun setSamplingRate(rate: SamplingRate) {
        _samplingRate.value = rate
        val running = activeSensors.filter { it != SensorType.CAMERA }.toList()
        running.forEach { stopReading(it) }
        running.forEach { startReading(it) }
    }

    override fun setPermissionGranted(type: SensorType, granted: Boolean) {
        permissionGranted[type] = granted
        if (type == SensorType.CAMERA) {
            cameraProvider.setPermissionGranted(granted)
        }
        if (!granted) {
            updateState(type, SensorState.PERMISSION_DENIED)
            if (type == SensorType.CAMERA) {
                unbindCamera()
            } else {
                stopReading(type)
            }
        } else if (activeSensors.contains(type)) {
            if (type != SensorType.CAMERA) startReading(type)
        } else {
            updateState(type, if (isSensorAvailable(type)) SensorState.IDLE else SensorState.UNAVAILABLE)
        }
    }

    override fun startReading(type: SensorType) = startReading(type, forceRestart = false)

    override fun startReading(type: SensorType, forceRestart: Boolean) {
        // #region agent log
        if (type == SensorType.LIGHT) {
            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                hypothesisId = "H-LUZ-A",
                location = "LaboratorioSensorManager.startReading",
                message = "startReading LIGHT",
                data = mapOf(
                    "paused" to paused,
                    "alreadyActive" to activeSensors.contains(type),
                    "forceRestart" to forceRestart,
                    "available" to isSensorAvailable(type)
                )
            )
        }
        // #endregion
        if (paused) return
        if (type == SensorType.CAMERA) {
            prepareCameraIfNeeded()
            return
        }
        if (activeSensors.contains(type)) {
            if (!forceRestart) return
            stopReading(type)
        }
        if (!isSensorAvailable(type)) {
            updateState(type, SensorState.UNAVAILABLE)
            return
        }
        if (SensorPermissions.requiresRuntimePermission(type) && permissionGranted[type] != true) {
            updateState(type, SensorState.PERMISSION_DENIED)
            return
        }

        val onReading: (SensorReading) -> Unit = { reading ->
            latestReadings.getValue(type).value = reading
            readingFlows.getValue(type).tryEmit(reading)
        }

        try {
            val started = when (type) {
                SensorType.ACCELEROMETER,
                SensorType.GYROSCOPE,
                SensorType.MAGNETOMETER,
                SensorType.LIGHT -> motionProvider.start(type, _samplingRate.value, onReading)
                SensorType.LOCATION -> {
                    if (!locationProvider.isGpsEnabled()) {
                        updateState(type, SensorState.DISABLED)
                        return
                    }
                    locationProvider.start(_samplingRate.value, onReading)
                    true
                }
                SensorType.MICROPHONE -> audioProvider.start(scope, _samplingRate.value, onReading)
                SensorType.CAMERA -> true
            }
            if (!started) {
                // #region agent log
                android.util.Log.e(
                    if (type == SensorType.ACCELEROMETER) "PenduloDiag" else "LuzDiag",
                    "start($type) devolvió false → estado=${if (type == SensorType.LIGHT) "UNAVAILABLE" else "ERROR"}"
                )
                com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                    hypothesisId = if (type == SensorType.ACCELEROMETER) "H-PEND-ERR" else "H-LUZ-REG",
                    location = "LaboratorioSensorManager.startReading",
                    message = "start devolvió false",
                    data = mapOf(
                        "type" to type.name,
                        "reason" to "provider.start devolvió false",
                        "samplingRate" to _samplingRate.value.name,
                        "paused" to paused,
                        "mappedState" to if (type == SensorType.LIGHT) "UNAVAILABLE" else "ERROR"
                    ),
                    runId = "luz-isolation"
                )
                // #endregion
                // LIGHT sin ALS usable (p. ej. solo Camera Light OEM) ≠ fallo de app: UNAVAILABLE.
                // ERROR se reserva para fallos reales (p. ej. acelerómetro del péndulo).
                updateState(
                    type,
                    if (type == SensorType.LIGHT) SensorState.UNAVAILABLE else SensorState.ERROR
                )
                return
            }
            activeSensors.add(type)
            updateState(type, SensorState.ACTIVE)
        } catch (exception: Exception) {
            // #region agent log
            android.util.Log.e(
                if (type == SensorType.ACCELEROMETER) "PenduloDiag" else "LuzDiag",
                "Asignando SensorState.ERROR por excepción en start($type): ${exception.message}",
                exception
            )
            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                hypothesisId = if (type == SensorType.ACCELEROMETER) "H-PEND-ERR" else "H-LUZ-EX",
                location = "LaboratorioSensorManager.startReading.catch",
                message = "Asignando SensorState.ERROR por excepción",
                data = mapOf(
                    "type" to type.name,
                    "exception" to exception.javaClass.name,
                    "message" to exception.message
                ),
                runId = "post-fix"
            )
            // #endregion
            updateState(type, SensorState.ERROR)
        }
    }

    override fun stopReading(type: SensorType) {
        when (type) {
            SensorType.ACCELEROMETER,
            SensorType.GYROSCOPE,
            SensorType.MAGNETOMETER,
            SensorType.LIGHT -> motionProvider.stop(type)
            SensorType.LOCATION -> locationProvider.stop()
            SensorType.MICROPHONE -> audioProvider.stop()
            SensorType.CAMERA -> cameraProvider.stop()
        }
        activeSensors.remove(type)
        updateState(
            type,
            when {
                !isSensorAvailable(type) -> SensorState.UNAVAILABLE
                SensorPermissions.requiresRuntimePermission(type) && permissionGranted[type] != true ->
                    SensorState.PERMISSION_DENIED
                else -> SensorState.IDLE
            }
        )
    }

    override fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        lensFacing: CameraLensFacing
    ) {
        // #region agent log
        com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
            hypothesisId = "H-LCAM-BIND",
            location = "LaboratorioSensorManager.bindCamera",
            message = "bindCamera solicitado",
            data = mapOf(
                "paused" to paused,
                "available" to isSensorAvailable(SensorType.CAMERA),
                "permissionGranted" to (permissionGranted[SensorType.CAMERA] == true),
                "lens" to lensFacing.name,
                "hasPreview" to (previewView != null)
            ),
            runId = "relative-light"
        )
        // #endregion
        if (paused) {
            // Medición iniciada en primer plano: no bloquear el bind por un onPause previo (p. ej. diálogo de permiso).
            paused = false
            // #region agent log
            android.util.Log.w(
                "LuzCamaraDiag",
                "bindCamera: paused=true al solicitar bind; reanudando para medición activa"
            )
            // #endregion
        }
        prepareCameraIfNeeded()
        if (!isSensorAvailable(SensorType.CAMERA)) {
            // #region agent log
            android.util.Log.e("LuzCamaraDiag", "bindCamera abortado: cámara no disponible")
            // #endregion
            updateState(SensorType.CAMERA, SensorState.UNAVAILABLE)
            _cameraHardwareState.value = CameraHardwareState.UNAVAILABLE
            return
        }
        if (permissionGranted[SensorType.CAMERA] != true) {
            // #region agent log
            android.util.Log.e("LuzCamaraDiag", "bindCamera abortado: permiso CAMERA denegado")
            // #endregion
            updateState(SensorType.CAMERA, SensorState.PERMISSION_DENIED)
            _cameraHardwareState.value = CameraHardwareState.PERMISSION_REQUIRED
            return
        }

        val onReading: (SensorReading) -> Unit = { reading ->
            latestReadings.getValue(SensorType.CAMERA).value = reading
            readingFlows.getValue(SensorType.CAMERA).tryEmit(reading)
        }

        cameraProvider.bind(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = lensFacing,
            onReading = onReading,
            onStateChange = { hwState ->
                _cameraHardwareState.value = hwState
                updateState(SensorType.CAMERA, mapCameraHardwareState(hwState))
            }
        )
        activeSensors.add(SensorType.CAMERA)
    }

    override fun switchCameraLens(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        lensFacing: CameraLensFacing
    ) {
        cameraProvider.switchLens(lifecycleOwner, previewView, lensFacing)
    }

    override fun captureCameraPhoto(outputFile: File, onResult: (Result<String>) -> Unit) {
        cameraProvider.capturePhoto(outputFile, onResult)
    }

    override fun setCameraFrameAnalyzer(analyzer: ((LumaFrame) -> Unit)?) {
        cameraProvider.setFrameAnalyzer(analyzer)
    }

    override fun setCameraAutoExposureLock(locked: Boolean) {
        cameraProvider.setAutoExposureLock(locked)
    }

    override fun unbindCamera() {
        cameraProvider.stop()
        activeSensors.remove(SensorType.CAMERA)
        _cameraHardwareState.value = CameraHardwareState.CLOSED
        updateState(
            SensorType.CAMERA,
            when {
                !isSensorAvailable(SensorType.CAMERA) -> SensorState.UNAVAILABLE
                permissionGranted[SensorType.CAMERA] != true -> SensorState.PERMISSION_DENIED
                else -> SensorState.IDLE
            }
        )
    }

    override fun hasCameraLens(facing: CameraLensFacing): Boolean =
        cameraProvider.hasLens(facing)

    override fun stopAll() {
        activeSensors.toList().forEach { type ->
            if (type == SensorType.CAMERA) unbindCamera() else stopReading(type)
        }
        motionProvider.stopAll()
        locationProvider.stop()
        audioProvider.stop()
        activeSensors.clear()
    }

    override fun onPause() {
        // Conservar qué sensores estaban activos para poder reanudarlos en onResume.
        pausedSensorSnapshot = activeSensors.filter { it != SensorType.CAMERA }.toSet()
        paused = true
        stopAll()
    }

    override fun onResume() {
        paused = false
        syncRuntimePermissionsFromSystem()
        refreshHardwareAvailability()
        val toRestart = pausedSensorSnapshot.ifEmpty {
            activeSensors.filter { it != SensorType.CAMERA }.toSet()
        }
        pausedSensorSnapshot = emptySet()
        activeSensors.removeAll(toRestart)
        toRestart.forEach { startReading(it) }
    }

    override fun release() {
        stopAll()
        cameraProvider.release()
        cameraPrepared = false
    }

    private fun syncRuntimePermissionsFromSystem() {
        SensorType.entries.forEach { type ->
            if (!SensorPermissions.requiresRuntimePermission(type)) return@forEach
            val granted = SensorPermissions.requiredPermissions(type).all { permission ->
                ContextCompat.checkSelfPermission(appContext, permission) ==
                    PackageManager.PERMISSION_GRANTED
            }
            if (granted != (permissionGranted[type] == true)) {
                setPermissionGranted(type, granted)
            }
        }
    }

    private fun prepareCameraIfNeeded() {
        if (cameraPrepared) return
        scope.launch {
            cameraProvider.prepare { ok ->
                cameraPrepared = ok
                if (!ok) {
                    updateState(SensorType.CAMERA, SensorState.UNAVAILABLE)
                    _cameraHardwareState.value = CameraHardwareState.UNAVAILABLE
                }
            }
        }
    }

    private fun mapCameraHardwareState(state: CameraHardwareState): SensorState = when (state) {
        CameraHardwareState.RUNNING -> SensorState.ACTIVE
        CameraHardwareState.READY,
        CameraHardwareState.INITIALIZING -> SensorState.IDLE
        CameraHardwareState.PERMISSION_REQUIRED -> SensorState.PERMISSION_DENIED
        CameraHardwareState.UNAVAILABLE,
        CameraHardwareState.NO_CAMERA -> SensorState.UNAVAILABLE
        CameraHardwareState.ERROR -> SensorState.ERROR
        // BUSY es transitorio (re-bind); no marcar ERROR permanente en la UI.
        CameraHardwareState.BUSY -> SensorState.IDLE
        CameraHardwareState.CLOSED -> SensorState.IDLE
    }

    private fun updateState(type: SensorType, state: SensorState) {
        states.getValue(type).value = state
    }

    private fun initialState(type: SensorType): SensorState = when {
        !isSensorAvailable(type) -> SensorState.UNAVAILABLE
        SensorPermissions.requiresRuntimePermission(type) &&
            permissionGranted[type] != true -> SensorState.PERMISSION_DENIED
        else -> SensorState.IDLE
    }
}
