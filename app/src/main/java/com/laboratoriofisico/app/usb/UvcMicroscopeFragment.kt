package com.laboratoriofisico.app.usb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.IFrameCallback
import com.jiangdg.uvc.UVCCamera
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Apertura UVC directa. La librería CameraFragment negocia a 61 fps y prepare_preview
 * responde err=-51. Aquí el rango es 1–30 y el permiso USB sale por el manager,
 * con PendingIntent mutable, porque USBMonitor usa FLAG_IMMUTABLE en Android 14.
 */
class UvcMicroscopeFragment() : Fragment(), TextureView.SurfaceTextureListener {

    constructor(manager: UvcMicroscopeManager) : this() {
        activeManager = manager
    }

    private val manager: UvcMicroscopeManager?
        get() = activeManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private val saveExecutor = Executors.newSingleThreadExecutor()
    private var textureView: TextureView? = null
    private var previewSurface: Surface? = null
    private var usbMonitor: USBMonitor? = null
    private var surfaceReady = false
    private var timeoutRunnable: Runnable? = null
    private var frameLogRunnable: Runnable? = null

    private val deviceListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            debug("onAttach ${device.usbLabel()}")
            if (!device.isUvcCamera()) return
            stream.device = device
            mainHandler.post { openDeviceOnce(device) }
        }

        override fun onConnect(
            device: UsbDevice,
            ctrlBlock: USBMonitor.UsbControlBlock,
            createNew: Boolean
        ) {
            debug("onConnect ${device.usbLabel()} createNew=$createNew permiso=true")
            if (!device.isUvcCamera()) return
            stream.device = device
            stream.ctrl = ctrlBlock
            if (stream.openRequested || stream.opening || stream.camera != null) {
                debug("onConnect no abre otra cámara")
                if (stream.needsRebind) mainHandler.post { rebindSurface() }
                return
            }
            mainHandler.post { openWhenReady(ctrlBlock) }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            debug("onDisconnect ${device.usbLabel()}")
            if (!device.belongsToSession()) return
            stream.device = null
            mainHandler.post { closeCamera() }
        }

        override fun onDetach(device: UsbDevice) {
            debug("onDetach ${device.usbLabel()}")
            if (!device.belongsToSession()) return
            stream.device = null
            mainHandler.post { closeCamera() }
        }

        override fun onCancel(device: UsbDevice) {
            debug("onCancel permiso USB rechazado ${device.usbLabel()}")
            stream.permissionAsked = false
            stream.openRequested = false
            stream.opening = false
            mainHandler.post {
                manager?.onPreviewError("Android no concedió permiso para usar el microscopio USB.")
            }
        }
    }

    private val frameCallback = IFrameCallback { frame ->
        if (frame == null || frame.remaining() <= 0) return@IFrameCallback
        val n = stream.frames.incrementAndGet()
        if (n == 1) {
            val label = stream.label ?: "desconocido"
            debug("primer frame real formato=$label bytes=${frame.remaining()}; se oculta el texto de estado")
            mainHandler.post {
                cancelTimeout()
                manager?.onPreviewOpened()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        appContext = inflater.context.applicationContext
        if (stream.camera == null && !stream.opening) {
            resetCameraLog()
            debug("log reiniciado al abrir Experimentos api=${Build.VERSION.SDK_INT}")
        }
        val preview = TextureView(inflater.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            surfaceTextureListener = this@UvcMicroscopeFragment
            isOpaque = true
        }
        textureView = preview
        return FrameLayout(inflater.context).apply { addView(preview) }
    }

    override fun onStart() {
        super.onStart()
        val cameraGranted = hasCameraPermission()
        debug("onStart permiso CAMERA=$cameraGranted")
        if (!cameraGranted) {
            manager?.setPreviewPhase("Pidiendo permiso…")
        }
        manager?.attachFragment(this)
        registerMonitor()
        startFrameLog()
    }

    override fun onStop() {
        val rotating = activity?.isChangingConfigurations == true
        debug("onStop rotacion=$rotating frames=${stream.frames.get()}")
        stopFrameLog()
        if (!rotating) {
            unregisterMonitor()
        }
        super.onStop()
    }

    override fun onDestroyView() {
        val rotating = activity?.isChangingConfigurations == true
        debug("onDestroyView rotacion=$rotating frames=${stream.frames.get()}")
        stopFrameLog()
        cancelTimeout()
        textureView?.surfaceTextureListener = null
        textureView = null
        surfaceReady = false
        stream.needsRebind = stream.camera != null
        previewSurface?.release()
        previewSurface = null
        if (!rotating) {
            closeCamera()
            unregisterMonitor()
            manager?.detachFragment(this)
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        saveExecutor.shutdown()
        super.onDestroy()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceReady = true
        previewSurface?.release()
        previewSurface = Surface(surface)
        debug("superficie creada ${width}x$height valida=${previewSurface?.isValid == true}")
        if (stream.camera != null) {
            rebindSurface()
            return
        }
        val pending = stream.ctrl
        if (pending != null && !stream.opening) {
            openWhenReady(pending)
            return
        }
        val device = stream.device
        if (device != null) openDeviceOnce(device) else registerMonitor()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        debug("superficie medida ${width}x$height")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        debug("superficie destruida; la cámara no se cierra frames=${stream.frames.get()}")
        surfaceReady = false
        stream.needsRebind = stream.camera != null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    fun tryOpenCamera() {
        if (stream.camera != null || stream.opening) return
        registerMonitor()
    }

    fun capturePhoto(outputFile: File, onResult: (Result<String>) -> Unit) {
        val bitmap = textureView
            ?.takeIf { it.isAvailable && stream.frames.get() > 0 }
            ?.bitmap
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            onResult(Result.failure(IllegalStateException("Aún no llegó una imagen del microscopio.")))
            return
        }
        saveExecutor.execute {
            val result = runCatching {
                outputFile.parentFile?.mkdirs()
                FileOutputStream(outputFile).use { streamOut ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, streamOut))
                }
                bitmap.recycle()
                outputFile.absolutePath
            }
            mainHandler.post { onResult(result) }
        }
    }

    private fun registerMonitor() {
        if (!isAdded) return
        runCatching {
            val monitor = USBMonitor.getInstance(requireContext().applicationContext)
            monitor.setOnDeviceConnectListener(deviceListener)
            usbMonitor = monitor
            if (monitor.isRegistered) {
                debug("USBMonitor ya estaba registrado")
            } else {
                monitor.register()
                debug("USBMonitor registrado en onStart")
            }
            logUsbDevices()
            if (stream.camera != null) {
                if (stream.needsRebind && surfaceReady) rebindSurface()
                return@runCatching
            }
            val device = monitor.deviceList.orEmpty().firstOrNull { it.isUvcCamera() } ?: stream.device
            if (device == null) {
                debug("no hay dispositivo UVC")
                return@runCatching
            }
            stream.device = device
            openDeviceOnce(device)
        }.onFailure { error ->
            debug("No se pudo iniciar el controlador UVC", error)
            manager?.onPreviewError("No se pudo iniciar el controlador UVC: ${error.message.orEmpty()}")
        }
    }

    private fun unregisterMonitor() {
        val monitor = usbMonitor ?: return
        runCatching {
            if (monitor.isRegistered) {
                monitor.unregister()
                debug("USBMonitor desregistrado")
            }
        }.onFailure { error ->
            debug("USBMonitor unregister falló", error)
        }
        usbMonitor = null
    }

    private fun openDeviceOnce(device: UsbDevice) {
        stream.device = device
        if (!hasCameraPermission()) {
            debug("permiso CAMERA=false; no se abre ${device.usbLabel()}")
            manager?.setPreviewPhase("Pidiendo permiso…")
            return
        }
        val usbGranted = hasUsbPermission(device)
        debug("permiso CAMERA=true USB=$usbGranted ${device.usbLabel()}")
        if (!usbGranted) {
            if (!stream.permissionAsked) {
                stream.permissionAsked = true
                debug("permiso USB pendiente; PendingIntent FLAG_MUTABLE y receiver RECEIVER_NOT_EXPORTED")
                manager?.setPreviewPhase("Pidiendo permiso…")
                manager?.requestUsbPermission()
            }
            return
        }
        stream.permissionAsked = false
        if (stream.camera != null) {
            if (stream.needsRebind) rebindSurface()
            return
        }
        if (stream.opening || stream.openRequested) return
        if (!surfaceReady || previewSurface?.isValid != true) {
            debug("permiso USB concedido; espera superficie")
            return
        }
        val monitor = usbMonitor
        if (monitor == null) {
            debug("openDevice sin USBMonitor")
            return
        }
        stream.openRequested = true
        val block = runCatching { monitor.openDevice(device) }.getOrElse { error ->
            stream.openRequested = false
            debug("openDevice falló", error)
            manager?.onPreviewError("No se pudo abrir la conexión USB del microscopio.")
            null
        } ?: return
        openWhenReady(block)
    }

    private fun openWhenReady(ctrlBlock: USBMonitor.UsbControlBlock) {
        stream.ctrl = ctrlBlock
        stream.openRequested = false
        if (stream.camera != null) {
            if (stream.needsRebind) rebindSurface()
            return
        }
        if (!hasCameraPermission()) {
            debug("permiso CAMERA=false antes de UVCCamera.open")
            manager?.setPreviewPhase("Pidiendo permiso…")
            return
        }
        val texture = textureView?.surfaceTexture
        if (!surfaceReady || texture == null || previewSurface?.isValid != true) {
            debug("openWhenReady espera superficie")
            return
        }
        if (stream.opening) return
        stream.opening = true
        val session = stream.session
        ensureCameraThread()
        stream.handler?.post {
            if (stream.session != session) {
                stream.opening = false
                return@post
            }
            closeCameraInternal(notify = false)
            stream.frames.set(0)
            val openedCamera = UVCCamera()
            try {
                openedCamera.open(ctrlBlock)
                if (stream.session != session) {
                    runCatching { openedCamera.destroy() }
                    stream.opening = false
                    return@post
                }
                stream.attempts = buildAttempts(openedCamera)
                stream.camera = openedCamera
                stream.attemptIndex = 0
                tryAttempt(0, session)
            } catch (error: Throwable) {
                stream.opening = false
                debug("UVCCamera.open falló", error)
                runCatching { openedCamera.destroy() }
                if (stream.camera === openedCamera) stream.camera = null
                mainHandler.post {
                    manager?.onPreviewError(
                        "No se pudo iniciar el video UVC: ${error.message ?: error.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    private fun rebindSurface() {
        val camera = stream.camera ?: return
        val display = previewSurface
        if (display == null || !display.isValid) return
        stream.needsRebind = false
        debug("reengancha la superficie sin abrir otra cámara frames=${stream.frames.get()}")
        stream.handler?.post {
            runCatching { camera.setPreviewDisplay(display) }
                .onFailure { error -> debug("reenganche de superficie falló", error) }
        }
    }

    private fun tryAttempt(index: Int, session: Int) {
        if (stream.session != session) return
        val openedCamera = stream.camera ?: return
        val display = previewSurface
        val texture = textureView?.surfaceTexture
        if (display == null || !display.isValid || texture == null || !surfaceReady) {
            stream.opening = false
            debug("startPreview abortado: la superficie todavía no existe")
            return
        }
        val pending = stream.attempts
        if (index >= pending.size) {
            stream.opening = false
            debug("ningún formato entregó frames")
            runCatching { openedCamera.stopPreview() }
            mainHandler.post {
                manager?.onPreviewError("No se pudo abrir el microscopio con ningún formato.")
            }
            return
        }
        val attempt = pending[index]
        stream.attemptIndex = index
        stream.label = attempt.label
        debug("intento formato=${attempt.label} fps=$MIN_FPS-$MAX_FPS bw=${attempt.bandwidth}")
        try {
            runCatching { openedCamera.stopPreview() }
            openedCamera.setPreviewSize(
                attempt.width,
                attempt.height,
                MIN_FPS,
                MAX_FPS,
                attempt.format,
                attempt.bandwidth
            )
            texture.setDefaultBufferSize(attempt.width, attempt.height)
            openedCamera.setPreviewDisplay(display)
            openedCamera.startPreview()
            openedCamera.setFrameCallback(frameCallback, UVCCamera.PIXEL_FORMAT_NV21)
            runCatching { openedCamera.updateCameraParams() }
            debug("resultado intento=${attempt.label} startPreview=llamado")
            scheduleTimeout(index, session)
        } catch (error: Throwable) {
            debug("resultado intento=${attempt.label} error=${error.message}")
            tryAttempt(index + 1, session)
        }
    }

    private fun buildAttempts(uvc: UVCCamera): List<PreviewAttempt> {
        val mjpeg = runCatching { uvc.getSupportedSizeList(UVCCamera.FRAME_FORMAT_MJPEG) }
            .getOrNull()
            .orEmpty()
        val yuyv = runCatching { uvc.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV) }
            .getOrNull()
            .orEmpty()
        val device = stream.device
        debug(
            "dispositivo vid=${device?.vendorId} pid=${device?.productId} " +
                "clase=${device?.deviceClass} interfaces=${device?.interfaceCount}"
        )
        debug("lista completa formatos=${runCatching { uvc.supportedSize }.getOrNull()}")
        debug("soportados MJPEG=${mjpeg.joinToString { it.sizeLabel() }}")
        debug("soportados YUYV=${yuyv.joinToString { it.sizeLabel() }}")
        val specs = listOf(
            PreviewAttempt(640, 480, UVCCamera.FRAME_FORMAT_MJPEG, "MJPEG 640x480", 1.0f),
            PreviewAttempt(1280, 720, UVCCamera.FRAME_FORMAT_MJPEG, "MJPEG 1280x720", 1.0f),
            PreviewAttempt(320, 240, UVCCamera.FRAME_FORMAT_MJPEG, "MJPEG 320x240", 1.0f),
            PreviewAttempt(640, 480, UVCCamera.FRAME_FORMAT_YUYV, "YUYV 640x480", 1.0f),
            PreviewAttempt(320, 240, UVCCamera.FRAME_FORMAT_YUYV, "YUYV 320x240", 1.0f)
        )
        val attempts = specs.flatMap { spec ->
            listOf(spec, spec.copy(bandwidth = 0.5f, label = "${spec.label} bw=0.5"))
        }.toMutableList()
        val first = mjpeg.firstOrNull() ?: yuyv.firstOrNull()
        if (first != null) {
            val format = if (mjpeg.isNotEmpty()) UVCCamera.FRAME_FORMAT_MJPEG else UVCCamera.FRAME_FORMAT_YUYV
            val name = if (format == UVCCamera.FRAME_FORMAT_MJPEG) "MJPEG" else "YUYV"
            val label = "primer tamaño ${first.width}x${first.height} $name"
            attempts += PreviewAttempt(first.width, first.height, format, label, 1.0f)
            attempts += PreviewAttempt(first.width, first.height, format, "$label bw=0.5", 0.5f)
        }
        return attempts.distinctBy { "${it.format}:${it.width}x${it.height}:${it.bandwidth}" }
    }

    private fun scheduleTimeout(index: Int, session: Int) {
        mainHandler.post {
            cancelTimeout()
            val baseline = stream.frames.get()
            val runnable = Runnable {
                if (!isAdded || stream.session != session || stream.attemptIndex != index) return@Runnable
                if (stream.frames.get() > baseline) return@Runnable
                debug("timeout sin frames en ${FORMAT_TIMEOUT_MS}ms con ${stream.label}")
                stream.handler?.post { tryAttempt(index + 1, session) }
            }
            timeoutRunnable = runnable
            mainHandler.postDelayed(runnable, FORMAT_TIMEOUT_MS)
        }
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun startFrameLog() {
        stopFrameLog()
        val runnable = object : Runnable {
            override fun run() {
                if (!isAdded) return
                debug("conteo frames=${stream.frames.get()} formato=${stream.label ?: "ninguno"}")
                mainHandler.postDelayed(this, FRAME_LOG_MS)
            }
        }
        frameLogRunnable = runnable
        mainHandler.postDelayed(runnable, FRAME_LOG_MS)
    }

    private fun stopFrameLog() {
        frameLogRunnable?.let { mainHandler.removeCallbacks(it) }
        frameLogRunnable = null
    }

    private fun ensureCameraThread() {
        if (stream.thread?.isAlive == true && stream.handler != null) return
        val thread = HandlerThread("microscopio-uvc").also { it.start() }
        stream.thread = thread
        stream.handler = Handler(thread.looper)
    }

    private fun closeCamera() {
        stream.session += 1
        stream.opening = false
        stream.openRequested = false
        stream.needsRebind = false
        stream.frames.set(0)
        stream.label = null
        cancelTimeout()
        val handler = stream.handler
        if (handler == null) {
            closeCameraInternal(notify = true)
            return
        }
        handler.post { closeCameraInternal(notify = true) }
    }

    private fun closeCameraInternal(notify: Boolean) {
        val camera = stream.camera
        stream.camera = null
        camera?.let {
            runCatching { it.setFrameCallback(null, UVCCamera.PIXEL_FORMAT_NV21) }
            runCatching { it.stopPreview() }
            runCatching { it.destroy() }
        }
        if (notify) {
            debug("cámara cerrada")
            mainHandler.post { manager?.onPreviewClosed() }
        }
    }

    private fun logUsbDevices() {
        val usb = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usb.deviceList.values.toList()
        if (devices.isEmpty()) {
            debug("dispositivos USB=0")
            return
        }
        devices.forEach { device -> debug("USB detectado ${device.usbLabel()}") }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun hasUsbPermission(device: UsbDevice): Boolean {
        val usb = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
        return usb.hasPermission(device)
    }

    private fun resetCameraLog() {
        val file = logFile() ?: return
        runCatching {
            synchronized(logLock) { file.writeText("") }
        }
    }

    private fun debug(message: String, error: Throwable? = null) {
        if (error != null) Log.e(TAG, message, error) else Log.d(TAG, message)
        val file = logFile() ?: return
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = buildString {
            append(stamp)
            append(' ')
            append(message)
            if (error != null) {
                append(' ')
                append(error.javaClass.simpleName)
                append(": ")
                append(error.message)
            }
            append('\n')
        }
        runCatching { synchronized(logLock) { file.appendText(line) } }
    }

    private fun logFile(): File? {
        val ctx = context?.applicationContext ?: appContext ?: return null
        val dir = ctx.getExternalFilesDir(null) ?: return null
        return File(dir, "camera_debug.txt")
    }

    private fun UsbDevice.belongsToSession(): Boolean {
        val current = stream.device ?: return isUvcCamera()
        return current.deviceId == deviceId
    }

    private fun UsbDevice.isUvcCamera(): Boolean {
        if (deviceClass == UsbConstants.USB_CLASS_VIDEO || deviceClass == USB_CLASS_MISC) return true
        for (index in 0 until interfaceCount) {
            val usbInterface = getInterface(index)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_VIDEO) return true
            if (
                usbInterface.interfaceClass == USB_CLASS_MISC &&
                usbInterface.interfaceSubclass == USB_SUBCLASS_COMMON &&
                usbInterface.interfaceProtocol == USB_PROTOCOL_IAD
            ) {
                return true
            }
        }
        return false
    }

    private fun UsbDevice.usbLabel(): String {
        val ifaces = (0 until interfaceCount).joinToString(",") { index ->
            val usbInterface = getInterface(index)
            "${usbInterface.interfaceClass}/${usbInterface.interfaceSubclass}/${usbInterface.interfaceProtocol}"
        }
        return "vid=$vendorId pid=$productId class=$deviceClass id=$deviceId ifaces=[$ifaces]"
    }

    private fun com.jiangdg.utils.Size.sizeLabel(): String {
        val rates = fps?.joinToString(",") { it.toInt().toString() }.orEmpty().ifBlank { "?" }
        return "${width}x$height@$rates"
    }

    private data class PreviewAttempt(
        val width: Int,
        val height: Int,
        val format: Int,
        val label: String,
        val bandwidth: Float
    )

    private class StreamState {
        var camera: UVCCamera? = null
        var thread: HandlerThread? = null
        var handler: Handler? = null
        var opening = false
        var openRequested = false
        var permissionAsked = false
        var needsRebind = false
        var session = 0
        val frames = AtomicInteger(0)
        var attempts: List<PreviewAttempt> = emptyList()
        var attemptIndex = 0
        var label: String? = null
        var ctrl: USBMonitor.UsbControlBlock? = null
        var device: UsbDevice? = null
    }

    companion object {
        private const val TAG = "CameraDebug"
        private const val MIN_FPS = 1
        private const val MAX_FPS = 30
        private const val FORMAT_TIMEOUT_MS = 5_000L
        private const val FRAME_LOG_MS = 2_000L
        private const val USB_CLASS_MISC = 239
        private const val USB_SUBCLASS_COMMON = 2
        private const val USB_PROTOCOL_IAD = 1

        private val logLock = Any()
        private val stream = StreamState()

        @Volatile
        private var appContext: Context? = null

        @Volatile
        private var activeManager: UvcMicroscopeManager? = null
    }
}
