package com.laboratoriofisico.app.sensores.provider

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureRequest
import android.os.SystemClock
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.model.CameraReading
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.util.LumaFrame
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sesión CameraX con preview real, captura e ImageAnalysis.
 * Los frames provienen del hardware; no hay heartbeats sintéticos.
 */
class CameraXSession(private val context: Context) {
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context.applicationContext)
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var analysisExecutor: Executor? = null
    private var boundCamera: Camera? = null
    private var headlessTexture: SurfaceTexture? = null
    private var headlessSurface: Surface? = null

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var hardwareState = CameraHardwareState.CLOSED
    private var onReading: ((SensorReading) -> Unit)? = null
    private var onStateChange: ((CameraHardwareState) -> Unit)? = null

    private val isBinding = AtomicBoolean(false)
    private val camera2LightMeter = Camera2LightMeter(context)
    private var lastFrameTimestampMs = 0L
    private var fpsEstimate: Double? = null
    private var lastLightCalibrationLogMs = 0L

    /** Si llega un bind mientras otro corre (p. ej. Objetos→Reflexión), no se descarta: se aplica al terminar. */
    @Volatile
    private var pendingBind: PendingBind? = null

    private data class PendingBind(
        val lifecycleOwner: LifecycleOwner,
        val previewView: PreviewView?,
        val facing: CameraLensFacing,
        val onReading: (SensorReading) -> Unit,
        val onStateChange: (CameraHardwareState) -> Unit
    )

    @Volatile
    private var frameAnalyzer: ((LumaFrame) -> Unit)? = null

    /** Consumidor opcional de los frames reales de ImageAnalysis (seguimiento de objetos). */
    fun setFrameAnalyzer(analyzer: ((LumaFrame) -> Unit)?) {
        frameAnalyzer = analyzer
    }

    fun hasCameraHardware(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    fun isLensAvailable(facing: Int): Boolean {
        val provider = cameraProvider ?: return false
        val selector = CameraSelector.Builder().requireLensFacing(facing).build()
        return provider.hasCamera(selector)
    }

    fun prepare(onReady: (Boolean) -> Unit) {
        if (!hasCameraHardware()) {
            updateState(CameraHardwareState.NO_CAMERA)
            onReady(false)
            return
        }
        updateState(CameraHardwareState.INITIALIZING)
        ProcessCameraProvider.getInstance(context.applicationContext).also { future ->
            future.addListener({
                try {
                    cameraProvider = future.get()
                    val hasAny = isLensAvailable(CameraSelector.LENS_FACING_BACK) ||
                        isLensAvailable(CameraSelector.LENS_FACING_FRONT)
                    if (!hasAny) {
                        updateState(CameraHardwareState.NO_CAMERA)
                        onReady(false)
                    } else {
                        updateState(CameraHardwareState.READY)
                        onReady(true)
                    }
                } catch (_: Exception) {
                    updateState(CameraHardwareState.ERROR)
                    onReady(false)
                }
            }, mainExecutor)
        }
    }

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        facing: CameraLensFacing,
        onReading: (SensorReading) -> Unit,
        onStateChange: (CameraHardwareState) -> Unit
    ) {
        if (isBinding.getAndSet(true)) {
            // No descartar: Reflexión de Luz (sin preview) debe poder tomar el control tras Objetos/Péndulo.
            pendingBind = PendingBind(lifecycleOwner, previewView, facing, onReading, onStateChange)
            // #region agent log
            android.util.Log.w(
                "LuzCamaraDiag",
                "bind concurrente: reintento pendiente hasPreview=${previewView != null} state=${hardwareState.name}"
            )
            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                hypothesisId = "H-LCAM-BIND",
                location = "CameraXSession.bind.busy",
                message = "bind concurrente encolado (no omitido)",
                data = mapOf(
                    "hardwareState" to hardwareState.name,
                    "hasPreview" to (previewView != null)
                ),
                runId = "luz-isolation"
            )
            // #endregion
            return
        }
        this.onReading = onReading
        this.onStateChange = onStateChange
        lensFacing = when (facing) {
            CameraLensFacing.BACK -> CameraSelector.LENS_FACING_BACK
            CameraLensFacing.FRONT -> CameraSelector.LENS_FACING_FRONT
        }

        if (previewView == null) {
            cameraProvider?.unbindAll()
            updateState(CameraHardwareState.INITIALIZING)
            camera2LightMeter.start(
                lensFacing = lensFacing,
                onSample = { sample ->
                    emitReading(
                        frameWidth = sample.width,
                        frameHeight = sample.height,
                        brightness = sample.luma,
                        format = "YUV_420_888",
                        exposureTimeNs = sample.exposureTimeNs,
                        iso = sample.iso,
                        aperture = sample.aperture
                    )
                    if (hardwareState != CameraHardwareState.RUNNING) {
                        updateState(CameraHardwareState.RUNNING)
                    }
                },
                onError = { message ->
                    updateState(CameraHardwareState.ERROR)
                    emitReading(errorMessage = message)
                }
            )
            finishBinding()
            return
        }
        camera2LightMeter.stop()

        val provider = cameraProvider
        if (provider == null) {
            prepare { ok ->
                isBinding.set(false)
                if (ok) {
                    bind(lifecycleOwner, previewView, facing, onReading, onStateChange)
                } else {
                    emitReading(errorMessage = "Cámara no disponible")
                    finishBinding()
                }
            }
            return
        }

        updateState(CameraHardwareState.INITIALIZING)
        provider.unbindAll()

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        if (!provider.hasCamera(selector)) {
            updateState(CameraHardwareState.NO_CAMERA)
            emitReading(errorMessage = "Cámara seleccionada no disponible")
            finishBinding()
            return
        }

        val useCases = mutableListOf<androidx.camera.core.UseCase>()

        if (previewView != null) {
            val preview = Preview.Builder().build().also { useCase ->
                useCase.surfaceProvider = previewView.surfaceProvider
            }
            useCases.add(preview)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            useCases.add(imageCapture!!)
        } else {
            // En el S20 FE, captura + análisis sin vista previa cierra la app.
            // El HAL de Samsung igual exige un Preview; se le da una superficie oculta.
            imageCapture = null
            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .build()
            attachHeadlessPreview(preview)
            useCases.add(preview)
        }

        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        if (previewView == null) {
            analysisBuilder.setTargetResolution(android.util.Size(640, 480))
        }
        val analysis = analysisBuilder.build()

        analysisExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        analysis.setAnalyzer(analysisExecutor!!) { image ->
            try {
                val now = SystemClock.elapsedRealtime()
                if (lastFrameTimestampMs > 0L) {
                    val dtSec = (now - lastFrameTimestampMs) / 1000.0
                    if (dtSec > 0.0) {
                        val instantFps = 1.0 / dtSec
                        fpsEstimate = fpsEstimate?.let { prev ->
                            prev * 0.85 + instantFps * 0.15
                        } ?: instantFps
                    }
                }
                lastFrameTimestampMs = now

                val lumaFrame = LumaFrameExtractor.extract(image, System.currentTimeMillis())
                val brightness = lumaFrame
                    ?.luma
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { samples -> samples.sumOf(Int::toLong).toDouble() / samples.size }
                val exposure = CameraFrameExposure.read(image)

                if (brightness != null && now - lastLightCalibrationLogMs >= 500L) {
                    lastLightCalibrationLogMs = now
                    val rawEquivalent = brightness / 255.0 * 60_000.0
                    // #region agent log
                    try {
                        android.util.Log.i(
                            "LuzCamaraDiag",
                            "frame lumaPromedio=%.3f rawEquivalente=%.1f clasificacion=PENDIENTE_CALIBRACION lens=%s size=%dx%d"
                                .format(
                                    brightness,
                                    rawEquivalent,
                                    if (lensFacing == CameraSelector.LENS_FACING_BACK) "BACK" else "FRONT",
                                    image.width,
                                    image.height
                                )
                        )
                        com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                            hypothesisId = "H-LCAM-LUMA",
                            location = "CameraXSession.ImageAnalysis",
                            message = "Muestra luma para calibración",
                            data = mapOf(
                                "lumaAverage" to brightness,
                                "rawEquivalent" to rawEquivalent,
                                "classification" to "PENDIENTE_CALIBRACION",
                                "lens" to if (lensFacing == CameraSelector.LENS_FACING_BACK) "BACK" else "FRONT",
                                "width" to image.width,
                                "height" to image.height
                            ),
                            runId = "camera-calibration"
                        )
                    } catch (logError: Exception) {
                        android.util.Log.e("LuzCamaraDiag", "Fallo al registrar luma: ${logError.message}", logError)
                    }
                    // #endregion
                }

                lumaFrame?.let { frame -> frameAnalyzer?.invoke(frame) }

                emitReading(
                    frameWidth = image.width,
                    frameHeight = image.height,
                    brightness = brightness,
                    fps = fpsEstimate,
                    format = when (image.format) {
                        ImageFormat.YUV_420_888 -> "YUV_420_888"
                        else -> "format_${image.format}"
                    },
                    exposureTimeNs = exposure?.exposureTimeNs,
                    iso = exposure?.iso,
                    aperture = exposure?.aperture
                )
            } catch (error: Exception) {
                // #region agent log
                android.util.Log.e("LuzCamaraDiag", "Error en ImageAnalysis: ${error.message}", error)
                com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                    hypothesisId = "H-LCAM-BIND",
                    location = "CameraXSession.ImageAnalysis.catch",
                    message = "Excepción en analyzer de cámara",
                    data = mapOf(
                        "exception" to error.javaClass.name,
                        "message" to (error.message ?: "")
                    ),
                    runId = "relative-light"
                )
                // #endregion
                emitReading(errorMessage = "Error al analizar frame de cámara")
            } finally {
                image.close()
            }
        }
        useCases.add(analysis)

        try {
            boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                *useCases.toTypedArray()
            )
            // #region agent log
            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                hypothesisId = "H-CAM-I",
                location = "CameraXSession.bind",
                message = "bindToLifecycle OK",
                data = mapOf(
                    "facing" to lensFacing,
                    "useCases" to useCases.size,
                    "hasPreview" to (previewView != null)
                )
            )
            android.util.Log.i(
                "LuzCamaraDiag",
                "bindToLifecycle OK hasPreview=${previewView != null} useCases=${useCases.size}"
            )
            // #endregion
            updateState(CameraHardwareState.RUNNING)
            emitReading()
        } catch (e: Exception) {
            // #region agent log
            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                hypothesisId = "H-CAM-I",
                location = "CameraXSession.bind.catch",
                message = "bindToLifecycle ERROR",
                data = mapOf("error" to (e.message ?: e.javaClass.simpleName))
            )
            // #endregion
            updateState(CameraHardwareState.ERROR)
            emitReading(errorMessage = "Error al abrir la cámara")
        } finally {
            finishBinding()
        }
    }

    private fun finishBinding() {
        isBinding.set(false)
        val next = pendingBind ?: return
        pendingBind = null
        // #region agent log
        android.util.Log.i(
            "LuzCamaraDiag",
            "aplicando bind pendiente hasPreview=${next.previewView != null}"
        )
        // #endregion
        bind(
            lifecycleOwner = next.lifecycleOwner,
            previewView = next.previewView,
            facing = next.facing,
            onReading = next.onReading,
            onStateChange = next.onStateChange
        )
    }

    /**
     * Congela la autoexposición después de obtener la línea base para que CameraX no compense
     * los cambios de iluminación que este experimento necesita observar.
     */
    @ExperimentalCamera2Interop
    fun setAutoExposureLock(locked: Boolean) {
        val camera = boundCamera
        if (camera == null) {
            android.util.Log.w("LuzCamaraDiag", "AE lock=$locked omitido: cámara aún no vinculada")
            return
        }
        try {
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
                .build()
            val future = Camera2CameraControl.from(camera.cameraControl)
                .setCaptureRequestOptions(options)
            future.addListener(
                {
                    // #region agent log
                    runCatching { future.get() }
                        .onSuccess {
                            android.util.Log.i("LuzCamaraDiag", "AE lock aplicado=$locked")
                            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                                hypothesisId = "H-LCAM-AE",
                                location = "CameraXSession.setAutoExposureLock",
                                message = "Control AE actualizado",
                                data = mapOf("locked" to locked, "success" to true),
                                runId = "relative-light"
                            )
                        }
                        .onFailure { error ->
                            android.util.Log.e("LuzCamaraDiag", "AE lock falló: ${error.message}", error)
                        }
                    // #endregion
                },
                mainExecutor
            )
        } catch (error: Exception) {
            android.util.Log.e("LuzCamaraDiag", "AE lock no soportado: ${error.message}", error)
        }
    }

    fun switchLens(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        newFacing: CameraLensFacing
    ) {
        val readingCallback = onReading ?: return
        val stateCallback = onStateChange ?: return
        unbind()
        bind(lifecycleOwner, previewView, newFacing, readingCallback, stateCallback)
    }

    fun capturePhoto(outputFile: File, onResult: (Result<String>) -> Unit) {
        val capture = imageCapture
        if (capture == null || hardwareState != CameraHardwareState.RUNNING) {
            onResult(Result.failure(IllegalStateException("Cámara no está en ejecución")))
            return
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    emitReading(capturedPath = outputFile.absolutePath)
                    onResult(Result.success(outputFile.absolutePath))
                }

                override fun onError(exception: ImageCaptureException) {
                    emitReading(errorMessage = exception.message ?: "Error de captura")
                    onResult(Result.failure(exception))
                }
            }
        )
    }

    fun unbind() {
        pendingBind = null
        camera2LightMeter.stop()
        cameraProvider?.unbindAll()
        boundCamera = null
        imageCapture = null
        lastFrameTimestampMs = 0L
        fpsEstimate = null
        lastLightCalibrationLogMs = 0L
        if (hardwareState != CameraHardwareState.PERMISSION_REQUIRED &&
            hardwareState != CameraHardwareState.UNAVAILABLE &&
            hardwareState != CameraHardwareState.NO_CAMERA
        ) {
            updateState(CameraHardwareState.CLOSED)
        }
        isBinding.set(false)
    }

    fun release() {
        frameAnalyzer = null
        (analysisExecutor as? java.util.concurrent.ExecutorService)?.shutdown()
        analysisExecutor = null
        unbind()
        cameraProvider = null
        onReading = null
        onStateChange = null
        updateState(CameraHardwareState.CLOSED)
    }

    fun currentState(): CameraHardwareState = hardwareState

    fun setPermissionRequired() {
        updateState(CameraHardwareState.PERMISSION_REQUIRED)
        emitReading(errorMessage = "Permiso de cámara requerido")
    }

    /**
     * Superficie mínima para el Preview. Sin ella, el S20 FE cierra el proceso
     * al medir luz solo con ImageAnalysis.
     */
    private fun attachHeadlessPreview(preview: Preview) {
        preview.setSurfaceProvider { request ->
            try {
                val texture = SurfaceTexture(false)
                texture.setDefaultBufferSize(request.resolution.width, request.resolution.height)
                val surface = Surface(texture)
                headlessTexture = texture
                headlessSurface = surface
                request.provideSurface(surface, mainExecutor) {
                    surface.release()
                    texture.release()
                    if (headlessSurface === surface) {
                        headlessSurface = null
                        headlessTexture = null
                    }
                }
            } catch (error: Exception) {
                android.util.Log.e("LuzCamaraDiag", "Preview oculto falló: ${error.message}", error)
                request.willNotProvideSurface()
            }
        }
    }

    private fun updateState(state: CameraHardwareState) {
        hardwareState = state
        onStateChange?.invoke(state)
    }

    private fun emitReading(
        frameWidth: Int? = null,
        frameHeight: Int? = null,
        brightness: Double? = null,
        fps: Double? = null,
        format: String? = null,
        capturedPath: String? = null,
        errorMessage: String? = null,
        exposureTimeNs: Long? = null,
        iso: Int? = null,
        aperture: Double? = null
    ) {
        val facing = when (lensFacing) {
            CameraSelector.LENS_FACING_FRONT -> CameraLensFacing.FRONT
            else -> CameraLensFacing.BACK
        }
        val reading = CameraReading(
            timestamp = System.currentTimeMillis(),
            hardwareState = hardwareState,
            lensFacing = facing,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            brightness = brightness,
            fpsEstimate = fps,
            imageFormat = format,
            capturedImagePath = capturedPath,
            errorMessage = errorMessage,
            exposureTimeNs = exposureTimeNs,
            iso = iso,
            aperture = aperture
        ).toSensorReading()
        onReading?.invoke(reading)
    }
}
