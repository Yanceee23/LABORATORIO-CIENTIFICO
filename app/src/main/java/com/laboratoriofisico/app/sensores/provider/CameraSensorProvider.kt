package com.laboratoriofisico.app.sensores.provider

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.util.LumaFrame
import java.io.File

class CameraSensorProvider(context: Context) {
    private val appContext = context.applicationContext
    private val session = CameraXSession(appContext)
    private var prepared = false
    private var permissionGranted = false

    fun hasCameraHardware(): Boolean = session.hasCameraHardware()

    fun isAvailable(): Boolean = hasCameraHardware()

    fun setPermissionGranted(granted: Boolean) {
        permissionGranted = granted
        if (!granted) {
            session.setPermissionRequired()
        }
    }

    fun prepare(onReady: (Boolean) -> Unit) {
        if (!hasCameraHardware()) {
            onReady(false)
            return
        }
        session.prepare { ok ->
            prepared = ok
            onReady(ok)
        }
    }

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        lensFacing: CameraLensFacing,
        onReading: (SensorReading) -> Unit,
        onStateChange: (CameraHardwareState) -> Unit
    ) {
        if (!permissionGranted) {
            session.setPermissionRequired()
            onStateChange(CameraHardwareState.PERMISSION_REQUIRED)
            return
        }
        if (!prepared) {
            prepare { ok ->
                if (ok) {
                    session.bind(lifecycleOwner, previewView, lensFacing, onReading, onStateChange)
                } else {
                    onStateChange(CameraHardwareState.UNAVAILABLE)
                }
            }
            return
        }
        session.bind(lifecycleOwner, previewView, lensFacing, onReading, onStateChange)
    }

    fun switchLens(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        lensFacing: CameraLensFacing
    ) {
        session.switchLens(lifecycleOwner, previewView, lensFacing)
    }

    fun capturePhoto(outputFile: File, onResult: (Result<String>) -> Unit) {
        session.capturePhoto(outputFile, onResult)
    }

    fun setFrameAnalyzer(analyzer: ((LumaFrame) -> Unit)?) {
        session.setFrameAnalyzer(analyzer)
    }

    fun setAutoExposureLock(locked: Boolean) {
        session.setAutoExposureLock(locked)
    }

    fun stop() {
        session.unbind()
    }

    fun release() {
        session.release()
        prepared = false
    }

    fun currentState(): CameraHardwareState = session.currentState()

    fun hasLens(facing: CameraLensFacing): Boolean {
        val selectorFacing = when (facing) {
            CameraLensFacing.BACK -> androidx.camera.core.CameraSelector.LENS_FACING_BACK
            CameraLensFacing.FRONT -> androidx.camera.core.CameraSelector.LENS_FACING_FRONT
        }
        return session.isLensAvailable(selectorFacing)
    }

    fun checkCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
