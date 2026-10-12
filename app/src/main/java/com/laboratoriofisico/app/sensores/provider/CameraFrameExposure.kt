package com.laboratoriofisico.app.sensores.provider

import android.hardware.camera2.CaptureResult
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.CameraCaptureResults

/** Tiempo, ISO y diafragma del frame que acaba de llegar. */
internal object CameraFrameExposure {
    data class Sample(
        val exposureTimeNs: Long,
        val iso: Int,
        val aperture: Double?
    )

    @Suppress("RestrictedApi")
    fun read(image: ImageProxy): Sample? {
        val result = CameraCaptureResults.retrieveCameraCaptureResult(image.imageInfo)
            ?.captureResult
            ?: return null
        val exposureTimeNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return null
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return null
        val aperture = result.get(CaptureResult.LENS_APERTURE)?.toDouble()
        if (exposureTimeNs <= 0L || iso <= 0) return null
        return Sample(exposureTimeNs, iso, aperture)
    }
}
