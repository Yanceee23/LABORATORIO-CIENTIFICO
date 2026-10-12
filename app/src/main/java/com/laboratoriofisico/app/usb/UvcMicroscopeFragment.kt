package com.laboratoriofisico.app.usb

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class UvcMicroscopeFragment(
    private val manager: UvcMicroscopeManager
) : CameraFragment() {
    private var rootView: FrameLayout? = null
    private var cameraContainer: FrameLayout? = null
    private var cameraView: AspectRatioTextureView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val saveExecutor = Executors.newSingleThreadExecutor()

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        val context = inflater.context
        val root = FrameLayout(context)
        val previewContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val preview = AspectRatioTextureView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        previewContainer.addView(preview)
        root.addView(previewContainer)
        rootView = root
        cameraContainer = previewContainer
        cameraView = preview
        return root
    }

    override fun getCameraView(): IAspectRatio? = cameraView

    override fun getCameraViewContainer(): ViewGroup? = cameraContainer

    override fun getGravity(): Int = Gravity.CENTER

    override fun getCameraRequest(): CameraRequest {
        UsbDebugNdjson.log(
            hypothesisId = "UVC-STABILITY",
            location = "UvcMicroscopeFragment.getCameraRequest",
            message = "Solicitando preview UVC mínimo y estable",
            data = mapOf(
                "requestedWidth" to STABLE_PREVIEW_WIDTH,
                "requestedHeight" to STABLE_PREVIEW_HEIGHT,
                "renderMode" to "NORMAL",
                "previewFormat" to "FORMAT_YUYV"
            )
        )
        return CameraRequest.Builder()
            .setPreviewWidth(STABLE_PREVIEW_WIDTH)
            .setPreviewHeight(STABLE_PREVIEW_HEIGHT)
            .setRenderMode(CameraRequest.RenderMode.NORMAL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setAudioSource(CameraRequest.AudioSource.NONE)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV)
            .setAspectRatioShow(true)
            .setCaptureRawImage(false)
            .setRawPreviewData(false)
            .create()
    }

    override fun onResume() {
        super.onResume()
        manager.attachFragment(this)
    }

    override fun onDestroyView() {
        manager.detachFragment(this)
        cameraView = null
        cameraContainer = null
        rootView = null
        super.onDestroyView()
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> manager.onPreviewOpened()
            ICameraStateCallBack.State.CLOSED -> manager.onPreviewClosed()
            ICameraStateCallBack.State.ERROR -> manager.onPreviewError(msg)
        }
    }

    fun capturePhoto(outputFile: File, onResult: (Result<String>) -> Unit) {
        try {
            UsbDebugNdjson.log(
                hypothesisId = "CAP",
                location = "UvcMicroscopeFragment.capturePhoto",
                message = "Antes de capturar frame del TextureView",
                data = mapOf(
                    "output" to outputFile.absolutePath,
                    "hasView" to (cameraView != null),
                    "viewAvailable" to (cameraView?.isAvailable == true)
                )
            )
            val preview = cameraView
            if (preview == null || !preview.isAvailable) {
                deliver(onResult, Result.failure(IllegalStateException("El preview UVC no está listo para capturar.")))
                return
            }
            val bitmap = preview.getBitmap()
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0 || bitmap.isRecycled) {
                UsbDebugNdjson.log(
                    hypothesisId = "CAP",
                    location = "UvcMicroscopeFragment.capturePhoto",
                    message = "Frame nulo o inválido",
                    data = mapOf(
                        "bitmapNull" to (bitmap == null),
                        "width" to bitmap?.width,
                        "height" to bitmap?.height
                    )
                )
                deliver(onResult, Result.failure(IllegalStateException("No se pudo leer el frame del microscopio.")))
                return
            }
            UsbDebugNdjson.log(
                hypothesisId = "CAP",
                location = "UvcMicroscopeFragment.capturePhoto",
                message = "Frame capturado del preview",
                data = mapOf("width" to bitmap.width, "height" to bitmap.height)
            )
            saveExecutor.execute {
                saveJpeg(bitmap, outputFile, onResult)
            }
        } catch (error: Throwable) {
            UsbDebugNdjson.log(
                hypothesisId = "CAP",
                location = "UvcMicroscopeFragment.capturePhoto.catch",
                message = "Excepción al capturar frame",
                data = mapOf("error" to (error.message ?: error.javaClass.simpleName))
            )
            deliver(onResult, Result.failure(IllegalStateException("Error al capturar la imagen del microscopio.", error)))
        }
    }

    private fun saveJpeg(bitmap: Bitmap, outputFile: File, onResult: (Result<String>) -> Unit) {
        try {
            UsbDebugNdjson.log(
                hypothesisId = "CAP",
                location = "UvcMicroscopeFragment.saveJpeg",
                message = "Antes de guardar JPEG",
                data = mapOf("output" to outputFile.absolutePath)
            )
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { stream ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
                if (!compressed) {
                    throw IllegalStateException("No se pudo comprimir el frame a JPEG.")
                }
            }
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            if (!outputFile.exists() || outputFile.length() == 0L) {
                throw IllegalStateException("El archivo JPEG quedó vacío.")
            }
            UsbDebugNdjson.log(
                hypothesisId = "CAP",
                location = "UvcMicroscopeFragment.saveJpeg",
                message = "JPEG guardado",
                data = mapOf(
                    "output" to outputFile.absolutePath,
                    "bytes" to outputFile.length()
                )
            )
            deliver(onResult, Result.success(outputFile.absolutePath))
        } catch (error: Throwable) {
            runCatching { if (!bitmap.isRecycled) bitmap.recycle() }
            runCatching { if (outputFile.exists()) outputFile.delete() }
            UsbDebugNdjson.log(
                hypothesisId = "CAP",
                location = "UvcMicroscopeFragment.saveJpeg.catch",
                message = "Excepción al guardar JPEG",
                data = mapOf("error" to (error.message ?: error.javaClass.simpleName))
            )
            deliver(onResult, Result.failure(IllegalStateException("Error al guardar la fotografía.", error)))
        }
    }

    private fun deliver(onResult: (Result<String>) -> Unit, result: Result<String>) {
        mainHandler.post {
            try {
                onResult(result)
            } catch (error: Throwable) {
                UsbDebugNdjson.log(
                    hypothesisId = "CAP",
                    location = "UvcMicroscopeFragment.deliver",
                    message = "Excepción en callback de captura",
                    data = mapOf("error" to (error.message ?: error.javaClass.simpleName))
                )
            }
        }
    }

    private companion object {
        const val STABLE_PREVIEW_WIDTH = 160
        const val STABLE_PREVIEW_HEIGHT = 120
    }
}
