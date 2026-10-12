package com.laboratoriofisico.app.sensores

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.model.SamplingRate
import com.laboratoriofisico.app.sensores.model.SensorInfo
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.util.LumaFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface SensorManager {
    val samplingRate: StateFlow<SamplingRate>

    fun getAvailableSensors(): List<SensorInfo>
    fun isSensorAvailable(type: SensorType): Boolean
    fun isLightSensorExperimental(): Boolean
    fun refreshHardwareAvailability()
    fun observeState(type: SensorType): StateFlow<SensorState>
    fun observeLatestReading(type: SensorType): StateFlow<SensorReading?>
    fun observeReadings(type: SensorType): Flow<SensorReading>

    fun setSamplingRate(rate: SamplingRate)
    fun setPermissionGranted(type: SensorType, granted: Boolean)

    fun startReading(type: SensorType)
    /** Reinicia el listener aunque el sensor ya estuviera activo (útil para sensores on-change). */
    fun startReading(type: SensorType, forceRestart: Boolean)
    fun stopReading(type: SensorType)
    fun stopAll()
    fun onResume()
    fun onPause()
    fun release()

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        lensFacing: CameraLensFacing = CameraLensFacing.BACK
    )

    fun switchCameraLens(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        lensFacing: CameraLensFacing
    )

    fun captureCameraPhoto(outputFile: File, onResult: (Result<String>) -> Unit)

    /**
     * Registra un consumidor de los frames reales de ImageAnalysis.
     * Pasar `null` lo desregistra; debe hacerse al salir de la pantalla que lo usa.
     */
    fun setCameraFrameAnalyzer(analyzer: ((LumaFrame) -> Unit)?)

    /** Congela/libera la autoexposición de CameraX para mediciones relativas de iluminación. */
    fun setCameraAutoExposureLock(locked: Boolean)

    fun unbindCamera()

    fun observeCameraHardwareState(): StateFlow<CameraHardwareState>

    fun hasCameraLens(facing: CameraLensFacing): Boolean
}
