package com.laboratoriofisico.app.sensores.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * Mide la luz con Camera2 para obtener, en cada frame, el brillo y la exposición real.
 * CameraX no adjunta esos datos al análisis y, sin ellos, el número no sigue la luz.
 */
internal class Camera2LightMeter(private val context: Context) {
    data class Sample(
        val luma: Double,
        val width: Int,
        val height: Int,
        val exposureTimeNs: Long,
        val iso: Int,
        val aperture: Double?
    )

    private val generation = AtomicInteger(0)
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var latestResult: TotalCaptureResult? = null

    fun start(
        lensFacing: Int,
        onSample: (Sample) -> Unit,
        onError: (String) -> Unit
    ) {
        releasePrevious()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onError("Permiso de cámara requerido")
            return
        }
        val current = generation.get()
        val worker = HandlerThread("luz-camera2").also { it.start() }
        val workerHandler = Handler(worker.looper)
        thread = worker
        handler = workerHandler
        workerHandler.post {
            try {
                open(current, lensFacing, workerHandler, onSample, onError)
            } catch (error: Exception) {
                android.util.Log.e("LuzCamaraDiag", "Camera2 no abrió: ${error.message}", error)
                if (current == generation.get()) onError("No se pudo abrir la cámara")
            }
        }
    }

    fun stop() {
        releasePrevious()
    }

    private fun releasePrevious() {
        generation.incrementAndGet()
        val worker = thread
        val workerHandler = handler
        val openDevice = device
        val openSession = session
        val openReader = reader
        thread = null
        handler = null
        device = null
        session = null
        reader = null
        latestResult = null
        if (workerHandler != null) {
            workerHandler.post {
                closeQuietly(openSession, openDevice, openReader)
                worker?.quitSafely()
            }
        } else {
            closeQuietly(openSession, openDevice, openReader)
            worker?.quitSafely()
        }
    }

    private fun open(
        current: Int,
        lensFacing: Int,
        workerHandler: Handler,
        onSample: (Sample) -> Unit,
        onError: (String) -> Unit
    ) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            val facing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            facing == lensFacing
        } ?: run {
            if (current == generation.get()) onError("Cámara no disponible")
            return
        }
        val fallbackAperture = manager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
            ?.firstOrNull()
            ?.toDouble()

        val imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2)
        reader = imageReader
        imageReader.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (current != generation.get()) return@setOnImageAvailableListener
                val result = latestResult
                val exposureNs = result?.get(CaptureResult.SENSOR_EXPOSURE_TIME)
                val iso = result?.get(CaptureResult.SENSOR_SENSITIVITY)
                if (exposureNs == null || iso == null) return@setOnImageAvailableListener
                val luma = averageLuma(image) ?: return@setOnImageAvailableListener
                val aperture = result.get(CaptureResult.LENS_APERTURE)?.toDouble() ?: fallbackAperture
                onSample(
                    Sample(
                        luma = luma,
                        width = image.width,
                        height = image.height,
                        exposureTimeNs = exposureNs,
                        iso = iso,
                        aperture = aperture
                    )
                )
            } catch (error: Exception) {
                android.util.Log.e("LuzCamaraDiag", "Frame de luz falló: ${error.message}", error)
            } finally {
                image.close()
            }
        }, workerHandler)

        @Suppress("MissingPermission")
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                if (current != generation.get()) {
                    camera.close()
                    return
                }
                device = camera
                try {
                    camera.createCaptureSession(
                        listOf(imageReader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(captureSession: CameraCaptureSession) {
                                if (current != generation.get()) {
                                    captureSession.close()
                                    return
                                }
                                session = captureSession
                                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                    addTarget(imageReader.surface)
                                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                    set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                                }.build()
                                captureSession.setRepeatingRequest(
                                    request,
                                    object : CameraCaptureSession.CaptureCallback() {
                                        override fun onCaptureCompleted(
                                            session: CameraCaptureSession,
                                            request: CaptureRequest,
                                            result: TotalCaptureResult
                                        ) {
                                            if (current == generation.get()) latestResult = result
                                        }
                                    },
                                    workerHandler
                                )
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                if (current == generation.get()) onError("No se pudo medir la luz")
                            }
                        },
                        workerHandler
                    )
                } catch (error: Exception) {
                    android.util.Log.e("LuzCamaraDiag", "Sesión de luz falló: ${error.message}", error)
                    if (current == generation.get()) onError("No se pudo medir la luz")
                }
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                if (device === camera) device = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                android.util.Log.e("LuzCamaraDiag", "Camera2 error=$error")
                camera.close()
                if (device === camera) device = null
                if (current == generation.get()) onError("No se pudo abrir la cámara")
            }
        }, workerHandler)
    }

    private fun closeQuietly(
        openSession: CameraCaptureSession?,
        openDevice: CameraDevice?,
        openReader: ImageReader?
    ) {
        try {
            openSession?.close()
        } catch (_: Exception) {
        }
        try {
            openDevice?.close()
        } catch (_: Exception) {
        }
        try {
            openReader?.close()
        } catch (_: Exception) {
        }
    }

    private fun averageLuma(image: android.media.Image): Double? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = max(1, plane.pixelStride)
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null
        val step = max(1, width / 80)
        var sum = 0L
        var count = 0
        var y = 0
        while (y < height) {
            val row = y * rowStride
            var x = 0
            while (x < width) {
                val index = row + x * pixelStride
                if (index < buffer.limit()) {
                    sum += buffer.get(index).toInt() and 0xFF
                    count++
                }
                x += step
            }
            y += step
        }
        if (count == 0) return null
        return sum.toDouble() / count
    }
}
